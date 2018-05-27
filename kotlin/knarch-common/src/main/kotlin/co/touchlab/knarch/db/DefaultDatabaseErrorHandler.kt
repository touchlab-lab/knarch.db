package co.touchlab.knarch.db

import co.touchlab.knarch.Log
import co.touchlab.knarch.db.sqlite.SQLiteDatabase
import co.touchlab.knarch.db.sqlite.SQLiteException

class DefaultDatabaseErrorHandler:DatabaseErrorHandler {
    /**
     * defines the default method to be invoked when database corruption is detected.
     * @param dbObj the {@link SQLiteDatabase} object representing the database on which corruption
     * is detected.
     */
    override fun onCorruption(dbObj:SQLiteDatabase) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.getPath())
        // is the corruption detected even before database could be 'opened'?
        deleteDatabaseFile(dbObj.getPath())
    }
    private fun deleteDatabaseFile(fileName:String) {
        if (fileName.equals(":memory:", ignoreCase = true) || fileName.trim({ it <= ' ' }).length == 0)
        {
            return
        }
        Log.e(TAG, "deleting the database file: $fileName")
        try
        {
            deleteDatabaseFile(fileName)
        }
        catch (e:Exception) {
            /* print warning and ignore exception */
            Log.w(TAG, "delete failed: " + e.message)
        }
    }
    companion object {
        private val TAG = "DefaultDatabaseErrorHandler"
    }
}