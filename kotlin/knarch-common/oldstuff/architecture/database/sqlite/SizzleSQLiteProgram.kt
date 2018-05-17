package co.touchlab.kurgan.architecture.database.sqlite

import co.touchlab.kurgan.architecture.database.sqlite.plain.SQLiteProgram
import co.touchlab.kurgan.architecture.database.support.SupportSQLiteProgram

class SizzleSQLiteProgram(private val mDelegate: SQLiteProgram) : SupportSQLiteProgram {
    override fun bindNull(index: Int) {
        mDelegate.bindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        mDelegate.bindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        mDelegate.bindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        mDelegate.bindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        mDelegate.bindBlob(index, value)
    }

    override fun clearBindings() {
        mDelegate.clearBindings()
    }

    fun close() {
        mDelegate.close()
    }

}