package org.racedaydreams.coach

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import java.util.EnumSet
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.reminders.EveningReminders

@RunWith(AndroidJUnit4::class)
class WeeklyUxTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private var scenario:ActivityScenario<CoachActivity>?=null
    @Before fun setup(){EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear();SqliteUserPreferencesRepository(context).save(UserProfile(RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),TrainingPreferences(TrainingStrategy.ZONE_2,EnumSet.allOf(DayOfWeek::class.java),3,DayOfWeek.SUNDAY,EnumSet.of(DayOfWeek.WEDNESDAY),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false));FeatureStore(context).putSetting("baseline.reviewed","true")}
    @After fun clear(){scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    private fun launch(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("Your running, your way")}
    private fun await(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun enter(label:String,value:String){compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)}
    @Test fun calendarViewsPersistAndProvideVisibleLegend(){launch();compose.onNodeWithText("Calendar").performClick();await("Activity calendar");compose.onNodeWithText("Week").performClick();await("Training calendar");compose.onNodeWithText("Legend:",substring=true).assertExists();compose.onNodeWithText("Month").performClick();compose.waitUntil(10000){FeatureStore(context).getSetting("calendar.view")=="Month"};scenario!!.recreate();await("Training calendar");compose.onNodeWithText("Legend:",substring=true).assertExists();compose.onNodeWithText("List").performClick();await("Activity calendar")}
    @Test fun primaryStrengthSupportsUnknownHistoricalPbWithoutInventingWorkout(){launch();compose.onNodeWithText("Strength").performClick();await("Log strength workout");compose.onNodeWithText("Add historical PB").performScrollTo().performClick();enter("Load (kg; added load for weighted bodyweight)","105");enter("Reps","5");compose.onNodeWithText("Save historical PB").performScrollTo().performClick();compose.waitUntil(10000){DevelopmentStore(context).historical().size==1};Assert.assertNull(DevelopmentStore(context).historical()[0].date());Assert.assertTrue(CoachingStore(context).strength().isEmpty())}
    @Test fun progressRecordsWeightWithNoHealthPermissionOrFakeActivity(){launch();compose.onNodeWithText("Progress").performClick();await("Body weight");compose.onNodeWithText("Record a measurement").performScrollTo().performClick();enter("Weight (kg)","80");compose.onNodeWithText("Add weight").performScrollTo().performClick();compose.waitUntil(10000){DevelopmentStore(context).weights().size==1};Assert.assertEquals(80.0,DevelopmentStore(context).weights()[0].kilograms(),.00001);Assert.assertTrue(FeatureStore(context).activities().isEmpty());compose.onNodeWithText("Latest:",substring=true).performScrollTo().assertExists()}
    @Test fun settingsIsDiscoverableOutsideBottomNavigation(){launch();compose.onNodeWithText("⚙ Settings").performClick();await("Activity data source");compose.onNodeWithText("Samsung Health — experimental").performScrollTo().performClick();compose.waitUntil(10000){FeatureStore(context).getSetting("provider.preference")=="SAMSUNG_EXPERIMENTAL"};compose.onNodeWithText("Samsung direct SDK is not included",substring=true).performScrollTo().assertExists()}
}
