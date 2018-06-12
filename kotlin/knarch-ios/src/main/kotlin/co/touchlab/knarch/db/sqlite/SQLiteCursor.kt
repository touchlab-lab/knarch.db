package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.AbstractWindowedCursor
import co.touchlab.knarch.db.CursorWindow
import co.touchlab.knarch.db.DatabaseUtils

/**
 * Execute a query and provide access to its result set through a Cursor
 * interface. For a query such as: {@code SELECT name, birth, phone FROM
 * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
 * phone) would be in the projection argument and everything from
 * {@code FROM} onward would be in the params argument.
 *
 * @param editTable the name of the table used for this query
 * @param query the {@link SQLiteQuery} object associated with this cursor object.
 */
open class SQLiteCursor
(private val mDriver:SQLiteCursorDriver, editTable:String?, query:SQLiteQuery):AbstractWindowedCursor() {
    override fun getPosition(): Int = position

    override fun isFirst(): Boolean = isFirst

    override fun isLast(): Boolean = isLast

    override fun isBeforeFirst(): Boolean =isBeforeFirst

    override fun isAfterLast(): Boolean =isAfterLast

    override fun getColumnNames(): Array<String> = columnNames

    override fun getColumnCount(): Int = columnCount

    override fun getCount(): Int = count

    /** The name of the table to edit */
    private val mEditTable:String? = editTable

    /** The names of the columns in the rows */
    override val columnNames:Array<String>

    /** The query object for the cursor */
    private val mQuery:SQLiteQuery

    /** The number of rows in the cursor */
    private var mCount = NO_COUNT

    /** The number of rows that can fit in the cursor window, 0 if unknown */
    private var mCursorWindowCapacity:Int = 0

    /** A mapping of column names to column indices, to speed up lookups */
    private var mColumnNameMap:Map<String, Int>?

    init{
        mColumnNameMap = null
        mQuery = query
        columnNames = query.getColumnNames()
    }

    /**
     * Get the database that this cursor is associated with.
     * @return the SQLiteDatabase that this cursor is associated with.
     */
    val database:SQLiteDatabase
        get() {
            return mQuery.getDatabase()
        }
    override val count:Int
        get() {
            if (mCount == NO_COUNT)
            {
                fillWindow(0)
            }
            return mCount
        }
    /**
     * Execute a query and provide access to its result set through a Cursor
     * interface. For a query such as: {@code SELECT name, birth, phone FROM
     * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
     * phone) would be in the projection argument and everything from
     * {@code FROM} onward would be in the params argument.
     *
     * @param db a reference to a Database object that is already constructed
     * and opened. This param is not used any longer
     * @param editTable the name of the table used for this query
     * @param query the rest of the query terms
     * cursor is finalized
     * @deprecated use {@link #SQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)} instead
     */
    @Deprecated("use {@link #SQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)} instead")
    constructor(db:SQLiteDatabase, driver:SQLiteCursorDriver,
                editTable:String?, query:SQLiteQuery) : this(driver, editTable, query) {}


    override fun onMove(oldPosition:Int, newPosition:Int):Boolean {
        // Make sure the row at newPosition is present in the window
        if ((mWindow == null || newPosition < mWindow!!.startPosition ||
                        newPosition >= (mWindow!!.startPosition + mWindow!!.numRows)))
        {
            fillWindow(newPosition)
        }
        return true
    }
    private fun fillWindow(requiredPos:Int) {
        clearOrCreateWindow(database.getPath())
        try
        {
            if (mCount == NO_COUNT)
            {
                val startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos, 0)
                mCount = mQuery.fillWindow(mWindow!!, startPos, requiredPos, true)
                mCursorWindowCapacity = mWindow!!.numRows
                /*if (Log.isLoggable(TAG, Log.DEBUG)) {
         Log.d(TAG, "received count(*) from native_fill_window: " + mCount);
         }*/
            }
            else
            {
                val startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos,
                        mCursorWindowCapacity)
                mQuery.fillWindow(mWindow!!, startPos, requiredPos, false)
            }
        }
        catch (ex:RuntimeException) {
            // Close the cursor window if the query failed and therefore will
            // not produce any results. This helps to avoid accidentally leaking
            // the cursor window if the client does not correctly handle exceptions
            // and fails to close the cursor.
            closeWindow()
            throw ex
        }
    }

    override fun getColumnIndex(cn:String):Int {
        var columnName = cn
        // Create mColumnNameMap on demand
        if (mColumnNameMap == null)
        {
            val columns = columnNames
            val columnCount = columns.size
            val map = HashMap<String, Int>(columnCount)
            for (i in 0 until columnCount)
            {
                map.put(columns[i], i)
            }
            mColumnNameMap = map
        }
        // Hack according to bug 903852
        val periodIndex = columnName.lastIndexOf('.')
        if (periodIndex != -1)
        {
            val e = Exception()
            Log.e(TAG, "requesting column name with table name -- $columnName", e)
            columnName = columnName.substring(periodIndex + 1)
        }
        val i = mColumnNameMap!!.get(columnName)
        if (i != null)
        {
            return i.toInt()
        }
        else
        {
            return -1
        }
    }
    override fun deactivate() {
        super.deactivate()
        mDriver.cursorDeactivated()
    }
    override fun close() {
        super.close()
        synchronized (this) {
            mQuery.close()
            mDriver.cursorClosed()
        }
    }

    fun setWindow(window:CursorWindow) {
        super.window = window
        mCount = NO_COUNT
    }

    /**
     * Changes the selection arguments. The new values take effect after a call to requery().
     */
    fun setSelectionArguments(selectionArgs:Array<String>) {
        mDriver.setBindArguments(selectionArgs)
    }

    companion object {
        internal val TAG = "SQLiteCursor"
        internal val NO_COUNT = -1
    }
}