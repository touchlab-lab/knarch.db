package co.touchlab.knarch.db

import co.touchlab.knarch.db.native.CppCursorWindow
import co.touchlab.knarch.db.native.NativeCursorWindow
import co.touchlab.knarch.db.sqlite.SQLiteClosable

open class CursorWindow/**
 * Creates a new empty cursor window and gives it a name.
 * <p>
 * The cursor initially has no rows or columns. Call {@link #setNumColumns(int)} to
 * set the number of columns before adding any rows to the cursor.
 * </p>
 *
 * @param name The name of the cursor window, or null if none.
 */
(initName:String? = null):SQLiteClosable() {

    val nativeCursorWindow:NativeCursorWindow
    var name:String


    /**
     * Gets the start position of this cursor window.
     * <p>
     * The start position is the zero-based index of the first row that this window contains
     * relative to the entire result set of the {@link Cursor}.
     * </p>
     *
     * @return The zero-based start position.
     */
    /**
     * Sets the start position of this cursor window.
     * <p>
     * The start position is the zero-based index of the first row that this window contains
     * relative to the entire result set of the {@link Cursor}.
     * </p>
     *
     * @param pos The new zero-based start position.
     */
    var startPosition:Int = 0

    /**
     * Gets the number of rows in this window.
     *
     * @return The number of rows in this cursor window.
     */
    val numRows:Int
        get() {
            acquireReference()
            try
            {
                return nativeCursorWindow.implGetNumRows()
            }
            finally
            {
                releaseReference()
            }
        }

    init{
        this.name = if (initName != null && initName.isNotEmpty()) initName else "<unnamed>"
        nativeCursorWindow = CppCursorWindow(this.name)
        // recordNewWindow(Binder.getCallingPid(), mWindowPtr);
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
        acquireReference()
        try
        {
            startPosition = 0
            nativeCursorWindow.implClear()
        }
        finally
        {
            releaseReference()
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implSetNumColumns(columnNum)
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Allocates a new row at the end of this cursor window.
     *
     * @return True if successful, false if the cursor window is out of memory.
     */
    fun allocRow():Boolean {
        acquireReference()
        try
        {
            return nativeCursorWindow.implAllocRow()
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Frees the last row in this cursor window.
     */
    fun freeLastRow() {
        acquireReference()
        try
        {
            nativeCursorWindow.implFreeLastRow()
        }
        finally
        {
            releaseReference()
        }
    }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implGetType(row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implGetBlob(row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implGetString(row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implGetLong(row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implGetDouble(row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
    fun getShort(row:Int, column:Int):Short {
        return getLong(row, column).toShort()
    }
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
    fun getInt(row:Int, column:Int):Int {
        return getLong(row, column).toInt()
    }
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
    fun getFloat(row:Int, column:Int):Float {
        return getDouble(row, column).toFloat()
    }
    /**
     * Copies a byte array into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putBlob(value:ByteArray, row:Int, column:Int):Boolean {
        acquireReference()
        try
        {
            return nativeCursorWindow.implPutBlob(value, row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implPutString(value, row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implPutLong(value, row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
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
        acquireReference()
        try
        {
            return nativeCursorWindow.implPutDouble(value, row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Puts a null value into the field at the specified row and column index.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    fun putNull(row:Int, column:Int):Boolean {
        acquireReference()
        try
        {
            return nativeCursorWindow.implPutNull(row - startPosition, column)
        }
        finally
        {
            releaseReference()
        }
    }
    fun describeContents():Int {
        return 0
    }
    open override fun onAllReferencesReleased() {
        dispose()
    }
    /*private static final LongSparseArray<Integer> sWindowToPidMap = new LongSparseArray<Integer>();
   private void recordNewWindow(int pid, long window) {
   synchronized (sWindowToPidMap) {
   sWindowToPidMap.put(window, pid);
   if (Log.isLoggable(STATS_TAG, Log.VERBOSE)) {
   Log.i(STATS_TAG, "Created a new Cursor. " + printStats());
   }
   }
   }
   private void recordClosingOfWindow(long window) {
   synchronized (sWindowToPidMap) {
   if (sWindowToPidMap.size() == 0) {
   // this means we are not in the ContentProvider.
   return;
   }
   sWindowToPidMap.delete(window);
   }
   }*/
//    private fun printStats():String {
//        val buff = StringBuilder()
//        val myPid = 0//Process.myPid(); TODO: GET ACTUAL PROCESS ID, MAYBE?
//        val total = 0
//        val pidCounts = HashMap<Int, Int>()
//        /*synchronized (sWindowToPidMap) {
//     int size = sWindowToPidMap.size();
//     if (size == 0) {
//     // this means we are not in the ContentProvider.
//     return "";
//     }
//     for (int indx = 0; indx < size; indx++) {
//     int pid = sWindowToPidMap.valueAt(indx);
//     int value = pidCounts.get(pid);
//     pidCounts.put(pid, ++value);
//     }
//     }*/
//        val numPids = pidCounts.size
//        for (i in 0 until numPids)
//        {
//            buff.append(" (# cursors opened by ")
//            val pid = pidCounts.keyAt(i)
//            if (pid == myPid)
//            {
//                buff.append("this proc=")
//            }
//            else
//            {
//                buff.append("pid " + pid + "=")
//            }
//            val num = pidCounts.get(pid)
//            buff.append(num + ")")
//            total += num
//        }
//        // limit the returned string size to 1000
//        val s = if ((buff.length > 980)) buff.substring(0, 980) else buff.toString()
//        return "# Open Cursors=$total$s"
//    }
    override fun toString():String {
        return name + nativeCursorWindow.toString()
    }

}

