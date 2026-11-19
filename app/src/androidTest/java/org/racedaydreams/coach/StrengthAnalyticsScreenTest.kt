package org.racedaydreams.coach

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.*
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.racedaydreams.coach.strength.StrengthAnalyticsScreen
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeUiApi::class)
class StrengthAnalyticsScreenTest {
    private val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<ComponentActivity>? = null

    // Use the same main-thread Android window/frame owner as production captures. A worker
    // test clock must never measure this AndroidComposeView while Android draws it on main.
    @get:Rule
    val windows: TestRule =
        RuleChain.outerRule(compose)
            .around(
                object : TestRule {
                    override fun apply(base: Statement, description: Description) =
                        object : Statement() {
                            override fun evaluate() {
                                WindowRecomposerPolicy.withFactory(
                                    WindowRecomposerFactory.LifecycleAware
                                ) {
                                    base.evaluate()
                                }
                            }
                        }
                }
            )

    @After
    fun closeWindow() {
        scenario?.close()
        scenario = null
    }

    private fun setProductionContent(content: @Composable () -> Unit) {
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario!!.onActivity { activity -> activity.setContent(content = content) }
    }

    private val now = Instant.parse("2026-11-05T12:00:00Z")

    @Test fun recordedEvidenceAndDisclosuresInLightMode() = verify(false, 1f)

    @Test fun recordedEvidenceAndDisclosuresInDarkMode() = verify(true, 1f)

    @Test fun recordedEvidenceAndDisclosuresAtDoubleText() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        val source =
            StrengthSession(
                "recorded",
                now,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                listOf(
                    StrengthSession.ExerciseSets(
                        requireNotNull(ExerciseCatalog.definition("bench-press")).exercise(),
                        listOf(
                            StrengthSession.SetEntry("warm", 5, 20.0, null, true),
                            StrengthSession.SetEntry("working", 6, 60.0, 7.0, false),
                        ),
                    )
                ),
            )
        setProductionContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    StrengthAnalyticsScreen(listOf(source), {}, now, LocalDate.of(2026, 11, 5))
                }
            }
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("1 recorded working set").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("1 recorded working set").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 recorded workout").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("How are sets attributed?").performScrollTo().performClick()
        compose.onNodeWithText(StrengthVolume.attribution()).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Secondary involvement").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View exercise evidence").performScrollTo().performClick()
        compose.onNodeWithText("6 recorded repetitions").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                "360.0 kg × repetitions · total recorded weight across repetitions for this exercise only"
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Hide exercise evidence").performScrollTo().performClick()
        compose.onNodeWithText("6 recorded repetitions").assertDoesNotExist()
        Assert.assertEquals(2, source.exercises().single().sets().size)
    }

    @Test
    fun emptyWeeksRemainUnknownTrainingRatherThanAssertedRest() {
        setProductionContent {
            MaterialTheme {
                StrengthAnalyticsScreen(emptyList(), {}, now, LocalDate.of(2026, 11, 5))
            }
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("0 recorded working sets").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithText(
                "No strength workouts recorded this week. Missing records do not tell us whether you trained."
            )
            .performScrollTo()
            .assertIsDisplayed()
    }
}
