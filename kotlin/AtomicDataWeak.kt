package co.touchlab.kite.threads



/**
 * Class WeakReference encapsulates weak reference to an object, which could be used to either
 * retrieve a strong reference to an object, or return null, if object was already destoyed by
 * the memory manager.
 */
class AtomicDataWeak<T : Any> {
    /**
     * Creates a weak reference object pointing to an object. Weak reference doesn't prevent
     * removing object, and is nullified once object is collected.
     */
    constructor(referred: T) {
        pointer = getWeakReferenceImpl(referred)
    }

    /**
     * Backing store for the object pointer, inaccessible directly.
     */
    @PublishedApi
    internal var pointer: WeakReferenceImpl?

    /**
     * Clears reference to an object.
     */
    public fun clear() {
        pointer = null
    }
}

/**
 * Returns either reference to an object or null, if it was collected.
 */
public inline fun <reified T : Any> AtomicDataWeak<T>.get() = pointer?.get() as T?