package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.db.Cursor
import co.touchlab.knarch.db.CursorWindow
import co.touchlab.knarch.db.DatabaseUtils
import co.touchlab.knarch.db.native.*
import co.touchlab.knarch.other.Printer
import co.touchlab.knarch.Log
import kotlin.system.*
import konan.worker.*
import kotlinx.cinterop.*
import konan.internal.ExportForCppRuntime

@SymbolName("SQLiteSupport_createDataStore")
private external fun createDataStore(maxCacheSize:Int):Int

@SymbolName("SQLiteSupport_getConnectionPtr")
private external fun getConnectionPtr(dataId:Int):Long

@SymbolName("SQLiteSupport_putConnectionPtr")
private external fun putConnectionPtr(dataId:Int, connectionPtr:Long)

@SymbolName("SQLiteSupport_incConnectionCount")
private external fun incConnectionCount(dataId:Int)

@SymbolName("SQLiteSupport_decConnectionCount")
private external fun decConnectionCount(dataId:Int)

@SymbolName("SQLiteSupport_getConnectionCount")
private external fun getConnectionCount(dataId:Int):Int

@SymbolName("SQLiteSupport_putStmt")
private external fun putStmt(dataId:Int, sql:String, ptr:NativePreparedStatement)

@SymbolName("SQLiteSupport_getStmt")
private external fun getStmt(dataId:Int, sql:String):NativePreparedStatement

@SymbolName("SQLiteSupport_hasStmt")
private external fun hasStmt(dataId:Int, sql:String):Boolean

@SymbolName("SQLiteSupport_getTransaction")
private external fun getTransaction(dataId:Int):SQLiteSession.Transaction?

@SymbolName("SQLiteSupport_putTransaction")
private external fun putTransaction(dataId:Int, trans:SQLiteSession.Transaction?)

@SymbolName("SQLiteSupport_removeTransaction")
private external fun removeTransaction(dataId:Int)

@SymbolName("SQLiteSupport_evictAll")
private external fun evictAll(dataId:Int)

@SymbolName("SQLiteSupport_remove")
private external fun remove(dataId:Int, sql:String)

@SymbolName("Android_Database_SQLiteConnection_nativeFinalizeStatement")
private external fun nativeFinalizeStatement(connectionPtr:Long, statementPtr:Long)

@ExportForCppRuntime
private fun finalizeStmt(connectionPtr:Long, ptr:NativePreparedStatement){
    nativeFinalizeStatement(connectionPtr, ptr.mStatementPtr)
}

class SQLiteConnection(private val mConfiguration:SQLiteDatabaseConfiguration) {
    private val mIsReadOnlyConnection:Boolean = false

    // The recent operations log.
    private val mRecentOperations = OperationLog()
    // The native SQLiteConnection pointer. (FOR INTERNAL USE ONLY)
    private val nativeDataId: Int = createDataStore(mConfiguration.maxSqlCacheSize)

    init{
        try
        {
            open()
        }
        catch (ex:SQLiteException) {
            dispose(false)
            throw ex
        }
    }

    fun incConnectionCount(){
        incConnectionCount(nativeDataId)
    }

    fun decConnectionCount(){
        decConnectionCount(nativeDataId)
    }

    fun getConnectionCount():Int = getConnectionCount(nativeDataId)

    internal fun getTransaction():SQLiteSession.Transaction? = getTransaction(nativeDataId)
    internal fun putTransaction(trans:SQLiteSession.Transaction?) {
        if(trans == null)
        {
            removeTransaction()
        }
        else
        {
            putTransaction(nativeDataId, trans)
        }
    }

    private fun removeTransaction(){
        removeTransaction(nativeDataId)
    }

    //Statement cache methods
    private fun cacheEvictAll(){
        evictAll(nativeDataId)
    }

    private fun cacheRemove(sql:String){
        remove(nativeDataId, sql)
    }

    private fun cacheGetStatement(sql:String):NativePreparedStatement{
        return getStmt(nativeDataId, sql)
    }

    private fun cacheHasStatement(sql:String):Boolean{
        return hasStmt(nativeDataId, sql)
    }

    private fun cachePutStatement(sql:String, stmt:NativePreparedStatement){
        if(!stmt.mInCache)
            throw IllegalStateException("Only mInCache goes in cache")
        putStmt(nativeDataId, sql, stmt)
    }

    internal fun hasConnection() = getConnectionPtr(nativeDataId) != 0L

    // Called by SQLiteConnectionPool only.
    // Closes the database closes and releases all of its associated resources.
    // Do not call methods on the connection after it is closed. It will probably crash.
    internal fun close() {
        dispose(false)
    }

    fun open() {
        val connectionPtr = nativeOpen(mConfiguration.path, mConfiguration.openFlags,
                mConfiguration.label,
                SQLiteDebug.DEBUG_SQL_STATEMENTS, SQLiteDebug.DEBUG_SQL_TIME,
                mConfiguration.lookasideSlotSize, mConfiguration.lookasideSlotCount)

        putConnectionPtr(nativeDataId, connectionPtr)

        setPageSize()
        setForeignKeyModeFromConfiguration()
        setWalModeFromConfiguration()
        setJournalSizeLimit()
        setAutoCheckpointInterval()
        // setLocaleFromConfiguration();
        // Register custom functions.

    }

    private fun dispose(finalized:Boolean) {

        val connectionPtr = getConnectionPtr(nativeDataId)

        if (connectionPtr != 0L)
        {
            val cookie = mRecentOperations.beginOperation("close", null, null)
            try
            {
                cacheEvictAll()
                nativeClose(connectionPtr)
                putConnectionPtr(nativeDataId, 0)
            }
            finally
            {
                mRecentOperations.endOperation(cookie)
            }
        }
    }
    private fun setPageSize() {
        if (!mConfiguration.isInMemoryDb() && !mIsReadOnlyConnection)
        {
            val newValue = SQLiteGlobal.defaultPageSize
            val value = executeForLong("PRAGMA page_size", null).toInt()
            if (value != newValue)
            {
                execute("PRAGMA page_size=$newValue", null)
            }
        }
    }
    private fun setAutoCheckpointInterval() {
        if (!mConfiguration.isInMemoryDb() && !mIsReadOnlyConnection)
        {
            val newValue = SQLiteGlobal.walAutoCheckpoint
            val value = executeForLong("PRAGMA wal_autocheckpoint", null).toInt()
            if (value != newValue)
            {
                executeForLong("PRAGMA wal_autocheckpoint=$newValue", null)
            }
        }
    }
    private fun setJournalSizeLimit() {
        if (!mConfiguration.isInMemoryDb() && !mIsReadOnlyConnection)
        {
            val newValue = SQLiteGlobal.journalSizeLimit
            var value = executeForLong("PRAGMA journal_size_limit", null).toInt()
            if (value != newValue)
            {
                executeForLong("PRAGMA journal_size_limit=$newValue", null)
            }
        }
    }
    private fun setForeignKeyModeFromConfiguration() {
        if (!mIsReadOnlyConnection)
        {
            val newValue = (if (mConfiguration.foreignKeyConstraintsEnabled) 1 else 0).toLong()
            val value = executeForLong("PRAGMA foreign_keys", null)
            if (value != newValue)
            {
                execute("PRAGMA foreign_keys=$newValue", null)
            }
        }
    }
    private fun setWalModeFromConfiguration() {
        if (!mConfiguration.isInMemoryDb() && !mIsReadOnlyConnection)
        {
            if ((mConfiguration.openFlags and SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING) !== 0)
            {
                setJournalMode("WAL")
                setSyncMode(SQLiteGlobal.walSyncMode)
            }
            else
            {
                setJournalMode(SQLiteGlobal.defaultJournalMode)
                setSyncMode(SQLiteGlobal.defaultSyncMode)
            }
        }
    }
    private fun setSyncMode(newValue:String) {
        val value = executeForString("PRAGMA synchronous", null)
        if (!canonicalizeSyncMode(value).equals(
                        canonicalizeSyncMode(newValue), ignoreCase = true))
        {
            execute("PRAGMA synchronous=$newValue", null)
        }
    }
    private fun setJournalMode(newValue:String) {
        val value = executeForString("PRAGMA journal_mode", null)
        if (!value.equals(newValue, ignoreCase = true))
        {
            try
            {
                val result = executeForString("PRAGMA journal_mode=$newValue", null)
                if (result.equals(newValue, ignoreCase = true))
                {
                    return
                }
                // PRAGMA journal_mode silently fails and returns the original journal
                // mode in some cases if the journal mode could not be changed.
            }
            //TODO: This was something else. Should review.
            catch (ex:SQLiteException) {
                // This error (SQLITE_BUSY) occurs if one connection has the database
                // open in WAL mode and another tries to change it to non-WAL.
            }
            // Because we always disable WAL mode when a database is first opened
            // (even if we intend to re-enable it), we can encounter problems if
            // there is another open connection to the database somewhere.
            // This can happen for a variety of reasons such as an application opening
            // the same database in multiple processes at the same time or if there is a
            // crashing content provider service that the ActivityManager has
            // removed from its registry but whose process hasn't quite died yet
            // by the time it is restarted in a new process.
            //
            // If we don't change the journal mode, nothing really bad happens.
            // In the worst case, an application that enables WAL might not actually
            // get it, although it can still use connection pooling.
            Log.w(TAG, ("Could not change the database journal mode of '"
                    + mConfiguration.label + "' from '" + value + "' to '" + newValue
                    + "' because the database is locked. This usually means that "
                    + "there are other open connections to the database which prevents "
                    + "the database from enabling or disabling write-ahead logging mode. "
                    + "Proceeding without changing the journal mode."))
        }
    }

    // Called by SQLiteConnectionPool only.
    /*internal fun reconfigure(configuration:SQLiteDatabaseConfiguration) {

        // Remember what changed.
        val foreignKeyModeChanged = (configuration.foreignKeyConstraintsEnabled !== mConfiguration.foreignKeyConstraintsEnabled)
        val walModeChanged = ((((configuration.openFlags xor mConfiguration.openFlags) and SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)) !== 0)
        // Update configuration parameters.
        mConfiguration.updateParametersFrom(configuration)
        // Update prepared statement cache size.
        mPreparedStatementCache.resize(configuration.maxSqlCacheSize)
        // Update foreign key mode.
        if (foreignKeyModeChanged)
        {
            setForeignKeyModeFromConfiguration()
        }
        // Update WAL.
        if (walModeChanged)
        {
            setWalModeFromConfiguration()
        }
    }*/

    // Called by SQLiteConnectionPool only.
    // Returns true if the prepared statement cache contains the specified SQL.
    internal fun isPreparedStatementInCache(sql:String):Boolean {
        return cacheHasStatement(sql)
    }

    /**
     * Prepares a statement for execution but does not bind its parameters or execute it.
     * <p>
     * This method can be used to check for syntax errors during compilation
     * prior to execution of the statement. If the {@code outStatementInfo} argument
     * is not null, the provided {@link SQLiteStatementInfo} object is populated
     * with information about the statement.
     * </p><p>
     * A prepared statement makes no reference to the arguments that may eventually
     * be bound to it, consequently it it possible to cache certain prepared statements
     * such as SELECT or INSERT/UPDATE statements. If the statement is cacheable,
     * then it will be stored in the cache for later.
     * </p><p>
     * To take advantage of this behavior as an optimization, the connection pool
     * provides a method to acquire a connection that already has a given SQL statement
     * in its prepared statement cache so that it is ready for execution.
     * </p>
     *
     * @param sql The SQL statement to prepare.
     * @param outStatementInfo The {@link SQLiteStatementInfo} object to populate
     * with information about the statement, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error.
     */
    fun prepare(sql:String, outStatementInfo:SQLiteStatementInfo?) {
        val cookie = mRecentOperations.beginOperation("prepare", sql, null)
        try
        {
            val statement = acquirePreparedStatement(sql)
            try
            {
                if (outStatementInfo != null)
                {
                    val connectionPtr = getConnectionPtr(nativeDataId)
                    outStatementInfo.numParameters = statement.mNumParameters
                    outStatementInfo.readOnly = statement.mReadOnly
                    val columnCount = nativeGetColumnCount(
                            connectionPtr, statement.mStatementPtr)
                    if (columnCount == 0)
                    {
                        outStatementInfo.columnNames = EMPTY_STRING_ARRAY
                    }
                    else
                    {
                        outStatementInfo.columnNames = Array(columnCount, {
                            col -> nativeGetColumnName(
                                connectionPtr, statement.mStatementPtr, col)
                        })
                    }
                }
            }
            finally
            {
                releasePreparedStatement(statement)
            }
        }
        catch (ex:RuntimeException) {
            mRecentOperations.failOperation(cookie, ex)
            throw ex
        }
        finally
        {
            mRecentOperations.endOperation(cookie)
        }
    }
    /**
     * Executes a statement that does not return a result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun execute(sql:String, bindArgs:Array<Any?>?) {
        val cookie = mRecentOperations.beginOperation("execute", sql, bindArgs)
        try
        {
            val statement = acquirePreparedStatement(sql)
            try
            {
                bindArguments(statement, bindArgs)
                nativeExecute(getConnectionPtr(nativeDataId), statement.mStatementPtr)
            }
            finally
            {
                releasePreparedStatement(statement)
            }
        }
        catch (ex:RuntimeException) {
            mRecentOperations.failOperation(cookie, ex)
            throw ex
        }
        finally
        {
            mRecentOperations.endOperation(cookie)
        }
    }
    /**
     * Executes a statement that returns a single <code>long</code> result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a <code>long</code>, or zero if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForLong(sql:String, bindArgs:Array<Any?>?):Long {
        val cookie = mRecentOperations.beginOperation("executeForLong", sql, bindArgs)
        try
        {
            val statement = acquirePreparedStatement(sql)
            try
            {
                bindArguments(statement, bindArgs)
                return nativeExecuteForLong(getConnectionPtr(nativeDataId), statement.mStatementPtr)
            }
            finally
            {
                releasePreparedStatement(statement)
            }
        }
        catch (ex:RuntimeException) {
            mRecentOperations.failOperation(cookie, ex)
            throw ex
        }
        finally
        {
            mRecentOperations.endOperation(cookie)
        }
    }
    /**
     * Executes a statement that returns a single {@link String} result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a <code>String</code>, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForString(sql:String, bindArgs:Array<Any?>?):String {
        val cookie = mRecentOperations.beginOperation("executeForString", sql, bindArgs)
        try
        {
            val statement = acquirePreparedStatement(sql)
            try {
                bindArguments(statement, bindArgs)
                return nativeExecuteForString(getConnectionPtr(nativeDataId), statement.mStatementPtr)

            }
            finally
            {
                releasePreparedStatement(statement)
            }
        }
        catch (ex:RuntimeException) {
            mRecentOperations.failOperation(cookie, ex)
            throw ex
        }
        finally
        {
            mRecentOperations.endOperation(cookie)
        }
    }
    /**
     * Executes a statement that returns a count of the number of rows
     * that were changed. Use for UPDATE or DELETE SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were changed.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForChangedRowCount(sql:String, bindArgs:Array<Any?>?):Int {
        var changedRows = 0
        val cookie = mRecentOperations.beginOperation("executeForChangedRowCount",
                sql, bindArgs)
        try
        {
            val statement = acquirePreparedStatement(sql)
            try
            {
                bindArguments(statement, bindArgs)
                changedRows = nativeExecuteForChangedRowCount(
                        getConnectionPtr(nativeDataId), statement.mStatementPtr)


                return changedRows
            }
            finally
            {
                releasePreparedStatement(statement)
            }
        }
        catch (ex:RuntimeException) {
            mRecentOperations.failOperation(cookie, ex)
            throw ex
        }
        finally
        {
            if (mRecentOperations.endOperationDeferLog(cookie))
            {
                mRecentOperations.logOperation(cookie, "changedRows=" + changedRows)
            }
        }
    }
    /**
     * Executes a statement that returns the row id of the last row inserted
     * by the statement. Use for INSERT SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The row id of the last row that was inserted, or 0 if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForLastInsertedRowId(sql:String, bindArgs:Array<Any?>?):Long {
        val cookie = mRecentOperations.beginOperation("executeForLastInsertedRowId",
                sql, bindArgs)
        try
        {
            val statement = acquirePreparedStatement(sql)
            try {
                bindArguments(statement, bindArgs)
                return nativeExecuteForLastInsertedRowId(
                        getConnectionPtr(nativeDataId), statement.mStatementPtr)

            }
            finally
            {
                releasePreparedStatement(statement)
            }
        }
        catch (ex:RuntimeException) {
            mRecentOperations.failOperation(cookie, ex)
            throw ex
        }
        finally
        {
            mRecentOperations.endOperation(cookie)
        }
    }
    /**
     * Executes a statement and populates the specified {@link CursorWindow}
     * with a range of results. Returns the number of rows that were counted
     * during query execution.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param window The cursor window to clear and fill.
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled
     * so that it does. Must be greater than or equal to <code>startPos</code>.
     * @param countAllRows True to count all rows that the query would return
     * regagless of whether they fit in the window.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were counted during query execution. Might
     * not be all rows in the result set unless <code>countAllRows</code> is true.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForCursorWindow(sql:String, bindArgs:Array<Any?>?,
                               window:CursorWindow, startPos:Int, requiredPos:Int, countAllRows:Boolean):Int {
        window.acquireReference()
        try
        {
            var actualPos = -1
            var countedRows = -1
            var filledRows = -1
            val cookie = mRecentOperations.beginOperation("executeForCursorWindow",
                    sql, bindArgs)
            try
            {
                val statement = acquirePreparedStatement(sql)
                try {
                    bindArguments(statement, bindArgs)

                    val result = nativeExecuteForCursorWindow(
                            getConnectionPtr(nativeDataId), statement.mStatementPtr, window.getWindowCursorPtr(),
                            startPos, requiredPos, countAllRows)
                    actualPos = (result shr 32).toInt()
                    countedRows = result.toInt()
                    filledRows = window.numRows
                    window.startPosition = actualPos
                    return countedRows

                }
                finally
                {
                    releasePreparedStatement(statement)
                }
            }
            catch (ex:RuntimeException) {
                mRecentOperations.failOperation(cookie, ex)
                throw ex
            }
            finally
            {
                if (mRecentOperations.endOperationDeferLog(cookie))
                {
                    mRecentOperations.logOperation(cookie, ("window='" + window
                            + "', startPos=" + startPos
                            + ", actualPos=" + actualPos
                            + ", filledRows=" + filledRows
                            + ", countedRows=" + countedRows))
                }
            }
        }
        finally
        {
            window.releaseReference()
        }
    }
    private fun acquirePreparedStatement(sql:String):NativePreparedStatement {
        /*if(hasStmt(nativeDataId, sql)) {
            val ps = konan.worker.attachObjectGraph<NativePreparedStatement>(getStmt(nativeDataId, sql))
            println("RRRRRR $ps")
            konan.worker.detachObjectGraph { ps }
        }*/
//        var statement = mPreparedStatementCache.get(sql)
//        var skipCache = hasStmt(nativeDataId, sql)
        if (cacheHasStatement(sql))
        {
            return cacheGetStatement(sql)
        }
        var statement:NativePreparedStatement? = null
        val statementPtr = nativePrepareStatement(getConnectionPtr(nativeDataId), sql)
        try
        {
            val numParameters = nativeGetParameterCount(getConnectionPtr(nativeDataId), statementPtr)
            val type = DatabaseUtils.getSqlStatementType(sql)
            val readOnly = nativeIsReadOnly(getConnectionPtr(nativeDataId), statementPtr)
            statement = obtainPreparedStatement(
                    sql,
                    statementPtr,
                    numParameters,
                    type,
                    readOnly,
                    isCacheable(type))

            if (statement.mInCache)
            {
                cachePutStatement(sql, statement)
            }
        }
        catch (ex:RuntimeException) {
            // Finalize the statement if an exception occurred and we did not add
            // it to the cache. If it is already in the cache, then leave it there.
            if (statement == null || !statement.mInCache)
            {
                nativeFinalizeStatement(getConnectionPtr(nativeDataId), statementPtr)
            }
            throw ex
        }

        return statement
    }

    private fun releasePreparedStatement(statement:NativePreparedStatement) {

        if (statement.mInCache)
        {
            try
            {
                nativeResetStatementAndClearBindings(getConnectionPtr(nativeDataId), statement.mStatementPtr)
            }
            catch (ex:SQLiteException) {
                // The statement could not be reset due to an error. Remove it from the cache.
                // When remove() is called, the cache will invoke its entryRemoved() callback,
                // which will in turn call finalizePreparedStatement() to finalize and
                // recycle the statement.
                if (DEBUG)
                {
                    Log.d(TAG, ("Could not reset prepared statement due to an exception. "
                            + "Removing it from the cache. SQL: "
                            + trimSqlForDisplay(statement.mSql)), ex)
                }
                cacheRemove(statement.mSql)
            }
        }
        else
        {
            nativeFinalizeStatement(getConnectionPtr(nativeDataId), statement.mStatementPtr)
        }
    }

    // CancellationSignal.OnCancelListener callback.
    // This method may be called on a different thread than the executing statement.
    // However, it will only be called between calls to attachCancellationSignal and
    // detachCancellationSignal, while a statement is executing. We can safely assume
    // that the SQLite connection is still alive.
    fun onCancel() {
        nativeCancel(getConnectionPtr(nativeDataId))
    }

    private fun bindArguments(statement:NativePreparedStatement, bindArgs:Array<Any?>?) {
        val count = if (bindArgs != null) bindArgs.size else 0
        if (count != statement.mNumParameters)
        {
            throw SQLiteException(
                    ("Expected " + statement.mNumParameters + " bind arguments but "
                            + count + " were provided."))
        }
        if (count == 0)
        {
            return
        }
        val statementPtr = statement.mStatementPtr
        for (i in 0 until count)
        {
            val connectionPtr = getConnectionPtr(nativeDataId)
            val arg = bindArgs!![i]
            when (DatabaseUtils.getTypeOfObject(arg)) {
                Cursor.FIELD_TYPE_NULL -> nativeBindNull(connectionPtr, statementPtr, i + 1)
                Cursor.FIELD_TYPE_INTEGER -> nativeBindLong(connectionPtr, statementPtr, i + 1,
                        (arg as Number).toLong())
                Cursor.FIELD_TYPE_FLOAT -> nativeBindDouble(connectionPtr, statementPtr, i + 1,
                        (arg as Number).toDouble())
                Cursor.FIELD_TYPE_BLOB -> nativeBindBlob(connectionPtr, statementPtr, i + 1, arg as ByteArray)
                Cursor.FIELD_TYPE_STRING -> if (arg is Boolean)
                {
                    // Provide compatibility with legacy applications which may pass
                    // Boolean values in bind args.
                    nativeBindLong(connectionPtr, statementPtr, i + 1,
                            (if (arg) 1 else 0).toLong())
                }
                else
                {
                    nativeBindString(connectionPtr, statementPtr, i + 1, arg.toString())
                }
                else -> if (arg is Boolean)
                {
                    nativeBindLong(connectionPtr, statementPtr, i + 1, (if (arg) 1 else 0).toLong())
                }
                else
                {
                    nativeBindString(connectionPtr, statementPtr, i + 1, arg.toString())
                }
            }
        }
    }

    /**
     * Dumps debugging information about this connection.
     *
     * @param printer The printer to receive the dump, not null.
     * @param verbose True to dump more verbose information.
     */
    fun dump(printer:Printer, verbose:Boolean) {
        dumpUnsafe(printer, verbose)
    }
    /**
     * Dumps debugging information about this connection, in the case where the
     * caller might not actually own the connection.
     *
     * This function is written so that it may be called by a thread that does not
     * own the connection. We need to be very careful because the connection state is
     * not synchronized.
     *
     * At worst, the method may return stale or slightly wrong data, however
     * it should not crash. This is ok as it is only used for diagnostic purposes.
     *
     * @param printer The printer to receive the dump, not null.
     * @param verbose True to dump more verbose information.
     */
    internal fun dumpUnsafe(printer:Printer, verbose:Boolean) {

        if (verbose)
        {
            printer.println(" connectionPtr: 0x" + getConnectionPtr(nativeDataId).toString(16))
        }

        mRecentOperations.dump(printer, verbose)
        if (verbose)
        {
            //TODO Maybe?
//            mPreparedStatementCache.dump(printer)
        }
    }

    /**
     * Describes the currently executing operation, in the case where the
     * caller might not actually own the connection.
     *
     * This function is written so that it may be called by a thread that does not
     * own the connection. We need to be very careful because the connection state is
     * not synchronized.
     *
     * At worst, the method may return stale or slightly wrong data, however
     * it should not crash. This is ok as it is only used for diagnostic purposes.
     *
     * @return A description of the current operation including how long it has been running,
     * or null if none.
     */
    internal fun describeCurrentOperationUnsafe():String? {
        return mRecentOperations.describeCurrentOperation()
    }
    /**
     * Collects statistics about database connection memory usage.
     *
     * @param dbStatsList The list to populate.
     */
    internal fun collectDbStats(dbStatsList:ArrayList<SQLiteDebug.DbStats>) {
        // Get information about the main database.
        val lookaside = nativeGetDbLookaside(getConnectionPtr(nativeDataId))
        var pageCount:Long = 0
        var pageSize:Long = 0
        try
        {
            pageCount = executeForLong("PRAGMA page_count;", null)
            pageSize = executeForLong("PRAGMA page_size;", null)
        }
        catch (ex:SQLiteException) {
            // Ignore.
        }
        dbStatsList.add(getMainDbStatsUnsafe(lookaside, pageCount, pageSize))
        // Get information about attached databases.
        // We ignore the first row in the database list because it corresponds to
        // the main database which we have already described.
        val window = CursorWindow("collectDbStats")
        try
        {
            executeForCursorWindow("PRAGMA database_list;", null, window, 0, 0, false)
            for (i in 1 until window.numRows)
            {
                val name = window.getString(i, 1)
                val path = window.getString(i, 2)
                pageCount = 0
                pageSize = 0
                try
                {
                    pageCount = executeForLong("PRAGMA $name.page_count;", null)
                    pageSize = executeForLong("PRAGMA $name.page_size;", null)
                }
                catch (ex:SQLiteException) {
                    // Ignore.
                }
                var label = " (attached) $name"
                if (!path.isEmpty())
                {
                    label += ": $path"
                }
                dbStatsList.add(SQLiteDebug.DbStats(label, pageCount, pageSize, 0, 0, 0, 0))
            }
        }
        catch (ex:SQLiteException) {
            // Ignore.
        }
        finally
        {
            window.close()
        }
    }
    /**
     * Collects statistics about database connection memory usage, in the case where the
     * caller might not actually own the connection.
     *
     * @return The statistics object, never null.
     */
    internal fun collectDbStatsUnsafe(dbStatsList:ArrayList<SQLiteDebug.DbStats>) {
        dbStatsList.add(getMainDbStatsUnsafe(0, 0, 0))
    }
    private fun getMainDbStatsUnsafe(lookaside:Int, pageCount:Long, pageSize:Long):SQLiteDebug.DbStats {
        // The prepared statement cache is thread-safe so we can access its statistics
        // even if we do not own the database connection.
        var label = mConfiguration.path

        //TODO Replace with real numbers if desired...
        return SQLiteDebug.DbStats(label, pageCount, pageSize, lookaside,
                -1,
                -1,
                -1)
    }

    private fun obtainPreparedStatement(sql:String,
                                        statementPtr:Long,
                                        numParameters:Int,
                                        type:Int,
                                        readOnly:Boolean,
                                        inCache:Boolean):NativePreparedStatement {
        return NativePreparedStatement(sql,
                statementPtr,
                numParameters,
                type,
                readOnly,
                inCache).freeze()
    }

    /*private inner class
    PreparedStatementCache(size:Int):LruCache<String, PreparedStatement>(size) {
        override fun entryRemoved(evicted:Boolean, key:String,
                                  oldValue:PreparedStatement?, newValue:PreparedStatement?) {
            if(oldValue != null) {
                oldValue.mInCache = false
                if (!oldValue.mInUse) {

                    finalizePreparedStatement(oldValue)
                }
            }
        }

        fun dump(printer:Printer) {
            printer.println(" Prepared statement cache:")
            val cache = snapshot()
            if (!cache.isEmpty())
            {
                var i = 0
                for (entry in cache.entries)
                {
                    val statement = entry.value!!
                    if (statement.mInCache)
                    { // might be false due to a race with entryRemoved
                        val sql = entry.key
                        printer.println((" " + i + ": statementPtr=0x"
                                + statement.mStatementPtr.toString(16)
                                + ", numParameters=" + statement.mNumParameters
                                + ", type=" + statement.mType
                                + ", readOnly=" + statement.mReadOnly
                                + ", sql=\"" + trimSqlForDisplay(sql) + "\""))
                    }
                    i += 1
                }
            }
            else
            {
                printer.println(" <none>")
            }
        }
    }*/
    private class OperationLog {
        //        private val mOperations = arrayOfNulls<Operation>(MAX_RECENT_OPERATIONS)
        private var mIndex:Int = 0
        private var mGeneration:Int = 0
        fun beginOperation(kind:String, sql:String?, bindArgs:Array<Any?>?):Int {
            return 0
        }
        fun failOperation(cookie:Int, ex:Exception) {

        }
        fun endOperation(cookie:Int) {

        }
        fun endOperationDeferLog(cookie:Int):Boolean {
            return endOperationDeferLogLocked(cookie)
        }
        fun logOperation(cookie:Int, detail:String) {

        }

        private fun endOperationDeferLogLocked(cookie:Int):Boolean {
            return false
        }

        private fun logOperationLocked(cookie:Int, detail:String?) {

        }
        private fun newOperationCookieLocked(index:Int):Int {
            return 0
        }

        fun describeCurrentOperation():String? {
            return null
        }

        fun dump(printer:Printer, verbose:Boolean) {

        }
        companion object {
            private val MAX_RECENT_OPERATIONS = 20
            private val COOKIE_GENERATION_SHIFT = 8
            private val COOKIE_INDEX_MASK = 0xff
        }
    }

    companion object {
        private val TAG = "SQLiteConnection"
        private val DEBUG = true
        private val EMPTY_STRING_ARRAY = arrayOf<String>()
        private val EMPTY_BYTE_ARRAY = ByteArray(0)
        //        private val TRIM_SQL_PATTERN = Pattern.compile("[\\s]*\\n+[\\s]*")
        @SymbolName("Android_Database_SQLiteConnection_nativeOpen")
        private external fun nativeOpen(path:String, openFlags:Int, label:String,
                                        enableTrace:Boolean, enableProfile:Boolean,
                                        lookasideSlotSize:Int, lookasideSlotCount:Int):Long
        @SymbolName("Android_Database_SQLiteConnection_nativeClose")
        private external fun nativeClose(connectionPtr:Long)
        //        @SymbolName("")
//        private external fun nativeRegisterLocalizedCollators(connectionPtr:Long, locale:String)
        @SymbolName("Android_Database_SQLiteConnection_nativePrepareStatement")
        private external fun nativePrepareStatement(connectionPtr:Long, sql:String):Long

        @SymbolName("Android_Database_SQLiteConnection_nativeGetParameterCount")
        private external fun nativeGetParameterCount(connectionPtr:Long, statementPtr:Long):Int
        @SymbolName("Android_Database_SQLiteConnection_nativeIsReadOnly")
        private external fun nativeIsReadOnly(connectionPtr:Long, statementPtr:Long):Boolean
        @SymbolName("Android_Database_SQLiteConnection_nativeGetColumnCount")
        private external fun nativeGetColumnCount(connectionPtr:Long, statementPtr:Long):Int
        @SymbolName("Android_Database_SQLiteConnection_nativeGetColumnName")
        private external fun nativeGetColumnName(connectionPtr:Long, statementPtr:Long,
                                                 index:Int):String
        @SymbolName("Android_Database_SQLiteConnection_nativeBindNull")
        private external fun nativeBindNull(connectionPtr:Long, statementPtr:Long,
                                            index:Int)
        @SymbolName("Android_Database_SQLiteConnection_nativeBindLong")
        private external fun nativeBindLong(connectionPtr:Long, statementPtr:Long,
                                            index:Int, value:Long)
        @SymbolName("Android_Database_SQLiteConnection_nativeBindDouble")
        private external fun nativeBindDouble(connectionPtr:Long, statementPtr:Long,
                                              index:Int, value:Double)
        @SymbolName("Android_Database_SQLiteConnection_nativeBindString")
        private external fun nativeBindString(connectionPtr:Long, statementPtr:Long,
                                              index:Int, value:String)
        @SymbolName("Android_Database_SQLiteConnection_nativeBindBlob")
        private external fun nativeBindBlob(connectionPtr:Long, statementPtr:Long,
                                            index:Int, value:ByteArray)
        @SymbolName("Android_Database_SQLiteConnection_nativeResetStatementAndClearBindings")
        private external fun nativeResetStatementAndClearBindings(
                connectionPtr:Long, statementPtr:Long)
        @SymbolName("Android_Database_SQLiteConnection_nativeExecute")
        private external fun nativeExecute(connectionPtr:Long, statementPtr:Long)
        @SymbolName("Android_Database_SQLiteConnection_nativeExecuteForLong")
        private external fun nativeExecuteForLong(connectionPtr:Long, statementPtr:Long):Long
        @SymbolName("Android_Database_SQLiteConnection_nativeExecuteForString")
        private external fun nativeExecuteForString(connectionPtr:Long, statementPtr:Long):String
        @SymbolName("Android_Database_SQLiteConnection_nativeExecuteForChangedRowCount")
        private external fun nativeExecuteForChangedRowCount(connectionPtr:Long, statementPtr:Long):Int
        @SymbolName("Android_Database_SQLiteConnection_nativeExecuteForLastInsertedRowId")
        private external fun nativeExecuteForLastInsertedRowId(
                connectionPtr:Long, statementPtr:Long):Long
        @SymbolName("Android_Database_SQLiteConnection_nativeExecuteForCursorWindow")
        private external fun nativeExecuteForCursorWindow(
                connectionPtr:Long, statementPtr:Long, windowPtr:Long,
                startPos:Int, requiredPos:Int, countAllRows:Boolean):Long
        @SymbolName("Android_Database_SQLiteConnection_nativeGetDbLookaside")
        private external fun nativeGetDbLookaside(connectionPtr:Long):Int
        @SymbolName("Android_Database_SQLiteConnection_nativeCancel")
        private external fun nativeCancel(connectionPtr:Long)
        @SymbolName("Android_Database_SQLiteConnection_nativeResetCancel")
        private external fun nativeResetCancel(connectionPtr:Long, cancelable:Boolean)

        private fun canonicalizeSyncMode(value:String):String {
            if (value == "0")
            {
                return "OFF"
            }
            else if (value == "1")
            {
                return "NORMAL"
            }
            else if (value == "2")
            {
                return "FULL"
            }
            return value
        }

        private fun isCacheable(statementType:Int):Boolean {
            if ((statementType == DatabaseUtils.STATEMENT_UPDATE || statementType == DatabaseUtils.STATEMENT_SELECT))
            {
                return true
            }
            return false
        }

        private fun trimSqlForDisplay(sql:String):String {
            //TODO: Maybe fix
            return sql//TRIM_SQL_PATTERN.matcher(sql).replaceAll(" ")
        }
    }
}

private data class NativePreparedStatement(
        // The SQL from which the statement was prepared.
        val mSql:String,
        // The native sqlite3_stmt object pointer.
        // Lifetime is managed explicitly by the connection.
        val mStatementPtr:Long,
        // The number of parameters that the prepared statement has.
        val mNumParameters:Int,
        // The statement type.
        val mType:Int,
        // True if the statement is read-only.
        val mReadOnly:Boolean,
        val mInCache:Boolean
)