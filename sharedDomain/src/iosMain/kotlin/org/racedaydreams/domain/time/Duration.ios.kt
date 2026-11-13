package org.racedaydreams.domain.time

actual class Duration internal constructor(private val value: DurationParts) {
    actual fun getSeconds(): Long = value.seconds

    actual fun getNano(): Int = value.nanos

    actual fun isNegative(): Boolean = value.seconds < 0

    actual fun isZero(): Boolean = value.seconds == 0L && value.nanos == 0

    override fun equals(other: Any?): Boolean = other is Duration && value == other.value

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.text()
}

actual object Durations {
    actual fun ofSeconds(seconds: Long, nanoAdjustment: Long): Duration =
        Duration(DurationParts.ofSeconds(seconds, nanoAdjustment))

    actual fun ofMinutes(minutes: Long): Duration = ofSeconds(checkedScale(minutes, 60))

    actual fun ofHours(hours: Long): Duration = ofSeconds(checkedScale(hours, 3600))

    actual fun ofMillis(millis: Long): Duration =
        ofSeconds(millis.floorDiv(1000L), millis.mod(1000L) * 1000000L)

    actual fun ofNanos(nanos: Long): Duration = ofSeconds(0, nanos)
}
