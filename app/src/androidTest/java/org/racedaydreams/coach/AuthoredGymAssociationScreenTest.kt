package org.racedaydreams.coach

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.GymAssociationCard
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
class AuthoredGymAssociationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun explicitAuthoredReviewRetainsFractionalRirAndIndependentActualLoad() = review(false)

    @Test fun authoredReviewRemainsUsableInDarkModeAtTwoHundredPercent() = review(true)

    private fun review(largeDark: Boolean) {
        val proposal = proposal()
        var chosen: GymWorkoutAssociation.Proposal? = null
        var choice: GymWorkoutAssociation.Choice? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else MaterialTheme.colorScheme
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        GymAssociationCard(
                            GymAssociationCoordinator.View(
                                "actual",
                                listOf(proposal),
                                null,
                                null,
                                null,
                            ),
                            true,
                            { p, c ->
                                chosen = p
                                choice = c
                            },
                            {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("gym-association-count-session").performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("gym-association-confirm").fetchSemanticsNodes().size == 1
        }
        compose
            .onNodeWithText("Bench Press · 3 × 5–8 reps · RIR 1.5–3.5 · 80 kg reference")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("RPE", substring = true).assertDoesNotExist()
        compose
            .onNodeWithTag("gym-association-confirm")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.runOnIdle {
            assertEquals(proposal, chosen)
            assertEquals(GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION, choice)
            assertEquals(60.0, chosen!!.actual().exercises()[0].sets()[0].kilograms(), 0.0)
            assertEquals(
                80.0,
                chosen!!
                    .authoredPrescription()
                    .definition()
                    .slots()[0]
                    .target()
                    .referenceLoadKg()!!,
                0.0,
            )
            assertNull(chosen!!.prescription())
        }
    }

    private fun proposal(): GymWorkoutAssociation.Proposal {
        val now = Instant.parse("2026-10-28T12:00:00Z")
        val exercise = ExerciseCatalog.definition("bench-press").exercise()
        val actual =
            StrengthSession(
                "actual",
                now.minusSeconds(3600),
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                listOf(
                    StrengthSession.ExerciseSets(
                        exercise,
                        listOf(StrengthSession.SetEntry("set", 6, 60.0, 7.0, false)),
                    )
                ),
            )
        val target =
            GymWorkoutDefinition.Target(
                3,
                5,
                8,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 1.5, 3.5),
                null,
                80.0,
            )
        val workout =
            StrengthProgramme.Workout(
                "session",
                LocalDate.of(2026, 10, 28),
                20,
                GymWorkoutDefinition(
                    "workout",
                    1,
                    now.minusSeconds(86400),
                    "Authored session",
                    listOf(GymWorkoutDefinition.Slot("bench", exercise, target, null)),
                ),
                null,
            )
        return GymWorkoutAssociation.Proposal.authored(
            actual,
            GymExecution.Origin.FREE,
            "authored",
            1,
            workout,
            now,
        )
    }
}
