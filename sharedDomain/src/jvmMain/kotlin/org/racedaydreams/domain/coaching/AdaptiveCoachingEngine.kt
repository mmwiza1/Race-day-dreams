package org.racedaydreams.domain.coaching

import java.util.Optional
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.*

/** Original Java Optional API; every calculation lives in the single common engine. */
object AdaptiveCoachingEngine {
    @JvmStatic
    fun review(
        version: PlanVersion,
        actual: List<ActivityRecord>,
        responses: List<RunExecution>,
        strength: List<StrengthSession>,
        today: LocalDate,
        now: Instant,
    ): Optional<AdjustmentProposal> =
        Optional.ofNullable(
            AdaptiveCoaching.review(version, actual, responses, strength, today, now)
        )

    @JvmStatic
    fun isQuality(category: WorkoutCategory): Boolean = AdaptiveCoaching.isQuality(category)

    @JvmStatic
    fun isDemanding(category: WorkoutCategory): Boolean = AdaptiveCoaching.isDemanding(category)

    @JvmStatic
    fun apply(
        plan: TrainingPlan,
        proposal: AdjustmentProposal,
        option: AdjustmentProposal.Option,
        completed: Set<String>,
    ): TrainingPlan = AdaptiveCoaching.apply(plan, proposal, option, completed)
}
