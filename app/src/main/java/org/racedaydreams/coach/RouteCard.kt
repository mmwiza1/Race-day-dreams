package org.racedaydreams.coach
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.racedaydreams.domain.polish.RoutePresentation
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.domain.activity.ActivityRecord

private data class RecordedRouteProjection(
    val points: List<RunMetricsEngine.Point>,
    val xy: List<RoutePresentation.XY>,
    val kilometres: List<RoutePresentation.Section>,
    val pauses: List<RecordingStore.Pause>,
    val story: List<String>,
)

@Composable fun RecordedRouteCard(activity:ActivityRecord){
    if(activity.source().provider() !in listOf("rdd","gpx"))return
    val context=LocalContext.current.applicationContext
    // One published detail snapshot prevents an initial empty derived result from being retained
    // independently of the asynchronously fetched accepted points.
    val projection by produceState<RecordedRouteProjection?>(null,activity.id(),activity.source()) {
        val (points,pauses)=withContext(Dispatchers.IO){
            val store=RecordingStore(context)
            store.points(activity.source().recordId()) to store.pauses(activity.source().recordId())
        }
        value=withContext(Dispatchers.Default){RecordedRouteProjection(points,RoutePresentation.fit(points),
            RoutePresentation.kilometres(points),pauses,RoutePresentation.story(points))}
    }
    val prepared=projection?:return
    val points=prepared.points;val xy=prepared.xy;val km=prepared.kilometres
    if(xy.size<2)return
    val pauses=prepared.pauses;val story=prepared.story
    val primary=MaterialTheme.colorScheme.primary;val fastColour=MaterialTheme.colorScheme.tertiary;val slowColour=MaterialTheme.colorScheme.error;val neutral=MaterialTheme.colorScheme.outline
    val fastest=km.minByOrNull{it.secondsPerKm()}?.kilometre();val slowest=km.maxByOrNull{it.secondsPerKm()}?.kilometre()
    val pauseIndices=remember(points,pauses){pauses.map{pause->var lo=0;var hi=points.size;while(lo<hi){val mid=(lo+hi)/2;if(points[mid].fix().monotonicMillis()<=pause.start)lo=mid+1 else hi=mid};(lo-1).coerceAtLeast(0)}}
    SemanticCard(Tile.Planned){
        Text("Your recorded route",style=MaterialTheme.typography.titleLarge)
        Canvas(Modifier.fillMaxWidth().height(280.dp).semantics{contentDescription="Recorded route with start and finish, direction arrows, full kilometre markers and ${pauses.size} explicit pauses. Gaps are not joined. ${story.joinToString(" ")}"}){
            val side=size.minDimension;val origin=Offset((size.width-side)/2,(size.height-side)/2)
            fun pos(p:RoutePresentation.XY)=origin+Offset((p.x()*side).toFloat(),(p.y()*side).toFloat())
            val stride=(points.size/1500).coerceAtLeast(1);var previous=0
            for(i in 1 until points.size){val p=points[i];val broken=p.discontinuity()||p.segment()!=points[i-1].segment();if(broken){previous=i;continue};if(i%stride==0||i==points.lastIndex){
                val k=(p.cumulativeMetres()/1000).toInt()+1;val colour=if(k==fastest)fastColour else if(k==slowest)slowColour else primary
                drawLine(colour,pos(xy[previous]),pos(xy[i]),4.dp.toPx(),StrokeCap.Round);previous=i
            }}
            drawCircle(primary,7.dp.toPx(),pos(xy.first()));drawCircle(slowColour,7.dp.toPx(),pos(xy.last()));drawCircle(tileColourForCanvas(),3.dp.toPx(),pos(xy.last()))
            val labelPaint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=neutral.toArgb();textSize=11.dp.toPx()}
            var boundary=1000.0
            for(i in 1 until points.size){val a=points[i-1];val b=points[i];if(b.cumulativeMetres()>=boundary){if(!b.discontinuity()&&b.segment()==a.segment()&&b.cumulativeMetres()>a.cumulativeMetres()){val f=(boundary-a.cumulativeMetres())/(b.cumulativeMetres()-a.cumulativeMetres());val x=xy[i-1];val y=xy[i];val marker=pos(RoutePresentation.XY(x.x()+(y.x()-x.x())*f,x.y()+(y.y()-x.y())*f));drawCircle(neutral,4.dp.toPx(),marker);drawContext.canvas.nativeCanvas.drawText((boundary/1000).toInt().toString(),marker.x+5.dp.toPx(),marker.y-5.dp.toPx(),labelPaint)};boundary+=1000}}
            pauseIndices.forEach{index->val p=pos(xy[index]);drawLine(neutral,p-Offset(4.dp.toPx(),5.dp.toPx()),p-Offset(4.dp.toPx(),-5.dp.toPx()),3.dp.toPx());drawLine(neutral,p+Offset(4.dp.toPx(),-5.dp.toPx()),p+Offset(4.dp.toPx(),5.dp.toPx()),3.dp.toPx())}
            if(xy.size>3){val a=pos(xy[xy.size/2-1]);val b=pos(xy[xy.size/2]);val angle=kotlin.math.atan2(b.y-a.y,b.x-a.x);val d=10.dp.toPx();val tip=b;drawLine(primary,tip,tip-Offset(kotlin.math.cos(angle-.6f)*d,kotlin.math.sin(angle-.6f)*d),2.dp.toPx());drawLine(primary,tip,tip-Offset(kotlin.math.cos(angle+.6f)*d,kotlin.math.sin(angle+.6f)*d),2.dp.toPx())}
        }
        Text("● Start · ◉ Finish · dots: kilometres · Ⅱ explicit pause · arrow: direction")
        Text("Fastest complete km: ${fastest?:"unavailable"} · Slowest complete km: ${slowest?:"unavailable"}. Colour is supplemental; gaps are left blank.")
        Text("${formatDistance(activity.distanceMetres()?:0.0,CoachUiState())} · elapsed ${org.racedaydreams.domain.PaceCalculator.formatTime(activity.elapsedTime())}")
        activity.movingTime()?.let{Text("Active ${org.racedaydreams.domain.PaceCalculator.formatTime(it)}")};activity.averageHeartRate()?.let{Text("Average HR ${it.toInt()} bpm")}
        if(RoutePresentation.reliableAltitude(points)){
            val alt=points.filter{it.fix().verticalAccuracy()!=null&&it.fix().verticalAccuracy()<=10}.mapNotNull{it.fix().altitude()};val lo=alt.minOrNull()!!;val hi=alt.maxOrNull()!!
            Text("Recorded elevation · ${decimal(lo)}–${decimal(hi)} m")
            Canvas(Modifier.fillMaxWidth().height(90.dp).semantics{contentDescription="Recorded altitude ranges from ${decimal(lo)} to ${decimal(hi)} metres. Vertical accuracy at most 10 m for at least 80% of observations."}){val span=(hi-lo).coerceAtLeast(1.0);for(i in 1 until points.size){val a=points[i-1];val b=points[i];if(a.fix().altitude()!=null&&b.fix().altitude()!=null&&a.fix().verticalAccuracy()!=null&&a.fix().verticalAccuracy()<=10&&b.fix().verticalAccuracy()!=null&&b.fix().verticalAccuracy()<=10&&!b.discontinuity())drawLine(primary,Offset((i-1).toFloat()/points.lastIndex*size.width,((1-(a.fix().altitude()-lo)/span)*size.height).toFloat()),Offset(i.toFloat()/points.lastIndex*size.width,((1-(b.fix().altitude()-lo)/span)*size.height).toFloat()),2.dp.toPx())}}
        }else Text("Detailed elevation is unavailable or not reliable enough to chart.")
    }
    SemanticCard(Tile.Insight){Text("Route story",style=MaterialTheme.typography.titleLarge);if(story.isEmpty())Text("Not enough uninterrupted distance evidence for pacing observations.")else story.take(2).forEach{Text(it)};pauses.take(2).forEach{Text("Explicit pause near ${decimal(it.metres/1000)} km · ${it.duration/1000} sec")};WhyDetails("Only complete uninterrupted kilometres and explicit recording events are used. GPS gaps are not described as intentional pauses. No reason for a stop is inferred.","How this was calculated")}
}
private fun tileColourForCanvas()=androidx.compose.ui.graphics.Color.White
