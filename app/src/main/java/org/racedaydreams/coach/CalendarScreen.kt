package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.temporal.TemporalAdjusters
import org.racedaydreams.domain.coaching.AdaptiveCoachingEngine

@Composable fun ActivityCalendarScreen(state:CoachUiState,model:CoachViewModel,workout:(String)->Unit,strength:(String)->Unit,activity:(String)->Unit){
    var focus by rememberSaveable{mutableStateOf(LocalDate.now().toString())};val day=LocalDate.parse(focus)
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("List","Week","Month").forEach{view->FilterChip(selected=state.calendarView==view,onClick={model.setting("calendar.view",view)},label={Text(view)})}}
        if(state.calendarView=="List"){ActivityHistoryScreen(state,activity);return@Column}
        CoachPage{
            CoachHeading("Training calendar")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={focus=(if(state.calendarView=="Month")day.minusMonths(1)else day.minusWeeks(1)).toString()}){Text("Previous")};TextButton(onClick={focus=LocalDate.now().toString()}){Text("Today")};TextButton(onClick={focus=(if(state.calendarView=="Month")day.plusMonths(1)else day.plusWeeks(1)).toString()}){Text("Next")}}
            Text(if(state.calendarView=="Month")day.format(java.time.format.DateTimeFormatter.ofPattern("MMMM uuuu",java.util.Locale.UK))else "Week of ${friendlyDate(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))}")
            Text("Dots count sessions; tap a date for labels. Legend: E Easy · Q Quality · L Long · S Strength · R Race. ✓ Completed · ○ Planned. Labels accompany every marker.")
            if(state.calendarView=="Month"){
                val first=day.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                Row(Modifier.fillMaxWidth()){listOf("M","T","W","T","F","S","S").forEach{Text(it,Modifier.weight(1f))}}
                repeat(6){week->Row(Modifier.fillMaxWidth()){repeat(7){i->val date=first.plusDays((week*7+i).toLong());val count=calendarLabels(state,date).size
                    TextButton(onClick={focus=date.toString()},modifier=Modifier.weight(1f).heightIn(min=56.dp).semantics{contentDescription="${friendlyDate(date)}, $count sessions"},contentPadding=PaddingValues(0.dp)){Column{Text("${date.dayOfMonth}");if(count>0)Text("•".repeat(count.coerceAtMost(3)))}}
                }}}
                CalendarDay(state,day,workout,strength,activity)
            }else repeat(7){CalendarDay(state,day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(it.toLong()),workout,strength,activity)}
        }
    }
}
private fun runDate(a:org.racedaydreams.domain.activity.ActivityRecord)=a.start().atOffset(a.startOffset()?:ZoneOffset.UTC).toLocalDate()
private fun strengthDate(a:org.racedaydreams.domain.strength.StrengthSession)=a.startedAt().atOffset(a.offset()).toLocalDate()
private fun calendarLabels(s:CoachUiState,d:LocalDate):List<String>{val out=mutableListOf<String>();s.activities.filter{runDate(it)==d}.forEach{out+="Completed run"};s.plannedWorkouts.filter{it.date()==d&&!s.activities.any{a->a.plannedWorkoutId()==it.id()}}.forEach{out+=it.category().name};s.strengthSessions.filter{strengthDate(it)==d}.forEach{out+="Strength"};if(s.profile?.preferences()?.gymDays()?.contains(d.dayOfWeek)==true&&s.strengthSessions.none{strengthDate(it)==d})out+="Planned strength";return out}
@Composable private fun CalendarDay(s:CoachUiState,d:LocalDate,workout:(String)->Unit,strength:(String)->Unit,activity:(String)->Unit){CoachCard{
    Text("${d.dayOfWeek.name.lowercase().replaceFirstChar{it.uppercase()}} · ${friendlyDate(d)}",style=MaterialTheme.typography.titleSmall)
    s.activities.filter{runDate(it)==d}.forEach{a->TextButton(onClick={activity(a.id())}){Text("✓ Run · ${a.distanceMetres()?.let{formatDistance(it,s)}?:"Distance unavailable"}")}}
    s.plannedWorkouts.filter{it.date()==d}.forEach{w->val done=s.activities.any{it.plannedWorkoutId()==w.id()};TextButton(onClick={workout(w.id())}){Text("${if(done)"✓" else "○"} ${w.category().name.replace('_',' ').lowercase()}${w.distanceKm()?.let{" · ${decimal(it)} km"}?:""}${if(!done&&d<LocalDate.now())" · Not completed"else ""}")}}
    s.strengthSessions.filter{strengthDate(it)==d}.forEach{Text("✓ Strength · ${it.duration().toMinutes()} min")}
    if(s.profile?.preferences()?.gymDays()?.contains(d.dayOfWeek)==true&&s.strengthSessions.none{strengthDate(it)==d}&&s.strengthPlannedDates.values.none{it==d})TextButton(onClick={strength(d.toString())}){Text("○ Planned strength · Log actual workout")}
    s.strengthPlannedDates.filterValues{it==d}.forEach{(id,_)->s.strengthSessions.firstOrNull{it.id()==id}?.let{Text("✓ Strength routine logged · ${friendlyDate(strengthDate(it))}")}}
    s.workoutDecisions.filter{it.originalDate()==d}.forEach{decision->Text(if(decision.kind()==org.racedaydreams.domain.coaching.WorkoutDecision.Kind.SKIPPED) "Workout skipped by choice" else "Workout moved to ${decision.movedTo()?.let(::friendlyDate)}")}
    if(calendarLabels(s,d).isEmpty())Text("Rest / no session planned")
}}
