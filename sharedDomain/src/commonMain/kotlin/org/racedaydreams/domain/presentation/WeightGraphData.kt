package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.coaching.coachingReadOnlyList
import org.racedaydreams.domain.time.*

/** Retained measurements only; connecting lines and axis padding never add observations. */
data class WeightGraphData(
    private val points: List<BodyWeight>?,
    private val minimum: Double,
    private val maximum: Double,
    private val unit: MassUnit?,
) {
    fun points(): List<BodyWeight>? = points

    fun minimum(): Double = minimum

    fun maximum(): Double = maximum

    fun unit(): MassUnit? = unit

    override fun toString(): String =
        "WeightGraphData[points=$points, minimum=$minimum, maximum=$maximum, unit=$unit]"

    fun x(i: Int): Double {
        val p = points!!
        if (p.size < 2) return 0.5
        val span = graphMillis(p.last().at()) - graphMillis(p.first().at())
        return if (span == 0L) 0.5
        else (graphMillis(p[i].at()) - graphMillis(p.first().at())).toDouble() / span
    }

    fun y(i: Int): Double =
        1 - (unit!!.fromKilograms(points!![i].kilograms()) - minimum) / (maximum - minimum)

    fun nearest(x: Double, y: Double): BodyWeight? {
        val p = points!!
        if (p.isEmpty()) return null
        var best = 0
        for (i in 1 until p.size) if (
            (x(i) - x).pow(2) + (y(i) - y).pow(2) < (x(best) - x).pow(2) + (y(best) - y).pow(2)
        )
            best = i
        return p[best]
    }

    fun nearest(fraction: Double): BodyWeight? {
        val p = points!!
        if (p.isEmpty()) return null
        var best = 0
        for (i in 1 until p.size) if (abs(x(i) - fraction) < abs(x(best) - fraction)) best = i
        return p[best]
    }

    companion object {
        @JvmStatic
        fun of(
            records: List<BodyWeight>,
            today: LocalDate,
            range: String,
            unit: MassUnit,
        ): WeightGraphData {
            val first =
                when (range) {
                    "Week" -> today.minusDays(7)
                    "Month" -> graphMonthsBefore(today, 1)
                    "3 Months" -> graphMonthsBefore(today, 3)
                    "Year" -> graphMonthsBefore(today, 12)
                    else -> CalendarDates.of(-999999999, 1, 1)
                }
            val points =
                records
                    .filter {
                        val date = CalendarDates.atOffset(it.at(), it.offset())
                        date.toEpochDay() >= first.toEpochDay() &&
                            date.toEpochDay() <= today.toEpochDay()
                    }
                    .sortedWith(compareBy<BodyWeight> { it.at() }.thenBy { it.id() })
            val minimum = points.minOfOrNull { unit.fromKilograms(it.kilograms()) } ?: 0.0
            val maximum = points.maxOfOrNull { unit.fromKilograms(it.kilograms()) } ?: 0.0
            val pad = max(if (unit == MassUnit.KG) 0.5 else 1.0, (maximum - minimum) * 0.15)
            return WeightGraphData(coachingReadOnlyList(points), minimum - pad, maximum + pad, unit)
        }
    }
}

/** JDK epoch-millisecond floor and overflow behaviour, including the negative endpoint. */
internal fun graphMillis(at: Instant): Long {
    val seconds = at.getEpochSecond()
    return if (seconds < 0 && at.getNano() > 0)
        checkedAdd(checkedScale(seconds + 1, 1000), at.getNano() / 1000000L - 1000)
    else checkedAdd(checkedScale(seconds, 1000), at.getNano() / 1000000L)
}

private fun graphMonthsBefore(day: LocalDate, count: Int): LocalDate {
    val index = day.getYear().toLong() * 12 + day.getMonthValue() - 1 - count
    val year = index.floorDiv(12).toInt()
    val month = index.mod(12).toInt() + 1
    val length =
        when (month) {
            2 -> if (IsoCalendar.leap(year)) 29 else 28
            4,
            6,
            9,
            11 -> 30
            else -> 31
        }
    return CalendarDates.of(year, month, min(day.getDayOfMonth(), length))
}
