package org.racedaydreams.coach.application

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachCard
import platform.Foundation.NSBundle

private val bundledNotices =
    listOf(
        "App licence (MIT)" to "LICENSE",
        "Apache License 2.0" to "apache-2.0.txt",
        "Dream Route sources and attributions" to "dream-basemaps-NOTICE.txt",
        "Open Database Licence 1.0" to "ODbL-1.0.txt",
    )

@Composable
internal fun ApplePrivacyAndLicences() {
    CoachCard {
        Text("Privacy", style = MaterialTheme.typography.titleLarge)
        Text(
            "Your records are saved locally. Apple Health access is optional. Race Day Dreams has no account, advertising, analytics or subscription."
        )
        Text("The app does not encrypt exported backup files. Save them somewhere you trust.")
    }
    CoachCard {
        Text("About and licences", style = MaterialTheme.typography.titleLarge)
        var selected by remember { mutableStateOf<String?>(null) }
        bundledNotices.forEach { (label, asset) ->
            val expanded = selected == asset
            TextButton(
                onClick = { selected = if (expanded) null else asset },
                modifier =
                    Modifier.semantics {
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    },
            ) {
                Text(label)
            }
            if (expanded) AppleLicenceText(asset)
        }
    }
}

@Composable
private fun AppleLicenceText(asset: String) {
    var result by remember(asset) { mutableStateOf<String?>(null) }
    LaunchedEffect(asset) {
        result =
            try {
                withContext(applicationIoDispatcher()) {
                    val path = NSBundle.mainBundle.pathForResource(asset, ofType = null)
                    path?.let {
                        readAppleBundleFile(it, 262_144L)
                            ?.decodeToString(throwOnInvalidSequence = true)
                    }
                } ?: "This licence could not be read."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                "This licence could not be read."
            }
    }
    SelectionContainer { Text(result ?: "Loading licence…") }
}
