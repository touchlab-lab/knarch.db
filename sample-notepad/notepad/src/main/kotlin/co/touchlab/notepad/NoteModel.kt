package co.touchlab.notepad

import co.touchlab.notepad.db.Note
import co.touchlab.notepad.db.NoteDbHelper
import co.touchlab.notepad.utils.backgroundTask
import co.touchlab.notepad.utils.currentTimeMillis

class NoteModel {
    companion object {
        val dbHelper = NoteDbHelper()

        init {
            dbHelper.noteUpdate = {
                backgroundTask({
                    dbHelper.getNotes()
                }, null){notes ->
                    println("Found ${notes.size} notes")
                    for (n in notes) {
                        println(n)
                    }
                }

            }
        }
    }

    fun insertTestNotes() {

        val notes = Array(10) { i ->
            val now = currentTimeMillis()
            Note(
                    "Title $i",
                    "Desc $i",
                    now,
                    now,
                    ByteArray(4) {
                        when (it) {
                            0 -> 1
                            1 -> 2
                            2 -> 3
                            3 -> 5
                            else -> 0
                        }
                    }
            )
        }

        backgroundTask(
                { dbHelper.insertNotes(it) }
                , notes) {
            println("Done inserting!!!")
        }
    }
}