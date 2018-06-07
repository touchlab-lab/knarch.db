package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import platform.Foundation.*
import konan.worker.*

class SQLiteDatabase private constructor(path: String, openFlags: Int, cursorFactory: CursorFactory?, errorHandler: DatabaseErrorHandler?)  {

    companion object {
        val CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY = 1 shl 1
        val CONNECTION_FLAG_READ_ONLY = 1 shl 0
        val TAG = "SQLiteDatabase";
        val EVENT_DB_CORRUPT = 75004
        /**
         * When a constraint violation occurs, an immediate ROLLBACK occurs,
         * thus ending the current transaction, and the command aborts with a
         * return code of SQLITE_CONSTRAINT. If no transaction is active
         * (other than the implied transaction that is created on every command)
         * then this algorithm works the same as ABORT.
         */
        val CONFLICT_ROLLBACK = 1;

        /**
         * When a constraint violation occurs,no ROLLBACK is executed
         * so changes from prior commands within the same transaction
         * are preserved. This is the default behavior.
         */
        val CONFLICT_ABORT = 2;

        /**
         * When a constraint violation occurs, the command aborts with a return
         * code SQLITE_CONSTRAINT. But any changes to the database that
         * the command made prior to encountering the constraint violation
         * are preserved and are not backed out.
         */
        val CONFLICT_FAIL = 3;

        /**
         * When a constraint violation occurs, the one row that contains
         * the constraint violation is not inserted or changed.
         * But the command continues executing normally. Other rows before and
         * after the row that contained the constraint violation continue to be
         * inserted or updated normally. No error is returned.
         */
        val CONFLICT_IGNORE = 4;

        /**
         * When a UNIQUE constraint violation occurs, the pre-existing rows that
         * are causing the constraint violation are removed prior to inserting
         * or updating the current row. Thus the insert or update always occurs.
         * The command continues executing normally. No error is returned.
         * If a NOT NULL constraint violation occurs, the NULL value is replaced
         * by the default value for that column. If the column has no default
         * value, then the ABORT algorithm is used. If a CHECK constraint
         * violation occurs then the IGNORE algorithm is used. When this conflict
         * resolution strategy deletes rows in order to satisfy a constraint,
         * it does not invoke delete triggers on those rows.
         * This behavior might change in a future release.
         */
        val CONFLICT_REPLACE = 5;

        /**
         * Use the following when no conflict action is specified.
         */
        val CONFLICT_NONE = 0;

        val CONFLICT_VALUES = arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")

        /**
         * Maximum Length Of A LIKE Or GLOB Pattern
         * The pattern matching algorithm used in the default LIKE and GLOB implementation
         * of SQLite can exhibit O(N^2) performance (where N is the number of characters in
         * the pattern) for certain pathological cases. To avoid denial-of-service attacks
         * the length of the LIKE or GLOB pattern is limited to SQLITE_MAX_LIKE_PATTERN_LENGTH bytes.
         * The default value of this limit is 50000. A modern workstation can evaluate
         * even a pathological LIKE or GLOB pattern of 50000 bytes relatively quickly.
         * The denial of service problem only comes into play when the pattern length gets
         * into millions of bytes. Nevertheless, since most useful LIKE or GLOB patterns
         * are at most a few dozen bytes in length, paranoid application developers may
         * want to reduce this parameter to something in the range of a few hundred
         * if they know that external users are able to generate arbitrary patterns.
         */
        val SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;

        /**
         * Open flag: Flag for {@link #openDatabase} to open the database for reading and writing.
         * If the disk is full, this may fail even before you actually write anything.
         *
         * {@more} Note that the value of this flag is 0, so it is the default.
         */
        val OPEN_READWRITE = 0x00000000;          // update native code if changing

        /**
         * Open flag: Flag for {@link #openDatabase} to open the database for reading only.
         * This is the only reliable way to open a database if the disk may be full.
         */
        val OPEN_READONLY = 0x00000001;           // update native code if changing

        val OPEN_READ_MASK = 0x00000001;         // update native code if changing

        /**
         * Open flag: Flag for {@link #openDatabase} to open the database without support for
         * localized collators.
         *
         * {@more} This causes the collator <code>LOCALIZED</code> not to be created.
         * You must be consistent when using this flag to use the setting the database was
         * created with.  If this is set, {@link #setLocale} will do nothing.
         */
        val NO_LOCALIZED_COLLATORS = 0x00000010;  // update native code if changing

        /**
         * Open flag: Flag for {@link #openDatabase} to create the database file if it does not
         * already exist.
         */
        val CREATE_IF_NECESSARY = 0x10000000;     // update native code if changing

        /**
         * Open flag: Flag for {@link #openDatabase} to open the database file with
         * write-ahead logging enabled by default.  Using this flag is more efficient
         * than calling {@link #enableWriteAheadLogging}.
         *
         * Write-ahead logging cannot be used with read-only databases so the value of
         * this flag is ignored if the database is opened read-only.
         *
         * @see #enableWriteAheadLogging
         */
        val ENABLE_WRITE_AHEAD_LOGGING = 0x20000000;

        /**
         * Absolute max value that can be set by {@link #setMaxSqlCacheSize(int)}.
         *
         * Each prepared-statement is between 1K - 6K, depending on the complexity of the
         * SQL statement & schema.  A large SQL cache may use a significant amount of memory.
         */
        val MAX_SQL_CACHE_SIZE = 100;

        /**
         * Attempts to release memory that SQLite holds but does not require to
         * operate properly. Typically this memory will come from the page cache.
         *
         * @return the number of bytes actually released
         */
        fun releaseMemory(): Int {
            return SQLiteGlobal.releaseMemory()
        }

        /**
         * Open the database according to the flags {@link #OPEN_READWRITE}
         * {@link #OPEN_READONLY} {@link #CREATE_IF_NECESSARY} and/or {@link #NO_LOCALIZED_COLLATORS}.
         *
         * <p>Sets the locale of the database to the  the system's current locale.
         * Call {@link #setLocale} if you would like something else.</p>
         *
         * <p>Accepts input param: a concrete instance of {@link DatabaseErrorHandler} to be
         * used to handle corruption when sqlite reports database corruption.</p>
         *
         * @param path to database file to open and/or create
         * @param factory an optional factory class that is called to instantiate a
         *            cursor when query is called, or null for default
         * @param flags to control database access mode
         * @param errorHandler the {@link DatabaseErrorHandler} obj to be used to handle corruption
         * when sqlite reports database corruption
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        fun openDatabase(path:String , factory:CursorFactory?, flags:Int,
                         errorHandler:DatabaseErrorHandler? = null):SQLiteDatabase {
            val db = SQLiteDatabase(path, flags, factory, errorHandler);
            db.open()
            return db
        }

        /**
         * Equivalent to openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler).
         * Kotlin - probably remove one of these
         */
        fun openOrCreateDatabase(path:String, factory:CursorFactory?, errorHandler:DatabaseErrorHandler? = null):SQLiteDatabase {
            return openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler);
        }

        /**
         * Deletes a database including its journal file and other auxiliary files
         * that may have been created by the database engine.
         *
         * @param file The database file path.
         * @return True if the database was successfully deleted.
         */
        fun deleteDatabase(file:File):Boolean {
            var deleted = false
            deleted = deleted or file.delete()
            deleted = deleted or File(file.getPath() + "-journal").delete()
            deleted = deleted or File(file.getPath() + "-shm").delete()
            deleted = deleted or File(file.getPath() + "-wal").delete()

            //TODO: Implement file list
            val dir = file.getParentFile()
            if (dir != null)
            {
                val prefix = file.getName() + "-mj"
                val files = dir.listFiles(object:FileFilter {
                    override fun accept(candidate:File):Boolean {
                        return candidate.getName().startsWith(prefix)
                    }
                })
                if (files != null)
                {
                    for (masterJournal in files)
                    {
                        deleted = deleted or masterJournal.delete()
                    }
                }
            }
            return deleted
        }
        /*public static boolean deleteDatabase(File file) {
            if (file == null) {
                throw new IllegalArgumentException("file must not be null");
            }

            boolean deleted = false;
            deleted |= file.delete();
            deleted |= new File(file.getPath() + "-journal").delete();
            deleted |= new File(file.getPath() + "-shm").delete();
            deleted |= new File(file.getPath() + "-wal").delete();

            File dir = file.getParentFile();
            if (dir != null) {
                final String prefix = file.getName() + "-mj";
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File candidate) {
                        return candidate.getName().startsWith(prefix);
                    }
                });
                if (files != null) {
                    for (File masterJournal : files) {
                        deleted |= masterJournal.delete();
                    }
                }
            }
            return deleted;
        }*/

        /**
         * Finds the name of the first table, which is editable.
         *
         * @param tables a list of tables
         * @return the first table listed
         */
        fun findEditTable(tables: String): String {
            if (!tables.isEmpty()) {
                // find the first word terminated by either a space or a comma
                val spacepos = tables.indexOf(' ')
                val commapos = tables.indexOf(',')

                if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
                    return tables.substring(0, spacepos)
                } else if (commapos > 0 && (commapos < spacepos || spacepos < 0)) {
                    return tables.substring(0, commapos)
                }
                return tables
            } else {
                throw IllegalStateException("Invalid tables")
            }
        }


        /**
         * Create a memory backed SQLite database.  Its contents will be destroyed
         * when the database is closed.
         *
         * <p>Sets the locale of the database to the  the system's current locale.
         * Call {@link #setLocale} if you would like something else.</p>
         *
         * @param factory an optional factory class that is called to instantiate a
         *            cursor when query is called
         * @return a SQLiteDatabase object, or null if the database can't be created
         */
        fun create(factory:CursorFactory?):SQLiteDatabase {
            // This is a magic string with special meaning for SQLite.
            return openDatabase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH,
                    factory, CREATE_IF_NECESSARY);
        }
    }

    private val sqliteSession = SQLiteSessionStateAtomic(SQLiteDatabaseConfiguration(path, openFlags))

    // The optional factory to use when creating new Cursors.  May be null.
    // INVARIANT: Immutable.
    private val mCursorFactory:CursorFactory? = cursorFactory

    // Error handler to be used when SQLite returns corruption errors.
    // INVARIANT: Immutable.
    private val mErrorHandler:DatabaseErrorHandler = errorHandler ?: DefaultDatabaseErrorHandler()

    /*override fun onAllReferencesReleased() {
        dispose(false);
    }*/

    private fun dispose(finalized:Boolean) {
        //TODO: close session?
        forceClose()
    }

    /**
     * Gets a label to use when describing the database in log messages.
     * @return The label.
     */
    fun getLabel() = sqliteSession.dbLabel()

    /**
     * Sends a corruption message to the database error handler.
     */
    fun onCorruption() {
        mErrorHandler.onCorruption(this)
    }

    /**
     * Gets the {@link SQLiteSession} that belongs to this thread for this database.
     * Once a thread has obtained a session, it will continue to obtain the same
     * session even after the database has been closed (although the session will not
     * be usable).  However, a thread that does not already have a session cannot
     * obtain one after the database has been closed.
     *
     * The idea is that threads that have active connections to the database may still
     * have work to complete even after the call to {@link #close}.  Active database
     * connections are not actually disposed until they are released by the threads
     * that own them.
     *
     * @return The session, never null.
     *
     * @throws IllegalStateException if the thread does not yet have a session and
     * the database is not open.
     */
    fun getThreadSession():SQLiteSessionStateAtomic {
//        kotlin.assert(sqliteSession != null, {"Must call open before trying to use db"})
        if(!sqliteSession.hasConnection())
            openInner()
        return sqliteSession
    }

    /**
     * Gets default connection flags that are appropriate for this thread, taking into
     * account whether the thread is acting on behalf of the UI.
     *
     * @param readOnly True if the connection should be read-only.
     * @return The connection flags.
     */
    fun getThreadDefaultConnectionFlags(readOnly:Boolean):Int = if(readOnly){CONNECTION_FLAG_READ_ONLY}
    else{CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY}



    /**
     * Begins a transaction in EXCLUSIVE mode.
     * <p>
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     * </p>
     * <p>Here is the standard idiom for transactions:
     *
     * <pre>
     *   db.beginTransaction();
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     */
    fun beginTransaction() {
        beginTransaction(null /* transactionStatusCallback */, true);
    }

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     * <p>
     * Here is the standard idiom for transactions:
     *
     * <pre>
     *   db.beginTransactionNonExclusive();
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     */
    fun beginTransactionNonExclusive() {
        beginTransaction(null /* transactionStatusCallback */, false);
    }

    /**
     * Begins a transaction in EXCLUSIVE mode.
     * <p>
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     * </p>
     * <p>Here is the standard idiom for transactions:
     *
     * <pre>
     *   db.beginTransactionWithListener(listener);
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     * commits, or is rolled back, either explicitly or by a call to
     * {@link #yieldIfContendedSafely}.
     */
    fun beginTransactionWithListener(transactionListener:SQLiteTransactionListener) {
        beginTransaction(transactionListener, true);
    }

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     * <p>
     * Here is the standard idiom for transactions:
     *
     * <pre>
     *   db.beginTransactionWithListenerNonExclusive(listener);
     *   try {
     *     ...
     *     db.setTransactionSuccessful();
     *   } finally {
     *     db.endTransaction();
     *   }
     * </pre>
     *
     * @param transactionListener listener that should be notified when the
     *            transaction begins, commits, or is rolled back, either
     *            explicitly or by a call to {@link #yieldIfContendedSafely}.
     */
    fun beginTransactionWithListenerNonExclusive(transactionListener:SQLiteTransactionListener) {
        beginTransaction(transactionListener, false);
    }

    private fun beginTransaction(transactionListener:SQLiteTransactionListener?,
                                 exclusive:Boolean) {
        acquireReference();
        try {
            getThreadSession().beginTransaction(
                    if(exclusive){SQLiteSession.TRANSACTION_MODE_EXCLUSIVE}
                    else{
                        SQLiteSession.TRANSACTION_MODE_IMMEDIATE}
                    ,
                    transactionListener,
                    getThreadDefaultConnectionFlags(false /*readOnly*/))
        } finally {
            releaseReference()
        }
    }

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    fun endTransaction() {
        acquireReference();
        try {
            getThreadSession().endTransaction();
        } finally {
            releaseReference();
        }
    }

    /**
     * Marks the current transaction as successful. Do not do any more database work between
     * calling this and calling endTransaction. Do as little non-database work as possible in that
     * situation too. If any errors are encountered between this and endTransaction the transaction
     * will still be committed.
     *
     * @throws IllegalStateException if the current thread is not in a transaction or the
     * transaction is already marked as successful.
     */
    fun setTransactionSuccessful() {
        acquireReference();
        try {
            getThreadSession().setTransactionSuccessful();
        } finally {
            releaseReference();
        }
    }

    /**
     * Returns true if the current thread has a transaction pending.
     *
     * @return True if the current thread is in a transaction.
     */
    fun inTransaction():Boolean {
        acquireReference();
        try {
            return getThreadSession().hasTransaction();
        } finally {
            releaseReference();
        }
    }

    /**
     * Returns true if the current thread is holding an active connection to the database.
     * <p>
     * The name of this method comes from a time when having an active connection
     * to the database meant that the thread was holding an actual lock on the
     * database.  Nowadays, there is no longer a true "database lock" although threads
     * may block if they cannot acquire a database connection to perform a
     * particular operation.
     * </p>
     *
     * @return True if the current thread is holding an active connection to the database.
     */
    fun isDbLockedByCurrentThread():Boolean {
        acquireReference();
        try {
            return getThreadSession().hasConnection();
        } finally {
            releaseReference();
        }
    }

    /**
     * Reopens the database in read-write mode.
     * If the database is already read-write, does nothing.
     *
     * @throws SQLiteException if the database could not be reopened as requested, in which
     * case it remains open in read only mode.
     * @throws IllegalStateException if the database is not open.
     *
     * @see #isReadOnly()
     * @hide
     */
    fun reopenReadWrite() {
        throwIfNotOpenLocked();

        if (!isReadOnlyLocked()) {
            return; // nothing to do
        }

//        TODO("Need to figure out pools")
        // Reopen the database in read-write mode.

        sqliteSession.dbConfigUpdate { conf ->
            conf!!.copy(openFlags = (conf.openFlags.and(OPEN_READ_MASK.inv())).or(OPEN_READWRITE))
        }
        forceClose()
        open()
    }

    fun forceClose(){
        sqliteSession.closeConnection()
    }

    fun open() {
        try {
            try {
                openInner();
            } catch (ex:SQLiteDatabaseCorruptException) {
                onCorruption();
                openInner();
            }
        } catch (ex:SQLiteException) {
            Log.e(TAG, "Failed to open database '" + getLabel() + "'.", ex);
            close();
            throw ex;
        }
    }

    fun close(){
        forceClose()
    }

    fun openInner() {
        sqliteSession.openConnection()
    }

    /**
     * Gets the database version.
     *
     * @return the database version
     */
    fun getVersion():Int = DatabaseUtils.longForQuery(this, "PRAGMA user_version;", null).toInt()

    /**
     * Sets the database version.
     *
     * @param version the new database version
     */
    fun setVersion(version:Int) {
        execSQL("PRAGMA user_version = $version");
    }

    /**
     * Returns the maximum size the database may grow to.
     *
     * @return the new maximum database size
     */
    fun getMaximumSize():Long {
        val pageCount = DatabaseUtils.longForQuery(this, "PRAGMA max_page_count;", null);
        return pageCount * getPageSize();
    }

    /**
     * Sets the maximum size the database will grow to. The maximum size cannot
     * be set below the current size.
     *
     * @param numBytes the maximum database size, in bytes
     * @return the new maximum database size
     */
    fun setMaximumSize(numBytes:Long):Long {
        val pageSize = getPageSize();
        var numPages = numBytes / pageSize;
        // If numBytes isn't a multiple of pageSize, bump up a page
        if ((numBytes % pageSize) != 0L) {
            numPages++
        }
        val newPageCount = DatabaseUtils.longForQuery(this, "PRAGMA max_page_count = $numPages", null)
        return newPageCount * pageSize
    }

    /**
     * Returns the current database page size, in bytes.
     *
     * @return the database page size, in bytes
     */
    fun getPageSize():Long {
        return DatabaseUtils.longForQuery(this, "PRAGMA page_size;", null);
    }

    /**
     * Sets the database page size. The page size must be a power of two. This
     * method does not work if any data has been written to the database file,
     * and must be called right after the database has been created.
     *
     * @param numBytes the database page size, in bytes
     */
    fun setPageSize(numBytes:Long) {
        execSQL("PRAGMA page_size = $numBytes");
    }

    /**
     * Compiles an SQL statement into a reusable pre-compiled statement object.
     * The parameters are identical to {@link #execSQL(String)}. You may put ?s in the
     * statement and fill in those values with {@link SQLiteProgram#bindString}
     * and {@link SQLiteProgram#bindLong} each time you want to run the
     * statement. Statements may not return result sets larger than 1x1.
     *<p>
     * No two threads should be using the same {@link SQLiteStatement} at the same time.
     *
     * @param sql The raw SQL statement, may contain ? for unknown values to be
     *            bound later.
     * @return A pre-compiled {@link SQLiteStatement} object. Note that
     * {@link SQLiteStatement}s are not synchronized, see the documentation for more details.
     */
    fun compileStatement(sql:String):SQLiteStatement {
        acquireReference()
        try {
            return SQLiteStatement(this, sql, null);
        } finally {
            releaseReference()
        }
    }

    /**
     * Query the given URL, returning a {@link Cursor} over the result set.
     *
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in order that they
     *         appear in the selection. The values will be bound as Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    fun query(distinct:Boolean, table:String, columns:Array<String>?, selection:String?,
              selectionArgs:Array<String>?, groupBy:String?, having:String?,
              orderBy:String?, limit:String?):Cursor {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs,
                groupBy, having, orderBy, limit);
    }

    /**
     * Query the given URL, returning a {@link Cursor} over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in order that they
     *         appear in the selection. The values will be bound as Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    fun queryWithFactory(cursorFactory:CursorFactory?,
                         distinct:Boolean, table:String, columns:Array<String>?, selection:String?,
                         selectionArgs:Array<String>?, groupBy:String?, having:String?,
                         orderBy:String?, limit:String?):Cursor {
        acquireReference();
        try {
            val sql = SQLiteQueryBuilder.buildQueryString(
                    distinct, table, columns, selection, groupBy, having, orderBy, limit);

            return rawQueryWithFactory(cursorFactory, sql, selectionArgs,
                    findEditTable(table));
        } finally {
            releaseReference();
        }
    }

    /**
     * Query the given table, returning a {@link Cursor} over the result set.
     *
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in order that they
     *         appear in the selection. The values will be bound as Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    fun query(table:String, columns:Array<String>?, selection:String?,
              selectionArgs:Array<String>?, groupBy:String?, having:String?,
              orderBy:String?):Cursor {

        return query(false, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, null /* limit */);
    }

    /**
     * Query the given table, returning a {@link Cursor} over the result set.
     *
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in order that they
     *         appear in the selection. The values will be bound as Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    fun query(table:String, columns:Array<String>?, selection:String?,
              selectionArgs:Array<String>?, groupBy:String?, having:String?,
              orderBy:String?,limit:String?) :Cursor {

        return query(false, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit);
    }

    /**
     * Runs the provided SQL and returns a {@link Cursor} over the result set.
     *
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     *     which will be replaced by the values from selectionArgs. The
     *     values will be bound as Strings.
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     */
    fun rawQuery(sql:String, selectionArgs:Array<String>?):Cursor {
        return rawQueryWithFactory(null, sql, selectionArgs, null);
    }

    /**
     * Runs the provided SQL and returns a cursor over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     *     which will be replaced by the values from selectionArgs. The
     *     values will be bound as Strings.
     * @param editTable the name of the first table, which is editable
     * @return A {@link Cursor} object, which is positioned before the first entry. Note that
     * {@link Cursor}s are not synchronized, see the documentation for more details.
     */
    fun rawQueryWithFactory(
            cursorFactory:CursorFactory?, sql:String, selectionArgs:Array<String>?,
            editTable:String?):Cursor {
        acquireReference();
        try {
            val driver:SQLiteCursorDriver = SQLiteDirectCursorDriver(this, sql, editTable);
            return driver.query(cursorFactory ?: mCursorFactory, selectionArgs);
        } finally {
            releaseReference();
        }
    }


    /**
     * Convenience method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>values</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>values</code> is empty.
     * @param values this map contains the initial column values for the
     *            row. The keys should be the column names and the values the
     *            column values
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    fun insert(table:String, nullColumnHack:String?, values:ContentValues?):Long {
        try {
            return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
        } catch (e:SQLException) {
            Log.e(TAG, "Error inserting $values", e);
            return -1;
        }
    }

    /**
     * Convenience method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>values</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>values</code> is empty.
     * @param values this map contains the initial column values for the
     *            row. The keys should be the column names and the values the
     *            column values
     * @throws SQLException
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    fun insertOrThrow(table:String, nullColumnHack:String?, values:ContentValues?):Long {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    /**
     * Convenience method for replacing a row in the database.
     *
     * @param table the table in which to replace the row
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>initialValues</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>initialValues</code> is empty.
     * @param initialValues this map contains the initial column values for
     *   the row.
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    fun replace(table:String, nullColumnHack:String?, initialValues:ContentValues):Long {
        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues,
                    CONFLICT_REPLACE);
        } catch (e:SQLException) {
            Log.e(TAG, "Error inserting $initialValues", e);
            return -1;
        }
    }

    /**
     * Convenience method for replacing a row in the database.
     *
     * @param table the table in which to replace the row
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>initialValues</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>initialValues</code> is empty.
     * @param initialValues this map contains the initial column values for
     *   the row. The key
     * @throws SQLException
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    fun replaceOrThrow(table:String, nullColumnHack:String?,
                       initialValues:ContentValues): Long {
        return insertWithOnConflict(table, nullColumnHack, initialValues,
                CONFLICT_REPLACE);
    }

    /**
     * General method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be <code>null</code>.
     *            SQL doesn't allow inserting a completely empty row without
     *            naming at least one column name.  If your provided <code>initialValues</code> is
     *            empty, no column names are known and an empty row can't be inserted.
     *            If not set to null, the <code>nullColumnHack</code> parameter
     *            provides the name of nullable column name to explicitly insert a NULL into
     *            in the case where your <code>initialValues</code> is empty.
     * @param initialValues this map contains the initial column values for the
     *            row. The keys should be the column names and the values the
     *            column values
     * @param conflictAlgorithm for insert conflict resolver
     * @return the row ID of the newly inserted row
     * OR the primary key of the existing row if the input param 'conflictAlgorithm' =
     * {@link #CONFLICT_IGNORE}
     * OR -1 if any error
     */
    fun insertWithOnConflict(table:String, nullColumnHack:String?,
                             initialValues:ContentValues?, conflictAlgorithm:Int):Long {
        acquireReference()
        try
        {
            val sql = StringBuilder()
            sql.append("INSERT")
            sql.append(CONFLICT_VALUES[conflictAlgorithm])
            sql.append(" INTO ")
            sql.append(table)
            sql.append('(')
            val size = initialValues?.size() ?: 0

            val bindArgs = arrayOfNulls<Any>(size)

            if (size > 0)
            {

                var i = 0
                for (colName in initialValues!!.keySet())
                {
                    sql.append(if ((i > 0)) "," else "")
                    sql.append(colName)
                    bindArgs[i++] = initialValues!!.get(colName)
                }
                sql.append(')')
                sql.append(" VALUES (")
                i = 0
                while (i < size)
                {
                    sql.append(if ((i > 0)) ",?" else "?")
                    i++
                }
            }
            else
            {
                sql.append("$nullColumnHack) VALUES (NULL")
            }
            sql.append(')')
            val statement = SQLiteStatement(this, sql.toString(), bindArgs)
            try
            {
                return statement.executeInsert()
            }
            finally
            {
                statement.close()
            }
        }
        finally
        {
            releaseReference()
        }
    }

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     * Passing null will delete all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise. To remove all rows and get a count pass "1" as the
     * whereClause.
     */
    fun delete(table:String, whereClause:String?, whereArgs:Array<String>?):Int {
        acquireReference()
        try
        {
            var i = 0
            var anyArgs = if(whereArgs == null){null}else{Array<Any?>(whereArgs.size, {
                whereArgs[i++]})}

            val statement = SQLiteStatement(this, ("DELETE FROM " + table +
                    (if (!whereClause.isNullOrEmpty()) " WHERE $whereClause" else "")), anyArgs)
            try
            {
                return statement.executeUpdateDelete()
            }
            finally
            {
                statement.close()
            }
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected
     */
    fun update(table:String, values:ContentValues, whereClause:String?, whereArgs:Array<String>?):Int {
        return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE)
    }
    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @param conflictAlgorithm for update conflict resolver
     * @return the number of rows affected
     */
    fun updateWithOnConflict(table:String, values:ContentValues,
                             whereClause:String?, whereArgs:Array<String>?, conflictAlgorithm:Int):Int {
        acquireReference()
        try
        {
            val sql = StringBuilder(120)
            sql.append("UPDATE ")
            sql.append(CONFLICT_VALUES[conflictAlgorithm])
            sql.append(table)
            sql.append(" SET ")
            // move all bind args to one array
            val setValuesSize = values.size()
            val bindArgsSize = if ((whereArgs == null)) setValuesSize else (setValuesSize + whereArgs.size)
            val bindArgs = arrayOfNulls<Any>(bindArgsSize)
            var i = 0
            for (colName in values.keySet())
            {
                sql.append(if ((i > 0)) "," else "")
                sql.append(colName)
                bindArgs[i++] = values.get(colName)
                sql.append("=?")
            }
            if (whereArgs != null)
            {
                i = setValuesSize
                while (i < bindArgsSize)
                {
                    bindArgs[i] = whereArgs[i - setValuesSize]
                    i++
                }
            }
            if (!(whereClause != null && whereClause.isEmpty()))
            {
                sql.append(" WHERE ")
                sql.append(whereClause)
            }
            val statement = SQLiteStatement(this, sql.toString(), bindArgs)
            try
            {
                return statement.executeUpdateDelete()
            }
            finally
            {
                statement.close()
            }
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Execute a single SQL statement that is NOT a SELECT
     * or any other SQL statement that returns data.
     * <p>
     * It has no means to return any data (such as the number of affected rows).
     * Instead, you're encouraged to use {@link #insert(String, String, ContentValues)},
     * {@link #update(String, ContentValues, String, String[])}, et al, when possible.
     * </p>
     * <p>
     * When using {@link #enableWriteAheadLogging()}, journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * {@link #enableWriteAheadLogging()}
     * </p>
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     * not supported.
     * @throws SQLException if the SQL string is invalid
     */
    fun execSQL(sql:String) {
        executeSql(sql, null)
    }
    /**
     * Execute a single SQL statement that is NOT a SELECT/INSERT/UPDATE/DELETE.
     * <p>
     * For INSERT statements, use any of the following instead.
     * <ul>
     * <li>{@link #insert(String, String, ContentValues)}</li>
     * <li>{@link #insertOrThrow(String, String, ContentValues)}</li>
     * <li>{@link #insertWithOnConflict(String, String, ContentValues, int)}</li>
     * </ul>
     * <p>
     * For UPDATE statements, use any of the following instead.
     * <ul>
     * <li>{@link #update(String, ContentValues, String, String[])}</li>
     * <li>{@link #updateWithOnConflict(String, ContentValues, String, String[], int)}</li>
     * </ul>
     * <p>
     * For DELETE statements, use any of the following instead.
     * <ul>
     * <li>{@link #delete(String, String, String[])}</li>
     * </ul>
     * <p>
     * For example, the following are good candidates for using this method:
     * <ul>
     * <li>ALTER TABLE</li>
     * <li>CREATE or DROP table / trigger / view / index / virtual table</li>
     * <li>REINDEX</li>
     * <li>RELEASE</li>
     * <li>SAVEPOINT</li>
     * <li>PRAGMA that returns no data</li>
     * </ul>
     * </p>
     * <p>
     * When using {@link #enableWriteAheadLogging()}, journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * {@link #enableWriteAheadLogging()}
     * </p>
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     * not supported.
     * @param bindArgs only byte[], String, Long and Double are supported in bindArgs.
     * @throws SQLException if the SQL string is invalid
     */
    fun execSQL(sql:String, bindArgs:Array<Any?>?) {
        if (bindArgs == null)
        {
            throw IllegalArgumentException("Empty bindArgs")
        }
        executeSql(sql, bindArgs)
    }

    private fun executeSql(sql:String, bindArgs:Array<Any?>?):Int {
        acquireReference()
        try
        {
            if (DatabaseUtils.getSqlStatementType(sql) == DatabaseUtils.STATEMENT_ATTACH)
            {
                var disableWal = false
//                    if (!mHasAttachedDbsLocked)
//                    {
//                        mHasAttachedDbsLocked = true
//                        disableWal = true
//                    }
                if (disableWal)
                {
                    disableWriteAheadLogging()
                }
            }
            val statement = SQLiteStatement(this, sql, bindArgs)
            try
            {
                return statement.executeUpdateDelete()
            }
            finally
            {
                statement.close()
            }
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Returns true if the database is opened as read only.
     *
     * @return True if database is opened as read only.
     */
    fun isReadOnly():Boolean {
        return isReadOnlyLocked()
    }
    private fun isReadOnlyLocked():Boolean {
        return sqliteSession.dbReadOnlyLocked()
    }
    /**
     * Returns true if the database is in-memory db.
     *
     * @return True if the database is in-memory.
     * @hide
     */
    fun isInMemoryDatabase():Boolean {
        return sqliteSession.dbInMemoryDb()
    }
    /**
     * Returns true if the database is currently open.
     *
     * @return True if the database is currently open (has not been closed).
     */
    fun isOpen():Boolean = sqliteSession.hasConnection()

    /**
     * Returns true if the new version code is greater than the current database version.
     *
     * @param newVersion The new version code.
     * @return True if the new version code is greater than the current database version.
     */
    fun needUpgrade(newVersion:Int):Boolean {
        return newVersion > getVersion()
    }
    /**
     * Gets the path to the database file.
     *
     * @return The path to the database file.
     */
    fun getPath():String {
        return sqliteSession.dbPath()
    }

    /**
     * Sets the maximum size of the prepared-statement cache for this database.
     * (size of the cache = number of compiled-sql-statements stored in the cache).
     *<p>
     * Maximum cache size can ONLY be increased from its current size (default = 10).
     * If this method is called with smaller size than the current maximum value,
     * then IllegalStateException is thrown.
     *<p>
     * This method is thread-safe.
     *
     * @param cacheSize the size of the cache. can be (0 to {@link #MAX_SQL_CACHE_SIZE})
     * @throws IllegalStateException if input cacheSize > {@link #MAX_SQL_CACHE_SIZE}.
     */
    fun setMaxSqlCacheSize(cacheSize: Int) {
        if (cacheSize > MAX_SQL_CACHE_SIZE || cacheSize < 0) {
            throw IllegalStateException(
                    "expected value between 0 and " + MAX_SQL_CACHE_SIZE)
        }
        throwIfNotOpenLocked()
        sqliteSession.dbConfigUpdate { it!!.copy(maxSqlCacheSize = cacheSize) }

        forceClose()
        open()
//        getThreadSession().mConnection.reconfigure(mConfigurationLocked)
//        TODO()
        /*    try
            {
                mConnectionPoolLocked.reconfigure(mConfigurationLocked)
            }
            catch (ex:RuntimeException) {
                mConfigurationLocked.maxSqlCacheSize = oldMaxSqlCacheSize
                throw ex
            }*/

    }

    /**
     * Sets whether foreign key constraints are enabled for the database.
     * <p>
     * By default, foreign key constraints are not enforced by the database.
     * This method allows an application to enable foreign key constraints.
     * It must be called each time the database is opened to ensure that foreign
     * key constraints are enabled for the session.
     * </p><p>
     * A good time to call this method is right after calling {@link #openOrCreateDatabase}
     * or in the {@link SQLiteOpenHelper#onConfigure} callback.
     * </p><p>
     * When foreign key constraints are disabled, the database does not check whether
     * changes to the database will violate foreign key constraints. Likewise, when
     * foreign key constraints are disabled, the database will not execute cascade
     * delete or update triggers. As a result, it is possible for the database
     * state to become inconsistent. To perform a database integrity check,
     * call {@link #isDatabaseIntegrityOk}.
     * </p><p>
     * This method must not be called while a transaction is in progress.
     * </p><p>
     * See also <a href="http://sqlite.org/foreignkeys.html">SQLite Foreign Key Constraints</a>
     * for more details about foreign key constraint support.
     * </p>
     *
     * @param enable True to enable foreign key constraints, false to disable them.
     *
     * @throws IllegalStateException if the are transactions is in progress
     * when this method is called.
     */
    fun setForeignKeyConstraintsEnabled(enable: Boolean) {
        throwIfNotOpenLocked()
        if (sqliteSession.dbForeignKeyConstraintsEnabled() == enable) {
            return
        }
        sqliteSession.dbConfigUpdate { it!!.copy(foreignKeyConstraintsEnabled = enable) }

        forceClose()
        open()

//        getThreadSession().mConnection.reconfigure(mConfigurationLocked)
//        TODO()
        /*try
        {
            mConnectionPoolLocked.reconfigure(mConfigurationLocked)
        }
        catch (ex:RuntimeException) {
            mConfigurationLocked.foreignKeyConstraintsEnabled = !enable
            throw ex
        }*/
    }
    /**
     * This method enables parallel execution of queries from multiple threads on the
     * same database. It does this by opening multiple connections to the database
     * and using a different database connection for each query. The database
     * journal mode is also changed to enable writes to proceed concurrently with reads.
     * <p>
     * When write-ahead logging is not enabled (the default), it is not possible for
     * reads and writes to occur on the database at the same time. Before modifying the
     * database, the writer implicitly acquires an exclusive lock on the database which
     * prevents readers from accessing the database until the write is completed.
     * </p><p>
     * In contrast, when write-ahead logging is enabled (by calling this method), write
     * operations occur in a separate log file which allows reads to proceed concurrently.
     * While a write is in progress, readers on other threads will perceive the state
     * of the database as it was before the write began. When the write completes, readers
     * on other threads will then perceive the new state of the database.
     * </p><p>
     * It is a good idea to enable write-ahead logging whenever a database will be
     * concurrently accessed and modified by multiple threads at the same time.
     * However, write-ahead logging uses significantly more memory than ordinary
     * journaling because there are multiple connections to the same database.
     * So if a database will only be used by a single thread, or if optimizing
     * concurrency is not very important, then write-ahead logging should be disabled.
     * </p><p>
     * After calling this method, execution of queries in parallel is enabled as long as
     * the database remains open. To disable execution of queries in parallel, either
     * call {@link #disableWriteAheadLogging} or close the database and reopen it.
     * </p><p>
     * The maximum number of connections used to execute queries in parallel is
     * dependent upon the device memory and possibly other properties.
     * </p><p>
     * If a query is part of a transaction, then it is executed on the same database handle the
     * transaction was begun.
     * </p><p>
     * Writers should use {@link #beginTransactionNonExclusive()} or
     * {@link #beginTransactionWithListenerNonExclusive(SQLiteTransactionListener)}
     * to start a transaction. Non-exclusive mode allows database file to be in readable
     * by other threads executing queries.
     * </p><p>
     * If the database has any attached databases, then execution of queries in parallel is NOT
     * possible. Likewise, write-ahead logging is not supported for read-only databases
     * or memory databases. In such cases, {@link #enableWriteAheadLogging} returns false.
     * </p><p>
     * The best way to enable write-ahead logging is to pass the
     * {@link #ENABLE_WRITE_AHEAD_LOGGING} flag to {@link #openDatabase}. This is
     * more efficient than calling {@link #enableWriteAheadLogging}.
     * <code><pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
     * myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
     * </pre></code>
     * </p><p>
     * Another way to enable write-ahead logging is to call {@link #enableWriteAheadLogging}
     * after opening the database.
     * <code><pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY, myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
     * </pre></code>
     * </p><p>
     * See also <a href="http://sqlite.org/wal.html">SQLite Write-Ahead Logging</a> for
     * more details about how write-ahead logging works.
     * </p>
     *
     * @return True if write-ahead logging is enabled.
     *
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called. WAL mode can only be changed when there are no
     * transactions in progress.
     *
     * @see #ENABLE_WRITE_AHEAD_LOGGING
     * @see #disableWriteAheadLogging
     */
    fun enableWriteAheadLogging(): Boolean {
        throwIfNotOpenLocked()
        if ((sqliteSession.dbOpenFlags() and ENABLE_WRITE_AHEAD_LOGGING) != 0) {
            return true
        }
        if (isReadOnlyLocked()) {
            // WAL doesn't make sense for readonly-databases.
            // TODO: True, but connection pooling does still make sense...
            return false
        }
        if (sqliteSession.dbInMemoryDb()) {
            Log.i(TAG, "can't enable WAL for memory databases.")
            return false
        }

        sqliteSession.dbConfigUpdate { it!!.copy(openFlags = it.openFlags or ENABLE_WRITE_AHEAD_LOGGING) }

        forceClose()
        open()
//        TODO()
        /*try
        {
            mConnectionPoolLocked.reconfigure(mConfigurationLocked)
        }
        catch (ex:RuntimeException) {
            mConfigurationLocked.openFlags = mConfigurationLocked.openFlags and ENABLE_WRITE_AHEAD_LOGGING.inv()
            throw ex
        }*/
        return true
    }
    /**
     * This method disables the features enabled by {@link #enableWriteAheadLogging()}.
     *
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called. WAL mode can only be changed when there are no
     * transactions in progress.
     *
     * @see #enableWriteAheadLogging
     */
    fun disableWriteAheadLogging() {
        throwIfNotOpenLocked()
        if ((sqliteSession.dbOpenFlags() and ENABLE_WRITE_AHEAD_LOGGING) == 0) {
            return
        }
        sqliteSession.dbConfigUpdate { it!!.copy(openFlags = it.openFlags and ENABLE_WRITE_AHEAD_LOGGING.inv()) }

        forceClose()
        open()
//        TODO()
        /*try
        {
            mConnectionPoolLocked.reconfigure(mConfigurationLocked)
        }
        catch (ex:RuntimeException) {
            mConfigurationLocked.openFlags = mConfigurationLocked.openFlags or ENABLE_WRITE_AHEAD_LOGGING
            throw ex
        }*/
    }
    /**
     * Returns true if write-ahead logging has been enabled for this database.
     *
     * @return True if write-ahead logging has been enabled for this database.
     *
     * @see #enableWriteAheadLogging
     * @see #ENABLE_WRITE_AHEAD_LOGGING
     */
    fun isWriteAheadLoggingEnabled():Boolean {
            throwIfNotOpenLocked()
            return (sqliteSession.dbOpenFlags() and ENABLE_WRITE_AHEAD_LOGGING) != 0
    }


    public override fun toString():String {
        return "SQLiteDatabase: " + getPath()
    }

    private fun throwIfNotOpenLocked() {
        if (!sqliteSession.hasConnection())
        {
            throw IllegalStateException(("The database '" + sqliteSession.dbLabel()
                    + "' is not open."))
        }
    }

    /**
     * Used to allow returning sub-classes of {@link Cursor} when calling query.
     */
    interface CursorFactory {
        /**
         * See {@link SQLiteCursor#SQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)}.
         */
        fun newCursor(db:SQLiteDatabase,
                      masterQuery:SQLiteCursorDriver, editTable:String?,
                      query:SQLiteQuery):Cursor
    }
    /**
     * A callback interface for a custom sqlite3 function.
     * This can be used to create a function that can be called from
     * sqlite3 database triggers.
     * @hide
     */
    interface CustomFunction {
        fun callback(args:Array<String>)
    }

    //Need to think on this stuff

    fun releaseReference() {}
    fun acquireReference() {}
}