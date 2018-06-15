package co.touchlab.knarch.db.sqlite

data class SQLiteDatabaseConfiguration(
        /**
         * The database path.
         */
        val path:String,
        /**
         * The flags used to open the database.
         */
        val openFlags:Int = 0,
        /**
         * The maximum size of the prepared statement cache for each database connection.
         * Must be non-negative.
         *
         * Default is 25.
         */
        val maxSqlCacheSize:Int = 25,
        /**
         * True if foreign key constraints are enabled.
         *
         * Default is false.
         */
        val foreignKeyConstraintsEnabled:Boolean = false,

        /**
         * The size in bytes of each lookaside slot
         *
         *
         * If negative, the default lookaside configuration will be used
         */
        val lookasideSlotSize:Int = -1,

        /**
         * The total number of lookaside memory slots per database connection
         *
         *
         * If negative, the default lookaside configuration will be used
         */
        val lookasideSlotCount:Int = -1
){

    companion object {
        /**
         * Special path used by in-memory databases.
         */
        val MEMORY_DB_PATH = ":memory:";

        fun stripPathForLogs(path:String):String {
            if (path.indexOf('@') == -1) {
                return path;
            }
            return path
//            return EMAIL_IN_DB_PATTERN.matcher(path).replaceAll("XX@YY");
        }
    }
    // The pattern we use to strip email addresses from database paths
    // when constructing a label to use in log messages.
//    private static final Pattern EMAIL_IN_DB_PATTERN =
//            Pattern.compile("[\\w\\.\\-]+@[\\w\\.\\-]+");

    override fun toString(): String {
        return "path: $path, lable: $label, openFlags: $openFlags, maxSqlCacheSize: $maxSqlCacheSize, foreignKeyConstraintsEnabled: $foreignKeyConstraintsEnabled"
    }

    /**
     * The label to use to describe the database when it appears in logs.
     * This is derived from the path but is stripped to remove PII.
     */
    val label:String
    get() = stripPathForLogs(path)


    /**
     * Returns true if the database is in-memory.
     * @return True if the database is in-memory.
     */
    fun isInMemoryDb() = path.equals(other = MEMORY_DB_PATH, ignoreCase = true)

    fun isLookasideConfigSet(): Boolean {
        return lookasideSlotCount >= 0 && lookasideSlotSize >= 0
    }

}