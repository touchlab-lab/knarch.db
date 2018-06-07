package co.touchlab.kite.threads
import platform.Foundation.*
import kotlinx.cinterop.COpaquePointer

var atCount = 0
val atTempMap = HashMap<Int, Any?>()

internal class Atomic<T>(producer: (() -> T)?){
    val mutex = NSLock()
    val atIndex = atCount++

    init {
        if(producer != null)
            putValue(producer)
    }

    internal fun putValue(producer: () -> T){
        putValueCounter(producer.invoke())
//        putDataPointer(dataId, konan.worker.detachObjectGraph {producer.invoke() as Any})
    }

//    var tempSpot :T? = null

    fun getTempAndClear():T{
        val t = atTempMap.get(atIndex)!! as T
        atTempMap.remove(atIndex)
        return t
    }

    fun loadTempFromCounter(){
        atTempMap.put(atIndex, getValueCounter())
    }

    fun hasTemp():Boolean = atTempMap.containsKey(atIndex)/* != null*/

    internal fun access(proc:(T?) -> Unit):Unit {

        mutex.lock()
        try {
            runProc(proc)
            if(hasTemp())
                putValue({getTempAndClear()})
        } finally {
            mutex.unlock()
        }
    }

    private fun runProc(proc:(T?) -> Unit){
        loadTempFromCounter()
        proc(atTempMap.get(atIndex) as T?)
    }

    internal fun accessUpdate(proc:(T?) -> T?):Unit {
        mutex.lock()
        try {
            runProcUpdate(proc)
            if(hasTemp())
                putValue({getTempAndClear()})
        } finally {
            mutex.unlock()
        }
    }

    private fun runProcUpdate(proc:(T?) -> T?):Unit{
        loadTempFromCounter()
        atTempMap.put(atIndex, proc(atTempMap.get(atIndex) as T?))
    }

    internal fun <W> accessWith(producer:()->W, proc:(T?, W) -> Unit):Unit {
        mutex.lock()
        try {
            val with = producer.invoke()
            runProcWith(proc, with)
            if(hasTemp())
                putValue({getTempAndClear()})
        } finally {
            mutex.unlock()
        }
    }

    private fun <W> runProcWith(proc:(T?, W) -> Unit, dataArg: W){
        loadTempFromCounter()
        proc(atTempMap.get(atIndex) as T?, dataArg)
    }

    internal fun <R> accessForResult(proc:(T?) -> R):R {
        mutex.lock()
        try {
            val result = runProcForResult(proc)
            if(hasTemp())
                putValue({getTempAndClear()})

            return result
        } finally {
            mutex.unlock()
        }
    }

    private fun <R> runProcForResult(proc:(T?) -> R):R{
        loadTempFromCounter()
        return proc(atTempMap.get(atIndex) as T?)
    }
}
