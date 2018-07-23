package co.touchlab.notepad

import android.app.Application
import co.touchlab.notepad.utils.AppContext

class NotepadApplication():Application(){
    override fun onCreate() {
        super.onCreate()
        AppContext.app = this
    }
}