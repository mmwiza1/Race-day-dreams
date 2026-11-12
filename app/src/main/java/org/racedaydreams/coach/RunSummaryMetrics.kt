@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord

/** Presentation only: source summaries retain their original distance and timing semantics. */
@Composable
internal fun RecordedRunSummary(activity: ActivityRecord, state: CoachUiState) {
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    Card(
        modifier = Modifier.fillMaxWidth().testTag("run-summary-metrics"),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Recorded summary", style = MaterialTheme.typography.labelLarge)
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text("Elapsed time", style = MaterialTheme.typography.labelLarge)
                Text(
                    PaceCalculator.formatTime(activity.elapsedTime()),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2,
            ) {
                RunSummaryValue(
                    "Distance",
                    activity.distanceMetres()?.let { formatDistance(it, state) }
                        ?: "Distance unavailable",
                    Modifier.weight(1f),
                )
                RunSummaryValue(
                    "Elapsed average pace",
                    recordedElapsedPace(activity, imperial) ?: "Pace unavailable",
                    Modifier.weight(1f),
                )
            }
            activity.movingTime()?.let {
                Text(
                    "Moving time: ${PaceCalculator.formatTime(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "Elapsed time includes pauses. Average pace uses that whole time and your recorded distance; moving time, when available, counts movement separately.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** No zone boundaries, missing values or whole-session HR streams are inferred from summaries. */
@Composable
internal fun RecordedHeartRateSummary(activity: ActivityRecord) {
    CoachCard {
        Column(
            Modifier.fillMaxWidth().testTag("recorded-heart-rate"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Recorded heart rate", style = MaterialTheme.typography.titleLarge)
            if (activity.averageHeartRate() == null && activity.maxHeartRate() == null) {
                Text("Average and maximum heart-rate summaries are unavailable for this run.")
            } else {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2,
                ) {
                    RunSummaryValue(
                        "Average recorded heart rate",
                        activity.averageHeartRate()?.let { "${decimal(it)} bpm" } ?: "Unavailable",
                        Modifier.weight(1f),
                    )
                    RunSummaryValue(
                        "Maximum recorded heart rate",
                        activity.maxHeartRate()?.let { "${decimal(it)} bpm" } ?: "Unavailable",
                        Modifier.weight(1f),
                    )
                }
                Text(
                    "Source-supplied measurements. Missing heart-rate observations remain unknown.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RunSummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

internal fun recordedElapsedPace(activity: ActivityRecord, imperial: Boolean): String? =
    activity
        .distanceMetres()
        ?.takeIf { it > 0 }
        ?.let { metres ->
            val unitMetres = if (imperial) 1609.344 else 1000.0
            "${formatPaceSeconds(activity.elapsedTime().toMillis() / 1000.0 / (metres / unitMetres))}/${if (imperial) "mi" else "km"}"
        }
