package co.touchlab.knarch.db

abstract class AbstractWindowedCursor:AbstractCursor() {
    /**
     * The cursor window owned by this cursor.
     */
    protected var mWindow:CursorWindow?=null
    /**
     * Sets a new cursor window for the cursor to use.
     * <p>
     * The cursor takes ownership of the provided cursor window; the cursor window
     * will be closed when the cursor is closed or when the cursor adopts a new
     * cursor window.
     * </p><p>
     * If the cursor previously had a cursor window, then it is closed when the
     * new cursor window is assigned.
     * </p>
     *
     * @param window The new cursor window, typically a remote cursor window.
     */
    override var window:CursorWindow?
        get() {
            return mWindow
        }
        set(window) {
            if (window !== mWindow)
            {
                closeWindow()
                mWindow = window
            }
        }
    
    override fun getBlob(columnIndex:Int):ByteArray {
        checkPosition()
        return mWindow!!.getBlob(position, columnIndex)
    }
    override fun getString(columnIndex:Int):String {
        checkPosition()
        return mWindow!!.getString(position, columnIndex)
    }
    override fun getShort(columnIndex:Int):Short {
        checkPosition()
        return mWindow!!.getShort(position, columnIndex)
    }
    override fun getInt(columnIndex:Int):Int {
        checkPosition()
        return mWindow!!.getInt(position, columnIndex)
    }
    override fun getLong(columnIndex:Int):Long {
        checkPosition()
        return mWindow!!.getLong(position, columnIndex)
    }
    override fun getFloat(columnIndex:Int):Float {
        checkPosition()
        return mWindow!!.getFloat(position, columnIndex)
    }
    override fun getDouble(columnIndex:Int):Double {
        checkPosition()
        return mWindow!!.getDouble(position, columnIndex)
    }
    override fun isNull(columnIndex:Int):Boolean {
        checkPosition()
        return mWindow!!.getType(position, columnIndex) == Cursor.FIELD_TYPE_NULL
    }
/**
     * @deprecated Use {@link #getType}
     */
    fun isBlob(columnIndex:Int):Boolean = getType(columnIndex) == Cursor.FIELD_TYPE_BLOB

    override fun getType(columnIndex:Int):Int {
        checkPosition()
        return mWindow!!.getType(position, columnIndex)
    }
    override fun checkPosition() {
        super.checkPosition()
        if (mWindow == null)
        {
            throw StaleDataException(("Attempting to access a closed CursorWindow." + "Most probable cause: cursor is deactivated prior to calling this method."))
        }
    }
    /**
     * Returns true if the cursor has an associated cursor window.
     *
     * @return True if the cursor has an associated cursor window.
     */
    fun hasWindow():Boolean {
        return mWindow != null
    }
    /**
     * Closes the cursor window and sets {@link #mWindow} to null.
     * @hide
     */
    protected fun closeWindow() {
        if (mWindow != null)
        {
            mWindow!!.close()
            mWindow = null
        }
    }
    /**
     * If there is a window, clear it.
     * Otherwise, creates a new window.
     *
     * @param name The window name.
     * @hide
     */
    protected fun clearOrCreateWindow(name:String) {
        if (mWindow == null)
        {
            mWindow = CursorWindow(name)
        }
        else
        {
            mWindow!!.clear()
        }
    }
    /** @hide */
    override fun onDeactivateOrClose() {
        super.onDeactivateOrClose()
        closeWindow()
    }
}