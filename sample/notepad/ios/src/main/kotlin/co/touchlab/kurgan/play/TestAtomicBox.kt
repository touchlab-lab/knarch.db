package co.touchlab.kurgan.play

import co.touchlab.kite.threads.*
import konan.worker.*
import kotlinx.cinterop.*

/*
class TestAtomicBox {
    val foo = makeData().freeze()
    fun goHello(){

        runFooWorkers(foo)

        foo.bar.access{
            println("Total count: ${it.meCount}")
        }

    }
}

fun doFooStuff(it:Foo){
    it.bar.access {bar ->
        bar.t = "My run ${bar.meCount}"
        for(asdf in bar.asdfArray){
            asdf.first = "Asdf run ${bar.meCount}"
            asdf.second = bar.meCount
        }
        if(bar.meCount % 100000 == 0)
            println("MeCount: ${bar.meCount}")
        bar.meCount++
    }
}

class Foo(var bar: AtomicBox<Bar>, var deep: BazDeep, val fb:FooBoo, val callbackWorker:Worker)
class Bar(var t: String, val asdfArray: Array<Asdf>, var meCount:Int = 0)
class Asdf(var first: String, var second: Int)
data class Baz(val a: String, val b: Int, val c: Float, val dp: BazDeep)
data class BazDeep(val r: Int, val s: String)
class FooBoo(var bar: AtomicBox<Bar>)

fun runFooWorkers(foo:Foo){
    val COUNT = 10
    val workers = Array(COUNT, { _ -> startWorker()})

    for (attempt in 1 .. 5) {
//        var workerCount = 0

        val futures = Array(workers.size, { workerIndex -> workers[workerIndex].schedule(TransferMode.CHECKED,
                {foo}
        ) {
            var loopCount = 0
            for(i in 0 until 15000) {
                doFooStuff(it)
                loopCount++
            }
            callCallback(loopCount, it)
        }
        })
        val futureSet = futures.toSet()
        var consumed = 0
        while (consumed < futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(100000)
            ready.forEach {
                it.consume {consumed++}
            }
        }
    }
    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
}

fun callCallback(loopCount:Int, foo:Foo){
    foo.callbackWorker.schedule(TransferMode.CHECKED,
            {loopCount}
    ) {
        println("Callback Worker: $it")
    }


}


fun makeData(): Foo {
    var bar = Bar("hello!",
            arrayOf
            (
                    Asdf("a",1),
                    Asdf("b",2),
                    Asdf("c",3),
                    Asdf("d",4)
            )
    )
    val atomicBox = AtomicBox({ bar })
    val foo = Foo(atomicBox, BazDeep(22, "TopDeep"), FooBoo(atomicBox), startWorker())
    return foo
}

*/
