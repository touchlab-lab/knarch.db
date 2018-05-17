package co.touchlab.kite.db

import co.touchlab.kite.db.sqlite.SQLiteDatabase

interface DatabaseErrorHandler{
    fun onCorruption(dbObj: SQLiteDatabase)
}