package co.touchlab.kurgan.architecture.database.sqlite

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import co.touchlab.kurgan.architecture.database.DatabaseErrorHandler

class PlatformSQLiteOpenHelper(
        val callback:PlatformSQLiteOpenHelperCallback,
        context: Context,
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