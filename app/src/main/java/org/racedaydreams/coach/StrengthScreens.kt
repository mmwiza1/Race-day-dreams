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

@Composable fun StrengthScreen(state:CoachUiState,model:CoachViewModel,planned:String?=null){
    var routineDate by rememberSaveable(planned){mutableStateOf(planned)}
    val plannedDate=routineDate?.let{runCatching{LocalDate.parse(it)}.getOrNull()}
    var logging by rememberSaveable(planned){mutableStateOf(plannedDate!=null)};var historical by rememberSaveable{mutableStateOf(false)};var custom by rememberSaveable{mutableStateOf(false)}
    var name by rememberSaveable{mutableStateOf("")};var error by rememberSaveable{mutableStateOf<String?>(null)};var region by rememberSaveable{mutableStateOf("LOWER")};var mode by rememberSaveable{mutableStateOf("WEIGHTED")}
    val progression=remember(state.strengthSessions){StrengthAnalytics.progression(state.strengthSessions)}
    fun mass(kg:Double)="${decimal(state.massUnit.fromKilograms(kg))} ${state.massUnit.name.lowercase()}"
    CoachPage{
        CoachHeading("Strength")
        Text("Your optional strength training, records and running context.")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){org.racedaydreams.domain.body.MassUnit.entries.forEach{u->FilterChip(selected=state.massUnit==u,onClick={model.setting("mass.unit",u.name)},label={Text(u.name.lowercase())})}}
        if(plannedDate!=null)Text("Planned strength · ${friendlyDate(plannedDate)}. Log what you actually did.")
        if(!logging) {
            val today=LocalDate.now()
            val upcoming=(0L..6L).map{today.plusDays(it)}.firstOrNull { day -> state.profile?.preferences()?.gymDays()?.contains(day.dayOfWeek)==true && state.strengthSessions.none{it.startedAt().atOffset(it.offset()).toLocalDate()==day} && state.strengthPlannedDates.values.none{it==day} }
            upcoming?.let { day -> CoachCard { Text("Next routine strength · ${friendlyDate(day)}");Text("From your normal strength days; actual exercises and effort have not been recorded.");OutlinedButton(onClick={routineDate=day.toString();logging=true},modifier=Modifier.heightIn(min=48.dp)){Text("Log planned strength")}} }
        }
        CoachButton(if(logging)"Hide workout form"else "Log strength workout",{if(!logging)routineDate=null;logging=!logging})
        if(logging)StrengthForm(state,model,plannedDate){logging=false;routineDate=null}
        OutlinedButton(onClick={historical=!historical},modifier=Modifier.heightIn(min=48.dp)){Text("Add historical PB")}
        if(historical)HistoricalForm(state,model){historical=false}
        TextButton(onClick={custom=!custom}){Text("Add custom exercise")}
        if(custom)CoachCard{
            CoachInput("Exercise name",name,{name=it})
            StrengthExercise.Region.values().forEach{v->FilterChip(selected=region==v.name,onClick={region=v.name},label={Text(v.name.lowercase())})}
            StrengthExercise.Mode.values().forEach{v->FilterChip(selected=mode==v.name,onClick={mode=v.name},label={Text(v.name.lowercase().replace('_',' '))})}
            error?.let{Text(it)}
            CoachButton("Save exercise",{try{model.saveExercise(StrengthExercise(UUID.randomUUID().toString(),name.trim(),StrengthExercise.Region.valueOf(region),StrengthExercise.Mode.valueOf(mode),false));custom=false}catch(e:Exception){error=e.message}})
        }
        CoachHeading("Exercise progress and PB history")
        if(progression.isEmpty()&&state.historicalStrength.isEmpty())Text("Log working sets or add a historical record. No maximal test is needed.")
        state.exercises.filter{e->progression.any{it.exerciseId()==e.id()}||state.historicalStrength.any{it.exercise().id()==e.id()}}.forEach{e->CoachCard{
            Text(e.name(),style=MaterialTheme.typography.titleMedium)
            val recorded=progression.filter{it.exerciseId()==e.id()}
            recorded.groupBy{it.category()}.forEach{(category,history)->val best=history.last();Text(if(category=="Estimated 1RM")"Estimated 1RM: ~${kotlin.math.round(state.massUnit.fromKilograms(best.value())).toInt()} ${state.massUnit.name.lowercase()} (Epley estimate)"else if(category=="Bodyweight reps")"Bodyweight reps: ${best.reps()}"else "$category: ${mass(best.kilograms())} × ${best.reps()}")
                WhyDetails(history.joinToString(" → "){"${shortDate(it.at().atZone(ZoneId.systemDefault()).toLocalDate())}: ${mass(it.kilograms())} × ${it.reps()}"},"Recorded workout PB history")
            }
            state.historicalStrength.filter{it.exercise().id()==e.id()}.forEach{r->
                Text("Manual historical PB · ${mass(r.kilograms())} × ${r.reps()}")
                Text(when(r.precision()){HistoricalStrengthRecord.DatePrecision.UNKNOWN->"Date unknown";HistoricalStrengthRecord.DatePrecision.APPROXIMATE->"Approximately ${friendlyDate(r.date())}";else->friendlyDate(r.date())})
                if(recorded.any{it.reps()==r.reps()&&it.kilograms()>r.kilograms()})Text("A recorded workout has exceeded this historical result.")
                r.notes()?.takeIf{it.isNotBlank()}?.let{Text(it)}
                TextButton(onClick={model.deleteHistorical(r.id())}){Text("Delete historical entry")}
            }
        }}
        CoachHeading("Latest sessions")
        state.strengthSessions.sortedByDescending{it.startedAt()}.forEach{s->CoachCard{
            Text("${friendlyDate(s.startedAt().atOffset(s.offset()).toLocalDate())} · ${s.duration().toMinutes()} min")
            state.strengthPlannedDates[s.id()]?.let{Text("Linked planned strength date: ${friendlyDate(it)}") }
            val context=StrengthAnalytics.context(s);Text("${context.region().name.lowercase().replace('_',' ')} · ${context.effort()} · lower-body involvement ${context.lowerBody().name.lowercase()}")
            WhyDetails(context.reasons().joinToString("\n"),"Why this strength context?")
            s.exercises().forEach{b->Text(b.exercise().name());b.sets().forEachIndexed{i,set->Text("Set ${i+1}: ${mass(set.kilograms())} × ${set.reps()}${set.rpe()?.let{" · RPE $it"}?:""}")}}
            if(s.notes().isNotBlank())Text(s.notes())
            TextButton(onClick={model.deleteStrength(s.id())}){Text("Delete session and recalculate PBs")}
        }}
    }
}
@Composable private fun ExercisePicker(state:CoachUiState,selected:String,onSelect:(String)->Unit){var menu by remember{mutableStateOf(false)};Box{OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(state.exercises.firstOrNull{it.id()==selected}?.name()?:"Choose exercise")};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){state.exercises.forEach{e->DropdownMenuItem(text={Text(e.name())},onClick={onSelect(e.id());menu=false})}}}}
@Composable private fun HistoricalForm(s:CoachUiState,model:CoachViewModel,done:()->Unit){
    var exercise by rememberSaveable{mutableStateOf(s.exercises.first().id())};var kg by rememberSaveable{mutableStateOf("")};var reps by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var precision by rememberSaveable{mutableStateOf("UNKNOWN")};var date by rememberSaveable{mutableStateOf(LocalDate.now().toString())};var error by rememberSaveable{mutableStateOf<String?>(null)}
    CoachCard{Text("Historical strength PB");ExercisePicker(s,exercise,{exercise=it});CoachInput("Load (${s.massUnit.name.lowercase()}; added load for weighted bodyweight)",kg,{kg=it},true);CoachInput("Reps",reps,{reps=it},true)
        HistoricalStrengthRecord.DatePrecision.values().forEach{p->FilterChip(selected=precision==p.name,onClick={precision=p.name},label={Text(p.name.lowercase())})};if(precision!="UNKNOWN")DateInput("PB date",date,{date=it});CoachInput("Notes (optional)",notes,{notes=it});error?.let{Text(it)}
        CoachButton("Save historical PB",{try{model.saveHistorical(HistoricalStrengthRecord(UUID.randomUUID().toString(),s.exercises.first{it.id()==exercise},s.massUnit.toKilograms(kg.toDouble()),reps.toInt(),if(precision=="UNKNOWN")null else LocalDate.parse(date),HistoricalStrengthRecord.DatePrecision.valueOf(precision),notes),done)}catch(e:Exception){error=e.message?:"Check record values."}})
    }
}
/** Draft storage uses strings internally, while visible entry has separate normal set fields. */
@Composable private fun StrengthForm(s:CoachUiState,model:CoachViewModel,plannedDate:LocalDate?,done:()->Unit){
    var selected by rememberSaveable{mutableStateOf(s.exercises.first().id())};var load by rememberSaveable{mutableStateOf("")};var reps by rememberSaveable{mutableStateOf("")};var effort by rememberSaveable{mutableStateOf("")};var sets by rememberSaveable{mutableStateOf(arrayListOf<String>())};var blocks by rememberSaveable{mutableStateOf(arrayListOf<String>())}
    var date by rememberSaveable{mutableStateOf((plannedDate?:LocalDate.now()).toString())};var time by rememberSaveable{mutableStateOf("18:00")};var minutes by rememberSaveable{mutableStateOf("")};var rpe by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var error by rememberSaveable{mutableStateOf<String?>(null)}
    fun parseSet(line:String):StrengthSession.SetEntry{val p=line.split('|');return StrengthSession.SetEntry(UUID.randomUUID().toString(),p[1].toInt(),p[0].toDouble(),p[2].takeIf{it.isNotBlank()}?.toDouble(),false)}
    CoachCard{
        Text("Completed strength workout");DateInput("Session date",date,{date=it});CoachInput("Start time (HH:mm, local)",time,{time=it});CoachInput("Duration (minutes)",minutes,{minutes=it},true);CoachInput("Session RPE 0–10 (optional)",rpe,{rpe=it},true)
        ExercisePicker(s,selected,{selected=it})
        CoachInput("Weight (${s.massUnit.name.lowercase()})",load,{load=it},true);CoachInput("Reps",reps,{reps=it},true);CoachInput("Set RPE 0–10 (optional)",effort,{effort=it},true)
        CoachButton("Add set",{try{val line="${s.massUnit.toKilograms(load.toDouble())}|$reps|$effort";parseSet(line);sets=ArrayList(sets+line);error=null}catch(e:Exception){error=e.message?:"Check set values."}})
        sets.forEachIndexed{i,line->val set=parseSet(line);Row(Modifier.fillMaxWidth()){Text("${i+1}: ${decimal(s.massUnit.fromKilograms(set.kilograms()))} ${s.massUnit.name.lowercase()} × ${set.reps()}",Modifier.weight(1f));TextButton(onClick={load=s.massUnit.fromKilograms(set.kilograms()).toString();reps=set.reps().toString();effort=set.rpe()?.toString()?:"";sets=ArrayList(sets.filterIndexed{index,_->index!=i})}){Text("Edit")};TextButton(onClick={sets=ArrayList(sets.filterIndexed{index,_->index!=i})}){Text("Remove")}}}
        if(sets.isNotEmpty())TextButton(onClick={sets=ArrayList(sets+sets.last())}){Text("Duplicate previous set")}
        OutlinedButton(onClick={if(sets.isNotEmpty()){blocks=ArrayList(blocks+"$selected\t${sets.joinToString(";")}");sets=arrayListOf()}else error="Add at least one set."},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Add exercise and sets")}
        blocks.forEachIndexed{i,b->Text("${i+1}. ${s.exercises.firstOrNull{it.id()==b.substringBefore('\t')}?.name()} · ${b.substringAfter('\t').split(';').size} sets")}
        if(blocks.isNotEmpty())TextButton(onClick={blocks=ArrayList(blocks.dropLast(1))}){Text("Remove last exercise")}
        CoachInput("Notes (optional)",notes,{notes=it});error?.let{Text(it)}
        CoachButton("Save strength workout",{try{require(sets.isEmpty()){ "Add the pending exercise and sets first." };val exercises=blocks.map{b->StrengthSession.ExerciseSets(s.exercises.first{it.id()==b.substringBefore('\t')},b.substringAfter('\t').split(';').map(::parseSet))};require(exercises.isNotEmpty()){ "Add at least one exercise." };val start=LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault());val duration=Duration.ofMinutes(minutes.toLong());require(start.toInstant().plus(duration)<=Instant.now()){ "Completed session must end before now." };model.saveStrength(StrengthSession(UUID.randomUUID().toString(),start.toInstant(),start.offset,duration,rpe.takeIf{it.isNotBlank()}?.toDouble(),notes,exercises),done,plannedDate)}catch(e:Exception){error=e.message?:"Check workout values."}})
    }
}
