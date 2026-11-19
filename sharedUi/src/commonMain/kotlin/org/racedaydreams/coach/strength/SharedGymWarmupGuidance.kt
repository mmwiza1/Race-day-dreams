package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.GymWarmup

/** Existing production preparation UI; callbacks only stage the editable warm-up form. */
@Composable
fun GymWarmupGuidanceCard(
    slotId: String,
    exerciseId: String,
    suggestion: GymWarmup.Suggestion?,
    enabled: Boolean,
    formatKilograms: (Double) -> String,
    onPrepare: (GymWarmup.Ramp?) -> Unit,
) {
    var open by rememberSaveable(slotId, exerciseId) { mutableStateOf(false) }
    var evidence by rememberSaveable(slotId, exerciseId) { mutableStateOf(false) }
    SemanticCard(Tile.Insight) {
        TextButton(
            onClick = { open = !open },
            modifier =
                Modifier.heightIn(min = 48.dp).semantics {
                    stateDescription = if (open) "Expanded" else "Collapsed"
                },
        ) {
            Text(if (open) "Optional warm-up ▴" else "Optional warm-up ▾")
        }
        if (open && suggestion != null) {
            Text(
                "Warm-up sets are optional practice before your main sets. Use a comfortable movement and record only what you actually do."
            )
            Text(requireNotNull(suggestion.instruction()))
            Text("These are practice ideas, not recorded sets. Review and edit them before saving.")
            suggestion.ramps().forEach { ramp ->
                OutlinedButton(onClick = { onPrepare(ramp) }, enabled = enabled) {
                    Text(
                        "Review ${formatKilograms(ramp.kilograms())} kg × ${ramp.repetitions()} practice repetitions"
                    )
                }
            }
            TextButton(onClick = { onPrepare(null) }, enabled = enabled) {
                Text("Enter my own warm-up")
            }
            TextButton(
                onClick = { evidence = !evidence },
                modifier =
                    Modifier.heightIn(min = 48.dp).semantics {
                        stateDescription = if (evidence) "Expanded" else "Collapsed"
                    },
            ) {
                Text(if (evidence) "Hide preparation evidence" else "Why this preparation?")
            }
            if (evidence) {
                Text(requireNotNull(suggestion.why()), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Warm-ups do not count as working sets, PBs or progression evidence. Suggested loads are not completed lifts or working-weight prescriptions."
                )
            }
            TextButton(onClick = { open = false }) { Text("Skip preparation guidance") }
        }
    }
}
