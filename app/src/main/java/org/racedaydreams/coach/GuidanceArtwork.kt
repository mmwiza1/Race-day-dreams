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

/** Only reviewed assets are counted. Missing artwork is visible to the release gate. */
data class GuidanceArtwork(val resource:Int,val description:String,val phases:List<String>)
@Composable private fun artwork(spec:org.racedaydreams.domain.strength.TechniqueIllustrations.Artwork?):GuidanceArtwork? {
    if(spec==null)return null
    val context=androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(spec.asset()) {
        val id=context.resources.getIdentifier(spec.asset(),"drawable",context.packageName)
        check(id!=0){"Missing bundled guidance asset"}
        GuidanceArtwork(id,spec.description(),spec.phases())
    }
}
@Composable fun exerciseArtwork(id:String)=artwork(org.racedaydreams.domain.strength.TechniqueIllustrations.forExercise(id))
@Composable fun routineArtwork(id:String)=artwork(org.racedaydreams.domain.strength.TechniqueIllustrations.forRoutine(id))
@Composable fun IllustratedMovement(art:GuidanceArtwork) {
    Image(painterResource(art.resource),contentDescription=art.description,
        modifier=Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(22.dp)),contentScale=ContentScale.Fit)
}
