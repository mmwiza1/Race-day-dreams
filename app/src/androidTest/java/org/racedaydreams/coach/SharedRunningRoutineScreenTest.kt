package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.data.SupportStore

/** Actual production wrapper + SQLite completion; optional routines never create gym actuals. */
class SharedRunningRoutineScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val pool = Executors.newSingleThreadExecutor()

    private fun <T> io(work: () -> T): T = pool.submit(Callable { work() }).get()

    private val finished = AtomicInteger()

    @Before
    fun clear() {
        io { SqliteUserPreferencesRepository(context).clear() }
    }

    @After
    fun close() {
        compose.waitForIdle()
        io { SqliteUserPreferencesRepository(context).clear() }
        pool.shutdownNow()
    }

    private fun show(largeDark: Boolean = false, id: String = "warm-easy") {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                RaceDayTheme(darkTheme = largeDark, dynamicColour = false) {
                    RoutineScreen(id, "related-workout") { finished.incrementAndGet() }
                }
            }
        }
    }

    private fun verifyCompletion() {
        compose.onNodeWithText("1 of 2").assertExists()
        assertEquals(0, io { SupportStore(context).routineCount(Instant.EPOCH) })
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("2 of 2").assertExists()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("1 of 2").assertExists()
        compose.onNodeWithText("Next").performClick()
        assertEquals(0, io { SupportStore(context).routineCount(Instant.EPOCH) })
        compose.onNodeWithText("Finish routine").assertIsDisplayed().performClick()
        compose.waitUntil(20_000) { finished.get() == 1 }
        assertEquals(
            1,
            io { SupportStore(context).routineCount(Instant.EPOCH, "warm-easy", "related-workout") },
        )
        assertTrue(io { CoachingStore(context).strength().isEmpty() })
    }

    @Test
    fun sharedRoutineNavigationAndExplicitCompletionInLightTheme() {
        show()
        verifyCompletion()
    }

    @Test
    fun sharedRoutineControlsRemainUsableInDarkThemeAtDoubleText() {
        show(largeDark = true)
        verifyCompletion()
    }

    @Test
    fun unknownRoutineStaysUnavailableAndCreatesNoCompletion() {
        show(id = "unknown-routine")
        compose.onNodeWithText("Routine unavailable").assertIsDisplayed()
        compose.onNodeWithText("Finish routine").assertDoesNotExist()
        assertEquals(0, finished.get())
        assertEquals(0, io { SupportStore(context).routineCount(Instant.EPOCH) })
    }
}
