package co.touchlab.kurgan.architecture.database.support

import co.touchlab.kurgan.architecture.ContentValues


fun execDeleteStatement(db: SupportSQLiteDatabase, table: String, whereClause: String?, whereArgs: Array<Any?>?): Int {
    val query = ("DELETE FROM " + table
            + if (whereClause.isNullOrEmpty()) "" else " WHERE $whereClause")
    val statement = db.compileStatement(query)
    SimpleSQLiteQuery.bind(statement, whereArgs)
    return statement.executeUpdateDelete()
}

private val CONFLICT_VALUES = arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")

fun execUpdateStatement(db: SupportSQLiteDatabase, table: String, conflictAlgorithm: Int,
                        values: ContentValues, whereClause: String?, whereArgs: Array<Any?>?): Int {
    // taken from SQLiteDatabase class.
    if (values.size() == 0) {
        throw IllegalArgumentException("Empty values")
    }
    val sql = StringBuilder(120)
    sql.append("UPDATE ")
    sql.append(CONFLICT_VALUES[conflictAlgorithm])
    sql.append(table)
    sql.append(" SET ")

    // move all bind args to one array
    val setValuesSize = values.size()
    val bindArgsSize = if (whereArgs == null) setValuesSize else setValuesSize + whereArgs.size
    val bindArgs = arrayOfNulls<Any>(bindArgsSize)
    var i = 0
    for (colName in values.keySet()) {
        sql.append(if (i > 0) "," else "")
        sql.append(colName)
        bindArgs[i++] = values.get(colName)
        sql.append("=?")
    }
    if (whereArgs != null) {
        i = setValuesSize
        while (i < bindArgsSize) {
            bindArgs[i] = whereArgs[i - setValuesSize]
            i++
        }
    }
    if (!whereClause.isNullOrEmpty()) {
        sql.append(" WHERE ")
        sql.append(whereClause)
    }
    val stmt = db.compileStatement(sql.toString())
    SimpleSQLiteQuery.bind(stmt, bindArgs)
    return stmt.executeUpdateDelete()
}

interface SupportSQLiteStatement : SupportSQLiteProgram {
    /**
     * Execute this SQL statement, if it is not a SELECT / INSERT / DELETE / UPDATE, for example
     * CREATE / DROP table, view, trigger, index etc.
     *
     * @throws android.database.SQLException If the SQL string is invalid for
     * some reason
     */
    fun execute()

    /**
     * Execute this SQL statement, if the the number of rows affected by execution of this SQL
     * statement is of any importance to the caller - for example, UPDATE / DELETE SQL statements.
     *
     * @return the number of rows affected by this SQL statement execution.
     * @throws android.database.SQLException If the SQL string is invalid for
     * some reason
     */
    fun executeUpdateDelete(): Int

    /**
     * Execute this SQL statement and return the ID of the row inserted due to this call.
     * The SQL statement should be an INSERT for this to be a useful call.
     *
     * @return the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     *
     * @throws android.database.SQLException If the SQL string is invalid for
     * some reason
     */
    fun executeInsert(): Long

    /**
     * Execute a statement that returns a 1 by 1 table with a numeric value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     *
     * @throws android.database.sqlite.SQLiteDoneException if the query returns zero rows
     */
    fun simpleQueryForLong(): Long

    /**
     * Execute a statement that returns a 1 by 1 table with a text value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     *
     * @throws android.database.sqlite.SQLiteDoneException if the query returns zero rows
     */
    fun simpleQueryForString(): String
}