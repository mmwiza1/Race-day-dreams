package org.racedaydreams.coach

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic delayed/cancelled decode and missing-file publication, independent of disk speed.
 */
class DreamArtworkPublicationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun obsoleteNonCancellableDecodeCannotPublishOverTheCurrentCity() {
        val selected = mutableStateOf(listOf("old-hero", "old-poster"))
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val oldFinished = CompletableDeferred<Unit>()
        val pixel = ImageBitmap(1, 1)
        compose.setContent {
            val artwork by
                rememberDreamArtwork(selected.value) { asset ->
                    if (asset == "old-hero") {
                        started.complete(Unit)
                        // BitmapFactory is synchronous too: cancellation cannot interrupt its
                        // decode.
                        withContext(NonCancellable) { release.await() }
                        oldFinished.complete(Unit)
                    }
                    pixel
                }
            Text(artwork.status)
            Text(artwork.assets.joinToString())
        }
        try {
            compose.waitUntil(20000) { started.isCompleted }
            compose.onNodeWithText("Loading bundled city artwork").assertExists()
            compose.runOnIdle { selected.value = listOf("new-hero", "new-poster") }
            compose.waitUntil(20000) {
                compose
                    .onAllNodesWithText("Offline Dream artwork ready")
                    .fetchSemanticsNodes()
                    .size == 1
            }
            compose.onNodeWithText("new-hero, new-poster").assertExists()
            release.complete(Unit)
            compose.waitUntil(20000) { oldFinished.isCompleted }
            compose.waitForIdle()
            compose.onNodeWithText("new-hero, new-poster").assertExists()
            compose.onNodeWithText("old-hero, old-poster").assertDoesNotExist()
            compose.onNodeWithText("Offline Dream artwork ready").assertExists()
        } finally {
            release.complete(Unit)
        }
    }

    @Test
    fun missingPosterFinishesAsUnavailableRatherThanPermanentLoadingOrFalseReadiness() {
        val pixel = ImageBitmap(1, 1)
        compose.setContent {
            val artwork by
                rememberDreamArtwork(listOf("hero", "missing-poster")) { asset ->
                    if (asset == "hero") pixel else null
                }
            Text(artwork.status)
        }
        compose.waitUntil(20000) {
            compose
                .onAllNodesWithText("Bundled Dream artwork unavailable: missing-poster")
                .fetchSemanticsNodes()
                .size == 1
        }
        compose.onNodeWithText("Loading bundled city artwork").assertDoesNotExist()
        compose.onNodeWithText("Offline Dream artwork ready").assertDoesNotExist()
    }
}
