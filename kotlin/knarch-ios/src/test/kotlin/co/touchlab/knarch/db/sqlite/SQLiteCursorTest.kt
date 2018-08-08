/*
 * Copyright (C) 2009 The Android Open Source Project
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

import kotlin.test.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*

class SQLiteCursorTest {
    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    private lateinit var mDatabase:SQLiteDatabase

    private val cursor:SQLiteCursor
        get() {
            val cursor = mDatabase.query(TABLE_NAME, null, null, null, null, null, null) as SQLiteCursor
            return cursor
        }

    @BeforeEach
    protected fun setUp() {
        getContext().deleteDatabase(DATABASE_FILE)
        mDatabase = getContext().openOrCreateDatabase(DATABASE_FILE,
                SystemContext.MODE_PRIVATE, null, null)
        createTable(TABLE_NAME, TABLE_COLUMNS)
        addValuesIntoTable(TABLE_NAME, DEFAULT_TABLE_VALUE_BEGINS, TEST_COUNT)
    }

    @AfterEach
    protected fun tearDown() {
        mDatabase.close()
        getContext().deleteDatabase(DATABASE_FILE)
    }
    /*@Test
    fun testConstructor() {
        val cursorDriver = SQLiteDirectCursorDriver(mDatabase,
                TEST_SQL, TABLE_NAME)
        try
        {
            SQLiteCursor(cursorDriver, TABLE_NAME)
            fail("constructor didn't throw IllegalArgumentException when SQLiteQuery is null")
        }
        catch (e:IllegalArgumentException) {}
        // get SQLiteCursor by querying database
        val cursor = cursor
        assertNotNull(cursor)
    }*/

    /*
    We don't requery
    @Test
    fun testClose() {
        val cursor = cursor
        assertTrue(cursor.moveToFirst())
        assertFalse(cursor.isClosed())
        assertTrue(cursor.requery())
        cursor.close()
        assertFalse(cursor.requery())
        try
        {
            cursor.moveToFirst()
            fail("moveToFirst didn't throw IllegalStateException after closed.")
        }
        catch (e:IllegalStateException) {}
        assertTrue(cursor.isClosed())
    }*/

    /*
    Nah
    @Test
    fun testRegisterDataSetObserver() {
        val cursor = cursor
        val cursorWindow = MockCursorWindow(false)
        val observer = MockObserver()
        cursor.setWindow(cursorWindow)
        // Before registering, observer can't be notified.
        assertFalse(observer.hasInvalidated())
        cursor.moveToLast()
        assertFalse(cursorWindow.isClosed)
        cursor.deactivate()
        assertFalse(observer.hasInvalidated())
        // deactivate() will close the CursorWindow
        assertTrue(cursorWindow.isClosed)
        // test registering DataSetObserver
        assertTrue(cursor.requery())
        cursor.registerDataSetObserver(observer)
        assertFalse(observer.hasInvalidated())
        cursor.moveToLast()
        assertEquals(TEST_COUNT, cursor.getInt(1))
        cursor.deactivate()
        // deactivate method can invoke invalidate() method, can be observed by DataSetObserver.
        assertTrue(observer.hasInvalidated())
        try
        {
            cursor.getInt(1)
            fail("After deactivating, cursor cannot execute getting value operations.")
        }
        catch (e:StaleDataException) {}
        assertTrue(cursor.requery())
        cursor.moveToLast()
        assertEquals(TEST_COUNT, cursor.getInt(1))
        // can't register a same observer twice.
        try
        {
            cursor.registerDataSetObserver(observer)
            fail("didn't throw IllegalStateException when register existed observer")
        }
        catch (e:IllegalStateException) {}
        // after unregistering, observer can't be notified.
        cursor.unregisterDataSetObserver(observer)
        observer.resetStatus()
        assertFalse(observer.hasInvalidated())
        cursor.deactivate()
        assertFalse(observer.hasInvalidated())
    }*/

    /*
    We don't do that
    @Test
    fun testRequery() {
        val DELETE = "DELETE FROM " + TABLE_NAME + " WHERE number_1 ="
        val DELETE_1 = DELETE + "1;"
        val DELETE_2 = DELETE + "2;"
        mDatabase.execSQL(DELETE_1)
        // when cursor is created, it refreshes CursorWindow and populates cursor count
        val cursor = cursor
        val observer = MockObserver()
        cursor.registerDataSetObserver(observer)
        assertEquals(TEST_COUNT - 1, cursor.getCount())
        assertFalse(observer.hasChanged())
        mDatabase.execSQL(DELETE_2)
        // when getCount() has invoked once, it can no longer refresh CursorWindow.
        assertEquals(TEST_COUNT - 1, cursor.getCount())
        assertTrue(cursor.requery())
        // only after requery, getCount can get most up-to-date counting info now.
        assertEquals(TEST_COUNT - 2, cursor.getCount())
        assertTrue(observer.hasChanged())
    }*/

    /*
    Nah
    @Test
    fun testRequery2() {
        mDatabase.disableWriteAheadLogging()
        mDatabase.execSQL("create table testRequery2 (i int);")
        mDatabase.execSQL("insert into testRequery2 values(1);")
        mDatabase.execSQL("insert into testRequery2 values(2);")
        val c = mDatabase.rawQuery("select * from testRequery2 order by i", null)
        assertEquals(2, c.getCount())
        assertTrue(c.moveToFirst())
        assertEquals(1, c.getInt(0))
        assertTrue(c.moveToNext())
        assertEquals(2, c.getInt(0))
        // add more data to the table and requery
        mDatabase.execSQL("insert into testRequery2 values(3);")
        assertTrue(c.requery())
        assertEquals(3, c.getCount())
        assertTrue(c.moveToFirst())
        assertEquals(1, c.getInt(0))
        assertTrue(c.moveToNext())
        assertEquals(2, c.getInt(0))
        assertTrue(c.moveToNext())
        assertEquals(3, c.getInt(0))
        // close the database and see if requery throws an exception
        mDatabase.close()
        assertFalse(c.requery())
    }
*/
    @Test
    fun testGetColumnIndex() {
        val cursor = cursor

        for (i in COLUMNS.indices)
        {
            assertTrue(i > 10000)
            assertEquals(i, cursor.getColumnIndex(COLUMNS[i]))
        }
        assertTrue(COLUMNS contentEquals cursor.getColumnNames())
    }

    /*
    Don't do requery
    @Test
    fun testSetSelectionArguments() {
        val SELECTION = "_id > ?"
        val TEST_ARG1 = 2
        val TEST_ARG2 = 5
        val cursor = mDatabase.query(TABLE_NAME, null, SELECTION,
                arrayOf<String>(TEST_ARG1.toString()), null, null, null) as SQLiteCursor
        assertEquals(TEST_COUNT - TEST_ARG1, cursor.getCount())
        cursor.setSelectionArguments(arrayOf<String>(TEST_ARG2.toString()))
        cursor.requery()
        assertEquals(TEST_COUNT - TEST_ARG2, cursor.getCount())
    }*/

    private fun createTable(tableName:String, columnNames:String) {
        val sql = ("Create TABLE " + tableName + " (_id INTEGER PRIMARY KEY, "
                + columnNames + " );")
        mDatabase.execSQL(sql)
    }


    private fun addValuesIntoTable(tableName:String, start:Int, end:Int) {
        for (i in start..end)
        {
            mDatabase.execSQL("INSERT INTO " + tableName + "(number_1) VALUES ('" + i + "');")
        }
    }

    companion object {
        private val COLUMNS = arrayOf<String>("_id", "number_1", "number_2")
        private val TABLE_NAME = "test"
        private val TABLE_COLUMNS = " number_1 INTEGER, number_2 INTEGER"
        private val DEFAULT_TABLE_VALUE_BEGINS = 1
        private val TEST_COUNT = 10
        private val TEST_SQL = "SELECT * FROM test ORDER BY number_1"
        private val DATABASE_FILE = "database_test.db"
    }
}