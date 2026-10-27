package org.racedaydreams.coach.application

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.baseline.AssessmentMode
import org.racedaydreams.domain.baseline.AssessmentResult
import org.racedaydreams.domain.baseline.BaselineAssessment
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.baseline.ManualBaselineInput

/** Optional baseline and explicit assessment operations; imported evidence remains distinct. */
class BaselineCoordinator(
    private val operations: CoachOperationRunner,
    private val baseline: BaselineRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
) {
    fun reviewBaseline(onSuccess: () -> Unit = {}) = operations.submit(onSuccess = onSuccess) {
        settings.putSetting("baseline.reviewed", "true")
    }

    fun saveManualBaseline(input: ManualBaselineInput, onSuccess: () -> Unit = {}) = operations.submit("Manual baseline details saved. Imported observations remain available.", onSuccess) {
        baseline.saveManualBaseline(input)
    }

    fun saveAssessment(distanceKm: Int, duration: Duration, date: LocalDate, time: LocalTime,
                       bestEffort: Boolean, rpe: Double?, notes: String, onSuccess: () -> Unit = {}) = operations.submit("Assessment result saved as manually entered evidence. A single effort is not a complete fitness assessment.", onSuccess) {
        require(distanceKm in 1..5) { "Choose an assessment distance from 1K to 5K." }
        val start = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault())
        require(!start.toInstant().isAfter(Instant.now())) { "A completed assessment cannot be in the future." }
        require(!start.toInstant().plus(duration).isAfter(Instant.now())) { "A completed assessment must end before the current time. Check its start time and duration." }
        val id = UUID.randomUUID().toString()
        val description = (if (bestEffort) "Best-effort" else "Controlled") + " assessment. Manually entered. " + notes
        val activity = ActivityRecord(
            id, SourceIdentity("assessment", id, null), start.toInstant(), start.toInstant().plus(duration),
            start.offset, start.plus(duration).offset, distanceKm * 1_000.0, null, null, null,
            null, null, rpe, description, emptyList(), emptyList(), null, null
        )
        val result = AssessmentResult(id, BaselineAssessment(distanceKm,
            if (bestEffort) AssessmentMode.BEST_EFFORT else AssessmentMode.CONTROLLED), duration, rpe,
            start.toInstant().plus(duration), activity.id())
        baseline.saveAssessment(activity, result)
    }

}
