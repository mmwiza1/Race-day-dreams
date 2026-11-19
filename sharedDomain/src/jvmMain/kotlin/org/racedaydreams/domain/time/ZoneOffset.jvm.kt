package org.racedaydreams.domain.time

actual typealias ZoneOffset = java.time.ZoneOffset

actual object Offsets {
    actual fun ofTotalSeconds(seconds: Int): ZoneOffset =
        java.time.ZoneOffset.ofTotalSeconds(seconds)
}
