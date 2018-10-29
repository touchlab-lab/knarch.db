/*
 * Copyright (C) 2010 The Android Open Source Project
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

package co.touchlab.knarch.db

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.sqlite.SQLiteDatabase
import co.touchlab.knarch.db.sqlite.SQLiteException

class DefaultDatabaseErrorHandler:DatabaseErrorHandler {
    /**
     * defines the default method to be invoked when database corruption is detected.
     * @param dbObj the {@link SQLiteDatabase} object representing the database on which corruption
     * is detected.
     */
    override fun onCorruption(dbObj:SQLiteDatabase) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.getPath())
        // is the corruption detected even before database could be 'opened'?
        deleteDatabaseFile(dbObj.getPath())
    }
    private fun deleteDatabaseFile(fileName:String) {
        if (fileName.equals(":memory:", ignoreCase = true) || fileName.trim({ it <= ' ' }).length == 0)
        {
            return
        }
        Log.e(TAG, "deleting the database file: $fileName")
        try
        {
            deleteDatabaseFile(fileName)
        }
        catch (e:Exception) {
            /* print warning and ignore exception */
            Log.w(TAG, "delete failed: " + e.message)
        }
    }
    companion object {
        private val TAG = "DefaultDatabaseErrorHandler"
    }
}