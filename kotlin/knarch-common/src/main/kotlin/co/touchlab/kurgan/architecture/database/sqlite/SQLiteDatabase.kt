package co.touchlab.kurgan.architecture.database.sqlite

import co.touchlab.kurgan.architecture.database.ContentValues
import co.touchlab.kurgan.architecture.database.Cursor

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

fun SQLiteDatabase.insert(table: String, conflictAlgorithm: Int = CONFLICT_ABORT, values: ContentValues): Long =
        insertWithOnConflict(table, null, values, conflictAlgorithm)

fun SQLiteDatabase.update(table: String, conflictAlgorithm: Int = CONFLICT_ABORT,
                          values: ContentValues, whereClause: String? = null, whereArgs: Array<String>? = null): Int =
        updateWithOnConflict(table, values, whereClause, whereArgs, conflictAlgorithm)

expect class SQLiteDatabase{

    fun beginTransaction():Unit
    fun beginTransactionNonExclusive():Unit
    fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener):Unit
    fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener):Unit
    fun endTransaction():Unit
    fun setTransactionSuccessful():Unit
    fun inTransaction(): Boolean
    fun yieldIfContendedSafely(): Boolean
    fun yieldIfContendedSafely(sleepAfterYieldDelay: Long): Boolean
    fun getVersion():Int
    fun setVersion(value:Int)
    fun getMaximumSize():Long
    fun setMaximumSize(value:Long):Long
    fun getPageSize():Long
    fun setPageSize(value:Long)

    fun compileStatement(sql:String): SQLiteStatement

    fun query(distinct:Boolean=false, table:String, columns:Array<String>? = null, selection:String? = null,
              selectionArgs:Array<String>? = null, groupBy:String? = null, having:String? = null,
              orderBy:String? = null, limit:String? = null):Cursor

    fun insertWithOnConflict(table: String, nullColumnHack:String?, contentValues: ContentValues, conflictAlgorithm: Int): Long
    fun updateWithOnConflict(table: String, values:ContentValues, whereClause:String?, whereArgs:Array<String>?, conflictAlgorithm:Int): Int

    fun delete(table:String, whereClause:String? = null, whereArgs:Array<String>? = null):Int

    fun execSQL(sql:String):Unit
    fun execSQL(sql:String, bindArgs:Array<Any?>):Unit



    fun rawQuery(sql:String, selectionArgs:Array<String>? = null):Cursor
    fun rawQueryWithFactory(cursorFactory: CursorFactory, sql:String, selectionArgs:Array<String?>?, editTable: String?): Cursor

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

}