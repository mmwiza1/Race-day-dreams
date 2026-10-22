package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.time.*
import org.racedaydreams.domain.body.MassUnit

@Composable fun WeeklyReviewScreen(s:CoachUiState,model:CoachViewModel,editRoutine:()->Unit = {})=CoachPage{
    CoachHeading("Weekly coaching review")
    val r=s.weeklyReview
    if(r==null)Text("Create a programme to review its completed weeks.") else{
        Text("Week of ${friendlyDate(r.week())}")
        CoachCard{Text("${r.completed()} / ${r.planned()} planned runs completed");Text("${decimal(r.kilometres())} km · ${r.strengthSessions()} strength sessions");r.longestKm()?.let{Text("Longest run: ${decimal(it)} km")}}
        val presentation=org.racedaydreams.domain.presentation.WeeklyReviewData.of(r)
        CoachHeading("What went well")
        if(presentation.positives().isEmpty())Text("More recorded training will help us see your response.")else presentation.positives().forEach{Text("✓ $it")}
        if(presentation.attention().isNotEmpty()){CoachHeading("What needs attention");presentation.attention().forEach{Text("△ $it")}}
        WhyDetails(r.observations().joinToString("\n\n"),"Week observations")
        if(r.observations().any{it.contains("missed or moved")})TextButton(onClick=editRoutine){Text("Review training days")}
        WhyDetails(r.channels().joinToString("\n\n"){"${it.name()}: ${it.state().name.lowercase().replace('_',' ')}\n${it.reason()}"},"Evidence channels")
        CoachHeading("Recommendation")
        CoachCard{Text(if(presentation.noChange())"No adjustment required"else "Review the next step",style=MaterialTheme.typography.titleMedium);Text(if(presentation.noChange())"Your recent training supports continuing the current plan."else r.recommendation())}
        var previewId by rememberSaveable(r.id()){mutableStateOf<String?>(null)}
        val preview=r.choices().firstOrNull{it.id()==previewId}
        if(s.weeklyDecision!=null)Text("Your decision: ${s.weeklyDecision}. Your choice is retained.")else r.choices().forEach{c->
            OutlinedButton(onClick={if(c.replacements().isEmpty())model.weeklyDecision(c.id())else previewId=c.id()},enabled=!s.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(c.label())}
        }
        if(preview!=null)AlertDialog(onDismissRequest={previewId=null},title={Text("Preview plan change")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(preview.id()=="paces")"Optional quality-pace reassessment"else r.recommendation())
            r.channels().firstOrNull{it.name()=="Performance trend"}?.takeIf{preview.id()=="paces"}?.let{Text(it.reason().substringBefore("."));WhyDetails(it.reason(),"Why these pace ranges?")}
            preview.replacements().forEach{next->
                val current=s.currentPlan?.plan()?.workouts()?.firstOrNull{it.id()==next.id()}
                Text(shortDate(next.date()),style=MaterialTheme.typography.titleSmall)
                Text("Current: ${current?.let{workoutSummary(it,s)}?:"Preserved workout unavailable"}")
                Text("Proposed: ${workoutSummary(next,s)}")
            }
            Text("Other sessions stay unchanged. Your race goal stays unchanged.")
        }},confirmButton={TextButton(onClick={model.weeklyDecision(preview.id());previewId=null},enabled=!s.busy){Text("Accept change")}},dismissButton={TextButton(onClick={previewId=null}){Text("Keep original plan")}})

    }
    var report by rememberSaveable{mutableStateOf(false)}
    TextButton(onClick={report=!report}){Text("Report a training break")}
    if(report){var from by rememberSaveable{mutableStateOf(LocalDate.now().minusWeeks(2).toString())};var returned by rememberSaveable{mutableStateOf(LocalDate.now().toString())};var reason by rememberSaveable{mutableStateOf("Life / travel")}
        DateInput("Break began",from,{from=it});DateInput("Returning",returned,{returned=it});CoachInput("Reason",reason,{reason=it});Text("This is return-to-training guidance, not medical clearance. If you have symptoms or injury concerns, seek appropriate advice.")
        CoachButton("Review my return",{model.reportInterruption(LocalDate.parse(from),LocalDate.parse(returned),reason);report=false})
    }
}

@Composable fun WeightSection(s:CoachUiState,model:CoachViewModel){
    var range by rememberSaveable{mutableStateOf("Month")};var value by rememberSaveable{mutableStateOf("")};var date by rememberSaveable{mutableStateOf(LocalDate.now().toString())};var editId by rememberSaveable{mutableStateOf<String?>(null)};var error by rememberSaveable{mutableStateOf<String?>(null)}
    CoachHeading("Body weight")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MassUnit.entries.forEach{u->FilterChip(selected=s.massUnit==u,onClick={model.setting("mass.unit",u.name)},label={Text(u.name.lowercase())})}}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Week","Month","3 Months","Year","All").forEach{r->FilterChip(selected=range==r,onClick={range=r},modifier=Modifier.weight(1f).heightIn(min=48.dp),label={Text(r)})}}
    val graph=remember(s.weights,range,s.massUnit){org.racedaydreams.domain.presentation.WeightGraphData.of(s.weights,LocalDate.now(),range,s.massUnit)}
    val points=graph.points()
    var selectedId by rememberSaveable(range){mutableStateOf<String?>(null)}
    val selected=points.firstOrNull{it.id()==selectedId}
    val latest=s.weights.maxByOrNull{it.at()};latest?.let{Text("Latest: ${decimal(s.massUnit.fromKilograms(it.kilograms()))} ${s.massUnit.name.lowercase()} · ${friendlyDate(it.at().atOffset(it.offset()).toLocalDate())}")}
    Text("$range · ${points.size} recorded measurements",style=MaterialTheme.typography.titleSmall)
    if(points.size>=2)Text("Change in selected period: ${decimal(s.massUnit.fromKilograms(points.last().kilograms()-points.first().kilograms()))} ${s.massUnit.name.lowercase()}")
    val description=if(points.isEmpty())"No measurements in this range."else points.joinToString("; "){"${shortDate(it.at().atOffset(it.offset()).toLocalDate())}: ${decimal(s.massUnit.fromKilograms(it.kilograms()))} ${s.massUnit.name.lowercase()}"}
    val colour=MaterialTheme.colorScheme.primary;val grid=MaterialTheme.colorScheme.outlineVariant
    if(points.isEmpty())Text(description)else{
        Row(Modifier.fillMaxWidth().height(168.dp)){
            Column(Modifier.width(60.dp).fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween){listOf(graph.maximum(),(graph.maximum()+graph.minimum())/2,graph.minimum()).forEach{Text(decimal(it),style=MaterialTheme.typography.labelSmall)}}
            Canvas(Modifier.weight(1f).fillMaxHeight().pointerInput(graph){detectTapGestures{tap->selectedId=graph.nearest(((tap.x-8f)/(size.width-16).coerceAtLeast(1)).toDouble(),((tap.y-8f)/(size.height-16).coerceAtLeast(1)).toDouble())?.id()}}.semantics{contentDescription="Weight graph in ${s.massUnit.name.lowercase()}. $description. Tap to inspect a recorded point. Lines connect actual measurements, not daily estimates."}){
                listOf(8f,size.height/2,size.height-8f).forEach{y->drawLine(grid,Offset(8f,y),Offset(size.width-8f,y),1.dp.toPx())}
                val coords=points.indices.map{Offset(8f+(size.width-16)*graph.x(it).toFloat(),8f+(size.height-16)*graph.y(it).toFloat())}
                coords.zipWithNext().forEach{(a,b)->drawLine(colour.copy(alpha=.5f),a,b,2.dp.toPx())}
                coords.forEachIndexed{i,at->drawCircle(colour,if(points[i].id()==selectedId)7.dp.toPx()else 4.dp.toPx(),at)}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(shortDate(points.first().at().atOffset(points.first().offset()).toLocalDate()),style=MaterialTheme.typography.labelSmall);Text(shortDate(points.last().at().atOffset(points.last().offset()).toLocalDate()),style=MaterialTheme.typography.labelSmall)}
        Text("${s.massUnit.name.lowercase()} · Tap a point for details",style=MaterialTheme.typography.labelSmall)
        selected?.let{CoachCard{Text("Recorded measurement",style=MaterialTheme.typography.titleSmall);Text("${shortDate(it.at().atOffset(it.offset()).toLocalDate())} · ${decimal(s.massUnit.fromKilograms(it.kilograms()))} ${s.massUnit.name.lowercase()}")}}
    }
    WhyDetails(description,"Recorded measurements")
    Text("Weight is a neutral personal trend. It does not determine your coaching recommendations.")
    var adding by rememberSaveable{mutableStateOf(false)}
    TextButton(onClick={adding=!adding}){Text(if(adding)"Hide weight form" else "Record a measurement")}
    if(adding){ DateInput("Measurement date",date,{date=it});CoachInput("Weight (${s.massUnit.name.lowercase()})",value,{value=it},true)
    error?.let{Text(it)}
    CoachButton(if(editId==null)"Add weight" else "Save measurement",{try{require(value.toDouble()>0);model.saveWeight(value.toDouble(),LocalDate.parse(date),editId);value="";editId=null;error=null}catch(e:Exception){error="Enter a positive weight."}})
    }
    points.takeLast(3).reversed().forEach{w->CoachCard{Text("${friendlyDate(w.at().atOffset(w.offset()).toLocalDate())} · ${decimal(s.massUnit.fromKilograms(w.kilograms()))} ${s.massUnit.name.lowercase()}");Text(org.racedaydreams.domain.activity.ProviderReconciliation.friendly(org.racedaydreams.domain.activity.SourceIdentity(w.provider(),w.sourceId(),w.origin())))
        if(w.provider()=="manual")Row{TextButton(onClick={adding=true;editId=w.id();value=decimal(s.massUnit.fromKilograms(w.kilograms()));date=w.at().atOffset(w.offset()).toLocalDate().toString()}){Text("Edit")};TextButton(onClick={model.deleteWeight(w.id())}){Text("Delete")}}
    }}
}

private fun workoutSummary(w:org.racedaydreams.domain.PlannedWorkout,s:CoachUiState):String {
    val factor=if(s.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL)1.609344 else 1.0
    val unit=if(factor>1)"mi"else "km"
    return org.racedaydreams.domain.presentation.CalendarData.label(w.category())+(w.distanceKm()?.let{" · ${formatDistance(it*1000,s)}"}?:w.duration()?.let{" · ${it.toMinutes()} min"}?:"")+w.segments().filter{it.minimumSecondsPerKm()!=null}.joinToString(""){" · ${formatPaceSeconds(it.minimumSecondsPerKm()*factor)}–${formatPaceSeconds((it.maximumSecondsPerKm()?:it.minimumSecondsPerKm())*factor)}/$unit"}
}
