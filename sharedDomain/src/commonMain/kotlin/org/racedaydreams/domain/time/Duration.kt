package org.racedaydreams.domain.time

/**
 * Exact seconds/nanoseconds; JVM APIs still receive java.time.Duration, including fractional
 * values.
 */
expect class Duration {
    fun getSeconds(): Long

    fun getNano(): Int

    fun isNegative(): Boolean

    fun isZero(): Boolean

    override fun toString(): String
}

expect object Durations {
    fun ofSeconds(seconds: Long, nanoAdjustment: Long = 0): Duration

    fun ofMinutes(minutes: Long): Duration

    fun ofHours(hours: Long): Duration

    fun ofMillis(millis: Long): Duration

    fun ofNanos(nanos: Long): Duration
}

/** Canonical exact representation. No floating-point duration or invented missing time. */
internal data class DurationParts(val seconds: Long, val nanos: Int) {
    init {
        require(nanos in 0..999999999)
    }

    fun text(): String {
        if (seconds == 0L && nanos == 0) return "PT0S"
        val effective = if (seconds < 0 && nanos > 0) seconds + 1 else seconds
        val hours = effective / 3600
        val minutes = effective % 3600 / 60
        val second = effective % 60
        return buildString {
            append("PT")
            if (hours != 0L) append("${hours}H")
            if (minutes != 0L) append("${minutes}M")
            if (second != 0L || nanos != 0 || hours == 0L && minutes == 0L) {
                append(if (seconds < 0 && nanos > 0 && second == 0L) "-0" else second.toString())
                if (nanos != 0) {
                    val fraction = if (seconds < 0) 1000000000 - nanos else nanos
                    append('.').append(fraction.toString().padStart(9, '0').trimEnd('0'))
                }
                append('S')
            }
        }
    }

    companion object {
        fun ofSeconds(seconds: Long, nanoAdjustment: Long): DurationParts =
            DurationParts(
                checkedAdd(seconds, nanoAdjustment.floorDiv(1000000000)),
                nanoAdjustment.mod(1000000000).toInt(),
            )
    }
}

internal fun checkedAdd(a: Long, b: Long): Long {
    val result = a + b
    if (((a xor result) and (b xor result)) < 0) throw ArithmeticException("long overflow")
    return result
}

internal fun checkedScale(value: Long, scale: Long): Long {
    if (value < Long.MIN_VALUE / scale || value > Long.MAX_VALUE / scale)
        throw ArithmeticException("long overflow")
    return value * scale
}
