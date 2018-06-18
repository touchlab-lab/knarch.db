/*
 * Copyright (C) 2011 The Android Open Source Project
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

import kotlin.math.max

object SQLiteGlobal {
    private val TAG = "SQLiteGlobal"

    /**
     * Gets the default page size to use when creating a database.
     */
    val defaultPageSize: Int
        get() = 1024

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    val defaultJournalMode: String
        get() = "delete"

    /**
     * Gets the journal size limit in bytes.
     */
    val journalSizeLimit: Int
        get() = 10000

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    val defaultSyncMode: String
        get() = "normal"

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    val walSyncMode: String
        get() = "normal"

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    val walAutoCheckpoint: Int
        get() {
            val value = 1000

            return max(1, value)
        }

    /**
     * ¯\_(ツ)_/¯
     */
    @SymbolName("Android_Database_SQLiteGlobal_nativeReleaseMemory")
    private external fun nativeReleaseMemory(): Int

    /**
     * Attempts to release memory by pruning the SQLite page cache and other
     * internal data structures.
     *
     * @return The number of bytes that were freed.
     */
    fun releaseMemory(): Int {
        return nativeReleaseMemory()
    }
}