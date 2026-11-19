package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Existing first-run focus choice. Saving and routing happen in the real host after success. */
@Composable
fun SharedStartingPath(
    record: () -> Unit,
    race: () -> Unit,
    strength: () -> Unit,
    hybrid: () -> Unit,
) = CoachPage {
    CoachHeading("Make today yours")
    Text(
        "Choose your primary focus. You can change it in Settings, and every feature remains available."
    )
    SemanticCard(Tile.Planned) {
        Text("Running", style = MaterialTheme.typography.titleLarge)
        Text(
            "Run independently or work towards a race. No gym programme or health access required."
        )
        CoachButton("Record my runs", record)
        CoachButton("Train for a race", race)
    }
    SemanticCard(Tile.Strength) {
        Text("Strength", style = MaterialTheme.typography.titleLarge)
        Text(
            "Set up optional strength programming or log your own workouts. No race goal or running profile required."
        )
        CoachButton("Focus on strength", strength)
    }
    SemanticCard(Tile.Race) {
        Text("Hybrid", style = MaterialTheme.typography.titleLarge)
        Text("Begin with your running goal; add strength when you choose.")
        CoachButton("Running + strength", hybrid)
    }
}
