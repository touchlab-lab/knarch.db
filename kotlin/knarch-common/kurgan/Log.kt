package co.touchlab.kurgan

class Log{
    companion object {
        val logImpl = LogImpl()
        fun e(tag:String, message:String) = logImpl.e(tag, message)
        fun w(tag:String, message:String) = logImpl.w(tag, message)
        fun i(tag:String, message:String) = logImpl.i(tag, message)
        fun d(tag:String, message:String) = logImpl.d(tag, message)
        fun v(tag:String, message:String) = logImpl.v(tag, message)
        fun e(tag:String, message:String, ex:Throwable) = logImpl.e(tag, message, ex)
        fun w(tag:String, message:String, ex:Throwable) = logImpl.w(tag, message, ex)
        fun i(tag:String, message:String, ex:Throwable) = logImpl.i(tag, message, ex)
        fun d(tag:String, message:String, ex:Throwable) = logImpl.d(tag, message, ex)
        fun v(tag:String, message:String, ex:Throwable) = logImpl.v(tag, message, ex)

    }
}

expect class LogImpl(){
    fun e(tag:String, message:String)
    fun w(tag:String, message:String)
    fun i(tag:String, message:String)
    fun d(tag:String, message:String)
    fun v(tag:String, message:String)
    fun e(tag:String, message:String, ex:Throwable)
    fun w(tag:String, message:String, ex:Throwable)
    fun i(tag:String, message:String, ex:Throwable)
    fun d(tag:String, message:String, ex:Throwable)
    fun v(tag:String, message:String, ex:Throwable)
}