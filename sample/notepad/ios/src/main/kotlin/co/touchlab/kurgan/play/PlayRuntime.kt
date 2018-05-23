package co.touchlab.kurgan.play

import co.touchlab.kurgan.play.notepad.*
import kotlinx.cinterop.*
import co.touchlab.kite.threads.*
import konan.worker.*
import co.touchlab.knarch.*
import co.touchlab.knarch.db.sqlite.*
import co.touchlab.knarch.db.*
import konan.test.*
import kotlin.test.*

@Test fun runTest() {
    assertTrue(true)
}

class PlayRuntime(){

    fun testAtomicBox(){
        memScoped {
//            val dataContainer = DataContainer(AtomicBox<MyThing>({ MyThing(lotso = mutableListOf<Int>(
//                    1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0
//            )) }), 123)
//        dataContainer.freeze()
//            goNuts(dataContainer)
//            val tab = TestAtomicBox()
//            tab.goHello()
        }
    }

    fun testThreads(){
        val th = ThreadsHello()
        th.callBack()
    }

    fun testTest()
    {
//        TestRunner.run(arrayOf("--ktest_filter=*LruCacheTest*"))
//        TestRunner.run(arrayOf("--ktest_filter=*testDelete*"))
        TestRunner.run()
    }



    fun testDb()
    {
        val context = DefaultSystemContext()

        val dbHelper = DbHelper(context, "testdbwithbytes", 1)
        val db = dbHelper.writableDatabase
        val cv = ContentValues()
        cv.put("asdf", "qwert")
        val stringToChange = "Hello I like bytes"
        val ba = ByteArray(stringToChange.length, {i -> stringToChange.get(i).toByte()})
        cv.put("fff", ba)
        db.insert("testtable", null, cv)
    }

    companion object {
        fun hi(mems: Boolean){
//            val pr = PlayRuntime()
//            pr.helloStart(mems)
        }
    }
}

class DbHelper(mContext:SystemContext, databaseName:String?, mNewVersion:Int):SQLiteOpenHelper(
        mContext = mContext,
        databaseName = databaseName,
        mNewVersion = mNewVersion
        ){

    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("create table testtable(asdf TEXT, fff BLOB)")
    }

    override fun onUpgrade(db:SQLiteDatabase, oldVersion:Int, newVersion:Int){

    }
}

///Users/kgalligan/temp4/databasetest

/*
class DataContainer(val atom:AtomicBox<MyThing>, var nocount:Int)

class MyThing(var counter:Long = 0, var lotso:MutableList<Int>)

fun goNuts(dataContainer: DataContainer){
    val COUNT = 10
    val workers = Array(COUNT, { _ -> startWorker()})

    for (attempt in 1 .. 5) {
        var workerCount = 0

        val futures = Array(workers.size, { workerIndex -> workers[workerIndex].schedule(TransferMode.CHECKED,
                {Pair(
                        dataContainer
                        ,workerCount++
                )}
        ) { input ->
            println("Worker Starting: ${input.second}")
            for(i in 0 until 50000){
                input.first.atom.access({
                    var localVar = it.counter
                    val b = (i+i).toDouble()/1000.toDouble()
                    if(localVar%100000==0L)
                    {println("Called Called double inner: $localVar")}

                    it.counter = localVar+1
                    return@access b
                })

                */
/*input.first.openBox()

                try {
                    var localVar = input.first.target.counter
                    val b = (i+i).toDouble()/1000.toDouble()
                    if(localVar%1000==0L)
                    {println("Called direct: $localVar")}

                    input.first.target.counter = localVar+1
                } finally {
                    input.first.closeBox()
                }*//*

            }
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
    dataContainer.atom.access {
        println("Total count: ${it.counter}")
    }
}*/
