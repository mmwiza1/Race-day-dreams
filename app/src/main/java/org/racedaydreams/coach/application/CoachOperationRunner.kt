package org.racedaydreams.coach.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = mutableState.asStateFlow()
    val current: CoachUiState
        get() = state.value

    fun update(transform: (CoachUiState) -> CoachUiState) {
        mutableState.update(transform)
    }

    fun refresh(beforeRead: suspend () -> Unit = {}) {
        scope.launch {
            update { it.copy(busy = true, error = null) }
            try {
                mutex.withLock {
                    beforeRead()
                    publishSnapshot()
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                showFailure(failure)
            }
        }
    }

    fun submit(
        message: String? = null,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {},
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
                onFailure()
                showFailure(failure)
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
        // Preserve retained observations and input; corruption is never replaced by defaults.
        val message =
            if (failure is IllegalArgumentException) {
                failure.message ?: "Check the entered values."
            } else {
                "Your local data could not be read or saved. It has been left in place. Try again, or use the confirmed local-data reset."
            }
        update { it.copy(loading = false, busy = false, error = message) }
    }
}
