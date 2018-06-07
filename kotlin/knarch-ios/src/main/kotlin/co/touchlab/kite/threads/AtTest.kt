package co.touchlab.kite.threads

class AtTest{
    internal val atomic: Atomic<MySubData> = Atomic(null)

    fun hasVal():Boolean{
        return atomic.accessForResult { s -> s != null }
    }

    fun hasSubVal(testString:String):Boolean {
        return atomic.accessForResult { s -> s!!.d.a == testString }
    }

    fun putVal(v:()->MySubData){
        atomic.putValue(v)
    }


}

class MySubData(var c:String, var d:MyData)

class MyData(var a:String, var b:Int)

//assertFalse (atomic.accessForResult { s -> s != null })
//
//atomic.putValue{MyData("asdf", 2)}
//
//assertTrue (atomic.accessForResult { s -> s != null })