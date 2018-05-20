package co.touchlab.kurgan.architecture

expect class ThreadLocal<T>(){
    fun get():T?
    open fun initialValue():T?
    fun remove()
    fun set(value:T?)
}