package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Visual only: coaching hysteresis and evidence validity remain in the service. */
@Composable fun PaceRange(fast:Double,slow:Double,current:Double?,imperial:Boolean) {
    val factor=if(imperial)1.609344 else 1.0
    val label=if(current==null)"Current pace unavailable"else when { current<fast->"Faster than target";current>slow->"Slower than target";else->"Within target range" }
    val ink=MaterialTheme.colorScheme.primary
    val track=MaterialTheme.colorScheme.outline
    GuidanceCard {
        Text(label,style=MaterialTheme.typography.titleSmall)
        Canvas(Modifier.fillMaxWidth().height(34.dp).semantics{contentDescription="$label. Target ${formatPaceSeconds(fast*factor)} to ${formatPaceSeconds(slow*factor)} per ${if(imperial)"mile"else"kilometre"}."}) {
            val left=size.width*.18f;val right=size.width*.82f;val y=size.height/2
            drawLine(track,Offset(0f,y),Offset(size.width,y),3.dp.toPx(),StrokeCap.Round)
            drawLine(ink,Offset(left,y),Offset(right,y),5.dp.toPx(),StrokeCap.Round)
            listOf(left,right).forEach{x->drawLine(ink,Offset(x,y-10.dp.toPx()),Offset(x,y+10.dp.toPx()),2.dp.toPx())}
            current?.let { val span=(slow-fast).coerceAtLeast(10.0);val position=((it-fast)/span).toFloat();val x=(left+position*(right-left)).coerceIn(7.dp.toPx(),size.width-7.dp.toPx());drawCircle(ink,7.dp.toPx(),Offset(x,y)) }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(formatPaceSeconds(fast*factor));Text(formatPaceSeconds(slow*factor))}
    }
}
