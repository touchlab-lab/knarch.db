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

package co.touchlab.knarch.threads

import co.touchlab.multiplatform.architecture.threads.ThreadLocal
import kotlin.test.*
import konan.worker.*

class ThreadLocalTest{
    @Test
    fun threadLocal(){
        val tl = ThreadLocal<Vals>()
        tl.set(Vals(1))
        val worker = startWorker()
        val future = worker.schedule(TransferMode.CHECKED,
                {tl.freeze()})
        {
            it.set(Vals(2))
            it.get().freeze()
        }

        val fromWorker = future.consume {result ->
            result
        }

        assertNotEquals(fromWorker, tl.get())

        assertEquals(tl.get()!!.a, 1)
        assertEquals(fromWorker!!.a, 2)

        tl.remove()

        val future2 = worker.schedule(TransferMode.CHECKED,
                {tl.freeze()})
        {
            it.get().freeze()
        }

        val fromWorker2 = future2.consume {result ->
            result
        }

        assertNull(tl.get())
        assertEquals(fromWorker2!!.a, 2)
    }
}

data class Vals(val a:Int)