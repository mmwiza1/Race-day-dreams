package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A neutral activity heading. Reserve the discipline colour for the icon and the next action. */
@Composable
fun ActivityHeading(
    title: String,
    glyph: Glyph,
    context: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Card),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoachSpace.Tiny)) {
            Text(
                title,
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                modifier = Modifier.semantics { heading() },
            )
            context?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

enum class ActivityActionRole {
    PRIMARY,
    SECONDARY,
    TERTIARY,
}

/** Filled for the current task, tonal for a supporting action, text for navigation. */
@Composable
fun ActivityAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: ActivityActionRole = ActivityActionRole.PRIMARY,
    enabled: Boolean = true,
) {
    val target = modifier.heightIn(min = CoachSpace.Touch)
    val shape = MaterialTheme.shapes.small
    when (role) {
        ActivityActionRole.PRIMARY ->
            Button(onClick, target, enabled = enabled, shape = shape) { Text(label) }
        ActivityActionRole.SECONDARY ->
            FilledTonalButton(onClick, target, enabled = enabled, shape = shape) { Text(label) }
        ActivityActionRole.TERTIARY ->
            TextButton(onClick, target, enabled = enabled, shape = shape) { Text(label) }
    }
}

@Composable
fun activityNumberStyle() =
    MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    )
