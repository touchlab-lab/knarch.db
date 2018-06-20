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

package co.touchlab.multiplatform.architecture.threads

actual open class ThreadLocalImpl<T> actual constructor(){
    private val threadLocalId = nextThreadLocalId()
    @Suppress("UNCHECKED_CAST")
    actual fun get():T?{
        return if(threadLocalMap.containsKey(threadLocalId))
            threadLocalMap.get(threadLocalId) as T
        else
            null
    }

    actual fun remove(){
        threadLocalMap.remove(threadLocalId)
    }

    actual fun set(value: T?){
        if(value == null)
            remove()
        else
            threadLocalMap.put(threadLocalId, value)
    }
}

private val threadLocalMap = HashMap<Int, Any>()

@SymbolName("ThreadSupport_nextThreadLocalId")
private external fun nextThreadLocalId():Int
