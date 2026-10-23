package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

/** Original RDD miniature city illustrations. Shapes are not copied organiser artwork. */
@Composable internal fun CityLandmarkArtwork(shape: String, modifier: Modifier = Modifier) {
    val navy=MaterialTheme.colorScheme.onSurface
    val blue=MaterialTheme.colorScheme.primary
    val stone=MaterialTheme.colorScheme.primaryContainer
    val window=MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val unitScale=minOf(size.width,size.height)/100f
        withTransform({scale(unitScale,unitScale,Offset.Zero)}) {
            drawOval(blue.copy(alpha=.09f),Offset(7f,81f),Size(86f,12f))
            fun rect(x:Float,y:Float,w:Float,h:Float,c:Color=stone) = drawRoundRect(c,Offset(x,y),Size(w,h),CornerRadius(1.5f))
            fun line(x:Float,y:Float,a:Float,b:Float,width:Float=2f,c:Color=navy)=drawLine(c,Offset(x,y),Offset(a,b),width,StrokeCap.Round)
            fun polygon(c:Color,vararg points:Float){val p=Path();points.asList().chunked(2).forEachIndexed{i,xy->if(i==0)p.moveTo(xy[0],xy[1])else p.lineTo(xy[0],xy[1])};p.close();drawPath(p,c)}
            fun windows(x:Float,y:Float,n:Int,rows:Int,spacing:Float=9f){for(row in 0 until rows)for(col in 0 until n)rect(x+col*spacing,y+row*10,4f,6f,blue.copy(alpha=.6f))}
            when(shape) {
                "river" -> {
                    val river=Path().apply{moveTo(9f,24f);cubicTo(91f,15f,12f,59f,65f,60f);cubicTo(99f,61f,63f,80f,92f,84f)}
                    drawPath(river,stone,style=Stroke(18f,cap=StrokeCap.Round))
                    drawPath(river,blue.copy(alpha=.65f),style=Stroke(8f,cap=StrokeCap.Round))
                }
                "bridge" -> {
                    for(x in listOf(25f,68f)){
                        rect(x-7,35f,14f,49f);rect(x-10,32f,20f,5f,navy);rect(x-5,27f,10f,7f)
                        polygon(navy,x-7,27f,x,16f,x+7,27f);windows(x-2,42f,1,3)
                        rect(x-9,79f,18f,6f,navy)
                    }
                    line(8f,73f,92f,73f,5f);line(24f,40f,69f,40f,3f,blue)
                    val cable=Path().apply{moveTo(8f,57f);quadraticBezierTo(18f,60f,25f,40f);quadraticBezierTo(48f,75f,68f,40f);quadraticBezierTo(77f,60f,92f,57f)}
                    drawPath(cable,blue,style=Stroke(2f))
                    for(x in 35..61 step 7)line(x.toFloat(),58f,x.toFloat(),71f,1f,blue)
                }
                "wheel" -> {
                    val o=Offset(50f,43f);drawCircle(blue,29f,o,style=Stroke(3f));drawCircle(navy,3f,o)
                    for(i in 0..11){val a=i*Math.PI/6;val p=o+Offset((29*Math.cos(a)).toFloat(),(29*Math.sin(a)).toFloat());drawLine(blue.copy(alpha=.6f),o,p,1f);drawOval(stone,p-Offset(3.5f,2.5f),Size(7f,5f))}
                    line(50f,43f,37f,84f,4f);line(50f,43f,65f,84f,4f);rect(31f,83f,40f,4f,blue)
                }
                "tower" -> {
                    polygon(stone,24f,86f,46f,23f,49f,11f,52f,23f,77f,86f,64f,86f,50f,61f,36f,86f)
                    line(50f,10f,50f,4f);line(40f,42f,59f,42f,4f,blue);line(33f,63f,68f,63f,5f,blue)
                    for(y in 35..74 step 13){val half=(y-14)*.34f;line(50-half,y.toFloat(),50+half,y+12f,1.3f,blue);line(50+half,y.toFloat(),50-half,y+12f,1.3f,blue)}
                    line(20f,87f,80f,87f,4f)
                }
                "temple" -> {
                    rect(27f,48f,46f,37f);rect(32f,62f,36f,23f,blue);rect(46f,61f,9f,24f,navy)
                    for(y in listOf(43f,29f,15f)){val half=if(y==43f)39f else if(y==29f)31f else 21f;polygon(navy,50-half,y+8,50-half+9,y,50f,y-8,50+half-9,y,50+half,y+8);line(50-half,y+8,50+half,y+8,2f,blue)}
                    line(50f,7f,50f,2f);rect(19f,84f,62f,4f)
                }
                "pyramid" -> {
                    rect(13f,51f,74f,34f);windows(19f,58f,8,2,8f)
                    polygon(blue.copy(alpha=.6f),25f,85f,50f,35f,77f,85f)
                    line(25f,85f,50f,35f);line(50f,35f,77f,85f);line(50f,35f,50f,85f,1f,window)
                    for(y in 56..78 step 10){val w=(y-35)*.5f;line(50-w,y.toFloat(),50+w,y.toFloat(),1f,window)}
                    line(18f,87f,84f,87f,3f)
                }
                "column" -> {
                    rect(30f,78f,40f,9f,navy);rect(35f,69f,30f,8f);rect(43f,29f,14f,40f)
                    for(x in 46..54 step 4)line(x.toFloat(),32f,x.toFloat(),66f,1f,blue)
                    rect(37f,26f,26f,5f,blue);polygon(navy,43f,25f,49f,9f,53f,9f,59f,25f)
                    drawCircle(blue,4f,Offset(51f,6f));polygon(blue,49f,12f,36f,9f,46f,20f,58f,12f,66f,8f,59f,21f)
                }
                "park" -> {
                    drawRoundRect(stone,Offset(13f,26f),Size(74f,60f),CornerRadius(14f))
                    val path=Path().apply{moveTo(25f,80f);cubicTo(75f,77f,31f,48f,75f,34f)}
                    drawPath(path,window,style=Stroke(6f,cap=StrokeCap.Round))
                    for(o in listOf(Offset(28f,44f),Offset(61f,63f),Offset(76f,77f))){line(o.x,o.y,o.x,o.y+12,2f);drawOval(blue.copy(alpha=.65f),o-Offset(8f,12f),Size(16f,21f))}
                }
                else -> {
                    // Original classical facade, with a distinct gate/dome/station roof.
                    rect(10f,51f,80f,34f);rect(26f,42f,48f,43f);windows(15f,59f,8,2,9f)
                    if(shape=="dome") {
                        drawArc(blue,180f,180f,true,Offset(35f,17f),Size(30f,39f));rect(34f,36f,32f,5f,navy)
                        line(50f,18f,50f,35f,1f,window);line(43f,21f,41f,35f,1f,window);line(57f,21f,59f,35f,1f,window)
                    } else if(shape=="station") {
                        polygon(navy,22f,42f,32f,29f,68f,29f,78f,42f);rect(43f,19f,14f,24f);drawCircle(window,5f,Offset(50f,31f));line(50f,31f,50f,27f,1f);line(50f,31f,53f,32f,1f)
                    } else {
                        polygon(navy,20f,43f,50f,25f,80f,43f);rect(20f,44f,60f,4f,blue)
                        for(x in 29..70 step 9){rect(x.toFloat(),50f,4f,30f,window);rect(x-1f,49f,6f,3f,navy)}
                    }
                    rect(43f,65f,14f,20f,navy);rect(7f,84f,86f,4f,blue)
                }
            }
        }
    }
}
