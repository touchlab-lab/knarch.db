/*
 * Copyright (C) 2006 The Android Open Source Project
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
import co.touchlab.knarch.db.AbstractWindowedCursor
import co.touchlab.knarch.db.CursorWindow
import co.touchlab.knarch.db.DatabaseUtils

/**
 * Execute a query and provide access to its result set through a Cursor
 * interface.
 *
 * @param query the {@link SQLiteQuery} object associated with this cursor object.
 */
open class SQLiteCursor
(private val mDriver:SQLiteCursorDriver, query:SQLiteQuery):AbstractWindowedCursor() {
    override fun getPosition(): Int = position

    override fun isFirst(): Boolean = isFirst

    override fun isLast(): Boolean = isLast

    override fun isBeforeFirst(): Boolean =isBeforeFirst

    override fun isAfterLast(): Boolean =isAfterLast

    override fun getColumnNames(): Array<String> = columnNames

    override fun getColumnCount(): Int = columnCount

    override fun getCount(): Int = count

    /** The names of the columns in the rows */
    override val columnNames:Array<String> = query.getColumnNames()

    /** The query object for the cursor */
    private val mQuery:SQLiteQuery = query

    /** The number of rows in the cursor */
    private var mCount = NO_COUNT

    /** The number of rows that can fit in the cursor window, 0 if unknown */
    private var mCursorWindowCapacity:Int = 0

    /** A mapping of column names to column indices, to speed up lookups */
    private var mColumnNameMap:Map<String, Int>? = null

    override val count:Int
        get() {
            if (mCount == NO_COUNT)
            {
                fillWindow(0)
            }
            return mCount
        }

    override fun onMove(oldPosition:Int, newPosition:Int):Boolean {
        // Make sure the row at newPosition is present in the window
        if ((mWindow == null || newPosition < mWindow!!.startPosition ||
                        newPosition >= (mWindow!!.startPosition + mWindow!!.numRows)))
        {
            fillWindow(newPosition)
        }
        return true
    }

    private fun fillWindow(requiredPos:Int) {
        clearOrCreateWindow()
        try
        {
            if (mCount == NO_COUNT)
            {
                val startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos, 0)
                mCount = mQuery.fillWindow(mWindow!!, startPos, requiredPos, true)
                mCursorWindowCapacity = mWindow!!.numRows
                if (Log.isLoggable(TAG, Log.DEBUG_)) {
                     Log.d(TAG, "received count(*) from native_fill_window: $mCount");
                     }
            }
            else
            {
                val startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos,
                        mCursorWindowCapacity)
                mQuery.fillWindow(mWindow!!, startPos, requiredPos, false)
            }
        }
        catch (ex:RuntimeException) {
            // Close the cursor window if the query failed and therefore will
            // not produce any results. This helps to avoid accidentally leaking
            // the cursor window if the client does not correctly handle exceptions
            // and fails to close the cursor.
            closeWindow()
            throw ex
        }
    }

    override fun getColumnIndex(columnName:String):Int {
        var columnNameLocal = columnName
        // Create mColumnNameMap on demand
        if (mColumnNameMap == null)
        {
            val columns = columnNames
            val columnCount = columns.size
            val map = HashMap<String, Int>(columnCount)
            for (i in 0 until columnCount)
            {
                map.put(columns[i], i)
            }
            mColumnNameMap = map
        }
        // Hack according to bug 903852
        val periodIndex = columnNameLocal.lastIndexOf('.')
        if (periodIndex != -1)
        {
            val e = Exception()
            Log.e(TAG, "requesting column name with table name -- $columnNameLocal", e)
            columnNameLocal = columnNameLocal.substring(periodIndex + 1)
        }
        val i = mColumnNameMap!!.get(columnNameLocal)
        return i ?: -1
    }

    override fun deactivate() {
        super.deactivate()
        mDriver.cursorDeactivated()
    }

    override fun close() {
        super.close()
        mQuery.close()
        mDriver.cursorClosed()
    }

    fun setWindow(window:CursorWindow) {
        super.window = window
        mCount = NO_COUNT
    }

    /**
     * Changes the selection arguments. The new values take effect after a call to requery().
     */
    fun setSelectionArguments(selectionArgs:Array<String>) {
        mDriver.setBindArguments(selectionArgs)
    }

    companion object {
        internal val TAG = "SQLiteCursor"
        internal val NO_COUNT = -1
    }
}