package co.touchlab.knarch.other

import kotlin.test.*

class LruCacheTest{
    @Test
    fun basicTest(){
        val cache = MyLruCache<String, RightSide>(5)
        cache.put("a", RightSide(1, "a"))
        cache.put("b", RightSide(2, "b"))
        assertEquals(2, cache.size())
        cache.put("a", RightSide(3, "c"))
        assertEquals(2, cache.size())
        assertEquals(3, cache.get("a")?.i)
        cache.put("c", RightSide(4, "c"))
        cache.put("d", RightSide(5, "d"))
        cache.put("e", RightSide(6, "e"))
        assertEquals(5, cache.size())
//        assertEquals(0, cache.removedCount)
        cache.put("f", RightSide(7, "f"))
        assertEquals(5, cache.size())
//        assertEquals(1, cache.removedCount)
    }

    @Test
    fun evictAllTest(){
        val cache = MyLruCache<String, RightSide>(5)
        cache.put("a", RightSide(1, "a"))
        cache.put("b", RightSide(2, "b"))
        assertEquals(0, cache.removedCount)
        cache.evictAll()
        assertEquals(2, cache.removedCount)
    }

}

class MyLruCache<K, V>(maxSize:Int):LruCache<K, V>(maxSize){
    var removedCount = 0
    override fun entryRemoved(evicted:Boolean, key:K, oldValue:V?, newValue:V?) {
        removedCount++
    }
}

data class RightSide(val i:Int, val s:String)
