package co.touchlab.kurgan.architecture.database.sqlite.plain

expect class SQLiteStatement: SQLiteProgram{
    fun execute()
    fun executeUpdateDelete(): Int
    fun executeInsert(): Long
    fun simpleQueryForLong(): Long
    fun simpleQueryForString(): String
}