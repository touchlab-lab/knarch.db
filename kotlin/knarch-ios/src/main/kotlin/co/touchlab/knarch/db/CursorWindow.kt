package co.touchlab.knarch.db

import co.touchlab.knarch.db.sqlite.SQLiteClosable

class CursorWindow/**
 * Creates a new empty cursor window and gives it a name.
 * <p>
 * The cursor initially has no rows or columns. Call {@link #setNumColumns(int)} to
 * set the number of columns before adding any rows to the cursor.
 * </p>
 *
 * @param name The name of the cursor window, or null if none.
 */
(initName:String? = null):SQLiteClosable() {
    /**
     * The native CursorWindow object pointer. (FOR INTERNAL USE ONLY)
     * @hide
     */
    var mWindowPtr:Long = 0

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
                return nativeGetNumRows(mWindowPtr)
            }
            finally
            {
                releaseReference()
            }
        }
    init{
        startPosition = 0
        this.name = if (initName != null && initName.isNotEmpty()) initName else "<unnamed>"
        if (sCursorWindowSize < 0)
        {
            /** The cursor window size. resource xml file specifies the value in kB.
             * convert it to bytes here by multiplying with 1024.
             */
            // sCursorWindowSize = Resources.getSystem().getInteger(
            // com.android.internal.R.integer.config_cursorWindowSize) * 1024;
            sCursorWindowSize = 2048 * 1024
        }
        mWindowPtr = nativeCreate(this.name, sCursorWindowSize)
        if (mWindowPtr == 0L)
        {
            throw CursorWindowAllocationException(("Cursor window allocation of " +
                    (sCursorWindowSize / 1024) + " kb failed. " /*+ printStats()*/))
        }

        // recordNewWindow(Binder.getCallingPid(), mWindowPtr);
    }
    /**
     * Creates a new empty cursor window.
     * <p>
     * The cursor initially has no rows or columns. Call {@link #setNumColumns(int)} to
     * set the number of columns before adding any rows to the cursor.
     * </p>
     *
     * @param localWindow True if this window will be used in this process only,
     * false if it might be sent to another processes. This argument is ignored.
     *
     * @deprecated There is no longer a distinction between local and remote
     * cursor windows. Use the {@link #CursorWindow(String)} constructor instead.
     */
    @Deprecated("There is no longer a distinction between local and remote\n"+
            " cursor windows. Use the {@link #CursorWindow(String)} constructor instead.")
    constructor(localWindow:Boolean) : this((null as String?)!!) {}

    private fun dispose() {
        if (mWindowPtr != 0L)
        {
            // recordClosingOfWindow(mWindowPtr);
            nativeDispose(mWindowPtr)
            mWindowPtr = 0
        }
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
            nativeClear(mWindowPtr)
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
            return nativeSetNumColumns(mWindowPtr, columnNum)
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
            return nativeAllocRow(mWindowPtr)
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
            nativeFreeLastRow(mWindowPtr)
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
            return nativeGetType(mWindowPtr, row - startPosition, column)
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
            return nativeGetBlob(mWindowPtr, row - startPosition, column)
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
            return nativeGetString(mWindowPtr, row - startPosition, column)
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
            return nativeGetLong(mWindowPtr, row - startPosition, column)
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
            return nativeGetDouble(mWindowPtr, row - startPosition, column)
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
            return nativePutBlob(mWindowPtr, value, row - startPosition, column)
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
            return nativePutString(mWindowPtr, value, row - startPosition, column)
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
            return nativePutLong(mWindowPtr, value, row - startPosition, column)
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
            return nativePutDouble(mWindowPtr, value, row - startPosition, column)
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
            return nativePutNull(mWindowPtr, row - startPosition, column)
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
        return name + " {" + mWindowPtr.toString(16) + "}"
    }
    companion object {
        private val STATS_TAG = "CursorWindowStats"
        // This static member will be evaluated when first used.
        private var sCursorWindowSize = -1

        @SymbolName("Android_Database_CursorWindow_nativeCreate")
        private external fun nativeCreate(name:String, cursorWindowSize:Int):Long
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
        @SymbolName("Android_Database_CursorWindow_nativeGetName")
        private external fun nativeGetName(windowPtr:Long):String
    }
}

class CursorWindowAllocationException(description:String):RuntimeException(description)