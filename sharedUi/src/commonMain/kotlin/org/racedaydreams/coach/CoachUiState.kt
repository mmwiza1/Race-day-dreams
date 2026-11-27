package org.racedaydreams.coach

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.baseline.AssessmentResult
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.coaching.AdjustmentProposal
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.domain.coaching.RunExecution
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.coaching.WorkoutDecision
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.health.HealthAvailability

/** Presentation state contains actual local observations; unknown metrics stay nullable. */
data class CoachUiState(
    val loading: Boolean = true,
    val startingPath: String? = null,
    val primaryFocus: PrimaryFocus? = null,
    val shoeDiscomfort: Set<String> = emptySet(),
    val busy: Boolean = false,
    val profile: UserProfile? = null,
    val activities: List<ActivityRecord> = emptyList(),
    val results: List<RecordedResult> = emptyList(),
    val shoes: List<Shoe> = emptyList(),
    val assessments: List<AssessmentResult> = emptyList(),
    val plannedWorkouts: List<PlannedWorkout> = emptyList(),
    val currentPlan: PlanVersion? = null,
    val planVersions: List<PlanVersion> = emptyList(),
    val proposals: List<AdjustmentProposal> = emptyList(),
    val strengthPlannedDates: Map<String, LocalDate> = emptyMap(),
    val strengthSessions: List<StrengthSession> = emptyList(),
    val exercises: List<StrengthExercise> = StrengthExercise.starters(),
    val runResponses: List<RunExecution> = emptyList(),
    val weights: List<BodyWeight> = emptyList(),
    val historicalStrength: List<HistoricalStrengthRecord> = emptyList(),
    val massUnit: MassUnit = MassUnit.KG,
    val calendarView: String = "List",
    val gymLinks:
        Map<String, org.racedaydreams.domain.presentation.StrengthTodaySummary.Association> =
        emptyMap(),
    val gymProgressionDecisions:
        List<org.racedaydreams.storage.LegacyGymProgrammePayloadJson.Decision> =
        emptyList(),
    val gymProgramme: org.racedaydreams.domain.strength.GymProgramme? = null,
    val authoredGymProgramme: org.racedaydreams.domain.strength.StrengthProgramme? = null,
    val strengthToday: org.racedaydreams.domain.presentation.StrengthTodaySummary? = null,
    val gymVersions: List<org.racedaydreams.domain.strength.GymProgramme> = emptyList(),
    val exerciseExclusions:
        List<org.racedaydreams.domain.strength.ExerciseExclusionRepository.Exclusion> =
        emptyList(),
    val exerciseChoices: List<org.racedaydreams.storage.SqliteStrengthChoices.Choice> = emptyList(),
    val acknowledgedPbs: Set<String> = emptySet(),
    val workoutDecisions: List<WorkoutDecision> = emptyList(),
    val weeklyReview: WeeklyCoaching.Review? = null,
    val weeklyDecision: String? = null,
    val milestonesEnabled: Boolean = false,
    val reminderPreferences: ReminderSettings =
        ReminderSettings.defaults(TimeZone.currentSystemDefault().id),
    val baseline: RunnerBaseline = emptyPresentationBaseline(),
    val manualBaseline: ManualBaselineInput = ManualBaselineInput.empty(),
    val performanceEligible: Boolean = false,
    val eligibilityReasons: List<String> = emptyList(),
    val baselineReviewed: Boolean = false,
    val draft: String? = null,
    val healthAvailability: HealthAvailability = HealthAvailability.UNAVAILABLE,
    val healthPermissions: Set<String> = emptySet(),
    val message: String? = null,
    val error: String? = null,
    val savedProfileRevision: Int = 0,
    val disciplineColours:
        Map<
            org.racedaydreams.domain.settings.ColourDiscipline,
            org.racedaydreams.domain.settings.AccentColour,
        > =
        emptyMap(),
)

// Preserve the original empty 84-day presentation window. This is not recorded health evidence.
private fun emptyPresentationBaseline(): RunnerBaseline {
    val from = Clock.System.now()
    val until = Clock.System.now()
    return RunnerBaseline.empty(
        Instants.ofEpochSecond(from.epochSeconds - 84 * 86400L, from.nanosecondsOfSecond.toLong()),
        Instants.ofEpochSecond(until.epochSeconds, until.nanosecondsOfSecond.toLong()),
    )
}
