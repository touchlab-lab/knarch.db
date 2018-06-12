package co.touchlab.knarch.db.sqlite

import kotlin.test.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*

class SQLiteOpenHelperTest{
    private val systemContext = DefaultSystemContext()
    private fun getContext():SystemContext = systemContext

    private lateinit var mOpenHelper:MockOpenHelper
    private val mFactory = object : SQLiteDatabase.CursorFactory{
        override fun newCursor(db:SQLiteDatabase,
                               driver:SQLiteCursorDriver, editTable:String?,
                               query:SQLiteQuery):Cursor{
            return MockCursor(db, driver, editTable, query)
        }
    }

    @BeforeEach
    protected fun setUp() {
        SQLiteDatabase.deleteDatabase(getContext().getDatabasePath(TEST_DATABASE_NAME))
        mOpenHelper = MockOpenHelper(getContext(), TEST_DATABASE_NAME, mFactory, TEST_VERSION)
    }

    @AfterEach
    protected fun tearDown() {
        mOpenHelper.close()
    }

    @Test
    fun testConstructor() {
        MockOpenHelper(getContext(), TEST_DATABASE_NAME, mFactory, TEST_VERSION)
        // Test with illegal version number.
        try
        {
            MockOpenHelper(getContext(), TEST_DATABASE_NAME, mFactory, TEST_ILLEGAL_VERSION)
            fail("Constructor of SQLiteOpenHelp should throws a IllegalArgumentException here.")
        }
        catch (e:IllegalArgumentException) {}
        // Test with null factory
        MockOpenHelper(getContext(), TEST_DATABASE_NAME, null, TEST_VERSION)
    }

    @Test
    fun testGetDatabase() {
        assertFalse(mOpenHelper.hasCalledOnOpen())
        // Test getReadableDatabase.
        var database:SQLiteDatabase = mOpenHelper.getReadableDatabase()
        assertNotNull(database)
        assertTrue(database.isOpen())
        assertTrue(mOpenHelper.hasCalledOnOpen())
        // Database has been opened, so onOpen can not be invoked.
        mOpenHelper.resetStatus()
        assertFalse(mOpenHelper.hasCalledOnOpen())
        // Test getWritableDatabase.
        var database2 = mOpenHelper.getWritableDatabase()
        assertTrue(database == database2)
        assertTrue(database.isOpen())
        assertFalse(mOpenHelper.hasCalledOnOpen())
        mOpenHelper.close()
        assertFalse(database.isOpen())
        // After close(), onOpen() will be invoked by getWritableDatabase.
        mOpenHelper.resetStatus()
        assertFalse(mOpenHelper.hasCalledOnOpen())
        var database3 = mOpenHelper.getWritableDatabase()
        assertNotNull(database)
        assertFalse(database == database3)
        assertTrue(mOpenHelper.hasCalledOnOpen())
        assertTrue(database3.isOpen())
        mOpenHelper.close()
        assertFalse(database3.isOpen())
    }

    /*@Test
    fun testLookasideDefault() {
        assertNotNull(mOpenHelper.getWritableDatabase())
        // Lookaside is always disabled on low-RAM devices
        val expectDisabled = mContext.getSystemService(ActivityManager::class.java).isLowRamDevice()
        verifyLookasideStats(mOpenHelper.getDatabaseName(), expectDisabled)
    }*/

    /*@Test
    fun testLookasideDisabled() {
        mOpenHelper.setLookasideConfig(0, 0)
        assertNotNull(mOpenHelper.getWritableDatabase())
        verifyLookasideStats(mOpenHelper.getDatabaseName(), true)
    }

    @Test
    fun testLookasideCustom() {
        mOpenHelper.setLookasideConfig(10000, 10)
        assertNotNull(mOpenHelper.getWritableDatabase())
        // Lookaside is always disabled on low-RAM devices
        val expectDisabled = mContext.getSystemService(ActivityManager::class.java).isLowRamDevice()
        verifyLookasideStats(mOpenHelper.getDatabaseName(), expectDisabled)
    }*/

    /*@Test
    fun testSetLookasideConfigValidation() {
        try
        {
            mOpenHelper.setLookasideConfig(-1, 0)
            fail("Negative slot size should be rejected")
        }
        catch (expected:IllegalArgumentException) {}
        try
        {
            mOpenHelper.setLookasideConfig(0, -10)
            fail("Negative slot count should be rejected")
        }
        catch (expected:IllegalArgumentException) {}
        try
        {
            mOpenHelper.setLookasideConfig(1, 0)
            fail("Illegal config should be rejected")
        }
        catch (expected:IllegalArgumentException) {}
        try
        {
            mOpenHelper.setLookasideConfig(0, 1)
            fail("Illegal config should be rejected")
        }
        catch (expected:IllegalArgumentException) {}
    }*/

    /*@Test
    fun testCloseIdleConnection() {
        mOpenHelper.setIdleConnectionTimeout(1000)
        mOpenHelper.getReadableDatabase()
        // Wait a bit and check that connection is still open
        Thread.sleep(600)
        val output = getDbInfoOutput()
        assertTrue("Connection #0 should be open. Output: " + output,
                output.contains("Connection #0:"))
        // Now cause idle timeout and check that connection is closed
        // We wait up to 5 seconds, which is longer than required 1 s to accommodate for delays in
        // message processing when system is busy
        val connectionWasClosed = waitForConnectionToClose(10, 500)
        assertTrue("Connection #0 should be closed", connectionWasClosed)
    }

    @Test
    fun testSetIdleConnectionTimeoutValidation() {
        try
        {
            mOpenHelper.setIdleConnectionTimeout(-1)
            fail("Negative timeout should be rejected")
        }
        catch (expected:IllegalArgumentException) {}
    }*/

    /*@Test
    fun testCloseIdleConnectionDefaultDisabled() {
        // Make sure system default timeout is not changed
        assertEquals(30000, SQLiteGlobal.getIdleConnectionTimeout())
        mOpenHelper.getReadableDatabase()
        // Wait past the timeout and verify that connection is still open
        Log.w(TAG, "Waiting for 35 seconds...")
        Thread.sleep(35000)
        val output = getDbInfoOutput()
        assertTrue("Connection #0 should be open. Output: " + output,
                output.contains("Connection #0:"))
    }*/

    private inner class MockOpenHelper(context:SystemContext, name:String?, factory:SQLiteDatabase.CursorFactory?, version:Int)
        :SQLiteOpenHelper(context, name, factory, version) {
        private var mHasCalledOnOpen = false
        override fun onCreate(db:SQLiteDatabase) {}
        override fun onUpgrade(db:SQLiteDatabase, oldVersion:Int, newVersion:Int) {}
        override fun onOpen(db:SQLiteDatabase) {
            mHasCalledOnOpen = true
        }
        fun hasCalledOnOpen():Boolean {
            return mHasCalledOnOpen
        }
        fun resetStatus() {
            mHasCalledOnOpen = false
        }
    }
    private inner class MockCursor(db:SQLiteDatabase, driver:SQLiteCursorDriver, editTable:String?,
                                   query:SQLiteQuery):SQLiteCursor(db, driver, editTable, query)
    companion object {
        private val TAG = "SQLiteOpenHelperTest"
        private val TEST_DATABASE_NAME = "database_test.db"
        private val TEST_VERSION = 1
        private val TEST_ILLEGAL_VERSION = 0
        /*private fun verifyLookasideStats(dbName:String, expectDisabled:Boolean) {
            val dbStatFound = false
            val info = SQLiteDebug.getDatabaseInfo()
            for (dbStat in info.dbStats)
            {
                if (dbStat.dbName.endsWith(dbName))
                {
                    dbStatFound = true
                    Log.i(TAG, "Lookaside for " + dbStat.dbName + " " + dbStat.lookaside)
                    if (expectDisabled)
                    {
                        assertTrue("lookaside slots count should be zero", dbStat.lookaside === 0)
                    }
                    else
                    {
                        assertTrue("lookaside slots count should be greater than zero",
                                dbStat.lookaside > 0)
                    }
                }
            }
            assertTrue("No dbstat found for " + dbName, dbStatFound)
        }*/
    }
}