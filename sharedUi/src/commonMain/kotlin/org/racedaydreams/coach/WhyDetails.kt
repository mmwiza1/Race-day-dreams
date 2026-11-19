package org.racedaydreams.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun WhyDetails(text: String, title: String = "Why?") {
    WhyDetails(title) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun WhyDetails(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(if (expanded) "Hide details" else title) },
            trailingContent = {
                Text(
                    if (expanded) "−" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "Collapse details" else "Expand details",
                    ) {
                        expanded = !expanded
                    },
        )
        if (expanded)
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
    }
}
