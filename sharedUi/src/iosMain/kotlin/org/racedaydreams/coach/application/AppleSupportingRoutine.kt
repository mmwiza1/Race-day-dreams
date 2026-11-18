@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.coach.application

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import org.racedaydreams.coach.SharedRoutineContent
import org.racedaydreams.domain.strength.TechniqueIllustrations
import org.racedaydreams.domain.support.RunningRoutines
import org.racedaydreams.recording.AppleRecordingClock
import platform.Foundation.NSBundle
import platform.Foundation.NSUUID
import platform.posix.*

@Composable
internal fun AppleSupportingRoutine(session: AppleCoachSession<*>, id: String?, done: () -> Unit) {
    val routine = remember(id) { id?.let { runCatching { RunningRoutines.get(it) }.getOrNull() } }
    val scope = rememberCoroutineScope()
    var saving by remember(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    SharedRoutineContent(
        id,
        routine,
        saving,
        error,
        illustration = { movement ->
            AppleGuidanceArtwork(TechniqueIllustrations.forRoutine(movement))
        },
        finish = {
            if (!saving && routine != null) {
                saving = true
                error = null
                scope.launch {
                    try {
                        withContext(applicationIoDispatcher()) {
                            session.runningSupport.completeRoutine(
                                NSUUID().UUIDString,
                                requireNotNull(routine.id()),
                                AppleRecordingClock.now(),
                            )
                        }
                        done()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = "Completion could not be saved. Your training plan is unchanged."
                    } finally {
                        saving = false
                    }
                }
            }
        },
    )
}

@Composable
internal fun AppleGuidanceArtwork(art: TechniqueIllustrations.Artwork?) {
    AppleGuidanceImage(
        art,
        Modifier.fillMaxWidth().aspectRatio(1.5f).clip(MaterialTheme.shapes.large),
    ) {
        Text("Visual guide unavailable. Follow the written guidance or choose a familiar movement.")
    }
}

@Composable
internal fun AppleGuidanceImage(
    art: TechniqueIllustrations.Artwork?,
    modifier: Modifier,
    decorative: Boolean = false,
    fallback: @Composable () -> Unit,
) {
    var image by remember(art) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(art) { mutableStateOf(art != null) }
    LaunchedEffect(art) {
        if (art != null) {
            image =
                withContext(applicationIoDispatcher()) {
                    runCatching { loadGuidanceBitmap(art.asset()) }.getOrNull()
                }
        }
        loading = false
    }
    val bitmap = image
    if (bitmap != null && art != null) {
        Image(
            bitmap,
            contentDescription = if (decorative) null else art.description(),
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else if (!loading || decorative) {
        fallback()
    }
}

private fun loadGuidanceBitmap(asset: String): ImageBitmap? {
    val path =
        NSBundle.mainBundle.pathForResource(asset, ofType = "webp", inDirectory = "drawable-nodpi")
            ?: return null
    val file = fopen(path, "rb") ?: return null
    try {
        if (fseeko(file, 0, SEEK_END) != 0) return null
        val size = ftello(file)
        if (size !in 1..4_194_304L || fseeko(file, 0, SEEK_SET) != 0) return null
        val bytes = ByteArray(size.toInt())
        val read = bytes.usePinned { fread(it.addressOf(0), 1u, size.toULong(), file) }
        if (read != size.toULong()) return null
        return SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } finally {
        fclose(file)
    }
}
