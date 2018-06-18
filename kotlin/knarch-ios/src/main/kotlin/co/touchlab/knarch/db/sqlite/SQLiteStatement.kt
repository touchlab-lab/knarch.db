/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * CHANGE NOTICE: File modified by Touchlab Inc to port to Kotlin and generally prepare for Kotlin/Native
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        withRefCorrupt {
            getSession().execute(getSql(), getBindArgs())
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
        return withRefCorrupt {
            getSession().executeForChangedRowCount(
                    getSql(), getBindArgs())
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
        return withRefCorrupt {
            getSession().executeForLastInsertedRowId(
                    getSql(), getBindArgs())
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
        return withRefCorrupt {
            getSession().executeForLong(
                    getSql(), getBindArgs())
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
        return withRefCorrupt {
            getSession().executeForString(
                    getSql(), getBindArgs())
        }
    }

    //Might be able to compose this with "withRef" from base class. Should also make sure inline is useful.
    private inline fun <R> withRefCorrupt(proc:() -> R):R{
        acquireReference()
        try
        {
            return proc()
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