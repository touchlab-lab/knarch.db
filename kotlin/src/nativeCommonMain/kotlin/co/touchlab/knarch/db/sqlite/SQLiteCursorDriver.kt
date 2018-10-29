/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * CHANGE NOTICE: File modified by Touchlab Inc to port to Kotlin and generally prepare for Kotlin/Native
 *
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