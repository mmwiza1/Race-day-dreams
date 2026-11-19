package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.*
import java.util.UUID
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/**
 * Original Android lifecycle and SavedState; generation/review operations and safeguards are
 * shared.
 */
class StrengthGenerationViewModel(app: Application, private val retained: SavedStateHandle) :
    AndroidViewModel(app) {
    private val executions = SqliteGymExecutionRepository(app)
    internal val shared =
        SharedStrengthGenerationCoordinator(
            AndroidSqlConnection.forApplication(app),
            SqliteConcreteStrengthProgrammeDraftRepository(app),
            SqliteStrengthProgrammingRepository(app),
            CoachingStore(app),
            executions,
            executions::recentActual,
            viewModelScope,
            { retained.get<String>("strength.concrete.draft") },
            { retained["strength.concrete.draft"] = it },
            Instant::now,
            LocalDate::now,
            { UUID.randomUUID().toString() },
        )
    val state = shared.state

    init {
        reload()
    }

    fun reload() = shared.reload()

    fun page(delta: Int) = shared.page(delta)

    fun open(draftId: String, done: () -> Unit) = shared.open(draftId, done)

    fun close() = shared.close()

    fun delete(draftId: String, revision: Int) = shared.delete(draftId, revision)

    fun clearMessage() = shared.clearMessage()

    fun prepareFuture(done: () -> Unit) = shared.prepareFuture(done)

    fun generate(
        name: String,
        start: LocalDate,
        weeks: Int,
        settings: StrengthProgramme.Settings,
        templateId: String?,
        recovery: Boolean,
        done: () -> Unit,
    ) = shared.generate(name, start, weeks, settings, templateId, recovery, done)

    fun putSlot(
        workoutId: String,
        slotId: String?,
        exercise: StrengthExercise,
        input: GymSlotInput,
        done: () -> Unit,
    ) = shared.putSlot(workoutId, slotId, exercise, input, done)

    fun removeSlot(workoutId: String, slotId: String) = shared.removeSlot(workoutId, slotId)

    fun reorder(workoutId: String, slotId: String, delta: Int) =
        shared.reorder(workoutId, slotId, delta)

    fun openProgrammeSwap(workoutId: String, slotId: String) =
        shared.openProgrammeSwap(workoutId, slotId)

    fun programmeSwapReason(reason: ExerciseSubstitutionEngine.Reason) =
        shared.programmeSwapReason(reason)

    fun closeProgrammeSwap() = shared.closeProgrammeSwap()

    fun useProgrammeSwap(replacementId: String, target: GymSlotInput?, allFuture: Boolean) =
        shared.useProgrammeSwap(replacementId, target, allFuture)

    fun reviewProgression(workoutId: String, slotId: String) =
        shared.reviewProgression(workoutId, slotId)

    fun closeProgression() = shared.closeProgression()

    fun saveProgressionProposal() = shared.saveProgressionProposal()

    fun openHistory() = shared.openHistory()

    fun historyPage(delta: Int) = shared.historyPage(delta)

    fun reviewHistorical(id: String, revision: Int) = shared.reviewHistorical(id, revision)

    fun historicalList() = shared.historicalList()

    fun closeHistory() = shared.closeHistory()

    fun activate(reviewed: ConcreteStrengthProgrammeDraft, decisionId: String, done: () -> Unit) =
        shared.activate(reviewed, decisionId, done)
}
