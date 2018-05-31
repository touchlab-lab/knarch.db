package co.touchlab.kite.threads
import platform.Foundation.*

class Atomic<T>(theData:T){
    val mutex = NSLock()
    init {
        putValue(theData)
    }
}

fun <T> Atomic<T>.access(proc:(T) -> Unit):Unit {
    mutex.lock()
    try {
        runProc(proc)
    } finally {
        mutex.unlock()
    }
}

fun <T, W> Atomic<T>.accessWith(producer:()->W, proc:(T, W) -> Unit):Unit {
    mutex.lock()
    try {
        val with = producer.invoke()
        runProcWith(proc, with)
    } finally {
        mutex.unlock()
    }
}

fun <T, R> Atomic<T>.accessForResult(proc:(T) -> R):R {
    mutex.lock()
    try {
        return runProcForResult(proc)
    } finally {
        mutex.unlock()
    }
}

