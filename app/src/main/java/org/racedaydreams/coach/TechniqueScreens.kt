package org.racedaydreams.coach
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.strength.TechniqueGuide

/** App-owned schematic movement drawings, rendered offline from the actual guide. */
@Composable fun TechniqueSection(id:String){
    val guide=TechniqueGuide.forExercise(id)
    if(guide==null){Text("A visual guide for this exercise is not available yet. Choose a familiar movement or seek qualified coaching.");return}
    val art=exerciseArtwork(id)
    if(art!=null) {
        IllustratedMovement(art)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround) { art.phases.forEachIndexed { i,label -> Text("${i+1}. $label",style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f)) } }
        GuidanceCard { Text("How to do it",style=MaterialTheme.typography.titleMedium);Text("1. ${guide.setup()}");Text("2. ${guide.movement()}") }
    } else {
        listOf("Setup" to guide.setup(),"Movement" to guide.movement()).forEachIndexed { i,pair -> GuidanceCard { Text("${i+1}. ${pair.first}",style=MaterialTheme.typography.titleMedium);TechniqueDrawing(id,guide.pattern(),i,pair.second);Text(pair.second) } }
    }
    GuidanceNote("Key cues",guide.cues().joinToString("\n"){"✓ $it"})
    GuidanceNote("Common mistakes",guide.mistakes().joinToString("\n"){"• $it"},caution=true)
    Text("Primary muscles: ${guide.muscles()}")
    Text("Alternatives: ${guide.alternatives().joinToString()}")
    WhyDetails("General technique guidance. Start conservatively and seek qualified in-person coaching if you are unsure about a movement. Illustrations are general guidance, not individual form assessment or an exact joint-angle prescription.","About this guidance")
}
@Composable private fun TechniqueDrawing(id:String,pattern:String,frame:Int,description:String){
    val ink=MaterialTheme.colorScheme.onSurface;val load=MaterialTheme.colorScheme.primary;val muscle=MaterialTheme.colorScheme.tertiary;val skin=Color(0xFFCF9B7C);val hair=Color(0xFF4A3028)
    Canvas(Modifier.fillMaxWidth().height(190.dp).semantics{contentDescription="Original illustrated $pattern, ${if(frame==0)"setup"else if(frame==1)"working position"else"controlled return"}: $description"}){
        fun point(x:Float,y:Float)=Offset(size.width/2+(x-.5f)*size.height,size.height*y)
        fun limb(a:Pair<Float,Float>,b:Pair<Float,Float>,colour:Color=ink)=drawLine(colour,point(a.first,a.second),point(b.first,b.second),9f,androidx.compose.ui.graphics.StrokeCap.Round)
        val bent=frame==1
        val horizontal=pattern in listOf("plank","press","bridge","curl")
        var hip=if(horizontal) .48f to .58f else if(bent&&pattern in listOf("hinge","row","squat","split")) .42f to .62f else .48f to .48f
        var shoulder=if(horizontal) .30f to .48f else if(bent&&pattern in listOf("hinge","row")) .60f to .40f else .48f to .25f
        var knee=if(horizontal).65f to .63f else if(bent&&pattern in listOf("squat","split")) .60f to .68f else .48f to .70f
        var foot=if(horizontal).80f to .72f else .48f to .92f
        var hand=when(pattern){"overhead","pull"-> .62f to (if(bent).06f else .32f);"press"-> .30f to (if(bent).15f else .43f);"row"->if(bent).53f to .45f else .62f to .68f;else-> .62f to (if(bent).65f else .49f)}
        if(pattern=="hinge")hand=if(bent).53f to .76f else .53f to .55f
        if(pattern=="row"){hip=.42f to .62f;shoulder=.60f to .40f}
        if(pattern=="press"){shoulder=.28f to .5f;hip=.50f to .5f;knee=.65f to .7f;foot=.65f to .92f;hand=.30f to (if(bent).18f else .43f);drawLine(ink,point(.22f,.60f),point(.57f,.60f),5f)}
        if(pattern=="bridge"){shoulder=.28f to .5f;hip=.50f to (if(bent).50f else .74f);knee=.68f to .58f;foot=.68f to .92f;drawLine(ink,point(.2f,.60f),point(.34f,.60f),5f)}
        if(pattern=="curl"){shoulder=.28f to .55f;hip=.48f to .55f;knee=.65f to .55f;foot=if(bent).65f to .23f else .82f to .55f;drawLine(ink,point(.22f,.65f),point(.68f,.65f),5f)}
        if(pattern=="pull")hand=.62f to (if(bent).32f else .06f)
        if(id=="leg-press"){shoulder=.28f to .32f;hip=.35f to .68f;knee=if(bent).48f to .42f else .60f to .55f;foot=.76f to .30f;drawLine(ink,point(.20f,.3f),point(.30f,.75f),5f);drawLine(load,point(.72f,.23f),point(.81f,.37f),7f)}
        if(pattern=="calf"&&bent){shoulder=.48f to .17f;hip=.48f to .40f;knee=.48f to .62f;foot=.48f to .86f}
        if(pattern=="calf")drawLine(ink,point(foot.first,foot.second),point(.56f,.92f),9f,androidx.compose.ui.graphics.StrokeCap.Round)
        if(pattern=="plank"){shoulder=.30f to .64f;hip=.52f to .73f;knee=if(bent).69f to .79f else .60f to .96f;foot=if(bent).85f to .85f else .80f to .96f;hand=.48f to .94f}
        if(pattern=="carry"){hand=.62f to .60f;if(bent){knee=.52f to .70f;foot=.60f to .92f;limb(hip,.40f to .68f);limb(.40f to .68f,.36f to .92f)}}
        val shoulderPoint=point(shoulder.first,shoulder.second);val hipPoint=point(hip.first,hip.second);val axis=hipPoint-shoulderPoint;val axisLength=axis.getDistance().coerceAtLeast(1f);val side=Offset(-axis.y,axis.x)/axisLength*size.height*.045f
        val shirt=Path().apply{moveTo((shoulderPoint-side).x,(shoulderPoint-side).y);lineTo((shoulderPoint+side).x,(shoulderPoint+side).y);lineTo((hipPoint+side*.8f).x,(hipPoint+side*.8f).y);lineTo((hipPoint-side*.8f).x,(hipPoint-side*.8f).y);close()}
        drawPath(shirt,load);drawPath(shirt,ink.copy(alpha=.5f),style=Stroke(1.5f))
        if(!horizontal&&pattern!="split"){val secondKnee=(knee.first-.065f) to (knee.second+.015f);val secondFoot=(foot.first-.065f) to foot.second;limb(hip,secondKnee);limb(secondKnee,secondFoot);drawLine(load,point(secondFoot.first-.02f,secondFoot.second),point(secondFoot.first+.06f,secondFoot.second),7f,androidx.compose.ui.graphics.StrokeCap.Round)}
        val emphasis=when(pattern){"press","pull","row","overhead"->shoulder;"calf"->knee.first to ((knee.second+foot.second)/2);"plank","carry"->((shoulder.first+hip.first)/2) to ((shoulder.second+hip.second)/2);else->hip}
        drawCircle(muscle.copy(alpha=.35f),size.height*.06f,point(emphasis.first,emphasis.second))
        limb(hip,knee);limb(knee,foot)
        drawLine(load,point(foot.first-.02f,foot.second),point(foot.first+.07f,foot.second),7f,androidx.compose.ui.graphics.StrokeCap.Round)
        if(frame==1){
            val start=if(pattern=="hinge")point(.40f,.54f)else point(.77f,.32f);val end=if(pattern=="hinge")point(.27f,.54f)else point(.77f,.60f)
            drawLine(load,start,end,3f);val dir=end-start;val len=dir.getDistance().coerceAtLeast(1f);val unit=dir/len;val wing=androidx.compose.ui.geometry.Offset(-unit.y,unit.x);drawLine(load,end,end-unit*12f+wing*7f,3f);drawLine(load,end,end-unit*12f-wing*7f,3f)
            if(pattern=="hinge")drawLine(muscle.copy(alpha=.7f),point(shoulder.first,shoulder.second),point(hip.first,hip.second),2f)
        }
        if(pattern=="plank"){limb(shoulder,.32f to .90f);limb(.32f to .90f,hand);if(bent)limb(foot,.90f to .96f)}else limb(shoulder,hand,skin)
        if(pattern=="split"){limb(hip,.30f to .76f);val rear=if(id=="split-squat").24f to .63f else .24f to .92f;limb(.30f to .76f,rear);if(id=="split-squat"){drawLine(ink,point(.17f,.66f),point(.30f,.66f),4f);drawLine(ink,point(.20f,.66f),point(.20f,.96f),4f)}}
        val head=if(horizontal)point(shoulder.first-.07f,shoulder.second)else point(shoulder.first,shoulder.second-.10f);val headRadius=size.height*.065f
        drawCircle(skin,headRadius,head)
        drawArc(hair,190f,180f,true,head-Offset(headRadius,headRadius),androidx.compose.ui.geometry.Size(headRadius*2,headRadius*2))
        drawCircle(hair,headRadius*.35f,head-Offset(headRadius*.9f,0f));drawCircle(ink,headRadius*.08f,head+Offset(headRadius*.45f,0f))
        drawLine(ink.copy(alpha=.3f),point(.15f,.96f),point(.85f,.96f),3f)
        if(pattern !in listOf("plank","calf")&&id!="leg-press"){
            val location=when(pattern){"squat"->shoulder;"bridge"->hip;"curl"->foot;else->hand}
            drawLine(load,point(location.first-.09f,location.second),point(location.first+.09f,location.second),7f);drawCircle(load,8f,point(location.first-.09f,location.second));drawCircle(load,8f,point(location.first+.09f,location.second))
        }
    }
}
