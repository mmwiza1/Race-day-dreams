package org.racedaydreams.coach.strength

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.racedaydreams.domain.strength.CustomStrengthExercise

@Composable
fun CustomExerciseMetadataSummary(metadata: CustomStrengthExercise) {
    Text("${metadata.bodyArea().label()} · ${metadata.role().label()}")
    metadata.notes()?.let { Text(it) }
    Text(
        "User-defined movement. No app-authored technique, estimated 1RM or automatic progression is inferred."
    )
}
