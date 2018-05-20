package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.Log

//TODO: Need to figure out a way to set these, or remove them
object SQLiteDebug {
    /**
     * Controls the printing of informational SQL log messages.
     *
     * Enable using "adb shell setprop log.tag.SQLiteLog VERBOSE".
     */
    val DEBUG_SQL_LOG = true//Log.isLoggable("SQLiteLog", Log.VERBOSE)
    /**
     * Controls the printing of SQL statements as they are executed.
     *
     * Enable using "adb shell setprop log.tag.SQLiteStatements VERBOSE".
     */
    val DEBUG_SQL_STATEMENTS = true//Log.isLoggable("SQLiteStatements", Log.VERBOSE)
    /**
     * Controls the printing of wall-clock time taken to execute SQL statements
     * as they are executed.
     *
     * Enable using "adb shell setprop log.tag.SQLiteTime VERBOSE".
     */
    val DEBUG_SQL_TIME = true//Log.isLoggable("SQLiteTime", Log.VERBOSE)
    /**
     * True to enable database performance testing instrumentation.
     * @hide
     */
    val DEBUG_LOG_SLOW_QUERIES = false//Build.IS_DEBUGGABLE;
    /**
     * Determines whether a query should be logged.
     *
     * Reads the "db.log.slow_query_threshold" system property, which can be changed
     * by the user at any time. If the value is zero, then all queries will
     * be considered slow. If the value does not exist or is negative, then no queries will
     * be considered slow.
     *
     * This value can be changed dynamically while the system is running.
     * For example, "adb shell setprop db.log.slow_query_threshold 200" will
     * log all queries that take 200ms or longer to run.
     * @hide
     */
    fun shouldLogSlowQuery(elapsedTimeMillis:Long):Boolean {
        return false
    }
    /**
     * contains statistics about a database
     */
    class DbStats(dbName:String, pageCount:Long, pageSize:Long, lookaside:Int,
                  hits:Int, misses:Int, cachesize:Int) {
        /** name of the database */
        var dbName:String
        /** the page size for the database */
        var pageSize:Long = 0
        /** the database size */
        var dbSize:Long = 0
        /** documented here http://www.sqlite.org/c3ref/c_dbstatus_lookaside_used.html */
        var lookaside:Int = 0
        /** statement cache stats: hits/misses/cachesize */
        var cache:String
        init{
            this.dbName = dbName
            this.pageSize = pageSize / 1024
            dbSize = (pageCount * pageSize) / 1024
            this.lookaside = lookaside
            this.cache = "$hits/$misses/$cachesize"
        }
    }
    /**
     * Dumps detailed information about all databases used by the process.
     * @param printer The printer for dumping database state.
     * @param args Command-line arguments supplied to dumpsys dbinfo
     */
    /*fun dump(printer:Printer, args:Array<String>) {
        val verbose = false
        for (arg in args)
        {
            if (arg == "-v")
            {
                verbose = true
            }
        }
        SQLiteDatabase.dumpAll(printer, verbose)
    }*/
}