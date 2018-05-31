package co.touchlab.knarch.db

import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*
import kotlin.test.*

class DatabaseUtilsTest {
    private lateinit var mDatabase:SQLiteDatabase
    private lateinit var mDatabaseFile:File

    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    @BeforeEach
    protected fun setUp() {
        val dbDir = getContext().getDir("tests", SystemContext.MODE_PRIVATE)
        mDatabaseFile = File(dbDir, "database_test.db")
        if (mDatabaseFile.exists())
        {
            mDatabaseFile.delete()
        }
        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDatabaseFile.getPath(), null)
        assertNotNull(mDatabase)
        mDatabase.execSQL(("CREATE TABLE " + TABLE_NAME + " (_id INTEGER PRIMARY KEY, " +
                "name TEXT, age INTEGER, address TEXT);"))
        mDatabase.execSQL(
                "CREATE TABLE blob_test (_id INTEGER PRIMARY KEY, name TEXT, data BLOB)")
        mDatabase.execSQL(
                "CREATE TABLE boolean_test (_id INTEGER PRIMARY KEY, value BOOLEAN)")
    }

    @AfterEach
    protected fun tearDown() {
        mDatabase.close()
        mDatabaseFile.delete()
    }

    @Test
    fun testAppendEscapedSQLString() {
        var expected = "name='Mike'"
        var sb = StringBuilder("name=")
        DatabaseUtils.appendEscapedSQLString(sb, "Mike")
        assertEquals(expected, sb.toString())
        expected = "'name=''Mike'''"
        sb = StringBuilder()
        DatabaseUtils.appendEscapedSQLString(sb, "name='Mike'")
        assertEquals(expected, sb.toString())
    }

    @Test
    fun testSqlEscapeString() {
        val expected = "'Jack'"
        assertEquals(expected, DatabaseUtils.sqlEscapeString("Jack"))
    }

    @Test
    fun testAppendValueToSql() {
        var expected = "address='LA'"
        var sb = StringBuilder("address=")
        DatabaseUtils.appendValueToSql(sb, "LA")
        assertEquals(expected, sb.toString())
        expected = "address=NULL"
        sb = StringBuilder("address=")
        DatabaseUtils.appendValueToSql(sb, null)
        assertEquals(expected, sb.toString())
        expected = "flag=1"
        sb = StringBuilder("flag=")
        DatabaseUtils.appendValueToSql(sb, true)
        assertEquals(expected, sb.toString())
    }

    @Test
    fun testBindObjectToProgram() {
        var name = "Mike"
        var age = 21
        var address = "LA"
        // at the beginning, there are no records in the database.
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        assertEquals(0, cursor.getCount())
        var sql = "INSERT INTO " + TABLE_NAME + " (name, age, address) VALUES (?, ?, ?);"
        var statement = mDatabase.compileStatement(sql)
        DatabaseUtils.bindObjectToProgram(statement, 1, name)
        DatabaseUtils.bindObjectToProgram(statement, 2, age)
        DatabaseUtils.bindObjectToProgram(statement, 3, address)
        statement.execute()
        statement.close()
        cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        assertEquals(1, cursor.getCount())
        cursor.moveToFirst()
        assertEquals(name, cursor.getString(1))
        assertEquals(age, cursor.getInt(2))
        assertEquals(address, cursor.getString(3))
        cursor.close()
    }

    @Test
    fun testCreateDbFromSqlStatements() {
        var dbName = "ExampleName"
        var sqls = ("CREATE TABLE " + TABLE_NAME + " (_id INTEGER PRIMARY KEY, name TEXT);\n"
                + "INSERT INTO " + TABLE_NAME + " (name) VALUES ('Mike');\n")
        DatabaseUtils.createDbFromSqlStatements(getContext(), dbName, 1, sqls)
        var db = getContext().openOrCreateDatabase(dbName, 0, null, null)
        var PROJECTION = arrayOf<String>("_id", // 0
                "name" // 1
        )
        var cursor = db.query(TABLE_NAME, PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        assertEquals(1, cursor.getCount())
        cursor.moveToFirst()
        assertEquals("Mike", cursor.getString(1))
        cursor.close()
        getContext().deleteDatabase(dbName)
    }

    //This is the failing one
    @Test
    fun testCursorDoubleToContentValues() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var contentValues = ContentValues()
        var key = "key"
        cursor.moveToFirst()
        DatabaseUtils.cursorDoubleToContentValues(cursor, "age", contentValues, key)
        assertEquals(20.0, contentValues.getAsDouble(key))
        DatabaseUtils.cursorDoubleToContentValues(cursor, "Error Field Name", contentValues, key)
        assertNull(contentValues.getAsDouble(key))
        DatabaseUtils.cursorDoubleToContentValues(cursor, "name", contentValues, key)
        assertEquals(0.0, contentValues.getAsDouble(key))
        cursor.close()
    }

    @Test
    fun testCursorDoubleToCursorValues() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var contentValues = ContentValues()
        cursor.moveToFirst()
        DatabaseUtils.cursorDoubleToCursorValues(cursor, "age", contentValues)
        assertEquals(20.0, contentValues.getAsDouble("age")!!)
        DatabaseUtils.cursorDoubleToCursorValues(cursor, "Error Field Name", contentValues)
        assertNull(contentValues.getAsDouble("Error Field Name"))
        DatabaseUtils.cursorDoubleToCursorValues(cursor, "name", contentValues)
        assertEquals(0.0, contentValues.getAsDouble("name")!!)
        cursor.close()
    }

    @Test
    fun testCursorIntToContentValues() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var contentValues = ContentValues()
        var key = "key"
        cursor.moveToFirst()
        DatabaseUtils.cursorIntToContentValues(cursor, "age", contentValues, key)
        assertEquals(20, contentValues.getAsInteger(key)!!)
        DatabaseUtils.cursorIntToContentValues(cursor, "Error Field Name", contentValues, key)
        assertNull(contentValues.getAsInteger(key))
        DatabaseUtils.cursorIntToContentValues(cursor, "name", contentValues, key)
        assertEquals(0, contentValues.getAsInteger(key)!!)
        contentValues = ContentValues()
        DatabaseUtils.cursorIntToContentValues(cursor, "age", contentValues)
        assertEquals(20, contentValues.getAsInteger("age")!!)
        DatabaseUtils.cursorIntToContentValues(cursor, "Error Field Name", contentValues)
        assertNull(contentValues.getAsInteger("Error Field Name"))
        DatabaseUtils.cursorIntToContentValues(cursor, "name", contentValues)
        assertEquals(0, contentValues.getAsInteger("name")!!)
        cursor.close()
    }

    @Test
    fun testcursorLongToContentValues() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var contentValues = ContentValues()
        var key = "key"
        cursor.moveToNext()
        DatabaseUtils.cursorLongToContentValues(cursor, "age", contentValues, key)
        assertEquals(20.toLong(), contentValues.getAsLong(key)!!)
        DatabaseUtils.cursorLongToContentValues(cursor, "Error Field Name", contentValues, key)
        assertNull(contentValues.getAsLong(key))
        DatabaseUtils.cursorLongToContentValues(cursor, "name", contentValues, key)
        assertEquals(0.toLong(), contentValues.getAsLong(key)!!)
        contentValues = ContentValues()
        DatabaseUtils.cursorLongToContentValues(cursor, "age", contentValues)
        assertEquals(20.toLong(), contentValues.getAsLong("age")!!)
        DatabaseUtils.cursorLongToContentValues(cursor, "Error Field Name", contentValues)
        assertNull(contentValues.getAsLong("Error Field Name"))
        DatabaseUtils.cursorLongToContentValues(cursor, "name", contentValues)
        assertEquals(0.toLong(), contentValues.getAsLong("name")!!)
        cursor.close()
    }

    @Test
    fun testCursorRowToContentValues() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var contentValues = ContentValues()
        cursor.moveToNext()
        DatabaseUtils.cursorRowToContentValues(cursor, contentValues)
        assertEquals("Mike", contentValues.get("name") as String)
        assertEquals("20", contentValues.get("age") as String)
        assertEquals("LA", contentValues.get("address") as String)
        mDatabase.execSQL(("INSERT INTO boolean_test (value)" + " VALUES (0);"))
        mDatabase.execSQL(("INSERT INTO boolean_test (value)" + " VALUES (1);"))
        cursor = mDatabase.query("boolean_test", arrayOf<String>("value"), null, null, null, null, null)
        assertNotNull(cursor)
        contentValues = ContentValues()
        cursor.moveToNext()
        DatabaseUtils.cursorRowToContentValues(cursor, contentValues)
        println("TTTTTTTTTTTTTTTT")
        println(contentValues.toString())
        println("TTTTTTTTTTTTTTTT")
        assertFalse(contentValues.getAsBoolean("value")!!)
        cursor.moveToNext()
        DatabaseUtils.cursorRowToContentValues(cursor, contentValues)
        println("TTTTTTTTTTTTTTTT")
        println(contentValues.toString())
        println("TTTTTTTTTTTTTTTT")
        assertTrue(contentValues.getAsBoolean("value")!!)
        cursor.close()
    }

    @Test
    fun testCursorStringToContentValues() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var contentValues = ContentValues()
        var key = "key"
        cursor.moveToNext()
        DatabaseUtils.cursorStringToContentValues(cursor, "age", contentValues, key)
        assertEquals("20", contentValues.get(key) as String)
        try
        {
            DatabaseUtils.cursorStringToContentValues(cursor, "Error Field Name",
                    contentValues, key)
            fail("should throw IllegalArgumentException.")
        }
        catch (e:IllegalArgumentException) {
            // expected
        }
        DatabaseUtils.cursorStringToContentValues(cursor, "name", contentValues, key)
        assertEquals("Mike", contentValues.get(key))
        contentValues = ContentValues()
        DatabaseUtils.cursorStringToContentValues(cursor, "age", contentValues)
        assertEquals("20", contentValues.get("age"))
        try
        {
            DatabaseUtils.cursorStringToContentValues(cursor, "Error Field Name", contentValues)
            fail("should throw IllegalArgumentException.")
        }
        catch (e:IllegalArgumentException) {
            // expected
        }
        DatabaseUtils.cursorStringToContentValues(cursor, "name", contentValues)
        assertEquals("Mike", contentValues.get("name"))
        cursor.close()
    }

    /*@Test
    fun testCursorStringToInsertHelper() {
        // create a new table.
        mDatabase.execSQL(("CREATE TABLE test_copy (_id INTEGER PRIMARY KEY, " + "name TEXT, age INTEGER, address TEXT);"))
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query("test_copy", TEST_PROJECTION, null, null, null, null, null)
        assertEquals(0, cursor.getCount())
        var insertHelper = InsertHelper(mDatabase, "test_copy")
        var indexName = insertHelper.getColumnIndex("name")
        var indexAge = insertHelper.getColumnIndex("age")
        var indexAddress = insertHelper.getColumnIndex("address")
        cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        cursor.moveToNext()
        insertHelper.prepareForInsert()
        DatabaseUtils.cursorStringToInsertHelper(cursor, "name", insertHelper, indexName)
        DatabaseUtils.cursorStringToInsertHelper(cursor, "age", insertHelper, indexAge)
        DatabaseUtils.cursorStringToInsertHelper(cursor, "address", insertHelper, indexAddress)
        insertHelper.execute()
        cursor = mDatabase.query("test_copy", TEST_PROJECTION, null, null, null, null, null)
        assertEquals(1, cursor.getCount())
        cursor.moveToNext()
        assertEquals("Mike", cursor.getString(1))
        assertEquals(20, cursor.getInt(2))
        assertEquals("LA", cursor.getString(3))
    }*/

    @Test
    fun testDumpCurrentRow() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        cursor.moveToNext()
        var expected = "0 {\n _id=1\n name=Mike\n age=20\n address=LA\n}\n"
        /*DatabaseUtils.dumpCurrentRow(cursor)
        var bos = ByteArrayOutputStream()
        var os = PrintStream(bos)
        DatabaseUtils.dumpCurrentRow(cursor, os)
        os.flush()
        os.close()
        assertEquals(expected, bos.toString())*/
        var sb = StringBuilder()
        DatabaseUtils.dumpCurrentRow(cursor, sb)
        assertEquals(expected, sb.toString())
        assertEquals(expected, DatabaseUtils.dumpCurrentRowToString(cursor))
        cursor.close()
    }

    @Test
    fun testDumpCursor() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Jack', '30', 'London');"))
        var cursor = mDatabase.query(TABLE_NAME, TEST_PROJECTION, null, null, null, null, null)
        assertNotNull(cursor)
        var pos = cursor.getPosition()
        var expected = (">>>>> Dumping cursor " + cursor + "\n" +
                "0 {\n" +
                " _id=1\n" +
                " name=Mike\n" +
                " age=20\n" +
                " address=LA\n" +
                "}\n" +
                "1 {\n" +
                " _id=2\n" +
                " name=Jack\n" +
                " age=30\n" +
                " address=London\n" +
                "}\n" +
                "<<<<<\n")
        /*DatabaseUtils.dumpCursor(cursor)
        var bos = ByteArrayOutputStream()
        var os = PrintStream(bos)
        DatabaseUtils.dumpCursor(cursor, os)
        os.flush()
        os.close()
        assertEquals(pos, cursor.getPosition()) // dumpCursor should not change status of cursor
        assertEquals(expected, bos.toString())*/
        var sb = StringBuilder()
        DatabaseUtils.dumpCursor(cursor, sb)
        assertEquals(pos, cursor.getPosition()) // dumpCursor should not change status of cursor
        assertEquals(expected, sb.toString())
        assertEquals(expected, DatabaseUtils.dumpCursorToString(cursor))
        assertEquals(pos, cursor.getPosition()) // dumpCursor should not change status of cursor
        cursor.close()
    }

    @Test
    fun testLongForQuery() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var query = "SELECT age FROM " + TABLE_NAME
        assertEquals(20, DatabaseUtils.longForQuery(mDatabase, query, null))
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Jack', '35', 'London');"))
        query = "SELECT age FROM " + TABLE_NAME + " WHERE name = ?"
        var args = arrayOf<String>("Jack")
        assertEquals(35, DatabaseUtils.longForQuery(mDatabase, query, args))
        args = arrayOf<String>("No such name")
        try
        {
            DatabaseUtils.longForQuery(mDatabase, query, args)
            fail("should throw SQLiteException")
        }
        catch (e:SQLiteException) {
            // expected
        }
        query = "SELECT count(*) FROM " + TABLE_NAME + ";"
        var statement = mDatabase.compileStatement(query)
        assertEquals(2, DatabaseUtils.longForQuery(statement, null))
        query = "SELECT age FROM " + TABLE_NAME + " WHERE address = ?;"
        statement = mDatabase.compileStatement(query)
        args = arrayOf<String>("London")
        assertEquals(35, DatabaseUtils.longForQuery(statement, args))
        args = arrayOf<String>("No such address")
        try
        {
            DatabaseUtils.longForQuery(statement, args)
            fail("should throw SQLiteException")
        }
        catch (e:SQLiteException) {
            // expected
        }
        statement.close()
    }

    @Test
    fun testQueryNumEntries() {
        assertEquals(0, DatabaseUtils.queryNumEntries(mDatabase, TABLE_NAME))
        mDatabase.execSQL(
                ("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                        " VALUES ('Mike', '20', 'LA');"))
        assertEquals(1, DatabaseUtils.queryNumEntries(mDatabase, TABLE_NAME))
        mDatabase.execSQL(
                ("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                        " VALUES ('Susan', '20', 'AR');"))
        assertEquals(2, DatabaseUtils.queryNumEntries(mDatabase, TABLE_NAME))
        mDatabase.execSQL(
                ("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                        " VALUES ('Christian', '25', 'AT');"))
        assertEquals(3, DatabaseUtils.queryNumEntries(mDatabase, TABLE_NAME))
        assertEquals(2, DatabaseUtils.queryNumEntries(mDatabase, TABLE_NAME, "AGE = 20"))
        assertEquals(1, DatabaseUtils.queryNumEntries(mDatabase, TABLE_NAME, "AGE = ?",
                arrayOf<String>("25")))
        try
        {
            DatabaseUtils.queryNumEntries(mDatabase, "NoSuchTable")
            fail("should throw SQLiteException.")
        }
        catch (e:SQLiteException) {
            // expected
        }
    }

    @Test
    fun testStringForQuery() {
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Mike', '20', 'LA');"))
        var query = "SELECT name FROM " + TABLE_NAME
        assertEquals("Mike", DatabaseUtils.stringForQuery(mDatabase, query, null))
        mDatabase.execSQL(("INSERT INTO " + TABLE_NAME + " (name, age, address)" +
                " VALUES ('Jack', '35', 'London');"))
        query = "SELECT name FROM " + TABLE_NAME + " WHERE address = ?"
        var args = arrayOf<String>("London")
        assertEquals("Jack", DatabaseUtils.stringForQuery(mDatabase, query, args))
        args = arrayOf<String>("No such address")
        try
        {
            DatabaseUtils.stringForQuery(mDatabase, query, args)
            fail("should throw SQLiteException")
        }
        catch (e:SQLiteException) {
            // expected
        }
        query = "SELECT name FROM " + TABLE_NAME + " WHERE age = ?;"
        var statement = mDatabase.compileStatement(query)
        args = arrayOf<String>("20")
        assertEquals("Mike", DatabaseUtils.stringForQuery(statement, args))
        args = arrayOf<String>("1000") // NO people can be older than this.
        /*try
        {
            DatabaseUtils.blobFileDescriptorForQuery(statement, args)
            fail("should throw SQLiteException")
        }
        catch (e:SQLiteException) {
            // expected
        }*/
        statement.close()
    }

    companion object {
        private val TEST_PROJECTION = arrayOf<String>("_id", // 0
                "name", // 1
                "age", // 2
                "address" // 3
        )
        private val TABLE_NAME = "test"
    }
}