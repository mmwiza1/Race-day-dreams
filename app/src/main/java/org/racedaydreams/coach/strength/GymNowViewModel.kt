package org.racedaydreams.coach.strength

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/** Android lifecycle, saved input and actual OS clock/alert bindings for the shared operations. */
class GymNowViewModel(application: Application, retained: SavedStateHandle) :
    AndroidViewModel(application) {
    private val repository = SqliteGymExecutionRepository(application)
    private val programmes = GymProgrammeStore(application)
    private val restClock = GymRestClock(application)
    private val alerts = GymRestAlerts(application)
    internal val shared =
        SharedGymNowCoordinator(
            AndroidSqlConnection.forApplication(application),
            repository,
            SqliteGymAssociationRepository(application),
            SqliteStrengthProgrammingRepository(application),
            CoachingStore(application),
            programmes::current,
            programmes::version,
            { limit, offset -> repository.favouriteWorkouts(limit, offset) },
            repository::recentActual,
            repository::finishedActualIds,
            object : GymNowRetainedState {
                override fun get(key: String): Any? = retained.get<Any>(key)

                override fun set(key: String, value: Any?) {
                    retained[key] = if (value is List<*>) ArrayList(value) else value
                }

                override fun remove(key: String) {
                    retained.remove<Any>(key)
                }

                override fun strings(key: String): List<String>? =
                    retained.get<ArrayList<String>>(key)
            },
            viewModelScope,
            GymWorkoutSessionCoordinator.TickSource {
                GymExecution.Tick(PROCESS_CLOCK, SystemClock.elapsedRealtime(), Instant.now())
            },
            restClock::now,
            alerts::sync,
            Instant::now,
            { ZoneId.systemDefault().rules.getOffset(it) },
            { UUID.randomUUID().toString() },
        )
    val state = shared.state

    init {
        refresh()
    }

    fun refresh() = shared.refresh()

    fun clearMessage() = shared.clearMessage()

    fun startFree(onReady: () -> Unit) = shared.startFree(onReady)

    fun startPlanned(sessionId: String, onReady: () -> Unit) =
        shared.startPlanned(sessionId, onReady)

    fun startFavourite(definition: GymWorkoutDefinition, onReady: () -> Unit) =
        shared.startFavourite(definition, onReady)

    fun repeatPrevious(actualId: String, onReady: () -> Unit) =
        shared.repeatPrevious(actualId, onReady)

    fun recover(id: String, onReady: () -> Unit) = shared.recover(id, onReady)

    fun discardUnfinished(id: String) = shared.discardUnfinished(id)

    fun favouritePage(delta: Int) = shared.favouritePage(delta)

    fun select(slotId: String) = shared.select(slotId)

    fun addExercise(exercise: StrengthExercise) = shared.addExercise(exercise)

    fun openSubstitutions(reason: ExerciseSubstitutionEngine.Reason) =
        shared.openSubstitutions(reason)

    fun changeSubstitutionReason(reason: ExerciseSubstitutionEngine.Reason) =
        shared.changeSubstitutionReason(reason)

    fun changeSubstitutionEquipment(equipment: Set<ExerciseMetadata.Equipment>) =
        shared.changeSubstitutionEquipment(equipment)

    fun showMoreSubstitutions() = shared.showMoreSubstitutions()

    fun closeSubstitutions() = shared.closeSubstitutions()

    fun replaceForToday(replacement: StrengthExercise) = shared.replaceForToday(replacement)

    fun pause() = shared.pause()

    fun resume() = shared.resume()

    fun discard() = shared.discard()

    fun retry() = shared.retry()

    fun reviewAssociation(
        proposal: GymWorkoutAssociation.Proposal,
        choice: GymWorkoutAssociation.Choice,
    ) = shared.reviewAssociation(proposal, choice)

    fun refreshAssociation() = shared.refreshAssociation()

    fun openCompleted(id: String, onReady: () -> Unit) = shared.openCompleted(id, onReady)

    fun reloadSavedEvidence() = shared.reloadSavedEvidence()

    fun updateForm(update: (GymSetDraft) -> GymSetDraft) = shared.updateForm(update)

    fun restPreferences(value: GymRestTimer.Preferences) = shared.restPreferences(value)

    fun restAction(action: String) = shared.restAction(action)

    fun warmupSuggestion() = shared.warmupSuggestion()

    fun prepareWarmup(ramp: GymWarmup.Ramp?) = shared.prepareWarmup(ramp)

    fun nextSuperset() = shared.nextSuperset()

    fun skipMovement() = shared.skipMovement()

    fun breakSuperset() = shared.breakSuperset()

    fun saveSet() = shared.saveSet()

    fun finish(early: Boolean = false) = shared.finish(early)

    fun clearFinishPrompt() = shared.clearFinishPrompt()

    fun setWorkoutVisible(visible: Boolean) = shared.setWorkoutVisible(visible)

    internal suspend fun observeWorkoutTime() = shared.observeWorkoutTime()

    fun generate(request: GymNowWorkouts.Request) = shared.generate(request)

    fun startGenerated(quick: Boolean, onReady: () -> Unit) = shared.startGenerated(quick, onReady)

    fun newBuilder() = shared.newBuilder()

    fun editFavourite(definition: GymWorkoutDefinition) = shared.editFavourite(definition)

    fun builderPair(slotId: String) = shared.builderPair(slotId)

    fun builderExtend(slotId: String) = shared.builderExtend(slotId)

    fun builderUngroup(group: String) = shared.builderUngroup(group)

    fun builderTitle(title: String) = shared.builderTitle(title)

    fun builderRemove(slotId: String) = shared.builderRemove(slotId)

    fun builderMove(slotId: String, delta: Int) = shared.builderMove(slotId, delta)

    fun builderDuplicate(slotId: String) = shared.builderDuplicate(slotId)

    fun builderPutSlot(slotId: String?, exercise: StrengthExercise, input: GymSlotInput) =
        shared.builderPutSlot(slotId, exercise, input)

    fun saveBuilderFavourite() = shared.saveBuilderFavourite()

    fun removeFavourite(definition: GymWorkoutDefinition) = shared.removeFavourite(definition)

    fun startBuilder(onReady: () -> Unit) = shared.startBuilder(onReady)

    companion object {
        private val PROCESS_CLOCK = UUID.randomUUID().toString()
    }
}
