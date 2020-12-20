/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.content.ClipDescription;//指定小部件的文本描述
import android.content.ContentProvider;
import android.content.ContentUris;//用于获取Uri路径后面的ID部分
import android.content.ContentValues; //类似HashTable,HashTable可以存储数据包括对象，ContentValues只能存储数据
import android.content.Context;//一个接口，提供有关应用的全局信息（指允许访问该应用的资源和类以及活动等）
import android.content.UriMatcher; //用于匹配Uri
import android.content.ContentProvider.PipeDataWriter;//将数据流写入管道的接口
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;  //报错处理
import android.database.Cursor;//Cursor是一个数据源，指向每一条数据，查询数据的时候用
import android.database.SQLException;//数据库异常处理
import android.database.sqlite.SQLiteDatabase;//一个数据库对象，提供了操作数据库的方法，是数据库操作类
import android.database.sqlite.SQLiteOpenHelper;//是SQLiteDatabase的帮助类，用来管理数据库的创建和版本更新
import android.database.sqlite.SQLiteQueryBuilder;//用来构建SQL查询的类
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;//用于携带数据，类似Map,可存放key-value名值对形式的值
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;//处理Text内容的集合方法类
import android.util.Log;//日志工作类，用于打印日志

import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;//访问出错处理
import java.io.FileOutputStream;//文件字节输出
import java.io.IOException;//异常处理
import java.io.OutputStreamWriter;//可使用指定的charset将要写入流中的字符编码成字节
import java.io.PrintWriter;//格式化打印输出
import java.io.UnsupportedEncodingException;//字符集异常处理
import java.sql.Date;
import java.util.HashMap;//hashmap<k,v>,k是映射所维护的键的类型，v是所映射值的类型

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 * 提供对笔记数据库的访问。每个注释都有一个标题、注释本身、创建日期和修改后的数据。
 */
public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {
    // Used for debugging and logging 用于调试和记录
    private static final String TAG = "NotePadProvider";

    /**
     * The database that the provider uses as its underlying data store
     * 提供程序用作其基础数据存储的数据库
     */
    private static final String DATABASE_NAME = "note_pad.db";

    /**
     * The database version 数据库版本
     */
    private static final int DATABASE_VERSION = 2;

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sNotesProjectionMap;

    /**
     * A projection map used to select columns from the database
     * 用于从数据库中选择列的投影图
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /**
     * Standard projection for the interesting columns of a normal note.
     * 普通音符有趣列的标准投影。
     */
    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,               // Projection position 0, the note's id
            NotePad.Notes.COLUMN_NAME_NOTE,  // Projection position 1, the note's content
            NotePad.Notes.COLUMN_NAME_TITLE, // Projection position 2, the note's title
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;
    /*
     * Constants used by the Uri matcher to choose an action based on the pattern
     * of the incoming URI
     * Uri匹配器根据传入Uri的模式选择操作时使用的常量
     */
    // The incoming URI matches the Notes URI pattern 传入的URI与Notes URI模式匹配
    private static final int NOTES = 1;

    // The incoming URI matches the Note ID URI pattern
    private static final int NOTE_ID = 2;

    // The incoming URI matches the Live Folder URI pattern
    private static final int LIVE_FOLDER_NOTES = 3;

    /**
     * A UriMatcher instance UriMatcher实例
     */
    private static final UriMatcher sUriMatcher;

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;


    /**
     * A block that instantiates and sets static objects 实例化和设置静态对象的块
     */
    static {

        /*
         * Creates and initializes the URI matcher 创建并初始化URI匹配器
         */
        // Create a new instance 创建新实例
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a pattern that routes URIs terminated with "notes" to a NOTES operation
        // 添加一个模式，将以“notes”结尾的uri路由到notes操作
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

        // Add a pattern that routes URIs terminated with "notes" plus an integer
        // to a note ID operation 添加一个模式，该模式将以“notes”和一个整数结尾的uri路由到note ID操作
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        // Add a pattern that routes URIs terminated with live_folders/notes to a
        // live folder operation
        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);

        /*
         * Creates and initializes a projection map that returns all columns
         * 创建并初始化返回所有列的投影映射
         */

        // Creates a new projection map instance. The map returns a column name
        // given a string. The two are usually equal.
        // 创建新的投影贴图实例。映射返回给定字符串的列名。两者通常是相等的。
        sNotesProjectionMap = new HashMap<String, String>();

        // Maps the string "_ID" to the column name "_ID" 将字符串“_ID”映射到列名“_ID”
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);

        // Maps "title" to "title"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);

        // Maps "note" to "note"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);

        // Maps "created" to "created"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);

        // Maps "modified" to "modified"
        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        /*
         * Creates an initializes a projection map for handling Live Folders
         */

        // Creates a new projection map instance 创建新的投影贴图实例
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // Maps "_ID" to "_ID AS _ID" for a live folder
        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);

        // Maps "NAME" to "title AS NAME"
        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
            LiveFolders.NAME);
    }

    /**
    *
    * This class helps open, create, and upgrade the database file. Set to package visibility
    * for testing purposes.
     * 这个类帮助打开、创建和升级数据库文件。设置为包可见性以进行测试。
    */
   static class DatabaseHelper extends SQLiteOpenHelper {

       DatabaseHelper(Context context) {

           // calls the super constructor, requesting the default cursor factory.
           // 调用超级构造函数，请求默认游标工厂。
           super(context, DATABASE_NAME, null, DATABASE_VERSION);
       }

       /**
        *
        * Creates the underlying database with table name and column names taken from the
        * NotePad class.使用来自记事本类的表名和列名创建基础数据库。
        */
       @Override
       public void onCreate(SQLiteDatabase db) {
           db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                   + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                   + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                   + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                   + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                   + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                   + ");");
       }

       /**
        *
        * Demonstrates that the provider must consider what happens when the
        * underlying datastore is changed. In this sample, the database is upgraded the database
        * by destroying the existing data.
        * A real application should upgrade the database in place.
        * 演示提供程序必须考虑更改基础数据存储时发生的情况。
        * 在此示例中，通过销毁现有数据来升级数据库。一个真正的应用程序应该升级数据库。
        */
       @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

           // Logs that the database is being upgraded
           Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                   + newVersion + ", which will destroy all old data");

           // Kills the table and existing data
           db.execSQL("DROP TABLE IF EXISTS notes");

           // Recreates the database with a new version
           onCreate(db);
       }
   }

   /**
    *
    * Initializes the provider by creating a new DatabaseHelper. onCreate() is called
    * automatically when Android creates the provider in response to a resolver request from a
    * client.
    * 通过创建新的DatabaseHelper初始化提供程序。
    * 当Android创建提供者以响应来自客户端的解析程序请求时，onCreate（）会自动调用。
    */
   @Override
   public boolean onCreate() {

       // Creates a new helper object. Note that the database itself isn't opened until
       // something tries to access it, and it's only created if it doesn't already exist.
       //创建新的辅助对象。请注意，在有人试图访问数据库之前，数据库本身是不会打开的，
       // 并且只有在数据库不存在的情况下才会创建它。
       mOpenHelper = new DatabaseHelper(getContext());

       // Assumes that any failures will be reported by a thrown exception.假设任何失败都将由引发的异常报告。
       return true;
   }

   /**
    * This method is called when a client calls 当客户端调用
    * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)}.
    * Queries the database and returns a cursor containing the results.
    *查询数据库并返回包含结果的游标。
    * @return A cursor containing the results of the query. The cursor exists but is empty if
    * the query returns no results or an exception occurs.
    * 包含查询结果的游标。游标存在，但如果查询未返回结果或发生异常，则游标为空。
    * @throws IllegalArgumentException if the incoming URI pattern is invalid.
    * 如果传入的URI模式无效，则为IllegalArgumentException。
    */
   @Override
   public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
           String sortOrder) {

       // Constructs a new query builder and sets its table name 构造新的查询生成器并设置其表名
       SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
       qb.setTables(NotePad.Notes.TABLE_NAME);

       /**
        * Choose the projection and adjust the "where" clause based on URI pattern-matching.
        * 选择投影并根据URI模式匹配调整“where”子句。
        */
       switch (sUriMatcher.match(uri)) {
           // If the incoming URI is for notes, chooses the Notes projection
           //如果传入的URI用于notes，则选择notes投影
           case NOTES:
               qb.setProjectionMap(sNotesProjectionMap);
               break;

           /* If the incoming URI is for a single note identified by its ID, chooses the
            * note ID projection, and appends "_ID = <noteID>" to the where clause, so that
            * it selects that single note
            * 如果传入的URI是针对由其ID标识的单个注释，则选择noteID投影，
            * 并在where子句中追加“_ID=<noteID>”，以便它选择该单个注释
            */
           case NOTE_ID:
               qb.setProjectionMap(sNotesProjectionMap);
               qb.appendWhere(
                   NotePad.Notes._ID +    // the name of the ID column
                   "=" +
                   // the position of the note ID itself in the incoming URI
                           // 注释ID本身在传入URI中的位置
                   uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
               break;

           case LIVE_FOLDER_NOTES:
               // If the incoming URI is from a live folder, chooses the live folder projection.
               qb.setProjectionMap(sLiveFolderProjectionMap);
               break;

           default:
               // If the URI doesn't match any of the known patterns, throw an exception.
               throw new IllegalArgumentException("Unknown URI " + uri);
       }


       String orderBy;
       // If no sort order is specified, uses the default 如果未指定排序顺序，则使用默认值
       if (TextUtils.isEmpty(sortOrder)) {
           orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
       } else {
           // otherwise, uses the incoming sort order
           orderBy = sortOrder;
       }

       // Opens the database object in "read" mode, since no writes need to be done.
       SQLiteDatabase db = mOpenHelper.getReadableDatabase();

       /*
        * Performs the query. If no problems occur trying to read the database, then a Cursor
        * object is returned; otherwise, the cursor variable contains null. If no records were
        * selected, then the Cursor object is empty, and Cursor.getCount() returns 0.
        */
       Cursor c = qb.query(
           db,            // The database to query
           projection,    // The columns to return from the query
           selection,     // The columns for the where clause
           selectionArgs, // The values for the where clause
           null,          // don't group the rows
           null,          // don't filter by row groups
           orderBy        // The sort order
       );

       // Tells the Cursor what URI to watch, so it knows when its source data changes
       c.setNotificationUri(getContext().getContentResolver(), uri);
       return c;
   }

   /**
    * This is called when a client calls {@link android.content.ContentResolver#getType(Uri)}.
    * Returns the MIME data type of the URI given as a parameter.
    *
    * @param uri The URI whose MIME type is desired.
    * @return The MIME type of the URI.
    * @throws IllegalArgumentException if the incoming URI pattern is invalid.
    */
   @Override
   public String getType(Uri uri) {

       /**
        * Chooses the MIME type based on the incoming URI pattern
        */
       switch (sUriMatcher.match(uri)) {

           // If the pattern is for notes or live folders, returns the general content type.
           case NOTES:
           case LIVE_FOLDER_NOTES:
               return NotePad.Notes.CONTENT_TYPE;

           // If the pattern is for note IDs, returns the note ID content type.
           case NOTE_ID:
               return NotePad.Notes.CONTENT_ITEM_TYPE;

           // If the URI pattern doesn't match any permitted patterns, throws an exception.
           default:
               throw new IllegalArgumentException("Unknown URI " + uri);
       }
    }

//BEGIN_INCLUDE(stream)
    /**
     * This describes the MIME types that are supported for opening a note
     * URI as a stream.
     */
    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    /**
     * Returns the types of available data streams.  URIs to specific notes are supported.
     * The application can convert such a note to a plain text stream.
     *
     * @param uri the URI to analyze
     * @param mimeTypeFilter The MIME type to check for. This method only returns a data stream
     * type for MIME types that match the filter. Currently, only text/plain MIME types match.
     * @return a data stream MIME type. Currently, only text/plan is returned.
     * @throws IllegalArgumentException if the URI pattern doesn't match any supported patterns.
     */
    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        /**
         *  Chooses the data stream type based on the incoming URI pattern.
         */
        switch (sUriMatcher.match(uri)) {

            // If the pattern is for notes or live folders, return null. Data streams are not
            // supported for this type of URI.
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return null;

            // If the pattern is for note IDs and the MIME filter is text/plain, then return
            // text/plain
            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

                // If the URI pattern doesn't match any permitted patterns, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
    }


    /**
     * Returns a stream of data for each supported stream type. This method does a query on the
     * incoming URI, then uses
     * {@link android.content.ContentProvider#openPipeHelper(Uri, String, Bundle, Object,
     * PipeDataWriter)} to start another thread in which to convert the data into a stream.
     *
     * @param uri The URI pattern that points to the data stream
     * @param mimeTypeFilter A String containing a MIME type. This method tries to get a stream of
     * data with this MIME type.
     * @param opts Additional options supplied by the caller.  Can be interpreted as
     * desired by the content provider.
     * @return AssetFileDescriptor A handle to the file.
     * @throws FileNotFoundException if there is no file associated with the incoming URI.
     */
    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {

        // Checks to see if the MIME type filter matches a supported MIME type.
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

        // If the MIME type is supported
        if (mimeTypes != null) {

            // Retrieves the note for this URI. Uses the query method defined for this provider,
            // rather than using the database query method.
            Cursor c = query(
                    uri,                    // The URI of a note
                    READ_NOTE_PROJECTION,   // Gets a projection containing the note's ID, title,
                                            // and contents
                    null,                   // No WHERE clause, get all matching records
                    null,                   // Since there is no WHERE clause, no selection criteria
                    null                    // Use the default sort order (modification date,
                                            // descending
            );


            // If the query fails or the cursor is empty, stop
            if (c == null || !c.moveToFirst()) {

                // If the cursor is empty, simply close the cursor and return
                if (c != null) {
                    c.close();
                }

                // If the cursor is null, throw an exception
                throw new FileNotFoundException("Unable to query " + uri);
            }

            // Start a new thread that pipes the stream data back to the caller.
            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    /**
     * Implementation of {@link android.content.ContentProvider.PipeDataWriter}
     * to perform the actual work of converting the data in one of cursors to a
     * stream of data for the client to read.执行将某个游标中的数据转换为供客户端读取的数据流的实际工作。
     */
    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
            Bundle opts, Cursor c) {
        // We currently only support conversion-to-text from a single note entry,
        // so no need for cursor data type checking here.
        //我们目前只支持从一个注释条目到文本的转换，因此不需要在这里进行游标数据类型检查。
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Ooops", e);
        } finally {
            c.close();
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }
//END_INCLUDE(stream)

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#insert(Uri, ContentValues)}.
     * Inserts a new row into the database. This method sets up default values for any
     * columns that are not included in the incoming map.
     * If rows were inserted, then listeners are notified of the change.
     * @return The row ID of the inserted row.
     * @throws SQLException if the insertion fails.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        // Validates the incoming URI. Only the full provider URI is allowed for inserts.
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // A map to hold the new record's values.
        ContentValues values;

        // If the incoming values map is not null, uses it for the new values.
        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            // Otherwise, create a new value map
            values = new ContentValues();
        }

        // Gets the current system time in milliseconds
        Long now = Long.valueOf(System.currentTimeMillis());
        Date date = new Date(now);
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd ");
        String dateTime = format.format(date);
        // If the values map doesn't contain the creation date, sets the value to the current time.
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
        }

        // If the values map doesn't contain the modification date, sets the value to the current
        // time.
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }

        // If the values map doesn't contain a title, sets the value to the default title.
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE) == false) {
            Resources r = Resources.getSystem();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
        }

        // If the values map doesn't contain note text, sets the value to an empty string.
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Performs the insert and returns the ID of the new note.
        long rowId = db.insert(
            NotePad.Notes.TABLE_NAME,        // The table to insert into.
            NotePad.Notes.COLUMN_NAME_NOTE,  // A hack, SQLite sets this column value to null
                                             // if values is empty.
            values                           // A map of column names, and the values to insert
                                             // into the columns.
        );

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID appended to it.
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data changed.
            getContext().getContentResolver().notifyChange(noteUri, null);

            return noteUri;
        }
        // 新建笔记，背景默认为白色
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_BACK_COLOR) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, NotePad.Notes.COLOR0);
        }

        // If the insert didn't succeed, then the rowID is <= 0. Throws an exception.
        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#delete(Uri, String, String[])}.
     * Deletes records from the database. If the incoming URI matches the note ID URI pattern,
     * this method deletes the one record specified by the ID in the URI. Otherwise, it deletes a
     * a set of records. The record or records must also match the input selection criteria
     * specified by where and whereArgs.
     *
     * If rows were deleted, then listeners are notified of the change.
     * @return If a "where" clause is used, the number of rows affected is returned, otherwise
     * 0 is returned. To delete all rows and get a row count, use "1" as the where clause.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        // Does the delete based on the incoming URI pattern.
        switch (sUriMatcher.match(uri)) {

            // If the incoming pattern matches the general pattern for notes, does a delete
            // based on the incoming "where" columns and arguments.
            case NOTES:
                count = db.delete(
                    NotePad.Notes.TABLE_NAME,  // The database table name
                    where,                     // The incoming where clause column names
                    whereArgs                  // The incoming where clause values
                );
                break;

                // If the incoming URI matches a single note ID, does the delete based on the
                // incoming data, but modifies the where clause to restrict it to the
                // particular note ID.
            case NOTE_ID:
                /*
                 * Starts a final WHERE clause by restricting it to the
                 * desired note ID.
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // The ID column name
                        " = " +                                          // test for equality
                        uri.getPathSegments().                           // the incoming note ID
                            get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // If there were additional selection criteria, append them to the final
                // WHERE clause
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // Performs the delete.
                count = db.delete(
                    NotePad.Notes.TABLE_NAME,  // The database table name.
                    finalWhere,                // The final WHERE clause
                    whereArgs                  // The incoming where clause values.
                );
                break;

            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted.
        return count;
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#update(Uri,ContentValues,String,String[])}
     * Updates records in the database. The column names specified by the keys in the values map
     * are updated with new data specified by the values in the map. If the incoming URI matches the
     * note ID URI pattern, then the method updates the one record specified by the ID in the URI;
     * otherwise, it updates a set of records. The record or records must match the input
     * selection criteria specified by where and whereArgs.
     * If rows were updated, then listeners are notified of the change.
     *
     * @param uri The URI pattern to match and update.
     * @param values A map of column names (keys) and new values (values).
     * @param where An SQL "WHERE" clause that selects records based on their column values. If this
     * is null, then all records that match the URI pattern are selected.
     * @param whereArgs An array of selection criteria. If the "where" param contains value
     * placeholders ("?"), then each placeholder is replaced by the corresponding element in the
     * array.
     * @return The number of rows updated.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // Does the update based on the incoming URI pattern
        switch (sUriMatcher.match(uri)) {

            // If the incoming URI matches the general notes pattern, does the update based on
            // the incoming data.
            case NOTES:

                // Does the update and returns the number of rows updated.
                count = db.update(
                    NotePad.Notes.TABLE_NAME, // The database table name.
                    values,                   // A map of column names and new values to use.
                    where,                    // The where clause column names.
                    whereArgs                 // The where clause column values to select on.
                );
                break;

            // If the incoming URI matches a single note ID, does the update based on the incoming
            // data, but modifies the where clause to restrict it to the particular note ID.
            case NOTE_ID:
                // From the incoming URI, get the note ID
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);

                /*
                 * Starts creating the final WHERE clause by restricting it to the incoming
                 * note ID.
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // The ID column name
                        " = " +                                          // test for equality
                        uri.getPathSegments().                           // the incoming note ID
                            get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // If there were additional selection criteria, append them to the final WHERE
                // clause
                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }


                // Does the update and returns the number of rows updated.
                count = db.update(
                    NotePad.Notes.TABLE_NAME, // The database table name.
                    values,                   // A map of column names and new values to use.
                    finalWhere,               // The final WHERE clause to use
                                              // placeholders for whereArgs
                    whereArgs                 // The where clause column values to select on, or
                                              // null if the values are in the where argument.
                );
                break;
            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;
    }

    /**
     * A test package can call this to get a handle to the database underlying NotePadProvider,
     * so it can insert test data into the database. The test case class is responsible for
     * instantiating the provider in a test context; {@link android.test.ProviderTestCase2} does
     * this during the call to setUp()
     *
     * @return a handle to the database helper object for the provider's data.
     */
    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
      /**
     * 在表中添加字段方法 操作步骤： 1、先更改表名 2、创建新表,表名为原来的表名 3、复制数据 4、删除旧表
     *
     * @param db
     *            数据库名
     * @param newColumnArr
     *            添加的新字段的表名数组
     * @param oldTableName
     *            旧表名，在方法内部将旧表名修改为 _temp_+oldTableName
     */
    private void addColumn(SQLiteDatabase db, String[] newColumnArr,
                           String oldTableName) {

        if (db == null || newColumnArr == null || newColumnArr.length < 1
                || TextUtils.isEmpty(oldTableName)) {
            return;
        }
        Cursor cursor = db.rawQuery("select * from " + oldTableName, null);
        if (cursor == null) {
            return;
        }
        String[] oldColumnNames = cursor.getColumnNames();
        String tempTableName = "_temp_" + oldTableName;
        db.execSQL(
                "alter table " + oldTableName + " rename to " + tempTableName);
        if (oldColumnNames.length < 1) {
            return;
        }
        StringBuffer createNewTableStr = new StringBuffer();
        createNewTableStr
                .append("create table if not exists " + oldTableName + "(");
        for (int i = 0; i < oldColumnNames.length; i++) {
            if (i == 0) {
                createNewTableStr.append(oldColumnNames[i]
                        + " integer primary key autoincrement,");
            } else {
                createNewTableStr.append(oldColumnNames[i] + ",");
            }
        }
        for (int i = 0; i < newColumnArr.length; i++) {
            if (i == newColumnArr.length - 1) {
                createNewTableStr.append(newColumnArr[i] + ")");
            } else {
                createNewTableStr.append(newColumnArr[i] + ",");
            }
        }
        db.execSQL(createNewTableStr.toString());
        StringBuffer copySQLStr = new StringBuffer();
        copySQLStr.append("insert into " + oldTableName + " select *,");
        for (int i = 0; i < newColumnArr.length; i++) {
            if (i == newColumnArr.length - 1) {
                copySQLStr.append("' ' from " + tempTableName);
            } else {
                copySQLStr.append("' ',");
            }
        }
        db.execSQL(copySQLStr.toString());
        db.execSQL("drop table " + tempTableName);
        cursor.close();
    }
}
