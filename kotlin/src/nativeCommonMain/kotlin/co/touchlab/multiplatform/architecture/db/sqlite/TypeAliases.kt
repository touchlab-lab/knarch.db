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

import co.touchlab.knarch.db.sqlite.SQLiteClosable
import co.touchlab.knarch.db.sqlite.SQLiteCursorDriver
import co.touchlab.knarch.db.sqlite.SQLiteOpenHelper
import co.touchlab.knarch.db.sqlite.SQLiteProgram
import co.touchlab.knarch.db.sqlite.SQLiteStatement
import co.touchlab.knarch.db.sqlite.SQLiteDatabase
import co.touchlab.knarch.db.sqlite.SQLiteQuery
import co.touchlab.knarch.db.sqlite.SQLiteTransactionListener

actual typealias SQLiteOpenHelper = SQLiteOpenHelper
actual typealias SQLiteStatement = SQLiteStatement
actual typealias SQLiteProgram = SQLiteProgram
actual typealias SQLiteClosable = SQLiteClosable
actual typealias SQLiteDatabase = SQLiteDatabase
actual typealias CursorFactory = SQLiteDatabase.CursorFactory
actual typealias SQLiteTransactionListener = SQLiteTransactionListener
actual typealias SQLiteCursorDriver = SQLiteCursorDriver
actual typealias SQLiteQuery = SQLiteQuery
