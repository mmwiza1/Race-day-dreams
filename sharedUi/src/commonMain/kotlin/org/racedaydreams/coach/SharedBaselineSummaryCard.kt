package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.Instant

/** Actual baseline summary; platform supplies existing display units and UTC boundary labels. */
@Composable
fun SharedBaselineSummaryCard(
    baseline: RunnerBaseline,
    formatDistance: (Double) -> String,
    formatNumber: (Double) -> String,
    formatTime: (Duration) -> String,
    formatUtcDate: (Instant) -> String,
) {
    CoachCard {
        Text(
            "Does this look right?",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (baseline.activityCount() > 0)
            Text("Found ${baseline.activityCount()} recorded runs in the available history window.")
        else Text("No running history is available yet. You can continue without it.")
        WhyDetails(
            "Window: ${formatUtcDate(baseline.windowStart())} to ${formatUtcDate(baseline.windowEnd())} (UTC boundaries)",
            "History window",
        )
        BaselineSummaryValue(
            "Average recorded weekly distance",
            baseline.weeklyDistanceKm()?.let { formatDistance(it * 1000) },
            RunnerBaseline.WEEKLY_DISTANCE,
            baseline,
        )
        BaselineSummaryValue(
            "Usual recorded runs/week",
            baseline.runsPerWeek()?.let(formatNumber),
            RunnerBaseline.RUNS_PER_WEEK,
            baseline,
        )
        BaselineSummaryValue(
            "Longest recent run",
            baseline.longestRunKm()?.let { formatDistance(it * 1000) },
            RunnerBaseline.LONGEST_RUN,
            baseline,
        )
        BaselineSummaryValue(
            "Typical elapsed run duration",
            baseline.typicalRunMinutes()?.let { "${formatNumber(it)} min" },
            RunnerBaseline.TYPICAL_DURATION,
            baseline,
        )
        BaselineSummaryValue(
            "Recent 5K result",
            baseline.recent5k()?.let(formatTime),
            RunnerBaseline.RECENT_5K,
            baseline,
        )
        BaselineSummaryValue(
            "Recent 10K result",
            baseline.recent10k()?.let(formatTime),
            RunnerBaseline.RECENT_10K,
            baseline,
        )
        BaselineSummaryValue(
            "Recent half-marathon result",
            baseline.recentHalf()?.let(formatTime),
            RunnerBaseline.RECENT_HALF,
            baseline,
        )
        Text(
            "Baseline confidence: ${baseline.confidence().name.lowercase().replaceFirstChar { it.uppercase() }}"
        )
        WhyDetails(
            "Confidence describes the amount, recency and quality of evidence. It is not a probability of race success.",
            "About confidence",
        )
    }
}

@Composable
private fun BaselineSummaryValue(
    label: String,
    value: String?,
    key: String,
    baseline: RunnerBaseline,
) {
    if (value != null) {
        val origin = baseline.provenance()[key]?.name?.lowercase()?.replace('_', ' ')
        Text("$label: $value${origin?.let { " · $it" } ?: ""}")
    }
}
