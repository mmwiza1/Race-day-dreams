package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
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

@Composable fun WeeklyReviewScreen(s:CoachUiState,model:CoachViewModel)=CoachPage{
    CoachHeading("Weekly coaching review")
    val r=s.weeklyReview
    if(r==null)Text("Create a programme to review its completed weeks.") else{
        Text("Week of ${friendlyDate(r.week())}")
        CoachCard{Text("${r.completed()} / ${r.planned()} planned runs completed");Text("${decimal(r.kilometres())} km · ${r.strengthSessions()} strength sessions");r.longestKm()?.let{Text("Longest run: ${decimal(it)} km")}}
        CoachHeading("What we noticed");r.observations().forEach{Text(it)}
        WhyDetails(r.channels().joinToString("\n\n"){"${it.name()}: ${it.state().name.lowercase().replace('_',' ')}\n${it.reason()}"},"Evidence channels")
        CoachHeading("Recommendation");Text(r.recommendation())
        if(s.weeklyDecision!=null)Text("Your decision: ${s.weeklyDecision}. Your choice is retained.")else r.choices().forEach{c->
            if(c.replacements().isNotEmpty())WhyDetails(c.replacements().joinToString("\n"){"${shortDate(it.date())}: ${it.category().name.lowercase().replace('_',' ')} · ${it.distanceKm()?.let{d->"${decimal(d)} km"}?:it.duration()?.toMinutes()?.let{d->"$d min"}?:"Effort guidance"}"},"Preview ${c.label().lowercase()}")
            OutlinedButton(onClick={model.weeklyDecision(c.id())},enabled=!s.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(c.label())}
        }
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
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Week","Month","3 Months","Year","All").forEach{r->TextButton(onClick={range=r},modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp)){Text(if(range==r)"[$r]"else r)}}}
    val threshold=when(range){"Week"->LocalDate.now().minusWeeks(1);"Month"->LocalDate.now().minusMonths(1);"3 Months"->LocalDate.now().minusMonths(3);"Year"->LocalDate.now().minusYears(1);else->LocalDate.MIN}
    val points=s.weights.filter{it.at().atOffset(it.offset()).toLocalDate()>=threshold}
    val latest=s.weights.lastOrNull();latest?.let{Text("Latest: ${decimal(s.massUnit.fromKilograms(it.kilograms()))} ${s.massUnit.name.lowercase()} · ${friendlyDate(it.at().atOffset(it.offset()).toLocalDate())}")}
    if(points.size>=2)Text("Change in selected period: ${decimal(s.massUnit.fromKilograms(points.last().kilograms()-points.first().kilograms()))} ${s.massUnit.name.lowercase()}")
    val description=if(points.isEmpty())"No measurements in this range."else points.joinToString("; "){"${shortDate(it.at().atOffset(it.offset()).toLocalDate())}: ${decimal(s.massUnit.fromKilograms(it.kilograms()))} ${s.massUnit.name.lowercase()}"}
    val colour=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(150.dp).semantics{contentDescription="Weight graph. $description. Lines join recorded points; there are no daily estimates."}){
        if(points.isNotEmpty()){
            val min=points.minOf{it.kilograms()};val max=points.maxOf{it.kilograms()};val first=points.first().at().toEpochMilli();val span=(points.last().at().toEpochMilli()-first).coerceAtLeast(1)
            val coords=points.map{Offset(10f+(size.width-20)*(it.at().toEpochMilli()-first).toFloat()/span,10f+(size.height-20)*(1-((it.kilograms()-min)/(max-min).coerceAtLeast(1.0))).toFloat())}
            coords.zipWithNext().forEach{(a,b)->drawLine(colour,a,b,3f)};coords.forEach{drawCircle(colour,5f,it)}
        }
    }
    WhyDetails(description,"Recorded measurements")
    Text("Weight is a neutral personal trend. It does not determine your coaching recommendations.")
    DateInput("Measurement date",date,{date=it});CoachInput("Weight (${s.massUnit.name.lowercase()})",value,{value=it},true)
    error?.let{Text(it)}
    CoachButton(if(editId==null)"Add weight" else "Save measurement",{try{require(value.toDouble()>0);model.saveWeight(value.toDouble(),LocalDate.parse(date),editId);value="";editId=null;error=null}catch(e:Exception){error="Enter a positive weight."}})
    points.takeLast(8).reversed().forEach{w->CoachCard{Text("${friendlyDate(w.at().atOffset(w.offset()).toLocalDate())} · ${decimal(s.massUnit.fromKilograms(w.kilograms()))} ${s.massUnit.name.lowercase()}");Text(if(w.provider()=="manual")"Manual entry"else "Health Connect")
        if(w.provider()=="manual")Row{TextButton(onClick={editId=w.id();value=decimal(s.massUnit.fromKilograms(w.kilograms()));date=w.at().atOffset(w.offset()).toLocalDate().toString()}){Text("Edit")};TextButton(onClick={model.deleteWeight(w.id())}){Text("Delete")}}
    }}
}
