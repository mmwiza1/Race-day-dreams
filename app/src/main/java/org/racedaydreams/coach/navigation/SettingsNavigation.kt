package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*
import org.racedaydreams.health.HealthConnectGateway

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.settingsGraph(
    state: CoachUiState,
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

    composable("settings") { SettingsScreen(state, model, model.health.historyPermissionAvailable(), connect,
        providerUpdate, providerSettings,
        { nav.navigate("goals") }, { nav.navigate("baseline") }, { nav.navigate("shoes") },
        exportBackup, restoreBackup,
        requestReset, requestWeightPermission, {nav.navigate("health-intelligence")}) }
}
