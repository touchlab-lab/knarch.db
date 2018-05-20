package co.touchlab.kurgan.architecture.database.sqlite

import co.touchlab.kurgan.architecture.database.ContentValues

expect fun deleteDatabase(path:String):Boolean

/*
fun execDeleteStatement(db: SQLiteDatabase, table: String, whereClause: String?, whereArgs: Array<Any?>?): Int {
    val query = ("DELETE FROM " + table
            + if (whereClause.isNullOrEmpty()) "" else " WHERE $whereClause")
    val statement = db.compileStatement(query)
    SQLiteQuery.bind(statement, whereArgs)
    return statement.executeUpdateDelete()
}

private val CONFLICT_VALUES = arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")

fun execUpdateStatement(db: SQLiteDatabase, table: String, conflictAlgorithm: Int,
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
    SQLiteQuery.bind(stmt, bindArgs)
    return stmt.executeUpdateDelete()
}*/
