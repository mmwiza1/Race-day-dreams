package org.racedaydreams.coach

import android.app.Application
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.navigation.appBarBack
import org.racedaydreams.coach.navigation.openSavedActivity
import org.racedaydreams.coach.navigation.selectTab

/** Back-stack contracts for audit findings C-03, C-05 and C-08, on the real route patterns. */
@RunWith(AndroidJUnit4::class)
class NavigationBackStackTest {
    private val routes =
        listOf(
            "home",
            "plan",
            "calendar",
            "progress",
            "strength?date={date}",
            "gym",
            "gym-now",
            "run-now",
            "spontaneous-run",
            "record-run",
            "execute/{version}/{workout}",
            "race-run/{version}/{workout}/{hilly}",
            "activity/{id}",
        )

    private fun controller(start: String): NavHostController {
        val nav = NavHostController(ApplicationProvider.getApplicationContext<Application>())
        nav.navigatorProvider.addNavigator(ComposeNavigator())
        nav.navigatorProvider.addNavigator(DialogNavigator())
        nav.setViewModelStore(ViewModelStore())
        nav.graph =
            nav.createGraph(startDestination = start) {
                routes.forEach { route -> composable(route) { Text(route) } }
            }
        return nav
    }

    /** Routes from the top of the back stack down, popping as it goes. */
    private fun drain(nav: NavHostController): List<String?> {
        val stack = mutableListOf(nav.currentDestination?.route)
        while (nav.previousBackStackEntry != null) {
            nav.popBackStack()
            stack += nav.currentDestination?.route
        }
        return stack
    }

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Test
    fun aSavedRunReplacesItsRecorderSoBackReturnsToWhereTheRunStarted() = onMain {
        val spontaneous = controller("home")
        spontaneous.navigate("run-now")
        spontaneous.navigate("spontaneous-run")
        spontaneous.openSavedActivity("spontaneous-run", "rdd:run/1")
        assertEquals("activity/{id}", spontaneous.currentDestination?.route)
        assertEquals("rdd:run/1", spontaneous.currentBackStackEntry?.arguments?.getString("id"))
        assertEquals(listOf("activity/{id}", "run-now", "home"), drain(spontaneous))

        val planned = controller("home")
        planned.navigate("plan")
        planned.navigate("execute/v1/w1")
        planned.openSavedActivity("execute/{version}/{workout}", "a")
        assertEquals(listOf("activity/{id}", "plan", "home"), drain(planned))

        val race = controller("home")
        race.navigate("race-run/v/w/true")
        race.openSavedActivity("race-run/{version}/{workout}/{hilly}", "a")
        assertEquals(listOf("activity/{id}", "home"), drain(race))

        // A recorder opened as the only screen (from the notification) leaves Home beneath.
        val notification = controller("record-run")
        notification.openSavedActivity("record-run", "a")
        assertEquals(listOf("activity/{id}", "home"), drain(notification))
    }

    @Test
    fun tabsReplaceEachOtherAboveHomeOrAboveTheStartDestination() = onMain {
        val nav = controller("home")
        nav.navigate("gym")
        nav.selectTab("plan")
        nav.selectTab("progress")
        assertEquals(listOf("progress", "home"), drain(nav))

        // Started on the recorder: tabs no longer pile up because Home is not on the stack.
        val recording = controller("record-run")
        recording.selectTab("calendar")
        recording.selectTab("progress")
        recording.selectTab("strength")
        assertEquals(listOf("strength?date={date}", "record-run"), drain(recording))
    }

    @Test
    fun appBarBackUsesTheSameHandlersAsSystemBackAndFallsBackToHomeOnlyAtARoot() = onMain {
        val dispatcher = OnBackPressedDispatcher()
        var screenBack = 0
        val screen =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    screenBack++
                }
            }
        dispatcher.addCallback(screen)
        val nav = controller("home")
        nav.navigate("gym-now")
        // A screen's own handler (for example a Gym Now sub-section) is used first.
        appBarBack(dispatcher, nav)
        assertEquals(1, screenBack)
        assertEquals("gym-now", nav.currentDestination?.route)

        screen.isEnabled = false
        appBarBack(dispatcher, nav)
        assertEquals("home", nav.currentDestination?.route)
        assertNull(nav.previousBackStackEntry)

        // A non-tab root with nothing to pop still reaches Home, as before.
        val root = controller("record-run")
        appBarBack(OnBackPressedDispatcher(), root)
        assertEquals("home", root.currentDestination?.route)
        appBarBack(null, root)
        assertEquals("record-run", root.currentDestination?.route)
    }
}
