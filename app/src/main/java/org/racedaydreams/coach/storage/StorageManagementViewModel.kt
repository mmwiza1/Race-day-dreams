package org.racedaydreams.coach.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.storage.DetailRetention

data class StorageManagementState(
    val busy: Boolean = true,
    val preferences: DetailRetention.Preferences = DetailRetention.Preferences.existing(),
    val inventory: SqliteRecordingInventoryRepository.Inventory? = null,
    val preview: DetailRetention.Preview? = null,
    val error: String? = null,
    val notice: String? = null,
)

/** Explicit foreground operations only. No periodic worker or competing recording engine. */
class StorageManagementViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SqliteDetailRetentionRepository(app)
    private val inventory = SqliteRecordingInventoryRepository(app)
    private val mutable = MutableStateFlow(StorageManagementState())
    val state: StateFlow<StorageManagementState> = mutable.asStateFlow()
    private var operation: Job? = null

    init {
        refresh()
    }

    private fun command(work: suspend () -> StorageManagementState) {
        if (operation?.isActive == true) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        operation =
            viewModelScope.launch {
                try {
                    mutable.value = withContext(Dispatchers.IO) { work() }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    mutable.update {
                        it.copy(
                            busy = false,
                            preview = null,
                            error =
                                failure.message
                                    ?: "Storage operation could not finish. Refresh to review retained detail; completed batches remain saved.",
                        )
                    }
                }
            }
    }

    private fun snapshot(notice: String? = null) =
        StorageManagementState(
            false,
            repository.preferences(),
            inventory.inspect(),
            notice = notice,
        )

    fun refresh() = command { snapshot() }

    fun choose(policy: DetailRetention.Policy) = command {
        repository.acknowledge(policy, Instant.now())
        snapshot("Retention choice saved. No recording detail has been removed.")
    }

    fun preview() = command {
        val preferences = repository.preferences()
        val result = repository.preview(preferences.policy(), Instant.now())
        snapshot().copy(preview = result)
    }

    fun cleanup() = command {
        val preview = mutable.value.preview ?: error("Review a fresh preview before cleanup.")
        val approval = repository.approve(preview, Instant.now())
        var batches = 0
        for (recording in approval.preview().recordings()) {
            currentCoroutineContext().ensureActive()
            if (repository.cleanOne(approval, recording.sessionId(), Instant.now())) batches++
            mutable.update {
                it.copy(
                    notice =
                        "Removed raw detail from $batches recordings so far. Summaries and PBs remain. Each completed recording is saved."
                )
            }
            yield() // Cancellation between atomic whole-session batches retains committed progress.
        }
        snapshot(
            "Removed eligible raw detail from $batches ${if (batches == 1) "recording" else "recordings"}. Workout summaries, PBs and training history remain. Database file size may not decrease."
        )
    }
}
