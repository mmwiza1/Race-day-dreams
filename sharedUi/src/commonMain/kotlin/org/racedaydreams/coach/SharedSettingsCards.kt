package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.presentation.AthleteLanguage
import org.racedaydreams.domain.settings.PrimaryFocus

@Composable
fun SharedPrimaryFocusSettings(state: CoachUiState, save: (PrimaryFocus) -> Unit) {
    var selected by
        rememberSaveable(state.primaryFocus) { mutableStateOf(state.primaryFocus?.storageValue()) }
    CoachCard {
        Text("Primary focus", style = MaterialTheme.typography.titleLarge)
        state.primaryFocus?.let { focus ->
            Text("Current primary focus: ${primaryFocusLabel(focus)}")
        }
        Text(
            "Choose the training you want to focus on. Every feature stays available. Changing focus preserves your goals, programmes and workout history."
        )
        PrimaryFocus.values().forEach { focus ->
            val label = primaryFocusLabel(focus)
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = selected == focus.storageValue(),
                        enabled = !state.busy,
                        role = Role.RadioButton,
                        onClick = { selected = focus.storageValue() },
                    )
                    .testTag("primary-focus-choice-${focus.storageValue()}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == focus.storageValue(),
                    onClick = null,
                    enabled = !state.busy,
                )
                Text(label)
            }
        }
        Button(
            onClick = { selected?.let { save(requireNotNull(PrimaryFocus.fromStorageValue(it))) } },
            enabled = !state.busy && selected != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save primary focus")
        }
    }
}

private fun primaryFocusLabel(focus: PrimaryFocus): String =
    when (focus) {
        PrimaryFocus.RUNNING -> "Running"
        PrimaryFocus.STRENGTH -> "Strength"
        PrimaryFocus.HYBRID -> "Hybrid"
    }

@Composable
fun SharedBackupSettings(
    busy: Boolean,
    export: () -> Unit,
    restore: (() -> Unit)? = null,
    completeExport: (() -> Unit)? = null,
    description: String = AthleteLanguage.BACKUP,
) = CoachCard {
    Text(
        if (restore == null) "Local backups" else "Local backup and restore",
        style = MaterialTheme.typography.titleLarge,
    )
    Text(description)
    Button(export, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Export local backup")
    }
    completeExport?.let {
        OutlinedButton(
            it,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Export complete backup")
        }
    }
    if (restore != null) {
        OutlinedButton(
            restore,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Choose backup to restore")
        }
        Text(
            "Restore validates the file before replacing local app data. Merge restore is not currently supported."
        )
    }
}
