package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.db.Cursor

class SQLiteDirectCursorDriver(db:SQLiteDatabase, sql:String, editTable:String?):SQLiteCursorDriver {
    private val mDatabase:SQLiteDatabase
    private val mEditTable:String?
    private val mSql:String
    private var mQuery:SQLiteQuery?=null
    init{
        mDatabase = db
        mEditTable = editTable
        mSql = sql
    }

    override fun query(factory:SQLiteDatabase.CursorFactory?, selectionArgs:Array<String>?):Cursor {
        val query = SQLiteQuery(mDatabase, mSql)
        val cursor:Cursor
        try
        {
            query.bindAllArgsAsStrings(selectionArgs)
            if (factory == null)
            {
                cursor = SQLiteCursor(this, mEditTable!!, query)
            }
            else
            {
                cursor = factory.newCursor(mDatabase, this, mEditTable!!, query)
            }
        }
        catch (ex:RuntimeException) {
            query.close()
            throw ex
        }
        mQuery = query
        return cursor
    }

    override fun cursorClosed() {
        // Do nothing
    }
    override fun setBindArguments(bindArgs:Array<String>) {
        mQuery!!.bindAllArgsAsStrings(bindArgs)
    }
    override fun cursorDeactivated() {
        // Do nothing
    }
    override fun cursorRequeried(cursor:Cursor) {
        // Do nothing
    }
    override fun toString():String {
        return "SQLiteDirectCursorDriver: $mSql"
    }
}