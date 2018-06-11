package co.touchlab.knarch.db.native

class CppCursorWindow(name:String):NativeCursorWindow{

    var mWindowPtr:Long = 0
    var dataArray:ByteArray?

    override fun implCreate(name: String, cursorWindowSize: Int, dataArray:ByteArray) {
        mWindowPtr = nativeCreate(name, cursorWindowSize, dataArray)
        if (mWindowPtr == 0L)
        {
            throw CursorWindowAllocationException(("Cursor window allocation of ${(cursorWindowSize / 1024)} kb failed. "))
            /*+ printStats()*/
        }
    }

    override fun implDispose() {
        if (mWindowPtr != 0L)
        {
            // recordClosingOfWindow(mWindowPtr);
            nativeDispose(mWindowPtr)
            mWindowPtr = 0
        }
    }

    override fun implClear() {
        nativeClear(mWindowPtr)
        dataArray = null
    }

    override fun implGetNumRows(): Int = nativeGetNumRows(mWindowPtr)
    override fun implSetNumColumns(columnNum: Int): Boolean = nativeSetNumColumns(mWindowPtr, columnNum)
    override fun implAllocRow(): Boolean = nativeAllocRow(mWindowPtr)
    override fun implFreeLastRow() {
        nativeFreeLastRow(mWindowPtr)
    }
    override fun implGetType(row: Int, column: Int): Int = nativeGetType(mWindowPtr, row, column)
    override fun implGetBlob(row: Int, column: Int): ByteArray = nativeGetBlob(mWindowPtr, row, column)
    override fun implGetString(row: Int, column: Int): String = nativeGetString(mWindowPtr, row, column)
    override fun implGetLong(row: Int, column: Int): Long = nativeGetLong(mWindowPtr, row, column)
    override fun implGetDouble(row: Int, column: Int): Double = nativeGetDouble(mWindowPtr, row, column)
    override fun implPutBlob(value: ByteArray, row: Int, column: Int): Boolean = nativePutBlob(mWindowPtr, value, row, column)
    override fun implPutString(value: String, row: Int, column: Int): Boolean = nativePutString(mWindowPtr, value, row, column)
    override fun implPutLong(value: Long, row: Int, column: Int): Boolean = nativePutLong(mWindowPtr, value, row, column)
    override fun implPutDouble(value: Double, row: Int, column: Int): Boolean = nativePutDouble(mWindowPtr, value, row, column)
    override fun implPutNull(row: Int, column: Int): Boolean = nativePutNull(mWindowPtr, row, column)
    override fun implGetName(): String = nativeGetName(mWindowPtr)

    override fun toString(): String {
        return " {" + mWindowPtr.toString(16) + "}"
    }

    init{
        dataArray = ByteArray(sCursorWindowSize)
        implCreate(name, sCursorWindowSize, dataArray!!)
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
        private external fun nativeCreate(name:String, cursorWindowSize:Int, dataArray:ByteArray):Long
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