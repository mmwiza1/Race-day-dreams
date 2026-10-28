package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason

/** Explicit programming preference, independent of visibility, logging and medical advice. */
@Composable
fun ExerciseExclusionControl(
    name: String,
    excluded: Boolean,
    busy: Boolean,
    change: (Reason?) -> Unit,
) {
    var confirm by rememberSaveable(name) { mutableStateOf(false) }
    var reason by rememberSaveable(name) { mutableStateOf(Reason.PREFERENCE) }
    if (excluded) {
        Text(
            "Excluded from new programme and alternative recommendations. Your history, PBs, favourites and existing targets remain."
        )
        OutlinedButton(
            onClick = { change(null) },
            enabled = !busy,
            modifier = Modifier.heightIn(min = 48.dp).testTag("exercise-restore"),
        ) {
            Text("Restore exercise")
        }
    } else {
        TextButton(
            onClick = { confirm = true },
            enabled = !busy,
            modifier = Modifier.heightIn(min = 48.dp).testTag("exercise-exclude"),
        ) {
            Text("Exclude from recommendations")
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Exclude $name?") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "This changes future selection only. Existing prescriptions and recorded evidence stay intact. You can restore the exercise at any time."
                    )
                    Reason.values().forEach { value ->
                        FilterChip(
                            selected = reason == value,
                            onClick = { reason = value },
                            label = {
                                Text(
                                    when (value) {
                                        Reason.PREFERENCE -> "Preference"
                                        Reason.EQUIPMENT_UNAVAILABLE -> "Equipment unavailable"
                                        Reason.UNCOMFORTABLE -> "Movement uncomfortable"
                                        Reason.OTHER -> "Other"
                                    }
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    if (reason == Reason.UNCOMFORTABLE)
                        Text(
                            "Avoid movements that reproduce pain. This preference is not an assessment or medical advice."
                        )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        change(reason)
                    },
                    enabled = !busy,
                ) {
                    Text("Exclude exercise")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
}
