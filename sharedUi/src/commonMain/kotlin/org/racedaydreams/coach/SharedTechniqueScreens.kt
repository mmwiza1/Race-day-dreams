package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.strength.TechniqueGuide

/**
 * Actual technique content; platform code supplies the reviewed image and original window policy.
 * Presentation follows the expressive language: the steps carry a section label outside a
 * borderless panel, while the cue/mistake notes keep their meaning-bearing tints.
 */
@Composable
fun SharedTechniqueContent(
    guide: TechniqueGuide?,
    phases: List<String>?,
    wideNotes: Boolean,
    illustration: @Composable () -> Unit,
) {
    if (guide == null) {
        Text(
            "A visual guide for this exercise is not available yet. Choose a familiar movement or seek qualified coaching.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    if (phases != null) {
        illustration()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            phases.forEachIndexed { i, label ->
                Text(
                    "${i+1}. $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        TechniqueSteps(listOf("1. ${guide.setup()}", "2. ${guide.movement()}"))
    } else {
        Text(
            "Illustrated guidance is not available for this movement yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TechniqueSteps(listOf(requireNotNull(guide.setup()), requireNotNull(guide.movement())))
    }
    val cues = requireNotNull(guide.cues()).joinToString("\n") { "✓ $it" }
    val mistakes = requireNotNull(guide.mistakes()).joinToString("\n") { "• $it" }
    if (wideNotes) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GuidanceNote("Key cues", cues, modifier = Modifier.weight(1f))
            GuidanceNote(
                "Common mistakes",
                mistakes,
                caution = true,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        GuidanceNote("Key cues", cues)
        GuidanceNote("Common mistakes", mistakes, caution = true)
    }
    Panel(contentPadding = PaddingValues(16.dp)) {
        Text("Primary muscles: ${guide.muscles()}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Alternatives: ${requireNotNull(guide.alternatives()).joinToString()}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    WhyDetails(
        "General technique guidance. Start conservatively and seek qualified in-person coaching if you are unsure about a movement. Illustrations are general guidance, not individual form assessment or an exact joint-angle prescription.",
        "About this guidance",
    )
}

/** "How to do it" label outside a borderless panel of the guide's own setup/movement steps. */
@Composable
private fun TechniqueSteps(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("How to do it", Glyph.LIST, ChartHue.PLUM)
        Panel(contentPadding = PaddingValues(16.dp)) {
            steps.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
