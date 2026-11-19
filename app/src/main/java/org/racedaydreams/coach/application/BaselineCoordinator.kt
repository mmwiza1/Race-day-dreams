package org.racedaydreams.coach.application

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.settings.SettingsRepository

/** Original Android timezone/identity binding; baseline application rules are common. */
class BaselineCoordinator internal constructor(private val shared: SharedBaselineCoordinator) {
    constructor(
        operations: CoachOperationRunner,
        baseline: BaselineRepository,
        settings: SettingsRepository,
    ) : this(
        SharedBaselineCoordinator(
            operations,
            baseline,
            settings,
            Instant::now,
            { UUID.randomUUID().toString() },
        )
    )

    fun reviewBaseline(onSuccess: () -> Unit = {}) = shared.reviewBaseline(onSuccess)

    fun saveManualBaseline(input: ManualBaselineInput, onSuccess: () -> Unit = {}) =
        shared.saveManualBaseline(input, onSuccess)

    fun saveAssessment(
        distanceKm: Int,
        duration: Duration,
        date: LocalDate,
        time: LocalTime,
        bestEffort: Boolean,
        rpe: Double?,
        notes: String,
        onSuccess: () -> Unit = {},
    ) =
        shared.saveAssessment(
            distanceKm,
            duration,
            {
                val start = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault())
                AssessmentTiming(start.toInstant(), start.offset) {
                    val end = start.plus(duration)
                    end.toInstant() to end.offset
                }
            },
            bestEffort,
            rpe,
            notes,
            onSuccess,
        )
}
