package co.touchlab.notepad.db

import co.touchlab.notepad.sqldelight.Note
import co.touchlab.notepad.sqldelight.NoteQueries
import co.touchlab.notepad.sqldelight.QueryWrapper
import com.squareup.sqldelight.multiplatform.create

import co.touchlab.notepad.utils.initContext

class NoteDbHelper {
    var noteUpdate: (()->Unit)? = null
    private val noteQueries:NoteQueries

    init {
        initContext()
        val wrapper = QueryWrapper(QueryWrapper.create("holla2"))

        noteQueries = wrapper.noteQueries
    }

    private fun insertNote(note: Note) {
        noteQueries.insertNote(note = note.note,
                title = note.title,
                created = note.created,
                modified = note.modified,
                hiblob = note.hiblob)
    }

    fun getNotes(): List<Note> = noteQueries.selectAll().executeAsList()

    fun insertNotes(note: Array<Note>) {
        noteQueries.transaction {
            for (s in note) {
                insertNote(s)
            }
        }
    }
}
