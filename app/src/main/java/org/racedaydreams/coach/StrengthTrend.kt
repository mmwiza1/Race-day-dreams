package org.racedaydreams.coach
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.strength.*
/** Only dated retained PB evidence is plotted; unknown dates stay in the text history. */
@Composable fun StrengthTrend(points:List<StrengthProgression.Point>,state:CoachUiState){
    val dated=points.filter{it.date()!=null}.sortedBy{it.date()}
    if(dated.isEmpty())return
    val category=points.firstOrNull()?.category()?:""
    val usesMass=category.startsWith("Load")||category.startsWith("Added load")||category=="Estimated 1RM"
    fun value(p:StrengthProgression.Point)=if(usesMass)state.massUnit.fromKilograms(p.value())else p.value()
    var selected by rememberSaveable(category){mutableStateOf<String?>(null)}
    val lo=dated.minOf{value(it)};val hi=dated.maxOf{value(it)};val first=dated.first().date().toEpochDay();val last=dated.last().date().toEpochDay()
    val summary=dated.joinToString{p->"${if(p.precision()==HistoricalStrengthRecord.DatePrecision.APPROXIMATE)"Around "else ""}${shortDate(p.date())}: ${decimal(value(p))}"}
    val ink=MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(120.dp).pointerInput(dated,lo,hi){detectTapGestures{tap->val fraction=(tap.x/size.width).coerceIn(0f,1f);selected=dated.minByOrNull{p->kotlin.math.abs((if(last==first).5 else (p.date().toEpochDay()-first).toDouble()/(last-first))-fraction)}?.id()}}.semantics{contentDescription="Strength PB progression. Recorded evidence points: $summary. Lines only connect recorded points; unknown dates are not plotted."}){
        val pad=12.dp.toPx();val w=size.width-2*pad;val h=size.height-2*pad
        drawLine(ink.copy(alpha=.5f),Offset(pad,pad+h),Offset(pad+w,pad+h),2f)
        fun position(p:StrengthProgression.Point)=Offset(pad+if(last==first)w/2 else ((p.date().toEpochDay()-first).toFloat()/(last-first))*w,pad+h-(if(hi==lo).5f else ((value(p)-lo)/(hi-lo)).toFloat())*h)
        for(i in 1 until dated.size)drawLine(Color(0xFFB88A24),position(dated[i-1]),position(dated[i]),3.dp.toPx())
        dated.forEach{drawCircle(Color(0xFFB88A24),5.dp.toPx(),position(it))}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(shortDate(dated.first().date()),style=MaterialTheme.typography.labelSmall);Text(shortDate(dated.last().date()),style=MaterialTheme.typography.labelSmall)}
    Text("Recorded PB evidence · ${decimal(lo)}–${decimal(hi)} ${if(points.firstOrNull()?.category()?.startsWith("Duration")==true)"seconds"else if(points.firstOrNull()?.category()?.startsWith("Distance")==true)"metres"else if(usesMass)state.massUnit.name.lowercase()else "reps"}",style=MaterialTheme.typography.labelSmall)
    dated.firstOrNull{it.id()==selected}?.let{p->Text("Selected PB: ${shortDate(p.date())} · ${decimal(value(p))} · ${p.provenance()}")}
}
