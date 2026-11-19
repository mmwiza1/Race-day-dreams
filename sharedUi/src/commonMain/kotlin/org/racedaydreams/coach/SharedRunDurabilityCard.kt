package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.presentation.DurabilityData

/** Actual recorded-run detail card; platform adapters supply existing unit/number text. */
@Composable
fun SharedRunDurabilityCard(
    activity: ActivityRecord,
    imperial: Boolean,
    formatPace: (Double) -> String,
    formatDecimal: (Double) -> String,
) {
    val durability = remember(activity) { DurabilityData.of(activity) }
    val evidence = checkNotNull(durability.evidence())
    CoachCard {
        Text("Long-run durability", style = MaterialTheme.typography.titleMedium)
        durability.status()?.let { Text(it) }
        if (evidence.sufficient()) {
            val factor = if (imperial) 1.609344 else 1.0
            val unit = if (factor > 1) "mi" else "km"
            durability.firstHalfSecondsPerKm()?.let {
                Text("First half: ${formatPace(it*factor)}/$unit")
            }
            durability.secondHalfSecondsPerKm()?.let {
                Text("Second half: ${formatPace(it*factor)}/$unit")
            }
            evidence.paceFadePercent()?.let { Text("Late-run pace change: ${formatDecimal(it)}%") }
            activity.rpe()?.let { Text("Session RPE: ${formatDecimal(it)}/10") }
        }
        WhyDetails(
            evidence.explanation() +
                " These are descriptive equal-time halves, not a precise fitness score. Terrain, stops, conditions and intention matter." +
                (evidence.heartRateDriftPercent()?.let {
                    " HR-to-pace change: ${formatDecimal(it)}%."
                } ?: " HR context unavailable."),
            "Durability details",
        )
    }
}
