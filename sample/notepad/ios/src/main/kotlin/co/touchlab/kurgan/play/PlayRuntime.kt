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

    fun testTest()
    {
//        TestRunner.run(arrayOf("--ktest_filter=*testStatementConstraint*"))
//        TestRunner.run(arrayOf("--ktest_filter=*basicMultithreadingTest*"))
//        TestRunner.run(arrayOf("--ktest_filter=*testQuery*"))
//        TestRunner.run(arrayOf("--ktest_filter=*SQLiteDatabaseTest*"))
        TestRunner.run()
    }
}
