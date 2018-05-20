package co.touchlab.knarch.db

import co.touchlab.knarch.Log

class ContentValues{

    companion object {
        val TAG = "ContentValues"
    }

        /** Holds the actual values */
    private val mValues:HashMap<String, Any?>

    /**
     * Creates an empty set of values using the default initial size
     */
    constructor() {
        // Choosing a default size of 8 based on analysis of typical
        // consumption by applications.
        mValues = HashMap()
    }

    /**
     * Creates an empty set of values using the given initial size
     *
     * @param size the initial size of the set of values
     */
    constructor(size:Int) {
        mValues = HashMap(size)
    }

    /**
     * Creates a set of values copied from the given set
     *
     * @param from the values to copy
     */
    constructor(from:ContentValues) {
        mValues = HashMap(from.mValues)
    }

    override fun equals(other:Any?):Boolean {
        if(other == null)
            return false
        if (!(other is ContentValues)) {
            return false
        }
        return mValues.equals(other.mValues)
    }

    override fun hashCode():Int {
        return mValues.hashCode()
    }

    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:String) {
        mValues.put(key, value)
    }

    /**
     * Adds all values from the passed in ContentValues.
     *
     * @param other the ContentValues from which to copy
     */
    fun putAll(other:ContentValues) {
        mValues.putAll(other.mValues)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Byte) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Short) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Int) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Long) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Float) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Double) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:Boolean) {
        mValues.put(key, value)
    }
    /**
     * Adds a value to the set.
     *
     * @param key the name of the value to put
     * @param value the data for the value to put
     */
    fun put(key:String, value:ByteArray) {
        mValues.put(key, value)
    }
    /**
     * Adds a null value to the set.
     *
     * @param key the name of the value to make null
     */
    fun putNull(key:String) {
        mValues.put(key, null)
    }
    /**
     * Returns the number of values.
     *
     * @return the number of values
     */
    fun size():Int {
        return mValues.size
    }
    /**
     * Remove a single value.
     *
     * @param key the name of the value to remove
     */
    fun remove(key:String) {
        mValues.remove(key)
    }
    /**
     * Removes all values.
     */
    fun clear() {
        mValues.clear()
    }
    /**
     * Returns true if this object has the named value.
     *
     * @param key the value to check for
     * @return {@code true} if the value is present, {@code false} otherwise
     */
    fun containsKey(key:String):Boolean {
        return mValues.containsKey(key)
    }

    /**
     * Gets a value. Valid value types are {@link String}, {@link Boolean}, and
     * {@link Number} implementations.
     *
     * @param key the value to get
     * @return the data for the value
     */
    fun get(key:String):Any? {
        return mValues.get(key)
    }
    /**
     * Gets a value and converts it to a String.
     *
     * @param key the value to get
     * @return the String for the value
     */
    fun getAsString(key:String):String? {
        val value = mValues.get(key)
        return if (value != null) value.toString() else null
    }
    /**
     * Gets a value and converts it to a Long.
     *
     * @param key the value to get
     * @return the Long value, or null if the value is missing or cannot be converted
     */
    fun getAsLong(key:String):Long? {
        val value = mValues.get(key)
        try
        {
            return if (value != null) (value as Number).toLong() else null
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                try
                {
                    return value.toString().toLong()
                }
                catch (e2:NumberFormatException) {
                    Log.e(TAG, "Cannot parse Long value for $value at key $key")
                    return null
                }
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Long: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value and converts it to an Integer.
     *
     * @param key the value to get
     * @return the Integer value, or null if the value is missing or cannot be converted
     */
    fun getAsInteger(key:String):Int? {
        val value = mValues.get(key)
        try
        {
            return if (value != null) (value as Number).toInt() else null
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                try
                {
                    return value.toString().toInt()
                }
                catch (e2:NumberFormatException) {
                    Log.e(TAG, "Cannot parse Integer value for $value at key $key")
                    return null
                }
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Integer: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value and converts it to a Short.
     *
     * @param key the value to get
     * @return the Short value, or null if the value is missing or cannot be converted
     */
    fun getAsShort(key:String):Short? {

        val value = mValues.get(key)
        try
        {
            return if (value != null) (value as Number).toShort() else null
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                try
                {
                    return value.toString().toShort()
                }
                catch (e2:NumberFormatException) {
                    Log.e(TAG, "Cannot parse Short value for $value at key $key")
                    return null
                }
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Short: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value and converts it to a Byte.
     *
     * @param key the value to get
     * @return the Byte value, or null if the value is missing or cannot be converted
     */
    fun getAsByte(key:String):Byte? {
        val value = mValues.get(key)
        try
        {
            return if (value != null) (value as Number).toByte() else null
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                try
                {
                    return value.toString().toByte()
                }
                catch (e2:NumberFormatException) {
                    Log.e(TAG, "Cannot parse Byte value for $value at key $key")
                    return null
                }
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Byte: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value and converts it to a Double.
     *
     * @param key the value to get
     * @return the Double value, or null if the value is missing or cannot be converted
     */
    fun getAsDouble(key:String):Double? {
        val value = mValues.get(key)
        try
        {
            return if (value != null) (value as Number).toDouble() else null
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                try
                {
                    return value.toString().toDouble()
                }
                catch (e2:NumberFormatException) {
                    Log.e(TAG, "Cannot parse Double value for $value at key $key")
                    return null
                }
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Double: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value and converts it to a Float.
     *
     * @param key the value to get
     * @return the Float value, or null if the value is missing or cannot be converted
     */
    fun getAsFloat(key:String):Float? {
        val value = mValues.get(key)
        try
        {
            return if (value != null) (value as Number).toFloat() else null
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                try
                {
                    return value.toString().toFloat()
                }
                catch (e2:NumberFormatException) {
                    Log.e(TAG, "Cannot parse Float value for $value at key $key")
                    return null
                }
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Float: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value and converts it to a Boolean.
     *
     * @param key the value to get
     * @return the Boolean value, or null if the value is missing or cannot be converted
     */
    fun getAsBoolean(key:String):Boolean? {
        val value = mValues.get(key)
        try
        {
            return value as Boolean
        }
        catch (e:ClassCastException) {
            if (value is CharSequence)
            {
                return value.toString().toBoolean()
            }
            else if (value is Number)
            {
                return (value as Number).toInt() != 0
            }
            else
            {
                Log.e(TAG, "Cannot cast value for $key to a Boolean: $value", e)
                return null
            }
        }
    }
    /**
     * Gets a value that is a byte array. Note that this method will not convert
     * any other types to byte arrays.
     *
     * @param key the value to get
     * @return the byte[] value, or null is the value is missing or not a byte[]
     */
    fun getAsByteArray(key:String):ByteArray? {
        val value = mValues.get(key)
        return value as? ByteArray
    }
    /**
     * Returns a set of all of the keys and values
     *
     * @return a set of all of the keys and values
     */
    fun valueSet():Set<Map.Entry<String, Any?>> {
        return mValues.entries
    }
    /**
     * Returns a set of all of the keys
     *
     * @return a set of all of the keys
     */
    fun keySet():Set<String> {
        return mValues.keys
    }

    /**
     * Returns a string containing a concise, human-readable description of this object.
     * @return a printable representation of this object.
     */
    override fun toString():String {
        val sb = StringBuilder()
        for (name in mValues.keys)
        {
            val value = getAsString(name)
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append("$name=$value")
        }
        return sb.toString()
    }
}