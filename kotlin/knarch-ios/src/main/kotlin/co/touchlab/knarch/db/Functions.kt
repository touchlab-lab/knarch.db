package co.touchlab.knarch.db

import co.touchlab.knarch.db.sqlite.*
import konan.internal.ExportForCppRuntime

@ExportForCppRuntime
fun ThrowSql_IllegalStateException(message: String): Unit =
        throw IllegalStateException(message)

@ExportForCppRuntime
fun ThrowSql_SQLiteException(exceptionClass:String, message:String): Unit =
        throw SQLiteException("$exceptionClass - $message")

