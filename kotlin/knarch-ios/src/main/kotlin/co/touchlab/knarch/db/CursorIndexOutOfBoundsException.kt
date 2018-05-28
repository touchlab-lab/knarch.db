package co.touchlab.knarch.db

class CursorIndexOutOfBoundsException:IndexOutOfBoundsException {
    constructor(index:Int, size:Int) : super("Index $index requested, with a size of $size") {}
    constructor(message:String) : super(message)
}