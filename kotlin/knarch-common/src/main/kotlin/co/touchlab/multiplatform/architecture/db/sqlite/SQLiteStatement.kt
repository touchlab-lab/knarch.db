package co.touchlab.multiplatform.architecture.db.sqlite

expect class SQLiteStatement: SQLiteProgram {
    fun execute()
    fun executeUpdateDelete(): Int
    fun executeInsert(): Long
    fun simpleQueryForLong(): Long
    fun simpleQueryForString(): String?
}