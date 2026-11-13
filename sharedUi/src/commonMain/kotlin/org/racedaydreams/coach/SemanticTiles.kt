package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class Tile {
    Planned,
    Progress,
    Insight,
    Race,
    Strength,
    Achievement,
    Warning,
    Neutral,
}

@Composable
fun tileColour(tile: Tile): Color {
    val scheme = MaterialTheme.colorScheme
    return when (tile) {
        Tile.Planned,
        Tile.Race -> scheme.primaryContainer
        Tile.Strength,
        Tile.Achievement -> scheme.tertiaryContainer
        Tile.Progress -> scheme.secondaryContainer
        Tile.Insight,
        Tile.Neutral -> scheme.surfaceContainerHigh
        Tile.Warning -> scheme.errorContainer
    }
}

@Composable
fun SemanticCard(tile: Tile, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = tileColour(tile),
                contentColor =
                    when (tile) {
                        Tile.Planned,
                        Tile.Race -> MaterialTheme.colorScheme.onPrimaryContainer
                        Tile.Strength,
                        Tile.Achievement -> MaterialTheme.colorScheme.onTertiaryContainer
                        Tile.Progress -> MaterialTheme.colorScheme.onSecondaryContainer
                        Tile.Warning -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
            ),
    ) {
        Column(
            Modifier.padding(CoachSpace.Inset),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
            content = content,
        )
    }
}
