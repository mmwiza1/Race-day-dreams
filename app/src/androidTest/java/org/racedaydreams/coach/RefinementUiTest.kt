package org.racedaydreams.coach

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith
import java.time.*
import java.util.EnumSet
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.reminders.EveningReminders

@RunWith(AndroidJUnit4::class)
class RefinementUiTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private var scenario:ActivityScenario<CoachActivity>?=null
    private val today=LocalDate.now()
    @Before fun seed(){
        EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()
        val profile=UserProfile(RaceGoal(RaceDistance.TEN_K,"",today.plusWeeks(8),GoalMode.FINISH,null),TrainingPreferences(TrainingStrategy.BALANCED,EnumSet.allOf(DayOfWeek::class.java),3,today.dayOfWeek,emptySet(),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false)
        SqliteUserPreferencesRepository(context).save(profile);FeatureStore(context).putSetting("baseline.reviewed","true")
        val ws=listOf(PlannedWorkout("long",today,WorkoutCategory.LONG_RUN,10.0,null,emptyList(),"Endurance"))
        CoachingStore(context).saveCurrent(PlanVersion("v","p",1,Instant.now(),"Original",profile,"Test fixture",TrainingPlan("p",today.minusDays(7),today.plusWeeks(8),emptyList(),ws)))
    }
    @After fun clear(){scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    private fun launch(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("Your running, your way")}
    private fun await(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun enter(label:String,value:String){compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)}
    @Test fun monthHasCategoryLabelsAndNoGenericCountDescription(){
        launch();compose.onNodeWithText("Calendar").performClick();await("Activity calendar");compose.onNodeWithText("Month").performClick();await("Training calendar")
        listOf("Easy","Quality","Long","Strength","Race","Rest").forEach{compose.onAllNodesWithText(it).filter(hasAnyAncestor(hasContentDescription("Workout category legend"))).assertCountEquals(1)}
        Assert.assertTrue(compose.onAllNodesWithContentDescription("Long run: planned",substring=true).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("Legend: outline planned",substring=true).assertExists()
        compose.onNode(hasContentDescription(friendlyDate(today),substring=true) and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Long run · 10.0 km · Planned").assertExists()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Week").performClick();await("Training calendar");compose.onNodeWithText("Legend:",substring=true).assertExists()
    }
    @Test fun graphSelectsAStoredMeasurementAndKeepsTextEquivalent(){
        DevelopmentStore(context).saveWeight(BodyWeight("a",78.4,today.minusDays(2).atTime(12,0).toInstant(ZoneOffset.UTC),ZoneOffset.UTC,"manual","a",null))
        launch();compose.onNodeWithText("Progress").performClick();await("Body weight")
        compose.onNodeWithContentDescription("Weight graph in kg.",substring=true).performScrollTo().performTouchInput{click(center)}
        compose.onNodeWithText("Recorded measurement").performScrollTo().assertExists()
        compose.onNodeWithText("Recorded measurements").performScrollTo().performClick()
        compose.onNodeWithText("${shortDate(today.minusDays(2))}: 78.4 kg").assertExists()
    }
    @Test fun unifiedProgressionShowsUnknownHistoricalAndRecordedEvidence(){
        val e=StrengthExercise.starters().first()
        DevelopmentStore(context).saveHistorical(HistoricalStrengthRecord("old",e,75.0,5,null,HistoricalStrengthRecord.DatePrecision.UNKNOWN,""))
        CoachingStore(context).saveStrength(StrengthSession("s",Instant.now().minusSeconds(86400),ZoneOffset.UTC,Duration.ofMinutes(45),7.0,"",listOf(StrengthSession.ExerciseSets(e,listOf(StrengthSession.SetEntry("set",5,80.0,7.0,false))))))
        launch();compose.onNodeWithText("Strength").performClick();await("Exercise progress and PB history")
        compose.onNodeWithText("View progression").performScrollTo().performClick()
        compose.onNodeWithText("Date unknown · not placed in chronology").performScrollTo().assertExists()
        compose.onAllNodesWithText("Recorded workout").fetchSemanticsNodes().let{Assert.assertTrue(it.isNotEmpty())}
        compose.onNodeWithText("Historical/manual PB").assertExists()
    }
    @Test fun editingASetRetainsItUntilUpdated(){
        launch();compose.onNodeWithText("Strength").performClick();await("Log strength workout");compose.onNodeWithText("Log strength workout").performClick()
        enter("Weight (kg)","80");enter("Reps","5");compose.onNodeWithText("Add set").performScrollTo().performClick()
        compose.onNodeWithText("Edit").performScrollTo().performClick();enter("Weight (kg)","85")
        compose.onNodeWithText("Update set").performScrollTo().performClick();compose.onNodeWithText("1: 85.0 kg × 5").performScrollTo().assertExists()
        compose.onNodeWithText("Duplicate previous set").performScrollTo().performClick();compose.onNodeWithText("2: 85.0 kg × 5").performScrollTo().assertExists()
    }
}
