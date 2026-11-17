package org.racedaydreams.coach.strength

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.coach.application.applicationIoDispatcher
import org.racedaydreams.domain.achievements.TrainingMilestones
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.SqlDatabase
import org.racedaydreams.storage.SqliteTrainingMilestones

data class AchievementsState(
    val loading: Boolean = true,
    val awards: List<SqliteTrainingMilestones.Stored> = emptyList(),
    val celebrations: Boolean = false,
    val availableSources: Set<Pair<TrainingMilestones.Source, String>> = emptySet(),
    val notice: String? = null,
    val error: String? = null,
)

/**
 * Original optional recognition operation, now shared; host owns lifecycle and real repositories.
 */
class SharedAchievementsCoordinator(
    database: SqlDatabase,
    private val scope: CoroutineScope,
    private val strength: CoachingRepository,
    private val running: ActivityRepository,
    private val now: () -> Instant,
    private val ioDispatcher: CoroutineDispatcher = applicationIoDispatcher(),
) {
    private val repository = SqliteTrainingMilestones(database)
    private val mutable = MutableStateFlow(AchievementsState())
    val state: StateFlow<AchievementsState> = mutable.asStateFlow()
    private var active: Job? = null

    fun refresh(celebrations: Boolean? = null) {
        if (active?.isActive == true) return
        mutable.update { it.copy(loading = true, error = null, notice = null) }
        active =
            scope.launch {
                try {
                    val value =
                        withContext(ioDispatcher) {
                            if (celebrations != null) repository.celebrations(celebrations)
                            val gym = strength.strength()
                            // Summary query only: achievement counts never load a GPS route.
                            val runs = running.activitySummaries()
                            val now = now()
                            val candidates = TrainingMilestones.eligible(gym, runs, now)
                            currentCoroutineContext().ensureActive()
                            val count = repository.recognize(candidates, now)
                            val enabled = repository.celebrations()
                            AchievementsState(
                                false,
                                repository.awards(),
                                enabled,
                                gym.map { TrainingMilestones.Source.GYM to it.id() }.toSet() +
                                    runs.map { TrainingMilestones.Source.RUN to it.id() },
                                if (enabled && count > 0)
                                    "$count new recorded milestones recognised. You can ignore celebrations without affecting your training."
                                else null,
                            )
                        }
                    mutable.value = value
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: RuntimeException) {
                    mutable.update {
                        it.copy(
                            loading = false,
                            error =
                                "Milestones could not be updated. Your workout history is unchanged.",
                        )
                    }
                }
            }
    }

    fun dismiss() {
        mutable.update { it.copy(notice = null) }
    }
}
