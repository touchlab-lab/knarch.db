package co.touchlab.kurgan.play

import konan.worker.*
import platform.darwin.*
import co.touchlab.kite.threads.*
import kotlinx.cinterop.*

class ThreadsHello {

    fun callBack() {

        println(testMakeString(1235))
        /*dispatchSingle<SomeData, SomeData>(
                {SomeData(21)},
                threadSafe {
                    SomeData(it.a * 2)
                },
                {
                    showText("The answer ${it.a}")
                }
        )*/
    }
}

fun showText(s:String){
    println(s)
}

data class SomeData(val a:Int)
