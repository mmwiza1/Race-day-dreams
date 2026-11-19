package org.racedaydreams.domain.support

actual typealias BasemapInput = java.io.InputStream

actual typealias BasemapReadException = java.io.IOException

actual object BasemapInputs {
    actual fun bytes(bytes: ByteArray): BasemapInput = java.io.ByteArrayInputStream(bytes)
}
