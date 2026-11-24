package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.activity.OnBackPressedDispatcher
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * The shell's app-bar Back behaves exactly like the system Back (audit C-03): it goes through the
 * activity's back dispatcher, so a screen's own handler steps out of its sub-section first and the
 * NavHost pops otherwise. Only when nothing would handle it (a non-tab root such as a recording
 * opened from the notification) does it fall back to Home, as before.
 */
fun appBarBack(dispatcher: OnBackPressedDispatcher?, nav: NavHostController) {
    when {
        dispatcher != null && dispatcher.hasEnabledCallbacks() -> dispatcher.onBackPressed()
        nav.previousBackStackEntry != null -> nav.popBackStack()
        // Never pops the last entry, which would leave an empty screen.
        else -> nav.navigate("home")
    }
}

/**
 * Bottom-navigation selection (audit C-05): tabs replace each other above Home. When Home is not on
 * the back stack (the app started on another destination), they replace each other above that start
 * destination instead of piling up.
 */
fun NavHostController.selectTab(destination: String) {
    val homeOnStack = runCatching { getBackStackEntry("home") }.isSuccess
    navigate(destination) {
        if (homeOnStack) popUpTo("home") else popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

/**
 * Opens a just-saved run in place of its recorder (audit C-08), so Back from the saved activity
 * returns to where the recording was started rather than to a stale "Record a run" screen. When the
 * recorder was the only screen (opened from the notification), Home is placed beneath it.
 */
fun NavHostController.openSavedActivity(recordingRoute: String, activityId: String) {
    if (previousBackStackEntry == null)
        navigate("home") { popUpTo(recordingRoute) { inclusive = true } }
    navigate("activity/${Uri.encode(activityId)}") { popUpTo(recordingRoute) { inclusive = true } }
}
