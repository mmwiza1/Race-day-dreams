package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.temporal.TemporalAdjusters
import org.racedaydreams.domain.presentation.CalendarData

fun calendarEntries(s:CoachUiState,d:LocalDate,today:LocalDate=LocalDate.now()):List<CalendarData.Entry>{
    val base=CalendarData.day(d,today,s.currentPlan,s.currentPlan?.plan()?.workouts()?:s.plannedWorkouts,s.activities,s.strengthSessions,s.strengthPlannedDates,s.workoutDecisions)
    val entries=CalendarData.withGym(d,today,base,s.gymProgramme,s.gymLinks.values.associate{it.prescribedSessionId() to it.actualSessionId()})
    return if(s.gymProgramme==null&&s.currentPlan==null&&s.profile?.preferences()?.gymDays()?.contains(d.dayOfWeek)==true&&entries.none{it.category()==CalendarData.Category.STRENGTH}&&s.strengthPlannedDates.values.none{it==d})entries+CalendarData.Entry(CalendarData.Category.STRENGTH,CalendarData.State.PLANNED,"Strength routine",null,null)else entries
}
private fun markerLetter(c:CalendarData.Category)=when(c){CalendarData.Category.REST->"–";CalendarData.Category.RUN->"U";else->c.name.take(1)}
private fun markerName(c:CalendarData.Category)=when(c){CalendarData.Category.RUN->"Unclassified run";else->c.name.lowercase().replaceFirstChar{it.uppercase()}}
@Composable private fun markerColour(c:CalendarData.Category):Color {val dark=isSystemInDarkTheme();return when(c){CalendarData.Category.EASY->if(dark)Color(0xFF80D8A0)else Color(0xFF206839);CalendarData.Category.QUALITY->if(dark)Color(0xFF9BC9FF)else Color(0xFF175FA6);CalendarData.Category.LONG->if(dark)Color(0xFFFFC078)else Color(0xFF955000);CalendarData.Category.STRENGTH->if(dark)Color(0xFFDDB5FF)else Color(0xFF78469E);CalendarData.Category.RACE->if(dark)Color(0xFFFFADA7)else Color(0xFFB12F32);else->if(dark)Color(0xFFCBCBCB)else Color(0xFF5E6268)}}
@Composable fun CalendarMarker(entry:CalendarData.Entry){val colour=markerColour(entry.category());val filled=entry.state()==CalendarData.State.COMPLETED
    Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(20.dp).semantics{contentDescription=entry.description()},contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){if(filled)drawCircle(colour)else drawCircle(colour,style=Stroke(1.5.dp.toPx()))}
        Text(markerLetter(entry.category()),fontSize=10.sp,color=if(filled)MaterialTheme.colorScheme.surface else colour,modifier=Modifier.clearAndSetSemantics{})
    }
        val status=when(entry.state()){CalendarData.State.MOVED->"↗";CalendarData.State.SKIPPED->"−";CalendarData.State.NOT_COMPLETED->"?";else->null}
        status?.let{Text(it,fontSize=9.sp,modifier=Modifier.clearAndSetSemantics{})}
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun CalendarLegend(){
    var expanded by rememberSaveable{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        TextButton(onClick={expanded=!expanded},modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(0.dp)){Text(if(expanded)"Hide calendar key"else "Calendar key ⓘ")}
        if(expanded) {
        FlowRow(Modifier.semantics{contentDescription="Workout category legend"},horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            CalendarData.Category.entries.forEach{c->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){CalendarMarker(CalendarData.Entry(c,CalendarData.State.PLANNED,markerName(c),null,null));Text(markerName(c),style=MaterialTheme.typography.labelMedium)}}
        }
        Text("Legend: outline planned · filled completed",style=MaterialTheme.typography.labelMedium)
        Text("↗ Moved · − Skipped · ? Not completed",style=MaterialTheme.typography.labelSmall)
        if(expanded)Text("Rest means an available day left free within your programme. Dates outside it and unavailable dates are not rest. U is an unclassified run; no intensity is inferred.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun ActivityCalendarScreen(state:CoachUiState,model:CoachViewModel,workout:(String)->Unit,strength:(String)->Unit,activity:(String)->Unit,allStrength:()->Unit={}){
    var focus by rememberSaveable{mutableStateOf(LocalDate.now().toString())};var showDay by rememberSaveable{mutableStateOf(false)};val day=LocalDate.parse(focus)
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("List","Week","Month").forEach{view->FilterChip(selected=state.calendarView==view,onClick={model.setting("calendar.view",view)},label={Text(view)})}}
        if(state.calendarView=="List"){ActivityHistoryScreen(state,activity,allStrength);return@Column}
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            CoachHeading("Training calendar")
            CalendarLegend()
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={focus=(if(state.calendarView=="Month")day.minusMonths(1)else day.minusWeeks(1)).toString()}){Text("Previous")};TextButton(onClick={focus=LocalDate.now().toString()}){Text("Today")};TextButton(onClick={focus=(if(state.calendarView=="Month")day.plusMonths(1)else day.plusWeeks(1)).toString()}){Text("Next")}}
            Text(if(state.calendarView=="Month")day.format(java.time.format.DateTimeFormatter.ofPattern("MMMM uuuu",java.util.Locale.UK))else "Week of ${friendlyDate(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))}",style=MaterialTheme.typography.titleMedium)
            val start=if(state.calendarView=="Month")day.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))else day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val entries=remember(state,start,state.calendarView){(0 until if(state.calendarView=="Month")42 else 7).associate{val d=start.plusDays(it.toLong());d to calendarEntries(state,d)}}
            if(state.calendarView=="Month"){
                Row(Modifier.fillMaxWidth()){listOf("M","T","W","T","F","S","S").forEach{Text(it,Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
                repeat(6){week->Row(Modifier.fillMaxWidth()){repeat(7){i->val date=start.plusDays((week*7+i).toLong());val sessions=entries[date].orEmpty();val outside=CalendarData.outsideMonth(date,day)
                    val selected=date==day
                    TextButton(onClick={focus=date.toString();showDay=true},modifier=Modifier.weight(1f).heightIn(min=48.dp).then(if(selected)Modifier.border(2.dp,MaterialTheme.colorScheme.primary,MaterialTheme.shapes.small)else Modifier).semantics(mergeDescendants=true){contentDescription="${friendlyDate(date)}${if(date==LocalDate.now())", today"else ""}${if(selected)", selected"else ""}. "+if(sessions.isEmpty())"No session planned"else sessions.joinToString("; "){it.description()}},contentPadding=PaddingValues(2.dp)){
                        Column(horizontalAlignment=Alignment.CenterHorizontally){Text("${date.dayOfMonth}",color=if(outside)MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.6f)else MaterialTheme.colorScheme.onSurface,modifier=if(date==LocalDate.now())Modifier.background(MaterialTheme.colorScheme.primaryContainer,CircleShape).padding(4.dp)else Modifier)
                            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalArrangement=Arrangement.spacedBy(2.dp)){sessions.take(4).forEach{CalendarMarker(it)};if(sessions.size>4)Text("+${sessions.size-4}",style=MaterialTheme.typography.labelSmall)}
                        }
                    }
                }}}
                if(showDay)ModalBottomSheet(onDismissRequest={showDay=false}){
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)){CalendarDay(state,day,entries[day].orEmpty(),workout,strength,activity)}
                }
            }else repeat(7){val d=start.plusDays(it.toLong());CalendarDay(state,d,entries[d].orEmpty(),workout,strength,activity)}
        }
    }
}
@Composable private fun CalendarDay(s:CoachUiState,d:LocalDate,entries:List<CalendarData.Entry>,workout:(String)->Unit,strength:(String)->Unit,activity:(String)->Unit)=CoachCard{
    Text("${d.dayOfWeek.name.lowercase().replaceFirstChar{it.uppercase()}} · ${friendlyDate(d)}",style=MaterialTheme.typography.titleSmall)
    if(entries.isEmpty())Text("No session planned")
    entries.forEach{entry->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        CalendarMarker(entry)
        val status=when(entry.state()){CalendarData.State.COMPLETED->"Completed";CalendarData.State.NOT_COMPLETED->"Not completed";CalendarData.State.SKIPPED->"Skipped by choice";CalendarData.State.MOVED->"Moved";else->"Planned"}
        val volume=if(entry.category()==CalendarData.Category.STRENGTH||entry.category()==CalendarData.Category.REST)null else {
            val recorded=s.activities.firstOrNull{it.id()==entry.activityId()}
            val planned=(s.currentPlan?.plan()?.workouts()?:s.plannedWorkouts).firstOrNull{it.id()==entry.id()}
            recorded?.let{it.distanceMetres()?.let{metres->formatDistance(metres,s)}?:"${it.elapsedTime().toMinutes()} min"}?:planned?.let{it.distanceKm()?.let{km->formatDistance(km*1000,s)}?:it.duration()?.let{time->"${time.toMinutes()} min"}}
        }
        val text="${entry.label()}${volume?.let{" · $it"}?:""} · $status"
        when{entry.activityId()!=null->TextButton(onClick={activity(entry.activityId())},modifier=Modifier.heightIn(min=48.dp)){Text(text)}
            entry.category()==CalendarData.Category.STRENGTH&&entry.state()==CalendarData.State.PLANNED->TextButton(onClick={strength(d.toString())},modifier=Modifier.heightIn(min=48.dp)){Text("$text · Log workout")}
            entry.category()==CalendarData.Category.STRENGTH->Text(text)
            entry.id()!=null&&entry.state()!=CalendarData.State.SKIPPED->TextButton(onClick={workout(entry.id())},modifier=Modifier.heightIn(min=48.dp)){Text(text)}
            else->Text(text)
        }
    }}
}
