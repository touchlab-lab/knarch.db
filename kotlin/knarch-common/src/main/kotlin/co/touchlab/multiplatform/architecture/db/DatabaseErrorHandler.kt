package co.touchlab.multiplatform.architecture.db

import co.touchlab.multiplatform.architecture.db.sqlite.SQLiteDatabase

expect interface DatabaseErrorHandler{
    fun onCorruption(dbObj: SQLiteDatabase)
}