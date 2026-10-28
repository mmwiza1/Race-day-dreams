package org.racedaydreams.coach

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.DayOfWeek
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.navigation.*
import org.racedaydreams.domain.*

/** Existing flat route contracts must survive feature ownership changes. */
@RunWith(AndroidJUnit4::class)
class FeatureNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun aRetainedFeatureGraphRendersLaterCoherentSnapshots() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModels = ViewModelStore()
        lateinit var model: CoachViewModel
        instrumentation.runOnMainSync {
            model = CoachViewModel(application, SavedStateHandle())
            viewModels.put("reactive-navigation", model)
        }
        fun profile(distance: RaceDistance) =
            UserProfile(
                RaceGoal(distance, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.ZONE_2,
                    EnumSet.allOf(DayOfWeek::class.java),
                    3,
                    DayOfWeek.SUNDAY,
                    EnumSet.noneOf(DayOfWeek::class.java),
                    ScheduleType.FLEXIBLE,
                    UnitSystem.METRIC,
                ),
                false,
            )
        val current =
            mutableStateOf(CoachUiState(loading = false, profile = profile(RaceDistance.FIVE_K)))
        try {
            compose.setContent {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") { homeGraph({ current.value }, model, nav) }
            }
            compose.onNodeWithText("5K").performScrollTo().assertIsDisplayed()
            compose.runOnIdle {
                current.value = current.value.copy(profile = profile(RaceDistance.TEN_K))
            }
            compose.onNodeWithText("10K").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("5K").assertCountEquals(0)
        } finally {
            instrumentation.runOnMainSync { viewModels.clear() }
        }
    }

    @Test
    fun featureGraphsRetainAllExistingRoutesWithoutIntroducingNestedBackStacks() {
        val expected =
            setOf(
                "welcome",
                "home",
                "weekly",
                "routines",
                "routine/{id}?related={related}",
                "run-now",
                "spontaneous-run",
                "record-run",
                "execute/{version}/{workout}",
                "race",
                "race-run/{version}/{workout}/{hilly}",
                "baseline",
                "manual",
                "assessment",
                "goals",
                "plan",
                "running-plan",
                "workout/{id}",
                "history?kind={kind}",
                "calendar",
                "activity/{id}",
                "strength?date={date}",
                "strength/log",
                "strength/pbs",
                "strength/source/{id}",
                "strength/history",
                "gym",
                "gym-now",
                "gym-workout",
                "gym-workout-builder",
                "gym/session/{id}",
                "exercises",
                "exercise/{id}",
                "progress",
                "progress/pbs",
                "progress/trends",
                "progress/weight",
                "shoes",
                "settings",
                "health-intelligence",
            )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val viewModels = ViewModelStore()
            val model = CoachViewModel(application, SavedStateHandle())
            viewModels.put("navigation-contract", model)
            try {
                val nav = NavHostController(application)
                nav.navigatorProvider.addNavigator(ComposeNavigator())
                nav.navigatorProvider.addNavigator(DialogNavigator())
                val state = CoachUiState(loading = false)
                val graph =
                    nav.createGraph(startDestination = "home") {
                        homeGraph({ state }, model, nav)
                        runningGraph({ state }, model, nav, { null }, {}, { _, _ -> }, {})
                        strengthGraph({ state }, model, nav)
                        progressGraph({ state }, model, nav)
                        settingsGraph({ state }, model, nav, { _, _ -> }, {}, {}, {}, {}, {}, {})
                    }
                assertEquals(expected, graph.map { it.route }.toSet())
                assertEquals("home", graph.startDestinationRoute)
                assertFalse(
                    "Feature ownership must not change Up/pop semantics",
                    graph.any { it is NavGraph },
                )
            } finally {
                viewModels.clear()
            }
        }
    }
}
