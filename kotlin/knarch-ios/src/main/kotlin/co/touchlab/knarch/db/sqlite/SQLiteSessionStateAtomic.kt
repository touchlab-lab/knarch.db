package co.touchlab.knarch.db.sqlite

import co.touchlab.kite.threads.*
import co.touchlab.knarch.db.CursorWindow
import konan.worker.*
import platform.Foundation.*
import co.touchlab.knarch.Log

class SQLiteSessionStateAtomic(dbConfig: SQLiteDatabaseConfiguration) {
//    private val atomicSession: Atomic<SQLiteSession> = Atomic(null)
    private val sessionDataId = createDataStore()
    private val sessionRecursiveLock = NSRecursiveLock()

    private fun openSession():SQLiteSession{
        return getDataPointer(sessionDataId)as SQLiteSession
    }

    private fun hasSession():Boolean{
        return getDataPointer(sessionDataId) != null
    }

    private fun putSession(session:SQLiteSession){
        putDataPointer(sessionDataId, session)
    }

    private val transLock = NSRecursiveLock()

    private fun transactionUnlock() {
        try {
            transLock.unlock()
        } catch (e: Exception) {
            //Presumably we weren't locked
            Log.w("SQLiteSessionStateAtomic", "Failed unlock", e)
        }
    }

    private val atomicDbConfig: Atomic<SQLiteDatabaseConfiguration> = Atomic({dbConfig.freeze()})

    fun dbLabel() = atomicDbConfig.accessForResult { conf -> conf!!.label }
    fun dbConfigUpdate(proc: (conf: SQLiteDatabaseConfiguration?) -> SQLiteDatabaseConfiguration) {
        atomicDbConfig.accessUpdate(proc)
    }

    fun dbConfigCopy(): SQLiteDatabaseConfiguration = atomicDbConfig.accessForResult { conf -> conf!!.freeze() }
    fun dbReadOnlyLocked(): Boolean = atomicDbConfig.accessForResult { (it!!.openFlags and SQLiteDatabase.OPEN_READ_MASK) == SQLiteDatabase.OPEN_READONLY }
    fun dbInMemoryDb(): Boolean = atomicDbConfig.accessForResult { it!!.isInMemoryDb() }
    fun dbOpenFlags(): Int = atomicDbConfig.accessForResult { it!!.openFlags }
    fun dbPath(): String = atomicDbConfig.accessForResult { it!!.path }
    fun dbForeignKeyConstraintsEnabled(): Boolean = atomicDbConfig.accessForResult { it!!.foreignKeyConstraintsEnabled }

    fun hasTransaction(): Boolean {
        sessionRecursiveLock.lock()
        try {
            return __hasTransaction()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __hasTransaction():Boolean = openSession().hasTransaction()

    fun hasNestedTransaction(): Boolean {
        sessionRecursiveLock.lock()
        try {
            return __hasNestedTransaction()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __hasNestedTransaction():Boolean = openSession().hasNestedTransaction()

    fun hasConnection(): Boolean {
        sessionRecursiveLock.lock()
        try {
            return __hasConnection()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __hasConnection():Boolean = hasSession()

    fun beginTransaction(transactionMode: Int,
                         transactionListener: SQLiteTransactionListener?,
                         connectionFlags: Int) {
        transLock.lock()
        sessionRecursiveLock.lock()

        try {
            try {
                __beginTransaction(transactionMode, transactionListener, connectionFlags)
            } finally {
                sessionRecursiveLock.unlock()
            }
        } catch (e: Exception) {
            transactionUnlock()
            throw e
        }
    }

    private fun __beginTransaction(transactionMode: Int,
                                   transactionListener: SQLiteTransactionListener?,
                                   connectionFlags: Int){
        transactionMode.freeze()
        transactionListener.freeze()
        connectionFlags.freeze()
        openSession().beginTransaction(transactionMode, transactionListener, connectionFlags)
    }

    fun setTransactionSuccessful() {
        sessionRecursiveLock.lock()
        try {
            __setTransactionSuccessful()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __setTransactionSuccessful(){
        openSession().setTransactionSuccessful()
    }

    fun endTransaction() {
        sessionRecursiveLock.lock()
        try {
            __endTransaction()
        } finally {
            sessionRecursiveLock.unlock()
            transactionUnlock()
        }
    }

    private fun __endTransaction()
    {
        openSession().endTransaction()
    }

    fun prepare(sql: String, connectionFlags: Int,
                outStatementInfo: SQLiteStatementInfo?) {
        freezeParams(sql)
        connectionFlags.freeze()
        sessionRecursiveLock.lock()
        try {
            __prepare(sql, connectionFlags, outStatementInfo)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __prepare(sql: String, connectionFlags: Int,
                          outStatementInfo: SQLiteStatementInfo?){
        openSession().prepare(sql, connectionFlags, outStatementInfo)
    }

    fun execute(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int) {
        freezeParams(sql, bindArgs)
        sessionRecursiveLock.lock()
        try {
            __execute(sql, bindArgs, connectionFlags)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __execute(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int){
        openSession().execute(sql, bindArgs, connectionFlags)
    }

    fun executeForLong(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Long {
        freezeParams(sql, bindArgs)
        sessionRecursiveLock.lock()
        try {
            return __executeForLong(sql, bindArgs, connectionFlags)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __executeForLong(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Long{
        return openSession().executeForLong(sql, bindArgs, connectionFlags)
    }

    fun executeForString(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): String? {
        freezeParams(sql, bindArgs)
        sessionRecursiveLock.lock()
        try {
            return __executeForString(sql, bindArgs, connectionFlags)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __executeForString(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): String? {
        return openSession().executeForString(sql, bindArgs, connectionFlags)
    }

    fun executeForChangedRowCount(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Int {
        freezeParams(sql, bindArgs)
        sessionRecursiveLock.lock()
        try {
            return __executeForChangedRowCount(sql, bindArgs, connectionFlags)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __executeForChangedRowCount(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Int {
        return openSession().executeForChangedRowCount(sql, bindArgs, connectionFlags)
    }

    fun executeForLastInsertedRowId(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Long {
        freezeParams(sql, bindArgs)
        sessionRecursiveLock.lock()
        try {
            return __executeForLastInsertedRowId(sql, bindArgs, connectionFlags)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __executeForLastInsertedRowId(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Long {
        return openSession().executeForLastInsertedRowId(sql, bindArgs, connectionFlags)
    }

    fun executeForCursorWindow(sql: String, bindArgs: Array<Any?>?,
                               window: CursorWindow, startPos: Int, requiredPos: Int, countAllRows: Boolean,
                               connectionFlags: Int): Int {
        freezeParams(sql, bindArgs)
        sessionRecursiveLock.lock()
        try {
            return __executeForCursorWindow(sql, bindArgs, window, startPos, requiredPos, countAllRows, connectionFlags)
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __executeForCursorWindow(sql: String, bindArgs: Array<Any?>?,
                                         window: CursorWindow, startPos: Int, requiredPos: Int, countAllRows: Boolean,
                                         connectionFlags: Int): Int {
        return openSession().executeForCursorWindow(sql, bindArgs, window, startPos, requiredPos, countAllRows, connectionFlags)
    }

    fun closeConnection() {
        sessionRecursiveLock.lock()
        try {
            __closeConnection()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __closeConnection() {
        openSession().mConnection.close()
    }

    fun checkOpenConnection(){
        sessionRecursiveLock.lock()
        try {
            __checkOpenConnection()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    private fun __checkOpenConnection(){
        if(!hasConnection())
            __openConnection()
    }

    fun openConnection() {
        sessionRecursiveLock.lock()
        try {
            __openConnection()
        } finally {
            sessionRecursiveLock.unlock()
        }
    }

    fun __openConnection()
    {
        val configCopy = dbConfigCopy()
        putSession(SQLiteSession(SQLiteConnection(configCopy)))
    }

    private fun freezeParams(sql: String, bindArgs: Array<Any?>? = null) {
        sql.freeze()
        if (bindArgs != null)
            bindArgs.freeze()
    }

    data class TransactionInfo(val transactionMode: Int,
                               val transactionListener: SQLiteTransactionListener?,
                               val connectionFlags: Int)
}


