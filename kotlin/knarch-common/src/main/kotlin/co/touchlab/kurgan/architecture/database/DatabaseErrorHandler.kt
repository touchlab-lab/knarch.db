package co.touchlab.kurgan.architecture.database

import co.touchlab.kurgan.architecture.database.sqlite.SQLiteDatabase

expect interface DatabaseErrorHandler{
    fun onCorruption(dbObj: SQLiteDatabase)
}