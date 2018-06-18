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

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.Cursor
import co.touchlab.knarch.db.DatabaseUtils

class SQLiteQueryBuilder {

    private var mProjectionMap:Map<String, String>? = null
    /**
     * Returns the list of tables being queried
     *
     * @return the list of tables being queried
     */
    /**
     * Sets the list of tables to query. Multiple tables can be specified to perform a join.
     * For example:
     * setTables("foo, bar")
     * setTables("foo LEFT OUTER JOIN bar ON (foo.id = bar.foo_id)")
     *
     * @param inTables the list of tables to query on
     */
    var tables :String?= ""

    fun setTables(t:String?){
        tables = t
    }

    fun getTables() = tables

    private var mWhereClause:StringBuilder? = null // lazily created
    private var mDistinct:Boolean = false
    private var mFactory:SQLiteDatabase.CursorFactory? = null
    private var mStrict:Boolean = false

    /**
     * Mark the query as DISTINCT.
     *
     * @param distinct if true the query is DISTINCT, otherwise it isn't
     */
    fun setDistinct(distinct:Boolean) {
        mDistinct = distinct
    }
    /**
     * Append a chunk to the WHERE clause of the query. All chunks appended are surrounded
     * by parenthesis and ANDed with the selection passed to {@link #query}. The final
     * WHERE clause looks like:
     *
     * WHERE (&lt;append chunk 1>&lt;append chunk2>) AND (&lt;query() selection parameter>)
     *
     * @param inWhere the chunk of text to append to the WHERE clause.
     */
    fun appendWhere(inWhere:CharSequence) {
        if (mWhereClause == null)
        {
            mWhereClause = StringBuilder(inWhere.length + 16)
        }
        if (mWhereClause!!.length == 0)
        {
            mWhereClause!!.append('(')
        }
        mWhereClause!!.append(inWhere)
    }
    /**
     * Append a chunk to the WHERE clause of the query. All chunks appended are surrounded
     * by parenthesis and ANDed with the selection passed to {@link #query}. The final
     * WHERE clause looks like:
     *
     * WHERE (&lt;append chunk 1>&lt;append chunk2>) AND (&lt;query() selection parameter>)
     *
     * @param inWhere the chunk of text to append to the WHERE clause. it will be escaped
     * to avoid SQL injection attacks
     */
    fun appendWhereEscapeString(inWhere:String) {
        if (mWhereClause == null)
        {
            mWhereClause = StringBuilder(inWhere.length + 16)
        }
        if (mWhereClause!!.length == 0)
        {
            mWhereClause!!.append('(')
        }

        DatabaseUtils.appendEscapedSQLString(mWhereClause!!, inWhere)
    }
    /**
     * Sets the projection map for the query. The projection map maps
     * from column names that the caller passes into query to database
     * column names. This is useful for renaming columns as well as
     * disambiguating column names when doing joins. For example you
     * could map "name" to "people.name". If a projection map is set
     * it must contain all column names the user may request, even if
     * the key and value are the same.
     *
     * @param columnMap maps from the user column names to the database column names
     */
    fun setProjectionMap(columnMap:Map<String, String>?) {
        mProjectionMap = columnMap
    }
    /**
     * Sets the cursor factory to be used for the query. You can use
     * one factory for all queries on a database but it is normally
     * easier to specify the factory when doing this query.
     *
     * @param factory the factory to use.
     */
    fun setCursorFactory(factory:SQLiteDatabase.CursorFactory) {
        mFactory = factory
    }
    /**
     * When set, the selection is verified against malicious arguments.
     * When using this class to create a statement using
     * {@link #buildQueryString(boolean, String, String[], String, String, String, String, String)},
     * non-numeric limits will raise an exception. If a projection map is specified, fields
     * not in that map will be ignored.
     * If this class is used to execute the statement directly using
     * {@link #query(SQLiteDatabase, String[], String, String[], String, String, String)}
     * or
     * {@link #query(SQLiteDatabase, String[], String, String[], String, String, String, String)},
     * additionally also parenthesis escaping selection are caught.
     *
     * To summarize: To get maximum protection against malicious third party apps (for example
     * content provider consumers), make sure to do the following:
     * <ul>
     * <li>Set this value to true</li>
     * <li>Use a projection map</li>
     * <li>Use one of the query overloads instead of getting the statement as a sql string</li>
     * </ul>
     * By default, this value is false.
     */
    fun setStrict(flag:Boolean) {
        mStrict = flag
    }
    /**
     * Perform a query by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to query on
     * @param projectionIn A list of which columns to return. Passing
     * null will return all columns, which is discouraged to prevent
     * reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE
     * itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     * will be replaced by the values from selectionArgs, in order
     * that they appear in the selection. The values will be bound
     * as Strings.
     * @param groupBy A filter declaring how to group rows, formatted
     * as an SQL GROUP BY clause (excluding the GROUP BY
     * itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     * the cursor, if row grouping is being used, formatted as an
     * SQL HAVING clause (excluding the HAVING itself). Passing
     * null will cause all row groups to be included, and is
     * required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     * ORDER BY clause (excluding the ORDER BY itself). Passing null
     * will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then {@link OperationCanceledException} will be thrown
     * when the query is executed.
     * @return a cursor over the result set
     * @see android.content.ContentResolver#query(android.net.Uri, String[],
     * String, String[], String)
     */
    fun query(db:SQLiteDatabase, projectionIn:Array<String>?,
                            selection:String?, selectionArgs:Array<String>?,
              groupBy:String?,
                            having:String?, sortOrder:String?, limit:String? = null):Cursor? {
        if (tables == null)
        {
            return null
        }
        if (mStrict && selection != null && selection.isNotEmpty())
        {
            // Validate the user-supplied selection to detect syntactic anomalies
            // in the selection string that could indicate a SQL injection attempt.
            // The idea is to ensure that the selection clause is a valid SQL expression
            // by compiling it twice: once wrapped in parentheses and once as
            // originally specified. An attacker cannot create an expression that
            // would escape the SQL expression while maintaining balanced parentheses
            // in both the wrapped and original forms.
            val sqlForValidation = buildQuery(projectionIn, "(" + selection + ")", groupBy,
                    having, sortOrder, limit)
            validateQuerySql(db, sqlForValidation) // will throw if query is invalid
        }
        val sql = buildQuery(
                projectionIn, selection, groupBy, having,
                sortOrder, limit)
        if (Log.isLoggable(TAG, Log.DEBUG_))
        {
            Log.d(TAG, "Performing query: $sql")
        }
        return db.rawQueryWithFactory(
                mFactory, sql, selectionArgs,
                SQLiteDatabase.findEditTable(tables!!)) // will throw if query is invalid
    }

    /**
     * Verifies that a SQL SELECT statement is valid by compiling it.
     * If the SQL statement is not valid, this method will throw a {@link SQLiteException}.
     */
    private fun validateQuerySql(db:SQLiteDatabase, sql:String) {
        db.getThreadSession().prepare(sql,
                 null)
    }

    /**
     * Construct a SELECT statement suitable for use in a group of
     * SELECT statements that will be joined through UNION operators
     * in buildUnionQuery.
     *
     * @param projectionIn A list of which columns to return. Passing
     * null will return all columns, which is discouraged to
     * prevent reading data from storage that isn't going to be
     * used.
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE
     * itself). Passing null will return all rows for the given
     * URL.
     * @param groupBy A filter declaring how to group rows, formatted
     * as an SQL GROUP BY clause (excluding the GROUP BY itself).
     * Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     * the cursor, if row grouping is being used, formatted as an
     * SQL HAVING clause (excluding the HAVING itself). Passing
     * null will cause all row groups to be included, and is
     * required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     * ORDER BY clause (excluding the ORDER BY itself). Passing null
     * will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return the resulting SQL SELECT statement
     */
    fun buildQuery(
            projectionIn:Array<String>?, selection:String?, groupBy:String?,
            having:String?, sortOrder:String?, limit:String?):String {
        val projection = computeProjection(projectionIn)
        val where = StringBuilder()
        val hasBaseWhereClause = mWhereClause != null && mWhereClause!!.isNotEmpty()
        if (hasBaseWhereClause)
        {
            where.append(mWhereClause.toString())
            where.append(')')
        }
        // Tack on the user's selection, if present.
        if (selection != null && selection.length > 0)
        {
            if (hasBaseWhereClause)
            {
                where.append(" AND ")
            }
            where.append('(')
            where.append(selection)
            where.append(')')
        }
        return buildQueryString(
                mDistinct, tables, projection, where.toString(),
                groupBy, having, sortOrder, limit)
    }

    /**
     * Construct a SELECT statement suitable for use in a group of
     * SELECT statements that will be joined through UNION operators
     * in buildUnionQuery.
     *
     * @param typeDiscriminatorColumn the name of the result column
     * whose cells will contain the name of the table from which
     * each row was drawn.
     * @param unionColumns the names of the columns to appear in the
     * result. This may include columns that do not appear in the
     * table this SELECT is querying (i.e. mTables), but that do
     * appear in one of the other tables in the UNION query that we
     * are constructing.
     * @param columnsPresentInTable a Set of the names of the columns
     * that appear in this table (i.e. in the table whose name is
     * mTables). Since columns in unionColumns include columns that
     * appear only in other tables, we use this array to distinguish
     * which ones actually are present. Other columns will have
     * NULL values for results from this subquery.
     * @param computedColumnsOffset all columns in unionColumns before
     * this index are included under the assumption that they're
     * computed and therefore won't appear in columnsPresentInTable,
     * e.g. "date * 1000 as normalized_date"
     * @param typeDiscriminatorValue the value used for the
     * type-discriminator column in this subquery
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE
     * itself). Passing null will return all rows for the given
     * URL.
     * @param groupBy A filter declaring how to group rows, formatted
     * as an SQL GROUP BY clause (excluding the GROUP BY itself).
     * Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     * the cursor, if row grouping is being used, formatted as an
     * SQL HAVING clause (excluding the HAVING itself). Passing
     * null will cause all row groups to be included, and is
     * required when row grouping is not being used.
     * @return the resulting SQL SELECT statement
     */
    fun buildUnionSubQuery(
            typeDiscriminatorColumn:String,
            unionColumns:Array<String>,
            columnsPresentInTable:Set<String>?,
            computedColumnsOffset:Int,
            typeDiscriminatorValue:String,
            selection:String,
            groupBy:String?,
            having:String?):String {
        val unionColumnsCount = unionColumns.size
        val projectionIn = Array(unionColumnsCount, {""})
        for (i in 0 until unionColumnsCount)
        {
            val unionColumn = unionColumns[i]
            if (unionColumn == typeDiscriminatorColumn)
            {
                projectionIn[i] = ("'" + typeDiscriminatorValue + "' AS "
                        + typeDiscriminatorColumn)
            }
            else if ((i <= computedColumnsOffset || (columnsPresentInTable != null && columnsPresentInTable.contains(unionColumn))))
            {
                projectionIn[i] = unionColumn
            }
            else
            {
                projectionIn[i] = "NULL AS $unionColumn"
            }
        }
        return buildQuery(
                projectionIn, selection, groupBy, having,
                null /* sortOrder */,
                null /* limit */)
    }

    /**
     * Given a set of subqueries, all of which are SELECT statements,
     * construct a query that returns the union of what those
     * subqueries return.
     * @param subQueries an array of SQL SELECT statements, all of
     * which must have the same columns as the same positions in
     * their results
     * @param sortOrder How to order the rows, formatted as an SQL
     * ORDER BY clause (excluding the ORDER BY itself). Passing
     * null will use the default sort order, which may be unordered.
     * @param limit The limit clause, which applies to the entire union result set
     *
     * @return the resulting SQL SELECT statement
     */
    fun buildUnionQuery(subQueries:Array<String>, sortOrder:String?, limit:String?):String {
        val query = StringBuilder(128)
        val subQueryCount = subQueries.size
        val unionOperator = if (mDistinct) " UNION " else " UNION ALL "
        for (i in 0 until subQueryCount)
        {
            if (i > 0)
            {
                query.append(unionOperator)
            }
            query.append(subQueries[i])
        }
        appendClause(query, " ORDER BY ", sortOrder)
        appendClause(query, " LIMIT ", limit)
        return query.toString()
    }
    private fun computeProjection(projectionIn:Array<String>?):Array<String>? {
        if (projectionIn != null && projectionIn.isNotEmpty())
        {
            if (mProjectionMap != null)
            {
                val projection = Array<String>(projectionIn.size, {""})
                val length = projectionIn.size
                for (i in 0 until length)
                {
                    val userColumn = projectionIn[i]
                    val column = mProjectionMap!!.get(userColumn)
                    if (column != null)
                    {
                        projection[i] = column
                        continue
                    }
                    if ((!mStrict && (userColumn.contains(" AS ") || userColumn.contains(" as "))))
                    {
                        /* A column alias already exist */
                        projection[i] = userColumn
                        continue
                    }
                    throw IllegalArgumentException(("Invalid column " + projectionIn[i]))
                }
                return projection
            }
            else
            {
                return projectionIn
            }
        }
        else if (mProjectionMap != null)
        {
            // Return all columns in projection map.
            val entrySet = mProjectionMap!!.entries
            val projection = Array(entrySet.size, {""})
            val entryIter = entrySet.iterator()
            var i = 0
            while (entryIter.hasNext())
            {
                val entry = entryIter.next()
                // Don't include the _count column when people ask for no projection.
                if (entry.key.equals(_COUNT))
                {
                    continue
                }
                projection[i++] = entry.value
            }
            return projection
        }
        return null
    }
    companion object {
        /**
         * The unique ID for a row.
         * <P>Type: INTEGER (long)</P>
         */
        val _ID = "_id"

        /**
         * The count of rows in a directory.
         * <P>Type: INTEGER</P>
         */
        val _COUNT = "_count"
        private val TAG = "SQLiteQueryBuilder"
//        private val sLimitPattern = Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?")
        private val sLimitPattern = "\\s*\\d+\\s*(,\\s*\\d+\\s*)?".toRegex()
        /**
         * Build an SQL query string from the given clauses.
         *
         * @param distinct true if you want each row to be unique, false otherwise.
         * @param tables The table names to compile the query against.
         * @param columns A list of which columns to return. Passing null will
         * return all columns, which is discouraged to prevent reading
         * data from storage that isn't going to be used.
         * @param where A filter declaring which rows to return, formatted as an SQL
         * WHERE clause (excluding the WHERE itself). Passing null will
         * return all rows for the given URL.
         * @param groupBy A filter declaring how to group rows, formatted as an SQL
         * GROUP BY clause (excluding the GROUP BY itself). Passing null
         * will cause the rows to not be grouped.
         * @param having A filter declare which row groups to include in the cursor,
         * if row grouping is being used, formatted as an SQL HAVING
         * clause (excluding the HAVING itself). Passing null will cause
         * all row groups to be included, and is required when row
         * grouping is not being used.
         * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
         * (excluding the ORDER BY itself). Passing null will use the
         * default sort order, which may be unordered.
         * @param limit Limits the number of rows returned by the query,
         * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
         * @return the SQL query string
         */
        fun buildQueryString(
                distinct:Boolean, tables:String?, columns:Array<String>?, where:String?,
                groupBy:String?, having:String?, orderBy:String?, limit:String?):String {
            if (groupBy.isNullOrEmpty() && !having.isNullOrEmpty())
            {
                throw IllegalArgumentException(
                        "HAVING clauses are only permitted when using a groupBy clause")
            }
            if (!limit.isNullOrEmpty() && !sLimitPattern.matches(limit!!))
            {
                throw IllegalArgumentException("invalid LIMIT clauses:" + limit)
            }
            val query = StringBuilder(120)
            query.append("SELECT ")
            if (distinct)
            {
                query.append("DISTINCT ")
            }
            if (columns != null && columns.isNotEmpty())
            {
                appendColumns(query, columns)
            }
            else
            {
                query.append("* ")
            }
            query.append("FROM ")
            query.append(tables)
            appendClause(query, " WHERE ", where)
            appendClause(query, " GROUP BY ", groupBy)
            appendClause(query, " HAVING ", having)
            appendClause(query, " ORDER BY ", orderBy)
            appendClause(query, " LIMIT ", limit)
            return query.toString()
        }
        private fun appendClause(s:StringBuilder, name:String, clause:String?) {
            if (!clause.isNullOrEmpty())
            {
                s.append(name)
                s.append(clause)
            }
        }
        /**
         * Add the names that are non-null in columns to s, separating
         * them with commas.
         */
        fun appendColumns(s:StringBuilder, columns:Array<String>) {
            val n = columns.size
            for (i in 0 until n)
            {
                val column = columns[i]
                if (i > 0)
                {
                    s.append(", ")
                }
                s.append(column)
            }
            s.append(' ')
        }
    }
}