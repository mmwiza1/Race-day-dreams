package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*

/** Read-only projection of completed actuals. No programme target is an analytics input. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrengthAnalyticsScreen(
    sessions: List<StrengthSession>,
    onBack: () -> Unit,
    asOf: Instant = Instant.now(),
    today: LocalDate = LocalDate.now(),
) {
    var window by rememberSaveable { mutableIntStateOf(8) }
    var attribution by rememberSaveable { mutableStateOf(false) }
    var exerciseDetails by rememberSaveable { mutableStateOf(false) }
    val cutoff = remember(sessions, today) { asOf }
    val weeks by
        produceState<List<StrengthVolume.Week>?>(null, sessions, window, today, cutoff) {
            value =
                withContext(Dispatchers.Default) {
                    StrengthVolume.weekly(
                        sessions,
                        today.minusWeeks(window.toLong() - 1),
                        window,
                        cutoff,
                    )
                }
        }
    val date = remember { DateTimeFormatter.ofPattern("d MMM", java.util.Locale.UK) }
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Back to Strength")
        }
        CoachHeading("Strength trends")
        Text("See what you recorded, rather than what your programme planned.")
        val rows = weeks
        if (rows == null) CircularProgressIndicator()
        else {
            val current = rows.last()
            SemanticCard(Tile.Strength) {
                Text("This week so far", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${current.workingSets()} recorded working ${if (current.workingSets() == 1) "set" else "sets"}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "${current.workouts()} recorded ${if (current.workouts() == 1) "workout" else "workouts"}"
                )
                if (current.workouts() == 0)
                    Text(
                        "No strength workouts recorded this week. Missing records do not tell us whether you trained."
                    )
                else if (current.workingSets() == 0)
                    Text(
                        "Only warm-up sets are recorded. They are kept separately from working sets."
                    )
                Text(
                    "Previous week: ${rows[rows.lastIndex-1].workingSets()} working sets. This week is still in progress."
                )
            }
            SemanticCard(Tile.Progress) {
                Text("Recorded working sets by week", style = MaterialTheme.typography.titleLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    listOf(8, 26, 52).forEach { count ->
                        CoachFilterChip(
                            selected = window == count,
                            onClick = { window = count },
                            label = { Text("$count weeks") },
                        )
                    }
                }
                val max = rows.maxOf { it.workingSets() }.coerceAtLeast(1)
                rows.forEach { week ->
                    val label =
                        "Week of ${week.monday().format(date)} · ${week.workingSets()} working sets · ${week.workouts()} workouts"
                    Column(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                            contentDescription = label
                        },
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "${week.monday().format(date)} · ${week.workingSets()} sets",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { week.workingSets().toFloat() / max },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clearAndSetSemantics {},
                        )
                    }
                }
                Text(
                    "Empty weeks mean no working sets were recorded. Set count does not measure total training stress.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SemanticCard(Tile.Insight) {
                Text("Primary areas this week", style = MaterialTheme.typography.titleLarge)
                Text("An approximate breakdown of your recorded working sets.")
                Text(org.racedaydreams.domain.presentation.AthleteLanguage.WORKING_SETS)
                current.primarySetShares().forEach { (area, shares) ->
                    Text("${area.label()} · ${decimal(shares)} set shares")
                }
                if (current.primarySetShares().isEmpty())
                    Text("No classified working sets recorded this week.")
                if (current.unclassifiedSets() > 0)
                    Text(
                        "${current.unclassifiedSets()} working sets have no supported muscle allocation. Their actual results are retained."
                    )
                TextButton(
                    onClick = { attribution = !attribution },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (attribution) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(
                        if (attribution) "Hide attribution details" else "How are sets attributed?"
                    )
                }
                if (attribution) {
                    Text(StrengthVolume.attribution())
                    Text("Secondary involvement", style = MaterialTheme.typography.titleMedium)
                    current.secondaryExposures().forEach { (area, exposures) ->
                        Text("${area.label()} · involved in $exposures working sets")
                    }
                    Text(
                        "Custom or changed exercise definitions remain unclassified; we do not infer missing anatomy."
                    )
                }
            }
            SemanticCard(Tile.Neutral) {
                Text("Exercise frequency and tasks", style = MaterialTheme.typography.titleLarge)
                Text("This week's completed working sets, with each measurement kept separate.")
                TextButton(
                    onClick = { exerciseDetails = !exerciseDetails },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (exerciseDetails) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(
                        if (exerciseDetails) "Hide exercise evidence" else "View exercise evidence"
                    )
                }
                if (exerciseDetails)
                    current.exercises().forEach { exercise ->
                        HorizontalDivider()
                        Text(
                            exercise.exercise().name(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${exercise.workingSets()} working sets across ${exercise.workouts()} workouts"
                        )
                        if (exercise.exercise().measurement().usesReps())
                            Text("${exercise.repetitions()} recorded repetitions")
                        if (exercise.seconds() > 0)
                            Text(
                                "${StrengthMeasurements.duration(exercise.seconds())} recorded duration"
                            )
                        if (exercise.metres() > 0)
                            Text("${decimal(exercise.metres())} m recorded distance")
                        exercise.loadRepetitions()?.let {
                            Text(
                                "${decimal(it)} kg × repetitions · total recorded weight across repetitions for this exercise only"
                            )
                        }
                    }
                if (exerciseDetails)
                    Text(
                        "External load × repetitions is descriptive, not a universal measure of effort. Bodyweight, holds and carries are not converted into equivalent kilograms.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
    }
}
