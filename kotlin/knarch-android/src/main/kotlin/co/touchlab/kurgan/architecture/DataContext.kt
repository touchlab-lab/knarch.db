package co.touchlab.kurgan.architecture

import android.app.Application

class JvmDataContext(val context: Application):DataContext {
    override fun nativeContext(): Any {
        return context
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = SharedPreferencesImpl(context.getSharedPreferences(name, mode))
}