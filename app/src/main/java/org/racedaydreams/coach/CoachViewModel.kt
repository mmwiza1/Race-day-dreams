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
    private val store = FeatureStore(application)
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
            val activities = store.activities()
            val results = store.results()
            val manual = store.savedManualBaseline()
            val now = Instant.now()
            val healthWindow = HealthConnectGateway.importWindow(store.importToken(HealthConnectGateway.PROVIDER))
            val from = maxOf(healthWindow?.first ?: now.minus(Duration.ofDays(84)), now.minus(Duration.ofDays(84)))
            val derived = BaselineEngine.derive(activities, results, from, now)
            val baseline = BaselineEngine.applyManualOverlay(derived, manual, store.manualBaselineRecordedAt())
            val eligibility = PerformanceEligibilityEvaluator.evaluate(baseline, now, store.manualBaselineRecordedAt())
            reminders.reconcile()
            CoachUiState(
                loading = false, profile = profile, activities = activities, results = results,
                shoes = store.shoes(), assessments = store.assessments(), baseline = baseline, manualBaseline = manual,
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

    fun saveProfile(profile: UserProfile) {
        draftWritesSuspended = true
        draftJob?.cancel()
        mutate("Your goal and routine are saved on this device.", onSuccess = {
            savedState.remove<String>("goalDraft")
            draftWritesSuspended = false
            mutableState.update { it.copy(savedProfileRevision = it.savedProfileRevision + 1, draft = null) }
        }, onFailure = { draftWritesSuspended = false }) {
            val eligible = mutableState.value.performanceEligible
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

    fun reviewBaseline() = mutate("Baseline reviewed. You can change it whenever your circumstances change.") {
        store.putSetting("baseline.reviewed", "true")
    }

    fun saveManualBaseline(input: ManualBaselineInput, onSuccess: () -> Unit = {}) = mutate("Manual baseline details saved. Imported observations remain available.", onSuccess) {
        store.saveManualBaseline(input)
    }

    fun syncHealth() = mutate {
        val result = health.sync(store)
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
