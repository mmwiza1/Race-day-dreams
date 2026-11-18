package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Existing application chrome; routing, lifecycle and recorder ownership stay with the host. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachShell(
    route: String?,
    useRail: Boolean,
    snackbars: SnackbarHostState,
    selectTab: (String) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val showNavigation = route in (coachTabs.map { it.first } + "strength?date={date}")
    Row(Modifier.fillMaxSize()) {
        if (useRail && showNavigation) {
            NavigationRail(
                Modifier.fillMaxHeight()
                    .width(if (LocalDensity.current.fontScale >= 1.5f) 144.dp else 80.dp)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState()),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                coachTabs.forEach { (destination, label) ->
                    NavigationRailItem(
                        modifier = Modifier.fillMaxWidth(),
                        selected =
                            route == destination ||
                                destination == "strength" && route?.startsWith("strength") == true,
                        onClick = { selectTab(destination) },
                        icon = { CoachDestinationIcon(destination) },
                        label = { Text(label) },
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (LocalDensity.current.fontScale >= 1.5f) "RDD"
                            else "Race Day Dreams",
                            maxLines = 1,
                            modifier = Modifier.semantics { contentDescription = "Race Day Dreams" },
                        )
                    },
                    navigationIcon = {
                        if (
                            route != null &&
                                route !in coachTabs.map { it.first } &&
                                route != "strength?date={date}" &&
                                route != "goals"
                        ) {
                            TextButton(onClick = onBack) { Text("Back") }
                        }
                    },
                    actions = { TextButton(onClick = onSettings) { Text("Settings") } },
                )
            },
            bottomBar = {
                if (!useRail && showNavigation) {
                    CoachNavigationBar(route, selectTab)
                }
            },
            snackbarHost = { SnackbarHost(snackbars) },
            content = content,
        )
    }
}

private val coachTabs =
    listOf(
        "home" to "Home",
        "plan" to "Plan",
        "calendar" to "Calendar",
        "progress" to "Progress",
        "strength" to "Strength",
    )

@Composable
fun CoachNavigationBar(route: String?, selectTab: (String) -> Unit) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        coachTabs.forEach { (destination, label) ->
            NavigationBarItem(
                modifier =
                    if (largeFont) Modifier.semantics { contentDescription = label } else Modifier,
                selected =
                    route == destination ||
                        destination == "strength" && route?.startsWith("strength") == true,
                icon = { CoachDestinationIcon(destination) },
                label = if (largeFont) null else ({ Text(label, maxLines = 1, softWrap = false) }),
                onClick = { selectTab(destination) },
            )
        }
    }
}
