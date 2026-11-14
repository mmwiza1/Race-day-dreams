package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.strength.TechniqueGuide

/**
 * Actual technique content; platform code supplies the reviewed image and original window policy.
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
            "A visual guide for this exercise is not available yet. Choose a familiar movement or seek qualified coaching."
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
                    modifier = Modifier.weight(1f),
                )
            }
        }
        GuidanceCard {
            Text("How to do it", style = MaterialTheme.typography.titleMedium)
            Text("1. ${guide.setup()}", style = MaterialTheme.typography.bodyMedium)
            Text("2. ${guide.movement()}", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Text("Illustrated guidance is not available for this movement yet.")
        GuidanceCard {
            Text("How to do it", style = MaterialTheme.typography.titleMedium)
            Text(requireNotNull(guide.setup()))
            Text(requireNotNull(guide.movement()))
        }
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
    Text("Primary muscles: ${guide.muscles()}")
    Text("Alternatives: ${requireNotNull(guide.alternatives()).joinToString()}")
    WhyDetails(
        "General technique guidance. Start conservatively and seek qualified in-person coaching if you are unsure about a movement. Illustrations are general guidance, not individual form assessment or an exact joint-angle prescription.",
        "About this guidance",
    )
}
