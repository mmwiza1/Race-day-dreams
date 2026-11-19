package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/**
 * Original optional recorded milestones; no streaks or claims about unretained lifetime history.
 */
object MilestoneEngine {
    @JvmStatic
    fun calculate(
        enabled: Boolean,
        activities: List<ActivityRecord>?,
        results: List<RecordedResult>?,
    ): List<Milestone> {
        if (!enabled) return emptyList()
        val milestones = mutableListOf<Milestone>()
        val firsts =
            listOf(
                PerformanceDistance.FIVE_K,
                PerformanceDistance.TEN_K,
                PerformanceDistance.HALF_MARATHON,
                PerformanceDistance.MARATHON,
            )
        for (distance in firsts) {
            var earliest: Milestone? = null
            for (activity in activities ?: throw NullPointerException()) {
                val metres = activity.distanceMetres()
                if (
                    metres == null ||
                        metres < distance.metres() ||
                        metres / ActivityChecks.seconds(activity.elapsedTime()) >
                            SampleSeries.MAX_SPEED_METRES_PER_SECOND ||
                        activity.samples().isNotEmpty() &&
                            SampleSeries(activity).quality == SampleQuality.REJECTED
                )
                    continue
                val date =
                    CalendarDates.atOffset(
                        activity.start(),
                        activity.startOffset() ?: Offsets.ofTotalSeconds(0),
                    )
                val candidate =
                    Milestone(
                        "first_" + distance.code(),
                        "First recorded " + distance.label(),
                        date,
                        activity.id(),
                        null,
                        "First qualifying activity in the app's retained history, not a claim about your lifetime running history.",
                    )
                if (earliest == null || date.toEpochDay() < earliest.date()!!.toEpochDay())
                    earliest = candidate
            }
            for (result in results ?: throw NullPointerException()) {
                if (result.distance().metres() < distance.metres()) continue
                if (earliest == null || result.date().toEpochDay() < earliest.date()!!.toEpochDay())
                    earliest =
                        Milestone(
                            "first_" + distance.code(),
                            "First recorded " + distance.label(),
                            result.date(),
                            result.activityId(),
                            result.id(),
                            "Based on a retained result and its stated provenance.",
                        )
            }
            earliest?.let { milestones.add(it) }
        }
        val recorded = results ?: throw NullPointerException()
        addThreshold(milestones, recorded, PerformanceDistance.FIVE_K, 30, "Sub-30 5K")
        addThreshold(milestones, recorded, PerformanceDistance.FIVE_K, 25, "Sub-25 5K")
        addThreshold(milestones, recorded, PerformanceDistance.TEN_K, 60, "Sub-60 10K")
        addThreshold(milestones, recorded, PerformanceDistance.TEN_K, 55, "Sub-55 10K")
        addThreshold(milestones, recorded, PerformanceDistance.TEN_K, 50, "Sub-50 10K")
        addThreshold(
            milestones,
            recorded,
            PerformanceDistance.HALF_MARATHON,
            120,
            "Sub-2-hour Half Marathon",
        )
        milestones.sortWith(compareBy<Milestone> { it.date()!!.toEpochDay() }.thenBy { it.code() })
        return Immutable.copyList(milestones)
    }

    private fun addThreshold(
        milestones: MutableList<Milestone>,
        results: List<RecordedResult>,
        distance: PerformanceDistance,
        minutes: Long,
        label: String,
    ) {
        var earliest: RecordedResult? = null
        for (result in results) {
            if (
                result.distance().code() != distance.code() ||
                    result.duration() >= Durations.ofMinutes(minutes)
            )
                continue
            val previous = earliest
            if (
                previous == null ||
                    result.date().toEpochDay() < previous.date().toEpochDay() ||
                    result.date() == previous.date() && result.id() < previous.id()
            )
                earliest = result
        }
        earliest?.let {
            milestones.add(
                Milestone(
                    "sub_${minutes}_${distance.code()}",
                    label,
                    it.date(),
                    it.activityId(),
                    it.id(),
                    "A recorded result met this threshold; its source is retained with the result.",
                )
            )
        }
    }
}
