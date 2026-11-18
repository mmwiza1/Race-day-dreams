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
import org.racedaydreams.coach.strength.StrengthRecordsScreen
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeUiApi::class)
class StrengthRecordsScreenTest {
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

    @Test fun measuredAndEstimatedPerformanceAreSeparateInLightMode() = verify(false, 1f)

    @Test fun measuredAndEstimatedPerformanceAreSeparateInDarkMode() = verify(true, 1f)

    @Test fun measuredAndEstimatedPerformanceAreSeparateAtDoubleText() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        val exercise = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
        val source =
            StrengthSession(
                "actual",
                Instant.now().minusSeconds(86400),
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                listOf(
                    StrengthSession.ExerciseSets(
                        exercise,
                        listOf(
                            StrengthSession.SetEntry("warm", 1, 150.0, null, true),
                            StrengthSession.SetEntry("work", 6, 80.0, null, false),
                        ),
                    )
                ),
            )
        var destination: String? = null
        var historical = 0
        setProductionContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    StrengthRecordsScreen(
                        listOf(source),
                        MassUnit.KG,
                        1,
                        { historical++ },
                        { destination = it },
                        {},
                    )
                }
            }
        }
        compose.waitUntil(20_000) {
            compose
                .onAllNodesWithText("Heaviest recorded working set")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("80.0 kg for 6 repetitions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("About 96 kg").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                "A calculation of a possible single-repetition maximum, not a recorded lift or a training-weight suggestion."
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("How is this estimated?").performScrollTo().performClick()
        compose
            .onNodeWithText(StrengthRecords.estimateExplanation())
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("View estimate's source workout").performScrollTo().performClick()
        compose.runOnIdle { Assert.assertEquals("actual", destination) }
        compose.onNodeWithText("View measured history").performScrollTo().performClick()
        compose
            .onNodeWithText("Best recorded load for 6 repetitions")
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText("Most recorded repetitions at 80.0 kg")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Hide measured history").performScrollTo().performClick()
        compose.onNodeWithText("Most recorded repetitions at 80.0 kg").assertDoesNotExist()
        compose
            .onNodeWithText("Historical entries & full PB history")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { Assert.assertEquals(1, historical) }
        Assert.assertEquals(2, source.exercises().single().sets().size)
    }
}
