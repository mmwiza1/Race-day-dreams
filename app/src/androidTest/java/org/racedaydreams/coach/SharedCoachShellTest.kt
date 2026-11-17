package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.R
import org.racedaydreams.coach.theme.RaceDayTheme

@RunWith(AndroidJUnit4::class)
class SharedCoachShellTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun bottomDestinationsKeepTheirOrderAndSelectionAndDispatchOnlyHostNavigation() {
        val route = mutableStateOf("home")
        val selected = mutableListOf<String>()
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachShell(
                    route.value,
                    false,
                    remember { SnackbarHostState() },
                    {
                        selected.add(it)
                        route.value = it
                    },
                    {},
                    {},
                ) {
                    Text("Preserved host content")
                }
            }
        }
        val labels = listOf("Home", "Plan", "Calendar", "Progress", "Strength")
        val positions =
            labels.map {
                compose
                    .onNodeWithText(it)
                    .assertHasClickAction()
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .left
            }
        assertEquals(positions.sorted(), positions)
        compose.onNodeWithText("Home").assertIsSelected()
        compose.onNodeWithText("Strength").performClick().assertIsSelected()
        compose.onNodeWithText("Plan").performClick().assertIsSelected()
        compose.onNodeWithText("Preserved host content").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("strength", "plan"), selected) }
    }

    @Test
    fun detailBackAndSettingsRemainHostActionsAndDoNotShowTheBottomDestinations() {
        val events = mutableListOf<String>()
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachShell(
                    "gym-workout",
                    false,
                    remember { SnackbarHostState() },
                    { events.add(it) },
                    { events.add("back") },
                    { events.add("settings") },
                ) {
                    Text("Actual workout stays hosted")
                }
            }
        }
        compose.onNodeWithText("Home").assertDoesNotExist()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Actual workout stays hosted").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("back", "settings"), events) }
    }

    @Test
    fun darkLargeTextKeepsEveryBottomDestinationAccessibleAndPreservesDatedStrengthSelection() {
        val selected = mutableListOf<String>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RaceDayTheme(darkTheme = true, dynamicColour = false) {
                    CoachShell(
                        "strength?date={date}",
                        false,
                        remember { SnackbarHostState() },
                        selected::add,
                        {},
                        {},
                    ) {
                        Text("Hosted destination")
                    }
                }
            }
        }
        listOf("Home", "Plan", "Calendar", "Progress", "Strength").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed().assertHasClickAction()
        }
        compose.onNodeWithContentDescription("Strength").assertIsSelected()
        compose.onNodeWithContentDescription("Progress").performClick()
        compose.onNodeWithText("RDD").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("progress"), selected) }
    }

    @Test
    fun railRetainsDatedStrengthSelectionAndDestinationCallbacks() {
        val selected = mutableListOf<String>()
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachShell(
                    "strength?date={date}",
                    true,
                    remember { SnackbarHostState() },
                    selected::add,
                    {},
                    {},
                ) {
                    Text("Rail content")
                }
            }
        }
        compose.onNodeWithText("Strength").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Home").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("home"), selected) }
    }

    @Test
    fun sharedNavigationIconsRenderExactlyLikeTheOriginalAndroidAssets() {
        val originals =
            linkedMapOf(
                "home" to R.drawable.ic_nav_home,
                "plan" to R.drawable.ic_nav_plan,
                "calendar" to R.drawable.ic_nav_calendar,
                "progress" to R.drawable.ic_nav_progress,
                "strength" to R.drawable.ic_nav_strength,
            )
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                Surface {
                    Column {
                        originals.forEach { (route, drawable) ->
                            Row {
                                Box(Modifier.testTag("shared-$route")) {
                                    CoachDestinationIcon(route)
                                }
                                Icon(
                                    painterResource(drawable),
                                    null,
                                    Modifier.testTag("original-$route"),
                                )
                            }
                        }
                    }
                }
            }
        }
        originals.keys.forEach { route ->
            val shared = compose.onNodeWithTag("shared-$route").captureToImage().toPixelMap()
            val original = compose.onNodeWithTag("original-$route").captureToImage().toPixelMap()
            assertEquals(original.width, shared.width)
            assertEquals(original.height, shared.height)
            for (x in 0 until original.width) for (y in 0 until original.height) {
                assertEquals("$route pixel $x,$y", original[x, y], shared[x, y])
            }
        }
    }
}
