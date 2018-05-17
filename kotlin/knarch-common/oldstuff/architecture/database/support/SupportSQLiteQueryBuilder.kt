package co.touchlab.kurgan.architecture.database.support

class SupportSQLiteQueryBuilder constructor(
        table: String,
        selection: String? = null,
        columns: Array<String>? = null,
        orderBy: String? = null,
        limit: String? = null,
        distinct: Boolean = false,
        groupBy: String? = null,
        having: String? = null
) {
    private val selectString: String

    init {
        if (!limit.isNullOrEmpty() && !sLimitPattern.matches(limit!!)) {
            throw IllegalArgumentException("invalid LIMIT clauses: $limit")
        }
        if (groupBy.isNullOrEmpty() && !having.isNullOrEmpty()) {
            throw IllegalArgumentException(
                    "HAVING clauses are only permitted when using a groupBy clause");
        }

        val query = StringBuilder(120)

        query.append("SELECT ")
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.isNotEmpty()) {
            appendColumns(query, columns)
        } else {
            query.append(" * ")
        }
        query.append(" FROM ")
        query.append(table)
        appendClause(query, " WHERE ", selection)
        appendClause(query, " GROUP BY ", groupBy)
        appendClause(query, " HAVING ", having)
        appendClause(query, " ORDER BY ", orderBy)
        appendClause(query, " LIMIT ", limit)

        selectString = query.toString()
    }

    companion object {
        val sLimitPattern = Regex("\\s*\\d+\\s*(,\\s*\\d+\\s*)?")
        fun builder(tableName:String):SupportSQLiteQueryBuilder = SupportSQLiteQueryBuilder(tableName)

        private fun appendClause(s: StringBuilder, name: String, clause: String?) {
            if (!isEmpty(clause)) {
                s.append(name)
                s.append(clause)
            }
        }

        /**
         * Add the names that are non-null in columns to s, separating
         * them with commas.
         */
        private fun appendColumns(s: StringBuilder, columns: Array<String>) {
            val n = columns.size

            for (i in 0 until n) {
                val column = columns[i]
                if (i > 0) {
                    s.append(", ")
                }
                s.append(column)
            }
            s.append(' ')
        }

        private fun isEmpty(input: String?): Boolean {
            return input == null || input.isEmpty()
        }
    }

    fun  create(bindArgs: Array<Any?>? = null):SupportSQLiteQuery {
        return SimpleSQLiteQuery(selectString, bindArgs)
    }
}