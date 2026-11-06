package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.domain.recovery.TrainingRecovery

@RunWith(AndroidJUnit4::class)
class TrainingRecoveryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightOptionsDoNotActivate() = verify(false, 1f)

    @Test fun darkOptionsDoNotActivate() = verify(true, 1f)

    @Test fun doubleTextOptionsRemainReachable() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        var kept = 0
        var proposed = 0
        val evidence =
            TrainingRecovery.Evidence(
                "strength",
                "actual",
                Instant.parse("2026-11-04T12:00:00Z"),
                "Several working sets had high recorded effort.",
            )
        val review =
            TrainingRecovery.Review(
                LocalDate.of(2026, 11, 5),
                "An optional lighter week is available for review",
                "Keep your programme or review fewer working sets for one week.",
                true,
                true,
                listOf(evidence),
                listOf("Missing effort remains unknown."),
            )
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    TrainingRecoveryContent(
                        TrainingRecoveryState(false, review, canReviewLighterWeek = true),
                        {},
                        {},
                        { kept++ },
                        { proposed++ },
                        { _, _ -> Assert.fail("No move was selected") },
                    )
                }
            }
        }
        compose.onNodeWithText(evidence.explanation()).assertDoesNotExist()
        compose
            .onNodeWithText("Supporting evidence ▾")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .performClick()
        compose
            .onNodeWithText("Missing effort remains unknown.")
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText("Supporting evidence ▴")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
            .performClick()
        compose.onNodeWithText("Missing effort remains unknown.").assertDoesNotExist()
        compose.onNodeWithText("Keep my current programme").performScrollTo().performClick()
        compose.runOnIdle {
            Assert.assertEquals(1, kept)
            Assert.assertEquals(0, proposed)
        }
        compose.onNodeWithText("Preview a lighter week").performScrollTo().performClick()
        compose.runOnIdle {
            Assert.assertEquals(1, proposed)
            Assert.assertEquals(1, review.evidence().size)
        }
        compose
            .onNodeWithText("No targets or future weeks have changed.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun unknownEvidenceNeverOffersLighterWeek() {
        val review =
            TrainingRecovery.review(
                emptyList(),
                emptyList(),
                emptyList(),
                Instant.parse("2026-11-05T12:00:00Z"),
                ZoneOffset.UTC,
            )
        compose.setContent {
            MaterialTheme {
                TrainingRecoveryContent(
                    TrainingRecoveryState(false, review),
                    {},
                    {},
                    {},
                    { Assert.fail("Unsupported deload") },
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Preview a lighter week").assertDoesNotExist()
        compose
            .onNodeWithText("No programme change is suggested from these records")
            .assertIsDisplayed()
    }
}
