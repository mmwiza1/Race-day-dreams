package org.racedaydreams.domain.time

actual typealias Instant = java.time.Instant

actual object Instants {
    actual fun ofEpochSecond(seconds: Long, nanoAdjustment: Long): Instant =
        java.time.Instant.ofEpochSecond(seconds, nanoAdjustment)
}
