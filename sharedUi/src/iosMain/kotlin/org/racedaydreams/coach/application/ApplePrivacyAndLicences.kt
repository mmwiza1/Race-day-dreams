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
        "Third-party inventory and source notices" to "third_party.md",
        "Apache License 2.0" to "apache-2.0.txt",
        "Kotlin licence" to "kotlin-LICENSE.txt",
        "Coroutines licence" to "coroutines-LICENSE.txt",
        "Serialization licence" to "serialization-LICENSE.txt",
        "Skia graphics licence" to "skia-LICENSE.txt",
        "Graphics engine component notices" to "ios-native-NOTICES.txt",
        "Berlin route source licence" to "berlin-2014-MIT.txt",
        "Tokyo route source licence" to "tokyo-2026-CC0.txt",
        "London route source licence" to "london-historical-MIT.txt",
        "New York route source attribution" to "new-york-2026-NOTICE.txt",
        "Paris 2026 annual route source attribution" to "paris-2026-NOTICE.txt",
        "Open Database Licence 1.0" to "ODbL-1.0.txt",
        "Dream Route sources and attributions" to "dream-basemaps-NOTICE.txt",
        "Dependency notices" to "transitive-NOTICES.txt",
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
