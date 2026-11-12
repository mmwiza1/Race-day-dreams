package org.racedaydreams.coach

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.recording.RunMetricsEngine

/** Real production controls and accepted persisted GPS evidence, without starting a service. */
@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeUiApi::class)
class LiveRunningRefinementTest {
    private val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private val windowRecomposers = mutableMapOf<Lifecycle, MutableList<Recomposer>>()

    private data class ClosingHost(val jobs: List<Job>, val windows: List<Recomposer>)

    private companion object {
        // A failed join is also a barrier against the next test deleting a busy fixture.
        val pendingHostShutdown = mutableListOf<ClosingHost>()
    }

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @get:Rule
    val androidWindows: TestRule =
        RuleChain.outerRule(compose)
            .around(
                TestRule { base, _ ->
                    object : Statement() {
                        override fun evaluate() {
                            val mainWindows = WindowRecomposerFactory { view ->
                                assertSame(Looper.getMainLooper(), Looper.myLooper())
                                val lifecycle =
                                    checkNotNull(view.findViewTreeLifecycleOwner()).lifecycle
                                WindowRecomposerFactory.LifecycleAware.createRecomposer(view).also {
                                    windowRecomposers
                                        .getOrPut(lifecycle) { mutableListOf() }
                                        .add(it)
                                }
                            }
                            WindowRecomposerPolicy.withFactory(mainWindows) { base.evaluate() }
                        }
                    }
                }
            )

    @Before
    fun resetFixture() {
        awaitClosedHosts()
        SqliteUserPreferencesRepository(context).clear()
    }

    @After
    fun closeFixture() {
        closeWindow()
        RecordingStore(context).discardUnfinished("ui-refined-route")
    }

    private fun closeWindow() {
        val host = scenario
        if (host != null) {
            var closing: ClosingHost? = null
            host.onActivity { activity ->
                val lifecycles =
                    windowRecomposers.keys.filter {
                        it == activity.lifecycle || it.currentState == Lifecycle.State.DESTROYED
                    }
                closing =
                    ClosingHost(
                        listOfNotNull(activity.lifecycleScope.coroutineContext[Job]),
                        lifecycles.flatMap { windowRecomposers.remove(it).orEmpty() },
                    )
            }
            synchronized(pendingHostShutdown) { pendingHostShutdown.add(checkNotNull(closing)) }
            try {
                host.close()
            } finally {
                scenario = null
            }
        }
        awaitClosedHosts()
    }

    private fun awaitClosedHosts() {
        val closing = synchronized(pendingHostShutdown) { pendingHostShutdown.toList() }
        if (closing.isEmpty()) return
        runBlocking {
            withTimeout(20000) {
                closing.forEach { host ->
                    host.jobs.forEach { it.join() }
                    host.windows.forEach { it.join() }
                }
            }
        }
        synchronized(pendingHostShutdown) { pendingHostShutdown.removeAll(closing.toSet()) }
    }

    private fun show(
        status: RunMetricsEngine.Status,
        commands: MutableList<String>,
        heartRate: Double? = null,
        scale: Float = 1f,
    ) {
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario!!.onActivity { activity ->
            activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                    RaceDayTheme(darkTheme = scale >= 2f, dynamicColour = false) {
                        ActiveRunPanel(
                            status,
                            null,
                            heartRate,
                            null,
                            null,
                            null,
                            false,
                            commands::add,
                        )
                    }
                }
            }
        }
        compose.waitUntil(20000) {
            compose.onAllNodesWithTag("live-statistics-tab").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun elapsedLeadsWithSeparateCurrentAndActiveAveragePaceAndRecordedHeartRate() {
        val commands = mutableListOf<String>()
        show(
            RunMetricsEngine.Status(5000.0, 1800000, 1500000, 337.0, "GPS ready", false),
            commands,
            152.0,
        )
        compose.onNodeWithTag("live-elapsed").assertExists()
        compose.onNodeWithText("0:30:00").assertExists()
        compose.onNodeWithText("Current pace").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5:37 /km").assertExists()
        compose.onNodeWithText("Active average pace").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5:00 /km").assertExists()
        compose.onNodeWithText("152 bpm").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        compose.onNodeWithText("Finish & save").assertIsDisplayed()
        assertTrue(commands.isEmpty())
    }

    @Test
    fun switchingToThePersistedRoutePreservesGapsCoordinatesAndRecordingCommands() {
        val start = Instant.parse("2026-11-01T12:00:00Z")
        val engine = RunMetricsEngine(0, start)
        for ((second, metres) in
            listOf(0L to 0.0, 1L to 3.0, 2L to 6.0, 63L to 500.0, 64L to 503.0)) {
            engine.accept(
                RunMetricsEngine.Fix(
                    start.plusSeconds(second),
                    second * 1000,
                    51.5,
                    -.1 + metres / 69220.0,
                    3.0,
                    null,
                    null,
                    3.0,
                )
            )
        }
        val store = RecordingStore(context)
        val accepted = engine.points().toList()
        assertTrue(accepted.map { it.segment() }.distinct().size > 1)
        store.start("ui-refined-route", start, 0)
        store.flush("ui-refined-route", accepted, engine.events(), 0, 0, 64000, "ACTIVE")
        val commands = mutableListOf<String>()
        show(engine.status(64000), commands)
        compose.onNodeWithTag("live-route-tab").performScrollTo().performClick()
        compose.waitUntil(20000) {
            compose.onAllNodesWithTag("live-route-canvas").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithTag("live-route-canvas")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasContentDescription("2 continuous GPS sections", substring = true))
        compose.onNodeWithText("GPS gaps remain blank.").assertExists()
        compose.onNodeWithTag("live-statistics-tab").performScrollTo().performClick()
        compose.onNodeWithTag("live-elapsed").assertExists()
        assertEquals(accepted, store.points("ui-refined-route"))
        assertEquals("ACTIVE", store.unfinished()!!.state)
        assertTrue(commands.isEmpty())
    }

    @Test
    fun emptyRouteAndLargeTextRetainSafeActionsWithoutInventingPaceOrHeartRate() {
        RecordingStore(context).start("ui-refined-route", Instant.now(), 0)
        val commands = mutableListOf<String>()
        show(
            RunMetricsEngine.Status(0.0, 60000, 60000, null, "Waiting for GPS", false),
            commands,
            scale = 2f,
        )
        compose.onNodeWithText("Pause").assertIsDisplayed()
        compose.onNodeWithText("Finish & save").assertIsDisplayed()
        compose.onNodeWithText("Heart rate").assertDoesNotExist()
        compose.onNodeWithText("Pace unavailable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("live-route-tab").performScrollTo().performClick()
        compose.waitUntil(20000) {
            compose
                .onAllNodesWithText(
                    "Your route will appear after two usable GPS observations have been saved."
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("live-route-canvas").assertDoesNotExist()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        compose.onNodeWithText("Finish & save").performClick()
        compose.onNodeWithText("Keep recording").performClick()
        assertTrue(commands.isEmpty())
        assertTrue(RecordingStore(context).points("ui-refined-route").isEmpty())
    }
}
