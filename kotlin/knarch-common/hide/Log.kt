package co.touchlab.kite

class Log{
    companion object {
        fun e(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
    }
}