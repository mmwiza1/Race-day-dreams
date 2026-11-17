package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.*
import java.util.UUID
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/** Android lifecycle/SavedState bindings for the existing shared typed authoring operations. */
class StrengthProgrammeViewModel(app: Application, private val retained: SavedStateHandle) :
    AndroidViewModel(app) {
    private val executions = SqliteGymExecutionRepository(app)
    internal val shared =
        SharedStrengthProgrammeCoordinator(
            AndroidSqlConnection.forApplication(app),
            SqliteStrengthProgrammingRepository(app),
            CoachingStore(app),
            executions::recentActual,
            viewModelScope,
            { retained.get<String>("strength.programme.draft") },
            { retained["strength.programme.draft"] = it },
            Instant::now,
            LocalDate::now,
            { UUID.randomUUID().toString() },
        )
    val state = shared.state

    init {
        reload()
    }

    fun clearMessage() = shared.clearMessage()

    fun reload() = shared.reload()

    fun page(delta: Int) = shared.page(delta)

    fun openDraft(draftId: String, done: () -> Unit = {}) = shared.openDraft(draftId, done)

    fun closeEditor() = shared.closeEditor()

    fun newDraft(done: () -> Unit = {}) = shared.newDraft(done)

    fun duplicateDraft(done: () -> Unit = {}) = shared.duplicateDraft(done)

    fun deleteDraft(draftId: String, revision: Int) = shared.deleteDraft(draftId, revision)

    fun header(
        name: String,
        start: LocalDate,
        settings: StrengthProgramme.Settings,
        done: () -> Unit = {},
    ) = shared.header(name, start, settings, done)

    fun block(
        blockId: String?,
        name: String,
        phase: StrengthProgramme.Phase,
        weeks: Int,
        done: () -> Unit = {},
    ) = shared.block(blockId, name, phase, weeks, done)

    fun duplicateBlock(blockId: String) = shared.duplicateBlock(blockId)

    fun deleteBlock(blockId: String) = shared.deleteBlock(blockId)

    fun moveBlock(blockId: String, position: Int) = shared.moveBlock(blockId, position)

    fun day(
        blockId: String,
        dayId: String?,
        title: String,
        weekday: DayOfWeek,
        notes: String?,
        done: () -> Unit = {},
    ) = shared.day(blockId, dayId, title, weekday, notes, done)

    fun duplicateDay(blockId: String, dayId: String, weekday: DayOfWeek) =
        shared.duplicateDay(blockId, dayId, weekday)

    fun deleteDay(blockId: String, dayId: String) = shared.deleteDay(blockId, dayId)

    fun putSlot(
        blockId: String,
        dayId: String,
        slotId: String?,
        exercise: StrengthExercise,
        input: GymSlotInput,
        done: () -> Unit,
    ) = shared.putSlot(blockId, dayId, slotId, exercise, input, done)

    fun superset(blockId: String, dayId: String, slotId: String, breakGroup: Boolean = false) =
        shared.superset(blockId, dayId, slotId, breakGroup)

    fun removeSlot(blockId: String, dayId: String, slotId: String) =
        shared.removeSlot(blockId, dayId, slotId)

    fun moveSlot(
        fromBlock: String,
        fromDay: String,
        toBlock: String,
        toDay: String,
        slotId: String,
        position: Int,
    ) = shared.moveSlot(fromBlock, fromDay, toBlock, toDay, slotId, position)

    fun preview(done: () -> Unit = {}) = shared.preview(done)

    fun dismissPreview() = shared.dismissPreview()

    fun activate(reviewed: ProgrammePreview? = state.value.preview, done: () -> Unit = {}) =
        shared.activate(reviewed, done)

    fun createCustom(
        name: String,
        area: ExerciseMetadata.BodyArea,
        role: ExerciseMetadata.Role,
        measurement: StrengthExercise.MeasurementProfile,
        weightedBodyweight: Boolean,
        equipment: Set<ExerciseMetadata.Equipment>,
        notes: String?,
        done: (String) -> Unit,
    ) =
        shared.createCustom(
            name,
            area,
            role,
            measurement,
            weightedBodyweight,
            equipment,
            notes,
            done,
        )

    fun stopCurrent(reviewed: StrengthProgrammingRepository.Reference?, done: () -> Unit = {}) =
        shared.stopCurrent(reviewed, done)
}
