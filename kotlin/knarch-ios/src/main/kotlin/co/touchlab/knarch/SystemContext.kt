package co.touchlab.knarch

import co.touchlab.knarch.db.DatabaseErrorHandler
import co.touchlab.knarch.db.sqlite.SQLiteDatabase

interface SystemContext{
    companion object {
        val MODE_ENABLE_WRITE_AHEAD_LOGGING = 0x0008
    }
    fun getDatabasePath(databaseName:String):String

    fun openOrCreateDatabase(name:String,
                                      mode:Int, factory:SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?):SQLiteDatabase
}