package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import org.racedaydreams.domain.strength.TechniqueGuide

/** Android asset and window boundary for the actual shared technique presentation. */
@Composable
fun TechniqueSection(id: String) {
    val guide = TechniqueGuide.forExercise(id)
    val art = if (guide == null) null else exerciseArtwork(id)
    SharedTechniqueContent(
        guide = guide,
        phases = art?.phases,
        wideNotes =
            LocalDensity.current.fontScale <= 1.15f &&
                LocalConfiguration.current.screenWidthDp >= 350,
        illustration = { if (art != null) IllustratedMovement(art) },
    )
}
