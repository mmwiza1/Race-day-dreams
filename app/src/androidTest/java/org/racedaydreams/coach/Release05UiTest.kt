package org.racedaydreams.coach
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.reminders.EveningReminders
import java.time.*
import java.util.EnumSet
class Release05UiTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private var scenario:ActivityScenario<CoachActivity>?=null
    @Before fun seed(){EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear();SqliteUserPreferencesRepository(context).save(UserProfile(RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),TrainingPreferences(TrainingStrategy.ZONE_2,EnumSet.allOf(DayOfWeek::class.java),3,DayOfWeek.SUNDAY,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false));FeatureStore(context).putSetting("baseline.reviewed","true")}
    @After fun clear(){scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    private fun await(t:String){compose.waitUntil(20000){compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun launch(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?")}
    @Test fun exerciseAliasUsesOneCanonicalDefinitionAndFavouritesPersist(){launch();compose.onNodeWithText("Strength").performClick();await("Exercises");compose.onNodeWithText("Exercises").performScrollTo().performClick();await("Search exercises or aliases");compose.onNodeWithText("Search exercises or aliases").performTextReplacement("RDL");compose.onNodeWithText("Romanian Deadlift").performClick();await("Favourite");compose.onNodeWithText("Favourite").performScrollTo().performClick();compose.waitUntil(10000){StrengthUserStore(context).choices().any{it.exerciseId()=="romanian-deadlift"&&it.favourite()}};compose.onNodeWithText("How to do it").performClick();await("Key cues");compose.onNodeWithText("Key cues").performScrollTo().assertExists()}
    @Test fun progressUsesDashboardAndOneUsefulEmptyPbState(){launch();compose.onNodeWithText("Progress").performClick();await("Running PBs");compose.onNodeWithText("Training trends").assertExists();compose.onNodeWithText("Body weight").assertExists();compose.onNodeWithText("Running PBs").performClick();compose.waitUntil(20000){compose.onAllNodesWithText("No running PBs yet.",substring=true).fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("1K").assertDoesNotExist();compose.onNodeWithText("View all distances").performClick();compose.onNodeWithText("1K").assertExists()}
    @Test fun gymProgrammingIsOptionalAndPersistsAnApprovedBlock(){
        launch()
        compose.onNodeWithText("Strength").performClick();await("Gym programme")
        compose.onNodeWithText("Gym programme").performClick();await("Create a gym programme")
        Assert.assertNull(GymProgrammeStore(context).current())
        compose.onNodeWithText("Create a gym programme").performClick();await("Your gym preferences")
        compose.onNodeWithText("Preview six-week block").performScrollTo().performClick();await("Preview — not applied")
        Assert.assertNull(GymProgrammeStore(context).current())
        compose.onNodeWithText("Accept this programme").performScrollTo().performClick()
        compose.waitUntil(20000){GymProgrammeStore(context).current()!=null}
        Assert.assertEquals(12,GymProgrammeStore(context).current().sessions().size)
        await("Change programme settings")
        compose.onNodeWithText("Stop generated programming").performScrollTo().performClick()
        compose.waitUntil(20000){GymProgrammeStore(context).current()==null}
        Assert.assertEquals(1,GymProgrammeStore(context).versions().size)
    }
}
