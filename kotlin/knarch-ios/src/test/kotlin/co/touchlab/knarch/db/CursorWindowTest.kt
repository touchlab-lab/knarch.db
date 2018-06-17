package co.touchlab.knarch.db

import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*
import kotlin.test.*
import kotlin.math.*
import platform.posix.*

class CursorWindowTest {
    /**
     * The method comes from unit_test CursorWindowTest.
     */
    private val oneByOneWindow:CursorWindow
        get() {
            val window = CursorWindow()
            assertTrue(window.setNumColumns(1))
            assertTrue(window.allocRow())
            return window
        }

    @Test
    fun testNull() {
        var window = oneByOneWindow
        // Put in a null value and read it back as various types
        assertTrue(window.putNull(0, 0))
        assertNull(window.getString(0, 0))
        assertEquals(0, window.getLong(0, 0))
        assertEquals(0.0, window.getDouble(0, 0))
        assertNull(window.getBlob(0, 0))
    }

    @Test
    fun testEmptyString() {
        var window = oneByOneWindow
        // put size 0 string and read it back as various types
        assertTrue(window.putString("", 0, 0))
        assertEquals("", window.getString(0, 0))
        assertEquals(0, window.getLong(0, 0))
        assertEquals(0.0, window.getDouble(0, 0))
    }

    @Test
    fun testDataStructureOperations() {
        var cursorWindow = CursorWindow()
        // Test with normal values
        assertTrue(cursorWindow.setNumColumns(0))
        // If the column has been set to zero, can't put String.
        assertFalse(cursorWindow.putString(TEST_STRING, 0, 0))
        // Test allocRow().
        assertTrue(cursorWindow.allocRow())
        assertEquals(1, cursorWindow.numRows)
        assertTrue(cursorWindow.allocRow())
        assertEquals(2, cursorWindow.numRows)
        // Though allocate a row, but the column number is still 0, so can't putString.
        assertFalse(cursorWindow.putString(TEST_STRING, 0, 0))
        // Test freeLstRow
        cursorWindow.freeLastRow()
        assertEquals(1, cursorWindow.numRows)
        cursorWindow.freeLastRow()
        assertEquals(0, cursorWindow.numRows)
        cursorWindow = CursorWindow()
        assertTrue(cursorWindow.setNumColumns(6))
        assertTrue(cursorWindow.allocRow())
        // Column number set to negative number, so now can put values.
        assertTrue(cursorWindow.putString(TEST_STRING, 0, 0))
        assertEquals(TEST_STRING, cursorWindow.getString(0, 0))
        // Test with negative value
        assertFalse(cursorWindow.setNumColumns(-1))
        // Test with reference limitation
        cursorWindow.releaseReference()
        try
        {
            cursorWindow.setNumColumns(5)
            fail("setNumColumns() should throws IllegalStateException here.")
        }
        catch (e:IllegalStateException) {
            // expected
        }
        // Test close(), close will also minus references, that will lead acquireReference()
        // related operation failed.
        cursorWindow.close()
        try
        {
            cursorWindow.acquireReference()
            fail("setNumColumns() should throws IllegalStateException here.")
        }
        catch (e:IllegalStateException) {
            // expected
        }
    }

    fun assertEquals(a:Float, b:Float, fuzz:Float){
        val diff = a.toDouble()-b.toDouble()
        assertTrue(fuzz.toDouble() >= diff.absoluteValue)
    }

    fun assertEquals(a:Double, b:Double, fuzz:Double){
        val diff = a-b
        assertTrue(fuzz >= diff.absoluteValue)
    }

    @Test
    fun testAccessDataValues() {
        var NUMBER_LONG_INTEGER = 0xaabbccddffL.toLong()
        var NUMBER_INTEGER = NUMBER_LONG_INTEGER.toInt()
        var NUMBER_SHORT = NUMBER_INTEGER.toShort()
        var NUMBER_FLOAT_SCIENCE = 7.332952E11f
        var NUMBER_DOUBLE_SCIENCE = 7.33295205887E11
        var NUMBER_FLOAT_SCIENCE_STRING = "7.332952E11"
        var NUMBER_DOUBLE_SCIENCE_STRING = "7.33295205887E11"
        var NUMBER_FLOAT_SCIENCE_STRING2 = "7.33295e+11"
        var originalBlob = ByteArray(Byte.MAX_VALUE.toInt())
        for (i in 0 until Byte.MAX_VALUE.toInt())
        {
            originalBlob[i] = i.toByte()
        }
        var cursorWindow = CursorWindow()
        cursorWindow.setNumColumns(5)
        cursorWindow.allocRow()
        // Test putString, getString, getLong, getInt, isBlob
        assertTrue(cursorWindow.putString(NUMBER_LONG_INTEGER.toString(), 0, 0))
        assertEquals(NUMBER_LONG_INTEGER.toString(), cursorWindow.getString(0, 0))
        assertEquals(NUMBER_LONG_INTEGER, cursorWindow.getLong(0, 0))
        assertEquals(NUMBER_INTEGER, cursorWindow.getInt(0, 0))
        assertEquals(NUMBER_SHORT, cursorWindow.getShort(0, 0))
        // Converting of Float, there would be some little precision differences. So just compare
        // first 6 digits.
        assertEquals(NUMBER_FLOAT_SCIENCE_STRING.substring(0, 6),
                cursorWindow.getFloat(0, 0).toString().substring(0, 6))
        assertEquals(NUMBER_DOUBLE_SCIENCE_STRING, cursorWindow.getDouble(0, 0).toString())
        assertFalse(cursorWindow.isNull(0, 0))
        assertFalse(cursorWindow.isBlob(0, 0))
        // Test null String
        assertTrue(cursorWindow.putString("", 0, 0))
        assertEquals("", cursorWindow.getString(0, 0))
        assertEquals(0, cursorWindow.getLong(0, 0))
        assertEquals(0, cursorWindow.getInt(0, 0))
        assertEquals(0, cursorWindow.getShort(0, 0))
        assertEquals(0.0, cursorWindow.getDouble(0, 0))
        assertEquals(0.0f, cursorWindow.getFloat(0, 0), 0.00000001f)
        assertFalse(cursorWindow.isNull(0, 0))
        assertFalse(cursorWindow.isBlob(0, 0))
        // Test putNull, getString, getLong, getDouble, getBlob, getInd, getShort, getFloat,
        // isBlob.
        assertTrue(cursorWindow.putNull(0, 1))
        assertNull(cursorWindow.getString(0, 1))
        assertEquals(0, cursorWindow.getLong(0, 1))
        assertEquals(0, cursorWindow.getInt(0, 1))
        assertEquals(0, cursorWindow.getShort(0, 1))
        assertEquals(0.0, cursorWindow.getDouble(0, 1))
        assertEquals(0.0f, cursorWindow.getFloat(0, 1), 0.00000001f)
        assertNull(cursorWindow.getBlob(0, 1))
        assertTrue(cursorWindow.isNull(0, 1))
        // If the field is null, isBlob will return true.
        assertTrue(cursorWindow.isBlob(0, 1))
        // Test putLong, getLong, getInt, getString , getShort, getFloat, getDouble, isBlob.
        assertTrue(cursorWindow.putLong(NUMBER_LONG_INTEGER, 0, 2))
        assertEquals(NUMBER_LONG_INTEGER, cursorWindow.getLong(0, 2))
        assertEquals(NUMBER_INTEGER, cursorWindow.getInt(0, 2))
        assertEquals(NUMBER_LONG_INTEGER.toString(), cursorWindow.getString(0, 2))
        assertEquals(NUMBER_SHORT, cursorWindow.getShort(0, 2))
        assertEquals(NUMBER_FLOAT_SCIENCE, cursorWindow.getFloat(0, 2), 0.00000001f)
        assertEquals(NUMBER_DOUBLE_SCIENCE, cursorWindow.getDouble(0, 2), 0.00000001)
        try
        {
            cursorWindow.getBlob(0, 2)
            fail("Can't get Blob from a Integer value.")
        }
        catch (e:SQLiteException) {
            // expected
        }
        assertFalse(cursorWindow.isNull(0, 2))
        assertFalse(cursorWindow.isBlob(0, 2))
        // Test putDouble
        assertTrue(cursorWindow.putDouble(NUMBER_DOUBLE_SCIENCE, 0, 3))
        assertEquals(NUMBER_LONG_INTEGER, cursorWindow.getLong(0, 3))
        assertEquals(NUMBER_INTEGER, cursorWindow.getInt(0, 3))
        // Converting from Double to String, there would be some little precision differences. So
        // Just compare first 6 digits.
        assertEquals(NUMBER_FLOAT_SCIENCE_STRING2.substring(0, 6), cursorWindow.getString(0, 3)
                .substring(0, 6))
        assertEquals(NUMBER_SHORT, cursorWindow.getShort(0, 3))
        assertEquals(NUMBER_FLOAT_SCIENCE, cursorWindow.getFloat(0, 3), 0.00000001f)
        assertEquals(NUMBER_DOUBLE_SCIENCE, cursorWindow.getDouble(0, 3), 0.00000001)
        try
        {
            cursorWindow.getBlob(0, 3)
            fail("Can't get Blob from a Double value.")
        }
        catch (e:SQLiteException) {
            // expected
        }
        assertFalse(cursorWindow.isNull(0, 3))
        assertFalse(cursorWindow.isBlob(0, 3))
        // Test putBlob
        assertTrue(cursorWindow.putBlob(originalBlob, 0, 4))
        var targetBlob = cursorWindow.getBlob(0, 4)
        assertTrue(originalBlob contentEquals targetBlob)
        assertFalse(cursorWindow.isNull(0, 4))
        // Test isBlob
        assertTrue(cursorWindow.isBlob(0, 4))
    }

    @Test
    fun testAccessStartPosition() {
        var TEST_POSITION_1 = 0
        var TEST_POSITION_2 = 3
        var cursorWindow = CursorWindow()
        fillCursorTestContents(cursorWindow, 5)
        // Test startPosition = 
        assertEquals(TEST_POSITION_1, cursorWindow.startPosition)
        assertEquals(3, cursorWindow.getInt(3, 0))
        assertEquals(TEST_STRING + "3", cursorWindow.getString(3, 1))
        assertEquals(4, cursorWindow.getInt(4, 0))
        assertEquals(TEST_STRING + "4", cursorWindow.getString(4, 1))
        cursorWindow.startPosition = (TEST_POSITION_2)
        assertEquals(TEST_POSITION_2, cursorWindow.startPosition)
        assertEquals(0, cursorWindow.getInt(3, 0))
        assertEquals(TEST_STRING + "0", cursorWindow.getString(3, 1))
        assertEquals(1, cursorWindow.getInt(4, 0))
        assertEquals(TEST_STRING + "1", cursorWindow.getString(4, 1))
        try
        {
            cursorWindow.getBlob(0, 0)
            fail("Row number is smaller than startPosition, will cause a IllegalStateException.")
        }
        catch (e:IllegalStateException) {
            // expected
        }
    }

    @Test
    fun testClearAndOnAllReferencesReleased() {
        var cursorWindow = MockCursorWindow(true)
        assertEquals(0, cursorWindow.numRows)
        fillCursorTestContents(cursorWindow, 10)
        assertEquals(10, cursorWindow.numRows)
        assertEquals(0, cursorWindow.startPosition)
        cursorWindow.startPosition = (5)
        assertEquals(5, cursorWindow.startPosition)
        // Test clear(). a complete calling process of cursorWindow has a perfect acquiring and
        // releasing pair, so the references number will be equal at the begin and the end.
        assertFalse(cursorWindow.hasReleasedAllReferences())
        cursorWindow.clear()
        assertEquals(0, cursorWindow.numRows)
        assertEquals(0, cursorWindow.startPosition)
        assertFalse(cursorWindow.hasReleasedAllReferences())
        // Test onAllReferencesReleased.
        // By default, cursorWindow's reference is 1, when it reachs 0, onAllReferencesReleased
        // be invoked.
        cursorWindow = MockCursorWindow(true)
        cursorWindow.releaseReference()
        assertTrue(cursorWindow.hasReleasedAllReferences())
    }

    private inner class MockCursorWindow(localWindow:Boolean):CursorWindow() {
        private var mHasReleasedAllReferences = false
        protected override fun onAllReferencesReleased() {
            super.onAllReferencesReleased()
            mHasReleasedAllReferences = true
        }
        fun hasReleasedAllReferences():Boolean {
            return mHasReleasedAllReferences
        }
        fun resetStatus() {
            mHasReleasedAllReferences = false
        }
    }

    private fun fillCursorTestContents(cursorWindow:CursorWindow, length:Int) {
        cursorWindow.clear()
        cursorWindow.startPosition = (0)
        cursorWindow.setNumColumns(2)
        for (i in 0 until length)
        {
            cursorWindow.allocRow()
            cursorWindow.putLong(i.toLong(), i, 0)
            cursorWindow.putString(TEST_STRING + i, i, 1)
        }
    }

    companion object {
        private val TEST_STRING = "Test String"
        private fun createTestList(rows:Int, cols:Int):ArrayList<ArrayList<Int>> {
            val list = ArrayList<ArrayList<Int>>()

            for (i in 0 until rows)
            {
                val col = ArrayList<Int>()
                list.add(col)
                for (j in 0 until cols)
                {
                    // generate random number
                    col.add(if (j == 0) i else arc4random_uniform(400000000))
                }
            }
            return list
        }
    }
}