package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.*
import java.util.UUID
import org.racedaydreams.domain.strength.*

@Composable
fun StrengthScreen(state: CoachUiState, model: CoachViewModel) {
    var logging by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("LOWER") }
    var mode by rememberSaveable { mutableStateOf("WEIGHTED") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val progression = remember(state.strengthSessions) { StrengthAnalytics.progression(state.strengthSessions) }
    CoachPage {
        CoachHeading("Strength")
        Text("Optional training context. Running and strength loads are shown separately; they are not physiologically interchangeable.")
        CoachButton(if (logging) "Hide workout form" else "Log strength workout") { logging = !logging }
        if (logging) StrengthForm(state, model) { logging = false }
        OutlinedButton(onClick = { custom = !custom }, modifier=Modifier.heightIn(min=48.dp)) { Text("Add custom exercise") }
        if (custom) CoachCard {
            CoachInput("Exercise name", name, { name=it })
            Text("Body region")
            StrengthExercise.Region.values().forEach { value -> FilterChip(selected=region==value.name, onClick={region=value.name}, label={Text(value.name.lowercase())}) }
            Text("Load type (bodyweight uses no added load)")
            StrengthExercise.Mode.values().forEach { value -> FilterChip(selected=mode==value.name, onClick={mode=value.name}, label={Text(value.name.replace('_',' ').lowercase())}) }
            error?.let { Text(it) }
            CoachButton("Save exercise") {
                try { model.saveExercise(StrengthExercise(UUID.randomUUID().toString(), name.trim(), StrengthExercise.Region.valueOf(region), StrengthExercise.Mode.valueOf(mode), false));custom=false;name="" }
                catch(e:Exception){error=e.message}
            }
        }
        CoachHeading("Exercise progress and PB history")
        if (progression.isEmpty()) Text("No strength records yet. Record working sets to see progress; warm-up sets do not award PBs.")
        state.exercises.filter { exercise -> progression.any { it.exerciseId()==exercise.id() } }.forEach { exercise -> CoachCard {
            Text(exercise.name(),style=MaterialTheme.typography.titleMedium)
            val records=progression.filter { it.exerciseId()==exercise.id() }
            records.groupBy { it.category() }.forEach { (category, history) ->
                val best=history.last()
                Text(if(category=="Estimated 1RM") "$category: ~${kotlin.math.round(best.value()).toInt()} kg (Epley estimate)"
                    else if(category=="Bodyweight reps") "$category: ${best.reps()}" else "$category: ${best.kilograms()} kg × ${best.reps()}")
                Text(history.joinToString(" → ") { "${it.at().atZone(ZoneId.systemDefault()).toLocalDate()}: ${it.kilograms()} kg × ${it.reps()}" }, style=MaterialTheme.typography.bodySmall)
            }
            Text("Estimates use suitable weighted sets of 1–10 reps; a one-rep set is recorded load. High-rep and weighted-bodyweight sets do not produce a 1RM estimate. No maximal test is needed.")
        } }
        CoachHeading("Latest sessions")
        state.strengthSessions.forEach { session -> CoachCard {
            val context=StrengthAnalytics.context(session)
            Text("${session.startedAt().atOffset(session.offset()).toLocalDate()} · ${session.duration().toMinutes()} min",style=MaterialTheme.typography.titleMedium)
            Text("${context.region()} · ${context.effort()} · lower-body involvement ${context.lowerBody()}")
            context.reasons().forEach { Text(it) }
            StrengthAnalytics.sessionLoad(session)?.let { Text("Strength session-RPE load: ${kotlin.math.round(it).toInt()} min·RPE (context only)") }
            session.exercises().forEach { exercise ->
                Text(exercise.exercise().name(),style=MaterialTheme.typography.titleSmall)
                exercise.sets().forEachIndexed { i, set -> Text("Set ${i+1}: ${set.kilograms()} kg × ${set.reps()}${set.rpe()?.let { " · RPE $it" } ?: ""}${if(set.warmUp()) " · warm-up" else ""}") }
            }
            if(session.notes().isNotBlank()) Text(session.notes())
            TextButton(onClick={model.deleteStrength(session.id())},modifier=Modifier.heightIn(min=48.dp)){Text("Delete session and recalculate PBs")}
        } }
    }
}

/** Saved text drafts survive rotation; no invented sets or default working weights are saved. */
@Composable
private fun StrengthForm(state: CoachUiState, model: CoachViewModel, done: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(state.exercises.first().id()) }
    var sets by rememberSaveable { mutableStateOf("") }
    var blocks by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable { mutableStateOf("18:00") }
    var minutes by rememberSaveable { mutableStateOf("") }; var rpe by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }; var error by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    CoachCard {
        Text("Completed strength workout")
        CoachInput("Date (YYYY-MM-DD)",date,{date=it});CoachInput("Start time (HH:mm, local)",time,{time=it})
        CoachInput("Duration (minutes)",minutes,{minutes=it},true);CoachInput("Session RPE 0–10 (optional)",rpe,{rpe=it},true)
        Box {
            OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(state.exercises.firstOrNull { it.id()==selected }?.name() ?: "Choose exercise")}
            DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {state.exercises.forEach { e -> DropdownMenuItem(text={Text(e.name())},onClick={selected=e.id();menu=false})}}
        }
        Text("Sets in order: one per line as kg,reps[,RPE]. Use 0 kg for bodyweight. These are working sets; warm-ups need not be logged here.")
        OutlinedTextField(sets,{sets=it},label={Text("Working sets (e.g. 80,5,7)")},modifier=Modifier.fillMaxWidth(),minLines=3)
        OutlinedButton(onClick={
            try { parseSets(sets);blocks=ArrayList(blocks+"$selected\t$sets");sets="";error=null }catch(e:Exception){error=e.message ?: "Check set entries."}
        },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Add exercise and sets")}
        blocks.forEachIndexed { i, block -> Text("${i+1}. ${state.exercises.firstOrNull { it.id()==block.substringBefore('\t') }?.name()} · ${block.substringAfter('\t').lines().size} sets") }
        if(blocks.isNotEmpty()) TextButton(onClick={blocks=ArrayList(blocks.dropLast(1))}){Text("Remove last exercise")}
        CoachInput("Notes (optional)",notes,{notes=it})
        error?.let { Text(it) }
        Button(enabled=!state.busy,onClick={
            try {
                val start=LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault())
                val exercises=blocks.map { block -> StrengthSession.ExerciseSets(state.exercises.first { it.id()==block.substringBefore('\t') },parseSets(block.substringAfter('\t'))) }
                require(exercises.isNotEmpty()){ "Add at least one exercise and its sets." }
                val duration=Duration.ofMinutes(minutes.toLong());require(start.toInstant().plus(duration)<=Instant.now()){ "A completed session must end before now." }
                val session=StrengthSession(UUID.randomUUID().toString(),start.toInstant(),start.offset,duration,rpe.takeIf{it.isNotBlank()}?.toDouble(),notes,exercises)
                model.saveStrength(session,done);error=null
            }catch(e:Exception){error=e.message ?: "Check workout values."}
        },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Save strength workout")}
    }
}
private fun parseSets(text:String):List<StrengthSession.SetEntry> {
    require(text.isNotBlank()){ "Enter at least one set." }
    return text.lines().filter{it.isNotBlank()}.map { line ->
        val parts=line.split(',').map{it.trim()};require(parts.size in 2..3){ "Use kg,reps or kg,reps,RPE for each set." }
        StrengthSession.SetEntry(UUID.randomUUID().toString(),parts[1].toInt(),parts[0].toDouble(),parts.getOrNull(2)?.takeIf{it.isNotBlank()}?.toDouble(),false)
    }
}
