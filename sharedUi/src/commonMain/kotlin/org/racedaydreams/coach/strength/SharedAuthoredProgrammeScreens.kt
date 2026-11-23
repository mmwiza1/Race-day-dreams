package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.StrengthProgramme

/** Frozen authored prescriptions are never converted to the older integer-RPE model. */
@Composable
fun SharedAuthoredProgrammeOverview(
    programme: StrengthProgramme,
    open: (String) -> Unit,
    formatDate: (org.racedaydreams.domain.time.LocalDate) -> String,
    author: (() -> Unit)? = null,
) {
    CoachPage {
        ScreenHeader(Discipline.STRENGTH, Glyph.CALENDAR, "Gym programme", programme.name())
        if (author != null) GymRowAction("Build or review programmes", true, author)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Programme revision ${programme.revision()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${formatDate(programme.start())} – ${formatDate(programme.end())} · ${blockCount(programme.blocks().size)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WhyDetails(programme.reason(), "Why this revision?")
        if (programme.tradeoffs().isNotEmpty())
            WhyDetails(programme.tradeoffs().joinToString("\n"), "Programme trade-offs")
        programme.blocks().forEach { block ->
            // Block header: the block (phase) name, its dates and length, then its sessions.
            SectionLabel(
                block.name(),
                Glyph.FLAG,
                ChartHue.PLUM,
                caption =
                    "${formatDate(block.start())} – ${formatDate(block.end())} · ${block.weeks()} weeks",
            )
            Panel(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                WhyDetails(block.explanation(), "Block emphasis")
                var limit by
                    remember(programme.id(), programme.revision(), block.id()) {
                        mutableIntStateOf(7)
                    }
                val chronological =
                    remember(block) { block.workouts().sortedBy { it.date().toEpochDay() } }
                chronological.take(limit).forEach { workout ->
                    HorizontalDivider()
                    GymSessionLink(
                        formatDate(workout.date()),
                        workout.definition().title(),
                        "${exerciseCount(workout.definition().slots().size)} · about ${workout.expectedMinutes()} min",
                    ) {
                        open(workout.id())
                    }
                }
                if (limit < block.workouts().size)
                    TextButton(
                        onClick = { limit += 7 },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Show more sessions")
                    }
            }
        }
    }
}

private fun blockCount(count: Int) = if (count == 1) "1 block" else "$count blocks"

@Composable
fun SharedAuthoredProgrammeWorkout(
    programme: StrengthProgramme,
    id: String?,
    start: (() -> Unit)?,
    formatDate: (org.racedaydreams.domain.time.LocalDate) -> String,
    formatTarget: (org.racedaydreams.domain.strength.GymWorkoutDefinition.Slot) -> String,
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
            Text("${formatDate(workout.date())} · revision ${programme.revision()}")
            Text("About ${workout.expectedMinutes()} min · planned, not recorded")
            workout.definition().slots().forEach { slot ->
                SemanticCard(Tile.Strength) {
                    Text(slot.exercise().name(), style = MaterialTheme.typography.titleLarge)
                    SharedPlainWorkoutTarget(slot, formatTarget)
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
