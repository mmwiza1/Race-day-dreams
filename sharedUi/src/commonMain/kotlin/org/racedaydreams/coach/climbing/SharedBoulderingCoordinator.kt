package org.racedaydreams.coach.climbing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.coach.application.applicationIoDispatcher
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.domain.time.ZoneOffset
import org.racedaydreams.storage.SqlDatabase
import org.racedaydreams.storage.SqliteClimbing

data class BoulderingState(
    val loading: Boolean = true,
    val sessions: List<ClimbingSession> = emptyList(),
    /** The single unsaved session kept on the device (live or a past-session log). */
    val draft: ClimbingDraft? = null,
    val gradeSystem: GradeSystem = GradeSystem.V_SCALE,
    /** A saved session being corrected; kept in memory until saved or cancelled. */
    val editing: ClimbingDraft? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    /** What the session and review screens edit. */
    fun target(): ClimbingDraft? = editing ?: draft
}

/**
 * Shared bouldering operations for Android and iOS. Climbing is stored separately from strength
 * training and never creates working sets, estimated 1RM, lifting records or muscle workload.
 */
class SharedBoulderingCoordinator(
    database: SqlDatabase,
    private val scope: CoroutineScope,
    private val now: () -> Instant,
    private val offsetAt: (Instant) -> ZoneOffset,
    private val newId: () -> String,
    private val io: CoroutineDispatcher = applicationIoDispatcher(),
) {
    private val repository = SqliteClimbing(database)
    private val mutable = MutableStateFlow(BoulderingState())
    val state: StateFlow<BoulderingState> = mutable.asStateFlow()
    private var persistJob: Job? = null
    /**
     * Counts local changes. A refresh that read the database while a change was made must not
     * replace the newer in-memory session with what it read.
     */
    private var generation = 0L

    fun refresh() {
        scope.launch {
            try {
                // A debounced draft write still pending would otherwise be read back as stale data.
                persistJob?.join()
                val started = generation
                val loaded =
                    withContext(io) {
                        Triple(repository.sessions(), repository.draft(), repository.gradeSystem())
                    }
                if (generation != started) {
                    // Something changed while reading: read again rather than overwrite it.
                    refresh()
                    return@launch
                }
                mutable.update {
                    it.copy(
                        loading = false,
                        sessions = loaded.first,
                        draft = loaded.second,
                        gradeSystem = loaded.third,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutable.update {
                    it.copy(
                        loading = false,
                        error = "Bouldering records could not be read. Nothing was changed.",
                    )
                }
            }
        }
    }

    fun clearMessage() = mutable.update { it.copy(message = null, error = null) }

    /** Starts a live session timed from now, unless an unsaved session already exists. */
    fun startLive() = startDraft { at ->
        ClimbingDraft(newId(), at, offsetAt(at), null, "", "", emptyList())
    }

    /** Starts logging a past session on [date]; its duration is entered rather than timed. */
    fun startPast(date: LocalDate) = startDraft { at ->
        val offset = offsetAt(at)
        val noon =
            Instants.ofEpochSecond(
                date.toEpochDay() * 86_400 + 12 * 3600 - offset.getTotalSeconds()
            )
        ClimbingDraft(newId(), noon, offset, 3600, "", "", emptyList())
    }

    private fun startDraft(create: (Instant) -> ClimbingDraft) {
        // Until the device has been read, an interrupted session may exist that a new one would
        // replace.
        if (mutable.value.loading) return
        if (mutable.value.draft != null) {
            mutable.update {
                it.copy(error = "Finish or discard the current bouldering session first.")
            }
            return
        }
        val draft = create(now())
        generation++
        mutable.update { it.copy(draft = draft, editing = null, error = null) }
        persist(draft, immediately = true)
    }

    /** Opens a saved session for correction without touching any unsaved draft. */
    fun edit(sessionId: String) {
        val session = mutable.value.sessions.firstOrNull { it.id == sessionId } ?: return
        mutable.update { it.copy(editing = ClimbingDraft.from(session), error = null) }
    }

    fun cancelEdit() = mutable.update { it.copy(editing = null, error = null) }

    fun setLocation(value: String) = change(debounce = true) { it.copy(location = value) }

    fun setNotes(value: String) = change(debounce = true) { it.copy(notes = value) }

    fun saveProblem(problem: ClimbingProblem) = change { draft ->
        val replaced = draft.problems.map { if (it.id == problem.id) problem else it }
        draft.copy(
            problems =
                if (replaced.any { it.id == problem.id }) replaced else draft.problems + problem
        )
    }

    fun removeProblem(id: String) = change { draft ->
        draft.copy(problems = draft.problems.filterNot { it.id == id })
    }

    /** Stops the live timer; the measured time can still be corrected before saving. */
    fun stopTimer() = change { draft ->
        if (draft.live())
            draft.copy(durationSeconds = draft.elapsedSeconds(now()).coerceAtLeast(60))
        else draft
    }

    fun setDurationMinutes(minutes: Long) = change { it.copy(durationSeconds = minutes * 60) }

    /** Moves a past-session log or correction by whole days; never into the future. */
    fun shiftDate(days: Long) = change { draft ->
        val moved = Instants.ofEpochSecond(draft.startedAt.getEpochSecond() + days * 86_400)
        if (moved.isAfter(now())) draft else draft.copy(startedAt = moved)
    }

    fun newProblemId(): String = newId()

    private fun change(debounce: Boolean = false, transform: (ClimbingDraft) -> ClimbingDraft) {
        val current = mutable.value
        val target = current.target() ?: return
        val updated =
            try {
                transform(target)
            } catch (invalid: IllegalArgumentException) {
                mutable.update { it.copy(error = invalid.message) }
                return
            }
        if (current.editing != null) mutable.update { it.copy(editing = updated, error = null) }
        else {
            generation++
            mutable.update { it.copy(draft = updated, error = null) }
            persist(updated, immediately = !debounce)
        }
    }

    /** The unsaved draft is written to the device so an interrupted session can be resumed. */
    private fun persist(draft: ClimbingDraft, immediately: Boolean) {
        persistJob?.cancel()
        persistJob =
            scope.launch {
                if (!immediately) delay(400)
                try {
                    withContext(io) { repository.saveDraft(draft) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutable.update {
                        it.copy(
                            error =
                                "The session could not be kept on this device. Keep the app open and try again."
                        )
                    }
                }
            }
    }

    /** Validates and saves the reviewed session; [saved] receives its identity. */
    fun save(saved: (String) -> Unit = {}) {
        val current = mutable.value
        val target = current.target() ?: return
        val session =
            try {
                target.toSession(target.durationSeconds ?: target.elapsedSeconds(now()))
            } catch (invalid: IllegalArgumentException) {
                mutable.update { it.copy(error = invalid.message) }
                return
            }
        generation++
        mutable.update { it.copy(busy = true, error = null) }
        scope.launch {
            try {
                persistJob?.cancelAndJoin()
                withContext(io) {
                    if (current.editing != null) repository.update(session)
                    else repository.finish(session)
                }
                val sessions = withContext(io) { repository.sessions() }
                mutable.update {
                    it.copy(
                        busy = false,
                        sessions = sessions,
                        draft = if (current.editing != null) it.draft else null,
                        editing = null,
                        message =
                            if (current.editing != null) "Session corrected."
                            else "Bouldering session saved.",
                    )
                }
                saved(session.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutable.update {
                    it.copy(
                        busy = false,
                        error = error.message ?: "The session was not saved. Nothing was lost.",
                    )
                }
            }
        }
    }

    fun discardDraft() {
        generation++
        scope.launch {
            persistJob?.cancelAndJoin()
            withContext(io) { repository.clearDraft() }
            mutable.update { it.copy(draft = null, message = "Unsaved session discarded.") }
        }
    }

    fun delete(sessionId: String) {
        generation++
        scope.launch {
            try {
                withContext(io) { repository.delete(sessionId) }
                val sessions = withContext(io) { repository.sessions() }
                mutable.update {
                    it.copy(sessions = sessions, editing = null, message = "Session deleted.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update {
                    it.copy(error = "The session could not be deleted. It is still saved.")
                }
            }
        }
    }

    fun setGradeSystem(system: GradeSystem) {
        generation++
        mutable.update { it.copy(gradeSystem = system) }
        scope.launch { withContext(io) { repository.gradeSystem(system) } }
    }
}
