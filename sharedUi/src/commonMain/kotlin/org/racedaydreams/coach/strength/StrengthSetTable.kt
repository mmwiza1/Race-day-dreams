package org.racedaydreams.coach.strength

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.coach.activityNumberStyle
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.presentation.MassText
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

private typealias ActualSet = StrengthSession.SetEntry

enum class StrengthRowState(val label: String) {
    DRAFT("Draft"),
    SAVED("Saved"),
    PREVIOUS("Previous"),
}

private class SetColumns(exercise: StrengthExercise, val mass: MassUnit, val yards: Boolean) {
    private val profile = exercise.measurement()
    val loadLabel =
        if (!profile.usesLoad()) null
        else if (exercise.id() == "farmers-carry" || exercise.id() == "suitcase-carry")
            "Per hand (${MassText.symbol(mass)})"
        else "Load (${MassText.symbol(mass)})"
    val amountLabel =
        when {
            profile.usesDuration() -> "Time"
            profile.usesDistance() -> if (yards) "Distance (yd)" else "Distance (m)"
            else -> "Reps"
        }

    fun load(set: ActualSet) = MassText.number(set.kilograms(), mass)

    fun amount(set: ActualSet): String {
        val seconds = set.durationSeconds()
        val metres = set.distanceMetres()
        return when {
            seconds != null -> StrengthPrescriptionText.duration(seconds)
            metres != null ->
                StrengthPrescriptionText.number(if (yards) metres / 0.9144 else metres)
            else -> set.reps().toString()
        }
    }

    fun previous(set: ActualSet): String =
        listOfNotNull(
                MassText.label(set.kilograms(), mass).takeIf { loadLabel != null },
                if (profile.usesReps()) "${set.reps()} reps"
                else if (profile.usesDistance()) "${amount(set)} ${if (yards) "yd" else "m"}"
                else amount(set),
            )
            .joinToString(" · ")
}

@Composable
fun StrengthSetTable(
    exercise: StrengthExercise,
    mass: MassUnit,
    sets: List<ActualSet>,
    state: StrengthRowState,
    modifier: Modifier = Modifier,
    previous: List<ActualSet> = emptyList(),
    yards: Boolean = false,
    editing: Int? = null,
    onEdit: ((Int) -> Unit)? = null,
    actions: (@Composable (Int) -> Unit)? = null,
) {
    if (sets.isEmpty()) return
    val columns = SetColumns(exercise, mass, yards)
    val previousWorking = previous.filterNot { it.warmUp() }
    val colours = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val table = maxWidth >= 254.dp && LocalDensity.current.fontScale < 1.5f
        Surface(shape = MaterialTheme.shapes.small, color = colours.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth()) {
                if (table)
                    Surface(color = colours.surfaceContainerHighest) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TableLabel("Set", Modifier.width(24.dp))
                            if (previousWorking.isNotEmpty())
                                TableLabel("Previous", Modifier.weight(1.2f))
                            columns.loadLabel?.let { TableLabel(it, Modifier.weight(1f)) }
                            TableLabel(columns.amountLabel, Modifier.weight(1f))
                            TableLabel("State", Modifier.width(40.dp))
                            if (actions != null) Spacer(Modifier.width(CoachSpace.Touch))
                        }
                    }
                var workingIndex = 0
                sets.forEachIndexed { index, set ->
                    if (index > 0) HorizontalDivider(color = colours.outlineVariant)
                    val prior =
                        if (set.warmUp()) null
                        else previousWorking.getOrNull(workingIndex++)?.let(columns::previous)
                    val number = index + 1
                    val stateLabel = if (editing == index) "Editing" else state.label
                    val action =
                        if (onEdit != null) Modifier.clickable(role = Role.Button) { onEdit(index) }
                        else Modifier
                    val label = if (onEdit != null) "Edit set $number" else "Set $number"
                    Surface(
                        color =
                            if (editing == index) colours.surfaceContainerHigh
                            else colours.surfaceContainerLow
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = CoachSpace.Touch),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val figure =
                                    Modifier.weight(1f)
                                        .then(action)
                                        .testTag("strength-set-${state.name.lowercase()}-$number")
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = label
                                            stateDescription = stateLabel
                                        }
                                        .heightIn(min = CoachSpace.Touch)
                                        .padding(vertical = 8.dp)
                                if (table) {
                                    Row(
                                        figure,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            number.toString(),
                                            Modifier.width(24.dp),
                                            style = activityNumberStyle(),
                                        )
                                        if (previousWorking.isNotEmpty())
                                            TableLabel(prior ?: "—", Modifier.weight(1.2f))
                                        columns.loadLabel?.let {
                                            ValueCell(it, columns.load(set), Modifier.weight(1f))
                                        }
                                        ValueCell(
                                            columns.amountLabel,
                                            columns.amount(set),
                                            Modifier.weight(1f),
                                        )
                                        if (state == StrengthRowState.SAVED)
                                            Text(
                                                "✓",
                                                Modifier.width(40.dp).clearAndSetSemantics {},
                                                color = colours.primary,
                                            )
                                        else TableLabel(stateLabel, Modifier.width(40.dp))
                                    }
                                } else {
                                    Column(
                                        figure,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text("Set $number", style = activityNumberStyle())
                                        prior?.let { TableLabel("Previous · $it") }
                                        columns.loadLabel?.let {
                                            ValueCell(
                                                it,
                                                columns.load(set),
                                                Modifier.fillMaxWidth(),
                                                showLabel = true,
                                            )
                                        }
                                        ValueCell(
                                            columns.amountLabel,
                                            columns.amount(set),
                                            Modifier.fillMaxWidth(),
                                            showLabel = true,
                                        )
                                        TableLabel(stateLabel)
                                    }
                                }
                                actions?.invoke(index)
                            }
                            val effort =
                                set.repetitionsInReserve()?.let {
                                    "RIR ${StrengthPrescriptionText.number(it)}"
                                } ?: set.rpe()?.let { "RPE ${StrengthPrescriptionText.number(it)}" }
                            if (set.warmUp() || effort != null)
                                TableLabel(
                                    listOfNotNull("Warm-up".takeIf { set.warmUp() }, effort)
                                        .joinToString(" · "),
                                    Modifier.padding(bottom = 8.dp),
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ValueCell(
    label: String,
    value: String,
    modifier: Modifier,
    showLabel: Boolean = false,
) {
    Column(modifier.semantics { contentDescription = "$label $value" }) {
        if (showLabel) TableLabel(label, Modifier.clearAndSetSemantics {})
        Text(value, style = activityNumberStyle(), modifier = Modifier.clearAndSetSemantics {})
    }
}

/** Compact editable value with a persistent label, including when its keyboard is open. */
@Composable
fun StrengthEntryField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colours = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        cursorBrush = SolidColor(colours.primary),
        textStyle =
            activityNumberStyle()
                .copy(
                    color =
                        if (focused) colours.onPrimaryContainer
                        else if (enabled) colours.onSurface else colours.onSurfaceVariant
                ),
        modifier =
            modifier
                .heightIn(min = CoachSpace.Touch)
                .onFocusChanged { focused = it.isFocused }
                .semantics(mergeDescendants = true) {},
        decorationBox = { field ->
            Surface(
                color = if (focused) colours.primaryContainer else colours.surfaceContainerHigh,
                contentColor = if (focused) colours.onPrimaryContainer else colours.onSurface,
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(min = CoachSpace.Touch)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Box {
                        if (value.isEmpty() && !focused)
                            Text(
                                "—",
                                Modifier.clearAndSetSemantics {},
                                style = activityNumberStyle(),
                            )
                        field()
                    }
                }
            }
        },
    )
}
