package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.coach.strength.PlainWorkoutTarget
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise

class PlainEnglishUiTest {
    @get:Rule val compose = createComposeRule()

    private fun show(timed: Boolean = false, rir: Boolean = false, large: Boolean = false) {
        val exercise =
            StrengthExercise(
                "task",
                "Task",
                StrengthExercise.Region.FULL,
                if (timed) StrengthExercise.Mode.BODYWEIGHT else StrengthExercise.Mode.WEIGHTED,
                false,
                if (timed) StrengthExercise.MeasurementProfile.DURATION
                else StrengthExercise.MeasurementProfile.LOAD_REPS,
            )
        val effort =
            if (timed) null
            else
                GymWorkoutDefinition.EffortRange(
                    if (rir) GymWorkoutDefinition.EffortScale.RIR
                    else GymWorkoutDefinition.EffortScale.RPE,
                    if (rir) 2.0 else 5.0,
                    if (rir) 3.0 else 7.0,
                )
        val slot =
            GymWorkoutDefinition.Slot(
                "slot",
                exercise,
                GymWorkoutDefinition.Target(
                    2,
                    if (timed) null else 4,
                    if (timed) null else 6,
                    if (timed) 45L else null,
                    null,
                    effort,
                    180,
                    if (timed) null else 82.0,
                ),
                null,
            )
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(d.density, if (large) 2f else 1f)
            ) {
                RaceDayTheme(darkTheme = timed, dynamicColour = false) {
                    Box(Modifier.width(280.dp).fillMaxHeight()) {
                        CoachPage { PlainWorkoutTarget(slot) }
                    }
                }
            }
        }
    }

    @Test
    fun referenceAndExactTargetsRemainBehindRealDisclosureAtDoubleText() {
        show(large = true)
        compose.onNodeWithText("Complete 2 sets of 4–6 repetitions.").assertIsDisplayed()
        compose.onNodeWithText("82 kg", substring = true).assertDoesNotExist()
        compose
            .onNodeWithText("Training targets and weight reference")
            .performScrollTo()
            .performClick()
        compose
            .onNodeWithText("not a weight you must lift or a recorded result", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText("RPE (rating of perceived effort): 5–7 out of 10", substring = true)
            .assertExists()
        compose.onNodeWithText("Hide details").performScrollTo().performClick()
        compose.onNodeWithText("82 kg", substring = true).assertDoesNotExist()
    }

    @Test
    fun timedExerciseUsesTimeRatherThanRepetitionsOrWeight() {
        show(timed = true)
        compose.onNodeWithText("Complete 2 sets of 45 seconds.").assertIsDisplayed()
        compose.onNodeWithText("Rest for 3 minutes between sets.").assertIsDisplayed()
        compose.onNodeWithText("repetitions", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Exact training targets").performScrollTo().performClick()
        compose
            .onNodeWithText("No external weight is prescribed", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun repetitionsInReserveIsExplainedWithoutConvertingToPerceivedEffort() {
        show(rir = true)
        compose
            .onNodeWithText("Finish each set with 2–3 repetitions left before failure.")
            .assertIsDisplayed()
        compose
            .onNodeWithText("Training targets and weight reference")
            .performScrollTo()
            .performClick()
        compose
            .onNodeWithText("RIR (repetitions in reserve): 2–3", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("RPE", substring = true).assertDoesNotExist()
    }
}
