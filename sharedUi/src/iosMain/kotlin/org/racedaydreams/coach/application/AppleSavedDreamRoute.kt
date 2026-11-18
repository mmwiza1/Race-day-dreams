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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachCard
import org.racedaydreams.coach.WhyDetails
import org.racedaydreams.coach.dreamCityPresentation
import org.racedaydreams.domain.presentation.DreamRouteProgress
import org.racedaydreams.domain.support.DreamBasemap
import org.racedaydreams.domain.support.DreamRoutes

@Composable
internal fun AppleSavedDreamRoute(session: AppleCoachSession<*>, activityId: String) {
    var failed by remember(session, activityId) { mutableStateOf(false) }
    var assignment by
        remember(session, activityId) { mutableStateOf<DreamRoutes.Assignment?>(null) }
    LaunchedEffect(session, activityId) {
        try {
            assignment =
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
    val course = remember(route) { Path().apply { appendDreamVertices(route.vertices()) } }
    val covered = remember(highlighted) { Path().apply { appendDreamVertices(highlighted) } }
    val frame =
        remember(route) {
            val vertices =
                route.vertices() +
                    DreamRoutes.landmarks(route).map { DreamRoutes.Vertex(it.x(), it.y()) }
            val left = vertices.minOf { it.x() }.toFloat() - 0.75f
            val top = vertices.minOf { it.y() }.toFloat() - 0.75f
            val right = vertices.maxOf { it.x() }.toFloat() + 0.75f
            val bottom = vertices.maxOf { it.y() }.toFloat() + 0.75f
            androidx.compose.ui.geometry.Rect(left, top, right, bottom)
        }
    var basemapLoaded by remember(route.id()) { mutableStateOf(false) }
    var basemap by remember(route.id()) { mutableStateOf<Map<DreamBasemap.Kind, Path>?>(null) }
    LaunchedEffect(route.id()) {
        basemap =
            withContext(applicationIoDispatcher()) {
                runCatching { loadAppleDreamBasemap(route.id()) }.getOrNull()
            }
        basemapLoaded = true
    }
    val water = MaterialTheme.colorScheme.secondaryContainer
    val green = MaterialTheme.colorScheme.tertiaryContainer
    val roads = MaterialTheme.colorScheme.outlineVariant
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
            val scale = minOf(size.width / frame.width, size.height / frame.height)
            val left = (size.width - frame.width * scale) / 2 - frame.left * scale
            val top = (size.height - frame.height * scale) / 2 - frame.top * scale
            if (scale > 0f)
                clipRect {
                    translate(left, top) {
                        scale(scale, scale, pivot = Offset.Zero) {
                            basemap?.get(DreamBasemap.Kind.GREEN)?.let { drawPath(it, green) }
                            basemap?.get(DreamBasemap.Kind.WATER)?.let { drawPath(it, water) }
                            basemap?.get(DreamBasemap.Kind.ROAD)?.let {
                                drawPath(it, roads, style = Stroke(width = 0.6.dp.toPx() / scale))
                            }
                            drawPath(
                                course,
                                outline,
                                style = Stroke(width = 2.dp.toPx() / scale, cap = StrokeCap.Round),
                            )
                            drawPath(
                                covered,
                                accent,
                                style = Stroke(width = 4.dp.toPx() / scale, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
        }
        if (city.geographic && basemapLoaded && basemap == null) {
            Text(
                "Offline city context is unavailable. Your saved course and progress are still shown."
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
