package org.racedaydreams.coach.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.*
import org.racedaydreams.coach.CoachTextInput
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.coach.SharedAssessmentScreen
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.domain.time.Offsets
import org.racedaydreams.recording.AppleRecordingClock

@Composable
internal fun AppleAssessmentScreen(
    session: AppleCoachSession<*>,
    state: CoachUiState,
    onDone: () -> Unit,
) {
    val zone = TimeZone.currentSystemDefault()
    val defaultStart =
        remember(zone) {
            Instant.fromEpochSeconds(AppleRecordingClock.now().getEpochSecond() - 3600)
                .toLocalDateTime(zone)
        }
    SharedAssessmentScreen(
        state,
        defaultStart.date.toString(),
        defaultStart.time.toString().take(5),
        dateField = { value, change ->
            CoachTextInput("Completed run date (YYYY-MM-DD)", value, change)
        },
        save = { input ->
            val time = LocalDateTime(LocalDate.parse(input.date), LocalTime.parse(input.time))
            val start = time.toInstant(zone)
            require(start.toLocalDateTime(zone) == time) {
                "Choose a start time that exists in your device time zone."
            }
            session.services.baseline.saveAssessment(
                input.distance,
                input.duration,
                {
                    val startedAt =
                        Instants.ofEpochSecond(
                            start.epochSeconds,
                            start.nanosecondsOfSecond.toLong(),
                        )
                    AssessmentTiming(
                        startedAt,
                        Offsets.ofTotalSeconds(zone.offsetAt(start).totalSeconds),
                    ) {
                        val end =
                            Instant.fromEpochSeconds(
                                start.epochSeconds + input.duration.getSeconds(),
                                start.nanosecondsOfSecond.toLong() + input.duration.getNano(),
                            )
                        Instants.ofEpochSecond(
                            end.epochSeconds,
                            end.nanosecondsOfSecond.toLong(),
                        ) to Offsets.ofTotalSeconds(zone.offsetAt(end).totalSeconds)
                    }
                },
                input.bestEffort,
                input.rpe,
                input.notes,
                onDone,
            )
        },
        onDone = onDone,
        saveError = state.error,
    )
}
