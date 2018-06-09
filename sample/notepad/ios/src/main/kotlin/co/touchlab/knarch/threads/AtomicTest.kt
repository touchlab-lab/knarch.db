package co.touchlab.knarch.threads

import kotlin.test.*
import konan.test.*
import konan.worker.*
import co.touchlab.kite.threads.*
import kotlin.coroutines.experimental.buildIterator

class AtomicTest {


    /*@Test
    fun accessForResult(){
        val atomic = AtTest()

//        assertFalse (atomic.hasVal())

        atomic.putVal {MySubData("cstring", MyData("astring", 2))}

        assertTrue (atomic.hasVal())
        assertTrue (atomic.hasSubVal("astring"))
    }*/

    /*@Test
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
        *//*bigAtom.access {
            it.printAll()
        }*//*
    }

    @Test
    fun testAMix(){
        val mf = MainFrozen()
        runWorkers(mf)
    }

    fun runWorkers(mf: MainFrozen) {
        val COUNT = 30
        val workers = Array(COUNT, { _ -> startWorker() })
        var shouldFail = false
        for (attempt in 1..2) {
            val futures = Array(workers.size, { workerIndex ->
                workers[workerIndex].schedule(TransferMode.CHECKED,
                        { mf.freeze() }) {
                    *//*val db = pair.first
                    val windex = pair.second
                    val name = "Mike"
                    val age = 21
                    val address = "LA"*//*
                    var allSuccess = true
                    for (runs in 0 until 100) {
                        var success = false
//                        db.beginTransaction()


                        try {
                            val sql = "INSERT INTO testmore (name, age, address) VALUES (?, ?, ?);"
                            it.subData.prepare(sql)
                            *//*val insertStatement = db.compileStatement(sql)
                            for (i in 0 until ((30 + 5) - windex)) {
                                DatabaseUtils.bindObjectToProgram(insertStatement, 1, "$name $i")
                                DatabaseUtils.bindObjectToProgram(insertStatement, 2, age + i)
                                DatabaseUtils.bindObjectToProgram(insertStatement, 3, address)
                                insertStatement.execute()
                            }
                            insertStatement.close()
                            db.setTransactionSuccessful()

                            success = true*//*
                        } finally {
//                            db.endTransaction()
                        }
                        *//*if (!success)
                            allSuccess = false*//*

//                        println("worker $windex run $runs")
                    }

                    return@schedule allSuccess
                }
            })
            val futureSet = futures.toSet()
            var consumed = 0

            while (consumed < futureSet.size) {
                val ready = futureSet.waitForMultipleFutures(100000)
                ready.forEach {
                    it.consume {
                        consumed++
                        if (!it) shouldFail = true
                    }
                }
            }
        }
        workers.forEach {
            it.requestTermination().consume { _ -> }
        }

        if (shouldFail)
            fail("Failed multi")
    }

    class MainFrozen{
        val subData = SubDataAtomic()
    }

    class SubDataAtomic{
        val subSubHotAtomic : Atomic<SubSubHot> = Atomic(SubSubHot())

        fun prepare(sql:String){
            sql.freeze()
            subSubHotAtomic.access {
                var mapVal :PreparedStatement? = it.map.get(sql)
                if(mapVal == null)
                {
                    val info = PreparedStatementInfo(sql, 0, 1, 2, false)
                    mapVal = PreparedStatement(info)
                    it.map.put(sql, mapVal)
                }

                mapVal.mInCache = !mapVal.mInCache
                mapVal.mInUse = !mapVal.mInUse
            }
        }
    }

    class SubSubHot{
        val map = HashMap<String, PreparedStatement?>()

    }

    class PreparedStatement(val info:PreparedStatementInfo) {
        // True if the statement is in the cache.
        var mInCache:Boolean = false
        // True if the statement is in use (currently executing).
        // We need this flag because due to the use of custom functions in triggers, it's
        // possible for SQLite calls to be re-entrant. Consequently we need to prevent
        // in use statements from being finalized until they are no longer in use.
        var mInUse:Boolean = false
    }

    data class PreparedStatementInfo(// The SQL from which the statement was prepared.
            val mSql:String,
            // The native sqlite3_stmt object pointer.
            // Lifetime is managed explicitly by the connection.
            val mStatementPtr:Long,
            // The number of parameters that the prepared statement has.
            val mNumParameters:Int,
            // The statement type.
            val mType:Int,
            // True if the statement is read-only.
            val mReadOnly:Boolean)
*/

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