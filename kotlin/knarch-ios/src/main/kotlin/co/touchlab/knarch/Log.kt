package co.touchlab.knarch

class Log{
    companion object {
        fun e(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun w(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun i(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun d(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun isLoggable(s:String, level:Int):Boolean{
            return true
        }

        /**
         * Priority constant for the println method; use Log.d.
         */
        val DEBUG_ = 3

        /**
         * Priority constant for the println method; use Log.i.
         */
        val INFO = 4

        /**
         * Priority constant for the println method; use Log.w.
         */
        val WARN = 5

        /**
         * Priority constant for the println method; use Log.e.
         */
        val ERROR = 6

        /**
         * Priority constant for the println method.
         */
        val ASSERT = 7
    }
}