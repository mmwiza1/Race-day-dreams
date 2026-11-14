package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.recovery.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*

data class TrainingRecoveryState(
    val busy: Boolean = true,
    val review: TrainingRecovery.Review? = null,
    val programme: StrengthProgramme? = null,
    val conflicts: List<TrainingRecovery.Conflict> = emptyList(),
    val alternatives: Map<String, List<LocalDate>> = emptyMap(),
    val canReviewLighterWeek: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

/**
 * Advisory evidence and explicit draft preparation; accepted programming remains repository-owned.
 */
class TrainingRecoveryViewModel(app: Application) : AndroidViewModel(app) {
    private val programmes = SqliteStrengthProgrammingRepository(app)
    private val drafts = SqliteConcreteStrengthProgrammeDraftRepository(app)
    private val gym = SqliteGymExecutionRepository(app)
    private val actuals = CoachingStore(app)
    private val activities = SqliteActivityRepository(app)
    private val health = HealthIntelligenceStore(app)
    private val focus = SqlitePrimaryFocusRepository(app)
    private val mutable = MutableStateFlow(TrainingRecoveryState())
    val state: StateFlow<TrainingRecoveryState> = mutable.asStateFlow()
    private var operation: Job? = null

    private data class Snapshot(
        val state: TrainingRecoveryState,
        val reference: StrengthProgrammingRepository.Reference?,
        val protected: Set<String>,
        val runs: List<PlannedWorkout>,
        val now: Instant,
        val offset: ZoneOffset,
    )

    init {
        refresh()
    }

    private fun snapshot(): Snapshot {
        val now = Instant.now()
        val offset = ZoneId.systemDefault().rules.getOffset(now)
        val today = now.atOffset(offset).toLocalDate()
        val review =
            TrainingRecovery.review(
                actuals.strength(),
                activities.activitySummaries(),
                health.recent(today),
                now,
                offset,
            )
        val reference = programmes.currentReference()
        val revision =
            reference
                ?.takeIf { it.owner() == StrengthProgrammingRepository.Owner.AUTHORED }
                ?.let { programmes.reviewRevision(it) }
        val programme = revision?.programme()
        val protected = revision?.completedWorkoutIds()?.toMutableSet() ?: mutableSetOf()
        if (programme != null) {
            val known = programme.workouts().map { it.id() }.toSet()
            gym.unfinished().forEach { row ->
                val stored =
                    gym.load(requireNotNull(row.id()))
                        ?: error("The unfinished gym workout is missing.")
                val source = stored.execution().source()
                if (
                    source?.kind() == GymExecution.SourceKind.PROGRAMME &&
                        source.id() == programme.id() &&
                        source.sessionId() in known
                )
                    protected.add(source.sessionId())
            }
        }
        val hybrid = focus.load().orElse(PrimaryFocus.RUNNING) == PrimaryFocus.HYBRID
        val runs = if (hybrid) actuals.current()?.plan()?.workouts() ?: emptyList() else emptyList()
        val conflicts =
            programme?.let { TrainingRecovery.conflicts(it, runs, today, protected) } ?: emptyList()
        val dates =
            conflicts
                .map { it.workoutId() }
                .distinct()
                .associateWith {
                    RecoveryProgrammeProposal.alternatives(programme!!, it, runs, today, protected)
                }
        val canReduce =
            programme != null &&
                review.lighterWeek() &&
                programme.workouts().any { w ->
                    !w.date().isBefore(today) &&
                        !w.date().isAfter(today.plusDays(6)) &&
                        w.id() !in protected &&
                        w.definition().slots().any { (it.target()?.sets() ?: 0) > 1 }
                }
        return Snapshot(
            TrainingRecoveryState(false, review, programme, conflicts, dates, canReduce),
            reference,
            protected.toSet(),
            runs,
            now,
            offset,
        )
    }

    fun refresh() {
        if (operation?.isActive == true) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        operation =
            viewModelScope.launch {
                try {
                    mutable.value = withContext(Dispatchers.IO) { snapshot().state }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    mutable.update {
                        it.copy(
                            busy = false,
                            error =
                                "Could not review the local evidence. Your programme is unchanged.",
                        )
                    }
                }
            }
    }

    /** Keep is deliberately a no-write choice, like keeping the existing progression target. */
    fun keep() {
        mutable.update { it.copy(notice = "Your current programme is unchanged.") }
    }

    fun prepareLighterWeek(open: (String) -> Unit) = prepare(null, null, open)

    fun prepareMove(workoutId: String, date: LocalDate, open: (String) -> Unit) =
        prepare(workoutId, date, open)

    private fun prepare(workoutId: String?, date: LocalDate?, open: (String) -> Unit) {
        if (operation?.isActive == true) return
        val seen = mutable.value
        mutable.value = seen.copy(busy = true, error = null, notice = null)
        operation =
            viewModelScope.launch {
                try {
                    val id =
                        withContext(Dispatchers.IO) {
                            val fresh = snapshot()
                            require(
                                fresh.state.programme == seen.programme &&
                                    fresh.state.review == seen.review &&
                                    fresh.state.conflicts == seen.conflicts &&
                                    fresh.state.alternatives == seen.alternatives
                            ) {
                                "Evidence or programming changed. Refresh and review the new guidance first."
                            }
                            val previous =
                                fresh.state.programme
                                    ?: error(
                                        "Use the programme editor to review this programme. Its original history remains unchanged."
                                    )
                            val today = fresh.now.atOffset(fresh.offset).toLocalDate()
                            val proposal =
                                if (workoutId == null)
                                    RecoveryProgrammeProposal.lighterWeek(
                                        previous,
                                        fresh.state.review!!,
                                        fresh.now,
                                        fresh.offset,
                                        fresh.protected,
                                    )
                                else
                                    RecoveryProgrammeProposal.move(
                                        previous,
                                        workoutId,
                                        requireNotNull(date),
                                        fresh.runs,
                                        today,
                                        fresh.now,
                                        fresh.protected,
                                    )
                            require(programmes.currentReference() == fresh.reference) {
                                "Current programming changed. Refresh before saving a preview."
                            }
                            currentCoroutineContext().ensureActive()
                            val draft =
                                ConcreteStrengthProgrammeDraft(
                                    UUID.randomUUID().toString(),
                                    1,
                                    fresh.reference,
                                    proposal.proposed(),
                                    emptyList(),
                                )
                            drafts.save(draft, 0)
                            draft.id()
                        }
                    mutable.update {
                        it.copy(
                            busy = false,
                            notice =
                                "Preview saved. Your active programme has not changed; review and activate separately.",
                        )
                    }
                    open(id)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    mutable.update {
                        it.copy(
                            busy = false,
                            error = failure.message ?: "No programme change was made.",
                        )
                    }
                }
            }
    }
}
