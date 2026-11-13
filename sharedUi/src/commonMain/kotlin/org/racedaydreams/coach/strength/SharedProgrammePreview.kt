package org.racedaydreams.coach.strength

import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.LocalDate

/** Original production preview. Platform supplies its retained date/target formatting. */
@Composable
fun SharedProgrammePreviewContent(
    programme: StrengthProgramme,
    formatDate: (LocalDate) -> String,
    goalLabel: (String) -> String,
    describeTarget: (GymWorkoutDefinition.Slot) -> String,
) {
    Text(programme.name(), style = MaterialTheme.typography.headlineSmall)
    Text(
        "${programme.settings().frequency()} sessions/week · ${programme.blocks().sumOf { it.weeks() }} weeks · ${goalLabel(programme.settings().goal().name)}"
    )
    Text("Not activated. Review targets and trade-offs before choosing to use this programme.")
    if (programme.tradeoffs().isNotEmpty())
        WhyDetails(programme.tradeoffs().joinToString("\n"), "Programme trade-offs")
    programme.blocks().forEach { block ->
        SemanticCard(Tile.Strength) {
            Text(block.name(), style = MaterialTheme.typography.titleLarge)
            Text("${formatDate(block.start())} – ${formatDate(block.end())}")
            WhyDetails(block.explanation(), "Block emphasis")
            // Concrete dated prescriptions are shown as retained, never reconstructed from the
            // first week.
            var limit by remember(block.id()) { mutableIntStateOf(7) }
            val chronological =
                remember(block) { block.workouts().sortedBy { it.date().toEpochDay() } }
            chronological.take(limit).forEach { workout ->
                Text(
                    "${formatDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min",
                    style = MaterialTheme.typography.titleMedium,
                )
                workout.definition().slots().forEach { slot ->
                    Text("${slot.exercise().name()} · ${describeTarget(slot)}")
                }
                workout.notes()?.let { Text(it) }
            }
            if (limit < block.workouts().size)
                TextButton(onClick = { limit += 7 }) { Text("Show more dated workouts") }
        }
    }
}
