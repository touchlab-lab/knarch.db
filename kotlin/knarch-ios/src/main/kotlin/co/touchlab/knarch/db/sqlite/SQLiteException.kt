package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.db.*

open class SQLiteException(message: String?=null, cause: Throwable?=null) : SQLException(message, cause)