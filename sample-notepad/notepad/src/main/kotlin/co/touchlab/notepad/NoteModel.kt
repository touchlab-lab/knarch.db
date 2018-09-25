package co.touchlab.notepad

import co.touchlab.multiplatform.architecture.threads.ThreadLocalImpl
import co.touchlab.notepad.db.Note
import co.touchlab.notepad.db.NoteDbHelper
import co.touchlab.notepad.utils.backgroundTask
import co.touchlab.notepad.utils.currentTimeMillis

//Should only exist in main thread as per rules. Not a great architecture, but fun to play with
var noteModelUpdateLocal : ((notes:Array<Note>)->Unit)? = null

class NoteModel {
    companion object {
        val dbHelper = NoteDbHelper()
    }

    fun insertNote(title:String, description:String){
        backgroundTask(
                {
                    val now = currentTimeMillis()
                    dbHelper.insertNotes(
                            Array(1) {
                                Note(title, description, now, now, null)
                            })
                }
        ){
            runUpdate()
        }
    }

    fun deleteNotes(){
        backgroundTask({
            dbHelper.deleteNotes()
        }){
            runUpdate()
        }
    }


    fun runUpdate() {
        backgroundTask({
            dbHelper.getNotes()
        }) {
            if (noteModelUpdateLocal != null) {
                noteModelUpdateLocal!!(it)
            }
        }
    }

    fun initUpdate(proc:(notes:Array<Note>)->Unit){
        noteModelUpdateLocal = proc
    }

    fun clearUpdate(){
        noteModelUpdateLocal = null
    }
}