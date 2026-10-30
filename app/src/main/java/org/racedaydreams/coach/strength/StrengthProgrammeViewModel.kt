package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

/** A review token binds the frozen candidate to the draft and current pointer actually reviewed. */
data class ProgrammePreview(
    val programme: StrengthProgramme,
    val before: StrengthProgrammingRepository.Reference?,
    val draftRevision: Int,
    val decisionId: String,
)

data class ProgrammeAuthoringState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val draft: StrengthProgrammeDraft? = null,
    val preview: ProgrammePreview? = null,
    val drafts: List<StrengthProgrammingRepository.DraftSummary> = emptyList(),
    val page: Int = 0,
    val moreDrafts: Boolean = false,
    val current: StrengthProgrammingRepository.Reference? = null,
    val exercises: List<StrengthExercise> = emptyList(),
    val favourites: Set<String> = emptySet(),
    val recent: List<String> = emptyList(),
    val archived: Set<String> = emptySet(),
    val excluded: Set<String> = emptySet(),
)

/** Feature-owned authoring. Only IDs enter SavedState; durable edits use typed optimistic rows. */
class StrengthProgrammeViewModel(app: Application, private val retained: SavedStateHandle) :
    AndroidViewModel(app) {
    private val repository = SqliteStrengthProgrammingRepository(app)
    private val choices = StrengthUserStore(app)
    private val coaching = CoachingStore(app)
    private val executions = SqliteGymExecutionRepository(app)
    private val mutable = MutableStateFlow(ProgrammeAuthoringState())
    val state: StateFlow<ProgrammeAuthoringState> = mutable.asStateFlow()

    init {
        reload()
    }

    private fun id() = UUID.randomUUID().toString()

    private fun enrich(value: ProgrammeAuthoringState): ProgrammeAuthoringState {
        val rows = repository.drafts(21, value.page * 20)
        val user = choices.choices()
        val custom = repository.customExercises().map { it.exercise().id() }.toSet()
        return value.copy(
            drafts = rows.take(20),
            moreDrafts = rows.size > 20,
            current = repository.currentReference(),
            exercises =
                coaching.exercises().filter {
                    ExerciseCatalog.definition(it.id()) != null || it.id() in custom
                },
            favourites =
                user.filter { it.favourite() && !it.archived() }.map { it.exerciseId() }.toSet(),
            archived = user.filter { it.archived() }.map { it.exerciseId() }.toSet(),
            excluded = choices.exclusions().map { it.exerciseId() }.toSet(),
            recent =
                executions
                    .recentActual(10)
                    .flatMap { it.exercises().map { block -> block.exercise().id() } }
                    .distinct(),
        )
    }

    private fun command(
        done: () -> Unit = {},
        operation: (ProgrammeAuthoringState) -> ProgrammeAuthoringState,
    ) {
        if (mutable.value.busy) return
        val before = mutable.value
        mutable.value = before.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.IO) { enrich(operation(before)) }
                mutable.value = next.copy(loading = false, busy = false, error = null)
                retained["strength.programme.draft"] = next.draft?.id()
                done()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutable.value =
                    before.copy(
                        loading = false,
                        busy = false,
                        error =
                            failure.message
                                ?: "The programme could not be saved. Your preserved history is unchanged.",
                    )
            }
        }
    }

    fun clearMessage() {
        if (!state.value.busy) mutable.update { it.copy(error = null, notice = null) }
    }

    fun reload() = command { current ->
        val savedId = retained.get<String>("strength.programme.draft")
        current.copy(draft = savedId?.let { repository.draft(it) }, preview = null)
    }

    fun page(delta: Int) = command { it.copy(page = (it.page + delta).coerceAtLeast(0)) }

    fun openDraft(draftId: String, done: () -> Unit = {}) =
        command(done) { current ->
            current.copy(
                draft = repository.draft(draftId) ?: error("This saved draft no longer exists."),
                preview = null,
            )
        }

    fun closeEditor() = command { it.copy(draft = null, preview = null) }

    fun newDraft(done: () -> Unit = {}) =
        command(done) { current ->
            val today = LocalDate.now()
            val weekdays = listOf(today.dayOfWeek, today.plusDays(3).dayOfWeek)
            val settings =
                StrengthProgramme.Settings(
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Priority.STRENGTH_FIRST,
                    GymProgramme.Experience.BEGINNER,
                    weekdays.toSet(),
                    2,
                    45,
                    emptySet(),
                )
            val days =
                weekdays.mapIndexed { index, day ->
                    StrengthProgrammeDraft.Day(id(), day, "Workout ${index + 1}", emptyList(), null)
                }
            val draft =
                StrengthProgrammeDraft(
                    id(),
                    1,
                    id(),
                    0,
                    "",
                    settings,
                    today,
                    listOf(
                        StrengthProgrammeDraft.WeekBlock(
                            id(),
                            StrengthProgramme.Phase.FOUNDATION,
                            "Foundation",
                            4,
                            days,
                        )
                    ),
                    StrengthProgramme.Origin.CUSTOM,
                    null,
                )
            repository.saveDraft(draft, 0)
            current.copy(
                draft = draft,
                preview = null,
                notice = "Draft saved locally. Nothing has been activated.",
            )
        }

    fun duplicateDraft(done: () -> Unit = {}) =
        command(done) { current ->
            val old = current.draft ?: error("Open a draft first.")
            val draft =
                StrengthProgrammeDraft(
                    id(),
                    1,
                    id(),
                    0,
                    old.name().take(110) + " copy",
                    old.settings(),
                    old.start(),
                    old.blocks(),
                    StrengthProgramme.Origin.CUSTOM,
                    null,
                )
            repository.saveDraft(draft, 0)
            current.copy(
                draft = draft,
                preview = null,
                notice = "Independent copy saved. The original programme and history remain.",
            )
        }

    fun deleteDraft(draftId: String, revision: Int) = command { current ->
        repository.deleteDraft(draftId, revision)
        if (current.draft?.id() == draftId) current.copy(draft = null, preview = null) else current
    }

    private fun edit(
        done: () -> Unit = {},
        change: (StrengthProgrammeDraft) -> StrengthProgrammeDraft,
    ) =
        command(done) { current ->
            val previous = current.draft ?: error("Open a saved draft first.")
            val next = change(previous)
            repository.saveDraft(next, previous.revision())
            current.copy(
                draft = next,
                preview = null,
                notice = "Draft saved locally. Active programming is unchanged.",
            )
        }

    fun header(
        name: String,
        start: LocalDate,
        settings: StrengthProgramme.Settings,
        done: () -> Unit = {},
    ) =
        edit(done) { old ->
            StrengthProgrammeDraft(
                old.id(),
                old.revision() + 1,
                old.programmeId(),
                old.baseRevision(),
                name.trim(),
                settings,
                start,
                old.blocks(),
                old.origin(),
                old.templateId(),
            )
        }

    fun block(
        blockId: String?,
        name: String,
        phase: StrengthProgramme.Phase,
        weeks: Int,
        done: () -> Unit = {},
    ) =
        edit(done) {
            val days = it.blocks().firstOrNull { b -> b.id() == blockId }?.days() ?: emptyList()
            it.replaceBlock(
                StrengthProgrammeDraft.WeekBlock(blockId ?: id(), phase, name.trim(), weeks, days)
            )
        }

    fun duplicateBlock(blockId: String) = edit { it.duplicateBlock(blockId, id()) }

    fun deleteBlock(blockId: String) = edit { it.deleteBlock(blockId) }

    fun moveBlock(blockId: String, position: Int) = edit { it.reorderBlock(blockId, position) }

    fun day(
        blockId: String,
        dayId: String?,
        title: String,
        weekday: DayOfWeek,
        notes: String?,
        done: () -> Unit = {},
    ) =
        edit(done) {
            val slots =
                it.blocks()
                    .first { b -> b.id() == blockId }
                    .days()
                    .firstOrNull { d -> d.id() == dayId }
                    ?.slots() ?: emptyList()
            it.replaceDay(
                blockId,
                StrengthProgrammeDraft.Day(
                    dayId ?: id(),
                    weekday,
                    title.trim(),
                    slots,
                    notes?.trim()?.ifEmpty { null },
                ),
            )
        }

    fun duplicateDay(blockId: String, dayId: String, weekday: DayOfWeek) = edit {
        it.duplicateDay(blockId, dayId, id(), weekday)
    }

    fun deleteDay(blockId: String, dayId: String) = edit { it.deleteDay(blockId, dayId) }

    fun putSlot(
        blockId: String,
        dayId: String,
        slotId: String?,
        exercise: StrengthExercise,
        input: GymSlotInput,
        done: () -> Unit,
    ) =
        edit(done) {
            require(!input.open) { "A programme exercise needs a reviewed target." }
            require(
                exercise.id() !in state.value.excluded && exercise.id() !in state.value.archived
            ) {
                "Restore this exercise before programming it."
            }
            val day =
                it.blocks().first { b -> b.id() == blockId }.days().first { d -> d.id() == dayId }
            val slot = gymSlotFromInput(slotId, exercise, input)
            val rows = day.slots().toMutableList()
            val index = rows.indexOfFirst { old -> old.id() == slot.id() }
            if (index < 0) rows.add(slot) else rows[index] = slot
            it.replaceDay(
                blockId,
                StrengthProgrammeDraft.Day(day.id(), day.weekday(), day.title(), rows, day.notes()),
            )
        }

    fun removeSlot(blockId: String, dayId: String, slotId: String) = edit {
        val day = it.blocks().first { b -> b.id() == blockId }.days().first { d -> d.id() == dayId }
        require(day.slots().any { s -> s.id() == slotId }) {
            "This exercise is no longer in the draft."
        }
        it.replaceDay(
            blockId,
            StrengthProgrammeDraft.Day(
                day.id(),
                day.weekday(),
                day.title(),
                day.slots().filterNot { s -> s.id() == slotId },
                day.notes(),
            ),
        )
    }

    fun moveSlot(
        fromBlock: String,
        fromDay: String,
        toBlock: String,
        toDay: String,
        slotId: String,
        position: Int,
    ) = edit { it.moveExercise(fromBlock, fromDay, toBlock, toDay, slotId, position) }

    fun preview(done: () -> Unit = {}) =
        command(done) { current ->
            val draft = current.draft ?: error("Open a saved draft first.")
            require(repository.draft(draft.id()) == draft) {
                "This draft changed elsewhere. Reload it before previewing."
            }
            require(!draft.start().isBefore(LocalDate.now())) {
                "Choose today or a future start date before activating a new programme."
            }
            require(repository.programme(draft.programmeId(), draft.baseRevision() + 1) == null) {
                "This draft was already activated. Duplicate it for a new programme; preserved revisions cannot be overwritten."
            }
            val custom = repository.customExercises().associateBy { it.exercise().id() }
            val excluded =
                choices.exclusions().map { it.exerciseId() }.toSet() +
                    choices.choices().filter { it.archived() }.map { it.exerciseId() }
            val candidate = draft.preview(Instant.now(), excluded, custom)
            current.copy(
                preview =
                    ProgrammePreview(
                        candidate,
                        repository.currentReference(),
                        draft.revision(),
                        id(),
                    )
            )
        }

    fun dismissPreview() {
        if (!state.value.busy) mutable.update { it.copy(preview = null) }
    }

    /**
     * Only an explicit confirmation invokes this command; the repository checks draft and
     * current-pointer expectations together in one transaction.
     */
    fun activate(reviewed: ProgrammePreview? = state.value.preview, done: () -> Unit = {}) =
        command(done) { current ->
            require(current.preview == reviewed) {
                "The preview changed. Review it again before activation."
            }
            val review =
                current.preview ?: error("Review a complete programme before activating it.")
            val draft = current.draft ?: error("The reviewed draft is missing.")
            require(
                draft.revision() == review.draftRevision && repository.draft(draft.id()) == draft
            ) {
                "The draft changed after review. Reload and preview again; nothing was activated."
            }
            repository.activateDraft(review.programme, review.before, review.decisionId, draft)
            current.copy(
                preview = null,
                draft = null,
                notice =
                    "Programme activated by your choice. Previous versions and actual sets are preserved.",
            )
        }

    fun stopCurrent(reviewed: StrengthProgrammingRepository.Reference?, done: () -> Unit = {}) =
        command(done) { current ->
            require(reviewed != null && reviewed == current.current) {
                "The active programme changed. Review it again before stopping."
            }
            repository.stop(reviewed, id())
            current.copy(
                notice = "Programming stopped. Historical prescriptions and actual workouts remain."
            )
        }
}
