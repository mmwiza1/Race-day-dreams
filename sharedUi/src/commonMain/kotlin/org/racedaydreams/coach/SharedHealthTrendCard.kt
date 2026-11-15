package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.racedaydreams.domain.health.HealthIntelligence
import org.racedaydreams.domain.presentation.HealthContextSummary

/** Actual health-context card; platform keeps permission requests, reads and number formatting. */
@Composable
fun SharedHealthTrendCard(t: HealthIntelligence.Trend, formatValue: (Double) -> String) {
    val summary = HealthContextSummary.of(t)
    GuidanceCard {
        summary.title()?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        if (t.metric() == HealthIntelligence.Metric.HRV_RMSSD)
            Text(
                "The gaps between heartbeats vary. Look at patterns from the same source, rather than one reading."
            )
        summary.value()?.let { Text(it, style = MaterialTheme.typography.headlineMedium) }
        if (t.recent() != null)
            Text("Recent daily average", style = MaterialTheme.typography.labelSmall)
        summary.context()?.let { Text(it) }
        WhyDetails(
            "Recent: ${t.recent()?.let{formatValue(it)}?:"Unavailable"} · Reference: ${t.baseline()?.let{formatValue(it)}?:"Unavailable"}\n${t.recentDays()}/7 recent days · ${t.referenceDays()}/28 reference days\nSource: ${t.source()}\n${t.limitation()}" +
                if (t.metric() == HealthIntelligence.Metric.SLEEP_MINUTES)
                    "\nSleep-session duration may include awake time; sleep stages are not used."
                else "",
            "View evidence & sources",
        )
        if (t.metric() == HealthIntelligence.Metric.HRV_RMSSD)
            WhyDetails(
                "HRV means heart-rate variability. These readings use RMSSD, measured in milliseconds (ms). Different devices or methods may not be comparable. Missing or old readings do not establish how recovered you are.",
                "About this measurement",
            )
    }
}
