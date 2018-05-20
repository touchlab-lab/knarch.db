package co.touchlab.kurgan.architecture

interface SharedPreferences{

    interface OnSharedPreferenceChangeListener {
        fun onSharedPreferenceChanged(sharedPreferences:SharedPreferences, key:String)
    }

    interface Editor {
        fun putString(key:String , value:String?):Editor
        fun putInt(key:String , value:Int):Editor
        fun putLong(key:String, value:Long):Editor
        fun putFloat(key:String,value:Float):Editor
        fun putBoolean(key:String, value:Boolean):Editor
        fun remove(key:String):Editor
        fun clear():Editor
        fun commit():Boolean
        fun apply()
    }


    fun getString(key:String, defValue:String?):String?
    fun getInt(key:String, defValue:Int):Int
    fun getLong(key:String, defValue:Long):Long
    fun getFloat(key:String, defValue:Float):Float
    fun getBoolean(key:String, defValue:Boolean):Boolean
    fun contains(key:String):Boolean
    fun edit():Editor

    //Todo

    /*fun getAllKeys():Set<String>
    fun registerOnSharedPreferenceChangeListener(listener:OnSharedPreferenceChangeListener)
    fun unregisterOnSharedPreferenceChangeListener(listener:OnSharedPreferenceChangeListener)*/
}

//expect class SharedPreferencesImpl(file:File, mode:Int, initialContents:Map<String, Any>, mainHandler: Handler) : SharedPreferences{
//
//}