package co.touchlab.kurgan.architecture

class SharedPreferencesImpl(val sp: android.content.SharedPreferences) : SharedPreferences{

    val changeListMap = HashMap<SharedPreferences.OnSharedPreferenceChangeListener, android.content.SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getString(key: String, defValue: String?): String? = sp.getString(key, defValue)
    override fun getInt(key: String, defValue: Int): Int = sp.getInt(key, defValue)

    override fun getLong(key: String, defValue: Long): Long = sp.getLong(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float = sp.getFloat(key, defValue)

    override fun getBoolean(key: String, defValue: Boolean): Boolean = sp.getBoolean(key, defValue)

    override fun contains(key: String): Boolean = sp.contains(key)

    class EditorImpl(val editor: android.content.SharedPreferences.Editor): SharedPreferences.Editor{
        override fun clear(): SharedPreferences.Editor {
            editor.clear()
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            editor.putLong(key, value)
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            editor.putFloat(key, value)
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            editor.putInt(key, value)
            return this
        }

        override fun apply() {
            editor.apply()
        }

        override fun remove(key: String): SharedPreferences.Editor {
            editor.remove(key)
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            editor.putBoolean(key, value)
            return this
        }

        override fun commit(): Boolean = editor.commit()

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            editor.putString(key, value)
            return this
        }
    }

    override fun edit(): SharedPreferences.Editor = EditorImpl(sp.edit())

    /*override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        throw UnsupportedOperationException("registerOnSharedPreferenceChangeListener")
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        throw UnsupportedOperationException("registerOnSharedPreferenceChangeListener")
    }*/

}
