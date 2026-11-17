package org.racedaydreams.coach.application

import androidx.compose.runtime.*
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.strength.SharedAchievementsScreen
import org.racedaydreams.coach.strength.SharedRecognizedMilestone
import org.racedaydreams.domain.achievements.TrainingMilestones
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.reminders.KotlinxReminderCalendar

@Composable
internal fun AppleAchievements(
    session: AppleCoachSession<*>,
    back: () -> Unit,
    openRun: (String) -> Unit,
    openGym: (String) -> Unit,
) {
    val model = session.achievements
    val state by model.state.collectAsState()
    LaunchedEffect(model) { model.refresh() }
    val zone = TimeZone.currentSystemDefault().id
    SharedAchievementsScreen(
        loading = state.loading,
        awards =
            state.awards.map {
                SharedRecognizedMilestone(
                    it.award,
                    AppleCalendarDates.label(KotlinxReminderCalendar.at(it.recognizedAt, zone).date),
                )
            },
        celebrations = state.celebrations,
        availableSources = state.availableSources,
        notice = state.notice,
        error = state.error,
        sourceDateLabel = { proof ->
            val date =
                proof.offset()?.let { CalendarDates.atOffset(proof.at(), it) }
                    ?: KotlinxReminderCalendar.at(proof.at(), zone).date
            AppleCalendarDates.label(date)
        },
        onSource = { proof ->
            when (proof.source()) {
                TrainingMilestones.Source.RUN -> openRun(proof.recordId())
                TrainingMilestones.Source.GYM -> openGym(proof.recordId())
            }
        },
        onBack = back,
        onDismiss = model::dismiss,
        onRefresh = model::refresh,
    )
}
