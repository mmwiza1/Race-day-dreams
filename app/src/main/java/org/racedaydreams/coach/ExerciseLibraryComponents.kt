package org.racedaydreams.coach

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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

@Composable
fun LibrarySearchIcon() {
    val colour = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        drawCircle(
            colour,
            size.minDimension * .27f,
            Offset(size.width * .42f, size.height * .42f),
            style = Stroke(2.dp.toPx()),
        )
        drawLine(
            colour,
            Offset(size.width * .62f, size.height * .62f),
            Offset(size.width * .88f, size.height * .88f),
            2.dp.toPx(),
        )
    }
}

@Composable
fun LibraryOpenIcon() {
    val colour = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(16.dp)) {
        drawLine(
            colour,
            Offset(size.width * .35f, size.height * .2f),
            Offset(size.width * .65f, size.height * .5f),
            2.dp.toPx(),
        )
        drawLine(
            colour,
            Offset(size.width * .65f, size.height * .5f),
            Offset(size.width * .35f, size.height * .8f),
            2.dp.toPx(),
        )
    }
}

@Composable
private fun LibraryMovementIcon() {
    val colour = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxSize()) {
        drawLine(
            colour,
            Offset(size.width * .2f, size.height * .5f),
            Offset(size.width * .8f, size.height * .5f),
            2.dp.toPx(),
        )
        for (x in listOf(.2f, .8f)) drawLine(
            colour,
            Offset(size.width * x, size.height * .25f),
            Offset(size.width * x, size.height * .75f),
            4.dp.toPx(),
        )
    }
}

/** Compact, readable at200%; name/alias matching remains owned by the existing callers. */
@Composable
fun CoachExerciseSearch(query: String, change: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = change,
        label = { Text("Search") },
        placeholder = { Text("Name or alias") },
        modifier =
            Modifier.fillMaxWidth().testTag("exercise-search").semantics {
                contentDescription = "Search exercises by name or alias"
            },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        leadingIcon = { LibrarySearchIcon() },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}
