package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.activity.ActivityRecord

/**
 * Presentation only: source summaries retain their original distance and timing semantics. The
 * headline figures lead (elapsed time, then distance and elapsed pace in their measure hues) on a
 * borderless panel under a glyph-labelled section heading.
 */
@Composable
fun SharedRecordedRunSummary(
    activity: ActivityRecord,
    formatDistance: (Double) -> String,
    elapsedPace: String?,
) {
    Column(
        Modifier.fillMaxWidth().testTag("run-summary-metrics"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Recorded summary", Glyph.RUN, ChartHue.TEAL)
        when (activity.environment()) {
            org.racedaydreams.domain.activity.RunEnvironment.TREADMILL -> Text("Treadmill run")
            org.racedaydreams.domain.activity.RunEnvironment.INDOOR -> Text("Indoor run")
            null -> Unit
        }
        Panel(contentPadding = PaddingValues(20.dp)) {
            MeasureFigure(
                "Elapsed time",
                PaceCalculator.formatTime(activity.elapsedTime()),
                ChartHue.TEAL,
                large = true,
            )
            Spacer(Modifier.height(4.dp))
            MeasureFigureRow(
                listOf(
                    Triple(
                        "Distance",
                        activity.distanceMetres()?.let(formatDistance) ?: "Distance unavailable",
                        ChartHue.BLUE,
                    ),
                    Triple(
                        "Elapsed average pace",
                        elapsedPace ?: "Pace unavailable",
                        ChartHue.VIOLET,
                    ),
                )
            )
            activity.movingTime()?.let {
                Text(
                    "Moving time: ${PaceCalculator.formatTime(it)}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                "Elapsed time includes pauses. Average pace uses the whole elapsed time and recorded distance. Moving time, when available, is reported separately by the recording or provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** No zone boundaries, missing values or whole-session HR streams are inferred from summaries. */
@Composable
fun SharedRecordedHeartRateSummary(activity: ActivityRecord, decimal: (Double) -> String) {
    Column(
        Modifier.fillMaxWidth().testTag("recorded-heart-rate"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Recorded heart rate", Glyph.TREND, ChartHue.CORAL)
        Panel {
            if (activity.averageHeartRate() == null && activity.maxHeartRate() == null) {
                Text("Average and maximum heart-rate summaries are unavailable for this run.")
            } else {
                MeasureFigureRow(
                    listOf(
                        Triple(
                            "Average recorded heart rate",
                            activity.averageHeartRate()?.let { "${decimal(it)} bpm" }
                                ?: "Unavailable",
                            ChartHue.CORAL,
                        ),
                        Triple(
                            "Maximum recorded heart rate",
                            activity.maxHeartRate()?.let { "${decimal(it)} bpm" } ?: "Unavailable",
                            ChartHue.CORAL,
                        ),
                    )
                )
                Text(
                    "Source-supplied measurements. Missing heart-rate observations remain unknown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun sharedRecordedElapsedPace(
    activity: ActivityRecord,
    imperial: Boolean,
    formatPaceSeconds: (Double) -> String,
): String? =
    activity
        .distanceMetres()
        ?.takeIf { it > 0 }
        ?.let { metres ->
            val unitMetres = if (imperial) 1609.344 else 1000.0
            "${formatPaceSeconds(activity.elapsedTime().toMillis() / 1000.0 / (metres / unitMetres))}/${if (imperial) "mi" else "km"}"
        }
