package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.domain.support.*
import org.racedaydreams.data.SupportStore

@Composable fun RoutineLibrary(open:(String)->Unit)=CoachPage {
    CoachHeading("Movement support")
    Text("Optional short routines. Two to four short mobility sessions can be useful when they fit your week. There is no compliance target, load or missed-mobility warning.")
    RunningRoutines.all().forEach{r->CoachCard{Text(r.title(),style=MaterialTheme.typography.titleMedium);Text("${r.minutes()} min · ${r.purpose()}");CoachButton("Start ${r.title()}"){open(r.id())}}}
    Text("General movement guidance. Keep movements comfortable, stop if they cause pain and seek qualified advice when unsure. This is not injury treatment or a recovery guarantee.")
}

@Composable fun RoutineScreen(id:String?,related:String?,done:()->Unit){
    val routine=remember(id){runCatching{RunningRoutines.get(id?:"")}.getOrNull()}
    if(routine==null){CoachPage{Text("Routine unavailable")};return}
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var index by rememberSaveable(id){mutableIntStateOf(0)};var saving by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val movement=routine.movements()[index]
    CoachPage {
        CoachHeading(routine.title());Text("${index+1} of ${routine.movements().size}")
        MovementIllustration(movement)
        Text(movement.title(),style=MaterialTheme.typography.headlineMedium);Text(movement.dose(),style=MaterialTheme.typography.headlineSmall)
        Text(movement.instructions());Text(movement.cue())
        WhyDetails(movement.mistake(),"Common mistake")
        Text("Keep the range comfortable. Stop if painful; this is general guidance, not treatment.")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedButton(onClick={index--},enabled=index>0,modifier=Modifier.heightIn(min=56.dp)){Text("Back")}
            Button(onClick={if(index+1<routine.movements().size)index++ else{saving=true;scope.launch{try{withContext(Dispatchers.IO){SupportStore(context).completeRoutine(routine.id(),related)};done()}catch(_:Exception){error="Completion could not be saved. Your training plan is unchanged."}finally{saving=false}}}},enabled=!saving,modifier=Modifier.heightIn(min=56.dp)){Text(if(index+1<routine.movements().size)"Next"else"Finish routine")}
        };error?.let{Text(it)}
    }
}

/** App-owned original line figures, supplemental to complete textual instructions.
 * Drawing units have no anatomical precision. No downloaded photographs or assets. */
@Composable private fun MovementIllustration(m:RunningRoutines.Movement){
    val ink=MaterialTheme.colorScheme.onSurface;val accent=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(200.dp).semantics{contentDescription="Schematic ${m.title()}. Follow the written instructions for movement and comfortable range."}) {
        val scale=size.minDimension/100;val origin=Offset(size.width/2-50*scale,0f)
        fun p(x:Float,y:Float)=origin+Offset(x*scale,y*scale)
        fun line(ax:Float,ay:Float,bx:Float,by:Float,color:Color=ink){drawLine(color,p(ax,ay),p(bx,by),4*scale,androidx.compose.ui.graphics.StrokeCap.Round)}
        if(m.pose()=="bridge"){
            drawCircle(ink,6*scale,p(20f,72f));line(27f,72f,52f,52f);line(52f,52f,73f,62f);line(73f,62f,85f,82f);line(30f,77f,48f,82f);line(15f,85f,90f,85f,accent)
        }else if(m.pose()=="sit"){
            drawCircle(ink,6*scale,p(45f,20f));line(45f,28f,45f,55f);line(45f,55f,70f,55f);line(70f,55f,70f,83f);line(45f,55f,60f,45f);line(60f,45f,76f,57f);line(45f,36f,65f,48f);line(35f,58f,75f,58f,accent)
        }else{
            drawCircle(ink,6*scale,p(48f,14f));line(48f,23f,50f,52f);line(48f,30f,32f,46f);line(32f,46f,24f,30f);line(48f,30f,68f,38f)
            when(m.pose()){
                "lunge"->{line(50f,52f,73f,54f);line(73f,54f,73f,84f);line(50f,52f,30f,75f);line(30f,75f,12f,85f)}
                "swing","march"->{line(50f,52f,48f,85f);line(50f,52f,73f,48f);line(73f,48f,85f,68f)}
                "quad"->{line(50f,52f,46f,85f);line(50f,52f,65f,70f);line(65f,70f,75f,49f);line(68f,38f,75f,49f)}
                "calf"->{line(50f,52f,40f,76f);line(50f,52f,59f,76f);line(40f,76f,45f,84f);line(59f,76f,64f,84f)}
                else->{line(50f,52f,35f,84f);line(50f,52f,70f,80f)}
            };line(15f,90f,90f,90f,accent)
        }
    }
}

@Composable fun DreamRouteSection(activity:org.racedaydreams.domain.activity.ActivityRecord){
    val metres=activity.distanceMetres()?:return;if(metres<=0)return
    val context=LocalContext.current
    val assignment by produceState<DreamRoutes.Assignment?>(null,activity.id()){value=withContext(Dispatchers.IO){SupportStore(context).dream(activity.id(),metres)}}
    val a=assignment?:return;val route=DreamRoutes.routes().first{it.id()==a.routeId()}
    val outline=MaterialTheme.colorScheme.outline;val highlight=MaterialTheme.colorScheme.primary
    CoachCard {
        Text("Your Dream Route · ${route.name()}",style=MaterialTheme.typography.titleMedium)
        Text(String.format(java.util.Locale.UK,"You covered the equivalent of %.2f km of a marathon. This original %s-inspired schematic is not the actual or official course.",metres/1000,route.name()))
        Canvas(Modifier.fillMaxWidth().height(180.dp).semantics{contentDescription="${route.name()} inspired marathon schematic. Highlighted equivalent from ${decimal(a.startMetres()/1000)} to ${decimal((a.startMetres()+kotlin.math.min(metres,42195.0))/1000)} kilometres. Not navigation or a physical location claim."}){
            fun p(v:DreamRoutes.Vertex)=Offset((v.x()/10*size.width).toFloat(),(v.y()/10*size.height).toFloat())
            route.vertices().zipWithNext().forEach{(x,y)->drawLine(outline,p(x),p(y),3.dp.toPx())}
            val finish=a.startMetres()+kotlin.math.min(42195.0,a.coveredMetres());var prior=DreamRoutes.at(route,a.startMetres())
            repeat(200){i->val next=DreamRoutes.at(route,a.startMetres()+(finish-a.startMetres())*(i+1)/200);drawLine(highlight,p(prior),p(next),6.dp.toPx());prior=next}
            drawCircle(highlight,7.dp.toPx(),p(DreamRoutes.at(route,a.startMetres())));drawCircle(highlight,7.dp.toPx(),p(DreamRoutes.at(route,finish)));drawCircle(outline,3.dp.toPx(),p(DreamRoutes.at(route,finish)))
        }
        Text("● Start · ◉ Finish · highlighted segment")
        if(metres>42195)Text("One full marathon equivalent plus ${decimal((metres-42195)/1000)} km. Your recorded run is unchanged.")
    }
}
