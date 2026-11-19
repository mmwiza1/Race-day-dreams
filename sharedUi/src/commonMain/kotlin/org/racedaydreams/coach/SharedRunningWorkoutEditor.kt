@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step
import org.racedaydreams.domain.polish.CustomWorkout

@Composable
fun SharedRunningWorkoutEditor(
    draftId: String,
    initial: CustomWorkout?,
    start: ((CustomWorkout) -> Unit)?,
    save: (CustomWorkout) -> Unit,
    close: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title() ?: "My workout") }
    var groups by remember { mutableStateOf(initial?.groups() ?: emptyList()) }
    var editing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var repeatsFor by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun update(change: () -> List<CustomWorkout.Group>) {
        runCatching(change)
            .onSuccess { candidate ->
                if (candidate.sumOf { it.repeats() * it.steps().size } <= 500) {
                    groups = candidate
                    error = null
                } else error = "A workout can have up to 500 steps, including repeats."
            }
            .onFailure { error = it.message }
    }

    fun commit(action: (CustomWorkout) -> Unit) {
        runCatching { CustomWorkout(draftId, title.trim(), groups) }
            .onSuccess(action)
            .onFailure { error = it.message }
    }

    CoachCard {
        Text("Custom running workout", style = MaterialTheme.typography.titleLarge)
        CoachTextInput("Workout name", title, { title = it })
        if (editing == null && repeatsFor == null)
            groups.forEachIndexed { index, group ->
                Text("Group ${index + 1} · ${group.repeats()} rounds")
                group.steps().forEachIndexed { stepIndex, step ->
                    TextButton(onClick = { editing = index to stepIndex }) {
                        Text("Edit step ${stepIndex + 1} · ${step.label()}")
                    }
                    TextButton(
                        onClick = {
                            update {
                                groups.toMutableList().apply {
                                    val remaining =
                                        group.steps().filterIndexed { i, _ -> i != stepIndex }
                                    if (remaining.isEmpty()) removeAt(index)
                                    else
                                        this[index] =
                                            CustomWorkout.Group(group.repeats(), remaining)
                                }
                            }
                        }
                    ) {
                        Text("Delete step ${stepIndex + 1}")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip)) {
                    TextButton(onClick = { editing = index to -1 }) { Text("Add step to group") }
                    TextButton(onClick = { repeatsFor = index }) { Text("Edit rounds") }
                    TextButton(
                        enabled = index > 0,
                        onClick = {
                            update {
                                groups.toMutableList().apply { add(index - 1, removeAt(index)) }
                            }
                        },
                    ) {
                        Text("Move group up")
                    }
                    TextButton(
                        onClick = {
                            update { groups.toMutableList().apply { add(index + 1, group) } }
                        }
                    ) {
                        Text("Duplicate group")
                    }
                    TextButton(
                        onClick = { update { groups.filterIndexed { i, _ -> i != index } } }
                    ) {
                        Text("Delete group")
                    }
                }
            }
        repeatsFor?.let { index ->
            key(index) {
                var rounds by remember { mutableStateOf(groups[index].repeats().toString()) }
                CoachTextInput("Rounds (1–100)", rounds, { rounds = it }, decimal = true)
                TextButton(
                    onClick = {
                        update {
                            val count = rounds.toIntOrNull()
                            require(count != null && count in 1..100) { "Enter 1–100 rounds." }
                            groups.toMutableList().apply {
                                this[index] = CustomWorkout.Group(count, groups[index].steps())
                            }
                        }
                        repeatsFor = null
                    }
                ) {
                    Text("Save rounds")
                }
                TextButton(onClick = { repeatsFor = null }) { Text("Cancel round edit") }
            }
        }
        editing?.let { (groupIndex, stepIndex) ->
            key(groupIndex, stepIndex) {
                RunningStepEditor(
                    groups.getOrNull(groupIndex)?.steps()?.getOrNull(stepIndex),
                    save = { step ->
                        update {
                            if (groupIndex < 0) groups + CustomWorkout.Group(1, listOf(step))
                            else
                                groups.toMutableList().apply {
                                    val group = this[groupIndex]
                                    val steps = group.steps().toMutableList()
                                    if (stepIndex < 0) steps.add(step) else steps[stepIndex] = step
                                    this[groupIndex] = CustomWorkout.Group(group.repeats(), steps)
                                }
                        }
                        editing = null
                    },
                    cancel = { editing = null },
                )
            }
        }
        if (editing == null && repeatsFor == null) {
            TextButton(onClick = { editing = -1 to -1 }) { Text("Add new group") }
            if (start != null) CoachButton("Start custom workout") { commit(start) }
            TextButton(onClick = { commit(save) }) { Text("Save favourite workout") }
            TextButton(onClick = close) { Text("Close editor") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun RunningStepEditor(step: Step?, save: (Step) -> Unit, cancel: () -> Unit) {
    var kind by remember { mutableStateOf(step?.kind() ?: WorkoutSegment.Kind.RUN) }
    var label by remember { mutableStateOf(step?.label().orEmpty()) }
    var metres by remember { mutableStateOf(step?.metres()?.toString().orEmpty()) }
    var seconds by remember { mutableStateOf(step?.millis()?.div(1000.0)?.toString().orEmpty()) }
    var fast by remember { mutableStateOf(step?.fastPace()?.toString().orEmpty()) }
    var slow by remember { mutableStateOf(step?.slowPace()?.toString().orEmpty()) }
    var effort by remember { mutableStateOf(step?.rpe()?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun parse(text: String, field: String): Double? {
        if (text.isBlank()) return null
        val number = text.trim().toDoubleOrNull()
        require(number != null && number.isFinite() && number > 0) { "Enter a valid $field." }
        return number
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip)) {
        WorkoutSegment.Kind.entries.forEach { option ->
            CoachFilterChip(
                option == kind,
                { kind = option },
                {
                    Text(
                        option.name.lowercase().replace('_', ' ').replaceFirstChar {
                            it.uppercase()
                        }
                    )
                },
            )
        }
    }
    CoachTextInput("Step name (optional)", label, { label = it })
    Text("Choose distance or time. Leave both blank to advance manually.")
    CoachTextInput("Distance (metres)", metres, { metres = it }, decimal = true)
    CoachTextInput("Time (seconds)", seconds, { seconds = it }, decimal = true)
    CoachTextInput("Fastest pace (seconds per km)", fast, { fast = it }, decimal = true)
    CoachTextInput("Slowest pace (seconds per km)", slow, { slow = it }, decimal = true)
    CoachTextInput("Effort out of 10 (optional)", effort, { effort = it }, decimal = true)
    TextButton(
        onClick = {
            runCatching {
                    val distance = parse(metres, "distance")
                    val duration = parse(seconds, "time")
                    require(distance == null || duration == null) { "Choose distance or time." }
                    require(distance == null || distance <= 300_000) {
                        "A step can be up to 300 km."
                    }
                    require(duration == null || duration <= 21_600) {
                        "A step can be up to six hours."
                    }
                    val fastest = parse(fast, "fastest pace")
                    val slowest = parse(slow, "slowest pace")
                    require((fastest == null) == (slowest == null)) { "Enter both pace limits." }
                    Step(
                        label.trim().ifEmpty { kind.name.lowercase().replace('_', ' ') },
                        kind,
                        distance,
                        duration?.let { (it * 1000).toLong() },
                        fastest,
                        slowest,
                        parse(effort, "effort"),
                    )
                }
                .onSuccess(save)
                .onFailure { error = it.message }
        }
    ) {
        Text("Save step")
    }
    TextButton(onClick = cancel) { Text("Cancel step edit") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
