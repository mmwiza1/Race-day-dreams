package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.floor
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.Durations

/** Original retained-activity validation and canonical time conversions. */
object ActivityChecks {
    @JvmStatic
    fun required(value: String?, name: String): String {
        require(value != null && value.trim { it <= ' ' }.isNotEmpty()) { "$name is required." }
        return value
    }

    @JvmStatic
    fun nonNegative(value: Double?, name: String) {
        require(value == null || value.isFinite() && value >= 0) {
            "$name must be finite and non-negative."
        }
    }

    @JvmStatic
    fun positive(value: Double?, name: String) {
        require(value == null || value.isFinite() && value > 0) {
            "$name must be positive when available."
        }
    }

    @JvmStatic
    fun duration(value: Duration?, name: String) {
        require(value == null || !value.isNegative()) { "$name cannot be negative." }
    }

    @JvmStatic
    fun <T> copy(values: List<T>?): List<T> {
        if (values == null) throw NullPointerException("A list is required.")
        for (value in values) if (value == null)
            throw NullPointerException("Null list entries are not supported.")
        return Immutable.copyList(values)
    }

    @JvmStatic
    fun seconds(duration: Duration): Double =
        duration.getSeconds() + duration.getNano() / 1000000000.0

    @JvmStatic
    fun duration(seconds: Double): Duration =
        Durations.ofNanos(roundedNanos(seconds * 1000000000.0))
}

/** Same JVM Math.round primitive, including non-finite and negative half/overflow behavior. */
internal fun roundedNanos(value: Double): Long {
    if (value.isNaN()) return 0
    if (value >= Long.MAX_VALUE.toDouble()) return Long.MAX_VALUE
    if (value <= Long.MIN_VALUE.toDouble()) return Long.MIN_VALUE
    val whole = floor(value)
    return whole.toLong() + if (value - whole >= 0.5) 1 else 0
}
