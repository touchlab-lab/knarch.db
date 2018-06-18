/*
 * Copyright (C) 2006 The Android Open Source Project
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

package co.touchlab.knarch.db

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.Cursor.Companion.FIELD_TYPE_STRING

abstract class AbstractCursor:CrossProcessCursor {
    var position:Int = 0
        protected set

    var closed:Boolean = false

    override fun isClosed():Boolean = closed

    /* -------------------------------------------------------- */
    /* These need to be implemented by subclasses */
    abstract val count:Int
    abstract val columnNames:Array<String>
    /* -------------------------------------------------------- */
    /* Methods that may optionally be implemented by subclasses */

    /**
     * If the cursor is backed by a {@link CursorWindow}, returns a pre-filled
     * window with the contents of the cursor, otherwise null.
     *
     * @return The pre-filled window that backs this cursor, or null if none.
     */
    override val window:CursorWindow?
        get() {
            return null
        }
    val columnCount:Int
        get() {
            return columnNames.size
        }
    val isFirst:Boolean
        get() {
            return position == 0 && count != 0
        }
    val isLast:Boolean
        get() {
            val cnt = count
            return position == (cnt - 1) && cnt != 0
        }
    val isBeforeFirst:Boolean
        get() {
            if (count == 0)
            {
                return true
            }
            return position == -1
        }
    val isAfterLast:Boolean
        get() {
            if (count == 0)
            {
                return true
            }
            return position == count
        }
    val wantsAllOnMoveCalls:Boolean
        get() {
            return false
        }

    abstract override fun getString(columnIndex:Int):String
    abstract override fun getShort(columnIndex:Int):Short
    abstract override fun getInt(columnIndex:Int):Int
    abstract override fun getLong(columnIndex:Int):Long
    abstract override fun getFloat(columnIndex:Int):Float
    abstract override fun getDouble(columnIndex:Int):Double
    abstract override fun isNull(columnIndex:Int):Boolean

    override fun getType(columnIndex:Int):Int {
        // Reflects the assumption that all commonly used field types (meaning everything
        // but blobs) are convertible to strings so it should be safe to call
        // getString to retrieve them.
        return FIELD_TYPE_STRING
    }

    // TODO implement getBlob in all cursor types
    override fun getBlob(columnIndex:Int):ByteArray {
        throw UnsupportedOperationException("getBlob is not supported")
    }

    open fun deactivate() {
        onDeactivateOrClose()
    }

    /** @hide */
    open fun onDeactivateOrClose() {

    }

    override fun close() {
        closed = true
        onDeactivateOrClose()
    }

    /**
     * This function is called every time the cursor is successfully scrolled
     * to a new position, giving the subclass a chance to update any state it
     * may have. If it returns false the move function will also do so and the
     * cursor will scroll to the beforeFirst position.
     *
     * @param oldPosition the position that we're moving from
     * @param newPosition the position that we're moving to
     * @return true if the move is successful, false otherwise
     */
    override fun onMove(oldPosition:Int, newPosition:Int):Boolean {
        return true
    }

    /* -------------------------------------------------------- */
    /* Implementation */
    init{
        position = -1
    }

    override fun moveToPosition(position:Int):Boolean {
        // Make sure position isn't past the end of the cursor
        val count = count
        if (position >= count)
        {
            this.position = count
            return false
        }
        // Make sure position isn't before the beginning of the cursor
        if (position < 0)
        {
            this.position = -1
            return false
        }
        // Check for no-op moves, and skip the rest of the work for them
        if (position == this.position)
        {
            return true
        }
        val result = onMove(this.position, position)
        if (result == false)
        {
            this.position = -1
        }
        else
        {
            this.position = position
        }
        return result
    }

    override fun fillWindow(position:Int, window:CursorWindow) {
        DatabaseUtils.cursorFillWindow(this, position, window)
    }

    override fun move(offset:Int):Boolean {
        return moveToPosition(position + offset)
    }

    override fun moveToFirst():Boolean {
        return moveToPosition(0)
    }

    override fun moveToLast():Boolean {
        return moveToPosition(count - 1)
    }

    override fun moveToNext():Boolean {
        return moveToPosition(position + 1)
    }

    override fun moveToPrevious():Boolean {
        return moveToPosition(position - 1)
    }

    override fun getColumnIndex(columnName:String):Int {

        var localColumnName = columnName
        // Hack according to bug 903852
        val periodIndex = localColumnName.lastIndexOf('.')
        if (periodIndex != -1)
        {
            val e = Exception()
            Log.e(TAG, "requesting column name with table name -- $localColumnName", e)
            localColumnName = localColumnName.substring(periodIndex + 1)
        }
        val columnNames = columnNames
        val length = columnNames.size
        for (i in 0 until length)
        {
            if (columnNames[i].equals(localColumnName, ignoreCase = true))
            {
                return i
            }
        }
        return -1
    }

    override fun getColumnIndexOrThrow(columnName:String):Int {
        val index = getColumnIndex(columnName)
        if (index < 0)
        {
            throw IllegalArgumentException("column '$columnName' does not exist")
        }
        return index
    }
    override fun getColumnName(columnIndex:Int):String {
        return columnNames[columnIndex]
    }

    /**
     * This function throws CursorIndexOutOfBoundsException if
     * the cursor position is out of bounds. Subclass implementations of
     * the get functions should call this before attempting
     * to retrieve data.
     *
     * @throws CursorIndexOutOfBoundsException
     */
    open fun checkPosition() {
        if (-1 == position || count == position)
        {
            throw CursorIndexOutOfBoundsException(position, count)
        }
    }
    protected fun finalize() {
        try
        {
            if (!isClosed()) close()
        }
        catch (e:Exception) {}
    }

    companion object {
        private val TAG = "Cursor"
    }
}