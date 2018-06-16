package co.touchlab.notepad

import co.touchlab.multiplatform.architecture.db.sqlite.SQLiteDatabase
import co.touchlab.notepad.db.NoteDbHelper

val TEST_NOTE_COUNT = 100000

fun createTestNotes(db: SQLiteDatabase, count:Int) {
    val list = ArrayList<Pair<String,String>>(count)
    for(i in 0..count) {
        list.add(Pair("title $i", "note $i"))
    }

//    NoteDbHelper.insertNotes(db, list.toTypedArray())
}