/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * CHANGE NOTICE: File modified by Touchlab Inc to port to Kotlin and generally prepare for Kotlin/Native
 *
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

package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.db.Cursor
import co.touchlab.knarch.db.CursorWindow
import co.touchlab.knarch.db.DatabaseUtils
import co.touchlab.knarch.other.Printer
import co.touchlab.knarch.Log
import co.touchlab.stately.collections.frozenLruCache
import kotlin.native.*
import kotlin.native.concurrent.*

class SQLiteConnection() {
    // The recent operations log.
    private val mRecentOperations = OperationLog()
    private val connectionPtr = AtomicLong(0L)

    private val dbConfigAtom:AtomicReference<SQLiteDatabaseConfiguration?> = AtomicReference(null)
    private val transactionAtom:AtomicReference<SQLiteSession.Transaction?> = AtomicReference(null)
    private val statementCache = AtomicReference(
            frozenLruCache<String, NativePreparedStatement>(0).freeze()
    )

    fun getDbConfig():SQLiteDatabaseConfiguration{
        val dbConfig = dbConfigAtom.value
        if(dbConfig == null)
            throw IllegalStateException("Accessing database configuration after database shutdown")
        else
            return dbConfig
    }

    /**
     * Store dbconfig in C++ structure. This will allow us to have some changed state across threads, but not break
     * KN rules around data management.
     *
     * To support this, the data itself must be frozen, which we do here. Also, the C++ structure itself enforces
     * a mutex lock to prevent thread timing issues.
     */
    fun putDbConfig(config:SQLiteDatabaseConfiguration){
        dbConfigAtom.value = config.freeze()
    }

    internal fun getTransaction():SQLiteSession.Transaction? = transactionAtom.value

    internal fun putTransaction(trans:SQLiteSession.Transaction?) {
        if(trans == null)
        {
            removeTransaction()
        }
        else
        {
            transactionAtom.value = trans.freeze()
        }
    }

    private fun removeTransaction(){
        transactionAtom.value = null
    }

    //Statement cache methods
    private fun cacheEvictAll(){
        statementCache.value.removeAll()
    }

    private fun cacheRemove(sql:String){
        statementCache.value.remove(sql)
    }

    private fun cacheGetStatement(sql:String):NativePreparedStatement{
        return statementCache.value.get(sql)!!
    }

    private fun cacheHasStatement(sql:String):Boolean{
        return statementCache.value.exists(sql)
    }

    private fun cachePutStatement(sql:String, stmt:NativePreparedStatement){
        if(!stmt.mInCache)
            throw IllegalStateException("Only mInCache goes in cache")
        statementCache.value.put(sql, stmt)
    }

    internal fun hasConnection() = connectionPtr.value != 0L

    // Closes the database closes and releases all of its associated resources.
    // Do not call methods on the connection after it is closed. It will probably crash.
    internal fun close() {
        dispose()
    }

    fun open(config:SQLiteDatabaseConfiguration) {
        val connectionPtrLong = nativeOpen(config.path, config.openFlags,
                config.label,
                SQLiteDebug.DEBUG_SQL_STATEMENTS, SQLiteDebug.DEBUG_SQL_TIME,
                config.lookasideSlotSize, config.lookasideSlotCount)

        statementCache.value = frozenLruCache<String, NativePreparedStatement>(config.maxSqlCacheSize) {
            nativeFinalizeStatement(connectionPtr.value, it.value.mStatementPtr)
        }.freeze()

        putDbConfig(config)
        connectionPtr.value = connectionPtrLong

        setPageSize()
        setForeignKeyModeFromConfiguration()
        setWalModeFromConfiguration()
        setJournalSizeLimit()
        setAutoCheckpointInterval()
        // setLocaleFromConfiguration();
        // Register custom functions.
    }

    private fun dispose() {

        val connectionPtrLong = connectionPtr.value

        if (connectionPtrLong != 0L)
        {
            val cookie = mRecentOperations.beginOperation("close", null, null)
            try
            {
                cacheEvictAll()
                nativeClose(connectionPtrLong)
                dbConfigAtom.value = null
                connectionPtr.value = 0
            }
            finally
            {
                mRecentOperations.endOperation(cookie)
            }
        }
    }

    private fun setPageSize() {
        val dbConfig = getDbConfig()
        if (!dbConfig.isInMemoryDb() && !dbConfig.isReadOnlyConnection())
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
        val dbConfig = getDbConfig()
        if (!dbConfig.isInMemoryDb() && !dbConfig.isReadOnlyConnection())
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
        val dbConfig = getDbConfig()
        if (!dbConfig.isInMemoryDb() && !dbConfig.isReadOnlyConnection())
        {
            val newValue = SQLiteGlobal.journalSizeLimit
            val value = executeForLong("PRAGMA journal_size_limit", null).toInt()
            if (value != newValue)
            {
                executeForLong("PRAGMA journal_size_limit=$newValue", null)
            }
        }
    }
    private fun setForeignKeyModeFromConfiguration() {
        if (!getDbConfig().isReadOnlyConnection())
        {
            val newValue = (if (getDbConfig().foreignKeyConstraintsEnabled) 1 else 0).toLong()
            val value = executeForLong("PRAGMA foreign_keys", null)
            if (value != newValue)
            {
                execute("PRAGMA foreign_keys=$newValue", null)
            }
        }
    }

    private fun setWalModeFromConfiguration() {
        val dbConfig = getDbConfig()
        if (!dbConfig.isInMemoryDb() && !dbConfig.isReadOnlyConnection())
        {
            if ((dbConfig.openFlags and SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING) != 0)
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
                    + getDbConfig().label + "' from '" + value + "' to '" + newValue
                    + "' because the database is locked. This usually means that "
                    + "there are other open connections to the database which prevents "
                    + "the database from enabling or disabling write-ahead logging mode. "
                    + "Proceeding without changing the journal mode."))
        }
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
            withPreparedStatement(sql){statement ->
                if (outStatementInfo != null)
                {
                    val connectionPtrLong = connectionPtr.value
                    outStatementInfo.numParameters = statement.mNumParameters
                    outStatementInfo.readOnly = statement.mReadOnly
                    val columnCount = nativeGetColumnCount(
                            connectionPtrLong, statement.mStatementPtr)
                    if (columnCount == 0)
                    {
                        outStatementInfo.columnNames = EMPTY_STRING_ARRAY
                    }
                    else
                    {
                        outStatementInfo.columnNames = Array(columnCount) { col -> nativeGetColumnName(
                                connectionPtrLong, statement.mStatementPtr, col)
                        }
                    }
                }
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
            withPreparedStatement(sql){
                statement ->
                bindArguments(statement, bindArgs)
                nativeExecute(connectionPtr.value, statement.mStatementPtr)
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
            return withPreparedStatement(sql){statement ->
                bindArguments(statement, bindArgs)
                nativeExecuteForLong(connectionPtr.value, statement.mStatementPtr)
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
            return withPreparedStatement(sql){statement ->
                bindArguments(statement, bindArgs)
                nativeExecuteForString(connectionPtr.value, statement.mStatementPtr)
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
            return withPreparedStatement(sql){statement ->
                bindArguments(statement, bindArgs)
                changedRows = nativeExecuteForChangedRowCount(
                        connectionPtr.value, statement.mStatementPtr)


                changedRows
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
            return withPreparedStatement(sql){statement ->
                bindArguments(statement, bindArgs)
                nativeExecuteForLastInsertedRowId(
                        connectionPtr.value, statement.mStatementPtr)
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
     * @return The number of rows that were counted during query execution. Might
     * not be all rows in the result set unless <code>countAllRows</code> is true.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     */
    fun executeForCursorWindow(sql:String,
                               bindArgs:Array<Any?>?,
                               window:CursorWindow,
                               startPos:Int,
                               requiredPos:Int,
                               countAllRows:Boolean):Int {
        //CursorWindow exposes some of its internals in this method. Not a huge fan, but it works.
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
                return withPreparedStatement(sql){statement ->
                    bindArguments(statement, bindArgs)

                    val result = nativeExecuteForCursorWindow(
                            connectionPtr.value, statement.mStatementPtr, window.getWindowCursorPtr(),
                            startPos, requiredPos, countAllRows)
                    actualPos = (result shr 32).toInt()
                    countedRows = result.toInt()
                    filledRows = window.numRows
                    window.startPosition = actualPos
                    countedRows
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

    private fun <T> withPreparedStatement(sql:String, proc:(statement:NativePreparedStatement) -> T):T{
        val statement = acquirePreparedStatement(sql)
        try {
            return proc.invoke(statement)
        }
        finally
        {
            releasePreparedStatement(statement)
        }
    }

    private fun acquirePreparedStatement(sql:String):NativePreparedStatement {
        if (cacheHasStatement(sql))
        {
            return cacheGetStatement(sql)
        }
        var statement:NativePreparedStatement? = null
        val statementPtr = nativePrepareStatement(connectionPtr.value, sql)
        try
        {
            val numParameters = nativeGetParameterCount(connectionPtr.value, statementPtr)
            val type = DatabaseUtils.getSqlStatementType(sql)
            val readOnly = nativeIsReadOnly(connectionPtr.value, statementPtr)
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
                nativeFinalizeStatement(connectionPtr.value, statementPtr)
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
                nativeResetStatementAndClearBindings(connectionPtr.value, statement.mStatementPtr)
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
            nativeFinalizeStatement(connectionPtr.value, statement.mStatementPtr)
        }
    }

    /**
     * Cancel an open operation.
     */
    fun cancel() {
        nativeCancel(connectionPtr.value)
    }

    private fun bindArguments(statement:NativePreparedStatement, bindArgs:Array<Any?>?) {
        val count = bindArgs?.size ?: 0
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
            val connectionPtr = connectionPtr.value
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
        val lookaside = nativeGetDbLookaside(connectionPtr.value)
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
        val window = CursorWindow()
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
        val label = getDbConfig().path

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

    private class OperationLog {
//                private val mOperations = frozenLruCache<Operation>(MAX_RECENT_OPERATIONS)
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

@SymbolName("Android_Database_SQLiteConnection_nativeFinalizeStatement")
private external fun nativeFinalizeStatement(connectionPtr:Long, statementPtr:Long)

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