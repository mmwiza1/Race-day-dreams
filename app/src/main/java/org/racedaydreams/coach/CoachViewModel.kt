package org.racedaydreams.coach

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.racedaydreams.coach.application.CoachApplicationServices
import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.coaching.AdjustmentProposal
import org.racedaydreams.domain.reminders.ReminderPreferences
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/**
 * Lifecycle owner for the coherent application read model and retained goal draft. Feature
 * coordinators own commands, repositories and application rules. Compatibility delegates preserve
 * established screens while new features use the coordinator APIs.
 */
class CoachViewModel(application: Application, private val savedState: SavedStateHandle) :
    AndroidViewModel(application) {
    private val services =
        CoachApplicationServices(application, viewModelScope) {
            savedState.get<String>("goalDraft")
        }
    private val operations = services.operations
    val state = operations.state
    val runningPlan = services.runningPlan
    val runningEvidence = services.runningEvidence
    val strength = services.strength
    val progress = services.progress
    val baseline = services.baseline
    val settings = services.settings
    val healthOperations = services.health
    val health = services.healthGateway
    private var draftJob: Job? = null
    private var draftWritesSuspended = false

    init {
        refresh(autoImport = true)
    }

    fun refresh(autoImport: Boolean = false) =
        operations.refresh { healthOperations.beforeRefresh(autoImport) }

    fun saveProfile(profile: UserProfile, createPlan: Boolean = false) {
        suspendDraftWrites()
        operations.submit(
            "Your goal and routine are saved on this device.",
            onSuccess = {
                clearRetainedDraft()
                operations.update { it.copy(savedProfileRevision = it.savedProfileRevision + 1) }
                if (createPlan) runningPlan.generatePlan()
            },
            onFailure = { draftWritesSuspended = false },
        ) {
            services.profile.save(profile, operations.current.performanceEligible)
        }
    }

    fun saveDraft(json: String) {
        if (draftWritesSuspended) return
        savedState["goalDraft"] = json
        operations.update { it.copy(draft = json) }
        draftJob?.cancel()
        draftJob =
            viewModelScope.launch {
                delay(350)
                try {
                    operations.serialiseDraftWrite { services.profile.saveDraft(json) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    operations.update {
                        it.copy(
                            error =
                                "The latest draft could not be saved. Keep this form open and try again."
                        )
                    }
                }
            }
    }

    fun flushDraft() {
        if (draftWritesSuspended) return
        val draft = savedState.get<String>("goalDraft") ?: return
        draftJob?.cancel()
        draftJob =
            viewModelScope.launch {
                try {
                    operations.serialiseDraftWrite { services.profile.saveDraft(draft) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    operations.update { it.copy(error = "The latest draft could not be saved.") }
                }
            }
    }

    fun cancelGoalEditing() {
        draftJob?.cancel()
        savedState.remove<String>("goalDraft")
        operations.submit { services.profile.clearDraft() }
    }

    fun restoreBackup(uri: Uri) {
        suspendDraftWrites()
        settings.restoreBackup(
            uri,
            onSuccess = { clearRetainedDraft() },
            onFailure = { draftWritesSuspended = false },
        )
    }

    fun reset() {
        if (!settings.canReset()) return
        suspendDraftWrites()
        settings.reset(
            onSuccess = {
                clearRetainedDraft()
                operations.update { it.copy(savedProfileRevision = 0) }
            },
            onFailure = { draftWritesSuspended = false },
        )
    }

    fun acknowledgeProfileSaved(revision: Int) {
        operations.update {
            if (it.savedProfileRevision == revision) it.copy(savedProfileRevision = 0) else it
        }
    }

    fun dismissMessage() {
        operations.update { it.copy(message = null) }
    }

    private fun suspendDraftWrites() {
        draftWritesSuspended = true
        draftJob?.cancel()
    }

    private fun clearRetainedDraft() {
        savedState.remove<String>("goalDraft")
        draftWritesSuspended = false
        operations.update { it.copy(draft = null) }
    }

    // Established entry points delegate to feature-owned operations; no rules are duplicated here.
    fun reviewBaseline(onSuccess: () -> Unit = {}) = baseline.reviewBaseline(onSuccess)

    fun saveManualBaseline(input: ManualBaselineInput, onSuccess: () -> Unit = {}) =
        baseline.saveManualBaseline(input, onSuccess)

    fun saveAssessment(
        distanceKm: Int,
        duration: Duration,
        date: LocalDate,
        time: LocalTime,
        bestEffort: Boolean,
        rpe: Double?,
        notes: String,
        onSuccess: () -> Unit = {},
    ) = baseline.saveAssessment(distanceKm, duration, date, time, bestEffort, rpe, notes, onSuccess)

    fun syncHealth() = healthOperations.syncHealth()

    fun permissionResult(granted: Set<String>) = healthOperations.permissionResult(granted)

    fun generatePlan() = runningPlan.generatePlan()

    fun decide(proposal: AdjustmentProposal, optionId: String) =
        runningPlan.decide(proposal, optionId)

    fun weeklyDecision(choice: String) = runningPlan.weeklyDecision(choice)

    fun reportInterruption(from: LocalDate, returned: LocalDate, reason: String) =
        runningPlan.reportInterruption(from, returned, reason)

    fun chooseEasy(workoutId: String, reason: String) = runningPlan.chooseEasy(workoutId, reason)

    fun changeWorkout(workoutId: String, movedTo: LocalDate? = null) =
        runningPlan.changeWorkout(workoutId, movedTo)

    fun saveResult(result: RecordedResult, onSuccess: () -> Unit = {}) =
        runningEvidence.saveResult(result, onSuccess)

    fun deleteResult(id: String) = runningEvidence.deleteResult(id)

    fun saveShoe(shoe: Shoe, onSuccess: () -> Unit = {}) = runningEvidence.saveShoe(shoe, onSuccess)

    fun assignShoe(activityId: String, shoeId: String?) =
        runningEvidence.assignShoe(activityId, shoeId)

    fun saveNotes(id: String, notes: String, rpe: Double?) =
        runningEvidence.saveNotes(id, notes, rpe)

    fun shoeDiscomfort(activityId: String, noted: Boolean) =
        runningEvidence.shoeDiscomfort(activityId, noted)

    fun linkRun(activityId: String, versionId: String, workoutId: String, reps: Int? = null) =
        runningEvidence.linkRun(activityId, versionId, workoutId, reps)

    fun completeRun(
        versionId: String,
        workoutId: String,
        distanceKm: Double,
        duration: Duration,
        date: LocalDate,
        time: LocalTime,
        rpe: Double?,
        notes: String,
        onSuccess: (String) -> Unit,
    ) =
        runningEvidence.completeRun(
            versionId,
            workoutId,
            distanceKm,
            duration,
            date,
            time,
            rpe,
            notes,
            onSuccess,
        )

    fun gymProgression(base: String, sessionId: String, exerciseId: String, accept: Boolean) =
        strength.gymProgression(base, sessionId, exerciseId, accept)

    fun saveGym(plan: GymProgramme) = strength.saveGym(plan)

    fun disableGym() = strength.disableGym()

    fun exerciseExclusion(
        id: String,
        reason: org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason?,
    ) = strength.exerciseExclusion(id, reason)

    fun exerciseChoice(id: String, favourite: Boolean, archived: Boolean) =
        strength.exerciseChoice(id, favourite, archived)

    fun acknowledgePb(id: String) = strength.acknowledgePb(id)

    fun saveStrength(
        session: StrengthSession,
        onSuccess: () -> Unit = {},
        plannedDate: LocalDate? = null,
    ) = strength.saveStrength(session, onSuccess, plannedDate)

    fun deleteStrength(id: String) = strength.deleteStrength(id)

    fun saveExercise(exercise: StrengthExercise) = strength.saveExercise(exercise)

    fun saveHistorical(record: HistoricalStrengthRecord, done: () -> Unit = {}) =
        strength.saveHistorical(record, done)

    fun deleteHistorical(id: String) = strength.deleteHistorical(id)

    fun saveWeight(value: Double, date: LocalDate, id: String? = null) =
        progress.saveWeight(value, date, id)

    fun deleteWeight(id: String) = progress.deleteWeight(id)

    fun startingPath(path: String) = settings.startingPath(path)

    fun setting(key: String, value: String) = settings.setting(key, value)

    fun exportBackup(uri: Uri) = settings.exportBackup(uri)

    fun setMilestonesEnabled(enabled: Boolean) = settings.setMilestonesEnabled(enabled)

    fun saveReminderPreferences(preferences: ReminderPreferences) =
        settings.saveReminderPreferences(preferences)
}
