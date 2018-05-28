package co.touchlab.knarch.db

interface CrossProcessCursor:Cursor {
    /**
     * Returns a pre-filled window that contains the data within this cursor.
     * <p>
     * In particular, the window contains the row indicated by {@link Cursor#getPosition}.
     * The window's contents are automatically scrolled whenever the current
     * row moved outside the range covered by the window.
     * </p>
     *
     * @return The pre-filled window, or null if none.
     */
    val window:CursorWindow?

    /**
     * Copies cursor data into the window.
     * <p>
     * Clears the window and fills it with data beginning at the requested
     * row position until all of the data in the cursor is exhausted
     * or the window runs out of space.
     * </p><p>
     * The filled window uses the same row indices as the original cursor.
     * For example, if you fill a window starting from row 5 from the cursor,
     * you can query the contents of row 5 from the window just by asking it
     * for row 5 because there is a direct correspondence between the row indices
     * used by the cursor and the window.
     * </p><p>
     * The current position of the cursor, as returned by {@link #getPosition},
     * is not changed by this method.
     * </p>
     *
     * @param position The zero-based index of the first row to copy into the window.
     * @param window The window to fill.
     */
    fun fillWindow(position:Int, window:CursorWindow)

    /**
     * This function is called every time the cursor is successfully scrolled
     * to a new position, giving the subclass a chance to update any state it
     * may have. If it returns false the move function will also do so and the
     * cursor will scroll to the beforeFirst position.
     * <p>
     * This function should be called by methods such as {@link #moveToPosition(int)},
     * so it will typically not be called from outside of the cursor class itself.
     * </p>
     *
     * @param oldPosition The position that we're moving from.
     * @param newPosition The position that we're moving to.
     * @return True if the move is successful, false otherwise.
     */
    fun onMove(oldPosition:Int, newPosition:Int):Boolean
}