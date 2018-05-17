package co.touchlab.kite.threads

import konan.worker.*
import konan.ref.*
import kotlinx.cinterop.*
import platform.Foundation.*
import konan.internal.ExportForCppRuntime

/*class DispatchQueue{
    fun <T1> post(mode: TransferMode, producer: () -> T1, @VolatileLambda job: (T1) -> Unit){
        val nativeJob = staticCFunction(job)
        nativeDispatch(mode.value, producer, nativeJob as CPointer<CFunction<*>>)
    }
}*/

fun isMainThread() = NSThread.isMainThread()

fun assertMainThread(){
    if(!isMainThread())
        throw IllegalStateException("Must be in main thread")
}

@SymbolName("Co_Touchlab_Kite_Threads_nativeDispatch")
external private fun <T1, T2> nativeDispatch(mode: Int, producer: () -> Any?, job: CPointer<CFunction<(T1) -> T2>>, callbackId: Int)

//typealias backgroundOp = staticCFunction
typealias CallbackLambda = (Any?) -> Unit


fun <T1, T2> dispatchSingle(producer: () -> T1, backgroundJob: CPointer<CFunction<(T1) -> T2>>, mainJob: (T2) -> Unit/*, mainError: (t:Throwable) -> Unit = {}*/){
    val state = dispatchQueueState!!
    val callbackId = state.addCallbackStrong(mainJob as CallbackLambda)
    val payload = DispatchPayload(backgroundJob, callbackId, producer)
    state.backgroundWorker.schedule(TransferMode.CHECKED, {payload.freeze()}){payload ->
        nativeDispatch(TransferMode.CHECKED.value, payload.producer, payload.backgroundJob, payload.callbackId)
    }
}

private data class DispatchPayload<T1, T2>(val backgroundJob: CPointer<CFunction<(T1) -> T2>>, val callbackId: Int, val producer: () -> T1)

private val dispatchQueueState = dispatchQueueStateFactory()

private fun dispatchQueueStateFactory(): DispatchQueueState? = if(isMainThread()){DispatchQueueState()}else{null}

@ExportForCppRuntime
fun DispatchLaunchpad(function: () -> Any?) = function()

@ExportForCppRuntime
fun ThrowDispatchInvalidState(): Unit =
        throw IllegalStateException("Illegal transfer state")

@ExportForCppRuntime
fun mainCallbackReturn(callbackId:Int){
    val result = pullResult(callbackId)
    val callbackLambda = dispatchQueueState!!.findCallback(callbackId)

    if(callbackLambda != null)
    {
        callbackLambda.invoke(result)
    }
}

@SymbolName("Co_Touchlab_Kite_Threads_pullResult")
external private fun pullResult(callbackId: Int):Any?

private class DispatchQueueState{
    val callbackMapStrong = HashMap<Int, CallbackLambda>()
    val callbackMapWeak = HashMap<Int, WeakReference<CallbackLambda>>()
    var callbackId :Int = 0
    var backgroundWorker = startWorker()

    fun findCallback(callbackId: Int):CallbackLambda?{
        if(callbackMapStrong.containsKey(callbackId))
            return callbackMapStrong.get(callbackId)
        else if(callbackMapWeak.containsKey(callbackId))
            return callbackMapWeak.get(callbackId)?.get()
        else
            return null
    }

    fun removeCallback(callbackId: Int){
        if(callbackMapStrong.containsKey(callbackId))
            callbackMapStrong.remove(callbackId)
        else if(callbackMapWeak.containsKey(callbackId))
            callbackMapWeak.remove(callbackId)
    }

    fun addCallbackWeak(c: CallbackLambda):Int{
        val id = nextId()
        callbackMapWeak.put(id, WeakReference(c))
        return id
    }

    fun addCallbackStrong(c: CallbackLambda):Int{
        val id = nextId()
        callbackMapStrong.put(id, c)
        return id
    }

    private fun nextId() = callbackId++
}

/*interface Emitter<T>{
    fun onComplete()
    fun onError(t: Throwable)
    fun onNext(value: T)
}

private class EmitterMessage<T>{
    val t:Throwable?
    val value:T?

    constructor(){
        //Complete
    }
    constructor(t:Throwable){
        this.t = t
    }
    constructor(value:T){
        this.value = value
    }
}*/

