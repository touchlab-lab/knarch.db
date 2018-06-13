package co.touchlab.multiplatform.architecture.db.sqlite

expect abstract class SQLiteOpenHelper{

    fun getWritableDatabase():SQLiteDatabase
    fun getReadableDatabase():SQLiteDatabase
//    abstract fun onCreate(db: SQLiteDatabase):Unit
//    abstract fun onUpgrade(db: SQLiteDatabase, oldVersion:Int, newVersion:Int):Unit
//    open fun onDowngrade(db: SQLiteDatabase, oldVersion:Int, newVersion:Int):Unit
//    open fun onOpen(db: SQLiteDatabase):Unit
//    open fun onConfigure(db: SQLiteDatabase):Unit

    fun close()

    fun setWriteAheadLoggingEnabled(enabled: Boolean)
}