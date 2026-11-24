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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
    // The first-run choice has nothing to go back to, and Settings is not useful before a focus
    // exists. Later setup steps (baseline, goals) keep their own Back and Settings as before.
    val firstRun = route == "welcome"
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
                    // The full name at a capped size: large text no longer turns it into "RDD"
                    // (C-06).
                    title = {
                        Text(
                            "Race Day Dreams",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = cappedTextStyle(MaterialTheme.typography.titleLarge),
                            modifier = Modifier.semantics { contentDescription = "Race Day Dreams" },
                        )
                    },
                    navigationIcon = {
                        if (
                            route != null &&
                                !firstRun &&
                                route !in coachTabs.map { it.first } &&
                                route != "strength?date={date}" &&
                                route != "goals"
                        ) {
                            TextButton(onClick = onBack) { Text("Back") }
                        }
                    },
                    // Already in Settings: Back is the way out, so a second Settings action would
                    // only reload the screen people are on.
                    actions = {
                        if (route != "settings" && !firstRun)
                            TextButton(onClick = onSettings) { Text("Settings") }
                    },
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
                // Always labelled (C-06): one line at a capped size, so the names stay visible at
                // 200% text instead of leaving ambiguous icons. The full name stays in semantics.
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = cappedTextStyle(MaterialTheme.typography.labelMedium),
                    )
                },
                onClick = { selectTab(destination) },
            )
        }
    }
}

/** Largest text scale applied to the shell's one-line title and navigation labels (C-06). */
internal const val SHELL_MAX_FONT_SCALE = 1.2f

/**
 * Scales [style] so that it grows with the system text size only up to [SHELL_MAX_FONT_SCALE].
 * Single-line chrome (the app bar title and navigation labels) stays legible and in place at 200%
 * text, while page content keeps the full scale.
 */
@Composable
internal fun cappedTextStyle(style: TextStyle): TextStyle {
    val scale = LocalDensity.current.fontScale
    if (scale <= SHELL_MAX_FONT_SCALE) return style
    val factor = SHELL_MAX_FONT_SCALE / scale
    return style.copy(
        fontSize = if (style.fontSize.isSp) style.fontSize * factor else style.fontSize,
        lineHeight = if (style.lineHeight.isSp) style.lineHeight * factor else style.lineHeight,
    )
}
