package org.racedaydreams.coach.application

import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.baseline.AssessmentMode
import org.racedaydreams.domain.baseline.AssessmentResult
import org.racedaydreams.domain.baseline.BaselineAssessment
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.ZoneOffset

/** Optional baseline and explicit assessment operations; imported evidence remains distinct. */
class SharedBaselineCoordinator(
    private val operations: CoachOperationRunner,
    private val baseline: BaselineRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val now: () -> Instant,
    private val newId: () -> String,
) {
    fun reviewBaseline(onSuccess: () -> Unit = {}) =
        operations.submit(onSuccess = onSuccess) {
            settings.putSetting("baseline.reviewed", "true")
        }

    fun saveManualBaseline(input: ManualBaselineInput, onSuccess: () -> Unit = {}) =
        operations.submit(
            "Manual baseline details saved. Imported observations remain available.",
            onSuccess,
        ) {
            baseline.saveManualBaseline(input)
        }

    fun saveAssessment(
        distanceKm: Int,
        duration: Duration,
        timing: () -> AssessmentTiming,
        bestEffort: Boolean,
        rpe: Double?,
        notes: String,
        onSuccess: () -> Unit = {},
    ) =
        operations.submit(
            "Assessment result saved as manually entered evidence. A single effort is not a complete fitness assessment.",
            onSuccess,
        ) {
            require(distanceKm in 1..5) { "Choose an assessment distance from 1K to 5K." }
            val captured = timing()
            require(!captured.start.isAfter(now())) {
                "A completed assessment cannot be in the future."
            }
            val end = captured.finish()
            require(Durations.between(captured.start, end.first) == duration) {
                "Assessment timing does not match its recorded duration."
            }
            require(!end.first.isAfter(now())) {
                "A completed assessment must end before the current time. Check its start time and duration."
            }
            val id = newId()
            val description =
                (if (bestEffort) "Best-effort" else "Controlled") +
                    " assessment. Manually entered. " +
                    notes
            val activity =
                ActivityRecord(
                    id,
                    SourceIdentity("assessment", id, null),
                    captured.start,
                    end.first,
                    captured.startOffset,
                    end.second,
                    distanceKm * 1_000.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    rpe,
                    description,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                )
            val result =
                AssessmentResult(
                    id,
                    BaselineAssessment(
                        distanceKm,
                        if (bestEffort) AssessmentMode.BEST_EFFORT else AssessmentMode.CONTROLLED,
                    ),
                    duration,
                    rpe,
                    end.first,
                    activity.id(),
                )
            baseline.saveAssessment(activity, result)
        }
}

/**
 * Real platform-zone conversion; finish stays lazy until the original future-start check passes.
 */
data class AssessmentTiming(
    val start: Instant,
    val startOffset: ZoneOffset,
    val finish: () -> Pair<Instant, ZoneOffset>,
)
