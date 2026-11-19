package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.*
import java.util.UUID
import org.racedaydreams.data.*

/** Actual Android lifecycle/cache/timezone bindings; recommendations and proposals are shared. */
class TrainingRecoveryViewModel(app: Application) : AndroidViewModel(app) {
    private val health = HealthIntelligenceStore(app)
    private val focus = SqlitePrimaryFocusRepository(app)
    private val shared =
        SharedTrainingRecoveryCoordinator(
            SqliteStrengthProgrammingRepository(app),
            SqliteConcreteStrengthProgrammeDraftRepository(app),
            SqliteGymExecutionRepository(app),
            CoachingStore(app),
            SqliteActivityRepository(app),
            health::recent,
            { focus.load().orElse(null) },
            viewModelScope,
            Instant::now,
            { at -> ZoneId.systemDefault().rules.getOffset(at) },
            { UUID.randomUUID().toString() },
        )
    val state = shared.state

    init {
        refresh()
    }

    fun refresh() = shared.refresh()

    fun keep() = shared.keep()

    fun prepareLighterWeek(open: (String) -> Unit) = shared.prepareLighterWeek(open)

    fun prepareMove(workoutId: String, date: LocalDate, open: (String) -> Unit) =
        shared.prepareMove(workoutId, date, open)
}
