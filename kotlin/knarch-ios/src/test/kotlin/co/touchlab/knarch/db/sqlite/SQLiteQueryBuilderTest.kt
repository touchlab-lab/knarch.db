package co.touchlab.knarch.db.sqlite

import kotlin.test.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*

class SQLiteQueryBuilderTest {
    private lateinit var mDatabase:SQLiteDatabase
    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    private val TEST_TABLE_NAME = "test"
    private val EMPLOYEE_TABLE_NAME = "employee"

    @BeforeEach
    protected fun setUp() {
        getContext().deleteDatabase(DATABASE_FILE)
        mDatabase = getContext().openOrCreateDatabase(DATABASE_FILE, SystemContext.MODE_PRIVATE, null, null)
        assertNotNull(mDatabase)
    }

    @AfterEach
    protected fun tearDown() {
        mDatabase.close()
        getContext().deleteDatabase(DATABASE_FILE)
    }

    @Test
    fun testConstructor() {
        SQLiteQueryBuilder()
    }

    @Test
    fun testSetDistinct() {
        var sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(TEST_TABLE_NAME)
        sqliteQueryBuilder.setDistinct(false)
        sqliteQueryBuilder.appendWhere("age=20")
        var sql = sqliteQueryBuilder.buildQuery(arrayOf<String>("age", "address"), null, null, null, null, null)
        assertEquals(TEST_TABLE_NAME, sqliteQueryBuilder.getTables())
        var expected = "SELECT age, address FROM " + TEST_TABLE_NAME + " WHERE (age=20)"
        assertEquals(expected, sql)
        sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(EMPLOYEE_TABLE_NAME)
        sqliteQueryBuilder.setDistinct(true)
        sqliteQueryBuilder.appendWhere("age>32")
        sql = sqliteQueryBuilder.buildQuery(arrayOf<String>("age", "address"), null, null, null, null, null)
        assertEquals(EMPLOYEE_TABLE_NAME, sqliteQueryBuilder.getTables())
        expected = "SELECT DISTINCT age, address FROM " + EMPLOYEE_TABLE_NAME + " WHERE (age>32)"
        assertEquals(expected, sql)
        sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(EMPLOYEE_TABLE_NAME)
        sqliteQueryBuilder.setDistinct(true)
        sqliteQueryBuilder.appendWhereEscapeString("age>32")
        sql = sqliteQueryBuilder.buildQuery(arrayOf<String>("age", "address"), null, null, null, null, null)
        assertEquals(EMPLOYEE_TABLE_NAME, sqliteQueryBuilder.getTables())
        expected = ("SELECT DISTINCT age, address FROM " + EMPLOYEE_TABLE_NAME
                + " WHERE ('age>32')")
        assertEquals(expected, sql)
    }

    @Test
    fun testSetProjectionMap() {

        var projectMap = HashMap<String, String>()
        projectMap.put("EmployeeName", "name")
        projectMap.put("EmployeeAge", "age")
        projectMap.put("EmployeeAddress", "address")
        var sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(TEST_TABLE_NAME)
        sqliteQueryBuilder.setDistinct(false)
        sqliteQueryBuilder.setProjectionMap(projectMap)
        var sql = sqliteQueryBuilder.buildQuery(arrayOf<String>("EmployeeName", "EmployeeAge"), null, null, null, null, null)
        var expected = "SELECT name, age FROM " + TEST_TABLE_NAME
        assertEquals(expected, sql)
        sql = sqliteQueryBuilder.buildQuery(null, null, null, null, null, null)// projectionIn is null
        assertTrue(sql.matches((("SELECT (age|name|address), (age|name|address), (age|name|address) "
                + "FROM " + TEST_TABLE_NAME)).toRegex()))
        assertTrue(sql.contains("age"))
        assertTrue(sql.contains("name"))
        assertTrue(sql.contains("address"))
        sqliteQueryBuilder.setProjectionMap(null)
        sql = sqliteQueryBuilder.buildQuery(arrayOf<String>("name", "address"), null, null, null, null, null)
        assertTrue(sql.matches((("SELECT (name|address), (name|address) "
                + "FROM " + TEST_TABLE_NAME)).toRegex()))
        assertTrue(sql.contains("name"))
        assertTrue(sql.contains("address"))
    }

    @Test
    fun testSetCursorFactory() {
        mDatabase.execSQL(("CREATE TABLE test (_id INTEGER PRIMARY KEY, " + "name TEXT, age INTEGER, address TEXT);"))
        mDatabase.execSQL("INSERT INTO test (name, age, address) VALUES ('Mike', '20', 'LA');")
        mDatabase.execSQL("INSERT INTO test (name, age, address) VALUES ('jack', '40', 'LA');")
        var sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(TEST_TABLE_NAME)
        var cursor = sqliteQueryBuilder.query(mDatabase, arrayOf<String>("name", "age"), null, null, null, null, null)
        assertNotNull(cursor)
        assertTrue(cursor is SQLiteCursor)

        var factory = object:SQLiteDatabase.CursorFactory {
            override fun newCursor(db:SQLiteDatabase, masterQuery:SQLiteCursorDriver,
                          editTable:String?, query:SQLiteQuery):Cursor {
                return MockCursor(db, masterQuery, editTable, query)
            }
        }

        sqliteQueryBuilder.setCursorFactory(factory)
        cursor = sqliteQueryBuilder.query(mDatabase, arrayOf<String>("name", "age"), null, null, null, null, null)
        assertNotNull(cursor)
        assertTrue(cursor is MockCursor)
    }
    private class MockCursor(db:SQLiteDatabase, driver:SQLiteCursorDriver,
                             editTable:String?, query:SQLiteQuery):SQLiteCursor(driver, query)

    @Test
    fun testBuildQueryString() {

        val DEFAULT_TEST_PROJECTION = arrayOf<String>("name", "age", "sum(salary)")
        val DEFAULT_TEST_WHERE = "age > 25"
        val DEFAULT_HAVING = "sum(salary) > 3000"
        val sql = SQLiteQueryBuilder.buildQueryString(false, "Employee",
                DEFAULT_TEST_PROJECTION,
                DEFAULT_TEST_WHERE, "name", DEFAULT_HAVING, "name", "100")
        var expected = ("SELECT name, age, sum(salary) FROM Employee WHERE " + DEFAULT_TEST_WHERE +
                " GROUP BY name " +
                "HAVING " + DEFAULT_HAVING + " " +
                "ORDER BY name " +
                "LIMIT 100")
        assertEquals(expected, sql)
    }

    @Test
    fun testBuildQuery() {
        val DEFAULT_TEST_PROJECTION = arrayOf<String>("name", "sum(salary)")
        val DEFAULT_TEST_WHERE = "age > 25"
        val DEFAULT_HAVING = "sum(salary) > 2000"
        val sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(TEST_TABLE_NAME)
        sqliteQueryBuilder.setDistinct(false)
        val sql = sqliteQueryBuilder.buildQuery(DEFAULT_TEST_PROJECTION,
                DEFAULT_TEST_WHERE, "name", DEFAULT_HAVING, "name", "2")
        val expected = ("SELECT name, sum(salary) FROM " + TEST_TABLE_NAME
                + " WHERE (" + DEFAULT_TEST_WHERE + ") " +
                "GROUP BY name HAVING " + DEFAULT_HAVING + " ORDER BY name LIMIT 2")
        assertEquals(expected, sql)
    }

    @Test
    fun testAppendColumns() {
        val sb = StringBuilder()
        val columns = arrayOf<String>("name", "age")
        assertEquals("", sb.toString())
        SQLiteQueryBuilder.appendColumns(sb, columns)
        assertEquals("name, age ", sb.toString())
    }

    @Test
    fun testQuery() {
        createEmployeeTable()
        var sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables("Employee")
        var cursor:Cursor? = sqliteQueryBuilder.query(mDatabase,
                arrayOf<String>("name", "sum(salary)"), null, null,
                "name", "sum(salary)>1000", "name")
        assertNotNull(cursor!!)
        assertEquals(3, cursor!!.getCount())
        val COLUMN_NAME_INDEX = 0
        val COLUMN_SALARY_INDEX = 1
        cursor!!.moveToFirst()
        assertEquals("Jim", cursor.getString(COLUMN_NAME_INDEX))
        assertEquals(4500, cursor.getInt(COLUMN_SALARY_INDEX))
        cursor!!.moveToNext()
        assertEquals("Mike", cursor.getString(COLUMN_NAME_INDEX))
        assertEquals(4000, cursor.getInt(COLUMN_SALARY_INDEX))
        cursor!!.moveToNext()
        assertEquals("jack", cursor.getString(COLUMN_NAME_INDEX))
        assertEquals(3500, cursor.getInt(COLUMN_SALARY_INDEX))
        sqliteQueryBuilder = SQLiteQueryBuilder()
        sqliteQueryBuilder.setTables(EMPLOYEE_TABLE_NAME)
        cursor = sqliteQueryBuilder.query(mDatabase,
                arrayOf<String>("name", "sum(salary)"), null, null,
                "name", "sum(salary)>1000", "name", "2" // limit is 2
        )
        assertNotNull(cursor!!)
        assertEquals(2, cursor!!.getCount())
        cursor!!.moveToFirst()
        assertEquals("Jim", cursor.getString(COLUMN_NAME_INDEX))
        assertEquals(4500, cursor.getInt(COLUMN_SALARY_INDEX))
        cursor.moveToNext()
        assertEquals("Mike", cursor.getString(COLUMN_NAME_INDEX))
        assertEquals(4000, cursor.getInt(COLUMN_SALARY_INDEX))
    }

    @Test
    fun testUnionQuery() {
        val innerProjection = arrayOf<String>("name", "age", "location")
        var employeeQueryBuilder = SQLiteQueryBuilder()
        var peopleQueryBuilder = SQLiteQueryBuilder()
        employeeQueryBuilder.setTables("employee")
        peopleQueryBuilder.setTables("people")
        var employeeSubQuery = employeeQueryBuilder.buildUnionSubQuery(
                "_id",
                innerProjection,
                null,
                2,
                "employee",
                "age=25",
                null,
                null)
        var peopleSubQuery = peopleQueryBuilder.buildUnionSubQuery(
                "_id",
                innerProjection,
                null,
                2,
                "people",
                "location=LA",
                null,
                null)
        var expected = "SELECT name, age, location FROM employee WHERE (age=25)"
        assertEquals(expected, employeeSubQuery)
        expected = "SELECT name, age, location FROM people WHERE (location=LA)"
        assertEquals(expected, peopleSubQuery)
        val unionQueryBuilder = SQLiteQueryBuilder()
        unionQueryBuilder.setDistinct(true)
        val unionQuery = unionQueryBuilder.buildUnionQuery(
                arrayOf<String>(employeeSubQuery, peopleSubQuery), null, null)
        expected = ("SELECT name, age, location FROM employee WHERE (age=25) " + "UNION SELECT name, age, location FROM people WHERE (location=LA)")
        assertEquals(expected, unionQuery)
    }

    private fun createEmployeeTable() {
        mDatabase.execSQL(("CREATE TABLE employee (_id INTEGER PRIMARY KEY, " + "name TEXT, month INTEGER, salary INTEGER);"))
        mDatabase.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('Mike', '1', '1000');"))
        mDatabase.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('Mike', '2', '3000');"))
        mDatabase.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('jack', '1', '2000');"))
        mDatabase.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('jack', '3', '1500');"))
        mDatabase.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('Jim', '1', '1000');"))
        mDatabase.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('Jim', '3', '3500');"))
    }


    companion object {
        private val DATABASE_FILE = "database_test.db"
    }
}