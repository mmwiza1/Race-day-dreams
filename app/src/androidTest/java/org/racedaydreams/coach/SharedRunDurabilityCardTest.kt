package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.activity.*

@RunWith(AndroidJUnit4::class)
class SharedRunDurabilityCardTest {
    @get:Rule val compose = createComposeRule()

    private fun run(detailed: Boolean) =
        ActivityRecord(
            "actual",
            SourceIdentity("native", "retained", null),
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(3600),
            null,
            null,
            10800.0,
            null,
            null,
            null,
            null,
            null,
            7.0,
            "",
            if (detailed)
                (0..360).map {
                    DistanceSample(Duration.ofSeconds(it * 10L), it * 30.0, null, null, null, false)
                }
            else emptyList(),
            emptyList(),
            null,
            null,
        )

    @Test
    fun qualifiedHalfPacesUseActualSamplesAndDetailsRemainOptional() {
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachPage {
                    SharedRunDurabilityCard(run(true), false, ::formatPaceSeconds, ::decimal)
                }
            }
        }
        compose.onNodeWithText("Stable pacing").assertExists()
        compose.onNodeWithText("First half: 5:33/km").assertExists()
        compose.onNodeWithText("Second half: 5:33/km").assertExists()
        compose.onNodeWithText("not a precise fitness score", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Durability details").performScrollTo().performClick()
        compose.onNodeWithText("not a precise fitness score", substring = true).assertExists()
    }

    @Test
    fun aggregateOnlyRecordNeverShowsInventedHalfPacesOrHeartRateContext() {
        compose.setContent {
            RaceDayTheme(darkTheme = true, dynamicColour = false) {
                CoachPage {
                    SharedRunDurabilityCard(run(false), false, ::formatPaceSeconds, ::decimal)
                }
            }
        }
        compose.onNodeWithText("Insufficient detailed run data").assertIsDisplayed()
        compose.onNodeWithText("First half:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Second half:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Durability details").performScrollTo().performClick()
        compose.onNodeWithText("HR context unavailable.", substring = true).assertExists()
    }

    @Test
    fun largeTextKeepsImperialMetricsAndDisclosureScrollable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    CoachPage {
                        SharedRunDurabilityCard(run(true), true, ::formatPaceSeconds, ::decimal)
                    }
                }
            }
        }
        compose.onNodeWithText("First half: 8:56/mi").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Second half: 8:56/mi").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Durability details").performScrollTo().performClick()
        compose.onNodeWithText("HR context unavailable.", substring = true).assertExists()
    }
}
