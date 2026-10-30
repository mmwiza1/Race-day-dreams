@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.*
import java.time.format.*
import java.util.Locale
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*

/** One phone template edits typed drafts. No execution clock or automatic programme changes. */
@Composable
fun StrengthProgrammeBuilderScreen(
    model: StrengthProgrammeViewModel,
    onBack: () -> Unit,
    onActivated: () -> Unit,
    onCreateCustom: (() -> Unit)? = null,
) {
    val state by model.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf("drafts") }
    var expandedDay by rememberSaveable { mutableStateOf<String?>(null) }
    var editHeader by rememberSaveable { mutableStateOf(false) }
    var blockForm by remember {
        mutableStateOf<Pair<String?, StrengthProgrammeDraft.WeekBlock?>?>(null)
    }
    var dayForm by remember { mutableStateOf<Pair<String, StrengthProgrammeDraft.Day?>?>(null) }
    var slotForm by remember { mutableStateOf<ProgrammeSlotLocation?>(null) }
    var moveSlot by remember { mutableStateOf<ProgrammeSlotLocation?>(null) }
    var duplicateDay by remember { mutableStateOf<Pair<String, StrengthProgrammeDraft.Day>?>(null) }
    var delete by remember { mutableStateOf<StrengthProgrammingRepository.DraftSummary?>(null) }
    var confirmStop by remember { mutableStateOf<StrengthProgrammingRepository.Reference?>(null) }
    var confirmActivate by remember { mutableStateOf<ProgrammePreview?>(null) }
    val enabled = !state.loading && !state.busy
    val draft = state.draft
    fun back() {
        if (state.busy) return
        if (section == "preview") {
            model.dismissPreview()
            section = "edit"
        } else if (section == "edit") {
            model.closeEditor()
            section = "drafts"
        } else onBack()
    }
    LaunchedEffect(state.loading, state.draft?.id(), state.preview) {
        if (!state.loading && section == "preview" && state.preview == null)
            section = if (state.draft == null) "drafts" else "edit"
        if (!state.loading && section == "edit" && state.draft == null) section = "drafts"
    }
    BackHandler { back() }
    key(section) {
        CoachPage {
            TextButton(onClick = ::back, enabled = !state.busy) {
                Text(
                    if (section == "preview") "Edit draft"
                    else if (section == "edit") "Saved drafts" else "Back"
                )
            }
            CoachHeading(
                if (section == "drafts") "Strength programmes"
                else if (section == "preview") "Review programme" else "Programme builder"
            )
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            state.notice?.let {
                Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (section == "drafts") {
                Text(
                    "Build a programme without a race goal. Draft edits save locally; nothing starts until you review and activate."
                )
                Button(
                    onClick = {
                        model.newDraft {
                            section = "edit"
                            expandedDay =
                                model.state.value.draft
                                    ?.blocks()
                                    ?.firstOrNull()
                                    ?.days()
                                    ?.firstOrNull()
                                    ?.id()
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Build a programme from scratch")
                }
                if (onCreateCustom != null)
                    TextButton(onClick = onCreateCustom, enabled = enabled) {
                        Text("Create custom exercise")
                    }
                if (state.current != null)
                    SemanticCard(Tile.Strength) {
                        Text(
                            "Current strength programme",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text("Your current programming remains active while you prepare a draft.")
                        WhyDetails(state.current!!.key(), "Preserved programme identity")
                        TextButton(onClick = { confirmStop = state.current }, enabled = enabled) {
                            Text("Stop programming")
                        }
                    }
                TextButton(onClick = { model.reload() }, enabled = enabled) {
                    Text("Refresh saved programmes")
                }
                Text("Saved drafts", style = MaterialTheme.typography.titleLarge)
                if (state.drafts.isEmpty())
                    Text(
                        "No drafts on this page. Create one above; your workout history stays independent."
                    )
                state.drafts.forEach { row ->
                    SemanticCard(Tile.Neutral) {
                        Text(
                            row.name().ifBlank { "Untitled programme" },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text("Starts ${friendlyDate(row.start())}")
                        Button(
                            onClick = {
                                model.openDraft(row.id()) {
                                    section = "edit"
                                    expandedDay = null
                                }
                            },
                            enabled = enabled,
                        ) {
                            Text("Open draft")
                        }
                        TextButton(onClick = { delete = row }, enabled = enabled) {
                            Text("Delete draft")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.page > 0)
                        TextButton(onClick = { model.page(-1) }, enabled = enabled) {
                            Text("Previous drafts")
                        }
                    if (state.moreDrafts)
                        TextButton(onClick = { model.page(1) }, enabled = enabled) {
                            Text("More drafts")
                        }
                }
            } else if (section == "preview" && state.preview != null) {
                ProgrammePreviewContent(state.preview!!.programme)
                Button(
                    onClick = { confirmActivate = state.preview },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Review activation")
                }
                OutlinedButton(
                    onClick = {
                        model.dismissPreview()
                        section = "edit"
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Keep editing draft")
                }
            } else if (draft != null) {
                Text(
                    draft.name().ifBlank { "Untitled programme" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${draft.settings().frequency()} sessions/week · starts ${friendlyDate(draft.start())}"
                )
                Text(
                    "Applied edits are saved automatically. This draft does not change active workouts or actual sets."
                )
                if (onCreateCustom != null)
                    TextButton(onClick = onCreateCustom, enabled = enabled) {
                        Text("Create custom exercise")
                    }
                OutlinedButton(onClick = { editHeader = true }, enabled = enabled) {
                    Text("Programme settings")
                }
                draft.blocks().forEachIndexed { blockIndex, block ->
                    SemanticCard(Tile.Strength) {
                        Text(block.name(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${block.weeks()} programme weeks · ${programmeLabel(block.phase().name)}"
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { blockForm = block.id() to block },
                                enabled = enabled,
                            ) {
                                Text("Edit block")
                            }
                            TextButton(
                                onClick = { model.duplicateBlock(block.id()) },
                                enabled = enabled,
                            ) {
                                Text("Duplicate block")
                            }
                            TextButton(
                                onClick = { model.moveBlock(block.id(), blockIndex - 1) },
                                enabled = enabled && blockIndex > 0,
                            ) {
                                Text("Earlier block")
                            }
                            TextButton(
                                onClick = { model.moveBlock(block.id(), blockIndex + 1) },
                                enabled = enabled && blockIndex < draft.blocks().lastIndex,
                            ) {
                                Text("Later block")
                            }
                            TextButton(
                                onClick = { model.deleteBlock(block.id()) },
                                enabled = enabled,
                            ) {
                                Text("Remove block")
                            }
                        }
                        block.days().forEach { day ->
                            HorizontalDivider()
                            TextButton(
                                onClick = {
                                    expandedDay = if (expandedDay == day.id()) null else day.id()
                                },
                                enabled = enabled,
                            ) {
                                Text(
                                    "${day.weekday().getDisplayName(TextStyle.FULL, Locale.UK)} · ${day.title()} · ${day.slots().size} ${if (day.slots().size == 1) "exercise" else "exercises"}"
                                )
                            }
                            if (expandedDay == day.id()) {
                                day.notes()?.let { Text(it) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = { dayForm = block.id() to day },
                                        enabled = enabled,
                                    ) {
                                        Text("Edit workout")
                                    }
                                    TextButton(
                                        onClick = { duplicateDay = block.id() to day },
                                        enabled = enabled,
                                    ) {
                                        Text("Duplicate workout")
                                    }
                                    TextButton(
                                        onClick = { model.deleteDay(block.id(), day.id()) },
                                        enabled = enabled,
                                    ) {
                                        Text("Remove workout")
                                    }
                                }
                                day.slots().forEachIndexed { index, slot ->
                                    Text(
                                        "${index + 1}. ${slot.exercise().name()}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(gymTargetDescription(slot))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = {
                                                slotForm =
                                                    ProgrammeSlotLocation(
                                                        block.id(),
                                                        day.id(),
                                                        slot,
                                                    )
                                            },
                                            enabled = enabled,
                                        ) {
                                            Text("Edit exercise")
                                        }
                                        TextButton(
                                            onClick = {
                                                model.moveSlot(
                                                    block.id(),
                                                    day.id(),
                                                    block.id(),
                                                    day.id(),
                                                    slot.id(),
                                                    index - 1,
                                                )
                                            },
                                            enabled = enabled && index > 0,
                                        ) {
                                            Text("Move up")
                                        }
                                        TextButton(
                                            onClick = {
                                                model.moveSlot(
                                                    block.id(),
                                                    day.id(),
                                                    block.id(),
                                                    day.id(),
                                                    slot.id(),
                                                    index + 1,
                                                )
                                            },
                                            enabled = enabled && index < day.slots().lastIndex,
                                        ) {
                                            Text("Move down")
                                        }
                                        TextButton(
                                            onClick = {
                                                moveSlot =
                                                    ProgrammeSlotLocation(
                                                        block.id(),
                                                        day.id(),
                                                        slot,
                                                    )
                                            },
                                            enabled = enabled,
                                        ) {
                                            Text("Move to workout")
                                        }
                                        TextButton(
                                            onClick = {
                                                model.removeSlot(block.id(), day.id(), slot.id())
                                            },
                                            enabled = enabled,
                                        ) {
                                            Text("Remove exercise")
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        slotForm = ProgrammeSlotLocation(block.id(), day.id(), null)
                                    },
                                    enabled = enabled && day.slots().size < 12,
                                ) {
                                    Text("Add exercise")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { dayForm = block.id() to null },
                            enabled = enabled && block.days().size < 6,
                        ) {
                            Text("Add workout")
                        }
                    }
                }
                OutlinedButton(
                    onClick = { blockForm = null to null },
                    enabled = enabled && draft.blocks().size < 12,
                ) {
                    Text("Add training block")
                }
                Button(
                    onClick = { model.preview { section = "preview" } },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Preview programme")
                }
                TextButton(
                    onClick = { model.duplicateDraft { expandedDay = null } },
                    enabled = enabled,
                ) {
                    Text("Duplicate as a new programme draft")
                }
                TextButton(onClick = { model.reload() }, enabled = enabled) {
                    Text("Reload saved draft")
                }
            } else {
                Text("Open a saved draft to continue. Nothing has been activated.")
                TextButton(onClick = { section = "drafts" }) { Text("View drafts") }
            }
        }
    }
    if (editHeader && draft != null)
        ProgrammeSettingsDialog(draft, state, { editHeader = false }) { name, start, settings ->
            model.header(name, start, settings) { editHeader = false }
        }
    blockForm?.let { form ->
        ProgrammeBlockDialog(form.second, state, { blockForm = null }) { name, phase, weeks ->
            model.block(form.first, name, phase, weeks) { blockForm = null }
        }
    }
    dayForm?.let { (blockId, day) ->
        ProgrammeDayDialog(day, draft!!.settings().days(), state, { dayForm = null }) {
            title,
            weekday,
            notes ->
            model.day(blockId, day?.id(), title, weekday, notes) { dayForm = null }
        }
    }
    slotForm?.let { location ->
        GymSlotEditor(
            location.slot,
            state.exercises,
            state.favourites,
            state.recent,
            state.archived,
            state.excluded,
            state.error,
            allowOpen = false,
            onDismiss = {
                if (!state.busy) {
                    slotForm = null
                    model.clearMessage()
                }
            },
            onSave = { exercise, input ->
                model.putSlot(location.block, location.day, location.slot?.id(), exercise, input) {
                    slotForm = null
                }
            },
        )
    }
    moveSlot?.let { location ->
        ProgrammeChoiceDialog("Move exercise to", state, { moveSlot = null }) {
            draft!!.blocks().forEach { block ->
                block
                    .days()
                    .filterNot { it.id() == location.day }
                    .forEach { day ->
                        TextButton(
                            onClick = {
                                model.moveSlot(
                                    location.block,
                                    location.day,
                                    block.id(),
                                    day.id(),
                                    location.slot!!.id(),
                                    day.slots().size,
                                )
                                moveSlot = null
                            },
                            enabled = enabled,
                        ) {
                            Text("${block.name()} · ${day.title()}")
                        }
                    }
            }
            Text(
                "Other workout targets are unchanged. Duplicate exercises must be combined before previewing."
            )
        }
    }
    duplicateDay?.let { (blockId, day) ->
        ProgrammeChoiceDialog("Duplicate workout onto", state, { duplicateDay = null }) {
            val used =
                draft!!.blocks().first { it.id() == blockId }.days().map { it.weekday() }.toSet()
            val free = draft.settings().days().filterNot { it in used }
            if (free.isEmpty()) Text("Choose another available day in Programme settings first.")
            free.forEach { weekday ->
                TextButton(
                    onClick = {
                        model.duplicateDay(blockId, day.id(), weekday)
                        duplicateDay = null
                    },
                    enabled = enabled,
                ) {
                    Text(weekday.getDisplayName(TextStyle.FULL, Locale.UK))
                }
            }
        }
    }
    delete?.let { row ->
        ProgrammeConfirmation(
            "Delete this draft?",
            "${row.name().ifBlank { "Untitled programme" }} will be removed. Active programming, historical prescriptions and actual sets remain.",
            state,
            "Delete draft",
            { delete = null },
        ) {
            model.deleteDraft(row.id(), row.revision())
            delete = null
        }
    }
    confirmStop?.let { reference ->
        ProgrammeConfirmation(
            "Stop current programming?",
            "Previous programme versions, actual sets and PB evidence remain. No workouts are deleted.",
            state,
            "Stop programming",
            { confirmStop = null },
        ) {
            model.stopCurrent(reference) {
                confirmStop = null
                onActivated()
            }
        }
    }
    confirmActivate?.let { review ->
        ProgrammeConfirmation(
            "Activate this programme?",
            "${review.programme.name()} starts ${friendlyDate(review.programme.start())}. ${if (review.before == null) "It becomes your active strength programme." else "It replaces the current programme immediately, including when its first workout is in the future. Previous versions and actual workouts remain preserved."} Targets are intentions, not completed sets.",
            state,
            "Activate programme",
            { confirmActivate = null },
        ) {
            model.activate(review) {
                confirmActivate = null
                onActivated()
            }
        }
    }
}

private data class ProgrammeSlotLocation(
    val block: String,
    val day: String,
    val slot: GymWorkoutDefinition.Slot?,
)

private val PROGRAMME_DATE =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.UK).withResolverStyle(ResolverStyle.STRICT)

internal fun programmeLabel(value: String) =
    when (value) {
        "SUPPORT_RUNNING" -> "Support my running"
        "STRENGTH" -> "Get stronger"
        "MUSCLE" -> "Build muscle"
        "POWER" -> "Power / athletic performance"
        "GENERAL" -> "General strength & fitness"
        else -> value.lowercase(Locale.UK).replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

@Composable
private fun ProgrammeSettingsDialog(
    draft: StrengthProgrammeDraft,
    state: ProgrammeAuthoringState,
    close: () -> Unit,
    save: (String, LocalDate, StrengthProgramme.Settings) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(draft.name()) }
    var date by rememberSaveable { mutableStateOf(draft.start().format(PROGRAMME_DATE)) }
    var goal by rememberSaveable { mutableStateOf(draft.settings().goal().name) }
    var priority by rememberSaveable { mutableStateOf(draft.settings().priority().name) }
    var experience by rememberSaveable { mutableStateOf(draft.settings().experience().name) }
    var frequency by rememberSaveable { mutableStateOf(draft.settings().frequency()) }
    var minutes by rememberSaveable { mutableStateOf(draft.settings().sessionMinutes().toString()) }
    var days by rememberSaveable { mutableStateOf(draft.settings().days().map { it.value }) }
    var equipment by rememberSaveable {
        mutableStateOf(draft.settings().equipment().map { it.name })
    }
    var showEquipment by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ProgrammeFormDialog(
        "Programme settings",
        state,
        close,
        {
            try {
                save(
                    name,
                    LocalDate.parse(date, PROGRAMME_DATE),
                    StrengthProgramme.Settings(
                        GymProgramme.Goal.valueOf(goal),
                        GymProgramme.Priority.valueOf(priority),
                        GymProgramme.Experience.valueOf(experience),
                        days.map { DayOfWeek.of(it) }.toSet(),
                        frequency,
                        minutes.toIntOrNull()
                            ?: throw IllegalArgumentException("Enter a whole session length."),
                        equipment.map { ExerciseMetadata.Equipment.valueOf(it) }.toSet(),
                    ),
                )
            } catch (failure: Exception) {
                error = failure.message ?: "Review programme settings."
            }
        },
    ) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        ProgrammeField(name, { name = it }, "Programme name")
        ProgrammeField(date, { date = it }, "Start date · dd/mm/yyyy")
        Text("Goal")
        ProgrammeChips(GymProgramme.Goal.values().map { it.name }, goal) { goal = it }
        Text("Priority")
        ProgrammeChips(GymProgramme.Priority.values().map { it.name }, priority) { priority = it }
        Text("Experience")
        ProgrammeChips(GymProgramme.Experience.values().map { it.name }, experience) {
            experience = it
        }
        Text("Workouts per programme week")
        ProgrammeChips((1..6).map { it.toString() }, frequency.toString()) {
            frequency = it.toInt()
        }
        ProgrammeField(
            minutes,
            { minutes = it },
            "Usual session length · minutes",
            KeyboardType.Number,
        )
        Text("Available days")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.values().forEach { day ->
                FilterChip(
                    day.value in days,
                    { days = if (day.value in days) days - day.value else days + day.value },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.UK)) },
                )
            }
        }
        TextButton(onClick = { showEquipment = !showEquipment }) {
            Text("Equipment · ${equipment.size} capabilities")
        }
        if (showEquipment) {
            Text("Select complete setups. Bodyweight movements need no equipment checkbox.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ExerciseMetadata.Equipment.values().forEach { item ->
                    FilterChip(
                        item.name in equipment,
                        {
                            equipment =
                                if (item.name in equipment) equipment - item.name
                                else equipment + item.name
                        },
                        label = { Text(programmeLabel(item.name)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgrammeBlockDialog(
    block: StrengthProgrammeDraft.WeekBlock?,
    state: ProgrammeAuthoringState,
    close: () -> Unit,
    save: (String, StrengthProgramme.Phase, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(block?.name() ?: "Development") }
    var phase by rememberSaveable { mutableStateOf(block?.phase()?.name ?: "DEVELOPMENT") }
    var weeks by rememberSaveable { mutableStateOf(block?.weeks()?.toString() ?: "4") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ProgrammeFormDialog(
        "Training block",
        state,
        close,
        {
            val value = weeks.toIntOrNull()
            if (value == null) error = "Enter a whole block duration."
            else save(name, StrengthProgramme.Phase.valueOf(phase), value)
        },
    ) {
        error?.let { Text(it) }
        ProgrammeField(name, { name = it }, "Block name")
        Text("Block emphasis")
        ProgrammeChips(StrengthProgramme.Phase.values().map { it.name }, phase) { phase = it }
        ProgrammeField(weeks, { weeks = it }, "Block duration · weeks", KeyboardType.Number)
        Text(
            "Block labels do not change your targets automatically. Review each workout prescription."
        )
    }
}

@Composable
private fun ProgrammeDayDialog(
    day: StrengthProgrammeDraft.Day?,
    available: Set<DayOfWeek>,
    state: ProgrammeAuthoringState,
    close: () -> Unit,
    save: (String, DayOfWeek, String?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(day?.title() ?: "New workout") }
    var weekday by rememberSaveable {
        mutableStateOf(day?.weekday()?.name ?: available.sortedBy { it.value }.first().name)
    }
    var notes by rememberSaveable { mutableStateOf(day?.notes() ?: "") }
    ProgrammeFormDialog(
        "Workout details",
        state,
        close,
        { save(title, DayOfWeek.valueOf(weekday), notes) },
    ) {
        ProgrammeField(title, { title = it }, "Workout name")
        Text("Training day")
        ProgrammeChips(available.sortedBy { it.value }.map { it.name }, weekday) { weekday = it }
        ProgrammeField(notes, { notes = it }, "Workout notes · optional")
        Text("Choose distinct available days. Preview checks the chosen weekly frequency.")
    }
}

@Composable
private fun ProgrammeField(
    value: String,
    change: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value,
        { change(it.take(2000)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@Composable
private fun ProgrammeChips(options: List<String>, selected: String, choose: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(
                option == selected,
                { choose(option) },
                label = { Text(programmeLabel(option)) },
            )
        }
    }
}

@Composable
private fun ProgrammeFormDialog(
    title: String,
    state: ProgrammeAuthoringState,
    close: () -> Unit,
    save: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) close() },
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = save, enabled = !state.busy) { Text("Save locally") }
        },
        dismissButton = { TextButton(onClick = close, enabled = !state.busy) { Text("Cancel") } },
    )
}

@Composable
private fun ProgrammeChoiceDialog(
    title: String,
    state: ProgrammeAuthoringState,
    close: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) close() },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                state.error?.let { Text(it) }
                content()
            }
        },
        confirmButton = { TextButton(onClick = close, enabled = !state.busy) { Text("Done") } },
    )
}

@Composable
private fun ProgrammeConfirmation(
    title: String,
    text: String,
    state: ProgrammeAuthoringState,
    action: String,
    close: () -> Unit,
    accept: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) close() },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = accept, enabled = !state.busy) { Text(action) } },
        dismissButton = {
            TextButton(onClick = close, enabled = !state.busy) { Text("Keep current") }
        },
    )
}

@Composable
internal fun ProgrammePreviewContent(programme: StrengthProgramme) {
    Text(programme.name(), style = MaterialTheme.typography.headlineSmall)
    Text(
        "${programme.settings().frequency()} sessions/week · ${programme.blocks().sumOf { it.weeks() }} weeks · ${programmeLabel(programme.settings().goal().name)}"
    )
    Text("Not activated. Review targets and trade-offs before choosing to use this programme.")
    if (programme.tradeoffs().isNotEmpty())
        WhyDetails(programme.tradeoffs().joinToString("\n"), "Programme trade-offs")
    programme.blocks().forEach { block ->
        SemanticCard(Tile.Strength) {
            Text(block.name(), style = MaterialTheme.typography.titleLarge)
            Text("${friendlyDate(block.start())} – ${friendlyDate(block.end())}")
            WhyDetails(block.explanation(), "Block emphasis")
            // Concrete dated prescriptions are shown as retained, never reconstructed from the
            // first week.
            var limit by remember(block.id()) { mutableIntStateOf(7) }
            val chronological = remember(block) { block.workouts().sortedBy { it.date() } }
            chronological.take(limit).forEach { workout ->
                Text(
                    "${friendlyDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min",
                    style = MaterialTheme.typography.titleMedium,
                )
                workout.definition().slots().forEach { slot ->
                    Text("${slot.exercise().name()} · ${gymTargetDescription(slot)}")
                }
                workout.notes()?.let { Text(it) }
            }
            if (limit < block.workouts().size)
                TextButton(onClick = { limit += 7 }) { Text("Show more dated workouts") }
        }
    }
}
