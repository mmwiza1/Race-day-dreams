package org.racedaydreams.coach.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachUiState

/**
 * Serialises application commands across features and publishes one coherent read model.
 *
 * Feature coordinators own their operations. They do not each create a competing copy of
 * application state or a separate transaction/database. Success callbacks run in the caller's
 * lifecycle scope, after the refreshed snapshot has been published.
 */
class CoachOperationRunner(
    private val scope: CoroutineScope,
    private val readSnapshot: suspend () -> CoachUiState,
    private val ioDispatcher: CoroutineDispatcher = applicationIoDispatcher(),
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = mutableState.asStateFlow()
    val current: CoachUiState
        get() = state.value

    fun update(transform: (CoachUiState) -> CoachUiState) {
        mutableState.update(transform)
    }

    /** True while a mergeable refresh is queued and has not started reading. Main thread only. */
    private var mergeableRefreshWaiting = false

    /**
     * Re-reads and publishes the snapshot.
     *
     * With [merge], a request made while another merged refresh is still queued, and has not yet
     * started reading, is folded into it. Resuming the app, returning from a permission screen and
     * re-entering a screen often ask at almost the same moment; the queued refresh reads after all
     * of those requests, so it already sees every change made before them, and one full read
     * replaces several. A refresh that has started reading is never relied on: a later request
     * queues a new read, so data is never left stale. Only use [merge] when [beforeRead] does
     * nothing. Call on the [scope]'s main thread.
     */
    fun refresh(beforeRead: suspend () -> Unit = {}, merge: Boolean = false) {
        if (merge) {
            if (mergeableRefreshWaiting) return
            mergeableRefreshWaiting = true
        }
        scope.launch {
            var waiting = merge
            update { it.copy(busy = true, error = null) }
            try {
                mutex.withLock {
                    if (waiting) {
                        mergeableRefreshWaiting = false
                        waiting = false
                    }
                    beforeRead()
                    publishSnapshot()
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                showFailure(failure)
            } finally {
                // Cancelled before reading: let the next request queue its own refresh.
                if (waiting) mergeableRefreshWaiting = false
            }
        }
    }

    fun submit(
        message: String? = null,
        onSuccess: () -> Unit = {},
        /** Receives the same message shown to the runner, after state is left unchanged. */
        onFailure: (String) -> Unit = {},
        operation: suspend () -> Unit,
    ) {
        scope.launch {
            update { it.copy(busy = true, error = null, message = null) }
            try {
                mutex.withLock {
                    withContext(ioDispatcher) { operation() }
                    publishSnapshot()
                    // Publish and deliver this command as one serial boundary. A queued
                    // feature must not replace its read model before its callback observes it.
                    onSuccess()
                    if (message != null) update { it.copy(message = message) }
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                val message = failureMessage(failure)
                onFailure(message)
                update { it.copy(loading = false, busy = false, error = message) }
            }
        }
    }

    /** Draft writes share the command lock without refreshing or clearing the form. */
    suspend fun serialiseDraftWrite(operation: () -> Unit) {
        mutex.withLock { withContext(ioDispatcher) { operation() } }
    }

    private suspend fun publishSnapshot() {
        val snapshot = readSnapshot()
        update {
            snapshot.copy(message = it.message, savedProfileRevision = it.savedProfileRevision)
        }
    }

    private fun showFailure(failure: Exception) {
        val message = failureMessage(failure)
        update { it.copy(loading = false, busy = false, error = message) }
    }

    // Preserve retained observations and input; corruption is never replaced by defaults.
    private fun failureMessage(failure: Exception): String =
        if (failure is IllegalArgumentException) {
            failure.message ?: "Check the entered values."
        } else {
            "Your local data could not be read or saved. It has been left in place. Try again, or use the confirmed local-data reset."
        }
}

internal expect fun applicationIoDispatcher(): CoroutineDispatcher
