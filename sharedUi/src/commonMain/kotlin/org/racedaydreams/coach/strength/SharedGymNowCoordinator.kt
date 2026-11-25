package org.racedaydreams.coach.strength

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.racedaydreams.coach.application.applicationIoDispatcher
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.presentation.MassText
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*
import org.racedaydreams.storage.*

/** Platform-owned saved input. Values are strings, integers or lists of strings; never actuals. */
interface GymNowRetainedState {
    operator fun get(key: String): Any?

    operator fun set(key: String, value: Any?)

    fun remove(key: String)

    fun strings(key: String): List<String>?
}

/**
 * Inputs are retained separately from actual evidence. A successful acknowledgement clears a set.
 */
data class GymSetDraft(
    val id: String,
    val kilograms: String = "",
    val repetitions: String = "",
    val seconds: String = "",
    val metres: String = "",
    val effort: String = "",
    val rir: Boolean = false,
    val warmUp: Boolean = false,
)

data class GymBuilderDraft(
    val id: String,
    val revision: Int = 1,
    val createdAt: Instant,
    val title: String = "",
    val slots: List<GymWorkoutDefinition.Slot> = emptyList(),
    val savedRevision: Int? = null,
)

data class GymNowState(
    val restPreferences: GymRestTimer.Preferences = GymRestTimer.Preferences.defaults(),
    val restTimer: GymRestTimer? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val programme: GymProgramme? = null,
    val authoredProgramme: StrengthProgramme? = null,
    val unfinished: List<GymExecutionRepository.Summary> = emptyList(),
    val favourites: List<GymWorkoutDefinition> = emptyList(),
    val favouritePage: Int = 0,
    val hasMoreFavourites: Boolean = false,
    val recent: List<StrengthSession> = emptyList(),
    val exercises: List<StrengthExercise> = emptyList(),
    val favouriteExerciseIds: Set<String> = emptySet(),
    val excludedExerciseIds: Set<String> = emptySet(),
    val archivedExerciseIds: Set<String> = emptySet(),
    val view: GymWorkoutSessionCoordinator.View? = null,
    val form: GymSetDraft,
    val builder: GymBuilderDraft,
    val generated: GymNowWorkouts.Suggestion? = null,
    val generatedSwap: GeneratedWorkoutSwap? = null,
    val finishEarlyRequired: Boolean = false,
    val substitution: GymSwapState? = null,
    val association: GymAssociationCoordinator.View? = null,
    val nativeRecentIds: Set<String> = emptySet(),
    val completedProgrammeSessions: Set<String> = emptySet(),
    val nearbyDemandingRunDates: Set<LocalDate> = emptySet(),
    /**
     * The user's mass unit setting. Loads are typed and shown in it; [GymSetDraft.kilograms] holds
     * the typed text in this unit and is converted to kilograms only when a set is saved (B-05).
     */
    val massUnit: MassUnit = MassUnit.KG,
)

data class GeneratedWorkoutSwap(
    val slotId: String,
    val choices: List<ExerciseSubstitutionEngine.Suggestion>,
)

/** Original Gym Now feature operations over one execution coordinator and one database pool. */
class SharedGymNowCoordinator(
    private val database: SqlDatabase,
    private val repository: GymExecutionRepository,
    associationRepository: GymAssociationRepository,
    private val programming: StrengthProgrammingRepository,
    private val coaching: CoachingRepository,
    private val currentProgramme: () -> GymProgramme?,
    private val programmeVersion: (String, Int) -> GymProgramme?,
    private val favouriteWorkouts: (Int, Int) -> List<GymWorkoutDefinition>,
    private val recentActual: (Int) -> List<StrengthSession>,
    private val finishedActualIds: (List<String>) -> Set<String>,
    private val retained: GymNowRetainedState,
    private val scope: CoroutineScope,
    private val ticks: GymWorkoutSessionCoordinator.TickSource,
    private val restNow: () -> GymRestTimer.Clock,
    private val syncRestAlert: (GymRestTimer?, GymRestTimer.Preferences) -> Unit,
    private val now: () -> Instant,
    private val offsetAt: (Instant) -> ZoneOffset,
    private val newId: () -> String,
    private val ioDispatcher: CoroutineDispatcher = applicationIoDispatcher(),
) {
    private fun newForm() = GymSetDraft(newId())

    private fun newBuilderDraft() = GymBuilderDraft(newId(), createdAt = now())

    private var savedRest: GymRestTimer? = null
    private val tools = SqliteGymTools(database)
    private val settings = SqliteSettings(database)
    private val userChoices = SqliteStrengthChoices(database)
    private val exclusions = SqliteExerciseExclusions(database)
    private val associations =
        GymAssociationCoordinator(associationRepository, { now() }, { newId() })
    private val mutex = Mutex()
    private var coordinator = GymWorkoutSessionCoordinator(repository, ticks)
    private val mutable =
        MutableStateFlow(
            GymNowState(
                builder = readBuilder(),
                form = (retained["gym.form.selected"] as? String)?.let(::readForm) ?: newForm(),
            )
        )
    val state: StateFlow<GymNowState> = mutable.asStateFlow()
    private var displayJob: Job? = null
    private var afterAcknowledgement: (() -> Unit)? = null
    private var terminalView: GymExecutionRepository.Stored? = null

    fun refresh() = command(allowPending = true) { Unit }

    fun clearMessage() {
        mutable.update { it.copy(error = null, notice = null) }
    }

    private data class Panel(
        val restPreferences: GymRestTimer.Preferences,
        val restTimer: GymRestTimer?,
        val programme: GymProgramme?,
        val authoredProgramme: StrengthProgramme?,
        val unfinished: List<GymExecutionRepository.Summary>,
        val favourites: List<GymWorkoutDefinition>,
        val hasMore: Boolean,
        val recent: List<StrengthSession>,
        val exercises: List<StrengthExercise>,
        val favouriteIds: Set<String>,
        val archivedIds: Set<String>,
        val excludedIds: Set<String>,
        val nativeRecentIds: Set<String>,
        val completedProgrammeSessions: Set<String>,
        val nearbyDemandingRunDates: Set<LocalDate>,
        val massUnit: MassUnit,
    )

    private fun panel(page: Int): Panel {
        val saved = favouriteWorkouts(21, page * 20)
        val choices = userChoices.choices()
        val recent = recentActual(10)
        val reference = programming.currentReference()
        val authored =
            if (reference?.owner() == StrengthProgrammingRepository.Owner.AUTHORED)
                programming.programme(reference.id(), reference.revision())
                    ?: error("The preserved authored programme is missing.")
            else null
        val observed = coordinator.view().observed()
        val executionId = observed?.id()
        savedRest
            ?.takeIf {
                it.executionId() != executionId ||
                    observed?.status() in
                        listOf(GymExecution.Status.FINISHED, GymExecution.Status.DISCARDED)
            }
            ?.let { syncRestAlert(it.skip(restNow()), tools.preferences()) }
        val rest = executionId?.let(tools::timer)
        return Panel(
            tools.preferences(),
            rest,
            if (authored == null) currentProgramme() else null,
            authored,
            repository.unfinished(),
            saved.take(20),
            saved.size > 20,
            recent,
            coaching.exercises(),
            choices.filter { it.favourite() }.map { it.exerciseId() }.toSet(),
            choices.filter { it.archived() }.map { it.exerciseId() }.toSet(),
            exclusions.exclusions().map { it.exerciseId() }.toSet(),
            finishedActualIds(recent.map { it.id() }),
            reference?.let { completedGymSessions(database, it.id()) } ?: emptySet(),
            nearbyDemandingRunDates(),
            runCatching { MassUnit.valueOf(settings.getSetting("mass.unit") ?: "KG") }
                .getOrDefault(MassUnit.KG),
        )
    }

    private fun nearbyDemandingRunDates(): Set<LocalDate> {
        val instant = now()
        val today = CalendarDates.atOffset(instant, offsetAt(instant))
        return coaching
            .current()
            ?.plan()
            ?.workouts()
            .orEmpty()
            .filter {
                it.date().toEpochDay() in
                    today.minusDays(1).toEpochDay()..today.plusDays(1).toEpochDay() &&
                    org.racedaydreams.domain.recovery.TrainingRecovery.demanding(it)
            }
            .map { it.date() }
            .toSet()
    }

    private fun command(
        onSuccess: () -> Unit = {},
        allowPending: Boolean = false,
        action: () -> Unit,
    ) {
        if (
            mutable.value.busy ||
                !allowPending &&
                    (mutable.value.view?.pending() != null ||
                        mutable.value.association?.pending() != null)
        )
            return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        scope.launch {
            var accepted = false
            var failure: String? = null
            var view: GymWorkoutSessionCoordinator.View? = null
            var fresh: Panel? = null
            try {
                withContext(ioDispatcher) {
                    mutex.withLock {
                        try {
                            action()
                            accepted = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            failure =
                                error.message
                                    ?: "The workout could not be saved. Your entered values are retained."
                        }
                        view =
                            coordinator.view().let { current ->
                                if (
                                    current.accepted() == null &&
                                        current.pending() == null &&
                                        terminalView != null
                                )
                                    GymWorkoutSessionCoordinator.View(
                                        terminalView,
                                        terminalView!!.execution(),
                                        null,
                                    )
                                else current
                            }
                        try {
                            val completed = view?.accepted()?.execution()
                            if (
                                completed?.status() == GymExecution.Status.FINISHED &&
                                    completed.origin() != GymExecution.Origin.PLANNED &&
                                    associations.view().actualId() != completed.id()
                            ) {
                                associations.review(completed.id())
                            }
                            fresh = panel(mutable.value.favouritePage)
                            if (fresh?.restTimer != null)
                                restNow() // initialise private epoch on IO
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (failure == null)
                                failure =
                                    error.message ?: "Saved workout information could not be read."
                        }
                    }
                }
                val snapshot = view
                savedRest = fresh?.restTimer
                mutable.update { previous ->
                    previous.copy(
                        restPreferences = fresh?.restPreferences ?: previous.restPreferences,
                        restTimer = savedRest?.observe(restNow()),
                        loading = false,
                        // Acknowledgement callbacks still own form cleanup. Keep input disabled
                        // until syncFormToSelection/onSuccess finish; finally publishes idle.
                        busy = true,
                        error = failure,
                        programme = if (fresh != null) fresh!!.programme else previous.programme,
                        authoredProgramme =
                            if (fresh != null) fresh!!.authoredProgramme
                            else previous.authoredProgramme,
                        unfinished = fresh?.unfinished ?: previous.unfinished,
                        favourites = fresh?.favourites ?: previous.favourites,
                        hasMoreFavourites = fresh?.hasMore ?: previous.hasMoreFavourites,
                        recent = fresh?.recent ?: previous.recent,
                        exercises = fresh?.exercises ?: previous.exercises,
                        favouriteExerciseIds = fresh?.favouriteIds ?: previous.favouriteExerciseIds,
                        archivedExerciseIds = fresh?.archivedIds ?: previous.archivedExerciseIds,
                        excludedExerciseIds = fresh?.excludedIds ?: previous.excludedExerciseIds,
                        view = snapshot,
                        association = associations.view(),
                        nativeRecentIds = fresh?.nativeRecentIds ?: previous.nativeRecentIds,
                        completedProgrammeSessions =
                            fresh?.completedProgrammeSessions
                                ?: previous.completedProgrammeSessions,
                        nearbyDemandingRunDates =
                            fresh?.nearbyDemandingRunDates ?: previous.nearbyDemandingRunDates,
                        massUnit = fresh?.massUnit ?: previous.massUnit,
                    )
                }
                syncFormToSelection()
                if (accepted && snapshot?.pending() == null) onSuccess()
                else if (snapshot?.pending() == null) afterAcknowledgement = null
            } finally {
                mutable.update { it.copy(busy = false, loading = false) }
            }
        }
    }

    fun startFree(onReady: () -> Unit) =
        start(onReady) {
            coordinator.start(newId(), GymExecution.Origin.FREE, null, null, offsetAt(now()))
        }

    fun startPlanned(sessionId: String, onReady: () -> Unit) {
        val selectedProgramme = mutable.value.programme
        val selectedAuthored = mutable.value.authoredProgramme
        start(onReady) {
            val reference = programming.currentReference()
            val prepared =
                if (selectedAuthored != null) {
                    require(
                        reference?.owner() == StrengthProgrammingRepository.Owner.AUTHORED &&
                            reference.id() == selectedAuthored.id() &&
                            reference.revision() == selectedAuthored.revision()
                    ) {
                        "The programme changed. Review the refreshed session before starting."
                    }
                    val programme =
                        programming.programme(reference.id(), reference.revision())
                            ?: error("The preserved authored programme is missing.")
                    GymWorkoutAdapter.fromAuthoredProgramme(programme, sessionId)
                } else {
                    val programme = currentProgramme() ?: error("There is no active gym programme.")
                    require(
                        selectedProgramme != null &&
                            selectedProgramme.id() == programme.id() &&
                            selectedProgramme.version() == programme.version() &&
                            reference?.owner() == StrengthProgrammingRepository.Owner.LEGACY
                    ) {
                        "The programme changed. Review the refreshed session before starting."
                    }
                    GymWorkoutAdapter.fromProgramme(
                        programme,
                        sessionId,
                        coaching.exercises().associateBy { it.id() },
                    )
                }
            require(sessionId !in completedGymSessions(database, prepared.source().id())) {
                "This programmed session is already completed."
            }
            coordinator.start(
                newId(),
                GymExecution.Origin.PLANNED,
                prepared.definition(),
                prepared.source(),
                offsetAt(now()),
            )
        }
    }

    fun startFavourite(definition: GymWorkoutDefinition, onReady: () -> Unit) =
        start(onReady) {
            coordinator.start(
                newId(),
                GymExecution.Origin.FAVOURITE,
                definition,
                GymExecution.SourceReference(
                    GymExecution.SourceKind.SAVED_WORKOUT,
                    definition.id(),
                    definition.revision(),
                    null,
                ),
                offsetAt(now()),
            )
        }

    fun repeatPrevious(actualId: String, onReady: () -> Unit) =
        start(onReady) {
            val previous =
                recentActual(20).firstOrNull { it.id() == actualId }
                    ?: error(
                        "This recent workout is no longer available. Refresh and choose again."
                    )
            val execution = repository.load(actualId)?.execution()
            val now = now()
            val id = newId()
            val prepared =
                if (execution != null && execution.status() == GymExecution.Status.FINISHED)
                    GymWorkoutAdapter.repeatExecution(id, 1, now, "Repeat workout", execution)
                else GymWorkoutAdapter.repeatActual(id, 1, now, "Repeat workout", previous)
            coordinator.start(
                newId(),
                GymExecution.Origin.REPEATED,
                prepared.definition(),
                prepared.source(),
                offsetAt(now),
            )
        }

    private fun start(onReady: () -> Unit, action: () -> Unit) {
        if (
            mutable.value.busy ||
                mutable.value.view?.pending() != null ||
                mutable.value.association?.pending() != null
        )
            return
        afterAcknowledgement = onReady
        command(
            onSuccess = {
                afterAcknowledgement?.invoke()
                afterAcknowledgement = null
            }
        ) {
            associations.clear()
            action()
            terminalView = null
            mutable.update { it.copy(finishEarlyRequired = false) }
        }
    }

    fun recover(id: String, onReady: () -> Unit) =
        start(onReady) { coordinator.recoverExplicitly(id) }

    fun discardUnfinished(id: String) = command {
        if (coordinator.view().accepted()?.execution()?.id() != id)
            coordinator.recoverExplicitly(id)
        coordinator.discard()
    }

    fun favouritePage(delta: Int) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        val page = (mutable.value.favouritePage + delta).coerceIn(0, 499)
        if (page == mutable.value.favouritePage) return
        mutable.update { it.copy(favouritePage = page) }
        refresh()
    }

    fun select(slotId: String) = command { coordinator.selectExercise(slotId) }

    fun addExercise(exercise: StrengthExercise) = command {
        coordinator.addExercises(listOf(GymWorkoutDefinition.Slot(newId(), exercise, null, null)))
    }

    fun openSubstitutions(reason: ExerciseSubstitutionEngine.Reason) = command {
        val execution =
            coordinator.view().accepted()?.execution()
                ?: error("Open your active workout before choosing a replacement.")
        require(execution.status() == GymExecution.Status.ACTIVE) {
            "Resume the workout before choosing a replacement."
        }
        val slot = execution.slots().first { it.id() == execution.selectedSlotId() }
        val source = execution.source()
        val authored =
            if (source?.kind() == GymExecution.SourceKind.PROGRAMME)
                programming.programme(source.id(), source.revision())?.settings()
            else null
        val preserved =
            if (source?.kind() == GymExecution.SourceKind.PROGRAMME && authored == null)
                programmeVersion(source.id(), source.revision())?.settings()
            else null
        val available =
            authored?.equipment() ?: initialSwapEquipment(preserved?.equipment() ?: emptySet())
        val prepared =
            GymSwapState(
                execution.id(),
                slot.id(),
                slot.exercise(),
                authored?.goal() ?: preserved?.goal() ?: GymProgramme.Goal.GENERAL,
                authored?.experience()
                    ?: preserved?.experience()
                    ?: GymProgramme.Experience.BEGINNER,
                available,
                if (authored != null || preserved != null)
                    "Based on this workout's preserved programme settings. Check which equipment is available for this swap."
                else
                    "No preserved programme preferences are available. General-fitness and beginner-compatible alternatives are shown; choose the equipment available for this swap.",
                reason,
                emptyList(),
            )
        val suggestions = rankSubstitutions(prepared)
        mutable.update { it.copy(substitution = prepared.copy(suggestions = suggestions)) }
    }

    fun changeSubstitutionReason(reason: ExerciseSubstitutionEngine.Reason) = command {
        val current = mutable.value.substitution ?: error("Open the replacement choices first.")
        val next = current.copy(reason = reason, visibleLimit = 4)
        val suggestions = rankSubstitutions(next)
        mutable.update { it.copy(substitution = next.copy(suggestions = suggestions)) }
    }

    fun changeSubstitutionEquipment(equipment: Set<ExerciseMetadata.Equipment>) = command {
        val current = mutable.value.substitution ?: error("Open the replacement choices first.")
        val next = current.copy(equipment = equipment.toSet(), visibleLimit = 4)
        val suggestions = rankSubstitutions(next)
        mutable.update { it.copy(substitution = next.copy(suggestions = suggestions)) }
    }

    fun showMoreSubstitutions() {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        mutable.update { state ->
            state.copy(
                substitution =
                    state.substitution?.let {
                        it.copy(visibleLimit = (it.visibleLimit + 6).coerceAtMost(50))
                    }
            )
        }
    }

    fun closeSubstitutions() {
        if (mutable.value.busy) return
        mutable.update { it.copy(substitution = null) }
    }

    /** The athlete confirms this command. No programme or saved favourite is rewritten. */
    fun replaceForToday(replacement: StrengthExercise) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        val choice = mutable.value.substitution ?: return
        val key = formKey(choice.slotId)
        afterAcknowledgement = {
            val clean = newForm()
            saveForm(key, clean)
            mutable.update {
                it.copy(
                    form = clean,
                    notice =
                        "Changed to ${replacement.name()} for today only. Previous sets and the original prescription are preserved. Choose a fresh load or task.",
                )
            }
        }
        // A failed write must expose the normal exact-retry controls, not leave a dialog over them.
        mutable.update { it.copy(substitution = null) }
        command(
            onSuccess = {
                afterAcknowledgement?.invoke()
                afterAcknowledgement = null
            }
        ) {
            val execution =
                coordinator.view().accepted()?.execution()
                    ?: error("The workout is no longer active.")
            require(
                execution.id() == choice.executionId &&
                    execution.status() == GymExecution.Status.ACTIVE &&
                    execution.selectedSlotId() == choice.slotId &&
                    execution.slots().any {
                        it.id() == choice.slotId && it.exercise() == choice.original
                    }
            ) {
                "The selected exercise changed. Review replacement choices again."
            }
            require(rankSubstitutions(choice).any { it.exercise() == replacement }) {
                "That alternative no longer matches these choices. Review the options again."
            }
            coordinator.replaceExercise(choice.slotId, replacement)
        }
    }

    private fun rankSubstitutions(
        choice: GymSwapState
    ): List<ExerciseSubstitutionEngine.Suggestion> {
        val choices = userChoices.choices()
        return ExerciseSubstitutionEngine.rank(
            ExerciseSubstitutionEngine.Context(
                choice.original,
                choice.goal,
                choice.experience,
                choice.equipment,
                choices.filter { it.archived() }.map { it.exerciseId() }.toSet() +
                    exclusions.exclusions().map { it.exerciseId() },
                choices.filter { it.favourite() }.map { it.exerciseId() }.toSet(),
                mutable.value.recent
                    .flatMap { session -> session.exercises().map { it.exercise().id() } }
                    .toSet(),
                choice.reason,
            ),
            50,
        )
    }

    private fun initialSwapEquipment(labels: Set<String>) = GymNowWorkouts.capabilities(labels)

    fun pause() = command { coordinator.pause() }

    fun resume() = command { coordinator.resume() }

    fun discard() = command { coordinator.discard() }

    fun retry() =
        command(
            allowPending = true,
            onSuccess = {
                val callback = afterAcknowledgement
                afterAcknowledgement = null
                callback?.invoke()
            },
        ) {
            if (associations.view().pending() != null) associations.retry()
            else coordinator.retryPending()
        }

    fun reviewAssociation(
        proposal: GymWorkoutAssociation.Proposal,
        choice: GymWorkoutAssociation.Choice,
    ) = command { associations.choose(proposal, choice) }

    fun refreshAssociation() = command { associations.reloadSaved() }

    fun openCompleted(id: String, onReady: () -> Unit) =
        command(onSuccess = onReady) {
            require(repository.unfinished().isEmpty()) {
                "Finish or discard the active workout before opening completed evidence."
            }
            val stored = repository.load(id) ?: error("Recorded workout is no longer available.")
            require(stored.execution().status() == GymExecution.Status.FINISHED) {
                "Choose a completed workout."
            }
            associations.clear()
            if (stored.execution().origin() != GymExecution.Origin.PLANNED) associations.review(id)
            coordinator = GymWorkoutSessionCoordinator(repository, ticks)
            terminalView = stored
        }

    /** Explicit escape from an unrecoverable stale-write conflict, never a silent pending drop. */
    fun reloadSavedEvidence() =
        command(allowPending = true) {
            if (associations.view().pending() != null) {
                associations.reloadSaved()
                mutable.update {
                    it.copy(
                        notice =
                            "Saved association reloaded. Review the current choice before changing it."
                    )
                }
                return@command
            }
            val pending = coordinator.view().pending() ?: error("There is no unacknowledged write.")
            val stored = repository.load(pending.snapshot().id())
            val replacement = GymWorkoutSessionCoordinator(repository, ticks)
            if (
                stored != null &&
                    stored.execution().status() != GymExecution.Status.FINISHED &&
                    stored.execution().status() != GymExecution.Status.DISCARDED
            )
                replacement.recoverExplicitly(stored.execution().id())
            coordinator = replacement
            terminalView = stored?.takeIf { !unfinished(it.execution()) }
            afterAcknowledgement = null
            mutable.update {
                it.copy(
                    notice =
                        "Saved workout reloaded. Your last entry is still in the form; check it before saving again."
                )
            }
        }

    fun updateForm(update: (GymSetDraft) -> GymSetDraft) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        val next = update(mutable.value.form)
        mutable.update { it.copy(form = next) }
        selectedSlot()?.let { saveForm(formKey(it.id()), next) }
    }

    fun restPreferences(value: GymRestTimer.Preferences) = command {
        tools.preferences(value)
        savedRest?.let { syncRestAlert(it.observe(restNow()), value) }
    }

    fun restAction(action: String) = command {
        val execution = coordinator.view().accepted()?.execution() ?: error("Open a workout first.")
        val slot = execution.slots().firstOrNull { it.id() == execution.selectedSlotId() }
        val now = restNow()
        val previous = tools.timer(execution.id())
        val next =
            tools.change(execution.id()) { old ->
                when (action) {
                    "start" ->
                        if (old?.observe(now)?.phase() == GymRestTimer.Phase.RUNNING)
                            old.observe(now)
                        else
                            GymRestTimer.start(
                                execution.id(),
                                newId(),
                                GymRestTimer.suggestedSeconds(
                                    slot?.target()?.restSeconds(),
                                    tools.preferences(),
                                ),
                                now,
                            )
                    "pause" -> requireNotNull(old).pause(now)
                    "resume" -> requireNotNull(old).resume(now)
                    "reset" -> requireNotNull(old).reset(now)
                    "+30" -> requireNotNull(old).adjust(30, now)
                    "-30" -> requireNotNull(old).adjust(-30, now)
                    "skip" -> requireNotNull(old).skip(now)
                    else -> error("Unknown rest action.")
                }
            }
        if (previous != null) syncRestAlert(previous.skip(now), tools.preferences())
        syncRestAlert(next, tools.preferences())
    }

    fun warmupSuggestion(): GymWarmup.Suggestion? {
        val execution = mutable.value.view?.observed() ?: return null
        val slot = selectedSlot() ?: return null
        return GymWarmup.suggest(
            slot,
            mutable.value.recent,
            execution.actualSets(),
            mutable.value.authoredProgramme?.settings()?.experience()
                ?: mutable.value.programme?.settings()?.experience()
                ?: GymProgramme.Experience.BEGINNER,
            now(),
        )
    }

    fun prepareWarmup(ramp: GymWarmup.Ramp?) = updateForm {
        it.copy(
            warmUp = true,
            // In the user's unit, rounded down to a loadable plate step (B-05, B-R8).
            kilograms =
                ramp?.let { r ->
                    MassText.warmup(r.kilograms(), mutable.value.massUnit).let { value ->
                        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                    }
                } ?: it.kilograms,
            repetitions = ramp?.repetitions()?.toString() ?: it.repetitions,
        )
    }

    fun nextSuperset() = command {
        GymSupersets.next(coordinator.view().observed() ?: error("Open a workout first."))?.let {
            coordinator.selectExercise(it.slotId())
        }
    }

    fun skipMovement() = command {
        val id = coordinator.view().observed()?.selectedSlotId() ?: error("Choose an exercise.")
        coordinator.skipExercise(id)
        GymSupersets.next(coordinator.view().observed() ?: error("Open a workout first."))?.let {
            coordinator.selectExercise(it.slotId())
        }
    }

    fun breakSuperset() = command {
        coordinator.breakSuperset(
            coordinator.view().observed()?.selectedSlotId() ?: error("Choose an exercise.")
        )
    }

    fun saveSet() {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        val slot = selectedSlot() ?: return
        val draft = mutable.value.form
        val unit = mutable.value.massUnit
        val formKey = formKey(slot.id())
        afterAcknowledgement = {
            val clean = newForm()
            saveForm(formKey, clean)
            mutable.update { it.copy(form = clean) }
        }
        command(
            onSuccess = {
                afterAcknowledgement?.invoke()
                afterAcknowledgement = null
            }
        ) {
            val profile = slot.exercise().measurement()
            val effort = optionalNumber(draft.effort, "effort")
            val useRir = profile.usesReps() && draft.rir
            val actual =
                StrengthSession.SetEntry(
                    draft.id,
                    if (profile.usesReps()) whole(draft.repetitions, "repetitions") else 0,
                    if (
                        profile.usesLoad() &&
                            slot.exercise().mode() != StrengthExercise.Mode.BODYWEIGHT
                    )
                        gymLoadKilograms(draft.kilograms, unit)
                    else 0.0,
                    if (useRir) null else effort,
                    draft.warmUp,
                    if (profile.usesDuration()) wholeLong(draft.seconds, "duration in seconds")
                    else null,
                    if (profile.usesDistance()) number(draft.metres, "distance in metres")
                    else null,
                    if (useRir) effort else null,
                )
            coordinator.saveSet(slot.id(), actual)
        }
    }

    fun finish(early: Boolean = false) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        if (!early && mutable.value.view?.observed()?.hasUnfinishedTargets() == true) {
            mutable.update { it.copy(finishEarlyRequired = true) }
            return
        }
        // The choice has been made. A failed write must expose the exact-retry controls,
        // rather than leaving a confirmation dialog over an unacknowledged finish.
        mutable.update { it.copy(finishEarlyRequired = false) }
        command { coordinator.finish(null, "", early) }
    }

    fun clearFinishPrompt() {
        mutable.update { it.copy(finishEarlyRequired = false) }
    }

    fun setWorkoutVisible(visible: Boolean) {
        if (!visible) {
            displayJob?.cancel()
            displayJob = null
            val current = mutable.value.view
            if (
                !mutable.value.busy &&
                    current?.pending() == null &&
                    current?.observed()?.let(::unfinished) == true
            )
                command { coordinator.checkpoint() }
            return
        }
        if (displayJob != null) return
        displayJob =
            scope.launch {
                while (true) {
                    delay(1000)
                    if (!mutable.value.busy) {
                        try {
                            observeWorkoutTime()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            mutable.update {
                                it.copy(
                                    error =
                                        error.message
                                            ?: "The workout clock needs review. Saved sets remain on this device."
                                )
                            }
                            break
                        }
                    }
                }
            }
    }

    /** Display ticks never replace a saved terminal projection with an empty coordinator. */
    suspend fun observeWorkoutTime() {
        fun canObserve() =
            !mutable.value.busy && mutable.value.view?.observed()?.let(::unfinished) == true
        if (!canObserve()) return
        mutex.withLock {
            if (!canObserve()) return@withLock
            val observed = withContext(ioDispatcher) { coordinator.observe() }
            // A command can begin while observation is in flight. It owns the next snapshot.
            if (canObserve())
                mutable.update {
                    it.copy(view = observed, restTimer = savedRest?.observe(restNow()))
                }
        }
    }

    private var generatedRequest: GymNowWorkouts.Request? = null

    fun generate(request: GymNowWorkouts.Request) = command {
        generatedRequest = request
        mutable.update { it.copy(generated = null, generatedSwap = null) }
        val suggestion =
            GymNowWorkouts.generate(
                newId(),
                now(),
                request,
                mutable.value.favouriteExerciseIds,
                mutable.value.archivedExerciseIds + exclusions.exclusions().map { it.exerciseId() },
            )
        mutable.update { it.copy(generated = suggestion) }
    }

    fun openGeneratedAlternatives(slotId: String) = command {
        val definition = mutable.value.generated?.definition() ?: return@command
        val request = generatedRequest ?: return@command
        val slot = definition.slots().firstOrNull { it.id() == slotId } ?: return@command
        val options =
            GymNowWorkouts.alternatives(
                request,
                definition,
                slot,
                mutable.value.archivedExerciseIds + mutable.value.excludedExerciseIds,
                mutable.value.favouriteExerciseIds,
            )
        mutable.update { it.copy(generatedSwap = GeneratedWorkoutSwap(slotId, options)) }
    }

    fun closeGeneratedAlternatives() {
        mutable.update { it.copy(generatedSwap = null) }
    }

    fun replaceGenerated(slotId: String, replacement: StrengthExercise) = command {
        val state = mutable.value
        val current = requireNotNull(state.generated)
        val review = requireNotNull(state.generatedSwap)
        require(review.slotId == slotId && review.choices.any { it.exercise() == replacement })
        val definition = current.definition()
        val slots =
            definition.slots().map { slot ->
                if (slot.id() == slotId)
                    slot.copy(
                        exercise = replacement,
                        target = slot.target()?.withoutLoadReference(),
                    )
                else slot
            }
        mutable.update {
            it.copy(
                generated = current.copy(definition = definition.copy(slots = slots)),
                generatedSwap = null,
            )
        }
    }

    fun startGenerated(quick: Boolean, onReady: () -> Unit) =
        start(onReady) {
            val definition =
                mutable.value.generated?.definition()
                    ?: error("Generate and review a workout first.")
            coordinator.start(
                newId(),
                if (quick) GymExecution.Origin.QUICK else GymExecution.Origin.GENERATED,
                definition,
                null,
                offsetAt(now()),
            )
        }

    fun newBuilder() {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        publishBuilder(newBuilderDraft())
    }

    fun editFavourite(definition: GymWorkoutDefinition) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        publishBuilder(
            GymBuilderDraft(
                definition.id(),
                definition.revision() + 1,
                now(),
                definition.title(),
                definition.slots(),
                definition.revision(),
            )
        )
    }

    fun builderPair(slotId: String) = changeBuilder { draft ->
        val from = draft.slots.indexOfFirst { it.id() == slotId }
        draft.copy(slots = GymSupersets.group(draft.slots, from, 2, newId()))
    }

    fun builderExtend(slotId: String) = changeBuilder { draft ->
        val at = draft.slots.indexOfFirst { it.id() == slotId }
        val group = draft.slots.getOrNull(at)?.supersetGroup() ?: error("Choose a superset.")
        val first = draft.slots.indexOfFirst { it.supersetGroup() == group }
        val last = draft.slots.indexOfLast { it.supersetGroup() == group }
        require(last < draft.slots.lastIndex && draft.slots[last + 1].supersetGroup() == null) {
            "Choose an adjacent ungrouped exercise."
        }
        draft.copy(
            slots =
                GymSupersets.group(
                    GymSupersets.ungroup(draft.slots, group),
                    first,
                    last - first + 2,
                    group,
                )
        )
    }

    fun builderUngroup(group: String) = changeBuilder {
        it.copy(slots = GymSupersets.ungroup(it.slots, group))
    }

    fun builderTitle(title: String) = changeBuilder { it.copy(title = title) }

    fun builderRemove(slotId: String) = changeBuilder {
        val group = it.slots.firstOrNull { slot -> slot.id() == slotId }?.supersetGroup()
        val rows = if (group == null) it.slots else GymSupersets.ungroup(it.slots, group)
        it.copy(slots = rows.filterNot { slot -> slot.id() == slotId })
    }

    fun builderMove(slotId: String, delta: Int) = changeBuilder { draft ->
        val rows = draft.slots.toMutableList()
        val from = rows.indexOfFirst { it.id() == slotId }
        val to = from + delta
        if (from < 0 || to !in rows.indices) draft
        else {
            val previous = rows[from]
            rows[from] = rows[to]
            rows[to] = previous
            draft.copy(slots = rows)
        }
    }

    fun builderDuplicate(slotId: String) = changeBuilder { draft ->
        val rows = draft.slots.toMutableList()
        val at = rows.indexOfFirst { it.id() == slotId }
        if (at < 0) draft
        else {
            val slot = rows[at]
            rows.add(
                at + 1,
                GymWorkoutDefinition.Slot(newId(), slot.exercise(), slot.target(), null),
            )
            draft.copy(slots = rows)
        }
    }

    fun builderPutSlot(slotId: String?, exercise: StrengthExercise, input: GymSlotInput): Boolean {
        return try {
            val slot = gymSlotFromInput(slotId, exercise, input, newId)
            changeBuilder { draft ->
                val rows = draft.slots.toMutableList()
                val at = rows.indexOfFirst { it.id() == slot.id() }
                if (at < 0) rows.add(slot)
                else
                    rows[at] =
                        GymWorkoutDefinition.Slot(
                            slot.id(),
                            slot.exercise(),
                            slot.target(),
                            rows[at].supersetGroup(),
                        )
                draft.copy(slots = rows)
            }
        } catch (error: IllegalArgumentException) {
            mutable.update { it.copy(error = error.message) }
            false
        }
    }

    fun saveBuilderFavourite() =
        command(
            onSuccess = {
                val current = mutable.value.builder
                publishBuilder(current.copy(savedRevision = current.revision))
                mutable.update {
                    it.copy(
                        notice =
                            "Favourite workout saved. Previous revisions and actual workouts are unchanged."
                    )
                }
            }
        ) {
            val definition = builderDefinition()
            repository.saveWorkout(definition, null)
            repository.savePreference(
                GymExecutionRepository.Preference(definition.id(), definition.revision(), true)
            )
        }

    fun removeFavourite(definition: GymWorkoutDefinition) = command {
        repository.savePreference(
            GymExecutionRepository.Preference(definition.id(), definition.revision(), false)
        )
    }

    fun startBuilder(onReady: () -> Unit) =
        start(onReady) {
            coordinator.start(
                newId(),
                GymExecution.Origin.CUSTOM,
                builderDefinition(),
                null,
                offsetAt(now()),
            )
        }

    private fun builderDefinition(): GymWorkoutDefinition {
        val draft = mutable.value.builder
        return GymWorkoutDefinition(
            draft.id,
            draft.revision,
            draft.createdAt,
            draft.title.trim(),
            draft.slots,
        )
    }

    private fun changeBuilder(change: (GymBuilderDraft) -> GymBuilderDraft): Boolean {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return false
        try {
            var current = mutable.value.builder
            if (current.savedRevision == current.revision)
                current = current.copy(revision = current.revision + 1, createdAt = now())
            val next = change(current)
            if (next.slots.isNotEmpty())
                GymWorkoutDefinition(
                    next.id,
                    next.revision,
                    next.createdAt,
                    "Workout draft",
                    next.slots,
                )
            publishBuilder(next)
            return true
        } catch (error: IllegalArgumentException) {
            mutable.update { it.copy(error = error.message) }
            return false
        }
    }

    private fun publishBuilder(draft: GymBuilderDraft) {
        mutable.update { it.copy(builder = draft, error = null, notice = null) }
        retained["gym.builder.title"] = draft.title
        retained["gym.builder.saved"] = draft.savedRevision
        if (draft.slots.isNotEmpty())
            retained["gym.builder.payload"] =
                FrozenGymWorkoutJson.encode(
                    GymWorkoutDefinition(
                        draft.id,
                        draft.revision,
                        draft.createdAt,
                        draft.title.takeIf { it.isNotBlank() && it.length <= 120 }
                            ?: "Workout draft",
                        draft.slots,
                    )
                )
        else retained.remove("gym.builder.payload")
    }

    private fun readBuilder(): GymBuilderDraft =
        try {
            (retained["gym.builder.payload"] as? String)?.let { payload ->
                val definition = FrozenGymWorkoutJson.decode(payload)
                GymBuilderDraft(
                    definition.id(),
                    definition.revision(),
                    definition.createdAt(),
                    (retained["gym.builder.title"] as? String) ?: definition.title(),
                    definition.slots(),
                    retained["gym.builder.saved"] as? Int,
                )
            } ?: newBuilderDraft().copy(title = retained["gym.builder.title"] as? String ?: "")
        } catch (_: IllegalArgumentException) {
            newBuilderDraft()
        }

    private fun selectedSlot(): GymWorkoutDefinition.Slot? {
        val current = mutable.value.view?.observed() ?: return null
        return current.slots().firstOrNull { it.id() == current.selectedSlotId() }
    }

    private fun syncFormToSelection() {
        val slot = selectedSlot() ?: return
        val current = mutable.value.form
        val key = formKey(slot.id())
        val selected = (retained["gym.form.selected"] as? String)
        if (selected != key) {
            if (selected != null) saveForm(selected, current)
            mutable.update { it.copy(form = readForm(key)) }
            retained["gym.form.selected"] = key
        }
    }

    private fun formKey(slotId: String): String = "${mutable.value.view?.observed()?.id()}::$slotId"

    private fun saveForm(slotId: String, form: GymSetDraft) {
        retained["gym.form.$slotId"] =
            listOf(
                form.id,
                form.kilograms,
                form.repetitions,
                form.seconds,
                form.metres,
                form.effort,
                form.rir.toString(),
                form.warmUp.toString(),
            )
    }

    private fun readForm(slotId: String): GymSetDraft {
        val values = retained.strings("gym.form.$slotId") ?: return newForm()
        return if (values.size == 8)
            GymSetDraft(
                values[0],
                values[1],
                values[2],
                values[3],
                values[4],
                values[5],
                values[6].toBooleanStrictOrNull() ?: false,
                values[7].toBooleanStrictOrNull() ?: false,
            )
        else newForm()
    }

    companion object {
        private fun unfinished(execution: GymExecution) =
            execution.status() != GymExecution.Status.FINISHED &&
                execution.status() != GymExecution.Status.DISCARDED

        private fun number(value: String, label: String): Double =
            value.trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("Enter $label as a number.")

        private fun whole(value: String, label: String): Int =
            value.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Enter $label as a whole number.")

        private fun wholeLong(value: String, label: String): Long =
            value.trim().toLongOrNull()
                ?: throw IllegalArgumentException("Enter $label as a whole number.")

        private fun optionalNumber(value: String, label: String): Double? =
            if (value.isBlank()) null else number(value, label)

        private fun optionalWhole(value: String, label: String): Int? =
            if (value.isBlank()) null else whole(value, label)
    }
}
