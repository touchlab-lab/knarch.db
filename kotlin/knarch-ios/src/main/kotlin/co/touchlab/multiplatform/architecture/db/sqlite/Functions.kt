package co.touchlab.multiplatform.architecture.db.sqlite

import co.touchlab.knarch.DefaultSystemContext
import co.touchlab.knarch.SystemContext
import co.touchlab.knarch.io.File
import co.touchlab.multiplatform.architecture.db.DatabaseErrorHandler


val systemContext = DefaultSystemContext()
fun getContext(): SystemContext = systemContext

actual fun createOpenHelper(
        name:String?,
        callback:PlatformSQLiteOpenHelperCallback,
        errorHandler: DatabaseErrorHandler?):SQLiteOpenHelper{

    return PlatformSQLiteOpenHelper(callback,
            getContext(),
            name,
            callback.version,
            errorHandler
            )
}

actual fun deleteDatabase(path:String):Boolean{
    return SQLiteDatabase.deleteDatabase(File(path))
}