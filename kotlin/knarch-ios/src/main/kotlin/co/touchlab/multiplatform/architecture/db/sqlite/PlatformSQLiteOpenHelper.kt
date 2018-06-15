package co.touchlab.multiplatform.architecture.db.sqlite

import co.touchlab.knarch.SystemContext
import co.touchlab.multiplatform.architecture.db.DatabaseErrorHandler
import co.touchlab.multiplatform.architecture.db.sqlite.PlatformSQLiteOpenHelperCallback

class PlatformSQLiteOpenHelper(
        val callback:PlatformSQLiteOpenHelperCallback,
        context: SystemContext,
        name:String?,
        version:Int,
        errorHandler: DatabaseErrorHandler?):SQLiteOpenHelper(context,name,null,version, errorHandler){

    override fun onCreate(db: SQLiteDatabase) {
        callback.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        callback.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        callback.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SQLiteDatabase) {
        callback.onOpen(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        callback.onConfigure(db)
    }
}