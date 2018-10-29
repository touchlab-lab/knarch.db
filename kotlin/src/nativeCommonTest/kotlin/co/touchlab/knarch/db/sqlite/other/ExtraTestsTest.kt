/*
 * Copyright (c) 2018 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.knarch.db.sqlite.other

import co.touchlab.knarch.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*
import kotlin.test.*
import platform.Foundation.*
import kotlinx.cinterop.*

class ExtraTestsTest{
    private lateinit var mDatabase:SQLiteDatabase
    private var mDatabaseFile:File?=null
    private var mDatabaseFilePath:String?=null
    private var mDatabaseDir:String?=null

    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    @BeforeEach
    protected fun setUp() {
        getContext().deleteDatabase(DATABASE_FILE_NAME)
        mDatabaseFilePath = getContext().getDatabasePath(DATABASE_FILE_NAME).path
        mDatabaseFile = getContext().getDatabasePath(DATABASE_FILE_NAME)
        mDatabaseDir = mDatabaseFile?.getParent()
        mDatabaseFile?.getParentFile()?.mkdirs() // directory may not exist
        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDatabaseFilePath!!, null)
        assertNotNull(mDatabase)
    }

    @AfterEach
    protected fun tearDown() {
        closeAndDeleteDatabase()
    }

    private fun closeAndDeleteDatabase() {
        mDatabase.close()
        SQLiteDatabase.deleteDatabase(mDatabaseFile!!)
    }

    @Test
    fun testBigData() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER, astr TEXT);")
        val stmt = mDatabase.compileStatement("INSERT INTO test (num, astr) VALUES (?, ?)")
        mDatabase.beginTransaction()
        try {

            for(i in 0 until 100000){
                if(i % 10000 == 0){
                    println("Inserting $i")
                }
                val insStr = "OK big string insert val $i oh Binky is sad because food"
                stmt.bindLong(1, i.toLong())
                stmt.bindString(2, insStr)
                stmt.executeInsert()
            }

            mDatabase.setTransactionSuccessful()
        } finally {
            mDatabase.endTransaction();
        }

        val cursor = mDatabase.query("test",
                null,
                null,
                null,
                null,
                null,
                null,
                null
                )

        val numCol = cursor.getColumnIndex("num")
        val aStrCol = cursor.getColumnIndex("astr")
        var rowCount = 0

        while (cursor.moveToNext()){
            if(rowCount % 10000 == 0){
                println("${cursor.getInt(numCol)}/${cursor.getString(aStrCol)}")
            }
            rowCount++
        }

        cursor.close()
    }

    companion object {
        private val TAG = "SQLiteDatabaseTest"
        private val DATABASE_FILE_NAME = "database_test.db"
        private val TABLE_NAME = "test"
        private val COLUMN_ID_INDEX = 0
        private val COLUMN_NAME_INDEX = 1
        private val COLUMN_AGE_INDEX = 2
        private val COLUMN_ADDR_INDEX = 3
        private val TEST_PROJECTION = arrayOf<String>("_id", // 0
                "name", // 1
                "age", // 2
                "address" // 3
        )
    }
}