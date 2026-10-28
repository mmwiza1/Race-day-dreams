package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.*
import org.racedaydreams.coach.strength.ExerciseExclusionControl
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason

class ExerciseExclusionControlTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun explicitConfirmationAndRestoreAreDifferentFromHiding() {
        var excluded by mutableStateOf(false)
        var selected: Reason? = null
        compose.setContent {
            MaterialTheme {
                ExerciseExclusionControl("Bench Press", excluded, false) {
                    selected = it
                    excluded = it != null
                }
            }
        }
        compose.onNodeWithTag("exercise-exclude").performClick()
        compose.onNodeWithText("Equipment unavailable").performClick()
        compose.onNodeWithText("Exclude exercise").performClick()
        compose.runOnIdle {
            Assert.assertTrue(excluded)
            Assert.assertEquals(Reason.EQUIPMENT_UNAVAILABLE, selected)
        }
        compose.onNodeWithTag("exercise-restore").performClick()
        compose.runOnIdle {
            Assert.assertFalse(excluded)
            Assert.assertNull(selected)
        }
        compose.onNodeWithTag("exercise-exclude").assertIsDisplayed()
    }
}
