package co.touchlab.multiplatform.architecture.db.sqlite

expect abstract class SQLiteProgram:SQLiteClosable{
    fun getDatabase():SQLiteDatabase
    fun getSql():String
    fun getBindArgs():Array<Any?>?
    fun getColumnNames():Array<String>
    fun bindNull(index:Int)
    fun bindLong(index:Int, value:Long)
    fun bindDouble(index:Int, value:Double)
    fun bindString(index:Int, value:String)
    fun bindBlob(index:Int, value:ByteArray)
    fun clearBindings()
    fun bindAllArgsAsStrings(bindArgs:Array<String>?)
}

expect abstract class SQLiteClosable{
    fun close()
}