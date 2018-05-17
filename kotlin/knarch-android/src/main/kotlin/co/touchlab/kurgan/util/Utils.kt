package co.touchlab.kurgan.util

import java.nio.charset.StandardCharsets

actual fun currentTimeMillis():Long = System.currentTimeMillis()

actual fun stringToUtf8(s:String):ByteArray{
    return s.toByteArray(StandardCharsets.UTF_8)
}

actual fun utf8ToString(b:ByteArray):String{
    return String(b, StandardCharsets.UTF_8)
}