package co.touchlab.notepad.db

import co.touchlab.multiplatform.architecture.db.ContentValues
import co.touchlab.multiplatform.architecture.db.sqlite.*
import co.touchlab.notepad.db.NoteColumns.Companion.NOTES_TABLE_NAME
import co.touchlab.notepad.db.NoteColumns.Companion.TITLE
import co.touchlab.notepad.db.NoteColumns.Companion.NOTE
import co.touchlab.notepad.db.NoteColumns.Companion.HI_BLOB
import co.touchlab.notepad.utils.currentTimeMillis

class NoteDbHelper {
    companion object {
        fun initDatabase(): SQLiteOpenHelper {
            return createOpenHelper("holla", initCallback(), null)
        }

        fun initCallback(): PlatformSQLiteOpenHelperCallback
        {
            return object : PlatformSQLiteOpenHelperCallback(3){
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL(
                            "CREATE TABLE " + NOTES_TABLE_NAME + " ("
                                    + NoteColumns._ID + " INTEGER PRIMARY KEY,"
                                    + NoteColumns.TITLE + " TEXT,"
                                    + NoteColumns.NOTE + " TEXT,"
                                    + NoteColumns.HI_BLOB + " BLOB,"
                                    + NoteColumns.CREATED_DATE + " INTEGER,"
                                    + NoteColumns.MODIFIED_DATE + " INTEGER"
                                    + ");")
                    println("Create table success!!!!!")
                }

                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    db.execSQL("DROP TABLE IF EXISTS notes")
                    onCreate(db)
                }
            }
        }

        fun insertNotes(db: SQLiteDatabase, note:Array<Pair<String,String>>){
            db.beginTransaction()
            try {

                val now = currentTimeMillis()
                for (s in note) {
                    val cv = ContentValues()
                    cv.put(NoteColumns.TITLE, s.first)
                    cv.put(NoteColumns.NOTE, s.second)
                    cv.put(NoteColumns.CREATED_DATE, now)
                    cv.put(NoteColumns.MODIFIED_DATE, now)

                    val protoBlob = "Blobs!"
                    val protoBlobArray = ByteArray(protoBlob.length, {index -> protoBlob.get(index).toByte()})

                    cv.put(NoteColumns.HI_BLOB, protoBlobArray)
                    db.insert(NOTES_TABLE_NAME, values = cv)
                }
                db.setTransactionSuccessful()
            }finally {
                db.endTransaction()
            }
        }

        val TEST_NOTE_COUNT = 100000

        fun createTestNotes(db: SQLiteDatabase, count:Int) {
            val list = ArrayList<Pair<String,String>>(count)
            for(i in 0..count) {
                list.add(Pair("title $i", "note $i"))
            }

            insertNotes(db, list.toTypedArray())
        }

        fun queryData(db: SQLiteDatabase, count:Int) {
            val list = ArrayList<Pair<String,String>>(TEST_NOTE_COUNT)

            val cursor = db.rawQuery(
                    "select * from $NOTES_TABLE_NAME " +
                            "order by ${NoteColumns.CREATED_DATE} desc " +
                            "limit $count",
                    null
            )

            val titleColumn = cursor.getColumnIndex(TITLE)
            val noteColumn = cursor.getColumnIndex(NOTE)
//            val blobColumn = cursor.getColumnIndex(HI_BLOB)

            println("queryData titleColumn $titleColumn")
            println("queryData noteColumn $noteColumn")
            while (cursor.moveToNext()){
                list.add(Pair(cursor.getString(titleColumn), cursor.getString(noteColumn)))
//                val blobColString = utf8ToString(cursor.getBlob(blobColumn))
//                if(list.size < 30)
//                {
//                    println("The Blob: $blobColString")
//                }
            }

            println("Count Result ${list.size}")
            for (i in 0..20) {
                val pair = list[i]
                println("title: ${pair.first}/note: ${pair.second}")
            }
        }

    }
}