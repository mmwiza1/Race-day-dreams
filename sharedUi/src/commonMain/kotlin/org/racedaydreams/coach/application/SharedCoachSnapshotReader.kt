package org.racedaydreams.coach.application

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.activity.ResultRepository
import org.racedaydreams.domain.activity.ShoeRepository
import org.racedaydreams.domain.baseline.BaselineEngine
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.baseline.PerformanceEligibilityEvaluator
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.coaching.AdaptiveCoaching
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.time.*
import org.racedaydreams.health.HealthAvailability

/** Coherent cross-feature projection. Reads/derived proposals never accept a plan change. */
class SharedCoachSnapshotReader(
    private val activitiesRepository: ActivityRepository,
    private val resultsRepository: ResultRepository,
    private val shoesRepository: ShoeRepository,
    private val baselineRepository: BaselineRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val planned: PlannedWorkoutRepository,
    private val sources: CoachSnapshotSources,
    private val coaching: CoachingRepository,
    private val savedDraft: () -> String?,
    private val programming: org.racedaydreams.domain.strength.StrengthProgrammingRepository? =
        null,
    private val now: () -> Instant,
    private val today: () -> LocalDate,
    private val ioDispatcher: CoroutineDispatcher = applicationIoDispatcher(),
) {
    suspend fun read(): CoachUiState {
        val permissions =
            if (sources.healthAvailability() == HealthAvailability.AVAILABLE) {
                try {
                    sources.grantedHealthPermissions()
                } catch (_: Exception) {
                    emptySet()
                }
            } else emptySet()
        val snapshot =
            withContext(ioDispatcher) {
                val profile = sources.profile()
                val activities = activitiesRepository.coachingActivities(today())
                val results = resultsRepository.results()
                val manual = baselineRepository.savedManualBaseline()
                val now = now()
                val healthWindow = sources.importWindow()
                val lookback =
                    Instants.ofEpochSecond(
                        now.getEpochSecond() - 84 * 86400L,
                        now.getNano().toLong(),
                    )
                val from = maxOf(healthWindow?.first ?: lookback, lookback)
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
                    AdaptiveCoaching.review(
                            currentPlan,
                            activities,
                            responses,
                            strength,
                            today(),
                            now,
                        )
                        ?.let { proposal ->
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
                            today(),
                            sources.interruption(),
                            sources.sessionDecisions(),
                            baseline.confidence(),
                            baselineRepository.assessments(),
                        )
                    }
                if (review != null && !sources.reviewKnown(requireNotNull(review.id())))
                    sources.saveReview(review)
                sources.reconcileReminders()
                val programmeReference = programming?.currentReference()
                val authoredGymProgramme =
                    if (
                        programmeReference?.owner() ==
                            org.racedaydreams.domain.strength.StrengthProgrammingRepository.Owner
                                .AUTHORED
                    )
                        programming?.programme(
                            programmeReference.id(),
                            programmeReference.revision(),
                        ) ?: error("The current authored programme is missing.")
                    else null
                val gymProgramme =
                    if (authoredGymProgramme == null) sources.gymProgramme() else null
                val gymLinks = sources.gymLinks()
                val associations = gymLinks.values.toList()
                val strengthToday =
                    if (authoredGymProgramme != null)
                        org.racedaydreams.domain.presentation.StrengthTodaySummary.fromAuthored(
                            authoredGymProgramme,
                            strength,
                            associations,
                            today(),
                            now,
                        )
                    else
                        org.racedaydreams.domain.presentation.StrengthTodaySummary.from(
                            gymProgramme,
                            strength,
                            associations,
                            today(),
                            now,
                        )
                CoachUiState(
                    loading = false,
                    startingPath = settings.getSetting("starting.path"),
                    primaryFocus = sources.primaryFocus(),
                    shoeDiscomfort = sources.shoeDiscomfort(),
                    profile = profile,
                    activities = activities,
                    results = results,
                    shoes = shoesRepository.shoes(),
                    assessments = baselineRepository.assessments(),
                    baseline = baseline,
                    manualBaseline = manual,
                    currentPlan = currentPlan,
                    planVersions = history,
                    weights = sources.weights(),
                    historicalStrength = sources.historicalStrength(),
                    massUnit =
                        runCatching { MassUnit.valueOf(settings.getSetting("mass.unit") ?: "KG") }
                            .getOrDefault(MassUnit.KG),
                    gymLinks = gymLinks,
                    gymProgressionDecisions = sources.gymDecisions(),
                    gymProgramme = gymProgramme,
                    authoredGymProgramme = authoredGymProgramme,
                    strengthToday = strengthToday,
                    gymVersions = sources.gymVersions(),
                    exerciseChoices = sources.exerciseChoices(),
                    exerciseExclusions = sources.exerciseExclusions(),
                    acknowledgedPbs = sources.acknowledgedPbs(),
                    calendarView = settings.getSetting("calendar.view") ?: "List",
                    workoutDecisions = sources.sessionDecisions(),
                    weeklyReview = review,
                    weeklyDecision = review?.let { sources.decisionForReview(it) },
                    proposals =
                        coaching.proposals().filter { it.baseVersionId() == currentPlan?.id() },
                    strengthPlannedDates = sources.strengthLinks(),
                    strengthSessions = strength,
                    exercises = coaching.exercises(),
                    runResponses = responses,
                    plannedWorkouts = planned.plannedWorkouts(),
                    milestonesEnabled = settings.getSetting("milestones.enabled") == "true",
                    reminderPreferences = sources.reminderPreferences(),
                    performanceEligible = eligibility.eligible(),
                    eligibilityReasons = eligibility.reasons(),
                    baselineReviewed = settings.getSetting("baseline.reviewed") == "true",
                    draft = savedDraft() ?: sources.profileDraft(),
                    healthAvailability = sources.healthAvailability(),
                    healthPermissions = permissions,
                )
            }
        return snapshot
    }
}
