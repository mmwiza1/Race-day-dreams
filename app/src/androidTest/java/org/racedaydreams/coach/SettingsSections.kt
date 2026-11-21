package org.racedaydreams.coach

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/**
 * Opens a collapsible Settings section unless it is already open. Settings reopens the section
 * last opened in the session, so a plain tap could otherwise close it.
 */
fun SemanticsNodeInteractionsProvider.openSettingsSection(tag: String) {
    val section = onNodeWithTag(tag).performScrollTo()
    if (section.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription) != "Expanded")
        section.performClick()
}
