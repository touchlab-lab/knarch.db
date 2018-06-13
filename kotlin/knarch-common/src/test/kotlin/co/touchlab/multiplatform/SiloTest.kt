package co.touchlab.multiplatform

expect annotation class Test

class SiloTest{
    @Test
    fun hiloTest(){
        assertEquals(hilo(), 323)
    }
}