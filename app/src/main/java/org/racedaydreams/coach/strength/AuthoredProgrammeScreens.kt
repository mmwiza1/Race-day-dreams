package org.racedaydreams.coach.strength

import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.StrengthProgramme

/** Frozen authored prescriptions are never converted to the older integer-RPE model. */
@Composable
internal fun AuthoredProgrammeOverview(
    programme: StrengthProgramme,
    open: (String) -> Unit,
    author: (() -> Unit)? = null,
) {
    CoachPage {
        CoachHeading(programme.name())
        if (author != null) CoachButton("Build or review programmes", author)
        Text("Programme revision ${programme.revision()}")
        WhyDetails(programme.reason(), "Why this revision?")
        if (programme.tradeoffs().isNotEmpty())
            WhyDetails(programme.tradeoffs().joinToString("\n"), "Programme trade-offs")
        programme.blocks().forEach { block ->
            SemanticCard(Tile.Strength) {
                Text(block.name(), style = MaterialTheme.typography.titleLarge)
                Text(
                    "${friendlyDate(block.start())} – ${friendlyDate(block.end())} · ${block.weeks()} weeks"
                )
                WhyDetails(block.explanation(), "Block emphasis")
                var limit by
                    remember(programme.id(), programme.revision(), block.id()) {
                        mutableIntStateOf(7)
                    }
                block.workouts().take(limit).forEach { workout ->
                    TextButton(onClick = { open(workout.id()) }) {
                        Text(
                            "${friendlyDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min"
                        )
                    }
                }
                if (limit < block.workouts().size)
                    TextButton(onClick = { limit += 7 }) { Text("Show more sessions") }
            }
        }
    }
}

@Composable
internal fun AuthoredProgrammeWorkout(
    programme: StrengthProgramme,
    id: String?,
    start: (() -> Unit)?,
) {
    val workout = programme.workouts().firstOrNull { it.id() == id }
    CoachPage {
        if (workout == null) {
            CoachHeading("Session unavailable")
            Text(
                "This session is not part of the current programme revision. Its recorded history remains preserved."
            )
        } else {
            CoachHeading(workout.definition().title())
            Text("${friendlyDate(workout.date())} · revision ${programme.revision()}")
            Text("About ${workout.expectedMinutes()} min · planned, not recorded")
            workout.definition().slots().forEach { slot ->
                SemanticCard(Tile.Strength) {
                    Text(slot.exercise().name(), style = MaterialTheme.typography.titleLarge)
                    Text(gymTargetDescription(slot))
                }
            }
            workout.notes()?.let { Text(it) }
            if (start != null) CoachButton("Start workout", start)
            Text(
                "Your entered sets are recorded independently. Starting does not complete the planned session."
            )
        }
    }
}
