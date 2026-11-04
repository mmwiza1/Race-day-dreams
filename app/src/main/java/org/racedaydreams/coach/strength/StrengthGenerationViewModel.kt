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
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*

/** Exact concrete generation/preview ownership; durable drafts contain dates, not UI timers. */
data class StrengthGenerationState(
    val busy: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val draft: ConcreteStrengthProgrammeDraft? = null,
    val accepted: Boolean = false,
    val saved: List<ConcreteStrengthProgrammeDraftRepository.Summary> = emptyList(),
    val page: Int = 0,
    val more: Boolean = false,
    val current: StrengthProgrammingRepository.Reference? = null,
    val exercises: List<StrengthExercise> = emptyList(),
    val favourites: Set<String> = emptySet(),
    val excluded: Set<String> = emptySet(),
    val archived: Set<String> = emptySet(),
)

class StrengthGenerationViewModel(app: Application, private val retained: SavedStateHandle) :
    AndroidViewModel(app) {
    private val repository = SqliteConcreteStrengthProgrammeDraftRepository(app)
    private val programmes = SqliteStrengthProgrammingRepository(app)
    private val coaching = CoachingStore(app)
    private val user = StrengthUserStore(app)
    private val mutable = MutableStateFlow(StrengthGenerationState())
    val state: StateFlow<StrengthGenerationState> = mutable.asStateFlow()

    init {
        reload()
    }

    private fun id() = UUID.randomUUID().toString()

    private fun refresh(value: StrengthGenerationState): StrengthGenerationState {
        val rows = repository.list(21, value.page * 20)
        val choices = user.choices()
        val custom = programmes.customExercises().map { it.exercise().id() }.toSet()
        return value.copy(
            saved = rows.take(20),
            more = rows.size > 20,
            current = programmes.currentReference(),
            accepted =
                value.draft?.programme()?.let {
                    programmes.programme(it.id(), it.revision()) != null
                } ?: false,
            exercises =
                coaching.exercises().filter {
                    ExerciseCatalog.definition(it.id()) != null || it.id() in custom
                },
            favourites =
                choices.filter { it.favourite() && !it.archived() }.map { it.exerciseId() }.toSet(),
            archived = choices.filter { it.archived() }.map { it.exerciseId() }.toSet(),
            excluded = user.exclusions().map { it.exerciseId() }.toSet(),
        )
    }

    private fun command(
        done: () -> Unit = {},
        work: (StrengthGenerationState) -> StrengthGenerationState,
    ) {
        if (mutable.value.busy) return
        val before = mutable.value
        mutable.value = before.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.IO) { refresh(work(before)) }
                mutable.value = next.copy(loading = false, busy = false, error = null)
                retained["strength.concrete.draft"] = next.draft?.id()
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
                                ?: "Could not save this preview. Your previous programme remains.",
                    )
            }
        }
    }

    fun reload() = command { before ->
        val selected = before.draft?.id() ?: retained.get<String>("strength.concrete.draft")
        before.copy(draft = selected?.let { repository.get(it) })
    }

    fun page(delta: Int) = command { it.copy(page = (it.page + delta).coerceAtLeast(0)) }

    fun open(draftId: String, done: () -> Unit) =
        command(done) {
            it.copy(
                draft = repository.get(draftId) ?: error("Saved preview is no longer available.")
            )
        }

    fun close() = command { it.copy(draft = null) }

    fun delete(draftId: String, revision: Int) = command {
        repository.delete(draftId, revision)
        it.copy(draft = if (it.draft?.id() == draftId) null else it.draft)
    }

    fun clearMessage() {
        mutable.update { it.copy(error = null, notice = null) }
    }

    fun generate(
        name: String,
        start: LocalDate,
        weeks: Int,
        settings: StrengthProgramme.Settings,
        templateId: String?,
        recovery: Boolean,
        done: () -> Unit,
    ) =
        command(done) { before ->
            val app = getApplication<Application>()
            val focus = SqlitePrimaryFocusRepository(app).load().orElse(PrimaryFocus.STRENGTH)
            val running =
                if (focus == PrimaryFocus.STRENGTH) emptyList()
                else coaching.current()?.plan()?.workouts().orEmpty()
            val easy =
                setOf(
                    WorkoutCategory.RECOVERY_RUN,
                    WorkoutCategory.EASY_RUN,
                    WorkoutCategory.ZONE_2_RUN,
                )
            val demanding = running.filter { it.category() !in easy }.map { it.date() }.toSet()
            val race =
                if (focus == PrimaryFocus.STRENGTH) null
                else
                    SqliteUserPreferencesRepository(app).load().orElse(null)?.raceGoal()?.raceDate()
            val excluded =
                user.exclusions().map { it.exerciseId() }.toSet() +
                    user.choices().filter { it.archived() }.map { it.exerciseId() }
            val favourites =
                user
                    .choices()
                    .filter { it.favourite() && !it.archived() }
                    .map { it.exerciseId() }
                    .toSet()
            val candidate =
                StrengthProgrammeGenerator()
                    .generate(
                        StrengthProgrammeGenerator.Request(
                            id(),
                            Instant.now(),
                            name,
                            settings,
                            start,
                            weeks,
                            templateId,
                            focus,
                            demanding,
                            race,
                            excluded,
                            favourites,
                            recovery,
                        )
                    )
            val draft =
                ConcreteStrengthProgrammeDraft(id(), 1, before.current, candidate, emptyList())
            repository.save(draft, 0)
            before.copy(
                draft = draft,
                notice =
                    "Preview saved locally. Review every block before activating. Your current programme is unchanged.",
            )
        }

    private fun edit(
        workoutId: String,
        done: () -> Unit = {},
        change: (StrengthProgramme.Workout) -> StrengthProgramme.Workout,
    ) =
        command(done) { before ->
            check(!before.accepted) {
                "Accepted programming is immutable. Prepare a future revision instead."
            }
            val old = before.draft ?: error("Open a preview first.")
            val workout = old.programme().workouts().first { it.id() == workoutId }
            val next = ConcreteStrengthProgrammeEditor.replaceWorkout(old, change(workout))
            repository.save(next, old.revision())
            before.copy(
                draft = next,
                notice = "Dated workout saved. Other weeks and your active programme are unchanged.",
            )
        }

    private fun withSlots(
        workout: StrengthProgramme.Workout,
        slots: List<GymWorkoutDefinition.Slot>,
    ): StrengthProgramme.Workout {
        val old = workout.definition()
        return StrengthProgramme.Workout(
            workout.id(),
            workout.date(),
            StrengthWorkoutBudget.workoutMinutes(slots),
            GymWorkoutDefinition(
                old.id(),
                Math.addExact(old.revision(), 1),
                Instant.now(),
                old.title(),
                slots,
            ),
            workout.notes(),
        )
    }

    fun putSlot(
        workoutId: String,
        slotId: String?,
        exercise: StrengthExercise,
        input: GymSlotInput,
        done: () -> Unit,
    ) =
        edit(workoutId, done) { workout ->
            val freshExcluded = user.exclusions().map { it.exerciseId() }.toSet()
            require(
                exercise.id() !in freshExcluded &&
                    user.choices().none { it.exerciseId() == exercise.id() && it.archived() }
            ) {
                "Restore this exercise before programming it."
            }
            require(!input.open) { "Review a target for every programme exercise." }
            val rows = workout.definition().slots().toMutableList()
            val slot = gymSlotFromInput(slotId, exercise, input)
            val index = rows.indexOfFirst { it.id() == slot.id() }
            if (slotId != null) require(index >= 0) { "Reload the changed workout." }
            if (index < 0) rows.add(slot) else rows[index] = slot
            withSlots(workout, rows)
        }

    fun removeSlot(workoutId: String, slotId: String) =
        edit(workoutId) { workout ->
            require(workout.definition().slots().any { it.id() == slotId }) {
                "Reload the changed workout."
            }
            withSlots(workout, workout.definition().slots().filterNot { it.id() == slotId })
        }

    fun reorder(workoutId: String, slotId: String, delta: Int) =
        edit(workoutId) { workout ->
            val rows = workout.definition().slots().toMutableList()
            val index = rows.indexOfFirst { it.id() == slotId }
            require(index >= 0 && index + delta in rows.indices) {
                "Choose an adjacent exercise position."
            }
            val slot = rows.removeAt(index)
            rows.add(index + delta, slot)
            withSlots(workout, rows)
        }

    fun activate(reviewed: ConcreteStrengthProgrammeDraft, decisionId: String, done: () -> Unit) =
        command(done) { before ->
            repository.activate(reviewed, decisionId)
            before.copy(
                draft = null,
                notice =
                    "Programme activated. Previous versions and all entered sets remain preserved.",
            )
        }
}
