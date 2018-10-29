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

import co.touchlab.knarch.db.DatabaseUtils

abstract class SQLiteProgram(
        db: SQLiteDatabase,
        sql: String,
        bindArgs: Array<Any?>?
) : SQLiteClosable() {

    private val EMPTY_STRING_ARRAY = arrayOf<String>()
    private val mDatabase:SQLiteDatabase = db
    private val mSql:String = sql.trim({ it <= ' ' })
    private val mReadOnly:Boolean
    private val mColumnNames:Array<String>
    private val mNumParameters:Int
    private val mBindArgs:Array<Any?>?

    fun getDatabase():SQLiteDatabase {
        return mDatabase
    }
    fun getSql():String {
        return mSql
    }
    fun getBindArgs():Array<Any?>? {
        return mBindArgs
    }
    fun getColumnNames():Array<String> {
        return mColumnNames
    }
    /** @hide */
    protected fun getSession():SQLiteSession {
        return mDatabase.getThreadSession()
    }
    /** @hide */
    protected fun getConnectionFlags():Int {
        return mDatabase.getThreadDefaultConnectionFlags(mReadOnly)
    }
    /** @hide */
    protected fun onCorruption() {
        mDatabase.onCorruption()
    }

    /**
     * Bind a NULL value to this statement. The value remains bound until
     * {@link #clearBindings} is called.
     *
     * @param index The 1-based index to the parameter to bind null to
     */
    fun bindNull(index:Int) {
        bind(index, null)
    }

    /**
     * Bind a long value to this statement. The value remains bound until
     * {@link #clearBindings} is called.
     *addToBindArgs
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind
     */
    fun bindLong(index:Int, value:Long) {
        bind(index, value)
    }
    /**
     * Bind a double value to this statement. The value remains bound until
     * {@link #clearBindings} is called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind
     */
    fun bindDouble(index:Int, value:Double) {
        bind(index, value)
    }
    /**
     * Bind a String value to this statement. The value remains bound until
     * {@link #clearBindings} is called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind, must not be null
     */
    fun bindString(index:Int, value:String) {
        bind(index, value)
    }
    /**
     * Bind a byte array value to this statement. The value remains bound until
     * {@link #clearBindings} is called.
     *
     * @param index The 1-based index to the parameter to bind
     * @param value The value to bind, must not be null
     */
    fun bindBlob(index:Int, value:ByteArray) {
        bind(index, value)
    }
    /**
     * Clears all existing bindings. Unset bindings are treated as NULL.
     */
    fun clearBindings() {
        if (mBindArgs != null)
        {
            for(i in 0 until mBindArgs.size)
            {
                mBindArgs[i] = null
            }
        }
    }
    /**
     * Given an array of String bindArgs, this method binds all of them in one single call.
     *
     * @param bindArgs the String array of bind args, none of which must be null.
     */
    fun bindAllArgsAsStrings(bindArgs:Array<String>?) {
        if (bindArgs != null)
        {
            for (i in bindArgs.size downTo 1)
            {
                bindString(i, bindArgs[i - 1])
            }
        }
    }

    override fun onAllReferencesReleased() {
        clearBindings()
    }

    private fun bind(index:Int, value:Any?) {
        if (index < 1 || index > mNumParameters)
        {
            throw IllegalArgumentException(("Cannot bind argument at index "
                    + index + " because the index is out of range. "
                    + "The statement has " + mNumParameters + " parameters."))
        }
        if (mBindArgs == null) {
            throw NullPointerException("bindArgs is null. Can't call 'bind' with null bind args")
        }
        mBindArgs[index - 1] = value
    }

    init {
        val n = DatabaseUtils.getSqlStatementType(mSql)
        when (n) {
            DatabaseUtils.STATEMENT_BEGIN, DatabaseUtils.STATEMENT_COMMIT, DatabaseUtils.STATEMENT_ABORT -> {
                mReadOnly = false
                mColumnNames = EMPTY_STRING_ARRAY
                mNumParameters = 0
            }
            else -> {
                val info = SQLiteStatementInfo()
                db.getThreadSession().prepare(mSql, info)
                mReadOnly = info.readOnly
                mColumnNames = info.columnNames
                mNumParameters = info.numParameters
            }
        }
        if (bindArgs != null && bindArgs.size > mNumParameters)
        {
            throw IllegalArgumentException(("Too many bind arguments. "
                    + bindArgs.size + " arguments were provided but the statement needs "
                    + mNumParameters + " arguments."))
        }
        if (mNumParameters != 0)
        {
            mBindArgs = arrayOfNulls<Any?>(mNumParameters)
            if (bindArgs != null)
            {
                for(i in 0 until bindArgs.size){
                    mBindArgs[i] = bindArgs[i]
                }
            }
        }
        else
        {
            mBindArgs = null
        }
    }
}