package org.racedaydreams.domain.time

actual class Instant internal constructor(private val value: InstantParts) : Comparable<Instant> {
    actual fun getEpochSecond(): Long = value.seconds

    actual fun getNano(): Int = value.nanos

    actual fun isAfter(other: Instant): Boolean = compareTo(other) > 0

    actual fun isBefore(other: Instant): Boolean = compareTo(other) < 0

    actual override fun compareTo(other: Instant): Int =
        value.seconds.compareTo(other.value.seconds).takeIf { it != 0 }
            ?: value.nanos.compareTo(other.value.nanos)

    override fun equals(other: Any?): Boolean = other is Instant && value == other.value

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.text()
}

actual object Instants {
    actual fun ofEpochSecond(seconds: Long, nanoAdjustment: Long): Instant =
        Instant(InstantParts.ofEpochSecond(seconds, nanoAdjustment))
}
