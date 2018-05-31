package co.touchlab.knarch.db.sqlite

import co.touchlab.kite.threads.Atomic
import co.touchlab.kite.threads.access
import co.touchlab.kite.threads.accessForResult
import co.touchlab.kite.threads.accessWith
import co.touchlab.knarch.db.CursorWindow
import konan.worker.*

class SQLiteSessionAtomic(producer:()->SQLiteSession){
    val atomicSession:Atomic<SQLiteSession> = Atomic(producer.invoke())

    fun hasTransaction():Boolean = atomicSession.accessForResult { s -> s.hasTransaction() }
    fun hasNestedTransaction():Boolean = atomicSession.accessForResult { s -> s.hasNestedTransaction() }
    fun hasConnection():Boolean = atomicSession.accessForResult { s -> s.hasConnection() }
    fun beginTransaction(transactionMode:Int,
                         transactionListener:SQLiteTransactionListener?,
                         connectionFlags:Int){
        atomicSession.accessWith({TransactionInfo(transactionMode, transactionListener, connectionFlags).freeze()})
        { s, w -> s.beginTransaction(w.transactionMode, w.transactionListener, w.connectionFlags) }
    }
    fun setTransactionSuccessful(){
        atomicSession.access { s -> s.setTransactionSuccessful() }
    }
    fun endTransaction(){
        atomicSession.access { s -> s.endTransaction() }
    }
    fun prepare(sql:String, connectionFlags:Int,
                outStatementInfo:SQLiteStatementInfo?){
        atomicSession.access {s ->
            s.prepare(sql, connectionFlags, outStatementInfo)
        }
    }
    fun execute(sql:String, bindArgs:Array<Any?>?, connectionFlags:Int){
        atomicSession.access { s ->
            s.execute(sql, bindArgs, connectionFlags)
        }
    }

    fun executeForLong(sql:String, bindArgs:Array<Any?>?, connectionFlags:Int):Long = atomicSession.accessForResult { s ->
        s.executeForLong(sql, bindArgs, connectionFlags)
    }

    fun executeForString(sql:String, bindArgs:Array<Any?>?, connectionFlags:Int):String? = atomicSession.accessForResult { s ->
        s.executeForString(sql, bindArgs, connectionFlags)
    }

    fun executeForChangedRowCount(sql:String, bindArgs:Array<Any?>?, connectionFlags:Int):Int =
            atomicSession.accessForResult { s -> s.executeForChangedRowCount(sql, bindArgs, connectionFlags) }
    fun executeForLastInsertedRowId(sql:String, bindArgs:Array<Any?>?, connectionFlags:Int):Long =
            atomicSession.accessForResult { s -> s.executeForLastInsertedRowId(sql, bindArgs, connectionFlags) }
    fun executeForCursorWindow(sql:String, bindArgs:Array<Any?>?,
                               window: CursorWindow, startPos:Int, requiredPos:Int, countAllRows:Boolean,
                               connectionFlags:Int):Int =
            atomicSession.accessForResult { s -> s.executeForCursorWindow(sql, bindArgs, window, startPos, requiredPos, countAllRows, connectionFlags)}

    fun closeConnection(){
        atomicSession.access { s ->
            s.mConnection.close()
        }
    }
    data class TransactionInfo(val transactionMode:Int,
                               val transactionListener:SQLiteTransactionListener?,
                               val connectionFlags:Int)
}

