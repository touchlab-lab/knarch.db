package co.touchlab.knarch.db

import co.touchlab.knarch.*
import co.touchlab.knarch.db.sqlite.*
import kotlin.math.max

class DatabaseUtils {
    companion object {


        private val TAG = "DatabaseUtils"
        private val DEBUG = true
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_SELECT = 1
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_UPDATE = 2
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_ATTACH = 3
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_BEGIN = 4
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_COMMIT = 5
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_ABORT = 6
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_PRAGMA = 7
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_DDL = 8
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_UNPREPARED = 9
        /** One of the values returned by {@link #getSqlStatementType(String)}. */
        val STATEMENT_OTHER = 99


        /**
         * Binds the given Object to the given SQLiteProgram using the proper
         * typing. For example, bind numbers as longs/doubles, and everything else
         * as a string by call toString() on it.
         *
         * @param prog the program to bind the object to
         * @param index the 1-based index to bind at
         * @param value the value to bind
         */
        fun bindObjectToProgram(prog: SQLiteProgram, index: Int,
                                value: Any) {
            if (value == null) {
                prog.bindNull(index)
            } else if (value is Double || value is Float) {
                prog.bindDouble(index, (value as Number).toDouble())
            } else if (value is Number) {
                prog.bindLong(index, (value as Number).toLong())
            } else if (value is Boolean) {
                val bool = value as Boolean
                if (bool) {
                    prog.bindLong(index, 1)
                } else {
                    prog.bindLong(index, 0)
                }
            } else if (value is ByteArray) {
                prog.bindBlob(index, value as ByteArray)
            } else {
                prog.bindString(index, value.toString())
            }
        }

        /**
         * Returns data type of the given object's value.
         *<p>
         * Returned values are
         * <ul>
         * <li>{@link Cursor#FIELD_TYPE_NULL}</li>
         * <li>{@link Cursor#FIELD_TYPE_INTEGER}</li>
         * <li>{@link Cursor#FIELD_TYPE_FLOAT}</li>
         * <li>{@link Cursor#FIELD_TYPE_STRING}</li>
         * <li>{@link Cursor#FIELD_TYPE_BLOB}</li>
         *</ul>
         *</p>
         *
         * @param obj the object whose value type is to be returned
         * @return object value type
         * @hide
         */
        fun getTypeOfObject(obj: Any?): Int {
            if (obj == null) {
                return Cursor.FIELD_TYPE_NULL
            } else if (obj is ByteArray) {
                return Cursor.FIELD_TYPE_BLOB
            } else if (obj is Float || obj is Double) {
                return Cursor.FIELD_TYPE_FLOAT
            } else if ((obj is Long || obj is Int
                            || obj is Short || obj is Byte)) {
                return Cursor.FIELD_TYPE_INTEGER
            } else {
                return Cursor.FIELD_TYPE_STRING
            }
        }

        /**
         * Fills the specified cursor window by iterating over the contents of the cursor.
         * The window is filled until the cursor is exhausted or the window runs out
         * of space.
         *
         * The original position of the cursor is left unchanged by this operation.
         *
         * @param cursor The cursor that contains the data to put in the window.
         * @param position The start position for filling the window.
         * @param window The window to fill.
         * @hide
         */
        fun cursorFillWindow(cursor: Cursor,
                             startPosition: Int, window: CursorWindow) {
            var position = startPosition
            if (position < 0 || position >= cursor.getCount()) {
                return
            }
            val oldPos = cursor.getPosition()
            val numColumns = cursor.getColumnCount()
            window.clear()
            window.startPosition = position
            window.setNumColumns(numColumns)

            if (cursor.moveToPosition(position)) {
                do {
                    if (!window.allocRow()) {
                        break
                    }
                    for (i in 0 until numColumns) {
                        val type = cursor.getType(i)
                        val success: Boolean
                        when (type) {
                            Cursor.FIELD_TYPE_NULL -> success = window.putNull(position, i)
                            Cursor.FIELD_TYPE_INTEGER -> success = window.putLong(cursor.getLong(i), position, i)
                            Cursor.FIELD_TYPE_FLOAT -> success = window.putDouble(cursor.getDouble(i), position, i)
                            Cursor.FIELD_TYPE_BLOB -> {
                                val value = cursor.getBlob(i)
                                success = if (value != null)
                                    window.putBlob(value, position, i)
                                else
                                    window.putNull(position, i)
                            }
                            Cursor.FIELD_TYPE_STRING -> {
                                val value = cursor.getString(i)
                                success = if (value != null)
                                    window.putString(value, position, i)
                                else
                                    window.putNull(position, i)
                            }
                            else // assume value is convertible to String
                            -> {
                                val value = cursor.getString(i)
                                success = if (value != null)
                                    window.putString(value, position, i)
                                else
                                    window.putNull(position, i)
                            }
                        }
                        if (!success) {
                            window.freeLastRow()
                            break
                        }
                    }
                    position += 1
                } while (cursor.moveToNext())
            }
            cursor.moveToPosition(oldPos)
        }

        /**
         * Appends an SQL string to the given StringBuilder, including the opening
         * and closing single quotes. Any single quotes internal to sqlString will
         * be escaped.
         *
         * This method is deprecated because we want to encourage everyone
         * to use the "?" binding form. However, when implementing a
         * ContentProvider, one may want to add WHERE clauses that were
         * not provided by the caller. Since "?" is a positional form,
         * using it in this case could break the caller because the
         * indexes would be shifted to accomodate the ContentProvider's
         * internal bindings. In that case, it may be necessary to
         * construct a WHERE clause manually. This method is useful for
         * those cases.
         *
         * @param sb the StringBuilder that the SQL string will be appended to
         * @param sqlString the raw string to be appended, which may contain single
         * quotes
         */
        fun appendEscapedSQLString(sb: StringBuilder, sqlString: String) {
            sb.append('\'')
            if (sqlString.indexOf('\'') != -1) {
                val length = sqlString.length
                for (i in 0 until length) {
                    val c = sqlString.get(i)
                    if (c == '\'') {
                        sb.append('\'')
                    }
                    sb.append(c)
                }
            } else
                sb.append(sqlString)
            sb.append('\'')
        }

        /**
         * SQL-escape a string.
         */
        fun sqlEscapeString(value: String): String {
            val escaper = StringBuilder()
            DatabaseUtils.appendEscapedSQLString(escaper, value)
            return escaper.toString()
        }

        /**
         * Appends an Object to an SQL string with the proper escaping, etc.
         */
        fun appendValueToSql(sql: StringBuilder, value: Any?) {
            if (value == null) {
                sql.append("NULL")
            } else if (value is Boolean) {
                val bool = value as Boolean
                if (bool) {
                    sql.append('1')
                } else {
                    sql.append('0')
                }
            } else {
                appendEscapedSQLString(sql, value.toString())
            }
        }

        /**
         * Concatenates two SQL WHERE clauses, handling empty or null values.
         */
        fun concatenateWhere(a: String, b: String): String {
            if (a.isEmpty()) {
                return b
            }
            if (b.isEmpty()) {
                return a
            }
            return "($a) AND ($b)"
        }

        private fun getKeyLen(arr: ByteArray): Int {
            if (arr[arr.size - 1].toInt() != 0) {
                return arr.size
            } else {
                // remove zero "termination"
                return arr.size - 1
            }
        }


        /**
         * Prints the contents of a Cursor to a StringBuilder. The position
         * is restored after printing.
         *
         * @param cursor the cursor to print
         * @param sb the StringBuilder to print to
         */
        fun dumpCursor(cursor: Cursor, sb: StringBuilder) {
            sb.append(">>>>> Dumping cursor " + cursor + "\n")
            if (cursor != null) {
                val startPos = cursor.getPosition()
                cursor.moveToPosition(-1)
                while (cursor.moveToNext()) {
                    dumpCurrentRow(cursor, sb)
                }
                cursor.moveToPosition(startPos)
            }
            sb.append("<<<<<\n")
        }

        /**
         * Prints the contents of a Cursor to a String. The position is restored
         * after printing.
         *
         * @param cursor the cursor to print
         * @return a String that contains the dumped cursor
         */
        fun dumpCursorToString(cursor: Cursor): String {
            val sb = StringBuilder()
            dumpCursor(cursor, sb)
            return sb.toString()
        }

        /**
         * Prints the contents of a Cursor's current row to a StringBuilder.
         *
         * @param cursor the cursor to print
         * @param sb the StringBuilder to print to
         */
        fun dumpCurrentRow(cursor: Cursor, sb: StringBuilder) {
            val cols = cursor.getColumnNames()
            sb.append("" + cursor.getPosition() + " {\n")
            val length = cols.size
            for (i in 0 until length) {
                var value: String
                try {
                    value = cursor.getString(i)
                } catch (e: SQLiteException) {
                    // assume that if the getString threw this exception then the column is not
                    // representable by a string, e.g. it is a BLOB.
                    value = "<unprintable>"
                }
                sb.append(" " + cols[i] + '='.toString() + value + "\n")
            }
            sb.append("}\n")
        }

        /**
         * Dump the contents of a Cursor's current row to a String.
         *
         * @param cursor the cursor to print
         * @return a String that contains the dumped cursor row
         */
        fun dumpCurrentRowToString(cursor: Cursor): String {
            val sb = StringBuilder()
            dumpCurrentRow(cursor, sb)
            return sb.toString()
        }

        /**
         * Reads a String out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The TEXT field to read
         * @param values The {@link ContentValues} to put the value into, with the field as the key
         */
        fun cursorStringToContentValues(cursor: Cursor, field: String,
                                        values: ContentValues) {
            cursorStringToContentValues(cursor, field, values, field)
        }

        /**
         * Reads a String out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The TEXT field to read
         * @param values The {@link ContentValues} to put the value into, with the field as the key
         * @param key The key to store the value with in the map
         */
        fun cursorStringToContentValues(cursor: Cursor, field: String,
                                        values: ContentValues, key: String) {
            values.put(key, cursor.getString(cursor.getColumnIndexOrThrow(field)))
        }

        /**
         * Reads an Integer out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The INTEGER field to read
         * @param values The {@link ContentValues} to put the value into, with the field as the key
         */
        fun cursorIntToContentValues(cursor: Cursor, field: String, values: ContentValues) {
            cursorIntToContentValues(cursor, field, values, field)
        }

        /**
         * Reads a Integer out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The INTEGER field to read
         * @param values The {@link ContentValues} to put the value into, with the field as the key
         * @param key The key to store the value with in the map
         */
        fun cursorIntToContentValues(cursor: Cursor, field: String, values: ContentValues,
                                     key: String) {
            val colIndex = cursor.getColumnIndex(field)
            if (!cursor.isNull(colIndex)) {
                values.put(key, cursor.getInt(colIndex))
            } else {
                values.remove(key)
            }
        }

        /**
         * Reads a Long out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The INTEGER field to read
         * @param values The {@link ContentValues} to put the value into, with the field as the key
         */
        fun cursorLongToContentValues(cursor: Cursor, field: String, values: ContentValues) {
            cursorLongToContentValues(cursor, field, values, field)
        }

        /**
         * Reads a Long out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The INTEGER field to read
         * @param values The {@link ContentValues} to put the value into
         * @param key The key to store the value with in the map
         */
        fun cursorLongToContentValues(cursor: Cursor, field: String, values: ContentValues,
                                      key: String) {
            val colIndex = cursor.getColumnIndex(field)
            if (!cursor.isNull(colIndex)) {
                val value = cursor.getLong(colIndex)
                values.put(key, value)
            } else {
                values.remove(key)
            }
        }

        /**
         * Reads a Double out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The REAL field to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorDoubleToCursorValues(cursor: Cursor, field: String, values: ContentValues) {
            cursorDoubleToContentValues(cursor, field, values, field)
        }

        /**
         * Reads a Double out of a field in a Cursor and writes it to a Map.
         *
         * @param cursor The cursor to read from
         * @param field The REAL field to read
         * @param values The {@link ContentValues} to put the value into
         * @param key The key to store the value with in the map
         */
        fun cursorDoubleToContentValues(cursor: Cursor, field: String,
                                        values: ContentValues, key: String) {
            val colIndex = cursor.getColumnIndex(field)
            if (!cursor.isNull(colIndex)) {
                values.put(key, cursor.getDouble(colIndex))
            } else {
                values.remove(key)
            }
        }

        /**
         * Read the entire contents of a cursor row and store them in a ContentValues.
         *
         * @param cursor the cursor to read from.
         * @param values the {@link ContentValues} to put the row into.
         */
        fun cursorRowToContentValues(cursor: Cursor, values: ContentValues) {
            val awc = if ((cursor is AbstractWindowedCursor)) cursor as AbstractWindowedCursor else null
            val columns = cursor.getColumnNames()
            val length = columns.size
            for (i in 0 until length) {
                if (awc != null && awc.isBlob(i)) {
                    values.put(columns[i], cursor.getBlob(i))
                } else {
                    values.put(columns[i], cursor.getString(i))
                }
            }
        }

        /**
         * Picks a start position for {@link Cursor# fillWindow} such that the
         * window will contain the requested row and a useful range of rows
         * around it.
         *
         * When the data set is too large to fit in a cursor window, seeking the
         * cursor can become a very expensive operation since we have to run the
         * query again when we move outside the bounds of the current window.
         *
         * We try to choose a start position for the cursor window such that
         * 1/3 of the window's capacity is used to hold rows before the requested
         * position and 2/3 of the window's capacity is used to hold rows after the
         * requested position.
         *
         * @param cursorPosition The row index of the row we want to get.
         * @param cursorWindowCapacity The estimated number of rows that can fit in
         * a cursor window, or 0 if unknown.
         * @return The recommended start position, always less than or equal to
         * the requested row.
         * @hide
         */
        fun cursorPickFillWindowStartPosition(
                cursorPosition: Int, cursorWindowCapacity: Int): Int {
            return max(cursorPosition - cursorWindowCapacity / 3, 0)
        }

        /**
         * Query the table for the number of rows in the table.
         * @param db the database the table is in
         * @param table the name of the table to query
         * @return the number of rows in the table
         */
        fun queryNumEntries(db: SQLiteDatabase, table: String): Long {
            return queryNumEntries(db, table, null, null)
        }

        /**
         * Query the table for the number of rows in the table.
         * @param db the database the table is in
         * @param table the name of the table to query
         * @param selection A filter declaring which rows to return,
         * formatted as an SQL WHERE clause (excluding the WHERE itself).
         * Passing null will count all rows for the given table
         * @return the number of rows in the table filtered by the selection
         */
        fun queryNumEntries(db: SQLiteDatabase, table: String, selection: String?): Long {
            return queryNumEntries(db, table, selection, null)
        }

        /**
         * Query the table for the number of rows in the table.
         * @param db the database the table is in
         * @param table the name of the table to query
         * @param selection A filter declaring which rows to return,
         * formatted as an SQL WHERE clause (excluding the WHERE itself).
         * Passing null will count all rows for the given table
         * @param selectionArgs You may include ?s in selection,
         * which will be replaced by the values from selectionArgs,
         * in order that they appear in the selection.
         * The values will be bound as Strings.
         * @return the number of rows in the table filtered by the selection
         */
        fun queryNumEntries(db: SQLiteDatabase, table: String, selection: String?,
                            selectionArgs: Array<String>?): Long {
            val s = if ((!selection.isNullOrEmpty())) " where " + selection else ""
            return longForQuery(db, "select count(*) from " + table + s,
                    selectionArgs)
        }

        /**
         * Query the table to check whether a table is empty or not
         * @param db the database the table is in
         * @param table the name of the table to query
         * @return True if the table is empty
         * @hide
         */
        fun queryIsEmpty(db: SQLiteDatabase, table: String): Boolean {
            val isEmpty = longForQuery(db, "select exists(select 1 from " + table + ")", null)
            return isEmpty == 0L
        }

        /**
         * Utility method to run the query on the db and return the value in the
         * first column of the first row.
         */
        fun longForQuery(db: SQLiteDatabase, query: String, selectionArgs: Array<String>?): Long {
            val prog = db.compileStatement(query)
            try {
                return longForQuery(prog, selectionArgs)
            } finally {
                prog.close()
            }
        }

        /**
         * Utility method to run the pre-compiled query and return the value in the
         * first column of the first row.
         */
        fun longForQuery(prog: SQLiteStatement, selectionArgs: Array<String>?): Long {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForLong()
        }

        /**
         * Utility method to run the query on the db and return the value in the
         * first column of the first row.
         */
        fun stringForQuery(db: SQLiteDatabase, query: String, selectionArgs: Array<String>?): String? {
            val prog = db.compileStatement(query)
            try {
                return stringForQuery(prog, selectionArgs)
            } finally {
                prog.close()
            }
        }

        /**
         * Utility method to run the pre-compiled query and return the value in the
         * first column of the first row.
         */
        fun stringForQuery(prog: SQLiteStatement, selectionArgs: Array<String>?): String? {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForString()
        }

        /**
         * Reads a String out of a column in a Cursor and writes it to a ContentValues.
         * Adds nothing to the ContentValues if the column isn't present or if its value is null.
         *
         * @param cursor The cursor to read from
         * @param column The column to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorStringToContentValuesIfPresent(cursor: Cursor, values: ContentValues,
                                                 column: String) {
            val index = cursor.getColumnIndex(column)
            if (index != -1 && !cursor.isNull(index)) {
                values.put(column, cursor.getString(index))
            }
        }

        /**
         * Reads a Long out of a column in a Cursor and writes it to a ContentValues.
         * Adds nothing to the ContentValues if the column isn't present or if its value is null.
         *
         * @param cursor The cursor to read from
         * @param column The column to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorLongToContentValuesIfPresent(cursor: Cursor, values: ContentValues,
                                               column: String) {
            val index = cursor.getColumnIndex(column)
            if (index != -1 && !cursor.isNull(index)) {
                values.put(column, cursor.getLong(index))
            }
        }

        /**
         * Reads a Short out of a column in a Cursor and writes it to a ContentValues.
         * Adds nothing to the ContentValues if the column isn't present or if its value is null.
         *
         * @param cursor The cursor to read from
         * @param column The column to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorShortToContentValuesIfPresent(cursor: Cursor, values: ContentValues,
                                                column: String) {
            val index = cursor.getColumnIndex(column)
            if (index != -1 && !cursor.isNull(index)) {
                values.put(column, cursor.getShort(index))
            }
        }

        /**
         * Reads a Integer out of a column in a Cursor and writes it to a ContentValues.
         * Adds nothing to the ContentValues if the column isn't present or if its value is null.
         *
         * @param cursor The cursor to read from
         * @param column The column to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorIntToContentValuesIfPresent(cursor: Cursor, values: ContentValues,
                                              column: String) {
            val index = cursor.getColumnIndex(column)
            if (index != -1 && !cursor.isNull(index)) {
                values.put(column, cursor.getInt(index))
            }
        }

        /**
         * Reads a Float out of a column in a Cursor and writes it to a ContentValues.
         * Adds nothing to the ContentValues if the column isn't present or if its value is null.
         *
         * @param cursor The cursor to read from
         * @param column The column to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorFloatToContentValuesIfPresent(cursor: Cursor, values: ContentValues,
                                                column: String) {
            val index = cursor.getColumnIndex(column)
            if (index != -1 && !cursor.isNull(index)) {
                values.put(column, cursor.getFloat(index))
            }
        }

        /**
         * Reads a Double out of a column in a Cursor and writes it to a ContentValues.
         * Adds nothing to the ContentValues if the column isn't present or if its value is null.
         *
         * @param cursor The cursor to read from
         * @param column The column to read
         * @param values The {@link ContentValues} to put the value into
         */
        fun cursorDoubleToContentValuesIfPresent(cursor: Cursor, values: ContentValues,
                                                 column: String) {
            val index = cursor.getColumnIndex(column)
            if (index != -1 && !cursor.isNull(index)) {
                values.put(column, cursor.getDouble(index))
            }
        }

        /**
         * Creates a db and populates it with the sql statements in sqlStatements.
         *
         * @param context the context to use to create the db
         * @param dbName the name of the db to create
         * @param dbVersion the version to set on the db
         * @param sqlStatements the statements to use to populate the db. This should be a single string
         * of the form returned by sqlite3's <tt>.dump</tt> command (statements separated by
         * semicolons)
         */
        fun createDbFromSqlStatements(
                context:SystemContext, dbName:String, dbVersion:Int, sqlStatements:String) {
            val db = context.openOrCreateDatabase(dbName, 0, null, null)
            // TODO: this is not quite safe since it assumes that all semicolons at the end of a line
            // terminate statements. It is possible that a text field contains ;\n. We will have to fix
            // this if that turns out to be a problem.
            val statements = sqlStatements.split(";\n")
            for (statement in statements)
            {
                if (statement.isEmpty()) continue
                db.execSQL(statement)
            }
            db.setVersion(dbVersion)
            db.close()
        }

        /**
         * Returns one of the following which represent the type of the given SQL statement.
         * <ol>
         * <li>{@link #STATEMENT_SELECT}</li>
         * <li>{@link #STATEMENT_UPDATE}</li>
         * <li>{@link #STATEMENT_ATTACH}</li>
         * <li>{@link #STATEMENT_BEGIN}</li>
         * <li>{@link #STATEMENT_COMMIT}</li>
         * <li>{@link #STATEMENT_ABORT}</li>
         * <li>{@link #STATEMENT_OTHER}</li>
         * </ol>
         * @param sql the SQL statement whose type is returned by this method
         * @return one of the values listed above
         */
        fun getSqlStatementType(sqlArg: String): Int {
            val sql = sqlArg.trim({ it <= ' ' })
            if (sql.length < 3) {
                return STATEMENT_OTHER
            }
            val prefixSql = sql.substring(0, 3).toUpperCase()
            if (prefixSql == "SEL") {
                return STATEMENT_SELECT
            } else if ((prefixSql == "INS" ||
                            prefixSql == "UPD" ||
                            prefixSql == "REP" ||
                            prefixSql == "DEL")) {
                return STATEMENT_UPDATE
            } else if (prefixSql == "ATT") {
                return STATEMENT_ATTACH
            } else if (prefixSql == "COM") {
                return STATEMENT_COMMIT
            } else if (prefixSql == "END") {
                return STATEMENT_COMMIT
            } else if (prefixSql == "ROL") {
                return STATEMENT_ABORT
            } else if (prefixSql == "BEG") {
                return STATEMENT_BEGIN
            } else if (prefixSql == "PRA") {
                return STATEMENT_PRAGMA
            } else if ((prefixSql == "CRE" || prefixSql == "DRO" ||
                            prefixSql == "ALT")) {
                return STATEMENT_DDL
            } else if (prefixSql == "ANA" || prefixSql == "DET") {
                return STATEMENT_UNPREPARED
            }
            return STATEMENT_OTHER
        }

        /**
         * Returns column index of "_id" column, or -1 if not found.
         * @hide
         */
        fun findRowIdColumnIndex(columnNames: Array<String>): Int {
            val length = columnNames.size
            for (i in 0 until length) {
                if (columnNames[i] == "_id") {
                    return i
                }
            }
            return -1
        }

    }
}