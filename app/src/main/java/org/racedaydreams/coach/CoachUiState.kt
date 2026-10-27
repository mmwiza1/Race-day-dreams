package org.racedaydreams.coach

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.baseline.AssessmentResult
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.coaching.AdjustmentProposal
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.domain.coaching.RunExecution
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.coaching.WorkoutDecision
import org.racedaydreams.domain.reminders.ReminderPreferences
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.health.HealthAvailability

/** Presentation state contains actual local observations; unknown metrics stay nullable. */
data class CoachUiState(
    val loading: Boolean = true,
    val startingPath: String? = null,
    val shoeDiscomfort:Set<String> = emptySet(),
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
    val strengthPlannedDates: Map<String,LocalDate> = emptyMap(),
    val strengthSessions: List<StrengthSession> = emptyList(),
    val exercises: List<StrengthExercise> = StrengthExercise.starters(),
    val runResponses: List<RunExecution> = emptyList(),
    val weights: List<BodyWeight> = emptyList(),
    val historicalStrength: List<HistoricalStrengthRecord> = emptyList(),
    val massUnit: MassUnit = MassUnit.KG,
    val calendarView: String = "List",
    val gymLinks: Map<String,org.racedaydreams.data.DevelopmentStore.GymLink> = emptyMap(),
    val gymProgressionDecisions: List<org.racedaydreams.data.GymProgrammeStore.Decision> = emptyList(),
    val gymProgramme: org.racedaydreams.domain.strength.GymProgramme? = null,
    val gymVersions: List<org.racedaydreams.domain.strength.GymProgramme> = emptyList(),
    val exerciseChoices: List<org.racedaydreams.data.StrengthUserStore.Choice> = emptyList(),
    val acknowledgedPbs: Set<String> = emptySet(),
    val workoutDecisions: List<WorkoutDecision> = emptyList(),
    val weeklyReview: WeeklyCoaching.Review? = null,
    val weeklyDecision: String? = null,
    val milestonesEnabled: Boolean = false,
    val reminderPreferences: ReminderPreferences = ReminderPreferences.defaults(ZoneId.systemDefault()),
    val baseline: RunnerBaseline = RunnerBaseline.empty(Instant.now().minus(Duration.ofDays(84)), Instant.now()),
    val manualBaseline: ManualBaselineInput = ManualBaselineInput.empty(),
    val performanceEligible: Boolean = false,
    val eligibilityReasons: List<String> = emptyList(),
    val baselineReviewed: Boolean = false,
    val draft: String? = null,
    val healthAvailability: HealthAvailability = HealthAvailability.UNAVAILABLE,
    val healthPermissions: Set<String> = emptySet(),
    val message: String? = null,
    val error: String? = null,
    val savedProfileRevision: Int = 0
)

