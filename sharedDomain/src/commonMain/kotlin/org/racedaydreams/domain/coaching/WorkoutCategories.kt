package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.WorkoutCategory

/** The original adaptive coach classification, shared without altering workout eligibility. */
object WorkoutCategories {
    @JvmStatic
    fun isQuality(category: WorkoutCategory?): Boolean {
        if (category == null) throw NullPointerException()
        return when (category) {
            WorkoutCategory.INTERVALS,
            WorkoutCategory.TEMPO_THRESHOLD_RUN,
            WorkoutCategory.RACE_PACE_RUN,
            WorkoutCategory.TIME_TRIAL,
            WorkoutCategory.PROGRESSION_RUN -> true
            else -> false
        }
    }

    @JvmStatic
    fun isDemanding(category: WorkoutCategory?): Boolean =
        isQuality(category) ||
            category == WorkoutCategory.LONG_RUN ||
            category == WorkoutCategory.RACE
}
