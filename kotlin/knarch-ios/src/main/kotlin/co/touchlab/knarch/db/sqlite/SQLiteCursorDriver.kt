package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.db.Cursor

interface SQLiteCursorDriver {
    /**
     * Executes the query returning a Cursor over the result set.
     *
     * @param factory The CursorFactory to use when creating the Cursors, or
     * null if standard SQLiteCursors should be returned.
     * @return a Cursor over the result set
     */
    fun query(factory:SQLiteDatabase.CursorFactory?, bindArgs:Array<String>?):Cursor
    /**
     * Called by a SQLiteCursor when it is released.
     */
    fun cursorDeactivated()
    /**
     * Called by a SQLiteCursor when it is requeried.
     */
    fun cursorRequeried(cursor:Cursor)
    /**
     * Called by a SQLiteCursor when it it closed to destroy this object as well.
     */
    fun cursorClosed()
    /**
     * Set new bind arguments. These will take effect in cursorRequeried().
     * @param bindArgs the new arguments
     */
    fun setBindArguments(bindArgs:Array<String>)
}