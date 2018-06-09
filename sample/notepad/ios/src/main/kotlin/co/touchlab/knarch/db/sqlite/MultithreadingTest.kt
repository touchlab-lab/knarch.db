package co.touchlab.knarch.db.sqlite

import co.touchlab.knarch.*
import co.touchlab.knarch.db.*
import co.touchlab.knarch.io.*
import co.touchlab.knarch.db.sqlite.*
import kotlin.test.*
import konan.test.*
import konan.worker.*
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.posix.*

class MultithreadingTest {
    lateinit var mDatabase: SQLiteDatabase
    lateinit var mDatabaseFile: File
    lateinit var mDatabaseFilePath: String
    lateinit var mDatabaseDir: String
    private val systemContext = DefaultSystemContext()
    private fun getContext(): SystemContext = systemContext

    @BeforeEach
    protected fun setUp() {
        getContext().deleteDatabase(DATABASE_FILE_NAME)
        println("A 1")
        mDatabaseFilePath = getContext().getDatabasePath(DATABASE_FILE_NAME).path
        println("A 2")
        mDatabaseFile = getContext().getDatabasePath(DATABASE_FILE_NAME)
        println("A 3")
        mDatabaseDir = mDatabaseFile.getParent()!!
        println("A 4")
        mDatabaseFile.getParentFile()?.mkdirs() // directory may not exist
        println("A 5 $mDatabaseFilePath")
        try {
            mDatabase = SQLiteDatabase.openOrCreateDatabase(mDatabaseFilePath, null).freeze()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        println("A 6")

        assertNotNull(mDatabase)

        println("A 7")
        try {
            mDatabase.execSQL("CREATE TABLE employee (_id INTEGER PRIMARY KEY, " + "name TEXT, month INTEGER, salary INTEGER);")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        println("A 8")
        mDatabase.execSQL("CREATE TABLE test (num INTEGER);")
        println("A 9")
        mDatabase.execSQL("CREATE TABLE testmore (_id INTEGER PRIMARY KEY, " + "name TEXT, age INTEGER, address TEXT);")
        println("A 10")
    }

    @AfterEach
    protected fun tearDown() {
        closeAndDeleteDatabase()
    }

    private fun closeAndDeleteDatabase() {
        /*try {*/
        mDatabase.close()
        SQLiteDatabase.deleteDatabase(mDatabaseFile)
        /*} catch (e: Exception) {
            //Ehh
        }*/
    }

    @Test
    fun basicMultithreadingTest() {
        runWorkers(mDatabase)
    }

    @Test
    fun cycleOperations() {

        val COUNT = 10
        val workers = Array(COUNT, { _ -> startWorker() })
        var shouldFail = false
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED,
                    { Pair(mDatabase, workerIndex).freeze() }) { pair ->
                val db = pair.first
                val windex = pair.second
                var allSuccess = true
                for (runs in 0 until 2000) {
                    var success = false

                    try {
                        when ((windex + runs) % 30) {
                            in 0..15 -> goExecSQL(db)
                            in 16..28 -> goQuery(db)
                            else -> goBig(db)
                        }
                        success = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (!success)
                        allSuccess = false
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
        workers.forEach {
            it.requestTermination().consume { _ -> }
        }

        if (shouldFail)
            fail("Failed cycleOperations")
    }



    fun runWorkers(dbArg: SQLiteDatabase) {
        val COUNT = 30
        val workers = Array(COUNT, { _ -> startWorker() })
        var shouldFail = false

        for (attempt in 1..3) {
            val futures = Array(workers.size, { workerIndex ->
                workers[workerIndex].schedule(TransferMode.CHECKED,
                        { Triple(dbArg, workerIndex, attempt).freeze() }) { triple ->
                    val db = triple.first
                    val windex = triple.second
                    val attempt = triple.third
                    val name = "Mike"
                    val age = 21
                    val address = "LA"
                    var allSuccess = true
                    for (runs in 0 until 5) {
                        var success = false
                        db.beginTransaction()

                        try {
                            val sql = "INSERT INTO testmore (name, age, address) VALUES (?, ?, ?);".freeze()
                            val insertStatement = db.compileStatement(sql)
                            for (i in 0 until ((30 + 5) - windex)) {
                                DatabaseUtils.bindObjectToProgram(insertStatement, 1, "$name $i")
                                DatabaseUtils.bindObjectToProgram(insertStatement, 2, age + i)
                                DatabaseUtils.bindObjectToProgram(insertStatement, 3, address)
                                insertStatement.execute()
                            }
                            insertStatement.close()
                            db.setTransactionSuccessful()

                            success = true
                        } finally {
                            db.endTransaction()
                        }
                        if (!success)
                            allSuccess = false

                        println("attempt $attempt worker $windex run $runs")
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
        var workerKill = 0
        workers.forEach {
            it.requestTermination().consume { _ -> }
            println("Killed $workerKill")
            workerKill++
        }

        if (shouldFail)
            fail("Failed multi")

        println("Exiting")
    }

    companion object {
        private val TAG = "SQLiteDatabaseTest"
        private val DATABASE_FILE_NAME = "database_test.db"
        private val TABLE_NAME = "test"
        private val COLUMN_ID_INDEX = 0
        private val COLUMN_NAME_INDEX = 1
        private val COLUMN_AGE_INDEX = 2
        private val COLUMN_ADDR_INDEX = 3
        private val TEST_PROJECTION = arrayOf<String>("_id", // 0
                "name", // 1
                "age", // 2
                "address" // 3
        )
    }
}

val goDb = true.freeze()

fun goExecSQL(db: SQLiteDatabase) {
    println("goExecSQL")
    if(goDb) {
        db.execSQL("INSERT INTO testmore (name, age, address) VALUES ('Mike', 20, 'LA');")
        db.execSQL("DELETE FROM testmore;")
    }
}

fun goQuery(db: SQLiteDatabase) {
    println("goQuery")
    if(goDb) {
        db.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('jack', '1', '2000');"))
        db.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('jack', '3', '1500');"))
        db.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('Jim', '1', '1000');"))
        db.execSQL(("INSERT INTO employee (name, month, salary) " + "VALUES ('Jim', '3', '3500');"))
        db.query(true, "employee", arrayOf<String>("name", "sum(salary)"), null, null, "name", "sum(salary)>1000", "name", null)
    }
}

fun goBig(db: SQLiteDatabase) {
    println("goBig")

    if(goDb) {
        db.beginTransaction()
        db.execSQL("CREATE TABLE testbig (num INTEGER, astr TEXT);")
        val stmt = db.compileStatement("INSERT INTO testbig (num, astr) VALUES (?, ?)")

        try {

            for (i in 0 until 10000) {
                val insStr = "OK big string insert val $i oh Binky is sad because food"
                stmt.bindLong(1, i.toLong())
                stmt.bindString(2, insStr)
                stmt.executeInsert()
            }

            val cursor = db.query("testbig",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )

            val numCol = cursor.getColumnIndex("num")
            val aStrCol = cursor.getColumnIndex("astr")
            var rowCount = 0

            while (cursor.moveToNext()) {
                rowCount++
            }

            cursor.close()
            db.execSQL("DROP TABLE testbig;")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction();
        }
    }
}