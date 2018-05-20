package co.touchlab.kite.threads

import kotlinx.cinterop.COpaquePointer
import konan.internal.ExportForCppRuntime

// Object that holds the value we're sharing. The first two params are only there to fill out the space
// that Weak.cpp assumes will be there. This will likely change if the KN team decides to clean up
// their implementation.
internal class AtomicCounter(var referred: COpaquePointer? = null, var lock: Int = 0, var theData:Any? = null) {

}


fun <T> Atomic<T>.putValue(target:T){
    atomicGetCounter(this).theData = target
}

@SymbolName("Atomic_atomicGetCounter")
external internal fun atomicGetCounter(referent: Any):AtomicCounter

// Create a counter object.
@ExportForCppRuntime
internal fun makeAtomicCounter(target: Any?) = AtomicCounter(theData = target)
