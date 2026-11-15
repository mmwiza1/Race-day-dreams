package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.activity.*

@Composable
fun SharedRunComparisonSection(
    current: ActivityRecord,
    activities: List<ActivityRecord>,
    prescriptions: List<PlannedWorkout>,
    imperial: Boolean,
    displayDate: (ActivityRecord) -> String,
    formatDistance: (Double) -> String,
    formatPace: (Double) -> String,
    formatDecimal: (Double) -> String,
) {
    var expanded by rememberSaveable(current.id()) { mutableStateOf(false) }
    var choose by rememberSaveable(current.id()) { mutableStateOf(false) }
    val matches =
        remember(current, activities, prescriptions) {
            ComparableRuns.find(current, activities, prescriptions)
        }
    var selected by rememberSaveable(current.id()) { mutableStateOf<String?>(null) }
    val prior = activities.firstOrNull { it.id() == selected } ?: matches.firstOrNull()?.activity()
    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text("Compare this run")
    }
    if (expanded) {
        if (prior == null) Text("No sufficiently comparable previous run found.")
        else
            CoachCard {
                Text("Run comparison", style = MaterialTheme.typography.titleLarge)
                Text("Current · ${displayDate(current)}")
                ComparisonMetrics(current, imperial, formatDistance, formatPace, formatDecimal)
                HorizontalDivider()
                Text("Previous · ${displayDate(prior)}")
                ComparisonMetrics(prior, imperial, formatDistance, formatPace, formatDecimal)
                Text(ComparableRuns.interpretation(current, prior))
                val match = matches.firstOrNull { it.activity()?.id() == prior.id() }
                WhyDetails(
                    match?.let {
                        "${it.confidence()} match: ${it.reasons()!!.joinToString("; ")}. Route matching is not assumed."
                    } ?: "You chose this comparison manually. Similarity has not been established.",
                    "Why these runs were compared",
                )
            }
        TextButton(onClick = { choose = true }) { Text("Choose another run") }
    }
    if (choose)
        AlertDialog(
            onDismissRequest = { choose = false },
            title = { Text("Choose previous run") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 350.dp)) {
                    val rows =
                        activities
                            .filter { it.id() != current.id() && it.start() < current.start() }
                            .sortedByDescending { it.start() }
                    items(rows.size, key = { rows[it].id() }) { i ->
                        TextButton(
                            onClick = {
                                selected = rows[i].id()
                                choose = false
                            }
                        ) {
                            Text(
                                "${displayDate(rows[i])} · ${rows[i].distanceMetres()?.let{formatDistance(it)}?:"Distance unavailable"}"
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choose = false }) { Text("Cancel") } },
        )
}

@Composable
private fun ComparisonMetrics(
    a: ActivityRecord,
    imperial: Boolean,
    formatDistance: (Double) -> String,
    formatPace: (Double) -> String,
    formatDecimal: (Double) -> String,
) {
    Text(
        "${a.distanceMetres()?.let{formatDistance(it)}?:"Distance unavailable"} · ${PaceCalculator.formatTime(a.elapsedTime())}"
    )
    val distance = a.distanceMetres()
    if (distance != null && distance > 0)
        Text(
            "${formatPace(a.elapsedTime().getSeconds()/(distance/1000)*(if(imperial)1.609344 else 1.0))}/${if(imperial)"mi"else "km"} average"
        )
    Text(
        a.averageHeartRate()?.let { "${formatDecimal(it)} bpm average" } ?: "Heart rate unavailable"
    )
    Text(a.rpe()?.let { "RPE ${formatDecimal(it)}" } ?: "RPE not recorded")
    val durability = LongRunDurability.analyse(a)
    durability.explanation()?.let { Text(it) }
}
