package co.touchlab.kurgan.architecture

interface DataContext{
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences
    /**
     * This should absolutely be removed, but want to get demo functional
     */
    @Deprecated("Just for sqlite source compatibility. Remove after refactor.")
    fun nativeContext():Any
}