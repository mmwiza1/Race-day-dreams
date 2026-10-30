package org.racedaydreams.coach.strength

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.GymWorkoutJson
import org.racedaydreams.data.SqliteGymAssociationRepository
import org.racedaydreams.data.SqliteGymExecutionRepository
import org.racedaydreams.data.SqliteStrengthProgrammingRepository
import org.racedaydreams.data.StrengthUserStore
import org.racedaydreams.domain.strength.ExerciseMetadata
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine
import org.racedaydreams.domain.strength.GymAssociationCoordinator
import org.racedaydreams.domain.strength.GymExecution
import org.racedaydreams.domain.strength.GymExecutionRepository
import org.racedaydreams.domain.strength.GymNowWorkouts
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymWorkoutAdapter
import org.racedaydreams.domain.strength.GymWorkoutAssociation
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.GymWorkoutSessionCoordinator
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthProgramme
import org.racedaydreams.domain.strength.StrengthProgrammingRepository
import org.racedaydreams.domain.strength.StrengthSession

/**
 * Inputs are retained separately from actual evidence. A successful acknowledgement clears a set.
 */
data class GymSetDraft(
    val id: String = UUID.randomUUID().toString(),
    val kilograms: String = "",
    val repetitions: String = "",
    val seconds: String = "",
    val metres: String = "",
    val effort: String = "",
    val rir: Boolean = false,
    val warmUp: Boolean = false,
)

data class GymSlotInput(
    val open: Boolean = false,
    val sets: String = "3",
    val minimumReps: String = "",
    val maximumReps: String = "",
    val seconds: String = "",
    val metres: String = "",
    val minimumEffort: String = "",
    val maximumEffort: String = "",
    val rir: Boolean = false,
    val restSeconds: String = "",
    val referenceKilograms: String = "",
)

data class GymBuilderDraft(
    val id: String = UUID.randomUUID().toString(),
    val revision: Int = 1,
    val createdAt: Instant = Instant.now(),
    val title: String = "",
    val slots: List<GymWorkoutDefinition.Slot> = emptyList(),
    val savedRevision: Int? = null,
)

/** A prepared, bounded today-only choice. None of these capabilities change saved gym settings. */
data class GymSwapState(
    val executionId: String,
    val slotId: String,
    val original: StrengthExercise,
    val goal: GymProgramme.Goal,
    val experience: GymProgramme.Experience,
    val equipment: Set<ExerciseMetadata.Equipment>,
    val contextExplanation: String,
    val reason: ExerciseSubstitutionEngine.Reason,
    val suggestions: List<ExerciseSubstitutionEngine.Suggestion>,
    val visibleLimit: Int = 4,
)

data class GymNowState(
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
    val form: GymSetDraft = GymSetDraft(),
    val builder: GymBuilderDraft = GymBuilderDraft(),
    val generated: GymNowWorkouts.Suggestion? = null,
    val finishEarlyRequired: Boolean = false,
    val substitution: GymSwapState? = null,
    val association: GymAssociationCoordinator.View? = null,
    val nativeRecentIds: Set<String> = emptySet(),
)

/**
 * Feature-owned orchestration. The domain coordinator owns execution and pending-write semantics.
 * All coordinator/repository calls run on IO under one lock; Compose receives immutable read state.
 */
class GymNowViewModel(application: Application, private val retained: SavedStateHandle) :
    AndroidViewModel(application) {
    private val repository = SqliteGymExecutionRepository(application)
    private val associations =
        GymAssociationCoordinator(SqliteGymAssociationRepository(application), Clock.systemUTC()) {
            UUID.randomUUID().toString()
        }
    private val coaching = CoachingStore(application)
    private val programmes = GymProgrammeStore(application)
    private val programming = SqliteStrengthProgrammingRepository(application)
    private val userChoices = StrengthUserStore(application)
    private val mutex = Mutex()
    private val ticks =
        GymWorkoutSessionCoordinator.TickSource {
            GymExecution.Tick(PROCESS_CLOCK, SystemClock.elapsedRealtime(), Instant.now())
        }
    private var coordinator = GymWorkoutSessionCoordinator(repository, ticks)
    private val mutable =
        MutableStateFlow(
            GymNowState(
                builder = readBuilder(),
                form = retained.get<String>("gym.form.selected")?.let(::readForm) ?: GymSetDraft(),
            )
        )
    val state: StateFlow<GymNowState> = mutable.asStateFlow()
    private var displayJob: Job? = null
    private var afterAcknowledgement: (() -> Unit)? = null
    private var terminalView: GymExecutionRepository.Stored? = null

    init {
        refresh()
    }

    fun refresh() = command(allowPending = true) { Unit }

    fun clearMessage() {
        mutable.update { it.copy(error = null, notice = null) }
    }

    private data class Panel(
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
    )

    private fun panel(page: Int): Panel {
        val saved = repository.favouriteWorkouts(21, page * 20)
        val choices = userChoices.choices()
        val recent = repository.recentActual(10)
        val reference = programming.currentReference()
        val authored =
            if (reference?.owner() == StrengthProgrammingRepository.Owner.AUTHORED)
                programming.programme(reference.id(), reference.revision())
                    ?: error("The preserved authored programme is missing.")
            else null
        return Panel(
            if (authored == null) programmes.current() else null,
            authored,
            repository.unfinished(),
            saved.take(20),
            saved.size > 20,
            recent,
            coaching.exercises(),
            choices.filter { it.favourite() }.map { it.exerciseId() }.toSet(),
            choices.filter { it.archived() }.map { it.exerciseId() }.toSet(),
            userChoices.exclusions().map { it.exerciseId() }.toSet(),
            repository.finishedActualIds(recent.map { it.id() }),
        )
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
        viewModelScope.launch {
            var accepted = false
            var failure: String? = null
            var view: GymWorkoutSessionCoordinator.View? = null
            var fresh: Panel? = null
            try {
                withContext(Dispatchers.IO) {
                    mutex.withLock {
                        try {
                            action()
                            accepted = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: RuntimeException) {
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
                        } catch (error: RuntimeException) {
                            if (failure == null)
                                failure =
                                    error.message ?: "Saved workout information could not be read."
                        }
                    }
                }
                val snapshot = view
                mutable.update { previous ->
                    previous.copy(
                        loading = false,
                        busy = false,
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
            coordinator.start(
                UUID.randomUUID().toString(),
                GymExecution.Origin.FREE,
                null,
                null,
                ZoneId.systemDefault().rules.getOffset(Instant.now()),
            )
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
                    val programme =
                        programmes.current() ?: error("There is no active gym programme.")
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
            coordinator.start(
                UUID.randomUUID().toString(),
                GymExecution.Origin.PLANNED,
                prepared.definition(),
                prepared.source(),
                ZoneId.systemDefault().rules.getOffset(Instant.now()),
            )
        }
    }

    fun startFavourite(definition: GymWorkoutDefinition, onReady: () -> Unit) =
        start(onReady) {
            coordinator.start(
                UUID.randomUUID().toString(),
                GymExecution.Origin.FAVOURITE,
                definition,
                GymExecution.SourceReference(
                    GymExecution.SourceKind.SAVED_WORKOUT,
                    definition.id(),
                    definition.revision(),
                    null,
                ),
                ZoneId.systemDefault().rules.getOffset(Instant.now()),
            )
        }

    fun repeatPrevious(actualId: String, onReady: () -> Unit) =
        start(onReady) {
            val previous =
                repository.recentActual(20).firstOrNull { it.id() == actualId }
                    ?: error(
                        "This recent workout is no longer available. Refresh and choose again."
                    )
            val execution = repository.load(actualId)?.execution()
            val now = Instant.now()
            val id = UUID.randomUUID().toString()
            val prepared =
                if (execution != null && execution.status() == GymExecution.Status.FINISHED)
                    GymWorkoutAdapter.repeatExecution(id, 1, now, "Repeat workout", execution)
                else GymWorkoutAdapter.repeatActual(id, 1, now, "Repeat workout", previous)
            coordinator.start(
                UUID.randomUUID().toString(),
                GymExecution.Origin.REPEATED,
                prepared.definition(),
                prepared.source(),
                ZoneId.systemDefault().rules.getOffset(now),
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
        coordinator.addExercises(
            listOf(GymWorkoutDefinition.Slot(UUID.randomUUID().toString(), exercise, null, null))
        )
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
                programmes.version(source.id(), source.revision())?.settings()
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
            val clean = GymSetDraft()
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
                    userChoices.exclusions().map { it.exerciseId() },
                choices.filter { it.favourite() }.map { it.exerciseId() }.toSet(),
                mutable.value.recent
                    .flatMap { session -> session.exercises().map { it.exercise().id() } }
                    .toSet(),
                choice.reason,
            ),
            50,
        )
    }

    /** Legacy broad equipment labels never imply that every machine or attachment exists. */
    private fun initialSwapEquipment(labels: Set<String>): Set<ExerciseMetadata.Equipment> {
        if ("full gym" in labels) return ExerciseMetadata.Equipment.values().toSet()
        val capabilities =
            mapOf(
                "barbell" to ExerciseMetadata.Equipment.BARBELL,
                "dumbbells" to ExerciseMetadata.Equipment.DUMBBELLS,
                "kettlebells" to ExerciseMetadata.Equipment.KETTLEBELLS,
                "squat rack" to ExerciseMetadata.Equipment.SQUAT_RACK,
                "bench" to ExerciseMetadata.Equipment.BENCH,
                "cables" to ExerciseMetadata.Equipment.CABLE_STATION,
                "pull-up bar" to ExerciseMetadata.Equipment.PULL_UP_BAR,
                "trap bar" to ExerciseMetadata.Equipment.TRAP_BAR,
                "sled" to ExerciseMetadata.Equipment.SLED,
            )
        return labels.mapNotNull(capabilities::get).toSet()
    }

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
                        "Saved evidence reloaded. Unacknowledged entry values remain in the form; review before saving again."
                )
            }
        }

    fun updateForm(update: (GymSetDraft) -> GymSetDraft) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        val next = update(mutable.value.form)
        mutable.update { it.copy(form = next) }
        selectedSlot()?.let { saveForm(formKey(it.id()), next) }
    }

    fun saveSet() {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        val slot = selectedSlot() ?: return
        val draft = mutable.value.form
        val formKey = formKey(slot.id())
        afterAcknowledgement = {
            val clean = GymSetDraft()
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
                        number(draft.kilograms, "load")
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
            viewModelScope.launch {
                while (true) {
                    delay(1000)
                    if (!mutable.value.busy) {
                        try {
                            observeWorkoutTime()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: RuntimeException) {
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
    internal suspend fun observeWorkoutTime() {
        fun canObserve() =
            !mutable.value.busy && mutable.value.view?.observed()?.let(::unfinished) == true
        if (!canObserve()) return
        mutex.withLock {
            if (!canObserve()) return@withLock
            val observed = withContext(Dispatchers.IO) { coordinator.observe() }
            // A command can begin while observation is in flight. It owns the next snapshot.
            if (canObserve()) mutable.update { it.copy(view = observed) }
        }
    }

    fun generate(request: GymNowWorkouts.Request) = command {
        mutable.update { it.copy(generated = null) }
        val suggestion =
            GymNowWorkouts.generate(
                UUID.randomUUID().toString(),
                Instant.now(),
                request,
                mutable.value.favouriteExerciseIds,
                userChoices.exclusions().map { it.exerciseId() }.toSet(),
            )
        mutable.update { it.copy(generated = suggestion) }
    }

    fun startGenerated(quick: Boolean, onReady: () -> Unit) =
        start(onReady) {
            val definition =
                mutable.value.generated?.definition()
                    ?: error("Generate and review a workout first.")
            coordinator.start(
                UUID.randomUUID().toString(),
                if (quick) GymExecution.Origin.QUICK else GymExecution.Origin.GENERATED,
                definition,
                null,
                ZoneId.systemDefault().rules.getOffset(Instant.now()),
            )
        }

    fun newBuilder() {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        publishBuilder(GymBuilderDraft())
    }

    fun editFavourite(definition: GymWorkoutDefinition) {
        if (mutable.value.busy || mutable.value.view?.pending() != null) return
        publishBuilder(
            GymBuilderDraft(
                definition.id(),
                definition.revision() + 1,
                Instant.now(),
                definition.title(),
                definition.slots(),
                definition.revision(),
            )
        )
    }

    fun builderTitle(title: String) = changeBuilder { it.copy(title = title) }

    fun builderRemove(slotId: String) = changeBuilder {
        it.copy(slots = it.slots.filterNot { slot -> slot.id() == slotId })
    }

    fun builderMove(slotId: String, delta: Int) = changeBuilder { draft ->
        val rows = draft.slots.toMutableList()
        val from = rows.indexOfFirst { it.id() == slotId }
        val to = from + delta
        if (from < 0 || to !in rows.indices) draft
        else {
            java.util.Collections.swap(rows, from, to)
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
                GymWorkoutDefinition.Slot(
                    UUID.randomUUID().toString(),
                    slot.exercise(),
                    slot.target(),
                    null,
                ),
            )
            draft.copy(slots = rows)
        }
    }

    fun builderPutSlot(slotId: String?, exercise: StrengthExercise, input: GymSlotInput): Boolean {
        return try {
            val slot = gymSlotFromInput(slotId, exercise, input)
            changeBuilder { draft ->
                val rows = draft.slots.toMutableList()
                val at = rows.indexOfFirst { it.id() == slot.id() }
                if (at < 0) rows.add(slot) else rows[at] = slot
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
                UUID.randomUUID().toString(),
                GymExecution.Origin.CUSTOM,
                builderDefinition(),
                null,
                ZoneId.systemDefault().rules.getOffset(Instant.now()),
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
                current = current.copy(revision = current.revision + 1, createdAt = Instant.now())
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
                GymWorkoutJson.write(
                    GymWorkoutDefinition(
                        draft.id,
                        draft.revision,
                        draft.createdAt,
                        draft.title.takeIf { it.isNotBlank() && it.length <= 120 }
                            ?: "Workout draft",
                        draft.slots,
                    )
                )
        else retained.remove<String>("gym.builder.payload")
    }

    private fun readBuilder(): GymBuilderDraft =
        try {
            retained.get<String>("gym.builder.payload")?.let { payload ->
                val definition = GymWorkoutJson.read(payload)
                GymBuilderDraft(
                    definition.id(),
                    definition.revision(),
                    definition.createdAt(),
                    retained["gym.builder.title"] ?: definition.title(),
                    definition.slots(),
                    retained["gym.builder.saved"],
                )
            } ?: GymBuilderDraft(title = retained["gym.builder.title"] ?: "")
        } catch (_: IllegalArgumentException) {
            GymBuilderDraft()
        }

    private fun selectedSlot(): GymWorkoutDefinition.Slot? {
        val current = mutable.value.view?.observed() ?: return null
        return current.slots().firstOrNull { it.id() == current.selectedSlotId() }
    }

    private fun syncFormToSelection() {
        val slot = selectedSlot() ?: return
        val current = mutable.value.form
        val key = formKey(slot.id())
        val selected = retained.get<String>("gym.form.selected")
        if (selected != key) {
            if (selected != null) saveForm(selected, current)
            mutable.update { it.copy(form = readForm(key)) }
            retained["gym.form.selected"] = key
        }
    }

    private fun formKey(slotId: String): String = "${mutable.value.view?.observed()?.id()}::$slotId"

    private fun saveForm(slotId: String, form: GymSetDraft) {
        retained["gym.form.$slotId"] =
            arrayListOf(
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
        val values = retained.get<ArrayList<String>>("gym.form.$slotId") ?: return GymSetDraft()
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
        else GymSetDraft()
    }

    companion object {
        private val PROCESS_CLOCK = UUID.randomUUID().toString()

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
