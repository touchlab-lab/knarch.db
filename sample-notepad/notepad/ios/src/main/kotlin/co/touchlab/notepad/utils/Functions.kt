package co.touchlab.notepad.utils

import kotlin.system.getTimeMillis
import platform.darwin.*
import konan.worker.*

actual fun currentTimeMillis():Long = getTimeMillis()

private var worker :Worker?=null

//Multiple worker contexts get a copy of global state. Not sure about threads created outside of K/N (probably not)
//Lazy create ensures we don't try to create multiple queues
private fun makeQueue():Worker{
    if(worker == null)
    {
        worker = startWorker()
    }
    return worker!!
}

/**
 * This is 100% absolutely *not* how you should architect background tasks in K/N, but
 * we don't really have a lot of good examples, so here's one that will at least work.
 *
 * Expect everything you pass in to be frozen, and if that's not possible, it'll all fail. Just FYI.
 */
actual fun <A, B> backgroundTask(backJob:(A)-> B, arg:A, mainJob:(B) -> Unit){

    val jobWrapper = JobWrapper(backJob, arg, mainJob).freeze()

    val worker = makeQueue()
    worker.schedule(TransferMode.CHECKED,
            { jobWrapper }){
        println("${it.arg}")
        val result  = detachObjectGraph { it.backJob(it.arg).freeze() as Any }
        dispatch_async(dispatch_get_main_queue()){
            val mainResult = attachObjectGraph<Any>(result) as B
            it.mainJob(mainResult)
        }
    }
}

data class JobWrapper<A, B>(val backJob:(A)-> B, val arg:A, val mainJob:(B) -> Unit)
