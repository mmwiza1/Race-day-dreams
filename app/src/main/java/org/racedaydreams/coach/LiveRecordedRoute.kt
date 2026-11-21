package org.racedaydreams.coach

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.domain.recording.IncrementalRoutePoints

/**
 * An optional offline outline of the recorder's accepted, persisted evidence. Preparation is
 * off-main and collection stops when this view leaves composition or the host stops.
 */
@Composable
internal fun LiveRecordedRoute() {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var projection by remember { mutableStateOf<PreparedLiveRoute?>(null) }
    var unavailable by remember { mutableStateOf(false) }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val store = RecordingStore(context)
            // Holds the decoded saved route; each refresh reads only newly saved points.
            val route = IncrementalRoutePoints(store::pointsFrom)
            while (isActive) {
                try {
                    val points =
                        withContext(Dispatchers.IO) {
                            val session =
                                store.unfinished()?.takeIf {
                                    it.state in listOf("ACTIVE", "PAUSED")
                                }
                            route.refresh(session?.id)
                        }
                    if (points != null) {
                        projection =
                            withContext(Dispatchers.Default) {
                                PreparedLiveRoute.fromPoints(points)
                            }
                    }
                    unavailable = false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A read problem must never stop the recorder or fabricate replacement
                    // evidence.
                    unavailable = true
                }
                delay(5_000)
            }
        }
    }
    SharedLiveRecordedRoute(projection, unavailable)
}
