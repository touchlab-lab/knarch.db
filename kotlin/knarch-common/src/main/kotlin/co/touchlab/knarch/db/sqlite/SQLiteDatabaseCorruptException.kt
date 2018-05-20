package co.touchlab.knarch.db.sqlite

class SQLiteDatabaseCorruptException:SQLiteException {
    constructor() {}
    constructor(error:String) : super(error) {}
}