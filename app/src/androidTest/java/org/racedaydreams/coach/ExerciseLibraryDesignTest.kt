package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.ExerciseBrowseScreen
import org.racedaydreams.coach.theme.RaceDayTheme

@RunWith(AndroidJUnit4::class)
class ExerciseLibraryDesignTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun wrappedFiltersKeepSelectionAndSeparateTouchTargetsAtDoubleText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(dynamicColour = false) {
                    var selected by remember { mutableStateOf("Chest") }
                    FlowRow(
                        Modifier.width(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    ) {
                        listOf("Chest", "Shoulders", "Core").forEach { label ->
                            CoachFilterChip(
                                selected == label,
                                { selected = label },
                                { Text(label) },
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Chest").assertIsSelected()
        compose.onNodeWithText("Core").performClick().assertIsSelected()
        compose.onNodeWithText("Chest").assertIsNotSelected()
        val nodes =
            listOf("Chest", "Shoulders", "Core").map {
                compose.onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode()
            }
        nodes.forEach { assertTrue(it.boundsInRoot.height >= 48 * compose.density.density - 1) }
        nodes.zipWithNext().forEach { (a, b) ->
            assertFalse(a.boundsInRoot.overlaps(b.boundsInRoot))
        }
    }

    @Test
    fun broadCategoryAndAliasSearchRemainRealFiltersInDarkLargeText() {
        var opened: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    Box(Modifier.width(280.dp).fillMaxHeight()) {
                        ExerciseBrowseScreen(CoachUiState(loading = false), {}, { opened = it })
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Add custom exercise").assertIsDisplayed()
        compose
            .onNodeWithTag("exercise-category-Chest")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithTag("exercise-search").performScrollTo().performTextInput("Bench Press")
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasTestTag("exercise-row-bench-press"))
        compose.onNodeWithTag("exercise-row-bench-press").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("bench-press", opened) }
        // The clickable card merges its name/context. Its decorative image readiness tag
        // deliberately lives in the unmerged tree; require the actual bundled bitmap.
        compose.waitUntil(20000) {
            compose
                .onAllNodesWithTag("exercise-thumbnail-ready-bench-press", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose
            .onNodeWithTag("exercise-thumbnail-ready-bench-press", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("exercise-search").performScrollTo().performTextReplacement("")
        compose.onNodeWithTag("exercise-shortcut-Excluded").performScrollTo().performClick()
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasText("Excluded exercises"))
        compose
            .onNodeWithText("Excluded exercises")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsProperties.Heading
                )
            )
    }

    @Test
    fun detailTabsScrollAndChangeActualSectionAtDoubleText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(dynamicColour = false) {
                    var section by remember { mutableStateOf("Overview") }
                    Column(Modifier.width(280.dp)) {
                        CoachSectionTabs(
                            listOf("Overview", "How to do it", "History", "PBs"),
                            section,
                        ) {
                            section = it
                        }
                        Text("Section: $section")
                    }
                }
            }
        }
        compose.onNodeWithText("How to do it").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Section: How to do it").assertIsDisplayed()
        compose.onNodeWithText("PBs").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Section: PBs").assertIsDisplayed()
    }
}
