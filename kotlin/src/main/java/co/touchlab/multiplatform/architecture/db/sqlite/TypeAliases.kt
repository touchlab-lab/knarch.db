/*
 * Copyright (c) 2018 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.multiplatform.architecture.db.sqlite

import android.database.sqlite.SQLiteClosable
import android.database.sqlite.SQLiteCursorDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteProgram
import android.database.sqlite.SQLiteQuery
import android.database.sqlite.SQLiteStatement
import android.database.sqlite.SQLiteTransactionListener

actual typealias SQLiteOpenHelper = SQLiteOpenHelper
actual typealias SQLiteStatement = SQLiteStatement
actual typealias SQLiteProgram = SQLiteProgram
actual typealias SQLiteClosable = SQLiteClosable
actual typealias SQLiteDatabase = SQLiteDatabase
actual typealias CursorFactory = SQLiteDatabase.CursorFactory
actual typealias SQLiteTransactionListener = SQLiteTransactionListener
actual typealias SQLiteCursorDriver = SQLiteCursorDriver
actual typealias SQLiteQuery = SQLiteQuery

