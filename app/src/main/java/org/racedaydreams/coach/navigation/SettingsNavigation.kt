package org.racedaydreams.coach.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.settingsGraph(
    readState: () -> CoachUiState,
    model: CoachViewModel,
    nav: NavHostController,
    connect: (Boolean, Boolean) -> Unit,
    providerUpdate: () -> Unit,
    providerSettings: () -> Unit,
    exportBackup: () -> Unit,
    restoreBackup: () -> Unit,
    requestReset: () -> Unit,
    requestWeightPermission: () -> Unit,
) {
    composable("health-intelligence") { HealthIntelligenceScreen(model) }

    composable("settings") {
        SettingsScreen(
            readState(),
            model,
            model.health.historyPermissionAvailable(),
            connect,
            providerUpdate,
            providerSettings,
            { nav.navigate("goals") },
            { nav.navigate("baseline") },
            { nav.navigate("shoes") },
            exportBackup,
            restoreBackup,
            requestReset,
            requestWeightPermission,
            { nav.navigate("health-intelligence") },
        )
    }
}
