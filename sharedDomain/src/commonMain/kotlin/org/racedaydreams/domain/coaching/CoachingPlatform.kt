package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.time.*

/** Existing synchronous transaction action; JVM stays binary/source-compatible with Runnable. */
expect interface CoachingTransaction {
    fun run()
}

internal expect fun performanceDecimal(value: Double): String

/** JDK ordered DoubleStream.sum compensation; do not replace with a different training formula. */
internal fun coachingSum(values: Iterable<Double>): Double {
    var sum = 0.0
    var compensation = 0.0
    var simple = 0.0
    for (value in values) {
        simple += value
        val adjusted = value - compensation
        val next = sum + adjusted
        compensation = (next - sum) - adjusted
        sum = next
    }
    val result = sum - compensation
    return if (result.isNaN() && simple.isInfinite()) simple else result
}

internal fun coachingBefore(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() < b.toEpochDay()

internal fun coachingAfter(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() > b.toEpochDay()

internal fun coachingMonday(day: LocalDate): LocalDate =
    day.minusDays((day.getDayOfWeek().getValue() - 1).toLong())

internal fun coachingDate(run: org.racedaydreams.domain.activity.ActivityRecord): LocalDate =
    CalendarDates.atOffset(run.start(), run.startOffset() ?: Offsets.ofTotalSeconds(0))

/** Exact ISO calendar six-month lookback, including end-of-month clamping and leap years. */
internal fun coachingSixMonthsBefore(day: LocalDate): LocalDate {
    val index = day.getYear().toLong() * 12 + day.getMonthValue() - 1 - 6
    val year = index.floorDiv(12).toInt()
    val month = index.mod(12).toInt() + 1
    val length =
        when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4,
            6,
            9,
            11 -> 30
            else -> 31
        }
    return CalendarDates.of(year, month, minOf(day.getDayOfMonth(), length))
}

/** Decimal half-up rounding of the retained canonical value, for one-decimal UK labels on iOS. */
internal fun coachingDecimalOne(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
    val text = value.toString().lowercase()
    val negative = text.startsWith('-')
    val unsigned = text.removePrefix("-")
    val mantissa = unsigned.substringBefore('e')
    val exponent = unsigned.substringAfter('e', "0").toInt()
    val point = mantissa.indexOf('.').let { if (it < 0) mantissa.length else it } + exponent
    val digits = mantissa.replace(".", "")
    val cut = point + 1
    var rounded =
        when {
            cut < 0 -> "0"
            cut == 0 -> if (digits.first() >= '5') "1" else "0"
            cut >= digits.length -> digits.padEnd(cut, '0')
            else -> {
                val prefix = digits.take(cut).toCharArray()
                if (digits[cut] >= '5') {
                    var i = prefix.lastIndex
                    while (i >= 0 && prefix[i] == '9') {
                        prefix[i] = '0'
                        i--
                    }
                    if (i < 0)
                        return (if (negative) "-" else "") +
                            "1" +
                            "0".repeat(prefix.size - 1) +
                            ".0"
                    prefix[i]++
                }
                prefix.concatToString()
            }
        }
    rounded = rounded.trimStart('0').ifEmpty { "0" }.padStart(2, '0')
    return (if (negative) "-" else "") + rounded.dropLast(1) + "." + rounded.last()
}
