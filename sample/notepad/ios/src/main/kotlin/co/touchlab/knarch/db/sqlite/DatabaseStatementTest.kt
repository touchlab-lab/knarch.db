package co.touchlab.knarch.db.sqlite

import kotlin.test.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*

class DatabaseStatementTest{
    private lateinit var mDatabase:SQLiteDatabase
    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    val isPerformanceOnly:Boolean
        get() {
            return false
        }

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
//    // These test can only be run once.
//    fun startPerformance(intermediates:Intermediates):Int {
//        return 1
//    }

    private fun populateDefaultTable() {
        mDatabase.execSQL("CREATE TABLE test (_id INTEGER PRIMARY KEY, data TEXT);")
        mDatabase.execSQL("INSERT INTO test (data) VALUES ('" + sString1 + "');")
        mDatabase.execSQL("INSERT INTO test (data) VALUES ('" + sString2 + "');")
        mDatabase.execSQL("INSERT INTO test (data) VALUES ('" + sString3 + "');")
    }

    @Test
    fun testExecuteStatement() {
        populateDefaultTable()
        val statement = mDatabase.compileStatement("DELETE FROM test")
        statement.execute()
        val c = mDatabase.query("test", null, null, null, null, null, null)
        assertEquals(0, c.getCount())
//        c.deactivate()
        statement.close()
    }

    @Test
    fun testSimpleQuery() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER NOT NULL, str TEXT NOT NULL);")
        mDatabase.execSQL("INSERT INTO test VALUES (1234, 'hello');")
        val statement1 = mDatabase.compileStatement("SELECT num FROM test WHERE str = ?")
        val statement2 = mDatabase.compileStatement("SELECT str FROM test WHERE num = ?")
        try
        {
            statement1.bindString(1, "hello")
            val value = statement1.simpleQueryForLong()
            assertEquals(1234, value)
            statement1.bindString(1, "world")
            statement1.simpleQueryForLong()
            fail("shouldn't get here")
        }
        catch (e:SQLiteException) {
            // expected
        }
        try
        {
            statement2.bindLong(1, 1234)
            val value = statement1.simpleQueryForString()
            assertEquals("hello", value)
            statement2.bindLong(1, 5678)
            statement1.simpleQueryForString()
            fail("shouldn't get here")
        }
        catch (e:SQLiteException) {
            // expected
        }
        statement1.close()
        statement2.close()
    }

    @Test
    fun testStatementLongBinding() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER);")
        val statement = mDatabase.compileStatement("INSERT INTO test (num) VALUES (?)")
        for (i in 0..9)
        {
            statement.bindLong(1, i.toLong())
            statement.execute()
        }
        statement.close()
        val c = mDatabase.query("test", null, null, null, null, null, null)
        val numCol = c.getColumnIndexOrThrow("num")
        c.moveToFirst()
        for (i in 0..9)
        {
            val num = c.getLong(numCol)
            assertEquals(i.toLong(), num)
            c.moveToNext()
        }
        c.close()
    }

    @Test
    fun testStatementStringBinding() {
        mDatabase.execSQL("CREATE TABLE test (num TEXT);")
        val statement = mDatabase.compileStatement("INSERT INTO test (num) VALUES (?)")
        for (i in 0..9)
        {
            statement.bindString(1, i.toString(16))
            statement.execute()
        }
        statement.close()
        val c = mDatabase.query("test", null, null, null, null, null, null)
        val numCol = c.getColumnIndexOrThrow("num")
        c.moveToFirst()
        for (i in 0..9)
        {
            val num = c.getString(numCol)
            assertEquals(i.toString(16), num)
            c.moveToNext()
        }
        c.close()
    }

    @Test
    fun testStatementClearBindings() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER);")
        val statement = mDatabase.compileStatement("INSERT INTO test (num) VALUES (?)")
        for (i in 0..9)
        {
            statement.bindLong(1, i.toLong())
            statement.clearBindings()
            statement.execute()
        }
        statement.close()
        val c = mDatabase.query("test", null, null, null, null, null, "ROWID")
        val numCol = c.getColumnIndexOrThrow("num")
        assertTrue(c.moveToFirst())
        for (i in 0..9)
        {
            assertTrue(c.isNull(numCol))
            c.moveToNext()
        }
        c.close()
    }

    @Test
    fun testSimpleStringBinding() {
        mDatabase.execSQL("CREATE TABLE test (num TEXT, value TEXT);")
        val statement = "INSERT INTO test (num, value) VALUES (?,?)"
        val args = arrayOfNulls<String>(2)
        for (i in 0..1)
        {
            args[i] = i.toString(16)
        }
        mDatabase.execSQL(statement, args as Array<Any?>)
        val c = mDatabase.query("test", null, null, null, null, null, null)
        val numCol = c.getColumnIndexOrThrow("num")
        val valCol = c.getColumnIndexOrThrow("value")
        c.moveToFirst()
        val num = c.getString(numCol)
        assertEquals("0", num)
        val `val` = c.getString(valCol)
        assertEquals("1", `val`)
        c.close()
    }

    @Test
    fun testStatementMultipleBindings() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER, str TEXT);")
        val statement = mDatabase.compileStatement("INSERT INTO test (num, str) VALUES (?, ?)")
        for (i in 0..9)
        {
            statement.bindLong(1, i.toLong())
            statement.bindString(2, i.toString(16))
            statement.execute()
        }
        statement.close()
        val c = mDatabase.query("test", null, null, null, null, null, "ROWID")
        val numCol = c.getColumnIndexOrThrow("num")
        val strCol = c.getColumnIndexOrThrow("str")
        assertTrue(c.moveToFirst())
        for (i in 0..9)
        {
            val num = c.getLong(numCol)
            val str = c.getString(strCol)
            assertEquals(i.toLong(), num)
            assertEquals(i.toString(16), str)
            c.moveToNext()
        }
        c.close()
    }

    /*private class StatementTestThread(db:SQLiteDatabase, statement:SQLiteStatement):Thread() {
        private val mDatabase:SQLiteDatabase
        private val mStatement:SQLiteStatement
        init{
            mDatabase = db
            mStatement = statement
        }
        public override fun run() {
            mDatabase.beginTransaction()
            for (i in 0..9)
            {
                mStatement.bindLong(1, i)
                mStatement.bindString(2, java.lang.Long.toHexString(i))
                mStatement.execute()
            }
            mDatabase.setTransactionSuccessful()
            mDatabase.endTransaction()
            val c = mDatabase.query("test", null, null, null, null, null, "ROWID")
            val numCol = c.getColumnIndexOrThrow("num")
            val strCol = c.getColumnIndexOrThrow("str")
            assertTrue(c.moveToFirst())
            for (i in 0..9)
            {
                val num = c.getLong(numCol)
                val str = c.getString(strCol)
                assertEquals(i, num)
                assertEquals(java.lang.Long.toHexString(i), str)
                c.moveToNext()
            }
            c.close()
        }
    }*/

    /*@Test
    fun testStatementMultiThreaded() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER, str TEXT);")
        val statement = mDatabase.compileStatement("INSERT INTO test (num, str) VALUES (?, ?)")
        val thread = StatementTestThread(mDatabase, statement)
        thread.start()
        try
        {
            thread.join()
        }
        finally
        {
            statement.close()
        }
    }*/

    @Test
    fun testStatementConstraint() {
        mDatabase.execSQL("CREATE TABLE test (num INTEGER NOT NULL);")
        val statement = mDatabase.compileStatement("INSERT INTO test (num) VALUES (?)")
        // Try to insert NULL, which violates the constraint
        try
        {
            statement.clearBindings()
            statement.execute()
            fail("expected exception not thrown")
        }
        catch (e:SQLiteException) {
            // expected
        }
        // Make sure the statement can still be used
        statement.bindLong(1, 1)
        statement.execute()
        statement.close()
        val c = mDatabase.query("test", null, null, null, null, null, null)
        val numCol = c.getColumnIndexOrThrow("num")
        c.moveToFirst()
        val num = c.getLong(numCol)
        assertEquals(1, num)
        c.close()
    }

    companion object {
        private val sString1 = "this is a test"
        private val sString2 = "and yet another test"
        private val sString3 = "this string is a little longer, but still a test"
        private val DATABASE_NAME = "database_test.db"
        private val CURRENT_DATABASE_VERSION = 42
    }
}