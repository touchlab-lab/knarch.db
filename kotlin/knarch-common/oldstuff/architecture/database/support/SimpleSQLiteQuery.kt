package co.touchlab.kurgan.architecture.database.support

/**
 * Creates an SQL query with the sql string and the bind arguments.
 *
 * @param query    The query string, can include bind arguments (.e.g ?).
 * @param bindArgs The bind argument value that will replace the placeholders in the query.
 */
class SimpleSQLiteQuery(query: String, bindArgs: Array<Any?>? = null) : SupportSQLiteQuery{
    private var mQuery: String = query
    private var mBindArgs: Array<Any?>? = bindArgs

    companion object {
        /**
         * Binds the given arguments into the given sqlite statement.
         *
         * @param statement The sqlite statement
         * @param bindArgs  The list of bind arguments
         */
        fun bind(statement: SupportSQLiteProgram, bindArgs: Array<Any?>?) {
            if (bindArgs == null) {
                return
            }
            val limit = bindArgs.size
            for (i in 0 until limit) {
                val arg = bindArgs[i]
                bind(statement, i + 1, arg)
            }
        }

        private fun bind(statement: SupportSQLiteProgram, index: Int, arg: Any?) {
            // extracted from android.database.sqlite.SQLiteConnection
            when (arg) {
                null -> statement.bindNull(index)
                is ByteArray -> statement.bindBlob(index, (arg as ByteArray?)!!)
                is Float -> statement.bindDouble(index, (arg as Float?)!!.toDouble())
                is Double -> statement.bindDouble(index, (arg as Double?)!!)
                is Long -> statement.bindLong(index, (arg as Long?)!!)
                is Int -> statement.bindLong(index, (arg as Int?)!!.toLong())
                is Short -> statement.bindLong(index, (arg as Short?)!!.toLong())
                is Byte -> statement.bindLong(index, (arg as Byte?)!!.toLong())
                is String -> statement.bindString(index, (arg as String?)!!)
                else -> throw IllegalArgumentException("Cannot bind " + arg + " at index " + index
                        + " Supported types: null, byte[], float, double, long, int, short, byte,"
                        + " string")
            }
        }
    }

    override fun getSql(): String {
        return mQuery
    }

    override fun bindTo(statement: SupportSQLiteProgram) {
        bind(statement, mBindArgs)
    }
}