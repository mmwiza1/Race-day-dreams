package org.racedaydreams.coach.body

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var details by rememberSaveable { mutableStateOf(false) }
    var measurements by rememberSaveable { mutableStateOf(false) }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(MassUnit.KG) }
    var weightCount by rememberSaveable { mutableIntStateOf(10) }
    val heightUnit = state.preferences.heightUnit()
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        CoachHeading("Body & protein guidance")
        Text(
            "Optional measurements and general adult nutrition guidance. Your programme works without them."
        )
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        state.notice?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        SemanticCard(Tile.Insight) {
            Text("General daily protein", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Checkbox(
                    state.preferences.proteinEnabled(),
                    onCheckedChange = onProtein,
                    enabled = !state.busy,
                    modifier =
                        Modifier.semantics { contentDescription = "Show optional protein guidance" },
                )
                Text(
                    "Show optional protein guidance",
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                )
            }
            state.advice?.let { advice ->
                Text(advice.instruction(), style = MaterialTheme.typography.titleMedium)
                Text(advice.explanation())
                advice.basis()?.let { basis ->
                    Text(
                        "Chosen basis: ${decimal(basis.kilograms())} kg · ${friendlyDate(basis.at().atOffset(basis.offset()).toLocalDate())}"
                    )
                    Text(bodySource(basis.provider(), basis.origin()))
                    if (
                        basis.provider() == "health-connect" &&
                            HealthConnectGateway.WEIGHT_PERMISSION !in state.granted
                    )
                        Text(
                            "Previously imported measurement; weight access is not currently granted."
                        )
                }
                if (state.preferences.proteinEnabled()) {
                    TextButton(onClick = { measurements = true }, enabled = !state.busy) {
                        Text("Choose or record a weight")
                    }
                    TextButton(
                        onClick = { details = !details },
                        modifier =
                            Modifier.heightIn(min = 48.dp).semantics {
                                stateDescription = if (details) "Expanded" else "Collapsed"
                            },
                    ) {
                        Text(if (details) "Calculation & sources ▴" else "Calculation & sources ▾")
                    }
                    if (details) {
                        if (advice.available())
                            Text(
                                "${decimal(advice.minimumPerKg()!!)}–${decimal(advice.maximumPerKg()!!)} grams per kilogram of the explicitly selected weight. Rounded to the nearest 5 grams; height is not used."
                            )
                        Text(ProteinGuidance.sources())
                        Text(advice.limitation())
                    }
                }
            }
            Text(
                "General guidance, not a medical prescription. Individual needs can differ.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SemanticCard(Tile.Neutral) {
            Text("Your measurements", style = MaterialTheme.typography.titleLarge)
            val latestHeight = state.heights.firstOrNull { !it.at().isAfter(Instant.now()) }
            if (latestHeight == null) Text("No height measurement recorded.")
            else {
                Text(
                    "Latest height: ${decimal(heightUnit.fromMetres(latestHeight.metres()))} ${heightUnit.label()}"
                )
                Text(bodySource(latestHeight.provider(), latestHeight.origin()))
                Text(bodyHeightDate(latestHeight))
                if (latestHeight.at().isBefore(Instant.now().minus(Duration.ofDays(365))))
                    Text(
                        "This is an older measurement; its source date is shown. You can record another if it has changed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                if (
                    latestHeight.provider() == "health-connect" &&
                        HealthConnectGateway.HEIGHT_PERMISSION !in state.granted
                )
                    Text(
                        "Retained height import; access is not currently granted.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
            TextButton(
                onClick = { measurements = !measurements },
                modifier =
                    Modifier.heightIn(min = 48.dp).semantics {
                        stateDescription = if (measurements) "Expanded" else "Collapsed"
                    },
            ) {
                Text(
                    if (measurements) "Hide measurement history & entry"
                    else "Measurement history & manual entry"
                )
            }
            if (measurements) {
                Text("Record a measurement", style = MaterialTheme.typography.titleMedium)
                Text("Manual entry is recorded now · ${friendlyDate(LocalDate.now())}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeightUnit.values().forEach { value ->
                        FilterChip(
                            heightUnit == value,
                            {
                                height = ""
                                onHeightUnit(value)
                            },
                            enabled = !state.busy,
                            label = { Text(value.label()) },
                        )
                    }
                }
                OutlinedTextField(
                    height,
                    { height = it },
                    label = { Text("Height (${heightUnit.label()})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Button(
                    onClick = { onHeightSave(height, heightUnit) },
                    enabled = !state.busy && height.isNotBlank(),
                ) {
                    Text("Save height measurement")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MassUnit.values().forEach { value ->
                        FilterChip(
                            unit == value,
                            {
                                weight = ""
                                unit = value
                            },
                            enabled = !state.busy,
                            label = { Text(value.name.lowercase()) },
                        )
                    }
                }
                OutlinedTextField(
                    weight,
                    { weight = it },
                    label = { Text("Body weight (${unit.name.lowercase()})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Button(
                    onClick = { onWeightSave(weight, unit) },
                    enabled = !state.busy && weight.isNotBlank(),
                ) {
                    Text("Save body-weight measurement")
                }
                Text("Weight history", style = MaterialTheme.typography.titleMedium)
                if (state.weights.isEmpty())
                    Text("No weight measurements recorded. No daily protein amount is inferred.")
                state.weights.take(weightCount).forEach { row ->
                    HorizontalDivider()
                    Text(
                        "${decimal(row.kilograms())} kg · ${friendlyDate(row.at().atOffset(row.offset()).toLocalDate())}"
                    )
                    Text(bodySource(row.provider(), row.origin()))
                    Text(
                        "Recorded ${bodyEvidenceTime.format(row.at())} · stored offset ${row.offset()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (
                        row.provider() == "health-connect" &&
                            HealthConnectGateway.WEIGHT_PERMISSION !in state.granted
                    )
                        Text(
                            "Retained import; permission is not currently granted.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    if (state.preferences.basis()?.matches(row) == true)
                        Text("Chosen for general guidance")
                    else
                        OutlinedButton(onClick = { onWeight(row.id()) }, enabled = !state.busy) {
                            Text("Use this weight for guidance")
                        }
                }
                if (state.weights.size > weightCount)
                    TextButton(onClick = { weightCount += 10 }) {
                        Text("Show more weight measurements")
                    }
                Text("Height history", style = MaterialTheme.typography.titleMedium)
                state.heights.forEach { row ->
                    HorizontalDivider()
                    Text(
                        "${decimal(heightUnit.fromMetres(row.metres()))} ${heightUnit.label()} · ${bodyHeightDate(row)}"
                    )
                    Text(bodySource(row.provider(), row.origin()))
                    Text(
                        "Recorded ${bodyEvidenceTime.format(row.at())}" +
                            (row.importedAt()?.let { " · imported ${bodyEvidenceTime.format(it)}" }
                                ?: if (row.provider() == "manual") " · manually entered"
                                else " · import time unavailable"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (
                        row.provider() == "health-connect" &&
                            HealthConnectGateway.HEIGHT_PERMISSION !in state.granted
                    )
                        Text(
                            "Retained import; permission is not currently granted.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
                if (state.moreHeights)
                    TextButton(onClick = onMoreHeights, enabled = !state.busy) {
                        Text("Show more height measurements")
                    }
            }
        }
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
    }
}

private fun bodySource(provider: String, origin: String?): String =
    if (provider == "manual") "Source: entered by you"
    else "Source: $provider" + (origin?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")

private fun bodyHeightDate(row: BodyHeight): String =
    if (row.offset() == null)
        "${bodyEvidenceTime.format(row.at())} · original time zone unavailable"
    else friendlyDate(row.at().atOffset(row.offset()).toLocalDate())

private val bodyEvidenceTime =
    DateTimeFormatter.ofPattern("d MMMM uuuu 'at' HH:mm:ss 'UTC'", Locale.UK)
        .withZone(ZoneOffset.UTC)
