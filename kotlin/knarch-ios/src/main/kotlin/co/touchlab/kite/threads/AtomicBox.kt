package co.touchlab.kite.threads

import konan.worker.*
import kotlinx.cinterop.*

class AtomicBox<T>(producer: () -> T) {
    val target: T
    val mutexCacheIndex: Int
    init {
        mutexCacheIndex = findMutextIndex()
        println("Mutex Cache Index: $mutexCacheIndex")
        val theval = producer.invoke()
//        unsafeFreeze(theval as Any)
        target = theval
        unsafeFreeze(this)
    }

    fun access(
            op:(T) -> Any?
    ):Any?
    {
        openBox(mutexCacheIndex)

        val result: Any?
        try {
            result = localAccess(op)
        } finally {
            closeBox(mutexCacheIndex)
        }

        return result
    }

    /**
     * This lives here because the runtime will inc/dec 'target' in scope, and the scope needs to be
     * inside the protected space or it'll create memory issues.
     */
    private fun localAccess(op:(T) -> Any?):Any? = op.invoke(target)

    /**
     * The value in 'target' should be frozen, but we want to be able to change it (relatively) safely.
     */
    @SymbolName("Co_Touchlab_Kite_Threads_AtomicBox_openBox")
    external fun openBox(mutexIndex:Int)

    @SymbolName("Co_Touchlab_Kite_Threads_AtomicBox_closeBox")
    external fun closeBox(mutexIndex:Int)
}

/**
 * We're going to mark the target as "frozen" even though it and it's graph are not.
 * Obviously this is getting around all of the safeguards of 'freeze', but we're experimenting
 * with ways to allow safe access to mutable state between threads. This is assuming that
 * as memory is freed later, the process doesn't treat children of frozen objects much differently
 * and will still reclaim them, assuming their reference counts are correct.
 *
 * The plan is to do the same refernce checks that a Worker does on a producer when passing into the
 * box. That should presumably mean what we're doing is reasonably safe.
 */
@SymbolName("Co_Touchlab_Kite_Threads_AtomicBox_unsafeFreeze")
external private fun unsafeFreeze(target: Any)

@SymbolName("Co_Touchlab_Kite_Threads_AtomicBox_findMutextIndex")
external private fun findMutextIndex():Int

//@SymbolName("Co_Touchlab_Kite_Threads_throwJobOverWallInternal")
//external fun throwJobOverWallInternal(mode: Int, producer: () -> Any?, job: CPointer<CFunction<*>>)

