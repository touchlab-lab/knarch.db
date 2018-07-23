package co.touchlab.notepad.utils

import android.app.Application
import android.os.Handler
import android.os.Looper
import co.touchlab.multiplatform.architecture.db.sqlite.AndroidNativeOpenHelperFactory
import co.touchlab.multiplatform.architecture.db.sqlite.NativeOpenHelperFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun <B> backgroundTask(backJob: () -> B, mainJob: (B) -> Unit) {
    AppContext.backgroundTask(backJob, mainJob)
}

actual fun createNativeOpenHelperFactory(): NativeOpenHelperFactory = AndroidNativeOpenHelperFactory(AppContext.app)

object AppContext{
    lateinit var app:Application

    val executor = Executors.newSingleThreadExecutor()
    val handler = Handler(Looper.getMainLooper())

    fun <B> backgroundTask(backJob: () -> B, mainJob: (B) -> Unit) {
        executor.execute {
            val aref = AtomicReference<B>()
            try {
                aref.set(backJob())
                handler.post {
                    mainJob(aref.get())
                }
            }catch (t:Throwable){
                t.printStackTrace()
            }

        }
    }
}