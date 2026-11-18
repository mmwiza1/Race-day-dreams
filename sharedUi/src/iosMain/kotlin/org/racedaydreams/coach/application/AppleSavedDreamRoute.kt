package org.racedaydreams.coach.application

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachCard
import org.racedaydreams.coach.WhyDetails
import org.racedaydreams.coach.dreamCityPresentation
import org.racedaydreams.domain.presentation.DreamRouteProgress
import org.racedaydreams.domain.support.DreamRoutes

@Composable
internal fun AppleSavedDreamRoute(session: AppleCoachSession<*>, activityId: String) {
    var failed by remember(session, activityId) { mutableStateOf(false) }
    val assignment by
        produceState<DreamRoutes.Assignment?>(null, session, activityId) {
            try {
                value =
                    withContext(applicationIoDispatcher()) {
                        session.runningSupport.dreamAssignment(activityId)
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed = true
            }
        }
    if (failed) Text("The saved Dream Route could not be loaded.")
    val saved = assignment ?: return
    val route = remember(saved.routeId()) { DreamRoutes.forId(saved.routeId()) }
    val city = remember(route.id()) { dreamCityPresentation(route) }
    val progress = remember(saved) { DreamRouteProgress.of(saved) }
    val highlighted =
        remember(route, saved) {
            DreamRoutes.section(
                route,
                saved.startMetres(),
                saved.startMetres() + progress.highlightedMetres(),
            )
        }
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    CoachCard {
        Text("Dream Route · ${route.name()}", style = MaterialTheme.typography.titleMedium)
        Text(city.course)
        Text(city.edition, style = MaterialTheme.typography.labelMedium)
        AppleBundledIllustration(
            "dream-art",
            city.hero,
            "Illustrated ${city.city} skyline.",
            Modifier.fillMaxWidth().aspectRatio(1.8f).clip(MaterialTheme.shapes.large),
        ) {
            Text("City illustration unavailable. Your saved course is shown below.")
        }
        Canvas(
            Modifier.fillMaxWidth().height(160.dp).semantics {
                contentDescription =
                    "Illustrated marathon route with your equivalent section highlighted."
            }
        ) {
            val scale = minOf(size.width, size.height) * 0.9f / 10f
            val left = (size.width - 10 * scale) / 2
            val top = (size.height - 10 * scale) / 2
            fun path(vertices: List<DreamRoutes.Vertex>) =
                Path().apply {
                    vertices.forEachIndexed { index, vertex ->
                        val point =
                            Offset(
                                left + vertex.x().toFloat() * scale,
                                top + vertex.y().toFloat() * scale,
                            )
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
            drawPath(
                path(route.vertices()),
                outline,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path(highlighted),
                accent,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Text(progress.equivalenceStatement(city.course))
        LinearProgressIndicator(
            progress = { progress.fraction().toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(progress.completionLabel())
        Text("Remaining equivalent distance: ${progress.remainingDistanceLabel()}")
        if (city.landmarks.isNotEmpty()) {
            var showLandmarks by remember(route.id()) { mutableStateOf(false) }
            TextButton(onClick = { showLandmarks = !showLandmarks }) {
                Text(if (showLandmarks) "Hide city landmarks" else "Explore city landmarks")
            }
            if (showLandmarks) {
                Text(
                    if (city.geographic) "Landmarks from the saved course edition."
                    else "Artwork inspired by the city."
                )
                city.landmarks.forEach { landmark ->
                    Text(landmark.label, style = MaterialTheme.typography.titleSmall)
                    AppleBundledIllustration(
                        "dream-art",
                        landmark.asset,
                        null,
                        Modifier.fillMaxWidth().aspectRatio(1.8f).clip(MaterialTheme.shapes.large),
                    ) {
                        Text("Illustration unavailable.")
                    }
                }
            }
        }
        val attribution = DreamRoutes.attribution(route)
        if (attribution.isNotEmpty()) Text(attribution, style = MaterialTheme.typography.labelSmall)
        WhyDetails(
            DreamRoutes.provenance(route) +
                " The highlighted section shows equivalent distance. Your recorded GPS route is shown separately.",
            "About this Dream Route",
        )
    }
}
