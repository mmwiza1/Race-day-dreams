package org.racedaydreams.coach
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.strength.TechniqueGuide

/** App-owned schematic movement drawings, rendered offline from the actual guide. */
@Composable fun TechniqueSection(id:String){
    val guide=TechniqueGuide.forExercise(id)
    if(guide==null){Text("A visual guide for this exercise is not available yet. Choose a familiar movement or seek qualified coaching.");return}
    Text("How to do it",style=MaterialTheme.typography.titleLarge)
    listOf("Setup" to guide.setup(),"Movement" to guide.movement()).forEachIndexed{i,pair->CoachCard{
        Text("${i+1}. ${pair.first}",style=MaterialTheme.typography.titleMedium)
        TechniqueDrawing(guide.pattern(),i,pair.second)
        Text(pair.second)
    }}
    Text("Key cues",style=MaterialTheme.typography.titleMedium);guide.cues().forEach{Text("• $it")}
    if(id=="romanian-deadlift")Text("Brace your trunk · hinge at the hips · keep the bar close · control the lowering phase")
    Text("Common mistakes",style=MaterialTheme.typography.titleMedium);guide.mistakes().forEach{Text("• $it")}
    Text("Primary muscles: ${guide.muscles()}")
    Text("Alternatives: ${guide.alternatives().joinToString()}")
    WhyDetails("General technique guidance. Start conservatively and seek qualified in-person coaching if you are unsure about a movement. Drawings are schematic and do not prescribe an exact joint angle.","About this guidance")
}
@Composable private fun TechniqueDrawing(pattern:String,frame:Int,description:String){
    val ink=MaterialTheme.colorScheme.onSurface;val load=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(130.dp).semantics{contentDescription="Schematic $pattern, ${if(frame==0)"setup"else "movement"}: $description"}){
        fun point(x:Float,y:Float)=Offset(size.width*x,size.height*y)
        fun limb(a:Pair<Float,Float>,b:Pair<Float,Float>)=drawLine(ink,point(a.first,a.second),point(b.first,b.second),6f)
        val bent=frame==1
        val horizontal=pattern in listOf("plank","press","bridge","curl")
        val hip=if(horizontal) .48f to .58f else if(bent&&pattern in listOf("hinge","row","squat","split")) .42f to .62f else .48f to .48f
        val shoulder=if(horizontal) .30f to .48f else if(bent&&pattern in listOf("hinge","row")) .60f to .40f else .48f to .25f
        val knee=if(horizontal).65f to .63f else if(bent&&pattern in listOf("squat","split")) .60f to .68f else .48f to .70f
        val foot=if(horizontal).80f to .72f else .48f to .92f
        val hand=when(pattern){"overhead","pull"-> .62f to (if(bent).06f else .32f);"press"-> .30f to (if(bent).15f else .43f);"row"->if(bent).53f to .45f else .62f to .68f;else-> .62f to (if(bent).65f else .49f)}
        limb(shoulder,hip);limb(hip,knee);limb(knee,foot);limb(shoulder,hand)
        if(pattern=="split"){limb(hip,.30f to .76f);limb(.30f to .76f,.24f to .92f)}
        drawCircle(ink,size.height*.065f,point(shoulder.first,shoulder.second-.10f))
        drawLine(ink.copy(alpha=.3f),point(.15f,.96f),point(.85f,.96f),3f)
        if(pattern !in listOf("plank","calf")){drawLine(load,point(hand.first-.09f,hand.second),point(hand.first+.09f,hand.second),7f);drawCircle(load,8f,point(hand.first-.09f,hand.second));drawCircle(load,8f,point(hand.first+.09f,hand.second))}
    }
}
