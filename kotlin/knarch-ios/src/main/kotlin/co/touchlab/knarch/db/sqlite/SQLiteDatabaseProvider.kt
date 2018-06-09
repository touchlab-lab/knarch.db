package co.touchlab.knarch.db.sqlite

/**
 * Sharing data between threads on KN can be very tricky. For sqlite, we want to share the same db connection, but
 * otherwise have some regular local state and replicate features of Android's sqlite stack. To facilitate that,
 * we share a frozen construct across threads, which then inflates local instances of SQLiteDatabase, but
 * injects them with shared state.
 */
interface SQLiteDatabaseProvider{
    fun getDatabase():SQLiteDatabase
}