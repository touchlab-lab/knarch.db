package co.touchlab.kurgan.architecture.database.sqlite

import co.touchlab.kurgan.Log
import co.touchlab.kurgan.architecture.database.DatabaseErrorHandler

expect fun createOpenHelper(
        name:String?,
        callback:PlatformSQLiteOpenHelperCallback,
        errorHandler: DatabaseErrorHandler?):SQLiteOpenHelper

/*expect class PlatformSQLiteOpenHelper:SQLiteOpenHelper{
    var callback:PlatformSQLiteOpenHelperCallback

    override fun onCreate(db: SQLiteDatabase) {
        callback.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        callback.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        callback.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SQLiteDatabase) {
        callback.onOpen(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        callback.onConfigure(db)
    }
}*/

abstract class PlatformSQLiteOpenHelperCallback(val version:Int) {
    val TAG = "PlatformSQLiteOpenHelper"

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
    open fun onConfigure(db:SQLiteDatabase){}

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    abstract fun onCreate(db:SQLiteDatabase)

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
    abstract fun onUpgrade(db:SQLiteDatabase, oldVersion:Int, newVersion:Int)

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
    open fun onDowngrade(db:SQLiteDatabase, oldVersion:Int, newVersion:Int) {
        throw SQLiteException("Can't downgrade database from version "
                + oldVersion + " to " + newVersion)
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
    open fun onOpen(db:SQLiteDatabase) {

    }

    /**
     * The method invoked when database corruption is detected. Default implementation will
     * delete the database file.
     *
     * @param db the {@link SupportSQLiteDatabase} object representing the database on which
     *           corruption is detected.
     */
    open fun onCorruption(db:SQLiteDatabase) {
        // the following implementation is taken from {@link DefaultDatabaseErrorHandler}.

        Log.e(TAG, "Corruption reported by sqlite on database: " + db.getPath())

        // is the corruption detected even before database could be 'opened'?
        if (!db.isOpen()) {
            // database files are not even openable. delete this database file.
            // NOTE if the database has attached databases, then any of them could be corrupt.
            // and not deleting all of them could cause corrupted database file to remain and
            // make the application crash on database open operation. To avoid this problem,
            // the application should provide its own {@link DatabaseErrorHandler} impl class
            // to delete ALL files of the database (including the attached databases).
            deleteDatabase(db.getPath())
            return
        }
    }
}