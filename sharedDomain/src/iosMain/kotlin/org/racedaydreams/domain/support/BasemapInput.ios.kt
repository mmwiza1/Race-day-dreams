package org.racedaydreams.domain.support

actual abstract class BasemapInput {
    actual open fun read(buffer: ByteArray): Int = readChunk(buffer)

    protected abstract fun readChunk(buffer: ByteArray): Int
}

actual open class BasemapReadException : Exception {
    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

actual object BasemapInputs {
    actual fun bytes(bytes: ByteArray): BasemapInput = ByteSource(bytes)
}

private class ByteSource(private val bytes: ByteArray) : BasemapInput() {
    private var position = 0

    override fun readChunk(buffer: ByteArray): Int {
        if (position >= bytes.size) return -1
        val count = minOf(buffer.size, bytes.size - position)
        bytes.copyInto(buffer, 0, position, position + count)
        position += count
        return count
    }
}
