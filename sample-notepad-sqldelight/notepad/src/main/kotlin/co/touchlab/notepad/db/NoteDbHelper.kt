package co.touchlab.notepad.db

import co.touchlab.notepad.sqldelight.Note
import co.touchlab.notepad.sqldelight.NoteQueries
import co.touchlab.notepad.sqldelight.QueryWrapper
import com.squareup.sqldelight.multiplatform.create

import co.touchlab.notepad.utils.initContext
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlDatabase

class NoteDbHelper {

    private val noteQueries: NoteQueries
    private val database:SqlDatabase
    init {
        val helperFactory = initContext()
        database = QueryWrapper.create("holla2", openHelperFactory = helperFactory)
        noteQueries = QueryWrapper(database).noteQueries
    }

    private fun makeQueries():NoteQueries {
        return noteQueries
    }
    private fun insertNote(note: Note) {
        noteQueries.insertNote(note = note.note,
                title = note.title,
                created = note.created,
                modified = note.modified,
                hiblob = note.hiblob)
    }

    fun getNotes(): Query<Note> = noteQueries.selectAll()

    fun insertNotes(note: Array<Note>) {
        noteQueries.transaction {
            for (s in note) {
                insertNote(s)
            }
        }
    }
}
