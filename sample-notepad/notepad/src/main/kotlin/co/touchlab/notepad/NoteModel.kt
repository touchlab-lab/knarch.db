package co.touchlab.notepad

import co.touchlab.multiplatform.architecture.threads.ThreadLocal
import co.touchlab.notepad.db.Note
import co.touchlab.notepad.db.NoteDbHelper
import co.touchlab.notepad.utils.backgroundTask
import co.touchlab.notepad.utils.currentTimeMillis

class NoteModel {
    companion object {
        val dbHelper = NoteDbHelper()
    }

    init {
        dbHelper.noteUpdate = {

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
    }

    val updateLocal = ThreadLocal<(notes:Array<Note>)->Unit>()

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


    fun runUpdate(){
        if (dbHelper.noteUpdate != null) {
            dbHelper.noteUpdate!!()
        }
    }

    fun initUpdate(proc:(notes:Array<Note>)->Unit){
        updateLocal.set(proc)
    }

    fun clearUpdate(){
        updateLocal.remove()
    }
}