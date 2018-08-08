package co.touchlab.notepad

import co.touchlab.multiplatform.architecture.threads.ThreadLocalImpl
import co.touchlab.notepad.db.Note
import co.touchlab.notepad.db.NoteDbHelper
import co.touchlab.notepad.utils.backgroundTask
import co.touchlab.notepad.utils.currentTimeMillis

class NoteModel {
    companion object {
        val dbHelper = NoteDbHelper()
    }

    val updateLocal = ThreadLocalImpl<(notes:Array<Note>)->Unit>()

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
            println("It worked?")
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
            val proc = updateLocal.get()
            println("proc null? ${proc == null}")
            if (proc != null) {
                proc(it)
            }
        }
    }

    fun initUpdate(proc:(notes:Array<Note>)->Unit){
        updateLocal.set(proc)
    }

    fun clearUpdate(){
        updateLocal.remove()
    }
}