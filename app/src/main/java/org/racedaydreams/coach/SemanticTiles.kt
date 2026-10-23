package org.racedaydreams.coach
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class Tile { Planned,Progress,Insight,Race,Strength,Achievement,Warning,Neutral }
@Composable fun tileColour(tile:Tile):Color {
    val dark=isSystemInDarkTheme()
    return Color(if(dark)when(tile){Tile.Planned->0xFF193B32;Tile.Progress->0xFF342A45;Tile.Insight->0xFF44351D;Tile.Race->0xFF193A40;Tile.Strength->0xFF293C32;Tile.Achievement->0xFF47391A;Tile.Warning->0xFF482B2B;Tile.Neutral->0xFF292C2E}
    else when(tile){Tile.Planned->0xFFE7F4EE;Tile.Progress->0xFFF1EDF9;Tile.Insight->0xFFFFF4DF;Tile.Race->0xFFE5F3F3;Tile.Strength->0xFFEAF1EA;Tile.Achievement->0xFFFFF3D1;Tile.Warning->0xFFFDECEA;Tile.Neutral->0xFFF4F4F5})
}
@Composable fun SemanticCard(tile:Tile,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=tileColour(tile),contentColor=MaterialTheme.colorScheme.onSurface)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
