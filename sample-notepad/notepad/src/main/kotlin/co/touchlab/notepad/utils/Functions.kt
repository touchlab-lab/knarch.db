package co.touchlab.notepad.utils

expect fun currentTimeMillis():Long

expect fun <A, B> backgroundTask(backJob:(A)-> B, arg:A, mainJob:(B) -> Unit)