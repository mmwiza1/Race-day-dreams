package org.racedaydreams.coach.strength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.SqliteActivityRepository

/** Android owns lifecycle; the real existing recognition operation and state are shared. */
class AchievementsViewModel(application: Application) : AndroidViewModel(application) {
    private val coordinator =
        SharedAchievementsCoordinator(
            AndroidSqlConnection.forApplication(application),
            viewModelScope,
            CoachingStore(application),
            SqliteActivityRepository(application),
            Instant::now,
        )
    val state = coordinator.state

    init {
        refresh()
    }

    fun refresh(celebrations: Boolean? = null) = coordinator.refresh(celebrations)

    fun dismiss() = coordinator.dismiss()
}
