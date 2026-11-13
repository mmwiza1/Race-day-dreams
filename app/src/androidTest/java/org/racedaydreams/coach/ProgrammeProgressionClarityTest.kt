package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.domain.strength.*

/** Production content; integration tests separately prove draft/CAS/activation/history safety. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ProgrammeProgressionClarityTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var inputModeManager: InputModeManager

    private fun review(
        id: String = "bench-press",
        currentLoad: Double? = 80.0,
        effort: Boolean = true,
    ): ProgrammeProgressionReview {
        val exercise = requireNotNull(ExerciseCatalog.definition(id)).exercise()
        val profile = exercise.measurement()
        val target =
            GymWorkoutDefinition.Target(
                2,
                if (profile.usesReps()) 4 else null,
                if (profile.usesReps()) 6 else null,
                if (profile.usesDuration()) 45L else null,
                if (profile.usesDistance()) 40.0 else null,
                if (effort)
                    GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 5.0, 7.0)
                else null,
                180,
                if (profile.usesLoad()) currentLoad else null,
            )
        val proposed =
            if (!effort) null
            else
                GymWorkoutDefinition.Target(
                    2,
                    target.minimumReps(),
                    target.maximumReps(),
                    target.seconds(),
                    target.metres(),
                    target.effort(),
                    180,
                    if (profile.usesLoad()) 82.0 else null,
                )
        val advice =
            StrengthTargetProgression.Advice(
                if (effort) StrengthProgressionAdvisor.Action.CONSIDER_INCREASE
                else StrengthProgressionAdvisor.Action.HOLD,
                if (effort) "Two comparable recorded workouts support an optional change."
                else "Missing effort remains unknown.",
                proposed,
                listOf("one", "two"),
            )
        return ProgrammeProgressionReview(
            "draft",
            1,
            "workout",
            GymWorkoutDefinition.Slot("slot-$id", exercise, target, null),
            advice,
        )
    }

    private fun install(
        review: ProgrammeProgressionReview,
        largeDark: Boolean = false,
        keep: () -> Unit = {},
        save: () -> Unit = {},
    ) {
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeDark) 2f else 1f)
            ) {
                MaterialTheme(
                    colorScheme = if (largeDark) darkColorScheme() else lightColorScheme()
                ) {
                    ProgrammeProgressionContent(review, false, null, keep, save)
                }
            }
        }
    }

    private fun scroll(text: String) {
        compose.onNodeWithTag("progression-review").performScrollToNode(hasText(text))
    }

    @Test
    fun advancedDetailsCollapseAndKeyboardExpandHaveStateSemantics() {
        install(review())
        compose.onNodeWithText("Your workout suggestion").assertIsDisplayed()
        compose.onNodeWithText("Here's what to do").assertIsDisplayed()
        compose.onNodeWithText("What's changing?").assertDoesNotExist()
        scroll("Advanced details")
        val disclosure = compose.onNodeWithText("Advanced details")
        disclosure
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        // Compose key injection bypasses Android's touch-mode transition. Establish the
        // platform keyboard mode first; real keyboard use does this before requesting focus.
        compose.runOnIdle {
            assertTrue(
                "Platform must enter keyboard mode",
                inputModeManager.requestInputMode(InputMode.Keyboard),
            )
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(InputMode.Keyboard, inputModeManager.inputMode) }
        disclosure.performSemanticsAction(SemanticsActions.RequestFocus) {
            assertTrue("Disclosure must receive keyboard focus", it())
        }
        disclosure.assertIsFocused()
        disclosure.performKeyInput { pressKey(Key.Enter) }
        disclosure.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded")
        )
        scroll("What's changing?")
        compose.onNodeWithText("What's changing?").assertIsDisplayed()
        scroll("Advanced details")
        disclosure.performClick()
        disclosure.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed")
        )
        compose.onNodeWithText("What's changing?").assertDoesNotExist()
    }

    @Test
    fun currentProposedAndRecordedWeightAreDifferentFacts() {
        var review = review()
        val exercise = review.slot.exercise()
        val workouts =
            listOf("one", "two").mapIndexed { index, id ->
                StrengthSession(
                    id,
                    Instant.parse("2026-11-01T12:00:00Z").minusSeconds(index * 86400L),
                    ZoneOffset.UTC,
                    Duration.ofMinutes(20),
                    null,
                    "",
                    listOf(
                        StrengthSession.ExerciseSets(
                            exercise,
                            listOf(StrengthSession.SetEntry("set-$id", 6, 80.0, 7.0, false)),
                        )
                    ),
                )
            }
        review = review.copy(supportingWorkouts = workouts)
        install(review)
        compose
            .onNodeWithTag("progression-review")
            .performScrollToNode(hasText("current weight reference is 80 kg", substring = true))
        compose
            .onNode(hasText("current weight reference is 80 kg", substring = true))
            .assertIsDisplayed()
        compose
            .onNodeWithTag("progression-review")
            .performScrollToNode(
                hasText("suggested new weight reference is 82 kg", substring = true)
            )
        compose
            .onNode(hasText("suggested new weight reference is 82 kg", substring = true))
            .assertIsDisplayed()
        compose
            .onNode(hasText("not a weight you must lift or a recorded result", substring = true))
            .assertExists()
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("View supporting workouts")
        compose
            .onNodeWithText("View supporting workouts")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithText("80 kg recorded · 6 repetitions").assertDoesNotExist()
        compose.onNodeWithText("View supporting workouts").performClick()
        scroll("Supporting workouts")
        compose.onAllNodesWithText("80 kg recorded · 6 repetitions").assertCountEquals(2)
        compose.onNodeWithText("82 kg recorded · 6 repetitions").assertDoesNotExist()
    }

    @Test
    fun missingCurrentReferenceIsNotInventedFromSuggestedLoad() {
        install(review(currentLoad = null))
        scroll("Your current target has no weight reference.")
        compose.onNodeWithText("Your current target has no weight reference.").assertIsDisplayed()
        compose
            .onNodeWithTag("progression-review")
            .performScrollToNode(
                hasText("suggested new weight reference is 82 kg", substring = true)
            )
        compose
            .onNode(hasText("suggested new weight reference is 82 kg", substring = true))
            .assertIsDisplayed()
        compose
            .onNode(hasText("training history includes an 82", substring = true))
            .assertDoesNotExist()
    }

    @Test
    fun saveAndKeepAreSeparateAndNeverActivateAutomaticallyAtLargeDarkText() {
        var saved = 0
        var kept = 0
        install(review(), largeDark = true, keep = { kept++ }, save = { saved++ })
        compose.onNodeWithText("Keep my current target").assertIsDisplayed().performClick()
        assertEquals(1, kept)
        assertEquals(0, saved)
        scroll("Save this suggestion for review")
        compose.onNodeWithText("Save this suggestion for review").assertIsDisplayed().performClick()
        assertEquals(1, saved)
        assertEquals(1, kept)
        scroll(
            "Saving this suggestion won't change your active programme. You can review and activate it separately."
        )
        compose
            .onNodeWithText(
                "Saving this suggestion won't change your active programme. You can review and activate it separately."
            )
            .assertIsDisplayed()
    }

    @Test
    fun missingEffortOffersNoSaveOrInventedEffortAtLargeDarkText() {
        install(review(effort = false), largeDark = true)
        compose.onNodeWithText("Save this suggestion for review").assertDoesNotExist()
        scroll("Why this suggestion?")
        compose.onNodeWithText("Missing effort remains unknown.").assertExists()
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("What's staying the same?")
        compose.onNodeWithText("No effort target is specified.").assertExists()
    }

    @Test
    fun durationDistanceAndBodyweightUseTheirOwnTaskInstructions() {
        var current by mutableStateOf(review("plank", currentLoad = null))
        compose.setContent {
            MaterialTheme { ProgrammeProgressionContent(current, false, null, {}, {}) }
        }
        compose.onNodeWithText("Complete 2 sets of 45 seconds.").assertIsDisplayed()
        compose.runOnIdle { current = review("farmers-carry") }
        compose.onNodeWithTag("progression-review").performScrollToIndex(0)
        compose.onNodeWithText("Complete 2 sets of 40 metres.").assertIsDisplayed()
        val carry = review("farmers-carry")
        val target = carry.slot.target()
        val longerCarry =
            GymWorkoutDefinition.Target(2, null, null, null, 45.0, target.effort(), 180, 80.0)
        compose.runOnIdle {
            current =
                carry.copy(
                    advice =
                        StrengthTargetProgression.Advice(
                            StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                            "A small distance increase at the same proven load is optional.",
                            longerCarry,
                            listOf("one", "two"),
                        )
                )
        }
        compose.onNodeWithTag("progression-review").performScrollToIndex(0)
        compose.onNodeWithText("Complete 2 sets of 45 metres.").assertIsDisplayed()
        scroll("Your weight reference is unchanged in this suggestion.")
        compose
            .onNodeWithText("Your weight reference is unchanged in this suggestion.")
            .assertIsDisplayed()
        compose.onAllNodes(hasText("0 kg more", substring = true)).assertCountEquals(0)
        compose.runOnIdle { current = review("push-up", currentLoad = null) }
        compose.onNodeWithTag("progression-review").performScrollToIndex(0)
        compose.onNodeWithText("Complete 2 sets of 4–6 repetitions.").assertIsDisplayed()
        compose
            .onNodeWithText("Use your body weight; no added weight is prescribed.")
            .assertExists()
    }

    @Test
    fun repetitionsInReserveAndAddedWeightAreExplainedWithoutRpeConversion() {
        val base = review("weighted-pull-up", currentLoad = 20.0)
        val effort =
            GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.0, 3.0)
        val target = GymWorkoutDefinition.Target(2, 4, 6, null, null, effort, 90, 20.0)
        val proposed = GymWorkoutDefinition.Target(2, 4, 6, null, null, effort, 90, 20.5)
        val review =
            base.copy(
                slot =
                    GymWorkoutDefinition.Slot(base.slot.id(), base.slot.exercise(), target, null),
                advice =
                    StrengthTargetProgression.Advice(
                        StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                        "Comparable added-weight evidence supports an optional change.",
                        proposed,
                        listOf("one", "two"),
                    ),
            )
        install(review)
        compose
            .onNodeWithText("Finish each set with 2–3 repetitions left before failure.")
            .assertExists()
        compose
            .onNodeWithTag("progression-review")
            .performScrollToNode(
                hasText("current added-weight reference is 20 kg", substring = true)
            )
        compose
            .onNode(hasText("current added-weight reference is 20 kg", substring = true))
            .assertIsDisplayed()
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("What's staying the same?")
        compose.onAllNodesWithText("RIR 2–3").assertCountEquals(1)
        compose.onNodeWithText("RIR = repetitions left before failure.").assertExists()
        compose.onAllNodes(hasText("RPE ", substring = true)).assertCountEquals(0)
    }

    @Test
    fun expandedDetailsRestoreAfterStateRecreation() {
        val restoration = StateRestorationTester(compose)
        val review = review()
        restoration.setContent {
            MaterialTheme { ProgrammeProgressionContent(review, false, null, {}, {}) }
        }
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        restoration.emulateSavedInstanceStateRestore()
        scroll("Advanced details")
        compose
            .onNodeWithText("Advanced details")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        scroll("What's changing?")
        compose.onNodeWithText("What's changing?").assertIsDisplayed()
    }

    @Test
    fun onlyGenuineChangesAreHighlightedAndUnchangedTargetsAppearOnce() {
        install(review())
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("What's changing?")
        compose.onNodeWithTag("change-weight-reference").assertExists()
        compose.onNodeWithTag("change-sets").assertDoesNotExist()
        compose.onNodeWithTag("change-effort").assertDoesNotExist()
        // This is a lazy page: materialise the unchanged card before inspecting its facts.
        scroll("What's staying the same?")
        compose.onNodeWithTag("unchanged-sets").assertExists()
        compose.onNodeWithTag("unchanged-repetitions").assertExists()
        compose.onNodeWithTag("unchanged-effort").assertExists()
        compose.onNodeWithTag("unchanged-rest").assertExists()
        compose.onAllNodesWithText("RPE 5–7/10").assertCountEquals(1)
        compose.onNodeWithText("Current target").assertDoesNotExist()
        compose.onNodeWithText("Proposed target").assertDoesNotExist()
        scroll("View supporting workouts")
        compose.onNodeWithText("Actual logged working sets").assertDoesNotExist()
    }

    @Test
    fun largeTextStacksComparisonAndSupportingDisclosureAcceptsKeyboard() {
        install(review(), largeDark = true)
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("What's changing?")
        val before =
            compose.onNodeWithTag("change-weight-reference-current").getUnclippedBoundsInRoot()
        val after =
            compose.onNodeWithTag("change-weight-reference-suggested").getUnclippedBoundsInRoot()
        assertTrue("200% values must stack vertically", after.top >= before.bottom)
        scroll("View supporting workouts")
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        compose.waitForIdle()
        val supporting = compose.onNodeWithText("View supporting workouts")
        supporting.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        supporting.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        supporting.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded")
        )
        scroll("Supporting workouts")
        compose.onNodeWithText("Actual logged working sets").assertExists()
        scroll("View supporting workouts")
        supporting.performClick()
        supporting.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed")
        )
        compose.onNodeWithText("Actual logged working sets").assertDoesNotExist()
    }

    @Test
    fun nestedDisclosureRestoresAndClosesWhenAdvancedIsCollapsed() {
        val restoration = StateRestorationTester(compose)
        val review = review()
        restoration.setContent {
            MaterialTheme { ProgrammeProgressionContent(review, false, null, {}, {}) }
        }
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("View supporting workouts")
        compose.onNodeWithText("View supporting workouts").performClick()
        restoration.emulateSavedInstanceStateRestore()
        scroll("View supporting workouts")
        compose
            .onNodeWithText("View supporting workouts")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        compose.onNodeWithText("View supporting workouts").assertDoesNotExist()
        scroll("Advanced details")
        compose.onNodeWithText("Advanced details").performClick()
        scroll("View supporting workouts")
        compose
            .onNodeWithText("View supporting workouts")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithText("Actual logged working sets").assertDoesNotExist()
    }
}
