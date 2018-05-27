package co.touchlab.knarch

import platform.Foundation.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.sqlite.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import kotlin.collections.List

class DefaultSystemContext:SystemContext{

    private fun getDatabaseDirPath():String{
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true);
        val documentsDirectory = paths[0] as String;

        val databaseDirectory = documentsDirectory + "/databases"

        val fileManager = NSFileManager.defaultManager()

        if (!fileManager.fileExistsAtPath(databaseDirectory))
            fileManager.createDirectoryAtPath(databaseDirectory, true, null, null); //Create folder

        return databaseDirectory
    }

    override fun getDatabasePath(databaseName:String):File{
        return File(getDatabaseDirPath(), databaseName)
    }

    override fun openOrCreateDatabase(name:String,
                                      mode:Int,
                                      factory:SQLiteDatabase.CursorFactory?,
                                      errorHandler: DatabaseErrorHandler?):SQLiteDatabase
    {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).path, factory, errorHandler);
    }

    override fun deleteDatabase(dbName:String):Boolean{
        return SQLiteDatabase.deleteDatabase(getDatabasePath(dbName))
    }
}