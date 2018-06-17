
import konan.test.*
import kotlin.test.*

fun main(args: Array<String>):Int {
    TestRunner.run(arrayOf("--ktest_filter=*testEnableAndDisableForeignKeys*"))
//    TestRunner.run()
    return TestRunner.exitCode
}