package org.racedaydreams.coach

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Small, bounded offline thumbnail cache; only visible lazy rows decode, away from the UI thread.
private val thumbnails = android.util.LruCache<String, ImageBitmap>(24)

@Composable
fun ExerciseLibraryThumbnail(id: String) {
    val art = exerciseArtwork(id)
    val resources = LocalContext.current.resources
    val pixels = with(LocalDensity.current) { 56.dp.roundToPx() }
    val bitmap by
        produceState<ImageBitmap?>(null, art?.resource, pixels) {
            val resource = art?.resource ?: return@produceState
            val key = "$resource:$pixels"
            value =
                thumbnails.get(key)
                    ?: withContext(Dispatchers.IO) {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeResource(resources, resource, bounds)
                            var sample = 1
                            while (
                                bounds.outWidth / (sample * 2) >= pixels &&
                                    bounds.outHeight / (sample * 2) >= pixels
                            ) sample *= 2
                            BitmapFactory.decodeResource(
                                    resources,
                                    resource,
                                    BitmapFactory.Options().apply { inSampleSize = sample },
                                )
                                ?.asImageBitmap()
                        }
                        ?.also { thumbnails.put(key, it) }
        }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            Modifier.size(56.dp)
                .testTag(
                    if (bitmap != null) "exercise-thumbnail-ready-$id"
                    else "exercise-thumbnail-placeholder-$id"
                ),
    ) {
        if (bitmap != null)
            Image(
                bitmap!!,
                null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        else Box(Modifier.padding(12.dp)) { LibraryMovementIcon() }
    }
}
