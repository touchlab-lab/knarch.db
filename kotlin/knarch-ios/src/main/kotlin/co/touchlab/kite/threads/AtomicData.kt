package co.touchlab.kite.threads

import kotlinx.cinterop.*
import konan.internal.ExportForCppRuntime

@ExportForCppRuntime
fun ThrowAtomicDataInvalidState(): Unit =
        throw IllegalStateException("Illegal transfer state")

class AtomicData<T>(producer: () -> T){
    val atomicDataReferenceId: Int
    init {
        atomicDataReferenceId = createNativeDataRecord(producer)
    }

    fun access(proc: (d:T)->T?){
        val d = openAccess<T>(atomicDataReferenceId)
        if(d != null) {
            val result = proc.invoke(d)
            closeAccess(atomicDataReferenceId, result ?: d)
        }
    }

    fun close(){
        removeDataStore(atomicDataReferenceId)
    }
}


@ExportForCppRuntime
fun AtomicDataLaunchpad(function: () -> Any?) = function()

@SymbolName("Co_Touchlab_Kite_Threads_AtomicData_init")
external private fun <T1> createNativeDataRecord(producer: () -> T1):Int

@SymbolName("Co_Touchlab_Kite_Threads_AtomicData_openAccess")
external private fun <T1> openAccess(id:Int):T1?

@SymbolName("Co_Touchlab_Kite_Threads_AtomicData_closeAccess")
external private fun <T1> closeAccess(id:Int, result:T1):Unit

@SymbolName("Co_Touchlab_Kite_Threads_AtomicData_removeDataStore")
external private fun removeDataStore(id:Int):Unit