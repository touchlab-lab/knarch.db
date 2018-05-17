package co.touchlab.kurgan.architecture.database.support

interface SupportSQLiteQuery{
    /**
     * The SQL query. This query can have placeholders(?) for bind arguments.
     *
     * @return The SQL query to compile
     */
    fun getSql(): String

    /**
     * Callback to bind the query parameters to the compiled statement.
     *
     * @param statement The compiled statement
     */
    fun bindTo(statement: SupportSQLiteProgram)
}