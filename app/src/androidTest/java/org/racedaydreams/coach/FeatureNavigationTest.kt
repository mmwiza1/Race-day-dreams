package org.racedaydreams.coach

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.navigation.*

/** Existing flat route contracts must survive feature ownership changes. */
@RunWith(AndroidJUnit4::class)
class FeatureNavigationTest {
    @Test
    fun featureGraphsRetainAllExistingRoutesWithoutIntroducingNestedBackStacks() {
        val expected = setOf(
            "welcome", "home", "weekly", "routines", "routine/{id}?related={related}",
            "run-now", "spontaneous-run", "record-run", "execute/{version}/{workout}",
            "race", "race-run/{version}/{workout}/{hilly}", "baseline", "manual", "assessment",
            "goals", "plan", "workout/{id}", "history?kind={kind}", "calendar", "activity/{id}",
            "strength?date={date}", "strength/log", "strength/pbs", "strength/source/{id}",
            "strength/history", "gym", "gym/session/{id}", "exercises", "exercise/{id}",
            "progress", "progress/pbs", "progress/trends", "progress/weight", "shoes",
            "settings", "health-intelligence",
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
                val graph = nav.createGraph(startDestination = "home") {
                    homeGraph(state, model, nav)
                    runningGraph(state, model, nav, null, {}, { _, _ -> }, {})
                    strengthGraph(state, model, nav)
                    progressGraph(state, model, nav)
                    settingsGraph(state, model, nav, { _, _ -> }, {}, {}, {}, {}, {}, {})
                }
                assertEquals(expected, graph.map { it.route }.toSet())
                assertEquals("home", graph.startDestinationRoute)
                assertFalse("Feature ownership must not change Up/pop semantics", graph.any { it is NavGraph })
            } finally {
                viewModels.clear()
            }
        }
    }
}
