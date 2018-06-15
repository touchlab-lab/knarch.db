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
