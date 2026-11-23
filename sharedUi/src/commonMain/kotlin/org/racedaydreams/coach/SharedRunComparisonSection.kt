package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
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
    // A quiet, full-width disclosure row rather than an outlined button: the comparison is
    // optional.
    Surface(
        onClick = { expanded = !expanded },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier =
            Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DisciplineGlyphDisc(Discipline.RUNNING, Glyph.TREND, 36.dp)
            Text(
                "Compare this run",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (expanded) "−" else "+",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
    if (expanded) {
        if (prior == null) Text("No sufficiently comparable previous run found.")
        else
            Panel {
                Text(
                    "Run comparison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                ComparisonColumn("Current · ${displayDate(current)}") {
                    ComparisonMetrics(current, imperial, formatDistance, formatPace, formatDecimal)
                }
                ComparisonColumn("Previous · ${displayDate(prior)}") {
                    ComparisonMetrics(prior, imperial, formatDistance, formatPace, formatDecimal)
                }
                Text(
                    ComparableRuns.interpretation(current, prior),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val match = matches.firstOrNull { it.activity()?.id() == prior.id() }
                WhyDetails(
                    match?.let {
                        "${it.confidence()} match: ${it.reasons()!!.joinToString("; ")}. Route matching is not assumed."
                    } ?: "You chose this comparison manually. Similarity has not been established.",
                    "Why these runs were compared",
                )
            }
        TextButton(onClick = { choose = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Choose another run")
        }
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
        "${a.distanceMetres()?.let{formatDistance(it)}?:"Distance unavailable"} · ${PaceCalculator.formatTime(a.elapsedTime())}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
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

/** One side of the comparison: a labelled column with a pace-hue rule, no nested card. */
@Composable
private fun ComparisonColumn(label: String, content: @Composable ColumnScope.() -> Unit) {
    val tones = chartAccent(ChartHue.VIOLET)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier.width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(tones.soft)
                .clearAndSetSemantics {}
        )
        Column(
            Modifier.padding(start = 12.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
