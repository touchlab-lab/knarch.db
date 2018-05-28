package co.touchlab.knarch.db.sqlite

import kotlin.math.max

object SQLiteGlobal {
    private val TAG = "SQLiteGlobal"

    private val sDefaultPageSize: Int = 0

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
     * Gets the connection pool size when in WAL mode.
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