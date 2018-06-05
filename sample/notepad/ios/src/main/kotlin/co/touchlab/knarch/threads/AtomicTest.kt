package co.touchlab.knarch.threads

import kotlin.test.*
import konan.test.*
import konan.worker.*
import co.touchlab.kite.threads.*
import kotlin.coroutines.experimental.buildIterator

class AtomicTest {

    @Test
    fun accessUpdateTest() {
        val COUNT = 50
        val bigAtom = Atomic(Incrementer(0)).freeze()
        val workers = Array(COUNT, { _ -> startWorker() })
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED,
                    { bigAtom }) { atom ->

                for (runs in 0 until 10000) {

                    atom.accessUpdate { inc ->
                        Incrementer(inc.count+1)
                    }
                }
            }
        })
        val futureSet = futures.toSet()
        var consumed = 0

        while (consumed < futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(100000)
            ready.forEach {
                it.consume {
                    consumed++
                }
            }
        }
        workers.forEach {
            it.requestTermination().consume { _ -> }
        }

        bigAtom.access {
            assertEquals(500000, it.count)
        }
    }

    @Test
    fun accessWithTest() {
        val COUNT = 50
        val bigAtom = Atomic(IncrementerVar(0)).freeze()
        val workers = Array(COUNT, { _ -> startWorker() })
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED,
                    { bigAtom }) { atom ->

                for (runs in 0 until 10000) {

                    atom.accessWith({runs}) { inc,theVal ->
                        inc.lastVal = theVal
                    }
                }
            }
        })
        val futureSet = futures.toSet()
        var consumed = 0

        while (consumed < futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(100000)
            ready.forEach {
                it.consume {
                    consumed++
                }
            }
        }
        workers.forEach {
            it.requestTermination().consume { _ -> }
        }

        bigAtom.access {
            assertEquals(9999, it.lastVal)
        }
    }

    @Test
    fun accessForResultTest() {
        val COUNT = 50
        val bigAtom = Atomic(Collector()).freeze()
        val workers = Array(COUNT, { _ -> startWorker() })
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED,
                    { bigAtom }) { atom ->

                val allResults = ArrayList<String>()
                for (runs in 0 until 10000) {
                    val result = atom.accessForResult { col ->
                        col.addPull("My run $runs")
                    }

                    allResults.add(result)
                }

                allResults
            }
        })
        val futureSet = futures.toSet()
        var consumed = 0

        val allAllResults = ArrayList<String>()

        while (consumed < futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(100000)
            ready.forEach {
                it.consume {
                    consumed++
                    allAllResults.addAll(it)
                }
            }
        }
        workers.forEach {
            it.requestTermination().consume { _ -> }
        }

        assertEquals(500000, allAllResults.size)
        /*bigAtom.access {
            it.printAll()
        }*/
    }
}

class Incrementer(val count: Int)
class IncrementerVar(var lastVal: Int)
class Collector(){
    val theList=ArrayList<String>()
    fun addPull(s:String):String{
        theList.add(s)
        if(theList.size > 30)
            return theList.removeAt(0)
        else
            return "nah..."
    }

    fun printAll(){
        theList.forEach { println(it) }
    }
}