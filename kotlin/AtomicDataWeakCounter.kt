package co.touchlab.kite.threads

/**
 *   Theory of operations:
 *
 *  Weak references in Kotlin/Native are implemented in the following way. Whenever weak reference to an
 * object is created, we atomically modify type info pointer in the object to point into a metaobject.
 * This metaobject contains a strong reference to the counter object (instance of WeakReferenceCounter class).
 * Every other weak reference contains a strong reference to the counter object.
 *
 *         [weak1]  [weak2]
 *             \      /
 *             V     V
 *     .......[Counter] <----
 *     .                     |
 *     .                     |
 *      ->[Object] -> [Meta]-
 *
 *   References from weak reference objects to the counter and from the metaobject to the counter are strong,
 *  and from the counter to the object is nullably weak. So whenever an object dies, if it has a metaobject,
 *  it is traversed to find a counter object, and atomically nullify reference to the object. Afterward, all attempts
 *  to get the object would yield null.
 */

// Clear holding the counter object, which refers to the actual object.
internal class AtomicDataWeakCounter(var referred: COpaquePointer?) : AtomicDataWeakImpl() {
    // Spinlock, potentially taken when materializing or removing 'referred' object.
    var lock: Int = 0

    @SymbolName("Konan_WeakReferenceCounter_get")
    external override fun get(): Any?
}

@PublishedApi
internal abstract class AtomicDataWeakImpl {
    abstract fun get(): Any?
}

// Get a counter from non-null object.
@SymbolName("Konan_getWeakReferenceImpl")
external internal fun getAtomicDataWeakImpl(referent: Any): AtomicDataWeakImpl

// Create a counter object.
@ExportForCppRuntime
internal fun makeAtomicDataWeakCounter(referred: COpaquePointer) = AtomicDataWeakCounter(referred)

