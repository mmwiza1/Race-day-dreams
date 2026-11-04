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

data class ProgrammeSwapReview(
    val draftId: String,
    val draftRevision: Int,
    val workoutId: String,
    val slotId: String,
    val original: StrengthExercise,
    val reason: ExerciseSubstitutionEngine.Reason,
    val suggestions: List<ExerciseSubstitutionEngine.Suggestion> = emptyList(),
)

data class ProgrammeProgressionReview(
    val draftId: String,
    val draftRevision: Int,
    val workoutId: String,
    val slot: GymWorkoutDefinition.Slot,
    val advice: StrengthTargetProgression.Advice,
)

/** Exact concrete generation/preview ownership; durable drafts contain dates, not UI timers. */
data class StrengthGenerationState(
    val busy: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val draft: ConcreteStrengthProgrammeDraft? = null,
    val accepted: Boolean = false,
    val superseded: Boolean = false,
    val baseProgramme: StrengthProgramme? = null,
    val swapReview: ProgrammeSwapReview? = null,
    val progressionReview: ProgrammeProgressionReview? = null,
    val protectedWorkoutIds: Set<String> = emptySet(),
    val saved: List<ConcreteStrengthProgrammeDraftRepository.Summary> = emptyList(),
    val page: Int = 0,
    val more: Boolean = false,
    val current: StrengthProgrammingRepository.Reference? = null,
    val historyOpen: Boolean = false,
    val historyPage: Int = 0,
    val history: List<StrengthProgrammingRepository.Summary> = emptyList(),
    val historyMore: Boolean = false,
    val historicalProgramme: StrengthProgramme? = null,
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
        val preserved =
            value.draft?.programme()?.let { programmes.programme(it.id(), it.revision()) }
        val baseReview =
            value.draft
                ?.takeIf { it.programme().parentRevision() > 0 }
                ?.before()
                ?.let { programmes.reviewRevision(it) }
        val history =
            if (value.historyOpen) programmes.versions(21, value.historyPage * 20) else emptyList()
        return value.copy(
            history = history.take(20),
            historyMore = history.size > 20,
            saved = rows.take(20),
            more = rows.size > 20,
            current = programmes.currentReference(),
            protectedWorkoutIds = baseReview?.let { programmeProtectedIds(it) } ?: emptySet(),
            baseProgramme = baseReview?.programme(),
            accepted = preserved != null && preserved == value.draft?.programme(),
            superseded = preserved != null && preserved != value.draft?.programme(),
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
        before.copy(
            draft = selected?.let { repository.get(it) },
            swapReview = null,
            progressionReview = null,
        )
    }

    fun page(delta: Int) = command { it.copy(page = (it.page + delta).coerceAtLeast(0)) }

    fun open(draftId: String, done: () -> Unit) =
        command(done) {
            it.copy(
                draft = repository.get(draftId) ?: error("Saved preview is no longer available.")
            )
        }

    fun close() = command { it.copy(draft = null, swapReview = null, progressionReview = null) }

    fun delete(draftId: String, revision: Int) = command {
        repository.delete(draftId, revision)
        it.copy(draft = if (it.draft?.id() == draftId) null else it.draft)
    }

    fun clearMessage() {
        mutable.update { it.copy(error = null, notice = null) }
    }

    /** Started prescriptions are frozen too; these IDs are protection, not completion claims. */
    private fun programmeProtectedIds(
        review: StrengthProgrammingRepository.RevisionReview
    ): Set<String> {
        val ids = review.completedWorkoutIds().toMutableSet()
        val known = review.programme().workouts().map { it.id() }.toSet()
        val gym = SqliteGymExecutionRepository(getApplication<Application>())
        gym.unfinished().forEach { row ->
            val source = gym.load(row.id()).execution().source()
            if (
                source?.kind() == GymExecution.SourceKind.PROGRAMME &&
                    source.id() == review.programme().id() &&
                    source.sessionId() in known
            )
                ids.add(source.sessionId())
        }
        return ids.toSet()
    }

    /** A copy for review only. No accepted prescription changes until activate. */
    fun prepareFuture(done: () -> Unit) =
        command(done) { before ->
            val expected = before.current ?: error("Choose an active authored programme first.")
            val review = programmes.reviewRevision(expected)
            require(programmes.currentReference() == expected) {
                "Current programming changed. Reload before reviewing."
            }
            val old = review.programme()
            val effective = maxOf(LocalDate.now(), old.effectiveFrom(), old.start())
            require(!effective.isAfter(old.end())) {
                "This programme has ended. Generate a new programme; its history remains preserved."
            }
            val now = Instant.now()
            require(!now.isBefore(old.createdAt())) {
                "Review the device clock before creating a programme revision."
            }
            val proposed =
                StrengthProgramme(
                    old.id(),
                    Math.addExact(old.revision(), 1),
                    old.revision(),
                    now,
                    old.name(),
                    old.origin(),
                    old.templateId(),
                    old.settings(),
                    old.blocks(),
                    effective,
                    "Athlete chose to review future strength programming; earlier and completed prescriptions remain.",
                    old.tradeoffs(),
                )
            StrengthProgrammeEditor.Proposal(
                old,
                proposed,
                review.completedWorkoutIds(),
                emptyList(),
            )
            val draft = ConcreteStrengthProgrammeDraft(id(), 1, expected, proposed, emptyList())
            repository.save(draft, 0)
            before.copy(draft = draft, notice = "Future revision saved for review.")
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
            val base =
                before.draft
                    ?.takeIf { it.programme().parentRevision() > 0 }
                    ?.let {
                        val reference =
                            it.before() ?: error("The preserved programme base is missing.")
                        require(programmes.currentReference() == reference) {
                            "Programming changed. Prepare a new revision from the current programme."
                        }
                        programmes.reviewRevision(reference)
                    }
            if (base != null)
                require(start == base.programme().start()) {
                    "Regeneration retains the original programme start."
                }
            val candidate =
                StrengthProgrammeGenerator()
                    .generate(
                        StrengthProgrammeGenerator.Request(
                            base?.programme()?.id() ?: id(),
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
            val proposal =
                base?.let {
                    StrengthProgrammeEditor.replaceFuture(
                        it.programme(),
                        candidate,
                        maxOf(
                            LocalDate.now(),
                            it.programme().effectiveFrom(),
                            it.programme().start(),
                        ),
                        Instant.now(),
                        programmeProtectedIds(it),
                        "Athlete reviewed regenerated future programming; completed and earlier workouts retain their exact prescriptions.",
                    )
                }
            val draft =
                ConcreteStrengthProgrammeDraft(
                    id(),
                    1,
                    base?.reference() ?: before.current,
                    proposal?.proposed() ?: candidate,
                    emptyList(),
                )
            repository.save(draft, 0)
            before.copy(draft = draft, notice = "Preview saved · activation is your choice.")
        }

    private fun edit(
        workoutId: String,
        done: () -> Unit = {},
        decisionReason: String? = null,
        change: (StrengthProgramme.Workout) -> StrengthProgramme.Workout,
    ) =
        command(done) { before ->
            check(!before.accepted && !before.superseded) {
                "Accepted programming is immutable. Prepare a future revision instead."
            }
            val old = before.draft ?: error("Open a preview first.")
            val workout = old.programme().workouts().first { it.id() == workoutId }
            if (old.programme().parentRevision() > 0) {
                val review = programmes.reviewRevision(old.before()!!)
                require(
                    !workout.date().isBefore(old.programme().effectiveFrom()) &&
                        workout.id() !in programmeProtectedIds(review)
                ) {
                    "Earlier, completed and active prescriptions are read only. Choose another future workout."
                }
            }
            val edited = ConcreteStrengthProgrammeEditor.replaceWorkout(old, change(workout))
            val next =
                if (decisionReason == null) edited
                else {
                    val p = edited.programme()
                    ConcreteStrengthProgrammeDraft(
                        edited.id(),
                        edited.revision(),
                        edited.before(),
                        StrengthProgramme(
                            p.id(),
                            p.revision(),
                            p.parentRevision(),
                            p.createdAt(),
                            p.name(),
                            p.origin(),
                            p.templateId(),
                            p.settings(),
                            p.blocks(),
                            p.effectiveFrom(),
                            decisionReason,
                            p.tradeoffs(),
                        ),
                        edited.substitutions(),
                    )
                }
            repository.save(next, old.revision())
            before.copy(
                draft = next,
                progressionReview = null,
                notice = "Saved this dated workout.",
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
            if (slotId != null && state.value.draft!!.programme().parentRevision() > 0) {
                val preserved =
                    programmes.reviewRevision(state.value.draft!!.before()!!).programme()
                require(
                    rows.first { it.id() == slotId }.exercise().id() == exercise.id() ||
                        !ConcreteStrengthProgrammeEditor.retainedIntent(
                            state.value.draft!!,
                            preserved,
                            workoutId,
                            slotId,
                        )
                ) {
                    "Use a reviewed programme substitution to change this exercise."
                }
            }
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

    private fun programmeSwapContext(
        current: StrengthGenerationState,
        review: ProgrammeSwapReview,
    ): ExerciseSubstitutionEngine.Context {
        val draft = current.draft ?: error("Open a saved future revision first.")
        require(
            !current.accepted &&
                !current.superseded &&
                draft.id() == review.draftId &&
                draft.revision() == review.draftRevision
        ) {
            "This preview changed. Reopen its replacement choices."
        }
        require(programmes.currentReference() == draft.before()) {
            "Current programming changed. Prepare a fresh revision."
        }
        val settings = draft.programme().settings()
        val choices = user.choices()
        val recent =
            SqliteGymExecutionRepository(getApplication<Application>())
                .recentActual(10)
                .flatMap { it.exercises().map { block -> block.exercise().id() } }
                .toSet()
        return ExerciseSubstitutionEngine.Context(
            review.original,
            settings.goal(),
            settings.experience(),
            settings.equipment(),
            user.exclusions().map { it.exerciseId() }.toSet() +
                choices.filter { it.archived() }.map { it.exerciseId() },
            choices.filter { it.favourite() && !it.archived() }.map { it.exerciseId() }.toSet(),
            recent,
            review.reason,
        )
    }

    fun openProgrammeSwap(workoutId: String, slotId: String) = command { before ->
        val draft = before.draft ?: error("Open a future programme revision first.")
        require(draft.programme().parentRevision() > 0) {
            "Edit unaccepted template exercises in the target editor."
        }
        val preserved = programmes.reviewRevision(draft.before()!!)
        val workout = draft.programme().workouts().first { it.id() == workoutId }
        require(
            !workout.date().isBefore(draft.programme().effectiveFrom()) &&
                workoutId !in programmeProtectedIds(preserved)
        ) {
            "Earlier and completed workouts are read only."
        }
        require(
            ConcreteStrengthProgrammeEditor.retainedIntent(
                draft,
                preserved.programme(),
                workoutId,
                slotId,
            )
        ) {
            "This newly drafted intent can be edited before activation."
        }
        val slot = workout.definition().slots().first { it.id() == slotId }
        val review =
            ProgrammeSwapReview(
                draft.id(),
                draft.revision(),
                workoutId,
                slotId,
                slot.exercise(),
                ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY,
            )
        val context = programmeSwapContext(before, review)
        before.copy(
            swapReview = review.copy(suggestions = ExerciseSubstitutionEngine.rank(context, 50))
        )
    }

    fun programmeSwapReason(reason: ExerciseSubstitutionEngine.Reason) = command { before ->
        val review =
            (before.swapReview ?: error("Open replacement choices first.")).copy(reason = reason)
        before.copy(
            swapReview =
                review.copy(
                    suggestions =
                        ExerciseSubstitutionEngine.rank(programmeSwapContext(before, review), 50)
                )
        )
    }

    fun closeProgrammeSwap() {
        if (!mutable.value.busy) mutable.update { it.copy(swapReview = null, error = null) }
    }

    fun useProgrammeSwap(replacementId: String, target: GymSlotInput?, allFuture: Boolean) =
        command { before ->
            val review = before.swapReview ?: error("Review replacement choices first.")
            val context = programmeSwapContext(before, review)
            val suggestion =
                ExerciseSubstitutionEngine.rank(context, 50).firstOrNull {
                    it.exercise().id() == replacementId
                } ?: error("This replacement is unavailable or excluded. Review the choices again.")
            val draft = before.draft!!
            val preserved = programmes.reviewRevision(draft.before()!!)
            val changedTarget =
                target?.let { gymSlotFromInput(review.slotId, suggestion.exercise(), it).target() }
            val next =
                ConcreteStrengthProgrammeEditor.substitute(
                    draft,
                    preserved.programme(),
                    review.workoutId,
                    review.slotId,
                    context,
                    replacementId,
                    changedTarget,
                    programmeProtectedIds(preserved),
                    allFuture,
                    Instant.now(),
                )
            repository.save(next, draft.revision())
            before.copy(
                draft = next,
                swapReview = null,
                notice = "Replacement proposal saved. Review and activate when ready.",
            )
        }

    private fun progressionAdvice(slot: GymWorkoutDefinition.Slot) =
        StrengthTargetProgression.review(
            slot,
            SqliteGymExecutionRepository(getApplication<Application>()).recentActual(20),
            Instant.now(),
        )

    fun reviewProgression(workoutId: String, slotId: String) = command { before ->
        require(!before.accepted && !before.superseded) { "Prepare a future revision first." }
        val draft = before.draft ?: error("Open a saved preview first.")
        val workout = draft.programme().workouts().first { it.id() == workoutId }
        require(
            !workout.date().isBefore(draft.programme().effectiveFrom()) &&
                workoutId !in before.protectedWorkoutIds
        ) {
            "Earlier, completed and active prescriptions are read only."
        }
        val slot = workout.definition().slots().first { it.id() == slotId }
        before.copy(
            progressionReview =
                ProgrammeProgressionReview(
                    draft.id(),
                    draft.revision(),
                    workoutId,
                    slot,
                    progressionAdvice(slot),
                )
        )
    }

    fun closeProgression() {
        if (!mutable.value.busy) mutable.update { it.copy(progressionReview = null, error = null) }
    }

    /** A reviewed optional reference/task is written only to this unaccepted dated proposal. */
    fun saveProgressionProposal() {
        val reviewed = mutable.value.progressionReview ?: return
        edit(
            reviewed.workoutId,
            decisionReason =
                "Athlete saved an optional target proposal for ${reviewed.slot.exercise().name()}. ${reviewed.advice.reason()} Supporting logged sessions: ${reviewed.advice.supportingSessionIds().joinToString(", ")}. Other dates and entered sets remain unchanged.",
        ) { workout ->
            val current = mutable.value.draft ?: error("Open this preview again.")
            require(
                current.id() == reviewed.draftId && current.revision() == reviewed.draftRevision
            ) {
                "The preview changed. Review progression again."
            }
            val slot = workout.definition().slots().first { it.id() == reviewed.slot.id() }
            require(slot == reviewed.slot) { "The target changed. Review progression again." }
            val fresh = progressionAdvice(slot)
            require(fresh == reviewed.advice && fresh.proposedTarget() != null) {
                "Supporting evidence changed or is insufficient. Review progression again."
            }
            withSlots(
                workout,
                workout.definition().slots().map {
                    if (it.id() == slot.id())
                        GymWorkoutDefinition.Slot(
                            it.id(),
                            it.exercise(),
                            fresh.proposedTarget(),
                            it.supersetGroup(),
                        )
                    else it
                },
            )
        }
    }

    fun openHistory() = command {
        it.copy(historyOpen = true, historyPage = 0, historicalProgramme = null)
    }

    fun historyPage(delta: Int) = command {
        it.copy(historyPage = (it.historyPage + delta).coerceAtLeast(0), historicalProgramme = null)
    }

    fun reviewHistorical(id: String, revision: Int) = command {
        it.copy(
            historicalProgramme =
                programmes.programme(id, revision)
                    ?: error("This preserved revision is unavailable.")
        )
    }

    fun historicalList() {
        if (!mutable.value.busy)
            mutable.update { it.copy(historicalProgramme = null, error = null) }
    }

    fun closeHistory() {
        if (!mutable.value.busy)
            mutable.update {
                it.copy(historyOpen = false, historicalProgramme = null, error = null)
            }
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
