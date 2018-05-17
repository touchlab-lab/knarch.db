package co.touchlab.kurgan

actual class LogImpl actual constructor() {
    actual fun e(tag: String, message: String) {

    }
    actual fun w(tag: String, message: String) {}
    actual fun i(tag: String, message: String) {}
    actual fun d(tag: String, message: String) {}
    actual fun v(tag: String, message: String) {}
    actual fun e(tag: String, message: String, ex: Throwable) {}
    actual fun w(tag: String, message: String, ex: Throwable) {}
    actual fun i(tag: String, message: String, ex: Throwable) {}
    actual fun d(tag: String, message: String, ex: Throwable) {}
    actual fun v(tag: String, message: String, ex: Throwable) {}
}