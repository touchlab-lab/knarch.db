/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    internal fun <R> withRef(proc:() -> R):R{
        acquireReference()
        try
        {
            return proc()
        }
        finally
        {
            releaseReference()
        }
    }

}