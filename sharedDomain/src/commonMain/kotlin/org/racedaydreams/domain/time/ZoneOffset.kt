package org.racedaydreams.domain.time

/** A recorded offset, not an inferred time zone. Missing source offsets remain nullable. */
expect class ZoneOffset {
    fun getTotalSeconds(): Int

    fun getId(): String

    override fun toString(): String
}

expect object Offsets {
    fun ofTotalSeconds(seconds: Int): ZoneOffset
}

internal fun offsetText(seconds: Int): String {
    require(seconds in -64800..64800) { "Zone offset must be within -18:00 to +18:00." }
    if (seconds == 0) return "Z"
    val positive = if (seconds < 0) -seconds else seconds
    return (if (seconds < 0) "-" else "+") +
        (positive / 3600).toString().padStart(2, '0') +
        ":" +
        (positive % 3600 / 60).toString().padStart(2, '0') +
        if (positive % 60 == 0) "" else ":" + (positive % 60).toString().padStart(2, '0')
}
