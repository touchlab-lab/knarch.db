package co.touchlab.knarch

import platform.Foundation.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.sqlite.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import kotlin.collections.List

class DefaultSystemContext:SystemContext{

    private fun getDirPath(folder:String):String{
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true);
        val documentsDirectory = paths[0] as String;

        val databaseDirectory = documentsDirectory + "/$folder"

        val fileManager = NSFileManager.defaultManager()

        if (!fileManager.fileExistsAtPath(databaseDirectory))
            fileManager.createDirectoryAtPath(databaseDirectory, true, null, null); //Create folder

        return databaseDirectory
    }

    private fun getDatabaseDirPath():String = getDirPath("databases")

    override fun getDir(folder:String, mode:Int):File {
        return File(getDirPath(folder))
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