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
internal fun coachingDecimalOne(value: Double): String =
    org.racedaydreams.domain.presentation.fixedDecimalText(value, 1)
