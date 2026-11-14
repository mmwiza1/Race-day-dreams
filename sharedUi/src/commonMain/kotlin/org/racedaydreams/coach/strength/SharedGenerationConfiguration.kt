@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/**
 * The actual programme configuration form; platform calendar/formatters are supplied explicitly.
 */
@Composable
fun ColumnScope.SharedGenerationConfiguration(
    templateId: String?,
    enabled: Boolean,
    initial: StrengthProgramme?,
    externalError: String?,
    today: () -> LocalDate,
    formatDate: (LocalDate) -> String,
    parseDate: (String) -> LocalDate,
    weekday: (Int) -> DayOfWeek,
    weekdayLabel: (DayOfWeek) -> String,
    formatLabel: (String) -> String,
    generate: (String, LocalDate, Int, StrengthProgramme.Settings, Boolean) -> Unit,
) {
    val template =
        remember(templateId) { templateId?.let { StrengthProgrammeTemplates.require(it) } }
    var name by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(initial?.name() ?: template?.name() ?: "My strength programme")
        }
    var date by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(formatDate(initial?.start() ?: today()))
        }
    var weeks by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf((initial?.blocks()?.sumOf { it.weeks() } ?: 12).toString())
        }
    var goal by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(
                (initial?.settings()?.goal()
                        ?: template?.defaultGoal()
                        ?: GymProgramme.Goal.GENERAL)
                    .name
            )
        }
    var experience by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(
                (initial?.settings()?.experience() ?: GymProgramme.Experience.BEGINNER).name
            )
        }
    var priority by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf((initial?.settings()?.priority() ?: GymProgramme.Priority.BALANCED).name)
        }
    var frequency by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(initial?.settings()?.frequency() ?: template?.frequency() ?: 3)
        }
    var minutes by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(
                (initial?.settings()?.sessionMinutes() ?: template?.suggestedMinutes() ?: 45)
                    .toString()
            )
        }
    var days by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(
                (initial?.settings()?.days() ?: DayOfWeek.values().toSet()).map { it.getValue() }
            )
        }
    var equipment by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(initial?.settings()?.equipment()?.map { it.name } ?: emptyList<String>())
        }
    var showEquipment by rememberSaveable(templateId, initial?.id()) { mutableStateOf(false) }
    var recovery by
        rememberSaveable(templateId, initial?.id()) {
            mutableStateOf(
                initial?.blocks()?.any {
                    it.phase() == StrengthProgramme.Phase.RECOVERY_ASSESSMENT
                } == true
            )
        }
    var error by rememberSaveable(templateId, initial?.id()) { mutableStateOf<String?>(null) }
    template?.let {
        SemanticCard(Tile.Insight) {
            if (it.frequency() != frequency) {
                Text("Customised structure", style = MaterialTheme.typography.titleLarge)
                Text(
                    "A new $frequency-day split follows your goal. The previous template remains preserved in the original revision."
                )
            } else {
                Text(requireNotNull(it.name()), style = MaterialTheme.typography.titleLarge)
                Text(requireNotNull(it.emphasis()))
                Text(requireNotNull(it.tradeoff()))
            }
        }
    }
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    GenerationField(name, { name = it }, "Programme name", enabled)
    GenerationField(
        date,
        { date = it },
        "Start date · dd/mm/yyyy",
        enabled,
        readOnly = (initial?.parentRevision() ?: 0) > 0,
    )
    if ((initial?.parentRevision() ?: 0) > 0)
        Text(
            "Original start retained. This regeneration proposes future changes only; earlier and completed workouts keep their exact prescriptions."
        )
    GenerationField(weeks, { weeks = it }, "Programme duration · weeks (1–26)", enabled, true)
    Text("Goal")
    GenerationChoices(GymProgramme.Goal.values().map { it.name }, goal, enabled, formatLabel) {
        goal = it
    }
    Text("Experience")
    GenerationChoices(
        GymProgramme.Experience.values().map { it.name },
        experience,
        enabled,
        formatLabel,
    ) {
        experience = it
    }
    Text("Priority")
    GenerationChoices(
        GymProgramme.Priority.values().map { it.name },
        priority,
        enabled,
        formatLabel,
    ) {
        priority = it
    }
    if (template == null || (initial?.parentRevision() ?: 0) > 0) {
        Text("Workouts per programme week")
        GenerationChoices(
            (1..6).map { it.toString() },
            frequency.toString(),
            enabled,
            formatLabel,
        ) {
            frequency = it.toInt()
        }
    } else Text("${template.frequency()} workouts per programme week · template structure")
    GenerationField(minutes, { minutes = it }, "Usual session length · minutes", enabled, true)
    Text("Available days · choose at least $frequency")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
    ) {
        DayOfWeek.values().forEach { day ->
            CoachFilterChip(
                day.getValue() in days,
                {
                    days =
                        if (day.getValue() in days) days - day.getValue() else days + day.getValue()
                },
                enabled = enabled,
                label = { Text(weekdayLabel(day)) },
            )
        }
    }
    TextButton(onClick = { showEquipment = !showEquipment }, enabled = enabled) {
        Text("Available equipment · ${equipment.size} capabilities")
    }
    Text(
        "Bodyweight needs no checkbox. Select the equipment you can actually use; unavailable setups will not be prescribed."
    )
    if (showEquipment)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        ) {
            ExerciseMetadata.Equipment.values().forEach { item ->
                CoachFilterChip(
                    item.name in equipment,
                    {
                        equipment =
                            if (item.name in equipment) equipment - item.name
                            else equipment + item.name
                    },
                    enabled = enabled,
                    label = { Text(formatLabel(item.name)) },
                )
            }
        }
    Row(
        Modifier.fillMaxWidth()
            .toggleable(
                value = recovery,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { recovery = it },
            )
    ) {
        Checkbox(recovery, null, enabled = enabled)
        Text(
            "Include a lower-volume recovery / assessment block when duration supports it",
            Modifier.weight(1f),
        )
    }
    Text(
        "No programme changes yet. Hybrid previews consider your retained demanding run days and race context without moving runs."
    )
    externalError?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Button(
        onClick = {
            try {
                val settings =
                    StrengthProgramme.Settings(
                        GymProgramme.Goal.valueOf(goal),
                        GymProgramme.Priority.valueOf(priority),
                        GymProgramme.Experience.valueOf(experience),
                        days.map { weekday(it) }.toSet(),
                        frequency,
                        minutes.toIntOrNull()
                            ?: throw IllegalArgumentException("Enter a whole session length."),
                        equipment.map { ExerciseMetadata.Equipment.valueOf(it) }.toSet(),
                    )
                generate(
                    name,
                    parseDate(date),
                    weeks.toIntOrNull()
                        ?: throw IllegalArgumentException("Enter whole programme weeks."),
                    settings,
                    recovery,
                )
                error = null
            } catch (failure: Exception) {
                error = failure.message ?: "Review these preferences."
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Generate saved preview")
    }
}

@Composable
private fun GenerationField(
    value: String,
    change: (String) -> Unit,
    label: String,
    enabled: Boolean,
    number: Boolean = false,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        value,
        change,
        label = { Text(label) },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GenerationChoices(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    formatLabel: (String) -> String,
    choose: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
    ) {
        options.forEach { value ->
            CoachFilterChip(
                value == selected,
                { choose(value) },
                enabled = enabled,
                label = { Text(formatLabel(value)) },
            )
        }
    }
}
