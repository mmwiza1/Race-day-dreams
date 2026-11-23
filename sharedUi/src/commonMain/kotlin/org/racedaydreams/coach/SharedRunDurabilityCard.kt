package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.presentation.DurabilityData

/**
 * Actual recorded-run detail section (pace hue, borderless panel); platform adapters supply
 * existing unit/number text.
 */
@Composable
fun SharedRunDurabilityCard(
    activity: ActivityRecord,
    imperial: Boolean,
    formatPace: (Double) -> String,
    formatDecimal: (Double) -> String,
) {
    val durability = remember(activity) { DurabilityData.of(activity) }
    val evidence = checkNotNull(durability.evidence())
    ExpressiveSection("Long-run durability", Glyph.TREND, ChartHue.VIOLET) {
        durability.status()?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (evidence.sufficient()) {
            val factor = if (imperial) 1.609344 else 1.0
            val unit = if (factor > 1) "mi" else "km"
            durability.firstHalfSecondsPerKm()?.let {
                Text(
                    "First half: ${formatPace(it*factor)}/$unit",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            durability.secondHalfSecondsPerKm()?.let {
                Text(
                    "Second half: ${formatPace(it*factor)}/$unit",
                    style = MaterialTheme.typography.titleMedium,
                )
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
