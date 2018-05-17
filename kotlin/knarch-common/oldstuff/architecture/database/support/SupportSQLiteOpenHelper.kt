package co.touchlab.kurgan.architecture.database.support

import co.touchlab.kurgan.*
import co.touchlab.kurgan.architecture.Context
import co.touchlab.kurgan.architecture.DataContext

expect fun deleteDatabase(path:String):Boolean

interface SupportSQLiteOpenHelper {
    companion object {
        val TAG:String = "SupportSQLite"
    }

    /**
     * Return the name of the SQLite database being opened, as given to
     * the constructor.
     */
    fun getDatabaseName():String

    /**
     * Enables or disables the use of write-ahead logging for the database.
     *
     * Write-ahead logging cannot be used with read-only databases so the value of
     * this flag is ignored if the database is opened read-only.
     *
     * @param enabled True if write-ahead logging should be enabled, false if it
     *                should be disabled.
     * @see SupportSQLiteDatabase#enableWriteAheadLogging()
     */
    fun setWriteAheadLoggingEnabled(enabled:Boolean)

    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be opened and
     * {@link Callback#onCreate}, {@link Callback#onUpgrade} and/or {@link Callback#onOpen} will be
     * called.
     *
     * <p>Once opened successfully, the database is cached, so you can
     * call this method every time you need to write to the database.
     * (Make sure to call {@link #close} when you no longer need the database.)
     * Errors such as bad permissions or a full disk may cause this method
     * to fail, but future attempts may succeed if the problem is fixed.</p>
     *
     * <p class="caution">Database upgrade may take a long time, you
     * should not call this method from the application main thread, including
     * from android.content.ContentProvider#onCreate ContentProvider.onCreate().
     *
     * @return a read/write database object valid until {@link #close} is called
     */
    fun getWritableDatabase(): SupportSQLiteDatabase

    /**
     * Create and/or open a database.  This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only.  In that case, a read-only
     * database object will be returned.  If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     * <p class="caution">Like {@link #getWritableDatabase}, this method may
     * take a long time to return, so you should not call it from the
     * application main thread, including from
     * android.content.ContentProvider#onCreate ContentProvider.onCreate().
     *
     * @return a database object valid until {@link #getWritableDatabase}
     * or {@link #close} is called.
     */
    fun getReadableDatabase(): SupportSQLiteDatabase

    /**
     * Close any open database object.
     */
    fun close()

    /**
     * Handles various lifecycle events for the SQLite connection, similar to
     * {@link android.database.sqlite.SQLiteOpenHelper}.
     */
    abstract class Callback(val version: Int) {

        /**
         * Called when the database connection is being configured, to enable features such as
         * write-ahead logging or foreign key support.
         * <p>
         * This method is called before {@link #onCreate}, {@link #onUpgrade}, {@link #onDowngrade},
         * or {@link #onOpen} are called. It should not modify the database except to configure the
         * database connection as required.
         * </p>
         * <p>
         * This method should only call methods that configure the parameters of the database
         * connection, such as {@link SupportSQLiteDatabase#enableWriteAheadLogging}
         * {@link SupportSQLiteDatabase#setForeignKeyConstraintsEnabled},
         * {@link SupportSQLiteDatabase#setLocale},
         * {@link SupportSQLiteDatabase#setMaximumSize}, or executing PRAGMA statements.
         * </p>
         *
         * @param db The database.
         */
        open fun onConfigure(db: SupportSQLiteDatabase) {

        }

        /**
         * Called when the database is created for the first time. This is where the
         * creation of tables and the initial population of the tables should happen.
         *
         * @param db The database.
         */
        abstract fun onCreate(db: SupportSQLiteDatabase)

        /**
         * Called when the database needs to be upgraded. The implementation
         * should use this method to drop tables, add tables, or do anything else it
         * needs to upgrade to the new schema version.
         *
         * <p>
         * The SQLite ALTER TABLE documentation can be found
         * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
         * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
         * you can use ALTER TABLE to rename the old table, then create the new table and then
         * populate the new table with the contents of the old table.
         * </p><p>
         * This method executes within a transaction.  If an exception is thrown, all changes
         * will automatically be rolled back.
         * </p>
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        abstract fun onUpgrade(db: SupportSQLiteDatabase, oldVersion:Int, newVersion:Int)

        /**
         * Called when the database needs to be downgraded. This is strictly similar to
         * {@link #onUpgrade} method, but is called whenever current version is newer than requested
         * one.
         * However, this method is not abstract, so it is not mandatory for a customer to
         * implement it. If not overridden, default implementation will reject downgrade and
         * throws SQLiteException
         *
         * <p>
         * This method executes within a transaction.  If an exception is thrown, all changes
         * will automatically be rolled back.
         * </p>
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        open fun onDowngrade(db: SupportSQLiteDatabase, oldVersion:Int, newVersion:Int) {
            throw RuntimeException("Can't downgrade database from version "
                    + oldVersion + " to " + newVersion);
        }

        /**
         * Called when the database has been opened.  The implementation
         * should check {@link SupportSQLiteDatabase#isReadOnly} before updating the
         * database.
         * <p>
         * This method is called after the database connection has been configured
         * and after the database schema has been created, upgraded or downgraded as necessary.
         * If the database connection must be configured in some way before the schema
         * is created, upgraded, or downgraded, do it in {@link #onConfigure} instead.
         * </p>
         *
         * @param db The database.
         */
        open fun onOpen(db: SupportSQLiteDatabase) {

        }

        /**
         * The method invoked when database corruption is detected. Default implementation will
         * delete the database file.
         *
         * @param db the {@link SupportSQLiteDatabase} object representing the database on which
         *           corruption is detected.
         */
        open fun onCorruption(db: SupportSQLiteDatabase) {
            // the following implementation is taken from {@link DefaultDatabaseErrorHandler}.

            Log.e(TAG, "Corruption reported by sqlite on database: " + db.getPath());
            // is the corruption detected even before database could be 'opened'?
            if (!db.isOpen()) {
                // database files are not even openable. delete this database file.
                // NOTE if the database has attached databases, then any of them could be corrupt.
                // and not deleting all of them could cause corrupted database file to remain and
                // make the application crash on database open operation. To avoid this problem,
                // the application should provide its own {@link DatabaseErrorHandler} impl class
                // to delete ALL files of the database (including the attached databases).
                deleteDatabaseFile(db.getPath())
                return
            }
        }

        private fun deleteDatabaseFile(fileName:String) {
            if (fileName.isNullOrEmpty() || fileName.equals(":memory:", true)) {
                return
            }
            Log.w(TAG, "deleting the database file: $fileName");
            try {
                deleteDatabase(fileName)
            } catch (e:Exception) {
            /* print warning and ignore exception */
                Log.w(TAG, "delete failed: ", e)
            }
        }
    }

    /**
     * The configuration to create an SQLite open helper object using {@link Factory}.
     */
    data class Configuration(val context: Context, val name: String?, val callback: Callback)


    /**
     * Factory class to create instances of {@link SupportSQLiteOpenHelper} using
     * {@link Configuration}.
     */
    interface Factory {
        /**
         * Creates an instance of {@link SupportSQLiteOpenHelper} using the given configuration.
         *
         * @param configuration The configuration to use while creating the open helper.
         *
         * @return A SupportSQLiteOpenHelper which can be used to open a database.
         */
        fun create(configuration: Configuration): SupportSQLiteOpenHelper
    }

}