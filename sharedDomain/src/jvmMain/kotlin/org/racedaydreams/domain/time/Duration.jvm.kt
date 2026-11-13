package org.racedaydreams.domain.time

actual typealias Duration = java.time.Duration

actual object Durations {
    actual fun ofSeconds(seconds: Long, nanoAdjustment: Long): Duration =
        java.time.Duration.ofSeconds(seconds, nanoAdjustment)

    actual fun ofMinutes(minutes: Long): Duration = java.time.Duration.ofMinutes(minutes)

    actual fun ofHours(hours: Long): Duration = java.time.Duration.ofHours(hours)

    actual fun ofMillis(millis: Long): Duration = java.time.Duration.ofMillis(millis)

    actual fun ofNanos(nanos: Long): Duration = java.time.Duration.ofNanos(nanos)
}
