package co.touchlab.kurgan.architecture.database.sqlite

import co.touchlab.kurgan.architecture.database.support.SupportSQLiteOpenHelper

class SizzleSQLiteOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return SizzleSQLiteOpenHelper(configuration.context,
                configuration.name,
                configuration.callback)
    }
}