@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.*
import java.time.format.*
import java.util.*
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*

/** Template selection, configuration and exact dated previews share one durable authoring path. */
@Composable
fun StrengthGenerationScreen(
    model: StrengthGenerationViewModel,
    onBack: () -> Unit,
    onActivated: () -> Unit,
    initialDraftId: String? = null,
) {
    val state by model.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf("library") }
    var templateId by rememberSaveable { mutableStateOf<String?>(null) }
    var workoutId by rememberSaveable { mutableStateOf<String?>(null) }
    var slotEdit by remember { mutableStateOf<Pair<String, GymWorkoutDefinition.Slot?>?>(null) }
    var confirmation by remember {
        mutableStateOf<Pair<ConcreteStrengthProgrammeDraft, String>?>(null)
    }
    var deletion by remember {
        mutableStateOf<ConcreteStrengthProgrammeDraftRepository.Summary?>(null)
    }
    val enabled = !state.busy && !state.loading
    fun back() {
        if (!enabled) return
        when {
            section == "preview" && workoutId != null -> workoutId = null
            section == "preview" -> {
                model.close()
                if (initialDraftId != null) onBack() else section = "library"
            }
            section == "configure" -> section = "library"
            else -> onBack()
        }
    }
    BackHandler { back() }
    LaunchedEffect(state.loading, initialDraftId) {
        if (!state.loading && initialDraftId != null && state.draft?.id() != initialDraftId) {
            model.open(initialDraftId) { section = "preview" }
        }
    }
    LaunchedEffect(state.loading) {
        if (!state.loading && state.draft != null && section == "library") section = "preview"
    }
    LaunchedEffect(state.loading, state.draft?.id()) {
        if (!state.loading && section == "preview" && state.draft == null) section = "library"
    }
    key(section, state.draft?.id(), workoutId) {
        CoachPage {
            TextButton(onClick = ::back, enabled = enabled) {
                Text(if (workoutId != null) "Programme preview" else "Back")
            }
            CoachHeading(
                when (section) {
                    "configure" -> "Shape your programme"
                    "preview" -> if (workoutId == null) "Review every block" else "Dated workout"
                    else -> "Templates & generation"
                }
            )
            if (state.busy || state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
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
            if (section == "library") {
                Text(
                    "Choose a template or generate from your preferences. Review before activation."
                )
                Button(
                    onClick = {
                        if (state.draft != null) model.close()
                        templateId = null
                        section = "configure"
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Generate from my preferences")
                }
                OutlinedButton(onClick = model::openHistory, enabled = enabled) {
                    Text("Programme revision history")
                }
                if (state.current?.owner() == StrengthProgrammingRepository.Owner.AUTHORED)
                    SemanticCard(Tile.Strength) {
                        Text("Current programme", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Prepare edits or regenerate future workouts. Nothing changes until a new revision is reviewed and activated."
                        )
                        Button(
                            onClick = {
                                model.prepareFuture {
                                    workoutId = null
                                    section = "preview"
                                }
                            },
                            enabled = enabled,
                        ) {
                            Text("Review future programme changes")
                        }
                    }
                Text("Editable starting points", style = MaterialTheme.typography.titleLarge)
                StrengthProgrammeTemplates.all().forEach { template ->
                    SemanticCard(Tile.Strength) {
                        Text(template.name(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${template.frequency()} days · about ${template.suggestedMinutes()} min per session"
                        )
                        Text(template.intendedUser())
                        WhyDetails(
                            "${template.emphasis()}\n${template.tradeoff()}",
                            "Emphasis & trade-offs",
                        )
                        OutlinedButton(
                            onClick = {
                                if (state.draft != null) model.close()
                                templateId = template.id()
                                section = "configure"
                            },
                            enabled = enabled,
                        ) {
                            Text("Configure ${template.name()}")
                        }
                    }
                }
                Text("Saved concrete previews", style = MaterialTheme.typography.titleLarge)
                if (state.saved.isEmpty())
                    Text(
                        "Generated previews save locally here. Your manual drafts remain in the programme builder."
                    )
                state.saved.forEach { row ->
                    SemanticCard(Tile.Neutral) {
                        Text(row.name(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Programme revision ${row.programmeRevision()} · draft edit ${row.revision()}"
                        )
                        Text(
                            "${friendlyDate(row.start())} – ${friendlyDate(row.end())}${if (row.revisionExists()) " · revision preserved" else " · not active"}"
                        )
                        TextButton(
                            onClick = {
                                model.open(row.id()) {
                                    workoutId = null
                                    section = "preview"
                                }
                            },
                            enabled = enabled,
                        ) {
                            Text("Open ${row.name()}")
                        }
                        TextButton(onClick = { deletion = row }, enabled = enabled) {
                            Text("Delete preview · ${row.name()}")
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    if (state.page > 0)
                        TextButton(onClick = { model.page(-1) }, enabled = enabled) {
                            Text("Previous previews")
                        }
                    if (state.more)
                        TextButton(onClick = { model.page(1) }, enabled = enabled) {
                            Text("More previews")
                        }
                }
                TextButton(onClick = { model.reload() }, enabled = enabled) {
                    Text("Refresh saved previews")
                }
            } else if (section == "configure") {
                GenerationConfiguration(
                    templateId,
                    enabled,
                    state.draft?.programme(),
                    state.error,
                ) { name, start, weeks, settings, recovery ->
                    val compatibleTemplate =
                        templateId?.takeIf {
                            StrengthProgrammeTemplates.require(it).frequency() ==
                                settings.frequency()
                        }
                    model.generate(name, start, weeks, settings, compatibleTemplate, recovery) {
                        workoutId = null
                        section = "preview"
                    }
                }
            } else
                state.draft?.let { draft ->
                    val programme = draft.programme()
                    val workout = programme.workouts().firstOrNull { it.id() == workoutId }
                    if (workout == null) {
                        Text(programme.name(), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${programme.settings().frequency()} days per week · ${programmeLabel(programme.settings().goal().name)} · ${programmeLabel(programme.settings().experience().name)}"
                        )
                        Text(
                            "${friendlyDate(programme.start())} – ${friendlyDate(programme.end())}"
                        )
                        if (programme.parentRevision() > 0)
                            Text(
                                "${if (state.accepted) "Accepted" else if (state.superseded) "Unaccepted" else "Proposed"} revision ${programme.revision()} · changes from ${friendlyDate(programme.effectiveFrom())}. Earlier and completed workouts remain read only."
                            )
                        if (state.superseded)
                            Text(
                                "Another proposal was accepted for this revision. This unaccepted preview remains read only. Prepare new changes from the current programme."
                            )
                        else if (state.accepted)
                            Text(
                                "Accepted prescription · read only. Removing this preview does not remove history."
                            )
                        else {
                            Text("Edit dated workouts below. Activation is your choice.")
                            Button(
                                onClick = { confirmation = draft to UUID.randomUUID().toString() },
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Review activation")
                            }
                            TextButton(
                                onClick = {
                                    templateId = programme.templateId()
                                    section = "configure"
                                },
                                enabled = enabled,
                            ) {
                                Text(
                                    if (programme.parentRevision() > 0)
                                        "Regenerate future programme"
                                    else "Generate another preview"
                                )
                            }
                        }
                        WhyDetails(programme.tradeoffs().joinToString("\n"), "Why this programme?")
                        programme.blocks().forEach { block ->
                            SemanticCard(Tile.Strength) {
                                Text(
                                    block.name().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    "${block.weeks()} weeks · ${friendlyDate(block.start())} – ${friendlyDate(block.end())}"
                                )
                                WhyDetails(block.explanation(), "Block emphasis")
                                var limit by remember(block.id()) { mutableIntStateOf(6) }
                                block
                                    .workouts()
                                    .sortedBy { it.date() }
                                    .take(limit)
                                    .forEach { row ->
                                        TextButton(
                                            onClick = { workoutId = row.id() },
                                            enabled = enabled,
                                        ) {
                                            Text(
                                                "${friendlyDate(row.date())} · ${row.definition().title()} · about ${row.expectedMinutes()} min"
                                            )
                                        }
                                    }
                                if (limit < block.workouts().size)
                                    TextButton(onClick = { limit += 6 }) {
                                        Text("Show more dated workouts")
                                    }
                            }
                        }
                    } else {
                        Text(
                            workout.definition().title(),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            "${friendlyDate(workout.date())} · about ${workout.expectedMinutes()} min"
                        )
                        val editable =
                            !state.accepted &&
                                !state.superseded &&
                                (programme.parentRevision() == 0 ||
                                    (!workout.date().isBefore(programme.effectiveFrom()) &&
                                        workout.id() !in state.protectedWorkoutIds))
                        if (!editable)
                            Text(
                                "Preserved earlier, completed, active or accepted prescription · read only"
                            )
                        workout.notes()?.let { WhyDetails(it, "Workout guidance") }
                        workout.definition().slots().forEachIndexed { index, slot ->
                            SemanticCard(Tile.Strength) {
                                Text(
                                    "${index + 1}. ${slot.exercise().name()}",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(gymTargetDescription(slot))
                                if (editable) {
                                    val retained =
                                        state.baseProgramme?.let {
                                            ConcreteStrengthProgrammeEditor.retainedIntent(
                                                draft,
                                                it,
                                                workout.id(),
                                                slot.id(),
                                            )
                                        } == true
                                    if (programme.parentRevision() > 0 && retained)
                                        OutlinedButton(
                                            onClick = {
                                                model.openProgrammeSwap(workout.id(), slot.id())
                                            },
                                            enabled = enabled,
                                        ) {
                                            Text("Swap ${slot.exercise().name()} in programme")
                                        }
                                    TextButton(
                                        onClick = {
                                            model.reviewProgression(workout.id(), slot.id())
                                        },
                                        enabled = enabled,
                                    ) {
                                        Text("Review progression for ${slot.exercise().name()}")
                                    }
                                    OutlinedButton(
                                        onClick = { slotEdit = workout.id() to slot },
                                        enabled = enabled,
                                    ) {
                                        Text("Edit ${slot.exercise().name()}")
                                    }
                                    FlowRow(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(CoachSpace.Chip),
                                        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                    ) {
                                        if (index > 0)
                                            TextButton(
                                                onClick = {
                                                    model.reorder(workout.id(), slot.id(), -1)
                                                },
                                                enabled = enabled,
                                            ) {
                                                Text("Move ${slot.exercise().name()} up")
                                            }
                                        if (index < workout.definition().slots().lastIndex)
                                            TextButton(
                                                onClick = {
                                                    model.reorder(workout.id(), slot.id(), 1)
                                                },
                                                enabled = enabled,
                                            ) {
                                                Text("Move ${slot.exercise().name()} down")
                                            }
                                        if (workout.definition().slots().size > 1)
                                            TextButton(
                                                onClick = {
                                                    model.removeSlot(workout.id(), slot.id())
                                                },
                                                enabled = enabled,
                                            ) {
                                                Text("Remove ${slot.exercise().name()}")
                                            }
                                    }
                                }
                            }
                        }
                        if (editable && workout.definition().slots().size < 12)
                            OutlinedButton(
                                onClick = { slotEdit = workout.id() to null },
                                enabled = enabled,
                            ) {
                                Text("Add exercise to dated workout")
                            }
                        Text(
                            "Only this dated workout changes. Other weeks, active programming and actual sets remain separate."
                        )
                    }
                }
        }
    }
    ProgrammeSwapSheet(model)
    ProgrammeProgressionSheet(model)
    ProgrammeHistorySheet(model)
    slotEdit?.let { (workout, slot) ->
        GymSlotEditor(
            slot,
            state.exercises,
            state.favourites,
            emptyList(),
            state.archived,
            state.excluded,
            state.error,
            allowOpen = false,
            allowExerciseChange =
                state.draft?.programme()?.parentRevision() == 0 ||
                    slot == null ||
                    state.baseProgramme?.let {
                        ConcreteStrengthProgrammeEditor.retainedIntent(
                            state.draft!!,
                            it,
                            workout,
                            slot.id(),
                        )
                    } != true,
            onDismiss = {
                if (enabled) {
                    slotEdit = null
                    model.clearMessage()
                }
            },
            onSave = { exercise, input ->
                model.putSlot(workout, slot?.id(), exercise, input) { slotEdit = null }
            },
        )
    }
    confirmation?.let { (reviewed, decisionId) ->
        AlertDialog(
            onDismissRequest = { if (enabled) confirmation = null },
            title = { Text("Activate this programme?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        if (reviewed.programme().parentRevision() > 0)
                            "${reviewed.programme().name()}: revision ${reviewed.programme().revision()} changes future programming from ${friendlyDate(reviewed.programme().effectiveFrom())}. Earlier and completed prescriptions, actual sets and PB evidence remain. Nothing is completed automatically."
                        else
                            "${reviewed.programme().name()} starts ${friendlyDate(reviewed.programme().start())}. Activation replaces current programming immediately, even if the start date is in the future. Historical versions, entered sets and PBs remain. Nothing is completed automatically."
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.activate(reviewed, decisionId) {
                            confirmation = null
                            onActivated()
                        }
                    },
                    enabled = enabled,
                ) {
                    Text("Activate programme")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }, enabled = enabled) {
                    Text("Keep reviewing")
                }
            },
        )
    }
    deletion?.let { row ->
        AlertDialog(
            onDismissRequest = { if (enabled) deletion = null },
            title = { Text("Delete this preview?") },
            text = {
                Text(
                    "${row.name()} will be removed from saved previews. Accepted programming, history and actual sets remain."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.delete(row.id(), row.revision())
                        deletion = null
                    },
                    enabled = enabled,
                ) {
                    Text("Delete preview")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletion = null }, enabled = enabled) { Text("Cancel") }
            },
        )
    }
}

private val GENERATION_DATE =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.UK).withResolverStyle(ResolverStyle.STRICT)

@Composable
private fun ColumnScope.GenerationConfiguration(
    templateId: String?,
    enabled: Boolean,
    initial: StrengthProgramme?,
    externalError: String?,
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
            mutableStateOf((initial?.start() ?: LocalDate.now()).format(GENERATION_DATE))
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
                (initial?.settings()?.days() ?: DayOfWeek.values().toSet()).map { it.value }
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
                Text(it.name(), style = MaterialTheme.typography.titleLarge)
                Text(it.emphasis())
                Text(it.tradeoff())
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
    GenerationChoices(GymProgramme.Goal.values().map { it.name }, goal, enabled) { goal = it }
    Text("Experience")
    GenerationChoices(GymProgramme.Experience.values().map { it.name }, experience, enabled) {
        experience = it
    }
    Text("Priority")
    GenerationChoices(GymProgramme.Priority.values().map { it.name }, priority, enabled) {
        priority = it
    }
    if (template == null || (initial?.parentRevision() ?: 0) > 0) {
        Text("Workouts per programme week")
        GenerationChoices((1..6).map { it.toString() }, frequency.toString(), enabled) {
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
                day.value in days,
                { days = if (day.value in days) days - day.value else days + day.value },
                enabled = enabled,
                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.UK)) },
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
                    label = { Text(programmeLabel(item.name)) },
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
                        days.map { DayOfWeek.of(it) }.toSet(),
                        frequency,
                        minutes.toIntOrNull()
                            ?: throw IllegalArgumentException("Enter a whole session length."),
                        equipment.map { ExerciseMetadata.Equipment.valueOf(it) }.toSet(),
                    )
                generate(
                    name,
                    LocalDate.parse(date, GENERATION_DATE),
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
                label = { Text(programmeLabel(value)) },
            )
        }
    }
}
