package co.touchlab.knarch.db

open class SQLException(message: String?=null, cause: Throwable?=null) : RuntimeException(message, cause)