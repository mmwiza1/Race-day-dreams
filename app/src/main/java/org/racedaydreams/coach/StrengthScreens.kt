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

@Composable fun StrengthScreen(state:CoachUiState,model:CoachViewModel,planned:String?=null, section:String="log", sourceId:String?=null){
    var routineDate by rememberSaveable(planned){mutableStateOf(planned)}
    val plannedDate=routineDate?.let{runCatching{LocalDate.parse(it)}.getOrNull()}
    var logging by rememberSaveable(planned){mutableStateOf(plannedDate!=null)};var historical by rememberSaveable{mutableStateOf(false)};var custom by rememberSaveable{mutableStateOf(false)}
    var name by rememberSaveable{mutableStateOf("")};var error by rememberSaveable{mutableStateOf<String?>(null)};var region by rememberSaveable{mutableStateOf("LOWER")};var mode by rememberSaveable{mutableStateOf("WEIGHTED")}
    val progression=remember(state.strengthSessions){StrengthAnalytics.progression(state.strengthSessions)}
    val timeline=remember(state.strengthSessions,state.historicalStrength){StrengthProgression.timeline(state.strengthSessions,state.historicalStrength)}
    fun mass(kg:Double)="${decimal(state.massUnit.fromKilograms(kg))} ${state.massUnit.name.lowercase()}"
    CoachPage{
        CoachHeading("Strength")
        Text("Your optional strength training, records and running context.")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){org.racedaydreams.domain.body.MassUnit.entries.forEach{u->FilterChip(selected=state.massUnit==u,onClick={model.setting("mass.unit",u.name)},label={Text(u.name.lowercase())})}}
        if(plannedDate!=null)Text("Planned strength · ${friendlyDate(plannedDate)}. Log what you actually did.")
        if(section=="log" && !logging) {
            val today=LocalDate.now()
            val upcoming=(0L..6L).map{today.plusDays(it)}.firstOrNull { day -> state.profile?.preferences()?.gymDays()?.contains(day.dayOfWeek)==true && state.strengthSessions.none{it.startedAt().atOffset(it.offset()).toLocalDate()==day} && state.strengthPlannedDates.values.none{it==day} }
            upcoming?.let { day -> CoachCard { Text("Next routine strength · ${friendlyDate(day)}");Text("From your normal strength days; actual exercises and effort have not been recorded.");OutlinedButton(onClick={routineDate=day.toString();logging=true},modifier=Modifier.heightIn(min=48.dp)){Text("Log planned strength")}} }
        }
        if(section=="log"){
        CoachButton(if(logging)"Hide workout form"else "Log strength workout",{if(!logging)routineDate=null;logging=!logging})
        if(logging)StrengthForm(state,model,plannedDate){logging=false;routineDate=null}
        }
        if(section=="pbs"){
        OutlinedButton(onClick={historical=!historical},modifier=Modifier.heightIn(min=48.dp)){Text("Add historical PB")}
        if(historical)HistoricalForm(state,model){historical=false}
        CoachHeading("Exercise progress and PB history")
        if(progression.isEmpty()&&state.historicalStrength.isEmpty())Text("Log working sets or add a historical record. No maximal test is needed.")
        state.exercises.filter{e->timeline.any{it.exerciseId()==e.id()}}.forEach{e->CoachCard{
            Text(e.name(),style=MaterialTheme.typography.titleMedium)
            val categories=timeline.filter{it.exerciseId()==e.id()}.map{it.category()}.distinct()
            var selectedCategory by rememberSaveable(e.id()){mutableStateOf(categories.firstOrNull{it=="Load × 5"}?:categories.first())}
            var choose by remember{mutableStateOf(false)}
            val category=selectedCategory.takeIf{it in categories}?:categories.first()
            Box{OutlinedButton(onClick={choose=true},modifier=Modifier.heightIn(min=48.dp)){Text(category)};DropdownMenu(expanded=choose,onDismissRequest={choose=false}){categories.forEach{c->DropdownMenuItem(text={Text(c)},onClick={selectedCategory=c;choose=false})}}}
            val best=StrengthProgression.best(timeline,e.id(),category)
            best?.let{if(category!="Estimated 1RM")PbCelebration(state,model,"strength:"+it.id()+":"+category);Text(if(category.startsWith("Duration"))"Current PB: ${StrengthMeasurements.duration(it.value().toLong())}"else if(category.startsWith("Distance"))"Current PB: ${decimal(it.value())} m"else if(category=="Estimated 1RM")"Estimated 1RM: ~${kotlin.math.round(state.massUnit.fromKilograms(it.value())).toInt()} ${state.massUnit.name.lowercase()}"else if(category=="Bodyweight reps")"Current PB: ${it.reps()} reps"else "Current PB: ${mass(it.kilograms())} × ${it.reps()}",style=MaterialTheme.typography.titleLarge);Text(it.provenance(),style=MaterialTheme.typography.labelMedium)}
            if(category=="Estimated 1RM")WhyDetails("Epley estimate from eligible working sets of at most 10 reps. This is approximate and does not require maximal testing.","How was this estimated?")
            var expanded by rememberSaveable(e.id()){mutableStateOf(false)}
            TextButton(onClick={expanded=!expanded},modifier=Modifier.heightIn(min=48.dp)){Text(if(expanded)"Hide progression"else "View progression")}
            if(expanded){val history=timeline.filter{it.exerciseId()==e.id()&&it.category()==category}
                StrengthTrend(history,state)
                history.forEach{r->
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                        HorizontalDivider()
                        Text(when(r.precision()){HistoricalStrengthRecord.DatePrecision.UNKNOWN->"Date unknown · not placed in chronology";HistoricalStrengthRecord.DatePrecision.APPROXIMATE->"Around ${shortDate(r.date())}";else->shortDate(r.date())},style=MaterialTheme.typography.titleSmall)
                        Text(if(category.startsWith("Duration"))StrengthMeasurements.duration(r.value().toLong())else if(category.startsWith("Distance"))"${decimal(r.value())} m"else if(category=="Estimated 1RM")"~${kotlin.math.round(state.massUnit.fromKilograms(r.value())).toInt()} ${state.massUnit.name.lowercase()} · estimated"else if(category=="Bodyweight reps")"${r.reps()} reps"else "${mass(r.kilograms())} × ${r.reps()}")
                        Text(r.provenance(),style=MaterialTheme.typography.labelMedium)
                        var details by rememberSaveable(r.id(),category){mutableStateOf(false)}
                        if(r.sessionId()==null||!r.notes().isNullOrBlank())TextButton(onClick={details=!details},modifier=Modifier.heightIn(min=48.dp)){Text(if(details)"Hide record details"else "Record details")}
                        if(details){
                            if(r.sessionId()==null){var edit by rememberSaveable(r.id()){mutableStateOf(false)};TextButton(onClick={edit=!edit}){Text("Edit historical PB")};if(edit)state.historicalStrength.firstOrNull{it.id()==r.id()}?.let{HistoricalForm(state,model,it){edit=false}}}
                            r.notes()?.takeIf{it.isNotBlank()}?.let{Text(it)};if(r.sessionId()==null)TextButton(onClick={model.deleteHistorical(r.id())}){Text("Delete historical entry")}}
                    }
                }
            }
        }}
        }
        if(section=="history"){
        CoachHeading("Latest sessions")
        var historyLimit by rememberSaveable{mutableIntStateOf(20)}
        if(state.strengthSessions.isEmpty())Text("No strength workouts yet. Log your first workout from Strength.")
        state.strengthSessions.sortedByDescending{it.startedAt()}.take(historyLimit).forEach{s->CoachCard{
            Text("${friendlyDate(s.startedAt().atOffset(s.offset()).toLocalDate())} · ${s.duration().toMinutes()} min")
            state.strengthPlannedDates[s.id()]?.let{Text("Linked planned strength date: ${friendlyDate(it)}") }
            state.gymLinks[s.id()]?.let{Text("Prescribed block: ${it.versionKey().substringAfterLast(":")} · original prescription retained")}
            val context=StrengthAnalytics.context(s);Text("${context.region().name.lowercase().replace('_',' ')} · ${context.effort()} · lower-body involvement ${context.lowerBody().name.lowercase()}")
            WhyDetails(context.reasons().joinToString("\n"),"Why this strength context?")
            val newRecords=progression.filter{r->r.sessionId()==s.id()&&r.category()!="Estimated 1RM"&&timeline.none{it.sessionId()==null&&it.exerciseId()==r.exerciseId()&&it.category()==r.category()&&it.value()>=r.value()}}
            if(newRecords.isNotEmpty())Text("PB evidence recorded · ${newRecords.map{it.exerciseId()}.distinct().size} exercises",style=MaterialTheme.typography.titleSmall)
            s.exercises().forEach{b->
                Text(b.exercise().name(),style=MaterialTheme.typography.titleMedium)
                Text("${b.sets().size} sets",style=MaterialTheme.typography.labelMedium)
                b.sets().forEachIndexed{i,set->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${i+1}. ${StrengthMeasurements.display(set,state.massUnit,state.profile?.preferences()?.units()?:org.racedaydreams.domain.UnitSystem.METRIC)}");Text(set.repetitionsInReserve()?.let{"RIR ${decimal(it)}"}?:set.rpe()?.let{"RPE ${decimal(it)}"}?:"Effort —")}}
            }
            if(s.notes().isNotBlank())Text(s.notes())
            var editing by rememberSaveable(s.id()){mutableStateOf(sourceId==s.id())}
            TextButton(onClick={editing=!editing}){Text("Edit source workout")}
            if(editing)key(s.id()){StrengthForm(state,model,state.strengthPlannedDates[s.id()],s){editing=false}}
            TextButton(onClick={model.deleteStrength(s.id())}){Text("Delete session and recalculate PBs")}
        }}
        if(state.strengthSessions.size>historyLimit)TextButton(onClick={historyLimit+=20}){Text("Load more workouts")}
        }
    }
}
@Composable private fun ExercisePicker(state:CoachUiState,selected:String,onSelect:(String)->Unit){
    var menu by remember{mutableStateOf(false)};var query by rememberSaveable{mutableStateOf("")}
    OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(state.exercises.firstOrNull{it.id()==selected}?.name()?:"Choose exercise")}
    if(menu)AlertDialog(onDismissRequest={menu=false},title={Text("Choose exercise")},text={Column{CoachInput("Search exercises or aliases",query,{query=it});val rows=exerciseMatches(state,query);Text("${rows.size} matching exercises");androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=320.dp)){items(rows.size){i->TextButton(onClick={onSelect(rows[i].id());menu=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(rows[i].name())}}}}},confirmButton={TextButton(onClick={menu=false}){Text("Cancel")}})
}
@Composable private fun HistoricalForm(s:CoachUiState,model:CoachViewModel,existing:HistoricalStrengthRecord?=null,done:()->Unit){
    var exercise by rememberSaveable{mutableStateOf(existing?.exercise()?.id()?:s.exercises.first().id())};var kg by rememberSaveable{mutableStateOf(existing?.kilograms()?.let{s.massUnit.fromKilograms(it).toString()}?:"")};var reps by rememberSaveable{mutableStateOf(existing?.let{it.durationSeconds()?.toString()?:it.distanceMetres()?.toString()?:it.reps().toString()}?:"")};var notes by rememberSaveable{mutableStateOf(existing?.notes()?:"")};var precision by rememberSaveable{mutableStateOf(existing?.precision()?.name?:"UNKNOWN")};var date by rememberSaveable{mutableStateOf((existing?.date()?:LocalDate.now()).toString())};var error by rememberSaveable{mutableStateOf<String?>(null)}
    CoachCard{Text("Historical strength PB");ExercisePicker(s,exercise,{exercise=it});val selected=s.exercises.first{it.id()==exercise};val profile=selected.measurement();if(profile.usesLoad())CoachInput("Load (${s.massUnit.name.lowercase()}; added load for weighted bodyweight)",kg,{kg=it},true);if(profile.usesReps())CoachInput("Reps",reps,{reps=it},true);if(profile.usesDuration())CoachInput("Duration (seconds or m:ss)",reps,{reps=it});if(profile.usesDistance())CoachInput("Distance (metres)",reps,{reps=it},true)
        HistoricalStrengthRecord.DatePrecision.values().forEach{p->FilterChip(selected=precision==p.name,onClick={precision=p.name},label={Text(p.name.lowercase())})};if(precision!="UNKNOWN")DateInput("PB date",date,{date=it});CoachInput("Notes (optional)",notes,{notes=it});error?.let{Text(it)}
        CoachButton("Save historical PB",{try{model.saveHistorical(HistoricalStrengthRecord(existing?.id()?:UUID.randomUUID().toString(),s.exercises.first{it.id()==exercise},if(profile.usesLoad())s.massUnit.toKilograms(kg.toDouble())else 0.0,if(profile.usesReps())reps.toInt()else 0,if(precision=="UNKNOWN")null else LocalDate.parse(date),HistoricalStrengthRecord.DatePrecision.valueOf(precision),notes,if(profile.usesDuration())StrengthMeasurements.seconds(reps)else null,if(profile.usesDistance())reps.toDouble()else null),done)}catch(e:Exception){error=e.message?:"Check record values."}})
    }
}
/** Draft storage uses strings internally, while visible entry has separate normal set fields. */
@Composable private fun StrengthForm(s:CoachUiState,model:CoachViewModel,plannedDate:LocalDate?,editing:StrengthSession?=null,done:()->Unit){
    var editingSet by rememberSaveable{mutableStateOf<Int?>(null)}
    var effortMode by rememberSaveable{mutableStateOf(s.gymProgramme?.settings()?.effort()?.name?:"RPE")}
    var selected by rememberSaveable{mutableStateOf(s.exercises.first().id())};var load by rememberSaveable{mutableStateOf("")};var reps by rememberSaveable{mutableStateOf("")};var effort by rememberSaveable{mutableStateOf("")};var sets by rememberSaveable{mutableStateOf(arrayListOf<String>())};var blocks by rememberSaveable{mutableStateOf(ArrayList(editing?.exercises()?.map{block->"${block.exercise().id()}\t${block.sets().joinToString(";"){"${it.kilograms()}|${it.reps()}|${it.rpe()?:""}|${it.durationSeconds()?:""}|${it.distanceMetres()?:""}|${it.warmUp()}|${it.id()}|${it.repetitionsInReserve()?:""}"}}"}?:emptyList()))}
    var seconds by rememberSaveable{mutableStateOf("")};var metres by rememberSaveable{mutableStateOf("")};
    var date by rememberSaveable{mutableStateOf((editing?.startedAt()?.atOffset(editing.offset())?.toLocalDate()?:plannedDate?:LocalDate.now()).toString())};var time by rememberSaveable{mutableStateOf(editing?.startedAt()?.atOffset(editing.offset())?.toLocalTime()?.withSecond(0)?.withNano(0)?.toString()?:"18:00")};var minutes by rememberSaveable{mutableStateOf(editing?.duration()?.toMinutes()?.toString()?:"")};var rpe by rememberSaveable{mutableStateOf(editing?.rpe()?.toString()?:"")};var notes by rememberSaveable{mutableStateOf(editing?.notes()?:"")};var error by rememberSaveable{mutableStateOf<String?>(null)}
    fun parseSet(line:String):StrengthSession.SetEntry{val p=line.split('|');return StrengthSession.SetEntry(p.getOrNull(6)?.takeIf{it.isNotBlank()}?:UUID.randomUUID().toString(),p[1].toInt(),p[0].toDouble(),p[2].takeIf{it.isNotBlank()}?.toDouble(),p.getOrNull(5)=="true",p.getOrNull(3)?.takeIf{it.isNotBlank()}?.toLong(),p.getOrNull(4)?.takeIf{it.isNotBlank()}?.toDouble(),p.getOrNull(7)?.takeIf{it.isNotBlank()}?.toDouble())}
    CoachCard{
        Text("Completed strength workout");DateInput("Session date",date,{date=it});CoachInput("Start time (HH:mm, local)",time,{time=it});CoachInput("Duration (minutes)",minutes,{minutes=it},true);CoachInput("Session RPE 0–10 (optional)",rpe,{rpe=it},true)
        ExercisePicker(s,selected,{next->if(next!=selected&&editingSet==null){if(sets.isNotEmpty()){blocks=ArrayList(blocks+"$selected\t${sets.joinToString(";")}");sets=arrayListOf()};selected=next;editingSet=null;load="";reps="";effort=""}})
        val exercise=s.exercises.first{it.id()==selected};val profile=exercise.measurement()
        val previous=s.strengthSessions.sortedByDescending{it.startedAt()}.firstNotNullOfOrNull{session->session.exercises().firstOrNull{it.exercise().id()==selected}}
        previous?.let { block -> CoachCard{Text("Last time",style=MaterialTheme.typography.titleSmall);block.sets().forEach{Text(StrengthMeasurements.display(it,s.massUnit,s.profile?.preferences()?.units()?:org.racedaydreams.domain.UnitSystem.METRIC))};TextButton(onClick={sets=ArrayList(block.sets().map{"${it.kilograms()}|${it.reps()}|${it.rpe()?:""}|${it.durationSeconds()?:""}|${it.distanceMetres()?:""}|false||${it.repetitionsInReserve()?:""}"})}){Text("Use previous sets")}} }
        if(profile.usesLoad())CoachInput("${if(selected=="farmers-carry"||selected=="suitcase-carry")"Load per hand"else "Weight"} (${s.massUnit.name.lowercase()})",load,{load=it},true)
        if(profile.usesReps())CoachInput("Reps",reps,{reps=it},true)
        if(profile.usesDuration())CoachInput("Duration (seconds or m:ss)",seconds,{seconds=it})
        if(profile.usesDistance())CoachInput(if(s.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL)"Distance (yards)"else "Distance (metres)",metres,{metres=it},true)
        if(profile.usesReps())Row{listOf("RPE","RIR").forEach{mode->FilterChip(selected=effortMode==mode,onClick={effortMode=mode;effort=""},label={Text(mode)})}}
        val usingRir=profile.usesReps()&&effortMode=="RIR"
        CoachInput(if(usingRir)"Reps in reserve (optional)"else "Set RPE 0–10 (optional)",effort,{effort=it},true)
        CoachButton(if(editingSet==null)"Add set"else "Update set",{try{val line="${if(profile.usesLoad())s.massUnit.toKilograms(load.toDouble())else 0.0}|${if(profile.usesReps())reps else "0"}|${if(usingRir)""else effort}|${if(profile.usesDuration())StrengthMeasurements.seconds(seconds)else ""}|${if(profile.usesDistance())metres.toDouble()*(if(s.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL)0.9144 else 1.0)else ""}|false||${if(usingRir)effort else ""}";StrengthSession.ExerciseSets(exercise,listOf(parseSet(line)));sets=ArrayList(if(editingSet==null)sets+line else sets.mapIndexed{i,old->if(i==editingSet)line else old});editingSet=null;error=null}catch(e:Exception){error=e.message?:"Check set values."}})
        sets.forEachIndexed{i,line->val set=parseSet(line);Row(Modifier.fillMaxWidth()){Text("${i+1}: ${StrengthMeasurements.display(set,s.massUnit,s.profile?.preferences()?.units()?:org.racedaydreams.domain.UnitSystem.METRIC)}",Modifier.weight(1f));TextButton(onClick={load=s.massUnit.fromKilograms(set.kilograms()).toString();reps=set.reps().toString();seconds=set.durationSeconds()?.toString()?:"";metres=set.distanceMetres()?.let{(it/(if(s.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL)0.9144 else 1.0)).toString()}?:"";effortMode=if(set.repetitionsInReserve()!=null)"RIR"else "RPE";effort=(set.repetitionsInReserve()?:set.rpe())?.toString()?:"";editingSet=i}){Text("Edit")};TextButton(onClick={sets=ArrayList(sets.filterIndexed{index,_->index!=i});editingSet=null}){Text("Remove")}}}
        if(editingSet!=null)TextButton(onClick={editingSet=null}){Text("Cancel set edit")}
        if(sets.isNotEmpty()&&editingSet==null)TextButton(onClick={sets=ArrayList(sets+sets.last())}){Text("Duplicate previous set")}
        OutlinedButton(onClick={if(editingSet!=null){error="Update or cancel the set edit first."}else if(sets.isNotEmpty()){blocks=ArrayList(blocks+"$selected\t${sets.joinToString(";")}");sets=arrayListOf();editingSet=null}else error="Add at least one set."},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Add exercise and sets")}
        if(blocks.isNotEmpty())Text("Session review",style=MaterialTheme.typography.titleMedium)
        blocks.forEachIndexed{i,b->CoachCard{
            Text(s.exercises.firstOrNull{it.id()==b.substringBefore('\t')}?.name()?:"Exercise",style=MaterialTheme.typography.titleSmall)
            b.substringAfter('\t').split(';').forEach{line->val set=parseSet(line);Text("${StrengthMeasurements.display(set,s.massUnit,s.profile?.preferences()?.units()?:org.racedaydreams.domain.UnitSystem.METRIC)}${set.rpe()?.let{" · RPE ${decimal(it)}"}?:""}")}
            Row{TextButton(onClick={if(sets.isNotEmpty()){error="Finish the current exercise first."}else{selected=b.substringBefore('\t');sets=ArrayList(b.substringAfter('\t').split(';'));blocks=ArrayList(blocks.filterIndexed{index,_->index!=i});editingSet=null}}){Text("Edit exercise")};TextButton(onClick={blocks=ArrayList(blocks.filterIndexed{index,_->index!=i})}){Text("Remove exercise")}}
        }}
        CoachInput("Notes (optional)",notes,{notes=it});error?.let{Text(it)}
        CoachButton("Save strength workout",{try{require(sets.isEmpty()){ "Add the pending exercise and sets first." };val exercises=blocks.map{b->StrengthSession.ExerciseSets(s.exercises.first{it.id()==b.substringBefore('\t')},b.substringAfter('\t').split(';').map(::parseSet))};require(exercises.isNotEmpty()){ "Add at least one exercise." };val start=LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault());val duration=Duration.ofMinutes(minutes.toLong());require(start.toInstant().plus(duration)<=Instant.now()){ "Completed session must end before now." };model.saveStrength(StrengthSession(editing?.id()?:UUID.randomUUID().toString(),start.toInstant(),start.offset,duration,rpe.takeIf{it.isNotBlank()}?.toDouble(),notes,exercises),done,plannedDate)}catch(e:Exception){error=e.message?:"Check workout values."}})
    }
}
