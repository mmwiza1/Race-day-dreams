package org.racedaydreams.coach

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme

/** Settings refinements: no self-link in the top bar and the last opened section is remembered. */
@RunWith(AndroidJUnit4::class)
class SettingsRefinementTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settingsDoesNotOfferItselfInTheTopBarButOtherScreensDo() {
        var route by mutableStateOf("settings")
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                CoachShell(route, false, remember { SnackbarHostState() }, {}, {}, {}) {
                    Text("Hosted content")
                }
            }
        }
        compose.onNodeWithText("Back").assertExists()
        compose.onNodeWithText("Settings").assertDoesNotExist()
        route = "shoes"
        compose.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun theLastOpenedSectionReopensWhenSettingsIsShownAgainInTheSession() {
        val lastOpened = mutableStateOf<String?>(null)
        var shown by mutableStateOf(true)
        compose.setContent {
            RaceDayTheme(dynamicColour = false) {
                if (shown)
                    ExpandableSection("Data", "Backup", "settings-data", lastOpened = lastOpened) {
                        Text("Backup controls")
                    }
            }
        }
        compose.onNodeWithText("Backup controls").assertDoesNotExist()
        compose.onNodeWithTag("settings-data").performClick()
        compose.onNodeWithText("Backup controls").assertExists()
        compose.runOnIdle { assertEquals("settings-data", lastOpened.value) }
        // Leaving and returning: a fresh section reopens from the session memory.
        shown = false
        compose.waitForIdle()
        shown = true
        compose.onNodeWithText("Backup controls").assertExists()
        // Closing it clears the memory, so the next visit starts closed.
        compose.onNodeWithTag("settings-data").performClick()
        compose.runOnIdle { assertEquals(null, lastOpened.value) }
    }
}
