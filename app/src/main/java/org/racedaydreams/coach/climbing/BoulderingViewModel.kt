package org.racedaydreams.coach.climbing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.data.AndroidSqlConnection

/** Android owns lifecycle; the bouldering operations and state are shared with iOS. */
class BoulderingViewModel(application: Application) : AndroidViewModel(application) {
    val coordinator =
        SharedBoulderingCoordinator(
            AndroidSqlConnection.forApplication(application),
            viewModelScope,
            Instant::now,
            { ZoneId.systemDefault().rules.getOffset(it) },
            { UUID.randomUUID().toString() },
        )

    init {
        coordinator.refresh()
    }
}
