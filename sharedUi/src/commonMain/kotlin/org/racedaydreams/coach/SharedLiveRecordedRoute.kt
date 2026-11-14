package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.polish.RoutePresentation
import org.racedaydreams.domain.polish.RouteRenderGeometry
import org.racedaydreams.domain.recording.RunMetricsEngine

/** Render data only. Platform adapters prepare off-main from their recorder's saved evidence. */
data class PreparedLiveRoute(
    val xy: List<RoutePresentation.XY>,
    val edges: List<RouteRenderGeometry.Edge>,
    val segments: Int,
) {
    companion object {
        fun fromPoints(points: List<RunMetricsEngine.Point>): PreparedLiveRoute {
            val xy = RoutePresentation.fit(points)
            return PreparedLiveRoute(
                xy,
                RouteRenderGeometry.prepare(points, xy, .0015),
                if (points.isEmpty()) 0
                else
                    1 +
                        points.zipWithNext().count {
                            it.second.discontinuity() || it.first.segment() != it.second.segment()
                        },
            )
        }
    }
}

/** Shared production route surface. No sensors, timers or persistence are created here. */
@Composable
fun SharedLiveRecordedRoute(projection: PreparedLiveRoute?, unavailable: Boolean) {
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
private fun LiveRouteDrawing(prepared: PreparedLiveRoute) {
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
        // A loop can finish at its start. Keep the square outside the latest-point ring
        // so both markers remain visible without displacing either recorded coordinate.
        val startHalf = 16.dp.toPx()
        val startCorner = first - Offset(startHalf, startHalf)
        val startSize = Size(startHalf * 2, startHalf * 2)
        drawRect(surface, startCorner, startSize)
        drawCircle(routeHalo, 13.dp.toPx(), last)
        drawCircle(route, 8.dp.toPx(), last)
        drawCircle(surface, 3.dp.toPx(), last)
        drawRect(start, startCorner, startSize, style = Stroke(3.dp.toPx()))
    }
}
