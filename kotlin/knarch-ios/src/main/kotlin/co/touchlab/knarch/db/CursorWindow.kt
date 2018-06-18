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

import co.touchlab.knarch.db.sqlite.SQLiteClosable

open class CursorWindow:SQLiteClosable() {

    private val nativeCursorWindow:CppCursorWindow = CppCursorWindow()

    /**
     * The start position is the zero-based index of the first row that this window contains
     * relative to the entire result set of the {@link Cursor}.
     */
    var startPosition:Int = 0

    /**
     * The number of rows in this cursor window.
     */
    val numRows:Int
        get() {
            return withRef { nativeCursorWindow.implGetNumRows() }
        }

    private fun dispose() {
        nativeCursorWindow.implDispose()
    }


    /**
     * Clears out the existing contents of the window, making it safe to reuse
     * for new data.
     * <p>
     * The start position ({@link #getStartPosition()}), number of rows ({@link #getNumRows()}),
     * and number of columns in the cursor are all reset to zero.
     * </p>
     */
    fun clear() {
        withRef {
            startPosition = 0
            nativeCursorWindow.implClear()
        }
    }

    /**
     * Sets the number of columns in this window.
     * <p>
     * This method must be called before any rows are added to the window, otherwise
     * it will fail to set the number of columns if it differs from the current number
     * of columns.
     * </p>
     *
     * @param columnNum The new number of columns.
     * @return True if successful.
     */
    fun setNumColumns(columnNum:Int):Boolean {
        return withRef { nativeCursorWindow.implSetNumColumns(columnNum) }
    }

    /**
     * Allocates a new row at the end of this cursor window.
     *
     * @return True if successful, false if the cursor window is out of memory.
     */
    fun allocRow():Boolean {
        return withRef { nativeCursorWindow.implAllocRow() }
    }

    /**
     * Frees the last row in this cursor window.
     */
    fun freeLastRow() {
        withRef { nativeCursorWindow.implFreeLastRow() }
    }

    /**
     * Returns the type of the field at the specified row and column index.
     * <p>
     * The returned field types are:
     * <ul>
     * <li>{@link Cursor#FIELD_TYPE_NULL}</li>
     * <li>{@link Cursor#FIELD_TYPE_INTEGER}</li>
     * <li>{@link Cursor#FIELD_TYPE_FLOAT}</li>
     * <li>{@link Cursor#FIELD_TYPE_STRING}</li>
     * <li>{@link Cursor#FIELD_TYPE_BLOB}</li>
     * </ul>
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The field type.
     */
    fun getType(row:Int, column:Int):Int {
        return withRef { nativeCursorWindow.implGetType(row - startPosition, column) }
    }

    /**
     * Gets the value of the field at the specified row and column index as a byte array.
     * <p>
     * The result is determined as follows:
     * <ul>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_NULL}, then the result
     * is <code>null</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_BLOB}, then the result
     * is the blob value.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_STRING}, then the result
     * is the array of bytes that make up the internal representation of the
     * string value.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_INTEGER} or
     * {@link Cursor#FIELD_TYPE_FLOAT}, then a {@link SQLiteException} is thrown.</li>
     * </ul>
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a byte array.
     */
    fun getBlob(row:Int, column:Int):ByteArray {
        return withRef { nativeCursorWindow.implGetBlob(row - startPosition, column) }
    }

    /**
     * Gets the value of the field at the specified row and column index as a string.
     * <p>
     * The result is determined as follows:
     * <ul>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_NULL}, then the result
     * is <code>null</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_STRING}, then the result
     * is the string value.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_INTEGER}, then the result
     * is a string representation of the integer in decimal, obtained by formatting the
     * value with the <code>printf</code> family of functions using
     * format specifier <code>%lld</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_FLOAT}, then the result
     * is a string representation of the floating-point value in decimal, obtained by
     * formatting the value with the <code>printf</code> family of functions using
     * format specifier <code>%g</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_BLOB}, then a
     * {@link SQLiteException} is thrown.</li>
     * </ul>
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a string.
     */
    fun getString(row:Int, column:Int):String {
        return withRef { nativeCursorWindow.implGetString(row - startPosition, column) }
    }

    /**
     * Gets the value of the field at the specified row and column index as a <code>long</code>.
     * <p>
     * The result is determined as follows:
     * <ul>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_NULL}, then the result
     * is <code>0L</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_STRING}, then the result
     * is the value obtained by parsing the string value with <code>strtoll</code>.
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_INTEGER}, then the result
     * is the <code>long</code> value.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_FLOAT}, then the result
     * is the floating-point value converted to a <code>long</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_BLOB}, then a
     * {@link SQLiteException} is thrown.</li>
     * </ul>
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a <code>long</code>.
     */
    fun getLong(row:Int, column:Int):Long {
        return withRef { nativeCursorWindow.implGetLong(row - startPosition, column) }
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * <code>double</code>.
     * <p>
     * The result is determined as follows:
     * <ul>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_NULL}, then the result
     * is <code>0.0</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_STRING}, then the result
     * is the value obtained by parsing the string value with <code>strtod</code>.
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_INTEGER}, then the result
     * is the integer value converted to a <code>double</code>.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_FLOAT}, then the result
     * is the <code>double</code> value.</li>
     * <li>If the field is of type {@link Cursor#FIELD_TYPE_BLOB}, then a
     * {@link SQLiteException} is thrown.</li>
     * </ul>
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a <code>double</code>.
     */
    fun getDouble(row:Int, column:Int):Double {
        return withRef { nativeCursorWindow.implGetDouble(row - startPosition, column) }
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * <code>short</code>.
     * <p>
     * The result is determined by invoking {@link #getLong} and converting the
     * result to <code>short</code>.
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a <code>short</code>.
     */
    fun getShort(row:Int, column:Int):Short = getLong(row, column).toShort()

    /**
     * Gets the value of the field at the specified row and column index as an
     * <code>int</code>.
     * <p>
     * The result is determined by invoking {@link #getLong} and converting the
     * result to <code>int</code>.
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as an <code>int</code>.
     */
    fun getInt(row:Int, column:Int):Int = getLong(row, column).toInt()

    /**
     * Gets the value of the field at the specified row and column index as a
     * <code>float</code>.
     * <p>
     * The result is determined by invoking {@link #getDouble} and converting the
     * result to <code>float</code>.
     * </p>
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as an <code>float</code>.
     */
    fun getFloat(row:Int, column:Int):Float = getDouble(row, column).toFloat()

    /**
     * Copies a byte array into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putBlob(value:ByteArray, row:Int, column:Int):Boolean {
        return withRef { nativeCursorWindow.implPutBlob(value, row - startPosition, column) }
    }

    /**
     * Copies a string into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putString(value:String, row:Int, column:Int):Boolean {
        return withRef { nativeCursorWindow.implPutString(value, row - startPosition, column) }
    }

    /**
     * Puts a long integer into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putLong(value:Long, row:Int, column:Int):Boolean {
        return withRef { nativeCursorWindow.implPutLong(value, row - startPosition, column) }
    }

    /**
     * Puts a double-precision floating point value into the field at the
     * specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putDouble(value:Double, row:Int, column:Int):Boolean {
        return withRef { nativeCursorWindow.implPutDouble(value, row - startPosition, column) }
    }

    /**
     * Puts a null value into the field at the specified row and column index.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putNull(row:Int, column:Int):Boolean {
        return withRef { nativeCursorWindow.implPutNull(row - startPosition, column) }
    }

    open override fun onAllReferencesReleased() {
        dispose()
    }

    fun getWindowCursorPtr():Long = nativeCursorWindow.mWindowPtr

    override fun toString():String {
        return nativeCursorWindow.toString()
    }

    //Deprecated methods available for tests
    /**
     * Returns true if the field at the specified row and column index
     * has type {@link Cursor#FIELD_TYPE_NULL}.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if the field has type {@link Cursor#FIELD_TYPE_NULL}.
     * @deprecated Use {@link #getType(int, int)} instead.
     */
    @Deprecated("Use {@link #getType(int, int)} instead.")
    fun isNull(row:Int, column:Int):Boolean {
        return getType(row, column) == Cursor.FIELD_TYPE_NULL
    }

    /**
     * Returns true if the field at the specified row and column index
     * has type {@link Cursor#FIELD_TYPE_BLOB} or {@link Cursor#FIELD_TYPE_NULL}.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if the field has type {@link Cursor#FIELD_TYPE_BLOB} or
     * {@link Cursor#FIELD_TYPE_NULL}.
     * @deprecated Use {@link #getType(int, int)} instead.
     */
    @Deprecated("Use {@link #getType(int, int)} instead.")
    fun isBlob(row:Int, column:Int):Boolean {
        val type = getType(row, column)
        return type == Cursor.FIELD_TYPE_BLOB || type == Cursor.FIELD_TYPE_NULL
    }
    /**
     * Returns true if the field at the specified row and column index
     * has type {@link Cursor#FIELD_TYPE_INTEGER}.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if the field has type {@link Cursor#FIELD_TYPE_INTEGER}.
     * @deprecated Use {@link #getType(int, int)} instead.
     */
    @Deprecated("Use {@link #getType(int, int)} instead.")
    fun isLong(row:Int, column:Int):Boolean {
        return getType(row, column) == Cursor.FIELD_TYPE_INTEGER
    }
    /**
     * Returns true if the field at the specified row and column index
     * has type {@link Cursor#FIELD_TYPE_FLOAT}.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if the field has type {@link Cursor#FIELD_TYPE_FLOAT}.
     * @deprecated Use {@link #getType(int, int)} instead.
     */
    @Deprecated("Use {@link #getType(int, int)} instead.")
    fun isFloat(row:Int, column:Int):Boolean {
        return getType(row, column) == Cursor.FIELD_TYPE_FLOAT
    }
    /**
     * Returns true if the field at the specified row and column index
     * has type {@link Cursor#FIELD_TYPE_STRING} or {@link Cursor#FIELD_TYPE_NULL}.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if the field has type {@link Cursor#FIELD_TYPE_STRING}
     * or {@link Cursor#FIELD_TYPE_NULL}.
     * @deprecated Use {@link #getType(int, int)} instead.
     */
    @Deprecated("Use {@link #getType(int, int)} instead.")
    fun isString(row:Int, column:Int):Boolean {
        val type = getType(row, column)
        return type == Cursor.FIELD_TYPE_STRING || type == Cursor.FIELD_TYPE_NULL
    }
}

/**
 * This class originally was intended to be a part of multiple implementations, but
 * that's not happening. TODO: Fold into the class above (but we have bigger fish to fry today)
 */
private class CppCursorWindow {

    var mWindowPtr:Long = 0

    /**
     * The memory for the window is normally managed by C++. However, if the user
     * does not call "close", we have a rather large memory leak.
     *
     * In Android, this is handled with finalize. Yes, we all know that Java won't always call
     * that, but in the usual course of things, it is called, and the memory is freed.
     *
     * Kotlin/Native has no finalize, but we can get direct pointer access rather easily. Creating
     * a ByteArray, and giving the window C++ the pointer to the data area allows us to accomplish
     * the same thing, and if the user doesn't close the window, we should get a GC clear when the reference
     * count goes to zero.
     *
     * This *may* be problematic if memory isn't being cleared at the moment this object's ref count is
     * zero. Although this is in theory equivalent to what happens in Java, it's possible setting the field
     * to null will also leave a rather large block of memory just "hanging out". There are more explicit
     * options to clear memory we can explore in that case. For now this appears to work, and if it ain't broke...
     */
    var dataArray:ByteArray?

    fun implCreate(cursorWindowSize: Int, dataArray:ByteArray) {
        mWindowPtr = nativeCreate(cursorWindowSize, dataArray)
        if (mWindowPtr == 0L)
        {
            throw CursorWindowAllocationException(("Cursor window allocation of ${(cursorWindowSize / 1024)} kb failed. "))
            /*+ printStats()*/
        }
    }

    fun implDispose() {
        if (mWindowPtr != 0L)
        {
            // recordClosingOfWindow(mWindowPtr);
            nativeDispose(mWindowPtr)
            mWindowPtr = 0
        }
        dataArray = null
    }

    fun implClear() {
        nativeClear(mWindowPtr)
    }

    fun implGetNumRows(): Int = nativeGetNumRows(mWindowPtr)
    fun implSetNumColumns(columnNum: Int): Boolean = nativeSetNumColumns(mWindowPtr, columnNum)
    fun implAllocRow(): Boolean = nativeAllocRow(mWindowPtr)
    fun implFreeLastRow() {
        nativeFreeLastRow(mWindowPtr)
    }
    fun implGetType(row: Int, column: Int): Int = nativeGetType(mWindowPtr, row, column)
    fun implGetBlob(row: Int, column: Int): ByteArray = nativeGetBlob(mWindowPtr, row, column)
    fun implGetString(row: Int, column: Int): String = nativeGetString(mWindowPtr, row, column)
    fun implGetLong(row: Int, column: Int): Long = nativeGetLong(mWindowPtr, row, column)
    fun implGetDouble(row: Int, column: Int): Double = nativeGetDouble(mWindowPtr, row, column)
    fun implPutBlob(value: ByteArray, row: Int, column: Int): Boolean = nativePutBlob(mWindowPtr, value, row, column)
    fun implPutString(value: String, row: Int, column: Int): Boolean = nativePutString(mWindowPtr, value, row, column)
    fun implPutLong(value: Long, row: Int, column: Int): Boolean = nativePutLong(mWindowPtr, value, row, column)
    fun implPutDouble(value: Double, row: Int, column: Int): Boolean = nativePutDouble(mWindowPtr, value, row, column)
    fun implPutNull(row: Int, column: Int): Boolean = nativePutNull(mWindowPtr, row, column)

    override fun toString(): String {
        return " {" + mWindowPtr.toString(16) + "}"
    }

    init{
        dataArray = ByteArray(sCursorWindowSize)
        implCreate(sCursorWindowSize, dataArray!!)
        // recordNewWindow(Binder.getCallingPid(), mWindowPtr);
    }

    @Suppress("JoinDeclarationAndAssignment")
    companion object {
        private val STATS_TAG = "CursorWindowStats"
        // This static member will be evaluated when first used.
        private val sCursorWindowSize :Int

        init {
            /** The cursor window size. resource xml file specifies the value in kB.
             * convert it to bytes here by multiplying with 1024.
             */
            // sCursorWindowSize = Resources.getSystem().getInteger(
            // com.android.internal.R.integer.config_cursorWindowSize) * 1024;
            sCursorWindowSize = 2048 * 1024
        }

        @SymbolName("Android_Database_CursorWindow_nativeCreate")
        private external fun nativeCreate(cursorWindowSize:Int, dataArray:ByteArray):Long
        @SymbolName("Android_Database_CursorWindow_nativeDispose")
        private external fun nativeDispose(windowPtr:Long)
        @SymbolName("Android_Database_CursorWindow_nativeClear")
        private external fun nativeClear(windowPtr:Long)
        @SymbolName("Android_Database_CursorWindow_nativeGetNumRows")
        private external fun nativeGetNumRows(windowPtr:Long):Int
        @SymbolName("Android_Database_CursorWindow_nativeSetNumColumns")
        private external fun nativeSetNumColumns(windowPtr:Long, columnNum:Int):Boolean
        @SymbolName("Android_Database_CursorWindow_nativeAllocRow")
        private external fun nativeAllocRow(windowPtr:Long):Boolean
        @SymbolName("Android_Database_CursorWindow_nativeFreeLastRow")
        private external fun nativeFreeLastRow(windowPtr:Long)
        @SymbolName("Android_Database_CursorWindow_nativeGetType")
        private external fun nativeGetType(windowPtr:Long, row:Int, column:Int):Int

        @SymbolName("Android_Database_CursorWindow_nativeGetBlob")
        private external fun nativeGetBlob(windowPtr:Long, row:Int, column:Int):ByteArray
        @SymbolName("Android_Database_CursorWindow_nativeGetString")
        private external fun nativeGetString(windowPtr:Long, row:Int, column:Int):String

        @SymbolName("Android_Database_CursorWindow_nativeGetLong")
        private external fun nativeGetLong(windowPtr:Long, row:Int, column:Int):Long
        @SymbolName("Android_Database_CursorWindow_nativeGetDouble")
        private external fun nativeGetDouble(windowPtr:Long, row:Int, column:Int):Double
        @SymbolName("Android_Database_CursorWindow_nativePutBlob")
        private external fun nativePutBlob(windowPtr:Long, value:ByteArray, row:Int, column:Int):Boolean
        @SymbolName("Android_Database_CursorWindow_nativePutString")
        private external fun nativePutString(windowPtr:Long, value:String, row:Int, column:Int):Boolean
        @SymbolName("Android_Database_CursorWindow_nativePutLong")
        private external fun nativePutLong(windowPtr:Long, value:Long, row:Int, column:Int):Boolean
        @SymbolName("Android_Database_CursorWindow_nativePutDouble")
        private external fun nativePutDouble(windowPtr:Long, value:Double, row:Int, column:Int):Boolean
        @SymbolName("Android_Database_CursorWindow_nativePutNull")
        private external fun nativePutNull(windowPtr:Long, row:Int, column:Int):Boolean
    }
}

class CursorWindowAllocationException(description:String):RuntimeException(description)
