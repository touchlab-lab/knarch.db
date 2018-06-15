package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.CursorWindow

class SQLiteQuery internal constructor(db:SQLiteDatabase, query:String):SQLiteProgram(db, query, null) {

    /**
     * Reads rows into a buffer.
     *
     * @param window The window to fill into
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled.
     * @param countAllRows True to count all rows that the query would
     * return regardless of whether they fit in the window.
     * @return Number of rows that were enumerated. Might not be all rows
     * unless countAllRows is true.
     *
     * @throws SQLiteException if an error occurs.
     */
    internal fun fillWindow(window:CursorWindow, startPos:Int, requiredPos:Int, countAllRows:Boolean):Int {
        return withRef {
            window.acquireReference()
            try
            {
                getSession().executeForCursorWindow(getSql(), getBindArgs(),
                        window, startPos, requiredPos, countAllRows)
            }
            catch (ex:SQLiteDatabaseCorruptException) {
                onCorruption()
                throw ex
            }
            catch (ex:SQLiteException) {
                Log.e(TAG, "exception: " + ex.message + "; query: " + getSql())
                throw ex
            }
            finally
            {
                window.releaseReference()
            }
        }
    }
    override fun toString():String {
        return "SQLiteQuery: " + getSql()
    }
    companion object {
        private val TAG = "SQLiteQuery"
    }
}