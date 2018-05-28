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