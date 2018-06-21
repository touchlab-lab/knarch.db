package co.touchlab.notepad

import co.touchlab.multiplatform.architecture.threads.ThreadLocal
import co.touchlab.notepad.db.NoteDbHelper
import co.touchlab.notepad.sqldelight.Note
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

    val updateLocal = ThreadLocal<(notes:List<co.touchlab.notepad.sqldelight.Note>)->Unit>()

    fun insertNote(title:String, description:String){
        backgroundTask(
                {
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
        ){
            println("It worked?")
            runUpdate()
        }
    }

    fun runUpdate(){
        if (dbHelper.noteUpdate != null) {
            dbHelper.noteUpdate!!()
        }
    }

    fun initUpdate(proc:(notes:List<co.touchlab.notepad.sqldelight.Note>)->Unit){
        updateLocal.set(proc)
    }

    fun clearUpdate(){
        updateLocal.remove()
    }
}