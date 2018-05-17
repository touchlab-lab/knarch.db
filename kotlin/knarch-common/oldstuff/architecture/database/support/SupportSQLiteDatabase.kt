package co.touchlab.kurgan.architecture.database.support

import co.touchlab.kurgan.architecture.ContentValues
import co.touchlab.kurgan.architecture.database.*

interface SupportSQLiteDatabase {
    companion object {
        /**
         * When a constraint violation occurs, an immediate ROLLBACK occurs,
         * thus ending the current transaction, and the command aborts with a
         * return code of SQLITE_CONSTRAINT. If no transaction is active
         * (other than the implied transaction that is created on every command)
         * then this algorithm works the same as ABORT.
         */
        val CONFLICT_ROLLBACK = 1

        /**
         * When a constraint violation occurs,no ROLLBACK is executed
         * so changes from prior commands within the same transaction
         * are preserved. This is the default behavior.
         */
        val CONFLICT_ABORT = 2

        /**
         * When a constraint violation occurs, the command aborts with a return
         * code SQLITE_CONSTRAINT. But any changes to the database that
         * the command made prior to encountering the constraint violation
         * are preserved and are not backed out.
         */
        val CONFLICT_FAIL = 3

        /**
         * When a constraint violation occurs, the one row that contains
         * the constraint violation is not inserted or changed.
         * But the command continues executing normally. Other rows before and
         * after the row that contained the constraint violation continue to be
         * inserted or updated normally. No error is returned.
         */
        val CONFLICT_IGNORE = 4

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
        val CONFLICT_REPLACE = 5

        /**
         * Use the following when no conflict action is specified.
         */
        val CONFLICT_NONE = 0
    }
    /**
     * Compiles the given SQL statement.
     *
     * @param sql The sql query.
     * @return Compiled statement.
     */
    fun compileStatement(sql: String): SupportSQLiteStatement

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     *
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransaction();
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     */
    fun beginTransaction()

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionNonExclusive();
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     */
    fun beginTransactionNonExclusive()

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     *
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionWithListener(listener);
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     * commits, or is rolled back, either explicitly or by a call to
     * [.yieldIfContendedSafely].
     */
    fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener)

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionWithListenerNonExclusive(listener);
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     *
     * @param transactionListener listener that should be notified when the
     * transaction begins, commits, or is rolled back, either
     * explicitly or by a call to [.yieldIfContendedSafely].
     */
    fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener)

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    fun endTransaction()

    /**
     * Marks the current transaction as successful. Do not do any more database work between
     * calling this and calling endTransaction. Do as little non-database work as possible in that
     * situation too. If any errors are encountered between this and endTransaction the transaction
     * will still be committed.
     *
     * @throws IllegalStateException if the current thread is not in a transaction or the
     * transaction is already marked as successful.
     */
    fun setTransactionSuccessful()

    /**
     * Returns true if the current thread has a transaction pending.
     *
     * @return True if the current thread is in a transaction.
     */
    fun inTransaction(): Boolean

    /**
     * Returns true if the current thread is holding an active connection to the database.
     *
     *
     * The name of this method comes from a time when having an active connection
     * to the database meant that the thread was holding an actual lock on the
     * database.  Nowadays, there is no longer a true "database lock" although threads
     * may block if they cannot acquire a database connection to perform a
     * particular operation.
     *
     *
     * @return True if the current thread is holding an active connection to the database.
     */
    //REVIEW: Not sure we need this
    fun isDbLockedByCurrentThread(): Boolean

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     *
     * @return true if the transaction was yielded
     */
    fun yieldIfContendedSafely(): Boolean

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     *
     * @param sleepAfterYieldDelay if > 0, sleep this long before starting a new transaction if
     * the lock was actually yielded. This will allow other background
     * threads to make some
     * more progress than they would if we started the transaction
     * immediately.
     * @return true if the transaction was yielded
     */
    fun yieldIfContendedSafely(sleepAfterYieldDelay: Long): Boolean

    val version:Int

    /**
     * The maximum size the database may grow to.
     * The maximum size cannot be set below the current size.
     */
    var maximumSize: Long

    /**
     * The database page size, in bytes.
     *
     * The page size must be a power of two. This
     * method does not work if any data has been written to the database file,
     * and must be called right after the database has been created.
     */
    var pageSize: Long

    /**
     * Runs the given query on the database. If you would like to have bind arguments,
     * use [.query].
     *
     * @param query The SQL query that includes the query and can bind into a given compiled
     * program.
     * @param bindArgs The query arguments to bind.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     * @see .query
     */
    fun query(query: String, bindArgs: Array<Any?>? = null): Cursor

    /**
     * Runs the given query on the database.
     *
     *
     * This class allows using type safe sql program bindings while running queries.
     *
     * @param query The SQL query that includes the query and can bind into a given compiled
     * program.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     * @see SimpleSQLiteQuery
     */
    fun query(supportQuery: SupportSQLiteQuery): Cursor

    /**
     * Convenience method for inserting a row into the database.
     *
     * @param table          the table to insert the row into
     * @param values         this map contains the initial column values for the
     * row. The keys should be the column names and the values the
     * column values
     * @param conflictAlgorithm for insert conflict resolver. One of
     * [SQLiteDatabase.CONFLICT_NONE], [SQLiteDatabase.CONFLICT_ROLLBACK],
     * [SQLiteDatabase.CONFLICT_ABORT], [SQLiteDatabase.CONFLICT_FAIL],
     * [SQLiteDatabase.CONFLICT_IGNORE], [SQLiteDatabase.CONFLICT_REPLACE].
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     * @throws SQLException If the insert fails
     */
    fun insert(table: String, conflictAlgorithm: Int = CONFLICT_ABORT, values: ContentValues): Long

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table       the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     * Passing null will delete all rows.
     * @param whereArgs   You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise. To remove all rows and get a count pass "1" as the
     * whereClause.
     */
    fun delete(table: String, whereClause: String?, whereArgs: Array<Any?>?): Int

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table       the table to update in
     * @param conflictAlgorithm for update conflict resolver. One of
     * [SQLiteDatabase.CONFLICT_NONE], [SQLiteDatabase.CONFLICT_ROLLBACK],
     * [SQLiteDatabase.CONFLICT_ABORT], [SQLiteDatabase.CONFLICT_FAIL],
     * [SQLiteDatabase.CONFLICT_IGNORE], [SQLiteDatabase.CONFLICT_REPLACE].
     * @param values      a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs   You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected
     */
    fun update(table: String, conflictAlgorithm: Int = CONFLICT_ABORT,
               values: ContentValues, whereClause: String?, whereArgs: Array<Any?>?): Int

    /**
     * Execute a single SQL statement that does not return any data.
     *
     *
     * When using [.enableWriteAheadLogging], journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * [.enableWriteAheadLogging]
    </value> *
     *
     * @param sql      the SQL statement to be executed. Multiple statements separated by semicolons
     * are
     * not supported.
     * @param bindArgs only byte[], String, Long and Double are supported in selectionArgs.
     * @throws SQLException if the SQL string is invalid
     * @see .query
     */
    fun execSQL(sql: String, bindArgs: Array<Any?>? = null)

    /**
     * Returns true if the database is opened as read only.
     *
     * @return True if database is opened as read only.
     */
    fun isReadOnly(): Boolean

    /**
     * Returns true if the database is currently open.
     *
     * @return True if the database is currently open (has not been closed).
     */
    fun isOpen(): Boolean

    /**
     * Returns true if the new version code is greater than the current database version.
     *
     * @param newVersion The new version code.
     * @return True if the new version code is greater than the current database version.
     */
    fun needUpgrade(newVersion: Int): Boolean

    /**
     * Gets the path to the database file.
     *
     * @return The path to the database file.
     */
    fun getPath(): String

    /**
     * Sets the maximum size of the prepared-statement cache for this database.
     * (size of the cache = number of compiled-sql-statements stored in the cache).
     *
     *
     * Maximum cache size can ONLY be increased from its current size (default = 10).
     * If this method is called with smaller size than the current maximum value,
     * then IllegalStateException is thrown.
     *
     *
     * This method is thread-safe.
     *
     * @param cacheSize the size of the cache. can be (0 to
     * [SQLiteDatabase.MAX_SQL_CACHE_SIZE])
     * @throws IllegalStateException if input cacheSize gt;
     * [SQLiteDatabase.MAX_SQL_CACHE_SIZE].
     */
    fun setMaxSqlCacheSize(cacheSize: Int)

    /**
     * Sets whether foreign key constraints are enabled for the database.
     *
     *
     * By default, foreign key constraints are not enforced by the database.
     * This method allows an application to enable foreign key constraints.
     * It must be called each time the database is opened to ensure that foreign
     * key constraints are enabled for the session.
     *
     *
     * A good time to call this method is right after calling `#openOrCreateDatabase`
     * or in the [SupportSQLiteOpenHelper.Callback.onConfigure] callback.
     *
     *
     * When foreign key constraints are disabled, the database does not check whether
     * changes to the database will violate foreign key constraints.  Likewise, when
     * foreign key constraints are disabled, the database will not execute cascade
     * delete or update triggers.  As a result, it is possible for the database
     * state to become inconsistent.  To perform a database integrity check,
     * call [.isDatabaseIntegrityOk].
     *
     *
     * This method must not be called while a transaction is in progress.
     *
     *
     * See also [SQLite Foreign Key Constraints](http://sqlite.org/foreignkeys.html)
     * for more details about foreign key constraint support.
     *
     *
     * @param enable True to enable foreign key constraints, false to disable them.
     * @throws IllegalStateException if the are transactions is in progress
     * when this method is called.
     */
    fun setForeignKeyConstraintsEnabled(enable: Boolean)

    /**
     * This method enables parallel execution of queries from multiple threads on the
     * same database.  It does this by opening multiple connections to the database
     * and using a different database connection for each query.  The database
     * journal mode is also changed to enable writes to proceed concurrently with reads.
     *
     *
     * When write-ahead logging is not enabled (the default), it is not possible for
     * reads and writes to occur on the database at the same time.  Before modifying the
     * database, the writer implicitly acquires an exclusive lock on the database which
     * prevents readers from accessing the database until the write is completed.
     *
     *
     * In contrast, when write-ahead logging is enabled (by calling this method), write
     * operations occur in a separate log file which allows reads to proceed concurrently.
     * While a write is in progress, readers on other threads will perceive the state
     * of the database as it was before the write began.  When the write completes, readers
     * on other threads will then perceive the new state of the database.
     *
     *
     * It is a good idea to enable write-ahead logging whenever a database will be
     * concurrently accessed and modified by multiple threads at the same time.
     * However, write-ahead logging uses significantly more memory than ordinary
     * journaling because there are multiple connections to the same database.
     * So if a database will only be used by a single thread, or if optimizing
     * concurrency is not very important, then write-ahead logging should be disabled.
     *
     *
     * After calling this method, execution of queries in parallel is enabled as long as
     * the database remains open.  To disable execution of queries in parallel, either
     * call [.disableWriteAheadLogging] or close the database and reopen it.
     *
     *
     * The maximum number of connections used to execute queries in parallel is
     * dependent upon the device memory and possibly other properties.
     *
     *
     * If a query is part of a transaction, then it is executed on the same database handle the
     * transaction was begun.
     *
     *
     * Writers should use [.beginTransactionNonExclusive] or
     * [.beginTransactionWithListenerNonExclusive]
     * to start a transaction.  Non-exclusive mode allows database file to be in readable
     * by other threads executing queries.
     *
     *
     * If the database has any attached databases, then execution of queries in parallel is NOT
     * possible.  Likewise, write-ahead logging is not supported for read-only databases
     * or memory databases.  In such cases, `enableWriteAheadLogging` returns false.
     *
     *
     * The best way to enable write-ahead logging is to pass the
     * [SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING] flag to
     * [SQLiteDatabase.openDatabase].  This is more efficient than calling
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
     * myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
    </pre>` *
     *
     *
     * Another way to enable write-ahead logging is to call `enableWriteAheadLogging`
     * after opening the database.
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY, myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
    </pre>` *
     *
     *
     * See also [SQLite Write-Ahead Logging](http://sqlite.org/wal.html) for
     * more details about how write-ahead logging works.
     *
     *
     * @return True if write-ahead logging is enabled.
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called.  WAL mode can only be changed when
     * there are no
     * transactions in progress.
     * @see SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
     *
     * @see .disableWriteAheadLogging
     */
    fun enableWriteAheadLogging(): Boolean

    /**
     * This method disables the features enabled by [.enableWriteAheadLogging].
     *
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called.  WAL mode can only be changed when
     * there are no
     * transactions in progress.
     * @see .enableWriteAheadLogging
     */
    fun disableWriteAheadLogging()

    /**
     * Returns true if write-ahead logging has been enabled for this database.
     *
     * @return True if write-ahead logging has been enabled for this database.
     * @see .enableWriteAheadLogging
     *
     * @see SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
     */
    fun isWriteAheadLoggingEnabled(): Boolean

    /**
     * Runs 'pragma integrity_check' on the given database (and all the attached databases)
     * and returns true if the given database (and all its attached databases) pass integrity_check,
     * false otherwise.
     *
     *
     * If the result is false, then this method logs the errors reported by the integrity_check
     * command execution.
     *
     *
     * Note that 'pragma integrity_check' on a database can take a long time.
     *
     * @return true if the given database (and all its attached databases) pass integrity_check,
     * false otherwise.
     */
    fun isDatabaseIntegrityOk(): Boolean

    fun close()
}