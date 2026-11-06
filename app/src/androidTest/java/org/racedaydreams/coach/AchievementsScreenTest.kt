package org.racedaydreams.coach

import android.app.Application
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ViewModelStore()
    private lateinit var model: AchievementsViewModel

    @Before
    fun prepare() {
        io {
            SqliteUserPreferencesRepository(app).clear()
            repeat(5) { index ->
                CoachingStore(app)
                    .saveStrength(
                        StrengthSession(
                            "actual-$index",
                            Instant.now().minusSeconds(86400L * (index + 1)),
                            ZoneOffset.UTC,
                            Duration.ofMinutes(30),
                            null,
                            "",
                            listOf(
                                StrengthSession.ExerciseSets(
                                    ExerciseCatalog.definition("push-up").exercise(),
                                    listOf(StrengthSession.SetEntry("set", 6, 0.0, null, false)),
                                )
                            ),
                        )
                    )
            }
        }
        compose.runOnIdle {
            model =
                ViewModelProvider(
                    store,
                    ViewModelProvider.AndroidViewModelFactory.getInstance(app),
                )[AchievementsViewModel::class.java]
        }
    }

    @After
    fun close() {
        if (::model.isInitialized) compose.waitUntil(20_000) { !model.state.value.loading }
        compose.runOnIdle { store.clear() }
        io { SqliteUserPreferencesRepository(app).clear() }
    }

    @Test fun realMilestonesAreOptionalAndIdempotentInLightMode() = verify(false, 1f)

    @Test fun realMilestonesAreOptionalAndIdempotentInDarkMode() = verify(true, 1f)

    @Test fun realMilestonesRemainReadableAtDoubleText() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        var opened: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    AchievementsScreen(model, onSource = { opened = it.recordId() }) {}
                }
            }
        }
        compose.waitUntil(20_000) { !model.state.value.loading }
        Assert.assertNull(model.state.value.error)
        Assert.assertEquals(2, model.state.value.awards.size)
        Assert.assertFalse(model.state.value.celebrations)
        compose
            .onNodeWithText("✓ Five recorded strength workouts")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onAllNodesWithText("View milestone evidence")[0].performScrollTo().performClick()
        compose
            .onNodeWithText(
                "Retained recognition from 5 recorded sources. Later imports do not award it again or silently redate it."
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onAllNodesWithText("Open recorded workout")[0].performScrollTo().performClick()
        compose.runOnIdle { Assert.assertNotNull(opened) }
        compose
            .onNodeWithContentDescription("Celebrate new milestones in app")
            .performScrollTo()
            .performClick()
        compose.waitUntil(20_000) { !model.state.value.loading && model.state.value.celebrations }
        compose.onNodeWithText("Refresh recorded evidence").performScrollTo().performClick()
        compose.waitUntil(20_000) { !model.state.value.loading }
        Assert.assertEquals(2, model.state.value.awards.size)
        Assert.assertNull(model.state.value.notice)
        Assert.assertEquals(5, io { CoachingStore(app).strength().size })
        Assert.assertTrue(io { SqliteAchievementsRepository(app).celebrations() })
    }

    private fun <T> io(work: () -> T): T = runBlocking(Dispatchers.IO) { work() }
}
