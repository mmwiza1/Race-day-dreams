package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.support.DreamCartography
import org.racedaydreams.domain.support.DreamRoutes

/** Offline geographic geometry and sourced city context; no tiles or invented streets. */
@Composable fun DreamDrawing(route: DreamRoutes.Route, start: Double, finish: Double) {
    val outline=MaterialTheme.colorScheme.outline.copy(alpha=.6f)
    val ink=MaterialTheme.colorScheme.onSurface
    val paper=MaterialTheme.colorScheme.surface
    val panel=MaterialTheme.colorScheme.surfaceContainerLow
    val accent=MaterialTheme.colorScheme.primary
    val water=accent.copy(alpha=.14f)
    val map=rememberDreamBasemap(route.id())
    val dark=androidx.compose.foundation.isSystemInDarkTheme()
    val mapPaper=if(dark) Color(0xff192936) else Color(0xffF2F5F8)
    val mapGreen=if(dark) Color(0xff29493F) else Color(0xffDDEFE2)
    val mapWater=if(dark) Color(0xff284E6B) else Color(0xffC6E4F6)
    val mapRoad=if(dark) Color(0xff405362) else Color.White
    val highlighted=remember(route,start,finish){DreamRoutes.section(route,start,finish)}
    val landmarks=remember(route.id()){DreamRoutes.landmarks(route)}
    val rivers=remember(route.id()){DreamCartography.rivers(route.id())}
    val bounds=remember(route.id()){
        val vertices=route.vertices()+landmarks.map{DreamRoutes.Vertex(it.x(),it.y())}
        listOf(vertices.minOf{it.x()}-.75,vertices.maxOf{it.x()}+.75,vertices.minOf{it.y()}-.75,vertices.maxOf{it.y()}+.75)
    }
    Canvas(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(20.dp)).background(if(map!=null)mapPaper else panel).semantics{
        stateDescription=if(map!=null) "Offline city context ready" else "Course overview"
        contentDescription="Dream Route ${route.name()}. Full course in grey; highlighted equivalent ${decimal(start/1000)}–${decimal(finish/1000)} km. Ring marks section start; checkered marker marks section finish. Five kilometre markers. Numbered landmarks: ${landmarks.mapIndexed{i,l->"${i+1}, ${l.label()}"}.joinToString()}. Simplified offline geographic context; not navigation or your physical location."
    }){
        val(minX,maxX,minY,maxY)=bounds
        val scale=minOf(size.width/(maxX-minX),size.height/(maxY-minY))
        val origin=Offset(((size.width-(maxX-minX)*scale)/2).toFloat(),((size.height-(maxY-minY)*scale)/2).toFloat())
        fun p(v:DreamRoutes.Vertex)=origin+Offset(((v.x()-minX)*scale).toFloat(),((v.y()-minY)*scale).toFloat())
        fun path(points:List<DreamRoutes.Vertex>)=Path().apply{points.forEachIndexed{i,v->val o=p(v);if(i==0)moveTo(o.x,o.y)else lineTo(o.x,o.y)}}
        clipRect{
            val basemap=map
            if(basemap!=null) {
                // Unit-space geometry/path preparation is cached off-main. Three draw calls,
                // irrespective of source feature count; raw user routes are never read here.
                withTransform({
                    translate(origin.x-(minX*scale).toFloat(),origin.y-(minY*scale).toFloat())
                    scale(scale.toFloat(),scale.toFloat(),Offset.Zero)
                }) {
                    drawPath(basemap.green,mapGreen)
                    drawPath(basemap.water,mapWater)
                    drawPath(basemap.roads,mapRoad,style=Stroke(.85.dp.toPx()/scale.toFloat(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                }
            } else {
                // Legacy drawings retain their original frame and sourced river context.
                rivers.forEach{drawPath(path(it.vertices()),water,style=Stroke(16.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))}
            }
            drawPath(path(route.vertices()),paper,style=Stroke(7.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            drawPath(path(route.vertices()),outline,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            drawPath(path(highlighted),accent.copy(alpha=.12f),style=Stroke(12.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            drawPath(path(highlighted),accent,style=Stroke(5.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=ink.toArgb();textSize=10.dp.toPx();typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)}
            for(km in 5..40 step 5){val o=p(DreamRoutes.at(route,km*1000.0));drawCircle(paper,4.dp.toPx(),o);drawCircle(outline,2.dp.toPx(),o);drawContext.canvas.nativeCanvas.drawText("$km",o.x+5.dp.toPx(),o.y-5.dp.toPx(),paint)}
            landmarks.forEachIndexed{i,l->val o=p(DreamRoutes.Vertex(l.x(),l.y()));drawCircle(paper,9.dp.toPx(),o);drawCircle(ink,7.dp.toPx(),o);paint.color=paper.toArgb();paint.textSize=10.dp.toPx();paint.textAlign=android.graphics.Paint.Align.CENTER;drawContext.canvas.nativeCanvas.drawText("${i+1}",o.x,o.y+3.5.dp.toPx(),paint)}
            val first=p(DreamRoutes.at(route,start));drawCircle(paper,7.dp.toPx(),first);drawCircle(accent,5.dp.toPx(),first);drawCircle(paper,2.dp.toPx(),first)
            val end=p(DreamRoutes.at(route,finish));val r=4.dp.toPx();drawCircle(paper,r*2.1f,end);drawCircle(ink,r*1.7f,end)
            for(row in 0..1)for(col in 0..1)drawRect(if((row+col)%2==0)paper else ink,end+Offset((col-1)*r,(row-1)*r),Size(r,r))
        }
    }
    if(landmarks.isNotEmpty()) {
        val largeText=androidx.compose.ui.platform.LocalDensity.current.fontScale>=1.15f
        if(largeText) Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            landmarks.forEachIndexed{i,l->Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                CityLandmarkArtwork(l.shape(),Modifier.size(48.dp))
                Text("${i+1} · ${l.label()}",style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
            }}
        } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            landmarks.forEachIndexed{i,l->Column(Modifier.weight(1f),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
                CityLandmarkArtwork(l.shape(),Modifier.size(48.dp))
                Text("${i+1} · ${l.label()}",style=MaterialTheme.typography.labelSmall,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }}
        }
    }
}
