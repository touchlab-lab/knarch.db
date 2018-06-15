package co.touchlab.knarch.db.sqlite

object SQLiteDebug {
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
}