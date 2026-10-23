package org.racedaydreams.coach
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.support.DreamRoutes

/** Original offline cartographic cues; no street tiles or copied race artwork. */
@Composable fun DreamDrawing(route:DreamRoutes.Route,start:Double,finish:Double){
    val outline=MaterialTheme.colorScheme.outline.copy(alpha=.45f);val ink=MaterialTheme.colorScheme.onSurface;val accent=MaterialTheme.colorScheme.primary;val landmark=MaterialTheme.colorScheme.tertiary.copy(alpha=.5f)
    Canvas(Modifier.fillMaxWidth().height(260.dp).semantics{contentDescription="Dream Route ${route.name()}. Stylised course-inspired visual, not navigation. Highlighted equivalent ${decimal(start/1000)}–${decimal(finish/1000)} km; runner at the end. Five kilometre markers. Landmarks: ${DreamRoutes.landmarks(route).joinToString{it.label()}}"}){
        val side=size.minDimension*.9f;val origin=Offset((size.width-side)/2,(size.height-side)/2)
        fun p(x:Double,y:Double)=origin+Offset((x/10*side).toFloat(),(y/10*side).toFloat())
        fun p(v:DreamRoutes.Vertex)=p(v.x(),v.y())
        DreamRoutes.landmarks(route).forEach{l->val o=p(l.x(),l.y());val r=side*.035f
            when(l.shape()){
                "river"->drawLine(landmark,o-Offset(r*2,0f),o+Offset(r*3,r),r*.7f,StrokeCap.Round)
                "park"->drawRoundRect(landmark,o-Offset(r,r),Size(r*2,r*3),CornerRadius(r*.5f))
                "bridge"->{drawLine(landmark,o-Offset(r*2,0f),o+Offset(r*2,0f),3.dp.toPx());for(x in listOf(-1f,1f)){drawLine(landmark,o+Offset(x*r,-r),o+Offset(x*r,r),3.dp.toPx())}}
                "gate"->{for(x in listOf(-1f,1f))drawLine(landmark,o+Offset(x*r,r),o+Offset(x*r,-r),3.dp.toPx());drawLine(landmark,o-Offset(r*1.4f,r),o+Offset(r*1.4f,-r),4.dp.toPx())}
                else->{drawLine(landmark,o+Offset(0f,-r*2),o+Offset(-r,r),3.dp.toPx());drawLine(landmark,o+Offset(0f,-r*2),o+Offset(r,r),3.dp.toPx());drawLine(landmark,o-Offset(r*.7f,0f),o+Offset(r*.7f,0f),2.dp.toPx())}
            }
        }
        route.vertices().zipWithNext().forEach{(a,b)->drawLine(outline,p(a),p(b),3.dp.toPx(),StrokeCap.Round)}
        var previous=DreamRoutes.at(route,start);repeat(300){i->val next=DreamRoutes.at(route,start+(finish-start)*(i+1)/300);drawLine(accent,p(previous),p(next),5.dp.toPx(),StrokeCap.Round);previous=next}
        for(km in 5..40 step 5)drawCircle(ink,2.5.dp.toPx(),p(DreamRoutes.at(route,km*1000.0)))
        drawCircle(accent,5.dp.toPx(),p(DreamRoutes.at(route,start)))
        val end=p(DreamRoutes.at(route,finish));val r=4.dp.toPx();drawCircle(ink,r,end-Offset(0f,r*3));drawLine(ink,end-Offset(0f,r*2),end+Offset(0f,r),r*.7f);drawLine(ink,end-Offset(r*2,r),end+Offset(r*2,-r),r*.6f);drawLine(ink,end+Offset(0f,r),end+Offset(-r*2,r*3),r*.6f);drawLine(ink,end+Offset(0f,r),end+Offset(r*2,r*2),r*.6f)
    }
    Text(DreamRoutes.landmarks(route).joinToString(" · "){it.label()},style=MaterialTheme.typography.labelMedium)
}
