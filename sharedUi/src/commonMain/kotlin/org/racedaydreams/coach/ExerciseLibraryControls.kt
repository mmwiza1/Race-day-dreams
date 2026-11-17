package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun LibrarySearchIcon() {
    val colour = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        drawCircle(
            colour,
            size.minDimension * .27f,
            Offset(size.width * .42f, size.height * .42f),
            style = Stroke(2.dp.toPx()),
        )
        drawLine(
            colour,
            Offset(size.width * .62f, size.height * .62f),
            Offset(size.width * .88f, size.height * .88f),
            2.dp.toPx(),
        )
    }
}

@Composable
fun LibraryOpenIcon() {
    val colour = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(16.dp)) {
        drawLine(
            colour,
            Offset(size.width * .35f, size.height * .2f),
            Offset(size.width * .65f, size.height * .5f),
            2.dp.toPx(),
        )
        drawLine(
            colour,
            Offset(size.width * .65f, size.height * .5f),
            Offset(size.width * .35f, size.height * .8f),
            2.dp.toPx(),
        )
    }
}

@Composable
fun LibraryMovementIcon() {
    val colour = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxSize()) {
        drawLine(
            colour,
            Offset(size.width * .2f, size.height * .5f),
            Offset(size.width * .8f, size.height * .5f),
            2.dp.toPx(),
        )
        for (x in listOf(.2f, .8f)) drawLine(
            colour,
            Offset(size.width * x, size.height * .25f),
            Offset(size.width * x, size.height * .75f),
            4.dp.toPx(),
        )
    }
}

/** Compact, readable at200%; name/alias matching remains owned by the existing callers. */
@Composable
fun CoachExerciseSearch(query: String, change: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = change,
        label = { Text("Search") },
        placeholder = { Text("Name or alias") },
        modifier =
            Modifier.fillMaxWidth().testTag("exercise-search").semantics {
                contentDescription = "Search exercises by name or alias"
            },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        leadingIcon = { LibrarySearchIcon() },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}
