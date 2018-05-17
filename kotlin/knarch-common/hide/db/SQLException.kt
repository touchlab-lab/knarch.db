package co.touchlab.kite.db


class SQLException(message: String?=null, cause: Throwable?=null) : RuntimeException(message, cause)