package org.racedaydreams.coach.application

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.Release08Store
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.data.StrengthUserStore
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.activity.ResultRepository
import org.racedaydreams.domain.activity.ShoeRepository
import org.racedaydreams.domain.baseline.BaselineEngine
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.baseline.PerformanceEligibilityEvaluator
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.coaching.AdaptiveCoachingEngine
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.health.HealthConnectGateway
import org.racedaydreams.reminders.EveningReminders

/** Coherent cross-feature projection. Reads/derived proposals never accept a plan change. */
class CoachSnapshotReader(
    private val activitiesRepository: ActivityRepository,
    private val resultsRepository: ResultRepository,
    private val shoesRepository: ShoeRepository,
    private val baselineRepository: BaselineRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val planned: PlannedWorkoutRepository,
    private val release: Release08Store,
    private val gym: GymProgrammeStore,
    private val choices: StrengthUserStore,
    private val coaching: CoachingRepository,
    private val development: DevelopmentStore,
    private val profiles: SqliteUserPreferencesRepository,
    private val reminders: EveningReminders,
    private val health: HealthConnectGateway,
    private val savedDraft: () -> String?,
) {
    suspend fun read(): CoachUiState {
        val permissions =
            if (health.availability() == HealthAvailability.AVAILABLE) {
                try {
                    health.grantedPermissions()
                } catch (_: Exception) {
                    emptySet()
                }
            } else emptySet()
        val snapshot =
            withContext(Dispatchers.IO) {
                val profile = profiles.load().orElse(null)
                val activities = activitiesRepository.coachingActivities(LocalDate.now())
                val results = resultsRepository.results()
                val manual = baselineRepository.savedManualBaseline()
                val now = Instant.now()
                val healthWindow =
                    HealthConnectGateway.importWindow(
                        activitiesRepository.importToken(HealthConnectGateway.PROVIDER)
                    )
                val from =
                    maxOf(
                        healthWindow?.first ?: now.minus(Duration.ofDays(84)),
                        now.minus(Duration.ofDays(84)),
                    )
                val derived = BaselineEngine.derive(activities, results, from, now)
                val baseline =
                    BaselineEngine.applyManualOverlay(
                        derived,
                        manual,
                        baselineRepository.manualBaselineRecordedAt(),
                    )
                val eligibility =
                    PerformanceEligibilityEvaluator.evaluate(
                        baseline,
                        now,
                        baselineRepository.manualBaselineRecordedAt(),
                    )
                val currentPlan = coaching.current()
                val strength = coaching.strength()
                val responses = coaching.responses()
                if (currentPlan != null) {
                    AdaptiveCoachingEngine.review(
                            currentPlan,
                            activities,
                            responses,
                            strength,
                            LocalDate.now(),
                            now,
                        )
                        .ifPresent { proposal ->
                            if (!coaching.proposalKnown(proposal.id()))
                                coaching.saveProposal(proposal)
                        }
                }
                val history = coaching.versions()
                val review =
                    currentPlan?.let {
                        WeeklyCoaching.review(
                            it,
                            history,
                            activities,
                            responses,
                            strength,
                            results,
                            LocalDate.now(),
                            development.interruption(),
                            development.sessionDecisions(),
                            baseline.confidence(),
                            baselineRepository.assessments(),
                        )
                    }
                if (review != null && !development.reviewKnown(review.id()))
                    development.saveReview(review)
                reminders.reconcile()
                CoachUiState(
                    loading = false,
                    startingPath = settings.getSetting("starting.path"),
                    shoeDiscomfort =
                        release.get("config", "shoe-feedback")?.getJSONArray("activities")?.let { a
                            ->
                            (0 until a.length()).map { a.getString(it) }.toSet()
                        } ?: emptySet(),
                    profile = profile,
                    activities = activities,
                    results = results,
                    shoes = shoesRepository.shoes(),
                    assessments = baselineRepository.assessments(),
                    baseline = baseline,
                    manualBaseline = manual,
                    currentPlan = currentPlan,
                    planVersions = history,
                    weights = development.weights(),
                    historicalStrength = development.historical(),
                    massUnit =
                        runCatching { MassUnit.valueOf(settings.getSetting("mass.unit") ?: "KG") }
                            .getOrDefault(MassUnit.KG),
                    gymLinks = development.gymLinks(),
                    gymProgressionDecisions = gym.decisions(),
                    gymProgramme = gym.current(),
                    gymVersions = gym.versions(),
                    exerciseChoices = choices.choices(),
                    acknowledgedPbs = choices.acknowledged(),
                    calendarView = settings.getSetting("calendar.view") ?: "List",
                    workoutDecisions = development.sessionDecisions(),
                    weeklyReview = review,
                    weeklyDecision = review?.let { development.decisionForReview(it) },
                    proposals =
                        coaching.proposals().filter { it.baseVersionId() == currentPlan?.id() },
                    strengthPlannedDates = development.strengthLinks(),
                    strengthSessions = strength,
                    exercises = coaching.exercises(),
                    runResponses = responses,
                    plannedWorkouts = planned.plannedWorkouts(),
                    milestonesEnabled = settings.getSetting("milestones.enabled") == "true",
                    reminderPreferences = reminders.preferences(),
                    performanceEligible = eligibility.eligible(),
                    eligibilityReasons = eligibility.reasons(),
                    baselineReviewed = settings.getSetting("baseline.reviewed") == "true",
                    draft = savedDraft() ?: profiles.loadDraft().orElse(null),
                    healthAvailability = health.availability(),
                    healthPermissions = permissions,
                )
            }
        return snapshot
    }
}
