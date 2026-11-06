package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.achievements.TrainingMilestones

data class AchievementsState(
    val loading: Boolean = true,
    val awards: List<SqliteAchievementsRepository.Stored> = emptyList(),
    val celebrations: Boolean = false,
    val availableSources: Set<Pair<TrainingMilestones.Source, String>> = emptySet(),
    val notice: String? = null,
    val error: String? = null,
)

/** Optional local recognition only; never modifies actuals, prescriptions or coaching decisions. */
class AchievementsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SqliteAchievementsRepository(application)
    private val strength = CoachingStore(application)
    private val running = SqliteActivityRepository(application)
    private val mutable = MutableStateFlow(AchievementsState())
    val state: StateFlow<AchievementsState> = mutable.asStateFlow()
    private var active: Job? = null

    init {
        refresh()
    }

    fun refresh(celebrations: Boolean? = null) {
        if (active?.isActive == true) return
        mutable.update { it.copy(loading = true, error = null, notice = null) }
        active =
            viewModelScope.launch {
                try {
                    val value =
                        withContext(Dispatchers.IO) {
                            if (celebrations != null) repository.celebrations(celebrations)
                            val gym = strength.strength()
                            // Summary query only: achievement counts never load a GPS route.
                            val runs = running.activitySummaries()
                            val now = Instant.now()
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
