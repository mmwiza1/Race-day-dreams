package org.racedaydreams.domain.time

/** Exact retained observation time. JVM actual is the original java.time.Instant. */
expect class Instant : Comparable<Instant> {
    fun getEpochSecond(): Long

    fun getNano(): Int

    fun isAfter(other: Instant): Boolean

    fun isBefore(other: Instant): Boolean

    override fun compareTo(other: Instant): Int

    override fun toString(): String
}

expect object Instants {
    fun ofEpochSecond(seconds: Long, nanoAdjustment: Long = 0): Instant
}

/**
 * Full original instant range; native kotlinx-datetime factories clamp outside their smaller range.
 */
internal data class InstantParts(val seconds: Long, val nanos: Int) {
    init {
        require(seconds in MIN_SECOND..MAX_SECOND && nanos in 0..999999999) {
            "Instant exceeds supported bounds."
        }
    }

    fun text(): String {
        val date = IsoCalendar.parts(seconds.floorDiv(86400L))
        val time = seconds.mod(86400L)
        val hour = (time / 3600).toString().padStart(2, '0')
        val minute = (time % 3600 / 60).toString().padStart(2, '0')
        val second = (time % 60).toString().padStart(2, '0')
        val fraction =
            if (nanos == 0) ""
            else {
                val width = if (nanos % 1000000 == 0) 3 else if (nanos % 1000 == 0) 6 else 9
                "." + nanos.toString().padStart(9, '0').take(width)
            }
        return "${IsoCalendar.text(date.year, date.month, date.day)}T$hour:$minute:$second${fraction}Z"
    }

    companion object {
        const val MIN_SECOND = -31557014167219200L
        const val MAX_SECOND = 31556889864403199L

        fun ofEpochSecond(seconds: Long, adjustment: Long): InstantParts {
            val normalized = DurationParts.ofSeconds(seconds, adjustment)
            return InstantParts(normalized.seconds, normalized.nanos)
        }
    }
}
