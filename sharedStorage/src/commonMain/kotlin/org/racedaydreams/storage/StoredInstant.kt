package org.racedaydreams.storage

import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants

/**
 * Exact canonical UTC representation written by the existing Android Instant.toString codecs.
 * Backup input validation/normalisation remains a separate responsibility.
 */
internal object StoredInstant {
    private val format =
        Regex(
            "([+-]?[0-9]{4,10})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\\.([0-9]{1,9}))?Z"
        )

    fun parse(text: String): Instant {
        val match =
            format.matchEntire(text)
                ?: throw IllegalArgumentException("Invalid stored UTC instant.")
        val year = match.groupValues[1].toLong()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        val hour = match.groupValues[4].toInt()
        val minute = match.groupValues[5].toInt()
        val second = match.groupValues[6].toInt()
        require(
            year in -1000000000L..1000000000L &&
                month in 1..12 &&
                day in 1..31 &&
                hour in 0..23 &&
                minute in 0..59 &&
                second in 0..59
        )
        val starts = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val leap = year % 4 == 0L && (year % 100 != 0L || year % 400 == 0L)
        val epochDay =
            365 * year + (year + 3).floorDiv(4) - (year + 99).floorDiv(100) +
                (year + 399).floorDiv(400) - 719528 + starts[month - 1] + day - 1 +
                if (month > 2 && leap) 1 else 0
        val nanos = match.groupValues[7].padEnd(9, '0').toLong()
        val result =
            Instants.ofEpochSecond(epochDay * 86400 + hour * 3600 + minute * 60 + second, nanos)
        require(result.toString() == text) { "Invalid or non-canonical stored UTC instant." }
        return result
    }
}
