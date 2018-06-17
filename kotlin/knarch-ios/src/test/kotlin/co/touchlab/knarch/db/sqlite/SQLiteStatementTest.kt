package co.touchlab.knarch.db.sqlite

import kotlin.test.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*

class SQLiteStatementTest {
    private lateinit var mDatabase:SQLiteDatabase
    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    @BeforeEach
    protected fun setUp() {
        getContext().deleteDatabase(DATABASE_NAME)
        mDatabase = getContext().openOrCreateDatabase(DATABASE_NAME, SystemContext.MODE_PRIVATE, null, null)
        assertNotNull(mDatabase)
        mDatabase.setVersion(CURRENT_DATABASE_VERSION)
    }

    @AfterEach
    protected fun tearDown() {
        mDatabase.close()
        getContext().deleteDatabase(DATABASE_NAME)
    }

    private fun populateDefaultTable() {
        mDatabase.execSQL("CREATE TABLE test (_id INTEGER PRIMARY KEY, data TEXT);")
    }
    
    private fun populateBlobTable() {
        mDatabase.execSQL("CREATE TABLE blob_test (_id INTEGER PRIMARY KEY, data BLOB)")
        for (i in BLOBS.indices)
        {
            val values = ContentValues()
            values.put("_id", i)
            val bytes = BLOBS[i]
            if(bytes == null)
                values.putNull("data")
            else
                values.put("data", bytes)
            mDatabase.insert("blob_test", null, values)
        }
    }

    @Test
    fun testExecute() {
        mDatabase.disableWriteAheadLogging()
        populateDefaultTable()
        assertEquals(0, DatabaseUtils.longForQuery(mDatabase, "select count(*) from test", null))
        // test update
        // insert 2 rows and then update them.
        var statement1 = mDatabase.compileStatement(
                "INSERT INTO test (data) VALUES ('" + STRING2 + "')")
        assertEquals(1, statement1.executeInsert())
        assertEquals(2, statement1.executeInsert())
        var statement2 = mDatabase.compileStatement("UPDATE test set data = 'a' WHERE _id > 0")
        assertEquals(2, statement2.executeUpdateDelete())
        statement2.close()
        // should still have 2 rows in the table
        assertEquals(2, DatabaseUtils.longForQuery(mDatabase, "select count(*) from test", null))
        // test delete
        // insert 2 more rows and delete 3 of them
        assertEquals(3, statement1.executeInsert())
        assertEquals(4, statement1.executeInsert())
        statement1.close()
        statement2 = mDatabase.compileStatement("DELETE from test WHERE _id < 4")
        assertEquals(3, statement2.executeUpdateDelete())
        statement2.close()
        // should still have 1 row1 in the table
        assertEquals(1, DatabaseUtils.longForQuery(mDatabase, "select count(*) from test", null))
        // if the SQL statement is something that causes rows of data to
        // be returned, executeUpdateDelete() (and execute()) throw an exception.
        statement2 = mDatabase.compileStatement("SELECT count(*) FROM test")
        try
        {
            statement2.executeUpdateDelete()
            fail("exception expected")
        }
        catch (e:SQLException) {
            // expected
        }
        finally
        {
            statement2.close()
        }
    }

    @Test
    fun testExecuteInsert() {
        populateDefaultTable()
        var c = mDatabase.query("test", null, null, null, null, null, null)
        assertEquals(0, c.getCount())
        // test insert
        var statement = mDatabase.compileStatement(
                "INSERT INTO test (data) VALUES ('" + STRING2 + "')")
        assertEquals(1, statement.executeInsert())
        statement.close()
        // try to insert another row with the same id. last inserted rowid should be -1
        statement = mDatabase.compileStatement("insert or ignore into test values(1, 1);")
        assertEquals(-1, statement.executeInsert())
        statement.close()
        c = mDatabase.query("test", null, null, null, null, null, null)
        assertEquals(1, c.getCount())
        c.moveToFirst()
        assertEquals(STRING2, c.getString(c.getColumnIndex("data")))
        c.close()
        // if the sql statement is something that causes rows of data to
        // be returned, executeInsert() throws an exception
        statement = mDatabase.compileStatement(
                "SELECT * FROM test WHERE data=\"" + STRING2 + "\"")
        try
        {
            statement.executeInsert()
            fail("exception expected")
        }
        catch (e:SQLException) {
            // expected
        }
        finally
        {
            statement.close()
        }
    }

    @Test
    fun testSimpleQueryForLong() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER NOT NULL, str TEXT NOT NULL);")
        mDatabase.execSQL("INSERT INTO test VALUES (1234, 'hello');")
        var statement = mDatabase.compileStatement("SELECT num FROM test WHERE str = ?")
        // test query long
        statement.bindString(1, "hello")
        var value = statement.simpleQueryForLong()
        assertEquals(1234, value)
        // test query returns zero rows
        statement.bindString(1, "world")
        try
        {
            statement.simpleQueryForLong()
            fail("There should be a SQLiteDoneException thrown out.")
        }
        catch (e:SQLiteException) {
            // expected.
        }
        statement.close()
    }

    @Test
    fun testSimpleQueryForString() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER NOT NULL, str TEXT NOT NULL);")
        mDatabase.execSQL("INSERT INTO test VALUES (1234, 'hello');")
        var statement = mDatabase.compileStatement("SELECT str FROM test WHERE num = ?")
        // test query String
        statement.bindLong(1, 1234)
        var value = statement.simpleQueryForString()
        assertEquals("hello", value)
        // test query returns zero rows
        statement.bindLong(1, 5678)
        try
        {
            statement.simpleQueryForString()
            fail("There should be a SQLiteDoneException thrown out.")
        }
        catch (e:SQLiteException) {
            // expected.
        }
        statement.close()
    }

    companion object {
        private val STRING1 = "this is a test"
        private val STRING2 = "another test"
        private val BLOBS = Array(6
        ) { i ->
            when (i) {
                0 -> parseBlob("86FADCF1A820666AEBD0789F47932151A2EF734269E8AC4E39630AB60519DFD8")
                1 -> ByteArray(1)
                2 -> null
                3 -> parseBlob("00")
                4 -> parseBlob("FF")
                5 -> parseBlob("D7B500FECF25F7A4D83BF823D3858690790F2526013DE6CAE9A69170E2A1E47238")
                else -> throw IllegalArgumentException("Nuh ugh")
            }
        }
        private val DATABASE_NAME = "database_test.db"
        private val CURRENT_DATABASE_VERSION = 42
        /*
         * Convert string of hex digits to byte array.
         * Results are undefined for poorly formed string.
         *
         * @param src hex string
         */
        private fun parseBlob(src:String):ByteArray {
            val len = src.length
            val result = ByteArray(len / 2)
            for (i in 0 until len / 2)
            {
                val `val`:Int
                val c1 = src.get(i * 2)
                val c2 = src.get(i * 2 + 1)
                val val1 = c1.toLong().toString(16).toInt()// Character.digit(c1, 16)
                val val2 = c2.toLong().toString(16).toInt()
                `val` = (val1 shl 4) or val2
                result[i] = `val`.toByte()
            }
            return result
        }
    }
}