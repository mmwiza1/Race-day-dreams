package org.racedaydreams.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.racedaydreams.R

/** Curated original assets only. A missing entry is NOT a claim of illustrated coverage. */
data class GuidanceArtwork(val resource:Int,val description:String,val phases:List<String>)
fun exerciseArtwork(id:String):GuidanceArtwork?=when(id) {
    "romanian-deadlift" -> GuidanceArtwork(R.drawable.guide_romanian_deadlift,"Romanian deadlift: standing with the bar close to the thighs; then hips move backwards with softly bent knees, a neutral back and the bar close to the shins.",listOf("Stand tall","Hinge at the hips"))
    "bench-press" -> GuidanceArtwork(R.drawable.guide_bench_press,"Bench press: lie supported with feet planted, lower the bar over the chest with bent elbows, then press above the chest with arms extended.",listOf("Controlled lowering","Press above the chest"))
    "walking-lunge" -> GuidanceArtwork(R.drawable.guide_walking_lunge,"Walking lunge: begin standing; step forward and bend both knees with the front foot flat, torso upright and back knee lowered under control.",listOf("Stand tall","Step and lower"))
    else -> null
}
fun routineArtwork(id:String):GuidanceArtwork?=when(id) {
    "lunge" -> exerciseArtwork("walking-lunge")
    "ankle" -> GuidanceArtwork(R.drawable.guide_ankle_rocks,"Ankle rocks: half-kneeling with the front foot flat, move the front knee gently forward over the toes while keeping the heel down.",listOf("Half-kneeling","Rock gently; heel down"))
    else -> null
}
@Composable fun IllustratedMovement(art:GuidanceArtwork) {
    Image(painterResource(art.resource),contentDescription=art.description,
        modifier=Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(22.dp)),contentScale=ContentScale.Fit)
}
