package co.touchlab.kite.threads
import platform.Foundation.*

class Atomic<T>(theData:T){
    val mutex = NSLock()
    init {
        putValue(theData)
    }
}

