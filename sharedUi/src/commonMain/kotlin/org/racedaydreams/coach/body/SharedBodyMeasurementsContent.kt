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
import org.racedaydreams.coach.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.time.Instant

/** Real date/number/access presentation. Unknown Apple read access must never be called denied. */
class BodyMeasurementsPresentation(
    val now: () -> Instant,
    val manualDate: () -> String,
    val formatNumber: (Double) -> String,
    val formatWeightDate: (BodyWeight) -> String,
    val formatHeightDate: (BodyHeight) -> String,
    val formatEvidenceTime: (Instant) -> String,
    val heightIsOld: (BodyHeight) -> Boolean,
    val basisAccessNote: (BodyWeight) -> String?,
    val weightAccessNote: (BodyWeight) -> String?,
    val heightAccessNote: (BodyHeight, Boolean) -> String?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedBodyMeasurementsContent(
    state: BodyMeasurementsState,
    onBack: () -> Unit,
    onProtein: (Boolean) -> Unit,
    onWeight: (String) -> Unit,
    onHeightSave: (String, HeightUnit) -> Unit,
    onWeightSave: (String, MassUnit) -> Unit,
    onHeightUnit: (HeightUnit) -> Unit,
    onMoreHeights: () -> Unit,
    presentation: BodyMeasurementsPresentation,
    providerControls: @Composable () -> Unit,
) {
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
        ProteinGuidanceCard(
            state = state,
            onProtein = onProtein,
            onChooseWeight = { measurements = true },
            formatNumber = presentation.formatNumber,
            formatBasisDate = presentation.formatWeightDate,
            basisAccessNote = state.advice?.basis()?.let(presentation.basisAccessNote),
        )
        SemanticCard(Tile.Neutral) {
            Text("Your measurements", style = MaterialTheme.typography.titleLarge)
            val latestHeight = state.heights.firstOrNull { !it.at().isAfter(presentation.now()) }
            if (latestHeight == null) Text("No height measurement recorded.")
            else {
                Text(
                    "Latest height: ${presentation.formatNumber(heightUnit.fromMetres(latestHeight.metres()))} ${heightUnit.label()}"
                )
                Text(bodySource(latestHeight.provider(), latestHeight.origin()))
                Text(presentation.formatHeightDate(latestHeight))
                if (presentation.heightIsOld(latestHeight))
                    Text(
                        "This is an older measurement; its source date is shown. You can record another if it has changed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                presentation.heightAccessNote(latestHeight, true)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
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
                Text("Manual entry is recorded now · ${presentation.manualDate()}")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    HeightUnit.values().forEach { value ->
                        CoachFilterChip(
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    MassUnit.values().forEach { value ->
                        CoachFilterChip(
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
                        "${presentation.formatNumber(row.kilograms())} kg · ${presentation.formatWeightDate(row)}"
                    )
                    Text(bodySource(row.provider(), row.origin()))
                    Text(
                        "Recorded ${presentation.formatEvidenceTime(row.at())} · stored offset ${row.offset()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    presentation.weightAccessNote(row)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
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
                        "${presentation.formatNumber(heightUnit.fromMetres(row.metres()))} ${heightUnit.label()} · ${presentation.formatHeightDate(row)}"
                    )
                    Text(bodySource(row.provider(), row.origin()))
                    Text(
                        "Recorded ${presentation.formatEvidenceTime(row.at())}" +
                            (row.importedAt()?.let {
                                " · imported ${presentation.formatEvidenceTime(it)}"
                            }
                                ?: if (row.provider() == "manual") " · manually entered"
                                else " · import time unavailable"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    presentation.heightAccessNote(row, false)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (state.moreHeights)
                    TextButton(onClick = onMoreHeights, enabled = !state.busy) {
                        Text("Show more height measurements")
                    }
            }
        }
        providerControls()
    }
}
