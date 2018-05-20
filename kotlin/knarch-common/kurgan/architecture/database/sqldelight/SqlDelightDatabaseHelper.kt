package co.touchlab.kurgan.architecture.database.sqldelight

import co.touchlab.kurgan.architecture.ThreadLocal
import co.touchlab.kurgan.architecture.database.Cursor
import co.touchlab.kurgan.architecture.database.sqlite.*
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.*

class SqlDelightDatabaseHelper(
        private val openHelper: SQLiteOpenHelper
) : SqlDatabase {
    //TODO: Thread local...
    private val transactions = ThreadLocal<SqlDelightDatabaseConnection.Transaction>()

    override fun getConnection(): SqlDatabaseConnection {
        return SqlDelightDatabaseConnection(openHelper.getWritableDatabase(), transactions)
    }

    override fun close() {
        return openHelper.close()
    }

    /*class Callback(
            private val helper: SqlDatabase.Helper,
            version: Int
    ) : SQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SQLiteDatabase) {
            helper.onCreate(SqlDelightDatabaseConnection(db, ThreadLocal()))
        }

        override fun onUpgrade(
                db: SQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
        ) {
            helper.onMigrate(SqlDelightDatabaseConnection(db, ThreadLocal()), oldVersion, newVersion)
        }
    }*/
}

/**
 * Wraps [database] into a [SqlDatabase] usable by a SqlDelight generated QueryWrapper.
 */
fun SqlDatabase.Helper.create(
        database: SQLiteDatabase
): SqlDatabase {
    return SqlDelightInitializationHelper(database)
}

/**
 * Wraps [context] into a [SqlDatabase] usable by a SqlDelight generated QueryWrapper.
 */
/*fun SqlDatabase.Helper.create(
        context: Context,
        version: Int,
        name: String? = null,
        callback: SupportSQLiteOpenHelper.Callback = SqlDelightDatabaseHelper.Callback(this, version)
): SqlDatabase {
    val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .callback(callback)
            .name(name)
            .build()
    return SqlDelightDatabaseHelper(FrameworkSQLiteOpenHelperFactory().create(configuration))
}*/

private class SqlDelightInitializationHelper(
        private val database: SQLiteDatabase
) : SqlDatabase {
    override fun getConnection(): SqlDatabaseConnection {
        return SqlDelightDatabaseConnection(database, ThreadLocal())
    }

    override fun close() {
        throw IllegalStateException("Tried to call close during initialization")
    }
}

private class SqlDelightDatabaseConnection(
        private val database: SQLiteDatabase,
        private val transactions: ThreadLocal<Transaction>
) : SqlDatabaseConnection {
    override fun newTransaction(): Transaction {
        val enclosing = transactions.get()
        val transaction = Transaction(enclosing)
        transactions.set(transaction)

        if (enclosing == null) {
            database.beginTransactionNonExclusive()
        }

        return transaction
    }

    override fun currentTransaction() = transactions.get()

    inner class Transaction(
            override val enclosingTransaction: Transaction?
    ) : Transacter.Transaction() {
        override fun endTransaction(successful: Boolean) {
            if (enclosingTransaction == null) {
                if (successful) {
                    database.setTransactionSuccessful()
                    database.endTransaction()
                } else {
                    database.endTransaction()
                }
            }
            transactions.set(enclosingTransaction)
        }
    }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type) = when(type) {
        SELECT -> SqlDelightQuery(sql, database)
        INSERT, UPDATE, DELETE, EXEC -> SqlDelightPreparedStatement(database.compileStatement(sql), type)
    }
}

private class SqlDelightPreparedStatement(
        private val statement: SQLiteStatement,
        private val type: SqlPreparedStatement.Type
) : SqlPreparedStatement {
    override fun bindBytes(index: Int, bytes: ByteArray?) {
        if (bytes == null) statement.bindNull(index) else statement.bindBlob(index, bytes)
    }

    override fun bindLong(index: Int, longVal: Long?) {
        if (longVal == null) statement.bindNull(index) else statement.bindLong(index, longVal)
    }

    override fun bindDouble(index: Int, doubleVal: Double?) {
        if (doubleVal == null) statement.bindNull(index) else statement.bindDouble(index, doubleVal)
    }

    override fun bindString(index: Int, stringVal: String?) {
        if (stringVal == null) statement.bindNull(index) else statement.bindString(index, stringVal)
    }

    override fun executeQuery() = throw UnsupportedOperationException()

    override fun execute() = when (type) {
        INSERT -> statement.executeInsert()
        UPDATE, DELETE -> statement.executeUpdateDelete().toLong()
        EXEC -> {
            statement.execute()
            1
        }
        SELECT -> throw AssertionError()
    }

}

private class SqlDelightQuery(
        private val sql: String,
        private val database: SQLiteDatabase
) : SqlPreparedStatement {
    val binds: MutableList<(SQLiteProgram) -> Unit> = ArrayList()
    val bindings: MutableList<String> = ArrayList()

    override fun bindBytes(index: Int, bytes: ByteArray?) {
//        binds.add { if (bytes == null) it.bindNull(index) else it.bindBlob(index, bytes) }
        TODO("No query blobs")
    }

    override fun bindLong(index: Int, longVal: Long?) {
//        binds.add { if (long == null) it.bindNull(index) else it.bindLong(index, long) }
        bindString(index, longVal!!.toString())
    }

    override fun bindDouble(index: Int, doubleVal: Double?) {
//        binds.add { if (double == null) it.bindNull(index) else it.bindDouble(index, double) }
        bindString(index, doubleVal!!.toString())
    }

    override fun bindString(index: Int, stringVal: String?) {
//        binds.add { if (string == null) it.bindNull(index) else it.bindString(index, string) }
        val iminus = index-1

        if(bindings.size <= iminus)
            bindings.add(stringVal!!)
        else
            bindings.add(iminus, stringVal!!)
    }

    override fun execute() = throw UnsupportedOperationException()

    override fun executeQuery() = SqlDelightResultSet(database.rawQuery(sql, bindings.toTypedArray()))

    /*override fun bindTo(statement: SQLiteProgram) {
        synchronized(binds) {
            while (binds.isNotEmpty()) {
                //I know, garbage
                binds.removeAt(0).invoke(statement)
            }
        }
    }

    override fun getSql() = sql*/
}

private class SqlDelightResultSet(
        private val cursor: Cursor
) : SqlResultSet {
    override fun next() = cursor.moveToNext()
    override fun getString(index: Int) = cursor.getString(index)
    override fun getLong(index: Int) = cursor.getLong(index)
    override fun getBytes(index: Int) = cursor.getBlob(index)
    override fun getDouble(index: Int) = cursor.getDouble(index)
    override fun close() = cursor.close()
}