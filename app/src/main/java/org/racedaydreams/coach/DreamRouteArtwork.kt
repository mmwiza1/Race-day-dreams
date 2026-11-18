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
