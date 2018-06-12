package co.touchlab.knarch

import konan.test.*
import kotlin.test.*

class TestHarness{
    fun testTest()
    {
//        TestRunner.run(arrayOf("--ktest_filter=*testStatementConstraint*"))
//        TestRunner.run(arrayOf("--ktest_filter=*basicMultithreadingTest*"))
//        TestRunner.run(arrayOf("--ktest_filter=*testQuery*"))
//        TestRunner.run(arrayOf("--ktest_filter=*SQLiteDatabaseTest*"))
        TestRunner.run()
    }
}