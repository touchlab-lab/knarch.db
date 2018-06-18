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

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.CursorWindow
import co.touchlab.knarch.db.DatabaseUtils
import platform.Foundation.*
import konan.worker.*

/**
 * In Android, the session is part of the stack that facilitates multithreaded sqlite access. Currently, due
 * to KN's restrictive threading rules, we're serializing db access. As this code matures, we *may*
 * refactor this to approximate Android's support, but currently the session is the gatekeeper to make
 * sure all access to the underlying connection is single threaded.
 *
 * This class can be shared across threads. In doing so, you should assume it is frozen. No state
 * here or below should change, with few exceptions kept in specialized and specially designated structures,
 * outside of the normal KN memory management lifecycle.
 */
class SQLiteSession(private val mConnection: SQLiteConnection) {
    private val sessionRecursiveLock = NSRecursiveLock()
    private val transLock = NSRecursiveLock()

    internal fun checkOpenConnection(){
        withLock {
            if(!mConnection.hasConnection())
                mConnection.open()
        }
    }

    private fun transactionUnlock() {
        try {
            transLock.unlock()
        } catch (e: Exception) {
            //Presumably we weren't locked
            Log.w("SQLiteSessionStateAtomic", "Failed unlock", e)
        }
    }

    private fun <T> withLock(proc:() -> T):T{
        sessionRecursiveLock.lock()
        try {
            return proc.invoke()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    /**
     * Returns true if the session has a transaction in progress.
     *
     * @return True if the session has a transaction in progress.
     */
    fun hasTransaction():Boolean {
        return withLock { mConnection.getTransaction() != null }
    }

    fun hasConnection(): Boolean = mConnection.hasConnection()

    fun getDbConfig():SQLiteDatabaseConfiguration = mConnection.getDbConfig()

    fun putDbConfig(config:SQLiteDatabaseConfiguration) {
        mConnection.putDbConfig(config)
    }

    /**
     * Begins a transaction.
     * <p>
     * Transactions may nest. If the transaction is not in progress,
     * then a database connection is obtained and a new transaction is started.
     * Otherwise, a nested transaction is started.
     * </p><p>
     * Each call to {@link #beginTransaction} must be matched exactly by a call
     * to {@link #endTransaction}. To mark a transaction as successful,
     * call {@link #setTransactionSuccessful} before calling {@link #endTransaction}.
     * If the transaction is not successful, or if any of its nested
     * transactions were not successful, then the entire transaction will
     * be rolled back when the outermost transaction is ended.
     * </p>
     *
     * @param transactionMode The transaction mode. One of: {@link #TRANSACTION_MODE_DEFERRED},
     * {@link #TRANSACTION_MODE_IMMEDIATE}, or {@link #TRANSACTION_MODE_EXCLUSIVE}.
     * Ignored when creating a nested transaction.
     * @param transactionListener The transaction listener, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation. Refer to {@link SQLiteConnectionPool}.
     * @throws IllegalStateException if {@link #setTransactionSuccessful} has already been
     * called for the current transaction.
     * @throws SQLiteException if an error occurs.
     *
     *
     * @see #setTransactionSuccessful
     * @see #yieldTransaction
     * @see #endTransaction
     */
    fun beginTransaction(transactionMode:Int,
                         transactionListener:SQLiteTransactionListener?) {
        transLock.lock()
        try {
            withLock {
                throwIfTransactionMarkedSuccessful()
                beginTransactionUnchecked(transactionMode, transactionListener)
            }
        } catch (e: Throwable) {
            transactionUnlock()
            throw e
        }
    }

    private fun beginTransactionUnchecked(transactionMode:Int,
                                          transactionListener:SQLiteTransactionListener?) {
        val mTransactionStack = mConnection.getTransaction()
        
        // Set up the transaction such that we can back out safely
        // in case we fail part way.
        if (mTransactionStack == null)
        {
            // Execute SQL might throw a runtime exception.

            when (transactionMode) {
                TRANSACTION_MODE_IMMEDIATE -> mConnection.execute("BEGIN IMMEDIATE;", null) // might throw
                TRANSACTION_MODE_EXCLUSIVE -> mConnection.execute("BEGIN EXCLUSIVE;", null) // might throw
                else -> mConnection.execute("BEGIN;", null) // might throw
            }
        }
        // Listener might throw a runtime exception.
        if (transactionListener != null)
        {
            try
            {
                transactionListener.onBegin() // might throw
            }
            catch (ex:RuntimeException) {
                if (mTransactionStack == null)
                {
                    mConnection.execute("ROLLBACK;", null) // might throw
                }
                throw ex
            }
        }
        // Bookkeeping can't throw, except an OOM, which is just too bad...
        val transaction = obtainTransaction(transactionMode, transactionListener)
        transaction.mParent = mTransactionStack
        mConnection.putTransaction(transaction)
    }

    /**
     * Marks the current transaction as having completed successfully.
     * <p>
     * This method can be called at most once between {@link #beginTransaction} and
     * {@link #endTransaction} to indicate that the changes made by the transaction should be
     * committed. If this method is not called, the changes will be rolled back
     * when the transaction is ended.
     * </p>
     *
     * @throws IllegalStateException if there is no current transaction, or if
     * {@link #setTransactionSuccessful} has already been called for the current transaction.
     *
     * @see #beginTransaction
     * @see #endTransaction
     */
    fun setTransactionSuccessful() {
        withLock {
            throwIfNoTransaction()
            throwIfTransactionMarkedSuccessful()
            mConnection.getTransaction()!!.mMarkedSuccessful = true
        }
    }

    /**
     * Ends the current transaction and commits or rolls back changes.
     * <p>
     * If this is the outermost transaction (not nested within any other
     * transaction), then the changes are committed if {@link #setTransactionSuccessful}
     * was called or rolled back otherwise.
     * </p><p>
     * This method must be called exactly once for each call to {@link #beginTransaction}.
     * </p>
     *
     * @throws IllegalStateException if there is no current transaction.
     * @throws SQLiteException if an error occurs.
     *
     *
     * @see #beginTransaction
     * @see #setTransactionSuccessful
     * @see #yieldTransaction
     */
    fun endTransaction() {
        try {
            withLock {
                //*Always* call, even if we got here some weird way and there's no lock
                throwIfNoTransaction()
                endTransactionUnchecked( false)
            }
        } finally {
            transactionUnlock()
        }
    }

    private fun endTransactionUnchecked(yielding:Boolean) {
        val top = mConnection.getTransaction()
        var successful = (top!!.mMarkedSuccessful || yielding) && !top.mChildFailed
        var listenerException:RuntimeException? = null
        val listener = top.mListener
        if (listener != null)
        {
            try
            {
                if (successful)
                {
                    listener.onCommit() // might throw
                }
                else
                {
                    listener.onRollback() // might throw
                }
            }
            catch (ex:RuntimeException) {
                listenerException = ex
                successful = false
            }
        }
        mConnection.putTransaction(top.mParent)

        val transStack = mConnection.getTransaction()
        if (transStack != null)
        {
            if (!successful)
            {
                transStack.mChildFailed = true
            }
        }
        else
        {
            if (successful)
            {
                mConnection.execute("COMMIT;", null) // might throw
            }
            else
            {
                mConnection.execute("ROLLBACK;", null) // might throw
            }
        }
        if (listenerException != null)
        {
            throw listenerException
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
     * then it will be stored in the cache for later and reused if possible.
     * </p>
     *
     * @param sql The SQL statement to prepare.
     * @param outStatementInfo The {@link SQLiteStatementInfo} object to populate
     * with information about the statement, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error.
     *
     */
    fun prepare(sql:String, outStatementInfo:SQLiteStatementInfo?) {
        withLock {
            mConnection.prepare(sql, outStatementInfo) // might throw
        }
    }
    
    /**
     * Executes a statement that does not return a result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    fun execute(sql:String, bindArgs:Array<Any?>?) {
        withLock {
            if (!executeSpecial(sql)) {
                mConnection.execute(sql, bindArgs) // might throw
            }
        }
    }
    
    /**
     * Executes a statement that returns a single <code>long</code> result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     *
     * @return The value of the first column in the first row of the result set
     * as a <code>long</code>, or zero if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    fun executeForLong(sql:String, bindArgs:Array<Any?>?):Long = withLock {
        if (executeSpecial(sql))
        {
            0
        }
        else {
            mConnection.executeForLong(sql, bindArgs)
        } // might throw
    }

    /**
     * Executes a statement that returns a single {@link String} result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation. Refer to {@link SQLiteConnectionPool}.
     * @return The value of the first column in the first row of the result set
     * as a <code>String</code>, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    fun executeForString(sql: String, bindArgs: Array<Any?>?): String? = withLock {
        if (executeSpecial(sql)) {
            null
        } else {
            mConnection.executeForString(sql, bindArgs) // might throw
        }
    }

    /**
     * Executes a statement that returns a count of the number of rows
     * that were changed. Use for UPDATE or DELETE SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     *
     * @return The number of rows that were changed.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    fun executeForChangedRowCount(sql: String, bindArgs: Array<Any?>?): Int =
            withLock {
                if (executeSpecial(sql)) {
                    0
                } else {
                    mConnection.executeForChangedRowCount(sql, bindArgs) // might throw
                }
            }

    /**
     * Executes a statement that returns the row id of the last row inserted
     * by the statement. Use for INSERT SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation. Refer to {@link SQLiteConnectionPool}.
     * @return The row id of the last row that was inserted, or 0 if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    fun executeForLastInsertedRowId(sql:String, bindArgs:Array<Any?>?):Long =
            withLock {
                if (executeSpecial(sql))
                {
                    0
                }else {
                    mConnection.executeForLastInsertedRowId(sql, bindArgs) // might throw
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
     *
     * @return The number of rows that were counted during query execution. Might
     * not be all rows in the result set unless <code>countAllRows</code> is true.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    fun executeForCursorWindow(sql:String, bindArgs:Array<Any?>?,
                               window:CursorWindow, startPos:Int, requiredPos:Int, countAllRows:Boolean
                               ):Int =
            withLock {
                if (executeSpecial(sql))
                {
                    window.clear()
                    0
                }
                else {
                    mConnection.executeForCursorWindow(sql, bindArgs,
                            window, startPos, requiredPos, countAllRows) // might throw
                }
            }


    /**
     * Performs special reinterpretation of certain SQL statements such as "BEGIN",
     * "COMMIT" and "ROLLBACK" to ensure that transaction state invariants are
     * maintained.
     *
     * This function is mainly used to support legacy apps that perform their
     * own transactions by executing raw SQL rather than calling {@link #beginTransaction}
     * and the like.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation. Refer to {@link SQLiteConnectionPool}.
     * @return True if the statement was of a special form that was handled here,
     * false otherwise.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     *
     */
    private fun executeSpecial(sql:String):Boolean {
        val type = DatabaseUtils.getSqlStatementType(sql)
        when (type) {
            DatabaseUtils.STATEMENT_BEGIN -> {
                beginTransaction(TRANSACTION_MODE_EXCLUSIVE, null)
                return true
            }
            DatabaseUtils.STATEMENT_COMMIT -> {
                setTransactionSuccessful()
                endTransaction()
                return true
            }
            DatabaseUtils.STATEMENT_ABORT -> {
                endTransaction()
                return true
            }
        }
        return false
    }

    fun closeConnection() {
        withLock {
            mConnection.close()
        }
    }

    private fun throwIfNoTransaction() {
        if (mConnection.getTransaction() == null)
        {
            throw IllegalStateException(("Cannot perform this operation because there is no current transaction."))
        }
    }

    private fun throwIfTransactionMarkedSuccessful() {
        val mTransactionStack= mConnection.getTransaction()
        if (mTransactionStack != null && mTransactionStack.mMarkedSuccessful)
        {
            throw IllegalStateException(("Cannot perform this operation because "
                    + "the transaction has already been marked successful. The only "
                    + "thing you can do now is call endTransaction()."))
        }
    }

    private fun obtainTransaction(mode:Int, listener:SQLiteTransactionListener?):Transaction {
        val transaction  = Transaction()
        transaction.mMode = mode
        transaction.mListener = listener
        return transaction
    }

    internal class Transaction {
        var mParent:Transaction? = null
        var mMode:Int = 0
        var mListener:SQLiteTransactionListener? = null
        var mMarkedSuccessful:Boolean = false
        var mChildFailed:Boolean = false
    }

    companion object {
        /**
         * Transaction mode: Immediate.
         * <p>
         * When an immediate transaction begins, the session acquires a
         * <code>RESERVED</code> lock.
         * </p><p>
         * While holding a <code>RESERVED</code> lock, this session is allowed to read
         * or write but other sessions are only allowed to read.
         * </p><p>
         * Corresponds to the SQLite <code>BEGIN IMMEDIATE</code> transaction mode.
         * </p>
         */
        val TRANSACTION_MODE_IMMEDIATE = 1
        /**
         * Transaction mode: Exclusive.
         * <p>
         * When an exclusive transaction begins, the session acquires an
         * <code>EXCLUSIVE</code> lock.
         * </p><p>
         * While holding an <code>EXCLUSIVE</code> lock, this session is allowed to read
         * or write but no other sessions are allowed to access the database.
         * </p><p>
         * Corresponds to the SQLite <code>BEGIN EXCLUSIVE</code> transaction mode.
         * </p>
         */
        val TRANSACTION_MODE_EXCLUSIVE = 2
    }
}