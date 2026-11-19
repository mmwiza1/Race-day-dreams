package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/** Original assignment-based calculation. The platform supplies today's local calendar date. */
object ShoeMileage {
    fun calculate(
        shoe: Shoe?,
        activities: List<ActivityRecord>?,
        localToday: LocalDate,
    ): ShoeMetrics {
        if (shoe == null) throw NullPointerException("Shoe")
        if (activities == null) throw NullPointerException("Activities")
        var recorded = 0.0
        var count = 0
        var known = 0
        var longest: Double? = null
        var first: Instant? = null
        for (activity in activities) {
            if (shoe.id() != activity.shoeId()) continue
            count++
            val start = activity.start()
            if (first == null || start.isBefore(first)) first = start
            val distance = activity.distanceMetres()
            if (distance != null) {
                recorded += distance
                longest = longest?.let { maxOf(it, distance) } ?: distance
                known++
            }
        }
        val total = shoe.startingDistanceMetres()?.let { recorded + it }
        val age =
            shoe.purchaseDate()?.let {
                if (it.toEpochDay() > localToday.toEpochDay()) null
                else localToday.toEpochDay() - it.toEpochDay()
            }
        val knownMinimum = total ?: recorded
        var guidance =
            when {
                knownMinimum >= 800000 ->
                    "Recorded use is substantial. Check cushioning, outsole wear and how the shoes feel regularly; consider replacement if their condition no longer suits you. Longevity varies by shoe, runner and surface."
                knownMinimum >= 600000 ->
                    "Pay closer attention to cushioning and outsole condition as mileage accumulates. There is no universal replacement distance; shoe longevity varies by model, runner and surface."
                knownMinimum >= 400000 ->
                    "Consider checking how the cushioning and outsole feel. Mileage is a prompt to inspect condition, not a hard replacement rule."
                else ->
                    "Check comfort, cushioning and outsole condition over time. Mileage alone does not determine when a shoe needs replacing."
            }
        if (known < count)
            guidance +=
                " Some assigned runs have no recorded distance, so known mileage is incomplete."
        if (total == null)
            guidance += " Earlier mileage was not supplied; only recorded use is known."
        return ShoeMetrics(
            shoe.id(),
            recorded,
            total,
            count,
            known,
            age,
            longest,
            if (known == 0) null else recorded / known,
            first,
            guidance,
        )
    }
}
