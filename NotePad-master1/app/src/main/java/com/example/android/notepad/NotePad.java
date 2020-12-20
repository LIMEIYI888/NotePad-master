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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Note Pad content provider and its clients. A contract defines the
 * information that a client needs to access the provider as one or more data tables. A contract
 * is a public, non-extendable (final) class that contains constants defining column names and
 * URIs. A well-written client depends only on the constants in the contract.
 * 定义记事本内容提供程序与其客户端之间的协定。
 * 契约将客户端访问提供程序所需的信息定义为一个或多个数据表。
 * 协定是一个公共的、不可扩展的（final）类，它包含定义列名和uri的常量。
 * 一个写得好的客户机只依赖于合同中的常量。
 */
public final class NotePad {
    public static final String AUTHORITY = "com.google.provider.NotePad";

    // This class cannot be instantiated 无法实例化此类
    private NotePad() {
    }

    /**
     * Notes table contract  note表的契约
     */
    public static final class Notes implements BaseColumns {


        // This class cannot be instantiated
        private Notes() {}

        /**
         * The table name offered by this provider 此程序提供表名
         */
        public static final String TABLE_NAME = "notes";

        /*
         * URI definitions URI定义
         */

        /**
         * The scheme part for this provider's URI 此提供程序的URI的组成部分
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */

        /**
         * Path part for the Notes URI
         */
        private static final String PATH_NOTES = "/notes";

        /**
         * Path part for the Note ID URI
         */
        private static final String PATH_NOTE_ID = "/notes/";

        /**
         * 0-relative position of a note ID segment in the path part of a note ID URI
         * 0—注释ID段在注释ID URI的路径部分中的相对位置
         */
        public static final int NOTE_ID_PATH_POSITION = 1;

        /**
         * Path part for the Live Folder URI
         */
        private static final String PATH_LIVE_FOLDER = "/live_folders/notes";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);

        /**
         * The content URI base for a single note. Callers must
         * append a numeric note id to this Uri to retrieve a note
         * 单个便笺的内容URI基。调用者必须向这个Uri附加一个数字的注释id来检索注释
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);

        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         * 单个便笺的内容URI匹配模式，由其ID指定。使用此模式可匹配传入的URI或构造意图。
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");

        /**
         * The content Uri pattern for a notes listing for live folders
         */
        public static final Uri LIVE_FOLDER_URI
            = Uri.parse(SCHEME + AUTHORITY + PATH_LIVE_FOLDER);

        /*
         * MIME type definitions
         */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /*
         * Column definitions
         */

        /**
         * Column name for the title of the note 注释标题的列名
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";

        /**
         * Column name of the note content
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_NOTE = "note";

        /**
         * Column name for the creation timestamp
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /**
         * Column name for the modification timestamp
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
        public static final String COLUMN_NAME_BACK_COLOR = "color";
        public static final int COLOR0 = 0; //#FFFFFF
        public static final int COLOR1 = 1; //#FFFF00
        public static final int COLOR2 = 2; //#87CEEB
        public static final int COLOR3 = 3; //#7FFFD4
        public static final int COLOR4 = 4; //#FFC0CB

    }
}
