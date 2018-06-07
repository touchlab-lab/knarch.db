package co.touchlab.kite.threads
import platform.Foundation.*
import kotlinx.cinterop.COpaquePointer

internal class Atomic<T>(producer: (() -> T)?){
    val mutex = NSLock()
    private val dataId:Int = createDataStore()
    init {
        if(producer != null)
            putValue(producer)
    }

    internal fun putValue(producer: () -> T){
        putDataPointer(dataId, konan.worker.detachObjectGraph {producer.invoke() as Any})
    }

    var tempSpot :T? = null

    fun getTempAndClear():T{
        val t = tempSpot!!
        tempSpot = null
        return t
    }

    fun hasTemp():Boolean = tempSpot != null

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
        val ref = getDataPointer(dataId)
        println("runProc $ref")
        if(ref == null)
        {
            proc(null)
        } else {
            val data = konan.worker.attachObjectGraph<Any>(ref)
            proc(data as T)
        }
    }

    internal fun accessUpdate(proc:(T?) -> T?):Unit {
        mutex.lock()
        try {
            tempSpot = runProcUpdate(proc)
            if(hasTemp())
                putValue({getTempAndClear()})
        } finally {
            mutex.unlock()
        }
    }

    private fun runProcUpdate(proc:(T?) -> T?):T?{
        val ref = getDataPointer(dataId)
        println("runProcUpdate ${ref}")

        return if(ref == null)
        {
            proc(null)
        } else {
            val data = konan.worker.attachObjectGraph<Any>(ref)
            proc(data as T)
        }
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
        val ref = getDataPointer(dataId)
        println("runProcWith ${ref}")
        if(ref == null)
        {
            proc(null, dataArg)
        } else {
            val data = konan.worker.attachObjectGraph<Any>(ref)
            proc(data as T, dataArg)
        }
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

        val ref = getDataPointer(dataId)
        println("runProcForResult $ref")
        val result: R
        if(ref == null)
        {
            result = proc(null)
        } else {
            val data = konan.worker.attachObjectGraph<Any>(ref)
            result = proc(data as T)
        }
        return result
    }

    /**
     * Get value. This is "final" in the sense that you now own the value and it
     * will no longer be shared.
     */
    internal fun getValue():T?{
        mutex.lock()
        try {
            val ref = getDataPointer(dataId)
            return if(ref == null) {
                null
            } else {
                konan.worker.attachObjectGraph<Any>(ref) as T
            }
        } finally {
            mutex.unlock()
        }
    }

    internal fun clear(){
        getValue()
    }
}

@SymbolName("Atomic_createDataStore")
external internal fun createDataStore():Int

@SymbolName("Atomic_putDataPointer")
external internal fun putDataPointer(dataId:Int, pt:COpaquePointer?)

@SymbolName("Atomic_getDataPointer")
external internal fun getDataPointer(dataId:Int):COpaquePointer?

@SymbolName("Atomic_removeDataPointer")
external internal fun removeDataPointer(dataId:Int)
