package co.touchlab.kurgan.architecture.database.support

interface SupportSQLiteProgram{
    /**
     * Bind a NULL value to this statement. The value remains bound until
     * [.clearBindings] is called.
     *
     * @param index The 1-based index to the parameter to bind null to
     */
    fun bindNull(index: Int)

    /**
     * Bind a long value to this statement. The value remains bound until
     * [.clearBindings] is called.
     * addToBindArgs
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind
     */
    fun bindLong(index: Int, value: Long)

    /**
     * Bind a double value to this statement. The value remains bound until
     * [.clearBindings] is called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind
     */
    fun bindDouble(index: Int, value: Double)

    /**
     * Bind a String value to this statement. The value remains bound until
     * [.clearBindings] is called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind, must not be null
     */
    fun bindString(index: Int, value: String)

    /**
     * Bind a byte array value to this statement. The value remains bound until
     * [.clearBindings] is called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind, must not be null
     */
    fun bindBlob(index: Int, value: ByteArray)

    /**
     * Clears all existing bindings. Unset bindings are treated as NULL.
     */
    fun clearBindings()
}