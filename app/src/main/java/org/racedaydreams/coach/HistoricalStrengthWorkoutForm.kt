package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.*
import java.util.UUID
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.strength.StrengthMeasurements
import org.racedaydreams.domain.strength.StrengthSession

private typealias ActualSet = StrengthSession.SetEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StrengthForm(
    s: CoachUiState,
    model: CoachViewModel,
    plannedDate: LocalDate?,
    editing: StrengthSession? = null,
    done: () -> Unit,
) {
    val today = LocalDate.now()
    val existingDate = editing?.startedAt()?.atOffset(editing.offset())?.toLocalDate()
    val imperial = s.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val metresPerUnit = if (imperial) 0.9144 else 1.0
    val distanceLabel = if (imperial) "Distance (yards)" else "Distance (metres)"
    val unit = s.massUnit
    var date by rememberSaveable {
        mutableStateOf((existingDate ?: plannedDate?.coerceAtMost(today) ?: today).toString())
    }
    var minutes by rememberSaveable {
        mutableStateOf(
            editing
                ?.takeIf { it.hasRecordedDuration() }
                ?.duration()
                ?.toMinutes()
                ?.toString()
                .orEmpty()
        )
    }
    var rpe by rememberSaveable { mutableStateOf(editing?.rpe()?.toString().orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(editing?.notes().orEmpty()) }
    var sessionDetails by rememberSaveable { mutableStateOf(false) }
    var setDetails by rememberSaveable { mutableStateOf(false) }
    var copied by rememberSaveable { mutableStateOf(false) }
    var confirmReuse by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    var selected by rememberSaveable {
        mutableStateOf(
            editing?.exercises()?.firstOrNull()?.exercise()?.id() ?: s.exercises.first().id()
        )
    }
    // Saveable strings hold the draft only. Every set receives its identity when explicitly added.
    var blocks by rememberSaveable {
        mutableStateOf(
            ArrayList(
                editing?.exercises()?.map { encodeBlock(it.exercise().id(), it.sets()) }.orEmpty()
            )
        )
    }
    var activeIndex by rememberSaveable { mutableIntStateOf(if (editing == null) -1 else 0) }
    var sets by rememberSaveable {
        mutableStateOf(
            ArrayList(editing?.exercises()?.firstOrNull()?.sets()?.map(::encodeSet).orEmpty())
        )
    }

    var editingSet by rememberSaveable { mutableStateOf<Int?>(null) }
    val editorLocation = remember { BringIntoViewRequester() }
    LaunchedEffect(editingSet) { if (editingSet != null) editorLocation.bringIntoView() }
    var load by rememberSaveable { mutableStateOf("") }
    var measure by rememberSaveable { mutableStateOf("") }
    var effort by rememberSaveable { mutableStateOf("") }
    var rir by rememberSaveable {
        mutableStateOf(s.gymProgramme?.settings()?.effort()?.name == "RIR")
    }
    var warmUp by rememberSaveable { mutableStateOf(false) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val exercise =
        editing?.exercises()?.firstOrNull { it.exercise().id() == selected }?.exercise()
            ?: s.exercises.first { it.id() == selected }
    val profile = exercise.measurement()
    val chosenDay = LocalDate.parse(date)
    val previous =
        remember(s.strengthSessions, exercise, chosenDay, editing?.id()) {
            s.strengthSessions
                .asSequence()
                .filter {
                    it.id() != editing?.id() &&
                        it.startedAt().atOffset(it.offset()).toLocalDate() <= chosenDay
                }
                .sortedByDescending { it.startedAt() }
                .mapNotNull { session ->
                    session.exercises().firstOrNull { it.exercise() == exercise }
                }
                .firstOrNull()
        }
    val previousWorking = previous?.sets()?.lastOrNull { !it.warmUp() }
    var enteredUnit by rememberSaveable { mutableStateOf(unit.name) }
    LaunchedEffect(unit) {
        if (enteredUnit != unit.name) {
            load =
                load.toDoubleOrNull()?.let {
                    unit.fromKilograms(MassUnit.valueOf(enteredUnit).toKilograms(it)).toString()
                } ?: load
            enteredUnit = unit.name
        }
    }
    // The model owns persistence errors. Keep the draft available for correction and retry.
    LaunchedEffect(s.error) { if (s.error != null) saving = false }

    fun clearEntry() {
        load = ""
        measure = ""
        effort = ""
        warmUp = false
        setDetails = false
        editingSet = null
        dirty = false
    }

    fun readIntoEntry(set: ActualSet, index: Int? = null) {
        load = if (profile.usesLoad()) unit.fromKilograms(set.kilograms()).toString() else ""
        measure =
            set.durationSeconds()?.toString()
                ?: set.distanceMetres()?.let { (it / metresPerUnit).toString() }
                ?: set.reps().toString()
        warmUp = index != null && set.warmUp()
        effort =
            if (index == null) ""
            else (set.repetitionsInReserve() ?: set.rpe())?.toString().orEmpty()
        if (index != null) rir = set.repetitionsInReserve() != null
        setDetails = warmUp || effort.isNotEmpty()
        editingSet = index
        dirty = index == null
        error = null
    }

    fun draftBlocks() =
        ArrayList(blocks).apply {
            if (activeIndex >= 0) {
                if (sets.isEmpty()) removeAt(activeIndex)
                else this[activeIndex] = encodeBlock(selected, sets.map(::decodeSet))
            } else if (sets.isNotEmpty()) add(encodeBlock(selected, sets.map(::decodeSet)))
        }

    fun switchExercise(next: String, index: Int? = null) {
        if (editingSet != null || dirty) {
            error = "Add, update or clear the current set before switching exercises."
            return
        }
        if (next == selected && (index == null || index == activeIndex)) return
        val refreshed = draftBlocks()
        activeIndex =
            if (index == null) refreshed.indexOfFirst { it.substringBefore('\t') == next }
            else if (activeIndex in 0 until index && sets.isEmpty()) index - 1 else index
        selected = next
        blocks = refreshed
        sets =
            ArrayList(refreshed.getOrNull(activeIndex)?.substringAfter('\t')?.split(';').orEmpty())
        clearEntry()
        copied = false
        error = null
    }

    fun reuse() {
        clearEntry()
        sets =
            ArrayList(
                previous?.sets().orEmpty().map {
                    encodeSet(it.copy(id = UUID.randomUUID().toString()))
                }
            )
        copied = true
        error = null
    }

    CoachCard {
        Text("Completed strength workout", style = MaterialTheme.typography.titleMedium)
        ExercisePicker(s, selected, { switchExercise(it) })
        if (previous != null) {
            Text("Last time", style = MaterialTheme.typography.labelLarge)
            previousWorking?.let {
                Text(
                    StrengthMeasurements.display(
                        it,
                        unit,
                        if (imperial) UnitSystem.IMPERIAL else UnitSystem.METRIC,
                    )
                )
            }
            OutlinedButton(
                onClick = { if (sets.isEmpty() && !dirty) reuse() else confirmReuse = true },
                modifier = Modifier.heightIn(min = CoachSpace.Touch),
            ) {
                Text("Use previous sets")
            }
            if (confirmReuse)
                AlertDialog(
                    onDismissRequest = { confirmReuse = false },
                    title = { Text("Replace these draft sets?") },
                    text = {
                        Text(
                            "Copy the sets from your last workout. Review them against what you did before saving."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmReuse = false
                                reuse()
                            }
                        ) {
                            Text("Replace draft sets")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmReuse = false }) { Text("Cancel") }
                    },
                )
        }
        if (copied)
            Text(
                "Copied as a draft. Keep only the sets you actually did.",
                style = MaterialTheme.typography.bodySmall,
            )

        sets.forEachIndexed { index, line ->
            val set = decodeSet(line)
            if (index > 0) HorizontalDivider()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "${index + 1}: ${StrengthMeasurements.display(set, unit, if (imperial) UnitSystem.IMPERIAL else UnitSystem.METRIC)}" +
                        if (set.warmUp()) " · warm-up" else ""
                )

                TextButton(
                    onClick = { readIntoEntry(set, index) },
                    modifier = Modifier.semantics { contentDescription = "Edit set ${index + 1}" },
                    enabled = editingSet == null && !dirty,
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = { sets = ArrayList(sets.filterIndexed { i, _ -> i != index }) },
                    modifier =
                        Modifier.semantics { contentDescription = "Remove set ${index + 1}" },
                    enabled = editingSet == null && !dirty,
                ) {
                    Text("Remove")
                }
            }
        }
        val weightLabel =
            if (selected == "farmers-carry" || selected == "suitcase-carry") "Load per hand"
            else "Weight"
        val measureLabel =
            when {
                profile.usesReps() -> "Reps"
                profile.usesDuration() -> "Duration (seconds or m:ss)"
                else -> distanceLabel
            }
        BoxWithConstraints(Modifier.fillMaxWidth().bringIntoViewRequester(editorLocation)) {
            val sideBySide =
                profile.usesLoad() && maxWidth >= 280.dp && LocalDensity.current.fontScale < 1.5f
            val weight: @Composable (Modifier) -> Unit = { modifier ->
                SetInput(
                    "$weightLabel (${unit.name.lowercase()})",
                    load,
                    {
                        load = it
                        dirty = true
                    },
                    modifier,
                )
            }
            val amount: @Composable (Modifier) -> Unit = { modifier ->
                SetInput(
                    measureLabel,
                    measure,
                    {
                        measure = it
                        dirty = true
                    },
                    modifier,
                    if (profile.usesDuration()) KeyboardType.Text
                    else if (profile.usesReps()) KeyboardType.Number else KeyboardType.Decimal,
                )
            }
            if (sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weight(Modifier.weight(1f))
                    amount(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (profile.usesLoad()) weight(Modifier.fillMaxWidth())
                    amount(Modifier.fillMaxWidth())
                }
            }
        }
        if (sets.isEmpty() && !dirty && previousWorking != null) {
            TextButton(onClick = { readIntoEntry(previousWorking) }) {
                Text("Use last working set values")
            }
        }
        TextButton(onClick = { setDetails = !setDetails }) {
            Text(if (setDetails) "Hide set details" else "Warm-up and effort (optional)")
        }
        if (setDetails) {
            if (profile.usesReps())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false, true).forEach { useRir ->
                        CoachFilterChip(
                            rir == useRir,
                            {
                                rir = useRir
                                effort = ""
                                dirty = true
                            },
                            { Text(if (useRir) "RIR" else "RPE") },
                        )
                    }
                }
            CoachInput(
                if (rir && profile.usesReps()) "Reps in reserve (optional)"
                else "Set RPE 0–10 (optional)",
                effort,
                {
                    effort = it
                    dirty = true
                },
                true,
            )
            CheckRow(
                "Warm-up set",
                warmUp,
                {
                    warmUp = it
                    dirty = true
                },
            )
        }
        val operation = editingSet
        FilledTonalButton(
            onClick = {
                try {
                    val usingRir = rir && profile.usesReps()
                    val effortValue = effort.takeIf { it.isNotBlank() }?.toDouble()
                    val pending =
                        ActualSet(
                            operation?.let { decodeSet(sets[it]).id() } ?: UUID.randomUUID().toString(),
                            if (profile.usesReps()) measure.toInt() else 0,
                            if (profile.usesLoad()) unit.toKilograms(load.toDouble()) else 0.0,
                            effortValue.takeUnless { usingRir },
                            warmUp,
                            if (profile.usesDuration()) StrengthMeasurements.seconds(measure) else null,
                            if (profile.usesDistance()) measure.toDouble() * metresPerUnit else null,
                            effortValue.takeIf { usingRir },
                        )
                    val revised = sets.map(::decodeSet).toMutableList()
                    if (operation == null) revised.add(pending) else revised[operation] = pending
                    StrengthSession.ExerciseSets(exercise, revised)
                    sets = ArrayList(revised.map(::encodeSet))
                    dirty = false
                    editingSet = null
                    error = null
                } catch (e: IllegalArgumentException) {
                    error = e.message ?: "Check your weight, amount and effort."
                }
            },
            modifier = Modifier.heightIn(min = CoachSpace.Touch),
        ) { Text(if (operation == null) "Add set" else "Update set") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (operation != null || dirty) {
                TextButton(
                    onClick = {
                        clearEntry()
                        error = null
                    }
                ) {
                    Text(if (operation == null) "Clear set entry" else "Cancel set edit")
                }
            } else if (sets.size in 1..99) {
                TextButton(
                    onClick = {
                        sets =
                            ArrayList(
                                sets +
                                    encodeSet(
                                        decodeSet(sets.last())
                                            .copy(id = UUID.randomUUID().toString())
                                    )
                            )
                        error = null
                    }
                ) {
                    Text("Duplicate previous set")
                }
            }
        }

        if (sets.isNotEmpty() && editingSet == null && !dirty)
            ExercisePicker(s, selected, { switchExercise(it) }, "Choose another exercise")

        val otherBlocks = blocks.withIndex().filter { it.index != activeIndex }
        if (otherBlocks.isNotEmpty()) {
            Text("Other exercises in this workout", style = MaterialTheme.typography.titleSmall)
            otherBlocks.forEach { (index, block) ->
                val id = block.substringBefore('\t')
                val name = s.exercises.firstOrNull { it.id() == id }?.name() ?: id
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { switchExercise(id, index) }) { Text("Edit $name") }
                    TextButton(
                        onClick = {
                            blocks = ArrayList(blocks.filterIndexed { i, _ -> i != index })
                            if (activeIndex > index) activeIndex--
                        }
                    ) {
                        Text("Remove $name")
                    }
                }
            }
        }
        DateInput("Session date", date, { date = it }, latest = today)
        TextButton(onClick = { sessionDetails = !sessionDetails }) {
            Text(if (sessionDetails) "Hide session details" else "Session details (optional)")
        }
        if (sessionDetails) {
            CoachInput("Duration (minutes)", minutes, { minutes = it }, true)
            CoachInput("Session RPE 0–10 (optional)", rpe, { rpe = it }, true)
            CoachInput("Notes (optional)", notes, { notes = it })
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                try {
                    require(operation == null && !dirty) {
                        "Add, update or clear the current set before saving."
                    }
                    val exercises =
                        draftBlocks().map { block ->
                            val id = block.substringBefore('\t')
                            val definition =
                                editing
                                    ?.exercises()
                                    ?.firstOrNull { it.exercise().id() == id }
                                    ?.exercise() ?: s.exercises.first { it.id() == id }
                            StrengthSession.ExerciseSets(
                                definition,
                                block.substringAfter('\t').split(';').map(::decodeSet),
                            )
                        }
                    require(exercises.isNotEmpty()) { "Add at least one completed set." }
                    val (start, duration) =
                        historicalStrengthTiming(
                            chosenDay,
                            minutes,
                            editing,
                            Instant.now(),
                            ZoneId.systemDefault(),
                        )
                    val actual =
                        StrengthSession(
                            editing?.id() ?: UUID.randomUUID().toString(),
                            start.toInstant(),
                            start.offset,
                            duration,
                            rpe.takeIf { it.isNotBlank() }?.toDouble(),
                            notes,
                            exercises,
                        )
                    saving = true
                    model.strength.saveStrength(actual, done, plannedDate)
                } catch (e: IllegalArgumentException) {
                    error = e.message ?: "Check workout values."
                }
            },
            enabled = !saving && !s.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save strength workout")
        }
    }
}

@Composable
private fun SetInput(
    label: String,
    text: String,
    change: (String) -> Unit,
    modifier: Modifier,
    keyboard: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = text,
        onValueChange = change,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

internal fun historicalStrengthTiming(
    date: LocalDate,
    minutes: String,
    editing: StrengthSession?,
    now: Instant,
    zone: ZoneId,
): Pair<OffsetDateTime, Duration> {
    require(date <= now.atZone(zone).toLocalDate()) { "Choose today or an earlier session date." }
    val previousDuration = editing?.duration()?.takeIf { editing.hasRecordedDuration() }
    val duration =
        when {
            minutes.isBlank() -> StrengthSession.UNRECORDED_DURATION
            previousDuration?.toMinutes()?.toString() == minutes -> previousDuration
            else ->
                Duration.ofMinutes(
                    minutes.toLong().also {
                        require(it in 1..720) { "Enter 1–720 minutes or leave duration blank." }
                    }
                )
        }
    val original =
        editing?.startedAt()?.atOffset(editing.offset())?.takeIf { it.toLocalDate() == date }
    val start =
        original
            ?: if (date == now.atZone(zone).toLocalDate()) {
                maxOf(date.atStartOfDay(zone).toInstant(), now.minus(duration))
                    .atZone(zone)
                    .toOffsetDateTime()
            } else date.atTime(12, 0).atZone(zone).toOffsetDateTime()
    require(start.toInstant().plus(duration) <= now) {
        "The session duration must fit before now on the selected day."
    }
    return start to duration
}

private fun encodeBlock(exercise: String, sets: List<ActualSet>) =
    "$exercise\t${sets.joinToString(";", transform = ::encodeSet)}"

private fun encodeSet(set: ActualSet) =
    listOf(
            set.id(),
            set.reps(),
            set.kilograms(),
            set.rpe(),
            set.warmUp(),
            set.durationSeconds(),
            set.distanceMetres(),
            set.repetitionsInReserve(),
        )
        .joinToString("|") { it?.toString().orEmpty() }

private fun decodeSet(line: String): ActualSet {
    val fields = line.split('|')
    return ActualSet(
        fields[0],
        fields[1].toInt(),
        fields[2].toDouble(),
        fields[3].toDoubleOrNull(),
        fields[4].toBooleanStrict(),
        fields[5].toLongOrNull(),
        fields[6].toDoubleOrNull(),
        fields[7].toDoubleOrNull(),
    )
}
