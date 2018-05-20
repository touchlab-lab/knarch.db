package co.touchlab.knarch.db.sqlite

class SQLiteStatement internal constructor(db:SQLiteDatabase, sql:String, bindArgs:Array<Any?>?):SQLiteProgram(db, sql, bindArgs) {
    /**
     * Execute this SQL statement, if it is not a SELECT / INSERT / DELETE / UPDATE, for example
     * CREATE / DROP table, view, trigger, index etc.
     *
     * @throws android.database.SQLException If the SQL string is invalid for
     * some reason
     */
    fun execute() {
        acquireReference()
        try
        {
            getSession().execute(getSql(), getBindArgs(), getConnectionFlags())
        }
        catch (ex:SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Execute this SQL statement, if the the number of rows affected by execution of this SQL
     * statement is of any importance to the caller - for example, UPDATE / DELETE SQL statements.
     *
     * @return the number of rows affected by this SQL statement execution.
     * @throws android.database.SQLException If the SQL string is invalid for
     * some reason
     */
    fun executeUpdateDelete():Int {
        acquireReference()
        try
        {
            return getSession().executeForChangedRowCount(
                    getSql(), getBindArgs(), getConnectionFlags())
        }
        catch (ex:SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Execute this SQL statement and return the ID of the row inserted due to this call.
     * The SQL statement should be an INSERT for this to be a useful call.
     *
     * @return the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     *
     * @throws android.database.SQLException If the SQL string is invalid for
     * some reason
     */
    fun executeInsert():Long {
        acquireReference()
        try
        {
            return getSession().executeForLastInsertedRowId(
                    getSql(), getBindArgs(), getConnectionFlags())
        }
        catch (ex:SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Execute a statement that returns a 1 by 1 table with a numeric value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     *
     * @throws android.database.sqlite.SQLiteDoneException if the query returns zero rows
     */
    fun simpleQueryForLong():Long {
        acquireReference()
        try
        {
            return getSession().executeForLong(
                    getSql(), getBindArgs(), getConnectionFlags())
        }
        catch (ex:SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
        finally
        {
            releaseReference()
        }
    }
    /**
     * Execute a statement that returns a 1 by 1 table with a text value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     *
     * @throws android.database.sqlite.SQLiteDoneException if the query returns zero rows
     */
    fun simpleQueryForString():String? {
        acquireReference()
        try
        {
            return getSession().executeForString(
                    getSql(), getBindArgs(), getConnectionFlags())
        }
        catch (ex:SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
        finally
        {
            releaseReference()
        }
    }
    override fun toString():String {
        return "SQLiteProgram: " + getSql()
    }
}