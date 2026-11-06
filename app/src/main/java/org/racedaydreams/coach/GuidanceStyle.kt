package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared soft blue/navy guidance surfaces. No screen owns its own palette. */
@Composable
fun guidanceBlue() =
    androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.primaryContainer,
        0.45f,
    )

@Composable
fun guidanceMint() =
    androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.tertiaryContainer,
        0.45f,
    )

@Composable
fun guidancePink() =
    androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.errorContainer,
        0.45f,
    )

@Composable
fun GuidanceCard(
    colour: Color = guidanceBlue(),
    padding: androidx.compose.ui.unit.Dp = 18.dp,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = colour,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Column(
            Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@Composable
fun GuidanceNote(
    title: String,
    text: String,
    caution: Boolean = false,
    modifier: Modifier = Modifier,
) =
    GuidanceCard(if (caution) guidancePink() else guidanceMint(), modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
