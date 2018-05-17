package co.touchlab.kurgan.architecture.database.sqlite

import co.touchlab.kurgan.architecture.Context
import co.touchlab.kurgan.architecture.database.DatabaseErrorHandler
import co.touchlab.kurgan.architecture.database.sqlite.plain.SQLiteDatabase
import co.touchlab.kurgan.architecture.database.sqlite.plain.SQLiteOpenHelper
import co.touchlab.kurgan.architecture.database.support.SupportSQLiteDatabase
import co.touchlab.kurgan.architecture.database.support.SupportSQLiteOpenHelper

class SizzleSQLiteOpenHelper(context: Context, name: String?, callback: SupportSQLiteOpenHelper.Callback) : SupportSQLiteOpenHelper {
    private var mDelegate: OpenHelper

    private fun createDelegate(context: Context, name: String?, callback: SupportSQLiteOpenHelper.Callback): OpenHelper {
        val dbRef = arrayOfNulls<SizzleSQLiteDatabase>(1)
        return OpenHelper(context, name, dbRef, callback)
    }

    override fun getDatabaseName(): String {
        return mDelegate.getDatabaseName()
    }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        mDelegate.setWriteAheadLoggingEnabled(enabled)
    }

    override fun getWritableDatabase(): SupportSQLiteDatabase {
        return mDelegate.writableSupportDatabase
    }

    override fun getReadableDatabase(): SupportSQLiteDatabase {
        return mDelegate.readableSupportDatabase
    }

    override fun close() {
        mDelegate.close()
    }

    internal class OpenHelper(context: Context, name: String?,
                              val mDbRef: Array<SizzleSQLiteDatabase?>,
                              val mCallback: SupportSQLiteOpenHelper.Callback) :
            SQLiteOpenHelper(context, name, null, mCallback.version, object : DatabaseErrorHandler
            {
                override fun onCorruption(dbObj: SQLiteDatabase) {
                    val db = mDbRef[0]
                    if (db != null) {
                        mCallback.onCorruption(db)
                    }
                }
            })
    {

        val writableSupportDatabase: SupportSQLiteDatabase
            get() {
                val db = super.getWritableDatabase()
                return getWrappedDb(db)
            }

        val readableSupportDatabase: SupportSQLiteDatabase
            get() {
                val db = super.getReadableDatabase()
                return getWrappedDb(db)
            }

        fun getWrappedDb(sqLiteDatabase: SQLiteDatabase?): SizzleSQLiteDatabase {
            var dbRef: SizzleSQLiteDatabase? = mDbRef[0]
            if (dbRef == null) {
                dbRef = SizzleSQLiteDatabase(sqLiteDatabase!!)
                mDbRef[0] = dbRef
            }
            return mDbRef[0]!!
        }

        override fun onCreate(db: SQLiteDatabase) {
            mCallback.onCreate(getWrappedDb(db))
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            mCallback.onUpgrade(getWrappedDb(db), oldVersion, newVersion)
        }

        override fun onConfigure(db: SQLiteDatabase) {
            mCallback.onConfigure(getWrappedDb(db))
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            mCallback.onDowngrade(getWrappedDb(db), oldVersion, newVersion)
        }

        override fun onOpen(db: SQLiteDatabase) {
            mCallback.onOpen(getWrappedDb(db))
        }

//        @Synchronized
        override fun close() {
            super.close()
            mDbRef[0] = null
        }
    }

    init {
        mDelegate = createDelegate(context, name, callback)
    }
}