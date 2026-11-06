package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.StrengthAnalyticsScreen
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
class StrengthAnalyticsScreenTest {
    @get:Rule val compose = createComposeRule()
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
                        ExerciseCatalog.definition("bench-press").exercise(),
                        listOf(
                            StrengthSession.SetEntry("warm", 5, 20.0, null, true),
                            StrengthSession.SetEntry("working", 6, 60.0, 7.0, false),
                        ),
                    )
                ),
            )
        compose.setContent {
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
            .onNodeWithText("360.0 kg × repetitions · this exact external-load exercise only")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Hide exercise evidence").performScrollTo().performClick()
        compose.onNodeWithText("6 recorded repetitions").assertDoesNotExist()
        Assert.assertEquals(2, source.exercises().single().sets().size)
    }

    @Test
    fun emptyWeeksRemainUnknownTrainingRatherThanAssertedRest() {
        compose.setContent {
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
