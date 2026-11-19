package org.racedaydreams.coach

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Original RDD navigation paths, shared verbatim. Keep identity/order consistent with Android. */
private val navigationVectors by lazy {
    mapOf(
            "home" to "M3,10 L12,2 L21,10 L19,10 L19,21 L14,21 L14,14 L10,14 L10,21 L5,21 L5,10 Z",
            "plan" to
                "M7,3 L9,3 L9,2 L15,2 L15,3 L19,3 L19,22 L5,22 L5,3 Z M8,7 L16,7 L16,5 L8,5 Z M8,11 L16,11 L16,10 L8,10 Z M8,15 L16,15 L16,14 L8,14 Z M8,19 L14,19 L14,18 L8,18 Z",
            "calendar" to
                "M5,2 L7,2 L7,4 L17,4 L17,2 L19,2 L19,4 L22,4 L22,22 L2,22 L2,4 L5,4 Z M4,9 L4,20 L20,20 L20,9 Z M7,12 L10,12 L10,15 L7,15 Z M14,12 L17,12 L17,15 L14,15 Z",
            "progress" to
                "M3,3 L5,3 L5,19 L22,19 L22,21 L3,21 Z M7,15 L11,10 L15,12 L20,5 L22,6 L16,15 L12,13 L9,17 Z",
            "strength" to
                "M1,9 L3,9 L3,6 L6,6 L6,10 L18,10 L18,6 L21,6 L21,9 L23,9 L23,15 L21,15 L21,18 L18,18 L18,14 L6,14 L6,18 L3,18 L3,15 L1,15 Z",
        )
        .mapValues { (name, path) ->
            ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
                .apply {
                    addPath(
                        pathData = PathParser().parsePathString(path).toNodes(),
                        fill = SolidColor(Color.White),
                        pathFillType = PathFillType.EvenOdd,
                    )
                }
                .build()
        }
}

@Composable
fun CoachDestinationIcon(destination: String) {
    Icon(
        navigationVectors[destination] ?: navigationVectors.getValue("strength"),
        contentDescription = null,
    )
}
