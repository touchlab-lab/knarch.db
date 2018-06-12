package co.touchlab.knarch.db.sqlite

abstract class SQLiteClosable{
    private var mReferenceCount = 1

    /**
     * Called when the last reference to the object was released by
     * a call to [.releaseReference] or [.close].
     */
    protected abstract fun onAllReferencesReleased()

    /**
     * Acquires a reference to the object.
     *
     * @throws IllegalStateException if the last reference to the object has already
     * been released.
     */
    fun acquireReference() {
        if (mReferenceCount <= 0) {
            throw IllegalStateException(
                    "attempt to re-open an already-closed object: " + this)
        }
        mReferenceCount++
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * @see .onAllReferencesReleased
     */
    fun releaseReference() {
        val refCountIsZero = --mReferenceCount == 0
        if (refCountIsZero) {
            onAllReferencesReleased()
        }
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * Calling this method is equivalent to calling [.releaseReference].
     *
     * @see .releaseReference
     * @see .onAllReferencesReleased
     */
    open fun close() {
        releaseReference()
    }
}