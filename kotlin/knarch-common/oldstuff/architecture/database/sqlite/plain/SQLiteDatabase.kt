package co.touchlab.kurgan.architecture.database.sqlite.plain

import co.touchlab.kurgan.architecture.ContentValues
import co.touchlab.kurgan.architecture.database.Cursor
import co.touchlab.kurgan.architecture.database.sqlite.SizzleSQLiteProgram
import co.touchlab.kurgan.architecture.database.support.SimpleSQLiteQuery
import co.touchlab.kurgan.architecture.database.support.SupportSQLiteQuery
import co.touchlab.kurgan.architecture.database.support.execDeleteStatement
import co.touchlab.kurgan.architecture.database.support.execUpdateStatement


expect interface CursorFactory{
    fun newCursor(db: SQLiteDatabase,
                  masterQuery: SQLiteCursorDriver,
                  editTable: String,
                  query: SQLiteQuery): Cursor
}

expect interface SQLiteTransactionListener{
    /**
     * Called immediately after the transaction begins.
     */
    fun onBegin()

    /**
     * Called immediately before commiting the transaction.
     */
    fun onCommit()

    /**
     * Called if the transaction is about to be rolled back.
     */
    fun onRollback()
}

expect interface SQLiteCursorDriver

/*
fun SQLiteDatabase.query(query: String, bindArgs: Array<Any?>?): Cursor = query(SimpleSQLiteQuery(query, bindArgs))
fun SQLiteDatabase.query(supportQuery: SupportSQLiteQuery): Cursor {
    return rawQueryWithFactory(
            object : CursorFactory {
                override fun newCursor(db: SQLiteDatabase, masterQuery: SQLiteCursorDriver, editTable: String, query: SQLiteQuery): Cursor {
                    supportQuery.bindTo(SizzleSQLiteProgram(query))
                    return SQLiteCursor(masterQuery, editTable, query)
                }
            },
            supportQuery.getSql(),
            arrayOfNulls(0),
            null)
}

fun SQLiteDatabase.insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long =
        insertWithOnConflict(table, null, values, conflictAlgorithm)

fun SQLiteDatabase.delete(table: String, whereClause: String?, whereArgs: Array<Any?>?): Int =
        execDeleteStatement(this, table, whereClause, whereArgs)

fun SQLiteDatabase.update(table: String, conflictAlgorithm: Int,
                          values: ContentValues, whereClause: String?, whereArgs: Array<Any?>?): Int =
        execUpdateStatement(this, table, conflictAlgorithm, values, whereClause, whereArgs)
*/

expect class SQLiteDatabase{


    fun beginTransaction():Unit
    fun beginTransactionNonExclusive():Unit
    fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener):Unit
    fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener):Unit
    fun endTransaction():Unit
    fun setTransactionSuccessful():Unit
    fun inTransaction(): Boolean
    fun isDbLockedByCurrentThread(): Boolean
    fun yieldIfContendedSafely(): Boolean
    fun yieldIfContendedSafely(sleepAfterYieldDelay: Long): Boolean
    fun getVersion():Int
    fun setVersion(value:Int)
    fun getMaximumSize():Long
    fun setMaximumSize(value:Long):Long
    fun getPageSize():Long
    fun setPageSize(value:Long)

    fun insertWithOnConflict(table: String, nullColumnHack:String?, contentValues: ContentValues, conflictAlgorithm: Int): Long

    fun execSQL(sql:String):Unit
    fun execSQL(sql:String, bindArgs:Array<Any?>):Unit

    fun rawQueryWithFactory(cursorFactory: CursorFactory, sql:String, selectionArgs:Array<String?>?, editTable: String?):Cursor

    //CursorFactory cursorFactory, String sql, String[] selectionArgs,
    //            String editTable

    fun isReadOnly():Boolean
    fun isOpen():Boolean
    fun needUpgrade(newVersion:Int):Boolean


    /**
     * Source docs tell me this can't be null, even if in memory.
     */
    fun getPath():String
    fun setMaxSqlCacheSize(cacheSize:Int):Unit
    fun setForeignKeyConstraintsEnabled(enable:Boolean):Unit
    fun enableWriteAheadLogging():Boolean
    fun disableWriteAheadLogging():Unit
    fun isWriteAheadLoggingEnabled():Boolean
    fun isDatabaseIntegrityOk():Boolean
    fun close():Unit

    fun compileStatement(sql:String):SQLiteStatement
}
