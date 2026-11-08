package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.domain.polish.RoutePresentation
import org.racedaydreams.domain.polish.RouteRenderGeometry
import org.racedaydreams.domain.recording.RunMetricsEngine

/**
 * Prepared render data only. Source observations remain owned by RecordingStore and the service.
 */
private data class LiveRouteProjection(
    val xy: List<RoutePresentation.XY>,
    val edges: List<RouteRenderGeometry.Edge>,
    val segments: Int,
)

private data class LiveRouteEvidence(
    val checkpoint: Pair<String, Long>?,
    val points: List<RunMetricsEngine.Point>,
)

/**
 * An optional offline outline of the recorder's accepted, persisted evidence. Preparation is
 * off-main and collection stops when this view leaves composition or the host stops.
 */
@Composable
internal fun LiveRecordedRoute() {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var projection by remember { mutableStateOf<LiveRouteProjection?>(null) }
    var unavailable by remember { mutableStateOf(false) }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val store = RecordingStore(context)
            var observed: Pair<String, Long>? = null
            while (isActive) {
                try {
                    val snapshot =
                        withContext(Dispatchers.IO) {
                            val session =
                                store.unfinished()?.takeIf {
                                    it.state in listOf("ACTIVE", "PAUSED")
                                }
                            if (session == null) LiveRouteEvidence(null, emptyList())
                            else {
                                val key = session.id to session.last
                                if (key == observed) null
                                else LiveRouteEvidence(key, store.points(session.id))
                            }
                        }
                    if (snapshot != null) {
                        val points = snapshot.points
                        val prepared =
                            withContext(Dispatchers.Default) {
                                val xy = RoutePresentation.fit(points)
                                LiveRouteProjection(
                                    xy,
                                    RouteRenderGeometry.prepare(points, xy, .0015),
                                    if (points.isEmpty()) 0
                                    else
                                        1 +
                                            points.zipWithNext().count {
                                                it.second.discontinuity() ||
                                                    it.first.segment() != it.second.segment()
                                            },
                                )
                            }
                        observed = snapshot.checkpoint
                        projection = prepared
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your recorded route", style = MaterialTheme.typography.titleLarge)
            val prepared = projection
            when {
                unavailable ->
                    Text(
                        "Route preview is temporarily unavailable. Recording controls are still available."
                    )
                prepared == null -> Text("Loading saved GPS evidence…")
                prepared.xy.size < 2 ->
                    Text(
                        "Your route will appear after two usable GPS observations have been saved."
                    )
                else -> {
                    LiveRouteDrawing(prepared)
                    Text(
                        "□ Start · ◉ Latest saved point · North is up",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (prepared.segments > 1)
                        Text("GPS gaps remain blank.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "Offline route outline · no street map. Shows accepted GPS evidence saved so far; updates while this view is visible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveRouteDrawing(prepared: LiveRouteProjection) {
    val route = MaterialTheme.colorScheme.primary
    val routeHalo = MaterialTheme.colorScheme.primaryContainer
    val start = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    Canvas(
        Modifier.fillMaxWidth().height(300.dp).testTag("live-route-canvas").semantics {
            contentDescription =
                "Accepted recorded route, north up. Outlined square: start. Ring: latest saved point. ${prepared.segments} continuous GPS sections; missing sections are not joined. This is an offline outline, not a street map."
        }
    ) {
        val side = size.minDimension
        val origin = Offset((size.width - side) / 2, (size.height - side) / 2)
        fun position(point: RoutePresentation.XY) =
            origin + Offset((point.x() * side).toFloat(), (point.y() * side).toFloat())
        for (edge in prepared.edges) {
            val a = position(prepared.xy[edge.from()])
            val b = position(prepared.xy[edge.to()])
            drawLine(routeHalo, a, b, 10.dp.toPx(), StrokeCap.Round)
            drawLine(route, a, b, 5.dp.toPx(), StrokeCap.Round)
        }
        val first = position(prepared.xy.first())
        val last = position(prepared.xy.last())
        val startHalf = 8.dp.toPx()
        val startCorner = first - Offset(startHalf, startHalf)
        val startSize = Size(startHalf * 2, startHalf * 2)
        drawRect(surface, startCorner, startSize)
        drawRect(start, startCorner, startSize, style = Stroke(3.dp.toPx()))
        drawCircle(routeHalo, 13.dp.toPx(), last)
        drawCircle(route, 8.dp.toPx(), last)
        drawCircle(surface, 3.dp.toPx(), last)
    }
}
