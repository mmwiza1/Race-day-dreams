package org.racedaydreams.coach

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Original bundled artwork. Only the displayed city's four small images are requested. */
private object DreamArtworkCache {
    private val images = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    // Decode away from rendering; synchronise cache misses to avoid duplicate allocations.
    @Synchronized fun load(context: Context, asset: String): ImageBitmap? {
        images.get(asset)?.let { return it }
        return try {
            context.assets.open("dream-art/$asset.webp").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()?.also { images.put(asset, it) }
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

internal data class DreamCityArtwork(
    val assets: List<String>,
    val images: List<ImageBitmap?>,
    val loading: Boolean,
) {
    val hero get() = images.firstOrNull()
    val posters get() = images.drop(1)
    val ready get() = !loading && images.size == assets.size && images.all { it != null }
    val missing get() = assets.filterIndexed { index, _ -> images.getOrNull(index) == null }
    val status get() = when {
        loading -> "Loading bundled city artwork"
        ready -> "Offline Dream artwork ready"
        else -> "Bundled Dream artwork unavailable: ${missing.joinToString()}"
    }
}

/** Publish one coherent city snapshot; readiness and visible images observe the same state. */
@Composable internal fun rememberDreamCityArtwork(city: DreamCityPresentation): State<DreamCityArtwork> {
    val context = LocalContext.current.applicationContext
    val assets = listOf(city.hero) + city.landmarks.map { it.asset }
    val loader: suspend (String) -> ImageBitmap? = remember(context) {
        { asset -> DreamArtworkCache.load(context, asset) }
    }
    return rememberDreamArtwork(assets, loader)
}

/** One producer-owned state object survives key changes; an old city can never report readiness. */
@Composable
internal fun rememberDreamArtwork(
    assets: List<String>,
    load: suspend (String) -> ImageBitmap?,
): State<DreamCityArtwork> {
    val pending = remember(assets) { DreamCityArtwork(assets, List(assets.size) { null }, true) }
    val loader by rememberUpdatedState(load)
    val snapshot = produceState(pending, assets) {
        value = pending
        val images = withContext(Dispatchers.IO) { assets.map { loader(it) } }
        value = DreamCityArtwork(assets, images, false)
    }
    return remember(assets) {
        derivedStateOf { snapshot.value.takeIf { it.assets == assets } ?: pending }
    }
}

internal data class DreamLandmarkPoster(val label: String, val asset: String)
internal data class DreamCityPresentation(
    val city: String, val course: String, val edition: String, val hero: String,
    val landmarks: List<DreamLandmarkPoster>, val geographic: Boolean
)

/** Identity is derived from the retained geometry ID, never from today's preference. */
internal fun dreamCityPresentation(route: org.racedaydreams.domain.support.DreamRoutes.Route): DreamCityPresentation {
    val city = when {
        route.id().startsWith("new-york") -> "new-york"
        else -> route.id().substringBefore('-')
    }
    fun poster(label: String, slug: String) = DreamLandmarkPoster(label, "$city-$slug")
    val all = when (city) {
        "london" -> listOf(poster("Tower Bridge", "tower-bridge"), poster("Buckingham Palace", "buckingham-palace"), poster("London Eye", "london-eye"))
        "berlin" -> listOf(poster("Brandenburg Gate", "brandenburg-gate"), poster("Reichstag", "reichstag"), poster("Victory Column", "victory-column"))
        "paris" -> listOf(poster("Eiffel Tower", "eiffel-tower"), poster("Château de Vincennes", "vincennes"), poster("Louvre", "louvre"))
        "tokyo" -> listOf(poster("Tokyo Tower", "tokyo-tower"), poster("Tokyo Station", "tokyo-station"), poster("Sensō-ji", "senso-ji"))
        "new-york" -> listOf(poster("Verrazzano Bridge", "verrazzano"), poster("Queensboro Bridge", "queensboro"), poster("Central Park finish", "central-park"))
        else -> error("Unknown retained Dream city")
    }
    val geographic = route.id().contains("-geo-")
    val course = when (route.id()) {
        "paris-2024-olympic-geo-v1" -> "Paris 2024 Olympic marathon course"
        "paris-2026-geo-v1" -> "Marathon de Paris"
        else -> if (geographic) "${route.name()} Marathon course" else "${route.name()} course-inspired marathon equivalent"
    }
    val edition = when (route.id()) {
        "london-historical-geo-v1" -> "Historical course · edition unspecified"
        "berlin-2014-geo-v1" -> "2014 course"
        "paris-2026-geo-v1", "tokyo-2026-geo-v1", "new-york-2026-geo-v1" -> "2026 course"
        "paris-2024-olympic-geo-v1" -> "Archived Olympic course · 2024"
        else -> "Archived course-inspired drawing"
    }
    // Do not relabel a historic assignment's landmarks as belonging to the annual course.
    val landmarks = if (geographic) {
        val retained = org.racedaydreams.domain.support.DreamRoutes.landmarks(route).map { it.label() }.toSet()
        all.filter { it.label in retained }
    } else all
    val displayCity = when (city) {
        "london" -> "London"
        "berlin" -> "Berlin"
        "paris" -> "Paris"
        "tokyo" -> "Tokyo"
        else -> "New York"
    }
    return DreamCityPresentation(displayCity, course, edition, "$city-hero", landmarks, geographic)
}
