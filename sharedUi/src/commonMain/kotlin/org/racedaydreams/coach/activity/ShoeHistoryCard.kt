package org.racedaydreams.coach.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.ShoeMetrics
import org.racedaydreams.domain.time.Instant

/** Existing production shoe card. Platform supplies unit/date formatting and optional insights. */
@Composable
fun ShoeHistoryCard(
    shoe: Shoe,
    summary: ShoeMetrics,
    busy: Boolean,
    formatDistance: (Double) -> String,
    formatFirstRunDate: (Instant) -> String,
    insights: () -> List<String>,
    onEdit: () -> Unit,
) {
    CoachCard {
        Text("${shoe.brand()} ${shoe.model()}".trim(), style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                shoe.retired() -> "Retired"
                shoe.defaultShoe() -> "Default for newly added runs"
                else -> "Active"
            },
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            summary.totalDistanceMetres()?.let { formatDistance(it) }
                ?: formatDistance(summary.recordedDistanceMetres()) + " recorded",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "${summary.runCount()} runs" +
                (summary.ageDays()?.let { " · ${if(it<60)"$it days"else"${it/30} months"}" } ?: "")
        )
        summary.wearGuidance()?.let { Text(it) }
        var expanded by rememberSaveable(shoe.id()) { mutableStateOf(false) }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide shoe details" else "View shoe details")
        }
        if (expanded) {
            Text("Recorded distance: ${formatDistance(summary.recordedDistanceMetres())}")
            summary.totalDistanceMetres()?.let {
                Text("Known mileage including entered starting mileage: ${formatDistance(it)}")
            }
                ?: Text(
                    "Earlier mileage was not supplied; recorded distance is a known lower bound, not lifetime shoe mileage."
                )
            Text(
                "${summary.runCount()} assigned runs · ${summary.runsWithKnownDistance()} with known distance"
            )
            if (summary.runsWithKnownDistance() < summary.runCount())
                Text(
                    "Some assigned runs have no distance. Mileage totals include known distances only."
                )
            summary.averageRunDistanceMetres()?.let {
                Text("Average known run distance: ${formatDistance(it)}")
            }
            summary.longestRunMetres()?.let { Text("Longest assigned run: ${formatDistance(it)}") }
            summary.ageDays()?.let { Text("Age since entered purchase date: $it days") }
            summary.firstRecordedRun()?.let {
                Text("First recorded run: ${formatFirstRunDate(it)} (device time zone)")
            }
            if (summary.firstRecordedRun() == null) Text("No first recorded run yet.")
            summary.wearGuidance()?.let { Text(it) }
            insights().forEach { Text(it) }
            shoe.notes()?.takeIf { it.isNotBlank() }?.let { Text(it) }
        }
        OutlinedButton(
            onClick = onEdit,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Edit shoes")
        }
        Text(
            "Assign or change these shoes from an individual run’s detail page.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
