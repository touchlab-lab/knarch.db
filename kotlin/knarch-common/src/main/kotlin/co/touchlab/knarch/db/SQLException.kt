package co.touchlab.knarch.db


class SQLException(message: String?=null, cause: Throwable?=null) : RuntimeException(message, cause)