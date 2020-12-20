
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

import android.app.Activity;  //展现的窗体
import android.content.ClipData;  //表示剪贴板里的所有数据集
import android.content.ClipboardManager;  //表示一个剪贴板
import android.content.ComponentName;//组件间的交流要用Intent,然后描述一个组件需要知道组件的应用包名
// ComponentName用来封装组件的应用包名和组件的名字
import android.content.ContentResolver; //类似插头（得到数据），与插座ContentProvider（提供数据）连接就可以操作数据
import android.content.ContentValues;//类似HashTable,HashTable可以存储数据包括对象，ContentValues只能存储数据
import android.content.Context;//一个接口，提供有关应用的全局信息（指允许访问该应用的资源和类以及活动等）
import android.content.Intent;//组件之间交互的一种方式，可在当前Activity指定动作，还可在组件间传递数据
import android.content.res.Resources;//报错处理
import android.database.Cursor;//Cursor是一个数据源，指向每一条数据，查询数据的时候用
import android.graphics.Canvas;//表示屏幕上的一块区域，可在上面绘制想要的东西
import android.graphics.Paint;//绘制的画笔
import android.graphics.Rect;//坐标
import android.icu.text.SimpleDateFormat;
import android.net.Uri;//路径
import android.os.Build;
import android.os.Bundle;//用于携带数据，类似Map,可存放key-value名值对形式的值
import android.util.AttributeSet;//连接自定义控件类中的变量和对控件定义的属性
import android.util.Log;//日志工作类，用于打印日志
import android.view.Menu;//定义菜单
import android.view.MenuInflater;//实例化Menu目录下的Menu布局文件
import android.view.MenuItem;
import android.widget.EditText;//相当TextField,是可以让用户输入文本的组件，是用户和程序之间传输数据的纽带

import androidx.annotation.RequiresApi;

import java.sql.Date;

/**
 * This Activity handles "editing" a note, where editing is responding to
 * {@link Intent#ACTION_VIEW} (request to view data), edit a note
 * {@link Intent#ACTION_EDIT}, create a note {@link Intent#ACTION_INSERT}, or
 * create a new note from the current contents of the clipboard {@link Intent#ACTION_PASTE}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler}
 * or {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";

    /*
     * Creates a projection that returns the note ID and the note contents.
     */
    private static final String[] PROJECTION =
        new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    };

    // A label for the saved state of the activity  活动已保存状态的标签
    private static final String ORIGINAL_CONTENT = "origContent";

    // This Activity can be started by more than one action. Each action is represented
    // as a "state" constant
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global mutable variables 全局可变变量
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends androidx.appcompat.widget.AppCompatEditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * @param canvas The canvas on which the background is drawn.
         */
        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            int count = getLineCount();   //获得TextView的真实行数，于是调用getLineCount()方法

            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             */
            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text 获取当前文本行的基线坐标
                int baseline = getLineBounds(i, r);

                /*
                 * Draws a line in the background from the left of the rectangle to the right,
                 * at a vertical position one dip below the baseline, using the "paint" object
                 * for details.
                 * 在背景中从矩形的左侧向右绘制一条线，在基线下一个倾斜的垂直位置，使用“绘制”对象获取详细信息。
                 */
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     * 此方法在活动第一次启动时由Android调用。根据输入的意图，它决定需要什么样的编辑，然后再进行编辑。
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {//onCreate()函数是在activity初始化的时候调用的
        //子类在重写onCreate()方法的时候必须调用父类的onCreate()方法，即super.onCreate()，否则会抛出异常
        super.onCreate(savedInstanceState);

        /*
         * Creates an Intent to use when the Activity object's result is sent back to the
         * caller.
         * 创建在将活动对象的结果发送回调用者时使用的意图。
         */
        final Intent intent = getIntent();

        /*
         *  Sets up for the edit, based on the action specified for the incoming Intent.
         * 根据为传入意图指定的操作为编辑设置。
         */

        // Gets the action that triggered the intent filter for this Activity
        //获取触发此活动的意向筛选器的操作
        final String action = intent.getAction();

        // For an edit action:
        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            //设置要编辑的活动状态，并获取要编辑的数据的URI。
            mState = STATE_EDIT;
            mUri = intent.getData();

            // For an insert or paste action:
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an
            // empty record in the provider
            //设置要插入的活动状态，获取常规注释URI，并在提供程序中插入空记录
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            /*
             * If the attempt to insert the new note fails, shuts down this Activity. The
             * originating Activity receives back RESULT_CANCELED if it requested a result.
             * Logs that the insert failed.
             * 如果尝试插入新便笺失败，则关闭此活动。如果请求的活动返回请求的结果，则发起u结果日志插入失败。
             */
            if (mUri == null) {

                // Writes the log identifier, a message, and the URI that failed.
                //写入失败的日志标识符、消息和URI。
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                // Closes the activity.
                finish();
                return;
            }

            // Since the new entry was created, this sets the result to be returned
            //由于创建了新条目，因此设置要返回的结果
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        // If the action was other than EDIT or INSERT:
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        /*
         * Using the URI passed in with the triggering Intent, gets the note or notes in
         * the provider.
         * Note: This is being done on the UI thread. It will block the thread until the query
         * completes. In a sample app, going against a simple provider based on a local database,
         * the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         * 使用传入的带有触发意图的URI，获取提供程序中的一个或多个注释。
         *注意：这是在UI线程上完成的。它将阻塞线程，直到查询完成。在一个示例应用程序中，
         * 与基于本地数据库的简单提供者相反，块将是瞬间的
         */
        mCursor = managedQuery(
            mUri,         // The URI that gets multiple notes from the provider.
                          //Content Provider需要返回的资源索引
            PROJECTION,   // A projection that returns the note ID and note content for each note.
                         //一种投影，用于标识那些列需要包含在返回数据中
            null,         // No "where" clause selection criteria.
                           //类似SQL语句中Where之后的条件判断
            null,         // No "where" clause selection values.
                        ////类似SQL语句中Where之后的条件判断
            null          // Use the default sort order (modification date, descending)
                                //对返回的信息进行排序
        );

        // For a paste, initializes the data from clipboard.
        // (Must be done after mCursor is initialized.)
        if (Intent.ACTION_PASTE.equals(action)) {
            // Does the paste
            performPaste();
            // Switches the state to EDIT so the title can be modified.
            mState = STATE_EDIT;
        }

        // Sets the layout for this Activity. See res/layout/note_editor.xml  设置此活动的布局。
        setContentView(R.layout.note_editor);

        // Gets a handle to the EditText in the the layout.  获取布局中EditText的句柄。
        mText = (EditText) findViewById(R.id.note);

        /*
         * If this Activity had stopped previously, its state was written the ORIGINAL_CONTENT
         * location in the saved Instance state. This gets the state.
         * 如果此活动先前已停止，则其状态将写入保存实例状态下的原始_内容位置。
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     *当活动即将到达前台时调用此方法。当活动到达任务堆栈的顶部时，或者当它第一次启动时，就会发生这种情况。
     * Moves to the first note in the list, sets an appropriate title for the action chosen by
     * the user, puts the note contents into the TextView, and saves the original text as a
     * backup.
     * 移动到列表中的第一个注释，为用户选择的操作设置适当的标题，将注释内容放入TextView，并将原始文本保存为备份。
     */
    @Override
    protected void onResume() {//onResume()是当该activity与用户能进行交互时被执行，
        // 用户可以获得activity的焦点，能够与用户交互
        super.onResume();

        /*
         * mCursor is initialized, since onCreate() always precedes onResume for any running
         * process. This tests that it's not null, since it should always contain data.
         * mCursor已初始化，因为对于任何正在运行的进程，onCreate（）总是在onResume之前。
         * 这将测试它是否不为null，因为它应该始终包含数据。
         */
        if (mCursor != null) {
            // Requery in case something changed while paused (such as the title)
            //重新查询，以防暂停时发生变化（如标题）
            mCursor.requery();

            /* Moves to the first record. Always call moveToFirst() before accessing data in
             * a Cursor for the first time. The semantics of using a Cursor are that when it is
             * created, its internal index is pointing to a "place" immediately before the first
             * record.
             * 移动到第一个记录。在第一次访问游标中的数据之前，请始终调用moveToFirst（）。
             * 使用游标的语义是，当它被创建时，它的内部索引指向firs记录前面的一个“位置”。
             */
            mCursor.moveToFirst();

            // Modifies the window title for the Activity according to the current Activity state.
            //根据当前活动状态修改活动的窗口标题。
            if (mState == STATE_EDIT) {
                // Set the title of the Activity to include the note title
                //设置活动的标题以包括便笺标题
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            // Sets the title to "create" for inserts 将插入的标题设置为“创建”
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            /*
             * onResume() may have been called after the Activity lost focus (was paused).
             * The user was either editing or creating a note when the Activity paused.
             * The Activity should re-display the text that had been retrieved previously, but
             * it should not move the cursor. This helps the user to continue editing or entering.
             * onResume（）可能是在活动失去焦点（暂停）后调用的。当活动发生时，
             * 用户正在编辑或创建便笺暂停了。那个活动应重新显示先前检索到的文本，但不应移动光标。
             * 这有助于用户继续编辑或输入
             */

            // Gets the note text from the Cursor and puts it in the TextView, but doesn't change
            //从光标获取注释文本并将其放到TextView中，但不会更改
            // the text cursor's position.
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            // Stores the original note text, to allow the user to revert changes.
            //存储原始注释文本，以允许用户还原更改。
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        /*
         * Something is wrong. The Cursor should always contain data. Report an error in the
         * note.
         * 有点不对劲。光标应始终包含数据。报告笔记中的错误。
         */
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    /**
     * This method is called when an Activity loses focus during its normal operation, and is then
     * later on killed. The Activity has a chance to save its state so that the system can restore
     * it.
     *当活动在其正常操作期间失去焦点时调用此方法，然后在稍后被终止。活动有机会保存其状态，以便系统可以恢复它。
     * Notice that this method isn't a normal part of the Activity lifecycle. It won't be called
     * if the user simply navigates away from the Activity.
     * 请注意，此方法不是活动生命周期的正常部分。如果用户只是导航离开活动，则不会调用它。
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    /**
     * This method is called when the Activity loses focus.
     *
     * For Activity objects that edit information, onPause() may be the one place where changes are
     * saved. The Android application model is predicated on the idea that "save" and "exit" aren't
     * required actions. When users navigate away from an Activity, they shouldn't have to go back
     * to it to complete their work. The act of going away should save everything and leave the
     * Activity in a state where Android can destroy it if necessary.
     *对于编辑信息的活动对象，onPause（）可能是保存更改的位置。Android应用程序模型的基础是“保存”
     * 和“退出”不是必需的操作。当用户离开某个活动时，他们不应该返回该活动来完成他们的工作。
     * 离开的行为应该保存所有的东西，并让活动处于一种安卓可以在必要时摧毁它的状态。
     * If the user hasn't done anything, then this deletes or clears out the note, otherwise it
     * writes the user's work to the provider.
     * 如果用户没有做任何事情，那么它会删除或清除注释，否则它会将用户的工作写入提供者。
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onPause() {
        super.onPause();

        /*
         * Tests to see that the query operation didn't fail (see onCreate()). The Cursor object
         * will exist, even if no records were returned, unless the query failed because of some
         * exception or error.
         *测试查询操作是否未失败（请参见onCreate（））。游标对象将存在，即使没有返回记录，除非查询因某些异常或错误而失败。
         */
        if (mCursor != null) {

            // Get the current note text.
            String text = mText.getText().toString();
            int length = text.length();

            /*
             * If the Activity is in the midst of finishing and there is no text in the current
             * note, returns a result of CANCELED to the caller, and deletes the note. This is done
             * even if the note was being edited, the assumption being that the user wanted to
             * "clear out" (delete) the note.
             *如果活动正在完成中，并且当前便笺中没有文本，则向调用方返回CANCELED的结果，
             * 并删除该便笺。即使正在编辑注释，也要这样做，假设用户希望“清除”（删除）非注释
             */
            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

                /*
                 * Writes the edits to the provider. The note has been edited if an existing note was
                 * retrieved into the editor *or* if a new note was inserted. In the latter case,
                 * onCreate() inserted a new empty note into the provider, and it is this new note
                 * that is being edited.
                 * 将编辑写入提供程序。如果现有注释是检索到编辑器‘或’中（如果插入了新注释）。
                 * 在后一种情况下，onCreate（）在提供程序中插入了一个新的空注释，正在编辑的正是这个新注释。
                 */
            } else if (mState == STATE_EDIT) {
                // Creates a map to contain the new values for the columns
                updateNote(text, null);
            } else if (mState == STATE_INSERT) {
                updateNote(text, text);
                mState = STATE_EDIT;
          }
        }
    }

    /**
     * This method is called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *当用户第一次为此活动单击设备的菜单按钮时，将调用此方法。Android传递一个菜单对象，其中填充了项。
     * Builds the menus for editing and inserting, and adds in alternative actions that
     * registered themselves to handle the MIME types for this application.
     *生成用于编辑和插入的菜单，并添加可供选择的操作，这些操作注册它们自己来处理此应用程序的MIME类型。
     * @param menu A Menu object to which items should be added.
     * @return True to display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource 从XML资源膨胀菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        // Only add extra menu items for a saved note 仅为已保存的备忘添加额外的菜单项
        if (mState == STATE_EDIT) {
            // Append to the
            // menu items for any other activities that can do stuff with it
            // as well.  This does a query on the system for any activities that
            // implement the ALTERNATIVE_ACTION for our data, adding a menu item
            // for each one that is found.
            //附加到菜单项中，以用于其他可以处理它的活动。
            // 这将在系统上查询为我们的数据实现可选操作的任何活动，并为找到的每个活动添加一个菜单项
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Check if note has changed and enable/disable the revert option 检查便笺是否已更改并启用/禁用还原选项
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * This method is called when a menu item is selected. Android passes in the selected item.
     * The switch statement in this method calls the appropriate method to perform the action the
     * user chose.
     *选择菜单项时调用此方法。Android在选定的项目。The此方法中的switch语句调用适当的方法来执行用户选择的操作。
     * @param item The selected MenuItem
     * @return True to indicate that the item was processed, and no further work is necessary. False
     * to proceed to further processing as indicated in the MenuItem object.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case R.id.menu_save:
            String text = mText.getText().toString();
            updateNote(text, null);
            finish();
            break;
        case R.id.menu_delete:
            deleteNote();
            finish();
            break;
        case R.id.menu_revert:
            cancelNote();
            break;
            case R.id.menu_color:
                changeColor();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

//BEGIN_INCLUDE(paste)
    /**
     * A helper method that replaces the note's data with the contents of the clipboard.
     * 用剪贴板的内容替换注释数据的助手方法。
     */
    private final void changeColor() {
        Intent intent = new Intent(null,mUri);
        intent.setClass(NoteEditor.this,NoteColor.class);
        NoteEditor.this.startActivity(intent);}
    @RequiresApi(api = Build.VERSION_CODES.N)
    private final void performPaste() {

        // Gets a handle to the Clipboard Manager 获取剪贴板管理器的句柄
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        // Gets a content resolver instance
        ContentResolver cr = getContentResolver();

        // Gets the clipboard data from the clipboard
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            // Gets the first item from the clipboard data
            ClipData.Item item = clip.getItemAt(0);

            // Tries to get the item's contents as a URI pointing to a note
            Uri uri = item.getUri();

            // Tests to see that the item actually is an URI, and that the URI
            // is a content URI pointing to a provider whose MIME type is the same
            // as the MIME type supported by the Note pad provider.
            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {

                // The clipboard holds a reference to data with a note MIME type. This copies it.
                Cursor orig = cr.query(
                        uri,            // URI for the content provider
                        PROJECTION,     // Get the columns referred to in the projection
                        null,           // No selection variables
                        null,           // No selection variables, so no criteria are needed
                        null            // Use the default sort order
                );

                // If the Cursor is not null, and it contains at least one record
                // (moveToFirst() returns true), then this gets the note data from it.
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    // Closes the cursor.
                    orig.close();
                }
            }

            // If the contents of the clipboard wasn't a reference to a note, then
            // this converts whatever it is to text.
            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            // Updates the current note with the retrieved title and text.
            updateNote(text, title);
        }
    }
//END_INCLUDE(paste)

    /**
     * Replaces the current note contents with the text and title provided as arguments.
     * 将当前注释内容替换为作为参数提供的文本和标题。
     * @param text The new note contents to use.
     * @param title The new note title to use
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private final void updateNote(String text, String title) {

        // Sets up a map to contain values to be updated in the provider.
        // 设置映射以包含要在提供程序中更新的值。
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        Long now = Long.valueOf(System.currentTimeMillis());
        Date date = new Date(now);
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd ");
        String dateTime = format.format(date);
        // If the action is to insert a new note, this creates an initial title for it.
        // 如果操作要插入新注释，则会为其创建初始标题。
        if (mState == STATE_INSERT) {

            // If no title was provided as an argument, create one from the note text.
            // 如果没有提供标题作为参数，请从注释文本创建一个标题。
            if (title == null) {
  
                // Get the note's length
                int length = text.length();

                // Sets the title by getting a substring of the text that is 31 characters long
                // or the number of characters in the note plus one, whichever is smaller.
                // 通过获取文本的子字符串（长度为31个字符）或注释中的字符数加1（以较小者为准）来设置标题。
                title = text.substring(0, Math.min(30, length));
  
                // If the resulting length is more than 30 characters, chops off any
                // trailing spaces  如果结果长度超过30个字符，则去掉所有尾随空格
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            // In the values map, sets the value of the title 在值映射中，设置标题的值
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        } else if (title != null) {
            // In the values map, sets the value of the title
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        // This puts the desired notes text into the map.这会将所需的注释文本放入地图中。
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, text);
        /*
         * Updates the provider with the new values in the map. The ListView is updated
         * automatically. The provider sets this up by setting the notification URI for
         * query Cursor objects to the incoming URI. The content resolver is thus
         * automatically notified when the Cursor for the URI changes, and the UI is
         * updated.
         * 使用映射中的新值更新提供程序。列表视图将自动更新。
         * 提供程序通过将查询游标对象的通知URI设置为传入的URI来进行设置。
         * 因此，当URI的游标发生变化，并且UI被更新时，内容解析器会自动得到通知
         * Note: This is being done on the UI thread. It will block the thread until the
         * update completes. In a sample app, going against a simple provider based on a
         * local database, the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         * 注意：这是在UI线程上完成的。它将阻塞线程，直到更新完成。
         * 在一个示例应用程序中，与一个基于本地数据库的简单提供者相反，块将是暂时的，但在实际应用程序中，
         * 您应该使用android.content.AsyncQueryHandler或者android.os.AsyncTask.
         */
        getContentResolver().update(
                mUri,    // The URI for the record to update.
                values,  // The map of column names and new values to apply to them.
                // 列名和要应用于它们的新值的映射。
                null,    // No selection criteria are used, so no where columns are necessary.
                null     // No where columns are used, so no where arguments are necessary.
            );


    }

    /**
     * This helper method cancels the work done on a note.  It deletes the note if it was
     * newly created, or reverts to the original text of the note i
     * 此助手方法取消对便笺所做的工作。如果是的话，它会删除注释
     * 新创建的，或还原为注释i的原始文本
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}
