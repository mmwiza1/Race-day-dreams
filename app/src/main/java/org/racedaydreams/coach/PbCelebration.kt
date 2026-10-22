package org.racedaydreams.coach
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
/** Persistent explicit acknowledgement, never a date-window guess or a notification. */
@Composable fun PbCelebration(state:CoachUiState,model:CoachViewModel,id:String,comparison:String?=null){
    if(id in state.acknowledgedPbs){Text("★ Personal best",style=MaterialTheme.typography.labelLarge);return}
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant),border=androidx.compose.foundation.BorderStroke(2.dp,Color(0xFFB88A24)),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp)){Text("★ NEW PB",style=MaterialTheme.typography.titleMedium);comparison?.let{Text(it)};TextButton(onClick={model.acknowledgePb(id)}){Text("Acknowledge PB")}}
    }
}
