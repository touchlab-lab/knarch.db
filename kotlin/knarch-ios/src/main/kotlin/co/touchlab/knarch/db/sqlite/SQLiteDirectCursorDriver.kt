/*
 * Copyright (C) 2007 The Android Open Source Project
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

class SQLiteDirectCursorDriver(db:SQLiteDatabase, sql:String, editTable:String?):SQLiteCursorDriver {
    private val mDatabase:SQLiteDatabase = db
    private val mEditTable:String? = editTable
    private val mSql:String = sql
    private var mQuery:SQLiteQuery?=null

    override fun query(factory:SQLiteDatabase.CursorFactory?, bindArgs:Array<String>?):Cursor {
        val query = SQLiteQuery(mDatabase, mSql)
        val cursor:Cursor
        try
        {
            query.bindAllArgsAsStrings(bindArgs)
            if (factory == null)
            {
                cursor = SQLiteCursor(this, query)
            }
            else
            {
                cursor = factory.newCursor(mDatabase, this, mEditTable, query)
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