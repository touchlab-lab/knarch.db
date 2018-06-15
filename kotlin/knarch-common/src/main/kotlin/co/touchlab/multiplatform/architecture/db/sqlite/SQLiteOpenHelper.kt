package co.touchlab.multiplatform.architecture.db.sqlite

expect abstract class SQLiteOpenHelper{
    fun getWritableDatabase():SQLiteDatabase
    fun getReadableDatabase():SQLiteDatabase
    fun setWriteAheadLoggingEnabled(enabled: Boolean)

    fun close()

    open fun onConfigure(db: SQLiteDatabase)
    abstract fun onCreate(db: SQLiteDatabase)
    abstract fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    open fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    open fun onOpen(db: SQLiteDatabase)
}