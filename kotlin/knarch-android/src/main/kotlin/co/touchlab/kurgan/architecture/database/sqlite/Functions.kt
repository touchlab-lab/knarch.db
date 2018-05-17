package co.touchlab.kurgan.architecture.database.sqlite

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import co.touchlab.kurgan.architecture.database.DatabaseErrorHandler
import java.io.File


var application:Application?=null

/**
 * Not pretty, but you know
 */
fun initApplicationDb(app: Application){
    application = app
}

actual fun createOpenHelper(
        name:String?,
        callback:PlatformSQLiteOpenHelperCallback,
        errorHandler: DatabaseErrorHandler?):SQLiteOpenHelper{

    if(application == null)
        throw IllegalArgumentException("Must call 'initApplicationDb' before creating a database")

    return PlatformSQLiteOpenHelper(callback,
            application!!,
            name,
            callback.version,
            errorHandler
            )
}

actual fun deleteDatabase(path:String):Boolean{
    return SQLiteDatabase.deleteDatabase(File(path))
}