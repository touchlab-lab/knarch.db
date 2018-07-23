package co.touchlab.notepad

import co.touchlab.notepad.sqldelight.Note
import co.touchlab.multiplatform.architecture.threads.*
import konan.worker.*

class NotesViewModel{

    val noteModel = NoteModel().freeze()
    var notesObserver:Observer<List<Note>>? = null

    fun registerForChanges(proc:(notes:List<Note>)->Unit){
        notesObserver = object : Observer<List<Note>>{
            override fun onChanged(t: List<Note>?){
                if(t != null)
                    proc(t)
            }
        }
        noteModel.notesLiveData().observeForever(notesObserver!!)
    }

    fun unregister(){
        noteModel.notesLiveData().removeObserver(notesObserver!!)
        notesObserver = null
    }

    fun insertNote(title:String, description:String){
        noteModel.insertNote(title, description)
    }
}