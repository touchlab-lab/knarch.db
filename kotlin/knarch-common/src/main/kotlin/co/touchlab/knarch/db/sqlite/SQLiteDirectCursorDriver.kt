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
        print("A 1")
        val query = SQLiteQuery(mDatabase, mSql)
        print("A 2")
        val cursor:Cursor
        try
        {
            query.bindAllArgsAsStrings(selectionArgs)
            print("A 3")
            if (factory == null)
            {
                cursor = SQLiteCursor(this, mEditTable, query)
            }
            else
            {
                cursor = factory.newCursor(mDatabase, this, mEditTable, query)
            }
            print("A 4")
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