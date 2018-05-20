package co.touchlab.kurgan.architecture.database

expect class ContentValues(){
    constructor(size: Int)
    constructor(from: ContentValues)

    fun put(key: String, value: String)
    fun putAll(other:ContentValues)
    fun put(key: String, value: Byte)
    fun put(key: String, value: Short)
    fun put(key: String, value: Int)
    fun put(key: String, value: Long)
    fun put(key: String, value: Float)
    fun put(key: String, value: Double)
    fun put(key: String, value: Boolean)
    fun put(key: String, value: ByteArray)
    fun putNull(key: String)
    fun size():Int
    fun remove(key: String)
    fun clear()
    fun containsKey(key: String): Boolean
    fun get(key:String):Any?
    fun getAsString(key: String): String?
    fun getAsLong(key: String): Long?
    fun getAsInteger(key: String): Int?
    fun getAsShort(key: String): Short?
    fun getAsByte(key: String): Byte?
    fun getAsDouble(key: String): Double?
    fun getAsFloat(key: String): Float?
    fun getAsBoolean(key: String): Boolean?
    fun getAsByteArray(key: String): ByteArray?
    fun valueSet(): Set<Map.Entry<String,Any?>>
    fun keySet():Set<String>

}