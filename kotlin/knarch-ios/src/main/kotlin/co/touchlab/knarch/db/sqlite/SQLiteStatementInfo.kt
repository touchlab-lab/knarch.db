package co.touchlab.knarch.db.sqlite

import konan.worker.*

class SQLiteStatementInfo{
    /**
     * The number of parameters that the statement has.
     */
    var numParameters: Int = 0

    /**
     * The names of all columns in the result set of the statement.
     */
    var columnNames: Array<String> = arrayOf()
    set(value) {
        field = value.copyOf().freeze()
    }

    /**
     * True if the statement is read-only.
     */
    var readOnly: Boolean = false
}