package co.touchlab.knarch.db

class StaleDataException:RuntimeException {
    constructor() : super() {}
    constructor(description:String) : super(description) {}
}