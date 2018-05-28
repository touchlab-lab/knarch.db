package co.touchlab.knarch

import co.touchlab.knarch.db.DatabaseErrorHandler
import co.touchlab.knarch.db.sqlite.SQLiteDatabase
import co.touchlab.knarch.io.*

interface SystemContext{
    companion object {
        /**
         * File creation mode: the default mode, where the created file can only
         * be accessed by the calling application (or all applications sharing the
         * same user ID).
         * @see .MODE_WORLD_READABLE
         *
         * @see .MODE_WORLD_WRITEABLE
         */
        val MODE_PRIVATE = 0x0000
        /**
         * @see .MODE_PRIVATE
         *
         * @see .MODE_WORLD_WRITEABLE
         */
        @Deprecated("Creating world-readable files is very dangerous, and likely\n" +
                "      to cause security holes in applications.  It is strongly discouraged;\n" +
                "      instead, applications should use more formal mechanism for interactions\n" +
                "      such as {@link ContentProvider}, {@link BroadcastReceiver}, and\n" +
                "      {@link android.app.Service}.  There are no guarantees that this\n" +
                "      access mode will remain on a file, such as when it goes through a\n" +
                "      backup and restore.\n" +
                "      File creation mode: allow all other applications to have read access\n" +
                "      to the created file.\n" +
                "      ")
        val MODE_WORLD_READABLE = 0x0001
        /**
         * @see .MODE_PRIVATE
         *
         * @see .MODE_WORLD_READABLE
         */
        @Deprecated("Creating world-writable files is very dangerous, and likely\n" +
                "      to cause security holes in applications.  It is strongly discouraged;\n" +
                "      instead, applications should use more formal mechanism for interactions\n" +
                "      such as {@link ContentProvider}, {@link BroadcastReceiver}, and\n" +
                "      {@link android.app.Service}.  There are no guarantees that this\n" +
                "      access mode will remain on a file, such as when it goes through a\n" +
                "      backup and restore.\n" +
                "      File creation mode: allow all other applications to have write access\n" +
                "      to the created file.\n" +
                "      ")
        val MODE_WORLD_WRITEABLE = 0x0002
        /**
         * File creation mode: for use with [.openFileOutput], if the file
         * already exists then write data to the end of the existing file
         * instead of erasing it.
         * @see .openFileOutput
         */
        val MODE_APPEND = 0x8000


        /**
         * Database open flag: when set, the database is opened with write-ahead
         * logging enabled by default.
         *
         * @see .openOrCreateDatabase
         * @see .openOrCreateDatabase
         * @see SQLiteDatabase.enableWriteAheadLogging
         */
        val MODE_ENABLE_WRITE_AHEAD_LOGGING = 0x0008
    }

    fun getDir(folder:String, mode:Int):File

    fun getDatabasePath(databaseName:String):File

    fun openOrCreateDatabase(name:String,
                                      mode:Int, factory:SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?):SQLiteDatabase

    fun deleteDatabase(dbName:String):Boolean
}