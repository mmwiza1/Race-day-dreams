package org.racedaydreams.domain.support

/** Caller owns/closes the source. Positive read counts or -1 at EOF; resource errors are thrown. */
expect abstract class BasemapInput {
    open fun read(buffer: ByteArray): Int
}

expect object BasemapInputs {
    fun bytes(bytes: ByteArray): BasemapInput
}

expect open class BasemapReadException : Exception {
    constructor(message: String?)

    constructor(message: String?, cause: Throwable?)
}
