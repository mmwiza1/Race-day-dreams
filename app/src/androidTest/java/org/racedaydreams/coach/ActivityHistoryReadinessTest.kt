package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/** Exercise the real lazy history UI with controlled IO completion/cancellation. */
@RunWith(AndroidJUnit4::class)
class ActivityHistoryReadinessTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelledOldQueryCannotMarkReplacementReadyBeforeItsNativeRowArrives() {
        val older = activity("older", "manual", 0)
        val native = activity("native", "rdd", 1)
        val state = mutableStateOf(CoachUiState(loading = false, activities = listOf(older)))
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val releaseCancellation = CompletableDeferred<Unit>()
        val firstFinished = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val result = CompletableDeferred<List<ActivityRecord>>()
        val queries = AtomicInteger()
        var opened: String? = null
        val loader: suspend (Instant?, String?, Int) -> List<ActivityRecord> = { _, _, _ ->
            when (queries.incrementAndGet()) {
                1 -> {
                    currentCoroutineContext().job.invokeOnCompletion { firstFinished.complete(Unit) }
                    firstStarted.complete(Unit)
                    try { awaitCancellation() }
                    finally {
                        firstCancelled.complete(Unit)
                        withContext(NonCancellable) { releaseCancellation.await() }
                    }
                }
                2 -> { secondStarted.complete(Unit); result.await() }
                else -> error("Unexpected duplicate history query")
            }
        }
        try {
            compose.setContent { MaterialTheme { ActivityHistoryScreen(state.value, { opened = it }, loadSummaryPage = loader) } }
            await(firstStarted)
            compose.runOnIdle { state.value = state.value.copy(activities = listOf(native)) }
            await(firstCancelled); await(secondStarted)
            releaseCancellation.complete(Unit)
            // Completion is the actual cancelled LaunchedEffect, after its finally block.
            await(firstFinished)
            compose.onNode(hasStateDescription("Loading recorded history")).assertExists()
            compose.onNode(hasStateDescription("Recorded history ready")).assertDoesNotExist()
            compose.onNodeWithText("Source: Recorded by Race Day Dreams").assertDoesNotExist()
            result.complete(listOf(native))
            waitForReady()
            compose.onNodeWithText("Source: Recorded by Race Day Dreams").assertExists()
            val nativeCard = hasClickAction() and (hasText("Source: Recorded by Race Day Dreams") or hasAnyDescendant(hasText("Source: Recorded by Race Day Dreams")))
            compose.onNode(nativeCard).performClick()
            compose.runOnIdle { assertEquals(native.id(), opened) }
        } finally {
            releaseCancellation.complete(Unit)
            result.complete(emptyList())
        }
    }

    @Test fun failedReplacementShowsUnavailableAndRetryLoadsItsOwnPage() {
        val native = activity("retry-native", "rdd", 1)
        val queries = AtomicInteger()
        compose.setContent { MaterialTheme {
            ActivityHistoryScreen(CoachUiState(loading = false, activities = listOf(native)), {}, loadSummaryPage = { _, _, _ ->
                if (queries.incrementAndGet() == 1) throw IOException("Controlled unavailable page")
                listOf(native)
            })
        } }
        compose.waitUntil(20000) { compose.onAllNodes(hasStateDescription("History unavailable")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasStateDescription("Recorded history ready")).assertDoesNotExist()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        waitForReady()
        compose.onNodeWithText("Source: Recorded by Race Day Dreams").assertExists()
        compose.runOnIdle { assertEquals(2, queries.get()) }
    }

    @Test fun nextPageKeepsTheSummaryCursorAndEarlierRows() {
        val records = (0..30).map { activity("page-$it", "manual", 100 - it) }
        val queries = AtomicInteger()
        var secondCursor: Pair<Instant?, String?>? = null
        var opened: String? = null
        compose.setContent { MaterialTheme {
            ActivityHistoryScreen(CoachUiState(loading = false, activities = records), { opened = it }, loadSummaryPage = { before, id, limit ->
                assertEquals(31, limit)
                when (queries.incrementAndGet()) {
                    1 -> { assertNull(before); assertNull(id); records }
                    2 -> { secondCursor = before to id; records.drop(30) }
                    else -> error("Unexpected duplicate history query")
                }
            })
        } }
        waitForReady()
        compose.onNodeWithText("Load more recorded runs").performScrollTo().performClick()
        compose.waitUntil(20000) { queries.get() == 2 && compose.onAllNodes(hasStateDescription("Recorded history ready")).fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertEquals(records[29].start() to records[29].id(), secondCursor) }
        // The first page remains in the same list after appending the final row.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("Recorded runs · list view").assertExists()
        val recordedCard = hasClickAction() and (hasText("Recorded run") or hasAnyDescendant(hasText("Recorded run")))
        compose.onAllNodes(recordedCard)[0].performClick()
        compose.runOnIdle { assertEquals(records.first().id(), opened) }
        compose.onNodeWithText("Load more recorded runs").assertDoesNotExist()
    }

    private fun waitForReady() = compose.waitUntil(20000) {
        compose.onAllNodes(hasStateDescription("Recorded history ready")).fetchSemanticsNodes().isNotEmpty()
    }

    private fun <T> await(value: Deferred<T>): T = runBlocking { withTimeout(20000) { value.await() } }

    private fun activity(id: String, provider: String, minute: Int): ActivityRecord {
        val start = Instant.parse("2026-01-01T12:00:00Z").plusSeconds(minute * 60L)
        return ActivityRecord(id, SourceIdentity(provider, id, null), start, start.plusSeconds(600),
            ZoneOffset.UTC, ZoneOffset.UTC, 1000.0, null, null, null, null, null, null, null,
            emptyList(), emptyList(), null, null)
    }
}
