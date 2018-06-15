package co.touchlab.multiplatform.architecture.db


expect interface Cursor{
    /**
     * Returns the numbers of rows in the cursor.
     *
     * @return the number of rows in the cursor.
     */
    fun getCount(): Int

    /**
     * Returns the current position of the cursor in the row set.
     * The value is zero-based. When the row set is first returned the cursor
     * will be at positon -1, which is before the first row. After the
     * last row is returned another call to next() will leave the cursor past
     * the last entry, at a position of count().
     *
     * @return the current cursor position.
     */
    fun getPosition(): Int

    /**
     * Move the cursor by a relative amount, forward or backward, from the
     * current position. Positive offsets move forwards, negative offsets move
     * backwards. If the final position is outside of the bounds of the result
     * set then the resultant position will be pinned to -1 or count() depending
     * on whether the value is off the front or end of the set, respectively.
     *
     *
     * This method will return true if the requested destination was
     * reachable, otherwise, it returns false. For example, if the cursor is at
     * currently on the second entry in the result set and move(-5) is called,
     * the position will be pinned at -1, and false will be returned.
     *
     * @param offset the offset to be applied from the current position.
     * @return whether the requested move fully succeeded.
     */
    fun move(offset: Int): Boolean

    /**
     * Move the cursor to an absolute position. The valid
     * range of values is -1 &lt;= position &lt;= count.
     *
     *
     * This method will return true if the request destination was reachable,
     * otherwise, it returns false.
     *
     * @param position the zero-based position to move to.
     * @return whether the requested move fully succeeded.
     */
    fun moveToPosition(position: Int): Boolean

    /**
     * Move the cursor to the first row.
     *
     *
     * This method will return false if the cursor is empty.
     *
     * @return whether the move succeeded.
     */
    fun moveToFirst(): Boolean

    /**
     * Move the cursor to the last row.
     *
     *
     * This method will return false if the cursor is empty.
     *
     * @return whether the move succeeded.
     */
    fun moveToLast(): Boolean

    /**
     * Move the cursor to the next row.
     *
     *
     * This method will return false if the cursor is already past the
     * last entry in the result set.
     *
     * @return whether the move succeeded.
     */
    fun moveToNext(): Boolean

    /**
     * Move the cursor to the previous row.
     *
     *
     * This method will return false if the cursor is already before the
     * first entry in the result set.
     *
     * @return whether the move succeeded.
     */
    fun moveToPrevious(): Boolean

    /**
     * Returns whether the cursor is pointing to the first row.
     *
     * @return whether the cursor is pointing at the first entry.
     */
    fun isFirst(): Boolean

    /**
     * Returns whether the cursor is pointing to the last row.
     *
     * @return whether the cursor is pointing at the last entry.
     */
    fun isLast(): Boolean

    /**
     * Returns whether the cursor is pointing to the position before the first
     * row.
     *
     * @return whether the cursor is before the first result.
     */
    fun isBeforeFirst(): Boolean

    /**
     * Returns whether the cursor is pointing to the position after the last
     * row.
     *
     * @return whether the cursor is after the last result.
     */
    fun isAfterLast(): Boolean

    /**
     * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
     * If you expect the column to exist use [.getColumnIndexOrThrow] instead, which
     * will make the error more clear.
     *
     * @param columnName the name of the target column.
     * @return the zero-based column index for the given column name, or -1 if
     * the column name does not exist.
     * @see .getColumnIndexOrThrow
     */
    fun getColumnIndex(columnName: String): Int

    /**
     * Returns the zero-based index for the given column name, or throws
     * [IllegalArgumentException] if the column doesn't exist. If you're not sure if
     * a column will exist or not use [.getColumnIndex] and check for -1, which
     * is more efficient than catching the exceptions.
     *
     * @param columnName the name of the target column.
     * @return the zero-based column index for the given column name
     * @see .getColumnIndex
     * @throws IllegalArgumentException if the column does not exist
     */
    fun getColumnIndexOrThrow(columnName: String): Int

    /**
     * Returns the column name at the given zero-based column index.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the column name for the given column index.
     */
    fun getColumnName(columnIndex: Int): String

    /**
     * Returns a string array holding the names of all of the columns in the
     * result set in the order in which they were listed in the result.
     *
     * @return the names of the columns returned in this query.
     */
    fun getColumnNames(): Array<String>

    /**
     * Return total number of columns
     * @return number of columns
     */
    fun getColumnCount(): Int

    /**
     * Returns the value of the requested column as a byte array.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null or the column type is not a blob type is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a byte array.
     */
    fun getBlob(columnIndex: Int): ByteArray

    /**
     * Returns the value of the requested column as a String.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null or the column type is not a string type is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a String.
     */
    fun getString(columnIndex: Int): String

    /**
     * Returns the value of the requested column as a short.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null, the column type is not an integral type, or the
     * integer value is outside the range [`Short.MIN_VALUE`,
     * `Short.MAX_VALUE`] is implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a short.
     */
    fun getShort(columnIndex: Int): Short

    /**
     * Returns the value of the requested column as an int.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null, the column type is not an integral type, or the
     * integer value is outside the range [`Integer.MIN_VALUE`,
     * `Integer.MAX_VALUE`] is implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as an int.
     */
    fun getInt(columnIndex: Int): Int

    /**
     * Returns the value of the requested column as a long.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null, the column type is not an integral type, or the
     * integer value is outside the range [`Long.MIN_VALUE`,
     * `Long.MAX_VALUE`] is implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a long.
     */
    fun getLong(columnIndex: Int): Long

    /**
     * Returns the value of the requested column as a float.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null, the column type is not a floating-point type, or the
     * floating-point value is not representable as a `float` value is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a float.
     */
    fun getFloat(columnIndex: Int): Float

    /**
     * Returns the value of the requested column as a double.
     *
     *
     * The result and whether this method throws an exception when the
     * column value is null, the column type is not a floating-point type, or the
     * floating-point value is not representable as a `double` value is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a double.
     */
    fun getDouble(columnIndex: Int): Double

    /**
     * Returns data type of the given column's value.
     * The preferred type of the column is returned but the data may be converted to other types
     * as documented in the get-type methods such as [.getInt], [.getFloat]
     * etc.
     *
     *
     * Returned column types are
     *
     *  * [.FIELD_TYPE_NULL]
     *  * [.FIELD_TYPE_INTEGER]
     *  * [.FIELD_TYPE_FLOAT]
     *  * [.FIELD_TYPE_STRING]
     *  * [.FIELD_TYPE_BLOB]
     *
     *
     *
     * @param columnIndex the zero-based index of the target column.
     * @return column value type
     */
    fun getType(columnIndex: Int): Int

    /**
     * Returns `true` if the value in the indicated column is null.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return whether the column value is null.
     */
    fun isNull(columnIndex: Int): Boolean

    /**
     * Closes the Cursor, releasing all of its resources and making it completely invalid.
     */
    fun close()

    /**
     * return true if the cursor is closed
     * @return true if the cursor is closed.
     */
    fun isClosed(): Boolean
}
