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

package co.touchlab.knarch

class Log{
    companion object {
        fun e(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun w(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun i(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun d(tag:String, message:String, t:Throwable? = null){
            println("$tag: $message")
        }
        fun isLoggable(s:String, level:Int):Boolean{
            return true
        }

        /**
         * Priority constant for the println method; use Log.v.
         */
        val VERBOSE = 2

        /**
         * Priority constant for the println method; use Log.d.
         */
        //TODO: Added the underscore because KN/Swift interop wasn't liking DEBUG
        //Remove in the future
        val DEBUG_ = 3

        /**
         * Priority constant for the println method; use Log.i.
         */
        val INFO = 4

        /**
         * Priority constant for the println method; use Log.w.
         */
        val WARN = 5

        /**
         * Priority constant for the println method; use Log.e.
         */
        val ERROR = 6

        /**
         * Priority constant for the println method.
         */
        val ASSERT = 7
    }
}