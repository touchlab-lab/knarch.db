package co.touchlab.knarch.db.sqlite

import co.touchlab.kite.threads.*
import co.touchlab.knarch.db.CursorWindow
import konan.worker.*
import platform.Foundation.*
import co.touchlab.knarch.Log

class SQLiteSessionStateAtomic(session: SQLiteSession?, dbConfig: SQLiteDatabaseConfiguration) {
    private val atomicSession: Atomic<SQLiteSession?> = Atomic(session)
    private val transLock = NSRecursiveLock()

    private fun transactionUnlock() {
        try {
            transLock.unlock()
        } catch (e: Exception) {
            //Presumably we weren't locked
            Log.w("SQLiteSessionStateAtomic", "Failed unlock", e)
        }
    }

    private val atomicDbConfig: Atomic<SQLiteDatabaseConfiguration> = Atomic(dbConfig)

    fun dbLabel() = atomicDbConfig.accessForResult { conf -> conf.label }
    fun dbConfigUpdate(proc: (conf: SQLiteDatabaseConfiguration) -> SQLiteDatabaseConfiguration) {
        atomicDbConfig.accessUpdate(proc)
    }

    fun dbConfigCopy(): SQLiteDatabaseConfiguration = atomicDbConfig.accessForResult { conf -> conf.freeze() }
    fun dbReadOnlyLocked(): Boolean = atomicDbConfig.accessForResult { (it.openFlags and SQLiteDatabase.OPEN_READ_MASK) == SQLiteDatabase.OPEN_READONLY }
    fun dbInMemoryDb(): Boolean = atomicDbConfig.accessForResult { it.isInMemoryDb() }
    fun dbOpenFlags(): Int = atomicDbConfig.accessForResult { it.openFlags }
    fun dbPath(): String = atomicDbConfig.accessForResult { it.path }
    fun dbForeignKeyConstraintsEnabled(): Boolean = atomicDbConfig.accessForResult { it.foreignKeyConstraintsEnabled }

    fun hasTransaction(): Boolean = atomicSession.accessForResult { s ->
        if (s == null) throw IllegalStateException("Not connected to db")
        s.hasTransaction() ?: false
    }

    fun hasNestedTransaction(): Boolean = atomicSession.accessForResult { s ->
        if (s == null) throw IllegalStateException("Not connected to db")
        s.hasNestedTransaction() ?: false
    }

    fun hasConnection(): Boolean = atomicSession.accessForResult { s -> s != null }
    fun beginTransaction(transactionMode: Int,
                         transactionListener: SQLiteTransactionListener?,
                         connectionFlags: Int) {
        transLock.lock()
        try {
            atomicSession.accessWith({ TransactionInfo(transactionMode, transactionListener, connectionFlags).freeze() })
            { s, w ->
                if (s == null) throw IllegalStateException("Not connected to db")
                s.beginTransaction(w.transactionMode, w.transactionListener, w.connectionFlags)
            }
        } catch (e: Exception) {
            transactionUnlock()
            throw e
        }
    }

    fun setTransactionSuccessful() {
        atomicSession.access { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.setTransactionSuccessful()
        }
    }

    fun endTransaction() {
        try {
            atomicSession.access { s ->
                if (s == null) throw IllegalStateException("Not connected to db")
                s.endTransaction()
            }
        } finally {
            transactionUnlock()
        }
    }

    fun prepare(sql: String, connectionFlags: Int,
                outStatementInfo: SQLiteStatementInfo?) {
        freezeParams(sql)
        atomicSession.access { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.prepare(sql, connectionFlags, outStatementInfo)
        }
    }

    fun execute(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int) {
        freezeParams(sql, bindArgs)
        atomicSession.access { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.execute(sql, bindArgs, connectionFlags)
        }
    }

    fun executeForLong(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Long {
        freezeParams(sql, bindArgs)
        return atomicSession.accessForResult { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.executeForLong(sql, bindArgs, connectionFlags)
        }
    }

    fun executeForString(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): String? {
        freezeParams(sql, bindArgs)
        return atomicSession.accessForResult { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.executeForString(sql, bindArgs, connectionFlags)
        }
    }

    fun executeForChangedRowCount(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Int {
        freezeParams(sql, bindArgs)
        return atomicSession.accessForResult { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.executeForChangedRowCount(sql, bindArgs, connectionFlags)
        }
    }

    fun executeForLastInsertedRowId(sql: String, bindArgs: Array<Any?>?, connectionFlags: Int): Long {
        freezeParams(sql, bindArgs)
        return atomicSession.accessForResult { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.executeForLastInsertedRowId(sql, bindArgs, connectionFlags)
        }
    }

    fun executeForCursorWindow(sql: String, bindArgs: Array<Any?>?,
                               window: CursorWindow, startPos: Int, requiredPos: Int, countAllRows: Boolean,
                               connectionFlags: Int): Int {
        freezeParams(sql, bindArgs)
        return atomicSession.accessForResult { s ->
            if (s == null) throw IllegalStateException("Not connected to db")
            s.executeForCursorWindow(sql, bindArgs, window, startPos, requiredPos, countAllRows, connectionFlags)
        }
    }

    fun closeConnection() {
        atomicSession.accessUpdate { s ->
            if (s != null)
                s.mConnection.close()
            null
        }
    }

    fun openConnection() {
        val configCopy = dbConfigCopy()
        atomicSession.accessUpdate {
            SQLiteSession(SQLiteConnection(configCopy, 0, true))
        }
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


