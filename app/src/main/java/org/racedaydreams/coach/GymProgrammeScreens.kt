@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.racedaydreams.coach
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.*
import org.racedaydreams.domain.strength.*

@Composable fun GymProgrammeScreen(state:CoachUiState,model:CoachViewModel,open:(String)->Unit){
    var setup by rememberSaveable{mutableStateOf(false)};var preview by remember{mutableStateOf<GymProgramme?>(null)};var error by rememberSaveable{mutableStateOf<String?>(null)}
    val current=state.gymProgramme
    var goal by rememberSaveable{mutableStateOf(current?.settings()?.goal()?.name?:"SUPPORT_RUNNING")};var priority by rememberSaveable{mutableStateOf(current?.settings()?.priority()?.name?:"RUNNING_FIRST")};var experience by rememberSaveable{mutableStateOf(current?.settings()?.experience()?.name?:"BEGINNER")};var minutes by rememberSaveable{mutableIntStateOf(current?.settings()?.minutes()?:45)};var frequency by rememberSaveable{mutableIntStateOf(current?.settings()?.frequency()?:2)};var effort by rememberSaveable{mutableStateOf(current?.settings()?.effort()?.name?:"RPE")};var days by rememberSaveable{mutableStateOf((current?.settings()?.days()?:state.profile?.preferences()?.gymDays()?:emptySet()).map{it.value})};var equipment by rememberSaveable{mutableStateOf((current?.settings()?.equipment()?:setOf("bodyweight")).toList())}
    CoachPage{
        CoachHeading("Gym programme")
        if(current==null&&!setup){Text("No generated gym programme. Manual logging, PBs and running context work independently.");CoachButton("Create a gym programme"){setup=true};TextButton(onClick={model.strength.disableGym()}){Text("I manage my own strength training")}}
        if(!setup&&state.gymVersions.isNotEmpty()){
            var history by rememberSaveable{mutableStateOf(false)};TextButton(onClick={history=!history}){Text(if(history)"Hide preserved versions"else "View preserved versions")}
            if(history)state.gymVersions.take(20).forEach{version->CoachCard{Text("Version ${version.version()} · ${friendlyDate(version.start())}",style=MaterialTheme.typography.titleMedium);Text(version.reason());var details by rememberSaveable(version.id(),version.version()){mutableStateOf(false)};TextButton(onClick={details=!details}){Text(if(details)"Hide prescriptions"else "View original prescriptions")};if(details)version.sessions().forEach{session->Text("${friendlyDate(session.date())} · ${session.title()}");session.exercises().forEach{e->Text("${ExerciseCatalog.definition(e.exerciseId())?.exercise()?.name()?:"Archived exercise"} · ${prescription(e,version.settings().effort())}")}}}}
        }
        if(current!=null&&!setup){Text("Version ${current.version()} · ${friendlyDate(current.start())} – ${friendlyDate(current.end())}");Text(label(current.settings().goal().name));WhyDetails(current.reason(),"Why this version?");current.tradeoffs().takeIf{it.isNotEmpty()}?.let{WhyDetails(it.joinToString("\n"),"Scheduling trade-offs")};current.sessions().filter{!it.date().isBefore(LocalDate.now())}.take(8).forEach{s->Card(onClick={open(s.id())},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("${friendlyDate(s.date())} · ${s.title()}");Text("${s.exercises().size} exercises")}}};CoachButton("Change programme settings"){setup=true};TextButton(onClick={model.strength.disableGym()}){Text("Stop generated programming")};Text("${state.gymVersions.size} preserved block versions")}
        if(setup){
            CoachHeading("Your gym preferences")
            Text("Goal");ChoiceChips(GymProgramme.Goal.values().map{it.name},goal){goal=it}
            Text("Priority");ChoiceChips(GymProgramme.Priority.values().map{it.name},priority){priority=it}
            Text("Experience");ChoiceChips(GymProgramme.Experience.values().map{it.name},experience){experience=it}
            Text("Sessions per week");ChoiceChips((1..6).map{it.toString()},frequency.toString()){frequency=it.toInt()}
            Text("Available gym days");FlowRow{DayOfWeek.values().forEach{day->FilterChip(selected=day.value in days,onClick={days=if(day.value in days)days-day.value else days+day.value},label={Text(day.getDisplayName(java.time.format.TextStyle.SHORT,java.util.Locale.UK))})}}
            Text("Equipment");FlowRow{listOf("bodyweight","dumbbells","barbell","squat rack","bench","machines","cables","kettlebells","pull-up bar","trap bar","sled","full gym").forEach{e->FilterChip(selected=e in equipment,onClick={equipment=if(e in equipment)equipment-e else equipment+e},label={Text(e)})}}
            Text("Session length");ChoiceChips(listOf("30","45","60","75"),minutes.toString()){minutes=it.toInt()}
            Text("Effort guidance");ChoiceChips(listOf("RPE","RIR"),effort){effort=it}
            error?.let{Text(it)}
            CoachButton("Preview six-week block"){try{val settings=GymProgramme.Settings(GymProgramme.Goal.valueOf(goal),GymProgramme.Priority.valueOf(priority),GymProgramme.Experience.valueOf(experience),days.map{DayOfWeek.of(it)}.toSet(),frequency,equipment.toSet(),minutes,GymProgramme.Effort.valueOf(effort));preview=model.strength.previewGenerated(settings);error=null}catch(e:Exception){error=e.message}}
            TextButton(onClick={setup=false}){Text("Cancel")}
        }
        preview?.let{p->CoachCard{Text("Preview — not applied",style=MaterialTheme.typography.titleLarge);Text("${p.sessions().size} sessions · ${label(p.settings().goal().name)}");p.sessions().take(p.settings().frequency()).forEach{s->Text("${friendlyDate(s.date())} · ${s.title()}");s.exercises().forEach{e->Text("${ExerciseCatalog.definition(e.exerciseId())?.exercise()?.name()} · ${prescription(e,p.settings().effort())}")}};p.tradeoffs().forEach{Text(it)};CoachButton("Accept this programme"){model.strength.saveGym(p);preview=null;setup=false};TextButton(onClick={preview=null}){Text("Keep current programme")}}}
    }
}
@Composable private fun ChoiceChips(options:List<String>,selected:String,choose:(String)->Unit){FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){options.forEach{o->FilterChip(selected=selected==o,onClick={choose(o)},label={Text(label(o))})}}}
private fun label(value:String)=when(value){"SUPPORT_RUNNING"->"Support my running";"MUSCLE"->"Build muscle";"STRENGTH"->"Get stronger";"GENERAL"->"General strength & fitness";"POWER"->"Power / athletic performance";"RPE","RIR"->value;else->value.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()}}
private fun prescription(e:GymProgramme.Prescription,effort:GymProgramme.Effort):String{val measure=if(e.seconds()!=null)"${e.seconds()} sec"else if(e.metres()!=null)"${e.metres()} m"else "${e.minimumReps()}–${e.maximumReps()} reps";val intensity=if(effort==GymProgramme.Effort.RIR&&e.minimumReps()>0)"~${10-e.maximumRpe()}–${10-e.minimumRpe()} RIR"else "RPE ~${e.minimumRpe()}–${e.maximumRpe()}";return "${e.sets()} × $measure · $intensity · ${e.restSeconds()/60.0} min rest"}
@Composable fun GymSessionScreen(state:CoachUiState,model:CoachViewModel,id:String?,log:(String)->Unit){
    val p=state.gymProgramme?:return;val session=p.sessions().firstOrNull{it.id()==id}?:return
    var progressionPreview by rememberSaveable{mutableStateOf<String?>(null)}
    var swapFor by rememberSaveable{mutableStateOf<String?>(null)};var swapQuery by rememberSaveable{mutableStateOf("")}
    var move by rememberSaveable{mutableStateOf(session.date().toString())};var proposed by remember{mutableStateOf<GymProgramme?>(null)};var error by remember{mutableStateOf<String?>(null)}
    val completed=state.strengthPlannedDates.values.toSet()
    fun preview(change:()->GymProgramme){try{proposed=change();error=null}catch(e:Exception){error=e.message}}
    CoachPage{
        CoachHeading(session.title());Text(friendlyDate(session.date()));Text("Plan version ${p.version()}")
        val raceDate=state.profile?.raceGoal()?.raceDate()
        if(raceDate!=null&&java.time.temporal.ChronoUnit.DAYS.between(session.date(),raceDate) in 0L..7L)SemanticCard(Tile.Insight){Text("Race-week strength",style=MaterialTheme.typography.titleMedium);Text("Consider keeping familiar movements and reducing demanding volume. Use Preview fewer sets below if it suits your taper. Nothing changes without your choice.")}
        session.exercises().forEach { exercise ->
            CoachCard {
                Text(ExerciseCatalog.definition(exercise.exerciseId())?.exercise()?.name() ?: exercise.exerciseId(), style=MaterialTheme.typography.titleLarge)
                Text(prescription(exercise,p.settings().effort()))
                exercise.referenceLoadKg()?.let { kg ->
                    Text("Your optional load reference: ${decimal(state.massUnit.fromKilograms(kg))} ${state.massUnit.name.lowercase()}")
                }
                WhyDetails(exercise.intent(),"Why this exercise?")
                if(session.date()>=LocalDate.now() && session.date() !in completed) {
                    TextButton(onClick={swapFor=exercise.exerciseId();swapQuery=""}) { Text("Swap exercise") }
                }
                val initial=org.racedaydreams.domain.polish.StartingLoad.suggest(exercise,state.strengthSessions,Instant.now())
                initial?.let{Text("Optional starting reference: ${decimal(state.massUnit.fromKilograms(it.lowKg()))}–${decimal(state.massUnit.fromKilograms(it.highKg()))} ${state.massUnit.name.lowercase()}");WhyDetails(it.reason(),"Why this load reference?")}
                val advice=StrengthProgressionAdvisor.review(exercise,state.strengthSessions,Instant.now())
                Text(advice.reason())
                advice.suggestedKilograms()?.let { kg ->
                    Text("Optional next load: ~${decimal(state.massUnit.fromKilograms(kg))} ${state.massUnit.name.lowercase()}. Keep your current load if preferred.")
                }
                val version=org.racedaydreams.data.GymProgrammeStore.key(p)
                val decided=state.gymProgressionDecisions.lastOrNull {
                    it.prescribedSession()==session.id() && it.exerciseId()==exercise.exerciseId() &&
                        (it.baseVersion()==version || it.resultingVersion()==version)
                }
                if(decided!=null) {
                    Text(if(decided.choice()=="KEEP") "You chose to keep the current prescription." else "You chose this optional load reference.")
                } else if(advice.action()==StrengthProgressionAdvisor.Action.CONSIDER_INCREASE && session.date()>=LocalDate.now() && session.date() !in completed) {
                    TextButton(onClick={progressionPreview=exercise.exerciseId()}) { Text("Preview load suggestion") }
                    TextButton(onClick={model.strength.gymProgression(version,session.id(),exercise.exerciseId(),false)}) { Text("Keep current load") }
                }
            }
        }
        WhyDetails(session.explanation(),"Why this session?")
        if(session.date()<=LocalDate.now())CoachButton("Record actual strength workout"){log(session.date().toString())}
        if(session.date()>=LocalDate.now()&&session.date() !in completed){DateInput("Move this session to",move,{move=it});CoachButton("Preview one-off move"){preview{model.strength.previewMove(p,session.id(),LocalDate.parse(move))}};OutlinedButton(onClick={preview{model.strength.previewFewerSets(p,session.id())}}){Text("Preview fewer sets")};TextButton(onClick={preview{model.strength.previewSkip(p,session.id())}}){Text("Skip this session")}}
        error?.let{Text(it)}
        proposed?.let{next->CoachCard{Text("Change preview",style=MaterialTheme.typography.titleLarge);Text(next.reason());val changed=next.sessions().firstOrNull{it.id()==session.id()};Text("Current: ${friendlyDate(session.date())} · ${session.exercises().sumOf{it.sets()}} working sets");Text(changed?.let{"Proposed: ${friendlyDate(it.date())} · ${it.exercises().sumOf{e->e.sets()}} working sets"}?:"Proposed: session skipped");changed?.let{model.strength.conflicts(it).forEach{Text(it)}};Text("Other sessions and recurring availability stay unchanged.");CoachButton("Accept change"){model.strength.saveGym(next);proposed=null};TextButton(onClick={proposed=null}){Text("Keep original")}}}
    }
    progressionPreview?.let{exerciseId->val e=session.exercises().first{it.exerciseId()==exerciseId};val advice=StrengthProgressionAdvisor.review(e,state.strengthSessions,Instant.now());AlertDialog(onDismissRequest={progressionPreview=null},title={Text("Load suggestion — not applied")},text={Column{Text("Previous evidence: ${advice.previousKilograms()?.let{decimal(state.massUnit.fromKilograms(it))}?:"Unknown"} ${state.massUnit.name.lowercase()}");Text("Proposed reference: ${advice.suggestedKilograms()?.let{decimal(state.massUnit.fromKilograms(it))}?:"Unavailable"} ${state.massUnit.name.lowercase()}");Text(advice.reason());Text("Rep ranges, other sessions and actual workout history stay unchanged. You can still use a different load when logging.")}},confirmButton={TextButton(onClick={model.strength.gymProgression(org.racedaydreams.data.GymProgrammeStore.key(p),session.id(),exerciseId,true);progressionPreview=null}){Text("Accept optional reference")}},dismissButton={TextButton(onClick={progressionPreview=null}){Text("Keep original")}})}
    swapFor?.let{from->
        val alternatives=model.strength.alternatives(p,from,swapQuery)
        AlertDialog(onDismissRequest={swapFor=null},title={Text("Choose an alternative")},text={Column{CoachInput("Search alternatives",swapQuery,{swapQuery=it});if(alternatives.isEmpty())Text("No matching alternative for this equipment and movement category.");androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=300.dp)){items(alternatives.size){i->TextButton(onClick={preview{model.strength.previewSwap(p,session.id(),from,alternatives[i].id())};swapFor=null}){Text(alternatives[i].name())}}}}},confirmButton={TextButton(onClick={swapFor=null}){Text("Cancel")}})
    }
}
