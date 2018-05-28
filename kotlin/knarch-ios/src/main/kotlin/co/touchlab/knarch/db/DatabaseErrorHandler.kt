package co.touchlab.knarch.db

import co.touchlab.knarch.db.sqlite.SQLiteDatabase

interface DatabaseErrorHandler{
    fun onCorruption(dbObj: SQLiteDatabase)
}