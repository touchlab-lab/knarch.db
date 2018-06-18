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

class SQLiteFtsTest {
    private lateinit var mDatabase:SQLiteDatabase

    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    @BeforeEach
    fun setUp() {
        mDatabase = getContext().openOrCreateDatabase("CTS_FTS", SystemContext.MODE_PRIVATE, null, null)
    }

    @AfterEach
    fun tearDown() {
        val path = mDatabase.getPath()
        mDatabase.close()
        SQLiteDatabase.deleteDatabase(File(path))
    }

    @Test
    fun testFts3Porter() {
        prepareFtsTable(TEST_TABLE, "fts3", "tokenize=porter")
        // Porter should include stemmed words
        val cursor = queryFtsTable(TEST_TABLE, "technology")
        try
        {
            assertEquals(2, cursor.getCount())
            cursor.moveToPosition(0)
            assertTrue(cursor.getString(0).contains(">TECHnology<"))
            cursor.moveToPosition(1)
            assertTrue(cursor.getString(0).contains(">technologies<"))
        }
        finally
        {
            cursor.close()
        }
    }

    @Test
    fun testFts3Simple() {
        prepareFtsTable(TEST_TABLE, "fts3", "tokenize=simple")
        // Simple shouldn't include stemmed words
        val cursor = queryFtsTable(TEST_TABLE, "technology")
        try
        {
            assertEquals(1, cursor.getCount())
            cursor.moveToPosition(0)
            assertTrue(cursor.getString(0).contains(">TECHnology<"))
        }
        finally
        {
            cursor.close()
        }
    }

    @Test
    fun testFts4Simple() {
        prepareFtsTable(TEST_TABLE, "fts4", "tokenize=simple")
        // Simple shouldn't include stemmed words
        val cursor = queryFtsTable(TEST_TABLE, "technology")
        try
        {
            assertEquals(1, cursor.getCount())
            cursor.moveToPosition(0)
            assertTrue(cursor.getString(0).contains(">TECHnology<"))
        }
        finally
        {
            cursor.close()
        }
    }

    private fun prepareFtsTable(table:String, ftsType:String, options:String) {
        mDatabase.execSQL(
                ("CREATE VIRTUAL TABLE " + table + " USING " + ftsType
                        + "(content TEXT, " + options + ");"))
//        val res = getContext().getResources()
        val values = ContentValues()
        for (content in TEST_CONTENT)
        {
            values.clear()
            values.put("content", content)
            mDatabase.insert(table, null, values)
        }
    }

    private fun queryFtsTable(table:String, match:String):Cursor {
        return mDatabase.query(table, arrayOf<String>("snippet(" + table + ")"),
                "content MATCH ?", arrayOf<String>(match), null, null, "rowid ASC")
    }

    companion object {
        private val TEST_TABLE = "cts_fts"
        private val TEST_CONTENT = arrayOf<String>("Any sufficiently advanced TECHnology is indistinguishable from magic.", "Those who would give up Essential Liberty to purchase a little Temporary Safety, deserve neither Liberty nor Safety.", "It is poor civic hygiene to install technologies that could someday facilitate a police state.")
    }
}