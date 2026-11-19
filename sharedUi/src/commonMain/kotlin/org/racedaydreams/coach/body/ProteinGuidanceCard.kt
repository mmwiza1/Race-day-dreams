package org.racedaydreams.coach.body

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.body.*

/** Existing production card. Formatting, permission facts and side effects stay platform-owned. */
@Composable
fun ProteinGuidanceCard(
    state: BodyMeasurementsState,
    onProtein: (Boolean) -> Unit,
    onChooseWeight: () -> Unit,
    formatNumber: (Double) -> String,
    formatBasisDate: (BodyWeight) -> String,
    basisAccessNote: String?,
) {
    var details by rememberSaveable { mutableStateOf(false) }
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
                    "Chosen basis: ${formatNumber(basis.kilograms())} kg · ${formatBasisDate(basis)}"
                )
                Text(bodySource(basis.provider(), basis.origin()))
                basisAccessNote?.let { Text(it) }
            }
            if (state.preferences.proteinEnabled()) {
                TextButton(onClick = onChooseWeight, enabled = !state.busy) {
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
                            "${formatNumber(advice.minimumPerKg()!!)}–${formatNumber(advice.maximumPerKg()!! )} grams per kilogram of the explicitly selected weight. Rounded to the nearest 5 grams; height is not used."
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
}

fun bodySource(provider: String, origin: String?): String =
    if (provider == "manual") "Source: entered by you"
    else "Source: $provider" + (origin?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
