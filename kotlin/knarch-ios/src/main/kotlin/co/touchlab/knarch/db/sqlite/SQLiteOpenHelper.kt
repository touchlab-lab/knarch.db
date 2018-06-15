package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.Log
import co.touchlab.knarch.SystemContext
import co.touchlab.knarch.db.DatabaseErrorHandler
import platform.Foundation.*

/**
 * A helper class to manage database creation and version management.
 *
 * For Kotlin/Native, this class is *not* thread safe, and as such will probably get a slight redesign in the future.
 * Calls to get*Database may use coroutines to avoid blocking the main thread, but this class itself will likely live
 * in main thread code.
 *
 * SQLiteDatabase and below are thread safe and can be passed around.
 *
 * @see <a href="https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper">SQLiteOpenHelper</a>
 */
abstract class SQLiteOpenHelper(private val mContext: SystemContext,
                                private val databaseName: String?,
                                private val mFactory: SQLiteDatabase.CursorFactory? = null,
                                private val mNewVersion: Int,
                                private val mErrorHandler: DatabaseErrorHandler? = null) {

    private val createLock = NSRecursiveLock()

    private fun getData():HelperData = getHelperInfo(nativeInfoId)

    private fun putData(data:HelperData?){
        putHelperInfo(nativeInfoId, data)
    }

    private val nativeInfoId = nextHelperInfoId()

    init {
        if (mNewVersion < 1) throw IllegalArgumentException("Version must be >= 1, was $mNewVersion")
        putData(HelperData())
    }

    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be opened and
     * {@link #onCreate}, {@link #onUpgrade} and/or {@link #onOpen} will be
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
     * from {@link android.content.ContentProvider#onCreate ContentProvider.onCreate()}.
     *
     * @throws SQLiteException if the database cannot be opened for writing
     * @return a read/write database object valid until {@link #close} is called
     */
    fun getWritableDatabase(): SQLiteDatabase {
        createLock.lock()
        try {
            return getDatabaseLocked(true)
        } finally {
            createLock.unlock()
        }
    }

    /**
     * Create and/or open a database. This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only. In that case, a read-only
     * database object will be returned. If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     * <p class="caution">Like {@link #getWritableDatabase}, this method may
     * take a long time to return, so you should not call it from the
     * application main thread, including from
     * {@link android.content.ContentProvider#onCreate ContentProvider.onCreate()}.
     *
     * @throws SQLiteException if the database cannot be opened
     * @return a database object valid until {@link #getWritableDatabase}
     * or {@link #close} is called.
     */
    fun getReadableDatabase(): SQLiteDatabase {
        createLock.lock()
        try {
            return getDatabaseLocked(false)
        } finally {
            createLock.unlock()
        }
    }

    /**
     * Enables or disables the use of write-ahead logging for the database.
     *
     * Write-ahead logging cannot be used with read-only databases so the value of
     * this flag is ignored if the database is opened read-only.
     *
     * @param enabled True if write-ahead logging should be enabled, false if it
     * should be disabled.
     *
     * @see SQLiteDatabase#enableWriteAheadLogging()
     */
    fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        createLock.lock()
        try {
            setWriteAheadLoggingEnabledLocked(enabled)
        } finally {
            createLock.unlock()
        }
    }

    private fun setWriteAheadLoggingEnabledLocked(enabled: Boolean) {
        val data = getData()
        if (data.mEnableWriteAheadLogging != enabled) {
            val db = data.mDatabase
            if (db != null && db.isOpen() && !db.isReadOnly()) {
                if (enabled) {
                    db.enableWriteAheadLogging()
                } else {
                    db.disableWriteAheadLogging()
                }
            }
            val dataCopy = data.copy(mEnableWriteAheadLogging = enabled)
            putData(dataCopy)
        }
    }

    /**
     * This is fairly complex. We should review the possibility of not supporting readonly at all, but for
     * now we'll leave as is.
     */
    private fun getDatabaseLocked(writable: Boolean): SQLiteDatabase {
        var data = getData()
        if (data.mDatabase != null) {
            if (!data.mDatabase!!.isOpen()) {
                // Darn! The user closed the database by calling mDatabase.close().
                data = data.copy(mDatabase = null)
                putData(data)
            } else if (!writable || !data.mDatabase!!.isReadOnly()) {
                // The database is already open for business.
                return data.mDatabase!!
            }
        }
        if (data.mIsInitializing) {
            throw IllegalStateException("getDatabase called recursively")
        }
        var db = data.mDatabase
        try {
            data = data.copy(mIsInitializing = true)

            if (db != null) {
                if (writable && db.isReadOnly()) {
                    db.reopenReadWrite()
                }
            } else if (databaseName == null) {
                db = SQLiteDatabase.create(null)
            } else {
                try {
                    db = mContext.openOrCreateDatabase(
                            databaseName,
                            if (data.mEnableWriteAheadLogging) {
                                SystemContext.MODE_ENABLE_WRITE_AHEAD_LOGGING
                            } else {
                                0
                            },
                            mFactory, mErrorHandler)
                } catch (ex: SQLiteException) {
                    if (writable) {
                        throw ex
                    }
                    Log.e(TAG, ("Couldn't open " + databaseName
                            + " for writing (will try read-only):"), ex)
                    val path = mContext.getDatabasePath(databaseName).path
                    db = SQLiteDatabase.openDatabase(path, mFactory,
                            SQLiteDatabase.OPEN_READONLY, mErrorHandler)
                }
            }
            onConfigure(db!!)
            val version = db.getVersion()
            if (version != mNewVersion) {
                if (db.isReadOnly()) {
                    throw SQLiteException(("Can't upgrade read-only database from version " +
                            db.getVersion() + " to " + mNewVersion + ": " + databaseName))
                }
                db.beginTransaction()
                try {
                    if (version == 0) {
                        onCreate(db)
                    } else {
                        if (version > mNewVersion) {
                            onDowngrade(db, version, mNewVersion)
                        } else {
                            onUpgrade(db, version, mNewVersion)
                        }
                    }
                    db.setVersion(mNewVersion)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            onOpen(db)
            if (db.isReadOnly()) {
                Log.w(TAG, "Opened $databaseName in read-only mode")
            }
            data = data.copy(mDatabase = db)
            return db
        } finally {
            data = data.copy(mIsInitializing = false)

            if (db != null && db != data.mDatabase) {
                db.close()
            }

            putData(data)
        }
    }

    /**
     * Close any open database object.
     */
    fun close() {
        createLock.lock()
        try {
            val data = getData()
            if (data.mIsInitializing) throw IllegalStateException("Closed during initialization")
            if (data.mDatabase != null && data.mDatabase.isOpen()) {
                data.mDatabase.close()
                putData(data.copy(mDatabase = null))
            }
        } finally {
            createLock.unlock()
        }
    }

    /**
     * Called when the database connection is being configured, to enable features
     * such as write-ahead logging or foreign key support.
     * <p>
     * This method is called before {@link #onCreate}, {@link #onUpgrade},
     * {@link #onDowngrade}, or {@link #onOpen} are called. It should not modify
     * the database except to configure the database connection as required.
     * </p><p>
     * This method should only call methods that configure the parameters of the
     * database connection, such as {@link SQLiteDatabase#enableWriteAheadLogging}
     * {@link SQLiteDatabase#setForeignKeyConstraintsEnabled},
     * {@link SQLiteDatabase#setLocale}, {@link SQLiteDatabase#setMaximumSize},
     * or executing PRAGMA statements.
     * </p>
     *
     * @param db The database.
     */
    open fun onConfigure(db: SQLiteDatabase) {}

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    abstract fun onCreate(db: SQLiteDatabase)

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
     * This method executes within a transaction. If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    abstract fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    /**
     * Called when the database needs to be downgraded. This is strictly similar to
     * {@link #onUpgrade} method, but is called whenever current version is newer than requested one.
     * However, this method is not abstract, so it is not mandatory for a customer to
     * implement it. If not overridden, default implementation will reject downgrade and
     * throws SQLiteException
     *
     * <p>
     * This method executes within a transaction. If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    open fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw SQLiteException(("Can't downgrade database from version " +
                oldVersion + " to " + newVersion))
    }

    /**
     * Called when the database has been opened. The implementation
     * should check {@link SQLiteDatabase#isReadOnly} before updating the
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
    open fun onOpen(db: SQLiteDatabase) {}

    companion object {
        private val TAG = "SQLiteOpenHelper"
    }
}

data class HelperData(val mDatabase: SQLiteDatabase? = null,
                      val mIsInitializing: Boolean = false,
                      val mEnableWriteAheadLogging: Boolean = false)


@SymbolName("SQLiteSupport_nextHelperInfoId")
private external fun nextHelperInfoId():Int

@SymbolName("SQLiteSupport_getHelperInfo")
private external fun getHelperInfo(dataId:Int):HelperData

@SymbolName("SQLiteSupport_putHelperInfo")
private external fun putHelperInfo(dataId:Int, data:HelperData?)
