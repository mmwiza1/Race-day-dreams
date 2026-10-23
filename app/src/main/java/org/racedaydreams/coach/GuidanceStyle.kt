package org.racedaydreams.coach

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared soft blue/navy guidance surfaces. No screen owns its own palette. */
@Composable fun guidanceBlue() = if (isSystemInDarkTheme()) Color(0xFF1B354C) else Color(0xFFEEF7FF)
@Composable fun guidanceMint() = if (isSystemInDarkTheme()) Color(0xFF183C38) else Color(0xFFEDF9F5)
@Composable fun guidancePink() = if (isSystemInDarkTheme()) Color(0xFF472B38) else Color(0xFFFFF0F3)
@Composable fun GuidanceCard(colour: Color = guidanceBlue(), padding: androidx.compose.ui.unit.Dp = 18.dp, spacing: androidx.compose.ui.unit.Dp = 8.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colour, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
@Composable fun GuidanceNote(title: String, text: String, caution: Boolean = false) = GuidanceCard(if (caution) guidancePink() else guidanceMint()) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
