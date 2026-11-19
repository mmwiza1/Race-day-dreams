package org.racedaydreams.domain.time

actual class ZoneOffset internal constructor(private val seconds: Int) {
    init {
        require(seconds in -64800..64800) { "Zone offset must be within -18:00 to +18:00." }
    }

    actual fun getTotalSeconds(): Int = seconds

    actual fun getId(): String = offsetText(seconds)

    override fun equals(other: Any?): Boolean = other is ZoneOffset && seconds == other.seconds

    override fun hashCode(): Int = seconds

    actual override fun toString(): String = getId()
}

actual object Offsets {
    actual fun ofTotalSeconds(seconds: Int): ZoneOffset = ZoneOffset(seconds)
}
