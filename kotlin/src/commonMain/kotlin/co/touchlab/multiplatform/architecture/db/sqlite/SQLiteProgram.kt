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

package co.touchlab.multiplatform.architecture.db.sqlite

expect abstract class SQLiteProgram:SQLiteClosable{
    fun bindNull(index:Int)
    fun bindLong(index:Int, value:Long)
    fun bindDouble(index:Int, value:Double)
    fun bindString(index:Int, value:String)
    fun bindBlob(index:Int, value:ByteArray)
    fun clearBindings()
    fun bindAllArgsAsStrings(bindArgs:Array<String>?)
}

expect abstract class SQLiteClosable{
    fun close()
}