package org.racedaydreams.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.presentation.DreamRouteProgress
import org.racedaydreams.domain.support.DreamRoutes

/** Presentation of the saved equivalent section; no new assignments, geometry or GPS. */
@Composable internal fun DreamRouteExperience(route: DreamRoutes.Route, assignment: DreamRoutes.Assignment) {
    val city = remember(route.id()) { dreamCityPresentation(route) }
    val progress = remember(assignment) { DreamRouteProgress.of(assignment) }
    val artwork by rememberDreamCityArtwork(city)
    val hero = artwork.hero
    val posters = artwork.posters
    val dark = isSystemInDarkTheme()
    val expandedText = LocalDensity.current.fontScale >= 1.4f
    val blue = if (dark) Color(0xff86BFFF) else Color(0xff0066DA)
    val ink = if (dark) Color(0xffF0F5FF) else Color(0xff102D50)
    val soft = if (dark) Color(0xff203B51) else Color(0xffEDF6FF)
    val card = if (dark) Color(0xff132635) else Color(0xffFFFFFF)

    Surface(Modifier.fillMaxWidth().semantics {
        stateDescription = if (hero != null && posters.all { it != null }) "Offline Dream artwork ready" else "Loading bundled city artwork"
    }, shape = RoundedCornerShape(24.dp), color = card, tonalElevation = 0.dp, shadowElevation = 2.dp) {
        Column {
            Box(Modifier.fillMaxWidth().heightIn(min = 176.dp)) {
                if (hero != null) Image(hero, contentDescription = "Original illustrated ${city.city} skyline. Atmospheric city art, not geographic landmark placement.",
                    modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                // Keep the illustration luminous in dark mode, with its own legible navy title panel.
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xffF2F7FF).copy(alpha = .92f), Color(0xffF2F7FF).copy(alpha = .50f), Color.Transparent))))
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dream Route · ${route.name()}", color = Color(0xff153657), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(14.dp))
                    Text(city.city, color = Color(0xff0D294B), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(city.course, color = Color(0xff183B60), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(.72f))
                    Text(city.edition, color = Color(0xff284767), style = MaterialTheme.typography.labelSmall)
                }
            }
            DreamDrawing(route, assignment.startMetres(), assignment.startMetres() + progress.highlightedMetres())
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Text carries the same information as the colour and bar.
                if (expandedText) {
                    ProgressHeadline(progress, blue, ink)
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(progress.completedDistanceLabel(), style = MaterialTheme.typography.headlineSmall, color = ink, fontWeight = FontWeight.Bold)
                        Text("of the marathon course", style = MaterialTheme.typography.bodySmall, color = ink)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${progress.percentage()}%", style = MaterialTheme.typography.headlineMedium, color = blue, fontWeight = FontWeight.Bold)
                        Text("complete", style = MaterialTheme.typography.bodySmall, color = ink)
                    }
                }
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (dark) Color(0xff36516B) else Color(0xffD3E6F9))
                    .progressSemantics(progress.fraction().toFloat())
                    .semantics { contentDescription = progress.accessibilitySummary(city.course) }) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress.fraction().toFloat())
                        .clip(RoundedCornerShape(8.dp)).background(blue))
                }
                Surface(color = soft, shape = RoundedCornerShape(14.dp)) {
                    Text(progress.equivalenceStatement(city.course), Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = ink)
                }
                Text(if (city.geographic) "Key landmarks on this route" else "City inspiration", style = MaterialTheme.typography.titleSmall, color = ink,
                    modifier = Modifier.semantics { heading() })
                if (expandedText) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    city.landmarks.forEachIndexed { index, landmark -> LandmarkPoster(landmark, posters[index], ink, Modifier.fillMaxWidth(), true) }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    city.landmarks.forEachIndexed { index, landmark -> LandmarkPoster(landmark, posters[index], ink, Modifier.weight(1f), false) }
                }
                Surface(color = soft, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    if (expandedText) Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DistanceValue(progress.completedDistanceLabel(), "Completed", ink)
                        DistanceValue(progress.remainingDistanceLabel(), "Remaining", ink)
                        DistanceValue(progress.totalDistanceLabel(), "Total distance", ink)
                    } else Row(Modifier.padding(vertical = 14.dp, horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) { DistanceValue(progress.completedDistanceLabel(), "Completed", ink) }
                        Column(Modifier.weight(1f)) { DistanceValue(progress.remainingDistanceLabel(), "Remaining", ink) }
                        Column(Modifier.weight(1f)) { DistanceValue(progress.totalDistanceLabel(), "Total distance", ink) }
                    }
                }
                Text("Start / Finish pins: full course · Blue rings: your equivalent section", color = ink, style = MaterialTheme.typography.labelSmall)
                DreamRoutes.attribution(route).takeIf { it.isNotEmpty() }?.let { Text(it, color = ink, style = MaterialTheme.typography.labelSmall) }
                WhyDetails("${DreamRoutes.provenance(route)} Your stable equivalent assignment is saved for this activity; it does not claim you ran in ${city.city}. Original city and landmark illustrations are decorative, not a navigation map or official event artwork.", "About this route ⓘ")
            }
        }
    }
}

@Composable private fun ProgressHeadline(progress: DreamRouteProgress, blue: Color, ink: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(progress.completedDistanceLabel(), style = MaterialTheme.typography.headlineSmall, color = ink, fontWeight = FontWeight.Bold)
        Text("of the marathon course", color = ink)
        Text(progress.completionLabel(), style = MaterialTheme.typography.titleLarge, color = blue)
    }
}

@Composable private fun LandmarkPoster(landmark: DreamLandmarkPoster, image: ImageBitmap?, ink: Color, modifier: Modifier, expanded: Boolean) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(if (expanded) 1.8f else 1f).clip(RoundedCornerShape(12.dp)))
        else Text("Illustration unavailable", color = ink, style = MaterialTheme.typography.bodySmall)
        Text(landmark.label, color = ink, style = if (expanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun DistanceValue(value: String, label: String, ink: Color) {
    Text(value, color = ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Text(label, color = ink, style = MaterialTheme.typography.labelSmall)
}
