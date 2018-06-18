/*
 * Copyright (c) 2018 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.multiplatform.architecture.db

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