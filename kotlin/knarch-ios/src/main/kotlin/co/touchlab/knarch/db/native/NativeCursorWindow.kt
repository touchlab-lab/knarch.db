package co.touchlab.knarch.db.native

interface NativeCursorWindow{
    fun implCreate(name:String, cursorWindowSize:Int, dataArray:ByteArray):Unit
    fun implDispose()
    fun implClear()
    fun implGetNumRows():Int
    fun implSetNumColumns(columnNum:Int):Boolean
    fun implAllocRow():Boolean
    fun implFreeLastRow()
    fun implGetType(row:Int, column:Int):Int

    fun implGetBlob(row:Int, column:Int):ByteArray
    fun implGetString(row:Int, column:Int):String

    fun implGetLong(row:Int, column:Int):Long
    fun implGetDouble(row:Int, column:Int):Double
    fun implPutBlob(value:ByteArray, row:Int, column:Int):Boolean
    fun implPutString(value:String, row:Int, column:Int):Boolean
    fun implPutLong(value:Long, row:Int, column:Int):Boolean
    fun implPutDouble(value:Double, row:Int, column:Int):Boolean
    fun implPutNull(row:Int, column:Int):Boolean
    fun implGetName():String
}