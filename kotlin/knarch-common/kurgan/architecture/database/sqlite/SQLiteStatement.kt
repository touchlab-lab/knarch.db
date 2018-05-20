package co.touchlab.kurgan.architecture.database.sqlite

expect class SQLiteStatement: SQLiteProgram {
    fun execute()
    fun executeUpdateDelete(): Int
    fun executeInsert(): Long
    fun simpleQueryForLong(): Long
    fun simpleQueryForString(): String
}