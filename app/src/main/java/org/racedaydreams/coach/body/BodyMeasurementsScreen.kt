package org.racedaydreams.coach.body

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.coach.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.health.HealthConnectGateway

@Composable
fun BodyMeasurementsScreen(
    model: BodyMeasurementsViewModel,
    onBack: () -> Unit,
    onPermissions: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }
    BodyMeasurementsContent(
        state,
        onBack,
        onPermissions,
        model::refresh,
        model::sync,
        model::protein,
        model::chooseWeight,
        model::saveHeight,
        model::saveWeight,
        model::heightUnit,
        model::moreHeights,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodyMeasurementsContent(
    state: BodyMeasurementsState,
    onBack: () -> Unit,
    onPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onProtein: (Boolean) -> Unit,
    onWeight: (String) -> Unit,
    onHeightSave: (String, HeightUnit) -> Unit,
    onWeightSave: (String, MassUnit) -> Unit,
    onHeightUnit: (HeightUnit) -> Unit,
    onMoreHeights: () -> Unit,
) {
    SharedBodyMeasurementsContent(
        state,
        onBack,
        onProtein,
        onWeight,
        onHeightSave,
        onWeightSave,
        onHeightUnit,
        onMoreHeights,
        BodyMeasurementsPresentation(
            Instant::now,
            { friendlyDate(LocalDate.now()) },
            ::decimal,
            { friendlyDate(it.at().atOffset(it.offset()).toLocalDate()) },
            ::bodyHeightDate,
            bodyEvidenceTime::format,
            { it.at().isBefore(Instant.now().minus(Duration.ofDays(365))) },
            { row ->
                if (
                    row.provider() == "health-connect" &&
                        HealthConnectGateway.WEIGHT_PERMISSION !in state.granted
                )
                    "Previously imported measurement; weight access is not currently granted."
                else null
            },
            { row ->
                if (
                    row.provider() == "health-connect" &&
                        HealthConnectGateway.WEIGHT_PERMISSION !in state.granted
                )
                    "Retained import; permission is not currently granted."
                else null
            },
            { row, summary ->
                if (
                    row.provider() == "health-connect" &&
                        HealthConnectGateway.HEIGHT_PERMISSION !in state.granted
                )
                    if (summary) "Retained height import; access is not currently granted."
                    else "Retained import; permission is not currently granted."
                else null
            },
        ),
        providerControls = {
            SemanticCard(Tile.Progress) {
                Text("Optional Health Connect", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Height and weight access are independent. Imported observations keep their source and date; they are not automatically chosen for nutrition guidance."
                )
                if (state.available) {
                    OutlinedButton(onClick = onPermissions, enabled = !state.busy) {
                        Text("Review height & weight access")
                    }
                    OutlinedButton(onClick = onSync, enabled = !state.busy) {
                        Text("Refresh authorized body measurements")
                    }
                } else
                    Text(
                        "Health Connect is unavailable here. Manual measurements and training remain available."
                    )
                TextButton(onClick = onRefresh, enabled = !state.busy) {
                    Text("Refresh local measurements & access")
                }
            }
        },
    )
}

private fun bodyHeightDate(row: BodyHeight): String =
    if (row.offset() == null)
        "${bodyEvidenceTime.format(row.at())} · original time zone unavailable"
    else friendlyDate(row.at().atOffset(row.offset()).toLocalDate())

private val bodyEvidenceTime =
    DateTimeFormatter.ofPattern("d MMMM uuuu 'at' HH:mm:ss 'UTC'", Locale.UK)
        .withZone(ZoneOffset.UTC)
