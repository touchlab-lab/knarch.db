package co.touchlab.kite.db.sqlite

class SQLiteDatabaseConfiguration{
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

    /**
     * The database path.
     */
    val path:String

    /**
     * The label to use to describe the database when it appears in logs.
     * This is derived from the path but is stripped to remove PII.
     */
    val label:String

    /**
     * The flags used to open the database.
     */
    var openFlags:Int = 0

    /**
     * The maximum size of the prepared statement cache for each database connection.
     * Must be non-negative.
     *
     * Default is 25.
     */
    var maxSqlCacheSize:Int = 25

    /**
     * True if foreign key constraints are enabled.
     *
     * Default is false.
     */
    var foreignKeyConstraintsEnabled = false

    /**
     * Creates a database configuration with the required parameters for opening a
     * database and default values for all other parameters.
     *
     * @param path The database path.
     * @param openFlags Open flags for the database, such as {@link SQLiteDatabase#OPEN_READWRITE}.
     */
    constructor(path:String, openFlags:Int) {
        this.path = path
        label = stripPathForLogs(path);
        this.openFlags = openFlags;

        // Set default values for optional parameters.
        maxSqlCacheSize = 25;
    }

    /**
     * Creates a database configuration as a copy of another configuration.
     *
     * @param other The other configuration.
     */
    constructor(other:SQLiteDatabaseConfiguration) {
        this.path = other.path;
        this.label = other.label;
        updateParametersFrom(other)
    }

    /**
     * Updates the non-immutable parameters of this configuration object
     * from the other configuration object.
     *
     * @param other The object from which to copy the parameters.
     */
    fun updateParametersFrom(other:SQLiteDatabaseConfiguration) {
        if (path != other.path) {
            throw IllegalArgumentException("other configuration must refer to "
                    + "the same database.");
        }

        openFlags = other.openFlags;
        maxSqlCacheSize = other.maxSqlCacheSize;
        foreignKeyConstraintsEnabled = other.foreignKeyConstraintsEnabled;
    }

    /**
     * Returns true if the database is in-memory.
     * @return True if the database is in-memory.
     */
    fun isInMemoryDb() = path.equals(other = MEMORY_DB_PATH, ignoreCase = true)


}