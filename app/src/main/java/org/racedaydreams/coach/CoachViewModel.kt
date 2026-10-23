package org.racedaydreams.coach

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.baseline.BaselineEngine
import org.racedaydreams.domain.baseline.AssessmentMode
import org.racedaydreams.domain.baseline.AssessmentResult
import org.racedaydreams.domain.baseline.BaselineAssessment
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.baseline.PerformanceEligibilityEvaluator
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.health.HealthConnectGateway
import org.racedaydreams.portability.BackupManager
import org.racedaydreams.domain.reminders.ReminderPreferences
import org.racedaydreams.reminders.EveningReminders

/** Presentation state contains actual local observations; unknown metrics stay nullable. */
data class CoachUiState(
    val loading: Boolean = true,
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

/** UI state and IO coordination only: analysis and eligibility belong to the domain. */
class CoachViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val app = application
    private val development = DevelopmentStore(application)
    private val store = FeatureStore(application)
    private val coaching: CoachingRepository = CoachingStore(application)
    private val profiles = SqliteUserPreferencesRepository(application)
    private val reminders = EveningReminders(application, store)
    val health = HealthConnectGateway(application)
    private val ioMutex = Mutex()
    private var draftJob: Job? = null
    private var draftWritesSuspended = false
    private val mutableState = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = mutableState.asStateFlow()

    init { refresh(autoImport = true) }

    fun refresh(autoImport: Boolean = false) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null) }
            try {
                ioMutex.withLock {
                    if (autoImport && health.availability() == HealthAvailability.AVAILABLE) {
                        val granted = health.grantedPermissions()
                        val needsReview = withContext(Dispatchers.IO) { store.getSetting("baseline.reviewed") != "true" }
                        if (needsReview && granted.isNotEmpty()) {
                            val result = withContext(Dispatchers.IO) { health.sync(store) }
                            mutableState.update { it.copy(message = result.message) }
                        }
                    }
                    loadSnapshot()
                }
            } catch (failure: Exception) { if (failure is CancellationException) throw failure; showStorageFailure(failure) }
        }
    }

    private suspend fun loadSnapshot() {
        val permissions = if (health.availability() == HealthAvailability.AVAILABLE) {
            try { health.grantedPermissions() } catch (_: Exception) { emptySet() }
        } else emptySet()
        val snapshot = withContext(Dispatchers.IO) {
            val profile = profiles.load().orElse(null)
            val activities = store.coachingActivities(LocalDate.now())
            val results = store.results()
            val manual = store.savedManualBaseline()
            val now = Instant.now()
            val healthWindow = HealthConnectGateway.importWindow(store.importToken(HealthConnectGateway.PROVIDER))
            val from = maxOf(healthWindow?.first ?: now.minus(Duration.ofDays(84)), now.minus(Duration.ofDays(84)))
            val derived = BaselineEngine.derive(activities, results, from, now)
            val baseline = BaselineEngine.applyManualOverlay(derived, manual, store.manualBaselineRecordedAt())
            val eligibility = PerformanceEligibilityEvaluator.evaluate(baseline, now, store.manualBaselineRecordedAt())
            val currentPlan = coaching.current()
            val strength = coaching.strength()
            val responses = coaching.responses()
            if (currentPlan != null) {
                AdaptiveCoachingEngine.review(currentPlan, activities, responses, strength, LocalDate.now(), now).ifPresent { proposal ->
                    if (!coaching.proposalKnown(proposal.id())) coaching.saveProposal(proposal)
                }
            }
            val history = coaching.versions()
            val review = currentPlan?.let { WeeklyCoaching.review(it, history, activities, responses, strength, results, LocalDate.now(), development.interruption(), development.sessionDecisions(), baseline.confidence(), store.assessments()) }
            if (review != null && !development.reviewKnown(review.id())) development.saveReview(review)
            reminders.reconcile()
            CoachUiState(
                loading = false, profile = profile, activities = activities, results = results,
                shoes = store.shoes(), assessments = store.assessments(), baseline = baseline, manualBaseline = manual,
                currentPlan = currentPlan, planVersions = history,
                weights = development.weights(), historicalStrength = development.historical(),
                massUnit = runCatching { MassUnit.valueOf(store.getSetting("mass.unit") ?: "KG") }.getOrDefault(MassUnit.KG),
                gymLinks = development.gymLinks(),
                gymProgressionDecisions = org.racedaydreams.data.GymProgrammeStore(getApplication()).decisions(),
                gymProgramme = org.racedaydreams.data.GymProgrammeStore(getApplication()).current(),
                gymVersions = org.racedaydreams.data.GymProgrammeStore(getApplication()).versions(),
                exerciseChoices = org.racedaydreams.data.StrengthUserStore(getApplication()).choices(),
                acknowledgedPbs = org.racedaydreams.data.StrengthUserStore(getApplication()).acknowledged(),
                calendarView = store.getSetting("calendar.view") ?: "List",
                workoutDecisions = development.sessionDecisions(),
                weeklyReview = review, weeklyDecision = review?.let { development.decisionForReview(it) },
                proposals = coaching.proposals().filter { it.baseVersionId() == currentPlan?.id() },
                strengthPlannedDates = development.strengthLinks(), strengthSessions = strength, exercises = coaching.exercises(), runResponses = responses,
                plannedWorkouts = store.plannedWorkouts(), milestonesEnabled = store.getSetting("milestones.enabled") == "true",
                reminderPreferences = reminders.preferences(),
                performanceEligible = eligibility.eligible(), eligibilityReasons = eligibility.reasons(),
                baselineReviewed = store.getSetting("baseline.reviewed") == "true",
                draft = savedState.get<String>("goalDraft") ?: profiles.loadDraft().orElse(null),
                healthAvailability = health.availability(), healthPermissions = permissions
            )
        }
        mutableState.update { snapshot.copy(message = it.message, savedProfileRevision = it.savedProfileRevision) }
    }

    private fun mutate(message: String? = null, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null, message = null) }
            try {
                ioMutex.withLock { withContext(Dispatchers.IO) { block() }; loadSnapshot() }
                onSuccess()
                if (message != null) mutableState.update { it.copy(message = message) }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                onFailure()
                showStorageFailure(failure)
            }
        }
    }

    private fun showStorageFailure(failure: Exception) {
        // Keep existing state and input. Never replace unreadable data with default/demo values.
        val message = if (failure is IllegalArgumentException) failure.message ?: "Check the entered values."
        else "Your local data could not be read or saved. It has been left in place. Try again, or use the confirmed local-data reset."
        mutableState.update { it.copy(loading = false, busy = false, error = message) }
    }

    fun saveProfile(profile: UserProfile, createPlan: Boolean = false) {
        draftWritesSuspended = true
        draftJob?.cancel()
        mutate("Your goal and routine are saved on this device.", onSuccess = {
            savedState.remove<String>("goalDraft")
            draftWritesSuspended = false
            mutableState.update { it.copy(savedProfileRevision = it.savedProfileRevision + 1, draft = null) }
            if(createPlan) generatePlan()
        }, onFailure = { draftWritesSuspended = false }) {
            val eligible = mutableState.value.performanceEligible
            val retainingPerformance = profiles.load().orElse(null)?.preferences()?.strategy() ==
                org.racedaydreams.domain.TrainingStrategy.PERFORMANCE
            require(profile.preferences().strategy() != org.racedaydreams.domain.TrainingStrategy.PERFORMANCE ||
                eligible || retainingPerformance) {
                "Current evidence does not support a new Performance selection. Review your baseline or choose another strategy; your saved goal is unchanged."
            }
            val validated = UserProfile(profile.raceGoal(), profile.preferences(), eligible)
            profiles.save(validated)
            store.putSetting("baseline.reviewed", "true")
        }
    }

    fun saveDraft(json: String) {
        if (draftWritesSuspended) return
        savedState["goalDraft"] = json
        mutableState.update { it.copy(draft = json) }
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(350)
            try { ioMutex.withLock { withContext(Dispatchers.IO) { profiles.saveDraft(json) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(error = "The latest draft could not be saved. Keep this form open and try again.") } }
        }
    }

    fun flushDraft() {
        if (draftWritesSuspended) return
        val draft = savedState.get<String>("goalDraft") ?: return
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            try { ioMutex.withLock { withContext(Dispatchers.IO) { profiles.saveDraft(draft) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(error = "The latest draft could not be saved.") } }
        }
    }

    fun cancelGoalEditing() {
        draftJob?.cancel()
        savedState.remove<String>("goalDraft")
        mutate { profiles.clearDraft() }
    }

    // Moving to the requested screen confirms review. A snackbar on the next screen would
    // obscure the retained form's bottom Continue button during the first interaction.
    fun reviewBaseline(onSuccess: () -> Unit = {}) = mutate(onSuccess = onSuccess) {
        store.putSetting("baseline.reviewed", "true")
    }

    fun saveManualBaseline(input: ManualBaselineInput, onSuccess: () -> Unit = {}) = mutate("Manual baseline details saved. Imported observations remain available.", onSuccess) {
        store.saveManualBaseline(input)
    }

    fun syncHealth() = mutate {
        val result = health.sync(store)
        if (HealthConnectGateway.WEIGHT_PERMISSION in health.grantedPermissions()) health.syncWeights(development)
        mutableState.update { it.copy(message = result.message) }
    }

    fun permissionResult(granted: Set<String>) {
        if (granted.isEmpty()) {
            mutableState.update { it.copy(message = "Health access was not granted. You can continue with optional manual details and use the app without it.") }
            refresh()
        } else syncHealth()
    }

    fun saveResult(result: RecordedResult, onSuccess: () -> Unit = {}) = mutate("Result saved with its stated source.", onSuccess) { store.saveResult(result) }
    fun deleteResult(id: String) = mutate("Result removed.") { store.deleteResult(id) }
    fun saveShoe(shoe: Shoe, onSuccess: () -> Unit = {}) = mutate("Shoe saved.", onSuccess) { store.saveShoe(shoe) }
    fun assignShoe(activityId: String, shoeId: String?) = mutate("Shoe assignment updated.") { store.assignShoe(activityId, shoeId) }
    fun saveNotes(id: String, notes: String, rpe: Double?) = mutate("Run notes saved.") { store.saveActivityNotes(id, notes, rpe) }

    fun saveAssessment(distanceKm: Int, duration: Duration, date: LocalDate, time: LocalTime,
                       bestEffort: Boolean, rpe: Double?, notes: String, onSuccess: () -> Unit = {}) = mutate("Assessment result saved as manually entered evidence. A single effort is not a complete fitness assessment.", onSuccess) {
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
        store.saveAssessment(activity, result)
    }

    // The populated Plan screen confirms the save. A snackbar here can cover its
    // bottom rebuild action immediately after generation, especially with large text.
    fun generatePlan() = mutate {
        val profile = profiles.load().orElse(null) ?: throw IllegalArgumentException("Save your goal and availability first.")
        val baseline = mutableState.value.baseline
        val candidate = OriginalTrainingPlanGenerator().generate(PlanRequest(profile, baseline, LocalDate.now(), emptySet(), coaching.strength()))
        val versions = coaching.versions()
        val responses = coaching.responses().associateBy { it.activityId() }
        val oldCurrent = coaching.current()
        val completed = responses.values.mapNotNull { response ->
            versions.firstOrNull { it.id() == response.planVersionId() }?.plan()?.workouts()?.firstOrNull { it.id() == response.plannedWorkoutId() }
        }.associateBy { it.id() }.toMutableMap()
        // Legacy links without a version retain the current prescription rather than guessing
        // an older one. Restored response links also remain protected while HC awaits re-import.
        store.activities().forEach { actual ->
            if (responses[actual.id()] == null) oldCurrent?.plan()?.workouts()?.firstOrNull { it.id() == actual.plannedWorkoutId() }?.let { completed.putIfAbsent(it.id(), it) }
        }
        val generated = PlanPreservation.retainCompleted(candidate, completed)
        val evidence = "Baseline window ${baseline.windowStart()} to ${baseline.windowEnd()}; confidence ${baseline.confidence()}; " +
            "weekly km ${baseline.weeklyDistanceKm() ?: "unknown"}; longest km ${baseline.longestRunKm() ?: "unknown"}; " +
            "consistent weeks ${baseline.consistentWeeks() ?: "unknown"}; sources ${baseline.provenance()}. " +
            "History, manual inputs and assessment evidence remain distinct. No race prediction is inferred."
        coaching.saveCurrent(PlanVersion(UUID.randomUUID().toString(), generated.id(),
            (coaching.versions().filter { it.planId() == generated.id() }.maxOfOrNull { it.version() } ?: 0) + 1,
            Instant.now(), if (coaching.current() == null) "Runner requested initial programme" else "Runner explicitly requested remaining programme rebuild",
            profile, evidence, generated))
    }

    fun decide(proposal: AdjustmentProposal, optionId: String) = mutate("Your decision is saved. Other workouts and your goal are unchanged.") {
        val completed = store.activities().mapNotNull { it.plannedWorkoutId() }.toSet()
        require(!store.activities().any { it.plannedWorkoutId() == proposal.workoutId() }) { "This workout has already been completed; refresh the plan." }
        coaching.decide(proposal, optionId, completed)
    }

    fun gymProgression(base: String, sessionId: String, exerciseId: String, accept: Boolean) = mutate("Strength progression choice recorded. Actual workouts are unchanged.") {
        val repository=org.racedaydreams.data.GymProgrammeStore(getApplication())
        val p=repository.current() ?: throw IllegalArgumentException("No current gym programme.")
        require(org.racedaydreams.data.GymProgrammeStore.key(p)==base){"The programme changed. Review the current suggestion."}
        val prescription=p.sessions().first{it.id()==sessionId}.exercises().first{it.exerciseId()==exerciseId}
        val actual=coaching.strength()
        val advice=org.racedaydreams.domain.strength.StrengthProgressionAdvisor.review(prescription,actual,Instant.now())
        val evidence=advice.reason()+"\n"+actual.filter{s->s.exercises().any{it.exercise().id()==exerciseId}}.sortedByDescending{it.startedAt()}.take(2).joinToString("\n"){s->s.id()+": "+s.exercises().filter{it.exercise().id()==exerciseId}.joinToString{it.sets().toString()}}
        val proposed=if(accept){require(advice.action()==org.racedaydreams.domain.strength.StrengthProgressionAdvisor.Action.CONSIDER_INCREASE&&advice.suggestedKilograms()!=null){"Current evidence does not support that increase."};org.racedaydreams.domain.strength.GymPlanEditor.loadReference(p,sessionId,exerciseId,advice.suggestedKilograms(),LocalDate.now(),Instant.now(),development.strengthLinks().values.toSet())}else null
        repository.decide(p,proposed,sessionId,exerciseId,evidence)
    }

    fun saveGym(plan: org.racedaydreams.domain.strength.GymProgramme) = mutate() {
        org.racedaydreams.data.GymProgrammeStore(getApplication()).save(plan)
    }
    fun disableGym() = mutate("Generated programming stopped. Your recorded workouts and PBs remain.") { org.racedaydreams.data.GymProgrammeStore(getApplication()).disable() }

    fun exerciseChoice(id: String, favourite: Boolean, archived: Boolean) = mutate {
        org.racedaydreams.data.StrengthUserStore(getApplication()).save(org.racedaydreams.data.StrengthUserStore.Choice(id,favourite,archived))
    }
    fun acknowledgePb(id: String) = mutate { org.racedaydreams.data.StrengthUserStore(getApplication()).acknowledge(id) }

    fun setting(key: String,value: String) = mutate { require(key in setOf("calendar.view","mass.unit","provider.preference")); store.putSetting(key,value) }
    fun saveWeight(value: Double,date: LocalDate,id: String? = null) = mutate {
        require(date <= LocalDate.now()) { "A measurement cannot be in the future." }
        val at = date.atTime(12,0).atZone(ZoneId.systemDefault())
        val identity=id ?: UUID.randomUUID().toString()
        development.saveWeight(BodyWeight(identity,state.value.massUnit.toKilograms(value),at.toInstant(),at.offset,"manual",identity,null))
    }
    fun deleteWeight(id:String) = mutate { development.deleteWeight(id) }
    fun saveHistorical(record: HistoricalStrengthRecord,done:()->Unit = {}) = mutate(onSuccess=done) {
        require(record.date()==null || record.date() <= LocalDate.now()) { "A historical PB cannot be in the future." }
        development.saveHistorical(record)
    }
    fun deleteHistorical(id:String) = mutate { development.deleteHistorical(id) }
    fun weeklyDecision(choice:String) = mutate {
        val review=state.value.weeklyReview ?: throw IllegalArgumentException("No review available.")
        development.decide(review,choice,coaching,coaching.responses().mapNotNull{it.plannedWorkoutId()}.toSet())
    }
    fun reportInterruption(from: LocalDate,returned: LocalDate,reason:String) = mutate {
        require(returned<=LocalDate.now()) { "Return date cannot be in the future." }
        development.saveInterruption(WeeklyCoaching.Interruption(from,returned,reason))
    }

    fun chooseEasy(workoutId:String,reason:String) = mutate("Your selected easy alternative was saved. Other sessions are unchanged.") {
        val current=coaching.current()?:error("No current plan")
        val original=current.plan().workouts().first{it.id()==workoutId}
        require(original.date()>=LocalDate.now()){"Only current or future sessions can be changed."}
        require(coaching.responses().none{it.plannedWorkoutId()==workoutId}&&store.activitySummaries().none{it.plannedWorkoutId()==workoutId}){"Completed sessions cannot be changed."}
        val easy=org.racedaydreams.domain.health.RecoveryCoaching.easyAlternative(original,reason)
        val p=current.plan();val updated=org.racedaydreams.domain.TrainingPlan(p.id(),p.startDate(),p.endDate(),p.phases(),p.workouts().map{if(it.id()==workoutId)easy else it})
        coaching.saveCurrent(PlanVersion(UUID.randomUUID().toString(),current.planId(),current.version()+1,Instant.now(),"Runner chose easy: $reason",current.goalSnapshot(),current.evidenceSnapshot(),updated))
    }

    fun changeWorkout(workoutId:String,movedTo:LocalDate? = null) = mutate {
        val current=coaching.current() ?: throw IllegalArgumentException("No current plan.")
        val workout=current.plan().workouts().firstOrNull{it.id()==workoutId} ?: throw IllegalArgumentException("Workout not found.")
        require(workout.category()!=org.racedaydreams.domain.WorkoutCategory.RACE) { "Change your race date through goal setup." }
        require(coaching.responses().none{it.plannedWorkoutId()==workoutId} && store.activities().none{it.plannedWorkoutId()==workoutId}) { "Completed sessions cannot be changed." }
        if(movedTo!=null) {
            require(movedTo>=LocalDate.now() && movedTo<=current.plan().endDate()) { "Choose a current or future date within this programme. A one-off move does not change recurring days." }
            require(current.plan().workouts().none{it.id()!=workoutId&&it.date()==movedTo}) { "A run is already planned on that date." }
            val monday=movedTo.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            require(current.plan().workouts().count{it.id()!=workoutId&&it.date()>=monday&&it.date()<monday.plusWeeks(1)}<current.goalSnapshot().preferences().runsPerWeek()) { "This move would exceed your maximum runs/week." }
        }
        val workouts=current.plan().workouts().mapNotNull { w->if(w.id()!=workoutId) w else if(movedTo==null) null else PlannedWorkout(w.id(),movedTo,w.category(),w.distanceKm(),w.duration(),w.segments(),"Moved by the runner. "+w.note()) }.sortedBy{it.date()}
        val next=PlanVersion(UUID.randomUUID().toString(),current.planId(),current.version()+1,Instant.now(),if(movedTo==null) "Runner skipped a session" else "Runner moved a session",current.goalSnapshot(),current.evidenceSnapshot(),org.racedaydreams.domain.TrainingPlan(current.planId(),current.plan().startDate(),current.plan().endDate(),current.plan().phases(),workouts))
        coaching.transaction { coaching.saveCurrent(next);development.saveSessionDecision(WorkoutDecision(UUID.randomUUID().toString(),workoutId,current.id(),workout.date(),movedTo,if(movedTo==null)WorkoutDecision.Kind.SKIPPED else WorkoutDecision.Kind.MOVED,Instant.now())) }
    }

    fun saveStrength(session: StrengthSession, onSuccess: () -> Unit = {}, plannedDate: LocalDate? = null) = mutate("Strength workout saved. PB history is derived from retained sets.", onSuccess) {
        coaching.transaction {
            coaching.saveStrength(session)
            if(plannedDate!=null && session.id() !in development.strengthLinks()) {
                val gym=org.racedaydreams.data.GymProgrammeStore(getApplication()).current()
                val prescription=gym?.sessions()?.firstOrNull{it.date()==plannedDate}
                if(gym!=null&&prescription!=null)development.linkGym(session.id(),plannedDate,org.racedaydreams.data.GymProgrammeStore.key(gym),prescription.id())
                else development.linkStrength(session.id(),plannedDate)
            }
        }
    }
    fun deleteStrength(id: String) = mutate("Strength workout removed; PB history has been recalculated.") { coaching.transaction { coaching.deleteStrength(id);development.deleteStrengthLink(id) } }
    fun saveExercise(exercise: StrengthExercise) = mutate("Exercise saved. Recorded sessions retain their original exercise definition.") { coaching.saveExercise(exercise) }

    fun linkRun(activityId: String, versionId: String, workoutId: String, reps: Int? = null) = mutate("Run linked to its preserved prescription.") {
        val activity = store.activities().firstOrNull { it.id() == activityId } ?: throw IllegalArgumentException("Run no longer available.")
        val version = coaching.versions().firstOrNull { it.id() == versionId } ?: throw IllegalArgumentException("Plan version no longer available.")
        val workout = version.plan().workouts().firstOrNull { it.id() == workoutId } ?: throw IllegalArgumentException("Workout not found.")
        require(workout.date() <= LocalDate.now()) { "A future workout cannot be marked completed." }
        require(store.activities().none { it.id() != activityId && it.plannedWorkoutId() == workoutId }) { "Another run is linked to this workout. Review it before changing this link." }
        val response = RunExecution(activity.id(), workout.id(), version.id(), reps, null, "Runner explicitly linked activity to prescription")
        coaching.transaction {
            coaching.saveResponse(response)
            coaching.saveFeedback(activity.id(), version.id(), RunScoreEngine.analyse(workout, activity, response))
        }
    }

    fun completeRun(versionId: String, workoutId: String, distanceKm: Double, duration: Duration,
                    date: LocalDate, time: LocalTime, rpe: Double?, notes: String, onSuccess: (String) -> Unit) = mutate(onSuccess = {}) {
        require(distanceKm.isFinite() && distanceKm > 0 && distanceKm <= 300) { "Enter an actual distance between 0 and 300 km." }
        require(!duration.isZero && !duration.isNegative && duration <= Duration.ofHours(48)) { "Enter a positive actual duration of up to 48 hours." }
        val start = date.atTime(time).atZone(ZoneId.systemDefault())
        require(!start.toInstant().plus(duration).isAfter(Instant.now())) { "A completed run must end before the current time." }
        val version = coaching.versions().firstOrNull { it.id() == versionId } ?: throw IllegalArgumentException("Plan version not found.")
        val workout = version.plan().workouts().firstOrNull { it.id() == workoutId } ?: throw IllegalArgumentException("Workout not found.")
        require(workout.date() <= LocalDate.now()) { "A future prescription cannot be marked completed." }
        require(store.activities().none { it.plannedWorkoutId() == workoutId }) { "This workout already has a recorded run." }
        val id = UUID.randomUUID().toString()
        val activity = ActivityRecord(id, SourceIdentity("manual", id, null), start.toInstant(), start.toInstant().plus(duration), start.offset,
            start.plus(duration).offset, distanceKm * 1000, null, null, null, null, null, rpe, notes, emptyList(), emptyList(), workoutId,
            store.shoes().firstOrNull { it.defaultShoe() }?.id())
        val response = RunExecution(id, workoutId, versionId, null, null, "Manually recorded actual run; no splits or reps invented")
        coaching.transaction {
            store.upsertManualActivity(activity)
            coaching.saveResponse(response)
            coaching.saveFeedback(id, versionId, RunScoreEngine.analyse(workout, activity, response))
        }
        withContext(Dispatchers.Main) { onSuccess(id) }
    }

    fun exportBackup(uri: Uri) = mutate("Backup saved to the file you selected. Keep it somewhere you trust.") {
        // Validate/encode before opening an existing destination in truncating mode.
        val prepared = ByteArrayOutputStream().also { BackupManager(app, store).exportTo(it) }.toByteArray()
        val output = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalArgumentException("The selected document could not be opened.")
        output.use { it.write(prepared) }
    }

    fun restoreBackup(uri: Uri) {
        draftWritesSuspended = true
        draftJob?.cancel()
        mutate("Backup restored. Data from Health Connect can be imported again.", onSuccess = {
            savedState.remove<String>("goalDraft")
            draftWritesSuspended = false
            mutableState.update { it.copy(draft = null) }
        }, onFailure = { draftWritesSuspended = false }) {
            reminders.cancelAllScheduled()
            try {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("The selected backup could not be opened.")
                input.use { BackupManager(app, store).restoreFrom(it) }
            } finally {
                // An invalid backup leaves the old plan intact; restore its alarms too.
                reminders.reconcile()
            }
        }
    }

    fun reset() {
        if(org.racedaydreams.recording.RunRecordingService.running.value){mutableState.update{it.copy(error="Finish the active recording before deleting app data.")};return}
        draftWritesSuspended = true
        draftJob?.cancel()
        mutate("Local app data deleted.", onSuccess = {
            savedState.remove<String>("goalDraft")
            draftWritesSuspended = false
            mutableState.update { it.copy(draft = null, savedProfileRevision = 0) }
        }, onFailure = { draftWritesSuspended = false }) {
            reminders.cancelAllScheduled()
            try { profiles.clear() } finally { reminders.reconcile() }
        }
    }

    fun acknowledgeProfileSaved(revision: Int) {
        mutableState.update { if (it.savedProfileRevision == revision) it.copy(savedProfileRevision = 0) else it }
    }

    fun setMilestonesEnabled(enabled: Boolean) = mutate { store.putSetting("milestones.enabled", enabled.toString()) }
    fun saveReminderPreferences(preferences: ReminderPreferences) = mutate("Reminder preferences saved. Reminders only apply to actual scheduled workouts.") {
        reminders.savePreferences(preferences)
    }
    fun dismissMessage() { mutableState.update { it.copy(message = null) } }
}
