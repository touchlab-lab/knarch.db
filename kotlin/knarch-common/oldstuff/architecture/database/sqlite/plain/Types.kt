package co.touchlab.kurgan.architecture.database.sqlite.plain

import co.touchlab.kurgan.architecture.database.Cursor

expect interface CrossProcessCursor:Cursor
expect abstract class AbstractCursor():CrossProcessCursor
expect abstract class AbstractWindowedCursor():AbstractCursor
expect class SQLiteCursor(driver: SQLiteCursorDriver, editTable: String, query:SQLiteQuery):AbstractWindowedCursor
