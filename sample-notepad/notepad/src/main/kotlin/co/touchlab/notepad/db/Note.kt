package co.touchlab.notepad.db

data class Note(val title:String,
                val note:String,
                val created:Long,
                val modified:Long,
                val hiBlob:ByteArray?)