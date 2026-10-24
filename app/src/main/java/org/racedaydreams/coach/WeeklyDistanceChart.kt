package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.activity.RecordedWeek

/** One bar per calendar week; zero recorded runs and unknown distance stay distinct. */
@Composable fun WeeklyDistanceChart(weeks:List<RecordedWeek>,state:CoachUiState) {
    if(weeks.isEmpty()){Text("Record runs to see your weekly distance.");return}
    var selected by remember(weeks){mutableIntStateOf(weeks.lastIndex)}
    val largest=weeks.mapNotNull{it.distanceMetres()}.maxOrNull()?.coerceAtLeast(1000.0)?:1000.0
    val blue=MaterialTheme.colorScheme.primary;val grid=MaterialTheme.colorScheme.outlineVariant
    fun description(w:RecordedWeek)="${shortDate(w.weekStart())}: "+when {w.activityCount()==0->"No recorded runs";w.distanceMetres()==null->"Distance unavailable";else->formatDistance(w.distanceMetres(),state)+" known distance, ${w.activityCount()} runs"+(if(w.missingDistanceCount()>0)", ${w.missingDistanceCount()} without distance"else"")}
    Text("${formatDistance(largest,state)}",style=MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics {
        contentDescription="Weekly running distance. "+weeks.joinToString("; ",transform=::description)
        customActions=weeks.mapIndexed{i,w->CustomAccessibilityAction("Select week of ${shortDate(w.weekStart())}"){selected=i;true}}
    }.pointerInput(weeks){detectTapGestures{position->selected=(position.x/size.width*weeks.size).toInt().coerceIn(weeks.indices)}}) {
        val cell=size.width/weeks.size;val baseline=size.height-8.dp.toPx()
        repeat(3){i->val y=baseline*(i+1)/3;drawLine(grid,Offset(0f,y),Offset(size.width,y),1.dp.toPx())}
        weeks.forEachIndexed{i,w->
            val value=w.distanceMetres();val x=i*cell+cell*.15f
            if(value!=null&&value>0){val height=(value/largest).toFloat()*baseline;drawRoundRect(blue.copy(alpha=if(i==selected)1f else .55f),Offset(x,baseline-height),Size(cell*.7f,height),androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))}
            else if(w.activityCount()>0) {drawLine(grid,Offset(x,baseline-8.dp.toPx()),Offset(x+cell*.7f,baseline),2.dp.toPx());drawLine(grid,Offset(x,baseline),Offset(x+cell*.7f,baseline-8.dp.toPx()),2.dp.toPx())}
            else drawCircle(grid,2.dp.toPx(),Offset(i*cell+cell/2,baseline))
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(shortDate(weeks.first().weekStart()),style=MaterialTheme.typography.labelSmall);Text(shortDate(weeks.last().weekStart()),style=MaterialTheme.typography.labelSmall)}
    Text(description(weeks[selected]),style=MaterialTheme.typography.titleSmall)
    weeks[selected].longestRunMetres()?.let{Text("Longest: ${formatDistance(it,state)}",style=MaterialTheme.typography.bodySmall)}
    Text("Tap a week to inspect · dot: no recorded runs · cross: distance unavailable",style=MaterialTheme.typography.bodySmall)
}
