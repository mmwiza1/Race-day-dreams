package org.racedaydreams.storage

import org.racedaydreams.domain.time.Instant

internal fun sqlRequired(value: String, field: String): String {
    require(value.length <= 256 && value.any { it.code > 0x20 }) { "Invalid $field." }
    return value
}

internal fun sqlText(value: String?): SqlValue = value?.let { SqlValue.Text(it) } ?: SqlValue.Null

internal fun sqlNumber(value: Double?): SqlValue = value?.let { SqlValue.Real(it) } ?: SqlValue.Null

internal fun SqlValue.sqlText(): String = (this as SqlValue.Text).value

internal fun SqlValue.nullableSqlText(): String? = if (this == SqlValue.Null) null else sqlText()

internal fun SqlValue.sqlLong(): Long = (this as SqlValue.Integer).value

internal fun SqlValue.nullableSqlNumber(): Double? =
    when (this) {
        SqlValue.Null -> null
        is SqlValue.Real -> value
        is SqlValue.Integer -> value.toDouble()
        else -> throw IllegalArgumentException("Invalid stored number; data was retained.")
    }

/** Exact java.time.Instant.toEpochMilli arithmetic, including negative fractions and overflow. */
internal fun storedEpochMillis(at: Instant): Long {
    val seconds = at.getEpochSecond()
    val nanos = at.getNano()
    val value = if (seconds < 0 && nanos > 0) seconds + 1 else seconds
    if (value < Long.MIN_VALUE / 1000 || value > Long.MAX_VALUE / 1000)
        throw ArithmeticException("Timestamp overflow.")
    val scaled = value * 1000
    val fraction = if (seconds < 0 && nanos > 0) nanos / 1000000L - 1000 else nanos / 1000000L
    val result = scaled + fraction
    if (((scaled xor result) and (fraction xor result)) < 0)
        throw ArithmeticException("Timestamp overflow.")
    return result
}
