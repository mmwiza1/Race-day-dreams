package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.racedaydreams.domain.presentation.HomeCoachingSummary

/** Production Home summary; review callback keeps decision ownership with the platform. */
@Composable
fun SharedHomeCoachingCard(summary: HomeCoachingSummary, weekly: () -> Unit) {
    SemanticCard(Tile.Progress) {
        Text("How am I doing?", style = MaterialTheme.typography.titleMedium)
        summary.title()?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
        summary.observations().orEmpty().take(3).forEach { Text(it) }
        Text("Plan recommendation: ${summary.recommendation()}")
        TextButton(onClick = weekly) { Text("Weekly coaching review") }
    }
}
