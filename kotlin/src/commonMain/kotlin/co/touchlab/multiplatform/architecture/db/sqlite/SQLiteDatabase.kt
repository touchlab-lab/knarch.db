/*
 * Copyright (c) 2018 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.multiplatform.architecture.db.sqlite

import co.touchlab.multiplatform.architecture.db.ContentValues
import co.touchlab.multiplatform.architecture.db.Cursor

expect interface CursorFactory{
    fun newCursor(db: SQLiteDatabase,
                  masterQuery: SQLiteCursorDriver,
                  editTable: String?,
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

fun SQLiteDatabase.insert(table: String, conflictAlgorithm: Int = CONFLICT_ABORT, values: ContentValues): Long =
        insertWithOnConflict(table, null, values, conflictAlgorithm)

fun SQLiteDatabase.update(table: String, conflictAlgorithm: Int = CONFLICT_ABORT,
                          values: ContentValues, whereClause: String? = null, whereArgs: Array<String>? = null): Int =
        updateWithOnConflict(table, values, whereClause, whereArgs, conflictAlgorithm)

fun SQLiteDatabase.query(distinct:Boolean=false,
                         table:String,
                         columns:Array<String>?=null,
                         selection:String?=null,
                             selectionArgs:Array<String>?=null,
                             orderBy:String?=null,
                         limit:String?=null):Cursor =
        query(distinct, table, columns, selection, selectionArgs, null, null, orderBy, limit)

fun <T> SQLiteDatabase.withTransaction(proc:()->T):T{
    beginTransaction()
    try {
        val t = proc()
        setTransactionSuccessful()
        return t
    }
    finally {
        endTransaction()
    }
}
expect class SQLiteDatabase{

    fun beginTransaction():Unit
    fun beginTransactionNonExclusive():Unit
    fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener):Unit
    fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener):Unit
    fun endTransaction():Unit
    fun setTransactionSuccessful():Unit
    fun inTransaction(): Boolean
    fun getVersion():Int
    fun setVersion(version:Int)
    fun getMaximumSize():Long
    fun setMaximumSize(numBytes:Long):Long
    fun getPageSize():Long
    fun setPageSize(numBytes:Long)

    fun compileStatement(sql:String): SQLiteStatement

    fun query(distinct:Boolean, table:String, columns:Array<String>?, selection:String?,
              selectionArgs:Array<String>?, groupBy:String?, having:String?,
              orderBy:String?, limit:String?):Cursor

    fun insertWithOnConflict(table: String, nullColumnHack:String?, initialValues: ContentValues?, conflictAlgorithm: Int): Long
    fun updateWithOnConflict(table: String, values:ContentValues, whereClause:String?, whereArgs:Array<String>?, conflictAlgorithm:Int): Int

    fun delete(table:String, whereClause:String?, whereArgs:Array<String>?):Int

    fun execSQL(sql:String):Unit
    fun execSQL(sql:String, bindArgs:Array<Any?>?):Unit

    fun rawQuery(sql:String, selectionArgs:Array<String>?):Cursor
    fun rawQueryWithFactory(cursorFactory: CursorFactory?, sql:String,
                            selectionArgs:Array<String>?, editTable: String?): Cursor

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
    fun close():Unit
}