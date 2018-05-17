package co.touchlab.kurgan.architecture.database.sqlite

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

