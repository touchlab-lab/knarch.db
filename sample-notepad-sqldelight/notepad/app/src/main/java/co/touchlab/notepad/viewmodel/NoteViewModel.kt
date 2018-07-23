package co.touchlab.notepad.viewmodel

import android.arch.lifecycle.ViewModel
import co.touchlab.notepad.NoteModel

class NoteViewModel : ViewModel(){
    val noteModel = NoteModel()
}