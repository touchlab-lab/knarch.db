package co.touchlab.notepad

import co.touchlab.notepad.db.NoteDbHelper
import co.touchlab.notepad.sqldelight.Note
import co.touchlab.notepad.utils.backgroundTask
import co.touchlab.notepad.utils.currentTimeMillis
import co.touchlab.multiplatform.architecture.threads.*
import co.touchlab.notepad.db.QueryLiveData
import com.squareup.sqldelight.Query

class NoteModel {

    companion object {
        val dbHelper = NoteDbHelper()
    }

    val liveData:ListLiveData

    init {
        val query = dbHelper.getNotes()
        liveData = ListLiveData(query)
        query.addListener(liveData)
    }

    fun shutDown(){
        dbHelper.getNotes().removeListener(liveData)
    }

    fun notesLiveData():MutableLiveData<List<Note>> = liveData

    fun insertNote(title: String, description: String) {
        backgroundTask {
            val now = currentTimeMillis()
            dbHelper.insertNotes(
                    Array(1) {
                        Note.Impl(
                                title = title,
                                note = description,
                                created = now,
                                modified = now,
                                hiblob = null,
                                id = Long.MIN_VALUE)
                    })
        }
    }

    class ListLiveData(q: Query<Note>) : QueryLiveData<Note, List<Note>>(q), Query.Listener {
        override fun extractData(q: Query<*>): List<Note>  = q.executeAsList() as List<Note>
    }
}