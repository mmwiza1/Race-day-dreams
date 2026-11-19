package org.racedaydreams.storage

import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.Durations

/**
 * Canonical duration strings already written in activity/workout payloads. No floating-point time.
 */
internal object StoredDuration {
    private val format =
        Regex("PT(?:(-?[0-9]+)H)?(?:(-?[0-9]+)M)?(?:(-?[0-9]+)(?:\\.([0-9]{1,9}))?S)?")

    fun parse(text: String): Duration {
        val parts =
            format.matchEntire(text)?.groupValues
                ?: throw IllegalArgumentException("Invalid stored duration.")
        require(parts.drop(1).any { it.isNotEmpty() })
        val hours = parts[1].ifEmpty { "0" }.toLong()
        val minutes = parts[2].ifEmpty { "0" }.toLong()
        val seconds = parts[3].ifEmpty { "0" }.toLong()
        val total = add(add(scale(hours, 3600), scale(minutes, 60)), seconds)
        var nanos = parts[4].padEnd(9, '0').toLong()
        if (parts[3].startsWith('-')) nanos = -nanos
        val result = Durations.ofSeconds(total, nanos)
        require(result.toString() == text) { "Invalid or non-canonical stored duration." }
        return result
    }

    private fun add(a: Long, b: Long): Long {
        val result = a + b
        if (((a xor result) and (b xor result)) < 0) throw ArithmeticException("Duration overflow.")
        return result
    }

    private fun scale(value: Long, scale: Long): Long {
        if (value < Long.MIN_VALUE / scale || value > Long.MAX_VALUE / scale)
            throw ArithmeticException("Duration overflow.")
        return value * scale
    }
}
