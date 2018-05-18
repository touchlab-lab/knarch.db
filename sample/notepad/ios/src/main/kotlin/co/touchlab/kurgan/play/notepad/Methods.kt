package co.touchlab.kurgan.play.notepad

import kotlinx.cinterop.*

actual fun memzy(body: () -> Unit){
    memScoped {
        autoreleasepool {
            body()
        }
    }
}
