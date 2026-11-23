package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.support.RunningRoutines

@Composable
fun SharedRoutineLibrary(open: (String) -> Unit) = CoachPage {
    ScreenHeader(Discipline.RECOVERY, Glyph.BODY, "Mobility", "Movement support")
    QuietNote(
        "Optional short routines. Two to four short mobility sessions can be useful when they fit your week. There is no compliance target, load or missed-mobility warning."
    )
    SectionLabel("Routines", Glyph.LIST, ChartHue.TEAL)
    RunningRoutines.all().forEach { r ->
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    requireNotNull(r.title()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${r.minutes()} min · ${r.purpose()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { open(requireNotNull(r.id())) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Discipline.RECOVERY.accent(),
                        contentColor = Discipline.RECOVERY.onAccent(),
                    ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text("Start ${r.title()}")
            }
        }
    }
    QuietNote(
        "General movement guidance. Keep movements comfortable, stop if they cause pain and seek qualified advice when unsure. This is not injury treatment or a recovery guarantee."
    )
}

/**
 * Actual optional routine execution; platform completion remains separate from training actuals.
 */
@Composable
fun SharedRoutineContent(
    id: String?,
    routine: RunningRoutines.Routine?,
    saving: Boolean,
    error: String?,
    illustration: @Composable (String) -> Unit,
    finish: () -> Unit,
) {
    if (routine == null) {
        CoachPage { Text("Routine unavailable") }
        return
    }
    var index by rememberSaveable(id) { mutableIntStateOf(0) }
    val movements = requireNotNull(routine.movements())
    val movement = movements[index]
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CoachHeading(requireNotNull(routine.title()))
            LinearProgressIndicator(
                progress = { (index + 1f) / movements.size },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text("${index+1} of ${movements.size}", style = MaterialTheme.typography.labelLarge)
            illustration(requireNotNull(movement.id()))
            Text(requireNotNull(movement.title()), style = MaterialTheme.typography.headlineLarge)
            Surface(color = guidanceBlue(), shape = MaterialTheme.shapes.large) {
                Text(
                    requireNotNull(movement.dose()),
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                requireNotNull(movement.instructions()),
                style = MaterialTheme.typography.bodyLarge,
            )
            GuidanceNote("Tip", requireNotNull(movement.cue()))
            GuidanceNote("Common mistake", requireNotNull(movement.mistake()), caution = true)
            WhyDetails(
                "Keep movements comfortable and stop if painful. General guidance, not individual assessment or treatment.",
                "About this routine",
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { index-- },
                enabled = index > 0,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Text("Back")
            }
            Button(
                onClick = { if (index + 1 < movements.size) index++ else finish() },
                enabled = !saving,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Text(if (index + 1 < movements.size) "Next" else "Finish routine")
            }
        }
        error?.let { Text(it) }
    }
}
