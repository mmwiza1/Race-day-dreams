package org.racedaydreams.coach

import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.support.DreamRoutes

/** Real bundled decode and publication, including cache eviction and retained composition reuse. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DreamArtworkReadinessTest {
    // Async decode/path completion must enqueue work for the UI test clock rather than
    // resume an unconfined test recomposer inline on the completing IO/Default worker.
    @get:Rule val compose = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun eachCityPublishesItsArtworkAndReadinessTogetherWhenSwitching() {
        val routes = DreamRoutes.routes().map { DreamRoutes.currentGeometry(it) }
        val selected = mutableStateOf(routes.first())
        val shown = mutableStateOf(true)
        compose.setContent {
            SideEffect {
                assertSame(
                    "Composition must apply on Android's main thread",
                    Looper.getMainLooper(),
                    Looper.myLooper(),
                )
            }
            MaterialTheme {
                CoachPage {
                    val route = selected.value
                    if (shown.value)
                        DreamRouteExperience(
                            route,
                            DreamRoutes.Assignment(route.id(), 1000.0, 7190.0),
                        )
                }
            }
        }
        repeat(3) {
            routes.forEach { route ->
                compose.runOnIdle { selected.value = route }
                compose.waitUntil(20000) {
                    compose
                        .onAllNodes(
                            hasStateDescription("Offline Dream artwork ready") and
                                hasAnyDescendant(hasText("Dream Route · ${route.name()}")),
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .size == 1
                }
                compose.onNodeWithText("Dream Route · ${route.name()}").assertExists()
                val city = dreamCityPresentation(route)
                compose
                    .onNodeWithContentDescription(
                        "Original illustrated ${city.city} skyline. Atmospheric city art, not geographic landmark placement.",
                        useUnmergedTree = true,
                    )
                    .assertExists()
                city.landmarks.forEach { landmark ->
                    compose.onNodeWithText(landmark.label, useUnmergedTree = true).assertExists()
                }
            }
        }
        // Exercise actual removal/disposal before the ActivityScenario teardown as well.
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        compose
            .onAllNodes(hasStateDescription("Offline Dream artwork ready"), useUnmergedTree = true)
            .assertCountEquals(0)
    }
}
