package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.presentation.*
import org.racedaydreams.domain.strength.*

@RunWith(AndroidJUnit4::class)
class SharedHomeAndStrengthTodayTest {
    @get:Rule val compose=createComposeRule()
    private val day=LocalDate.of(2026,11,15)
    private val actions=mutableListOf<String>()
    private fun show(completed:Boolean=false,planned:Boolean=false,dark:Boolean=false,large:Boolean=false) {
        val session=if(planned)GymProgramme.Session("session/id",day,"Upper",listOf(GymProgramme.Prescription("bench-press",2,6,8,null,null,6,8,120,"Original target")),"Original") else null
        val summary=StrengthTodaySummary(session,completed,day.minusDays(6),if(planned)1 else 0,if(completed)1 else 0,if(completed)1 else 0,0,null)
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large)2f else 1f)) {
                RaceDayTheme(darkTheme=dark,dynamicColour=false) {
                    SharedStrengthTodayScreen(summary,day,"Sunday, 15 November",planned,if(planned)45 else null,{actions.add(it)},{actions.add("session:$it")},{actions.add("gym-now")})
                }
            }
        }
    }
    @Test fun strengthOnlyStartsIndependentlyWithoutARaceOrAnAutomaticAssociation() {
        show()
        compose.onNodeWithText("Build a programme or log a workout independently. No race goal is needed.").assertExists()
        compose.onNodeWithText("0 actual strength workouts recorded").assertExists()
        compose.runOnIdle {assertEquals(emptyList<String>(),actions)}
        compose.onNode(hasText("Start unplanned workout") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle {assertEquals(listOf("gym-now"),actions)}
    }
    @Test fun plannedWorkoutKeepsRawSessionIdentityAndSeparateLogging() {
        show(planned=true,dark=true)
        compose.onNodeWithText("Bench Press").assertExists()
        compose.onNodeWithText("0 / 1 programmed sessions completed").assertExists()
        compose.onNodeWithText("View workout details").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(listOf("session:session/id"),actions)}
        compose.onNodeWithText("Log today's workout").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(listOf("session:session/id","strength?date=2026-11-15"),actions)}
    }
    @Test fun completedWorkoutAt200PercentKeepsEvidenceAndHistoryActionsScrollable() {
        show(completed=true,planned=true,dark=true,large=true)
        compose.onNodeWithText("Actual workout recorded and explicitly linked to this session.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start planned workout").assertDoesNotExist()
        compose.onNodeWithText("1 / 1 programmed sessions completed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 actual strength workout recorded").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Strength workout history").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(listOf("history?kind=STRENGTH"),actions)}
    }
    @Test fun homeKeepsUnknownEvidenceHonestAndReviewIsAnExplicitAction() {
        compose.setContent {RaceDayTheme(dynamicColour=false){CoachPage{SharedHomeCoachingCard(HomeCoachingSummary.from(null)){actions.add("review")}}}}
        compose.onNodeWithText("Still learning your training").assertIsDisplayed()
        compose.onNodeWithText("Plan recommendation: Continue gathering evidence").assertIsDisplayed()
        compose.runOnIdle {assertEquals(emptyList<String>(),actions)}
        compose.onNodeWithText("Weekly coaching review").performClick()
        compose.runOnIdle {assertEquals(listOf("review"),actions)}
    }
}
