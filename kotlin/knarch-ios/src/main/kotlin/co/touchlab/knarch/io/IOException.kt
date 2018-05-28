package co.touchlab.knarch.io

open class IOException : Exception {

    /**
     * Constructs an `IOException` with `null`
     * as its error detail message.
     */
    constructor() : super() {}

    /**
     * Constructs an `IOException` with the specified detail message.
     *
     * @param message
     * The detail message (which is saved for later retrieval
     * by the [.getMessage] method)
     */
    constructor(message: String) : super(message) {}

    /**
     * Constructs an `IOException` with the specified detail message
     * and cause.
     *
     *
     *  Note that the detail message associated with `cause` is
     * *not* automatically incorporated into this exception's detail
     * message.
     *
     * @param message
     * The detail message (which is saved for later retrieval
     * by the [.getMessage] method)
     *
     * @param cause
     * The cause (which is saved for later retrieval by the
     * [.getCause] method).  (A null value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {}

    /**
     * Constructs an `IOException` with the specified cause and a
     * detail message of `(cause==null ? null : cause.toString())`
     * (which typically contains the class and detail message of `cause`).
     * This constructor is useful for IO exceptions that are little more
     * than wrappers for other throwables.
     *
     * @param cause
     * The cause (which is saved for later retrieval by the
     * [.getCause] method).  (A null value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    constructor(cause: Throwable) : super(cause) {}

}