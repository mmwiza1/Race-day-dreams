package org.racedaydreams.coach.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.CoachTextInput
import org.racedaydreams.domain.activity.ActivityRecord

/** Edits athlete observations independently of recorded GPS and timing evidence. */
@Composable
fun SharedRunNotes(activity: ActivityRecord, busy: Boolean, onSave: (String, Double?) -> Unit) {
    var notes by
        rememberSaveable(activity.id(), activity.notes()) {
            mutableStateOf(activity.notes().orEmpty())
        }
    var effort by
        rememberSaveable(activity.id(), activity.rpe()) {
            mutableStateOf(activity.rpe()?.toString()?.removeSuffix(".0").orEmpty())
        }
    var error by rememberSaveable(activity.id()) { mutableStateOf<String?>(null) }

    CoachTextInput("Your effort / RPE, 0–10 (optional)", effort, { effort = it }, decimal = true)
    OutlinedTextField(
        notes,
        { notes = it },
        label = { Text("Your notes") },
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = {
            val input = effort.trim()
            val parsed = input.toDoubleOrNull()
            if (input.isNotEmpty() && (parsed == null || parsed !in 0.0..10.0)) {
                error = "Enter an effort from 0 to 10, or leave it blank."
            } else {
                error = null
                onSave(notes, parsed)
            }
        },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text("Save notes and effort")
    }
}
