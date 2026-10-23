package org.racedaydreams.coach
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.*
import org.junit.*
import org.racedaydreams.data.*
class Release08UiTest {
 @get:Rule val compose=createEmptyComposeRule()
 private val context:Context get()=ApplicationProvider.getApplicationContext()
 private var scenario:ActivityScenario<CoachActivity>?=null
 @Before fun seed(){SqliteUserPreferencesRepository(context).clear();FeatureStore(context).putSetting("starting.path","record")}
 @After fun clean(){scenario?.close();SqliteUserPreferencesRepository(context).clear()}
 private fun await(s:String){compose.waitUntil(20000){compose.onAllNodesWithText(s).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun recordOnlyTodayWorksWithoutCreatingAProfile(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?");Assert.assertFalse(SqliteUserPreferencesRepository(context).load().isPresent);compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick();await("Custom workout");compose.onNodeWithText("Free run").assertExists();compose.onNodeWithText("5 km").assertExists();compose.onNodeWithText("Add step").performScrollTo().performClick();await("Workout step");compose.onNodeWithText("Save step").performClick();compose.onNodeWithText("Start custom workout").performScrollTo().assertExists();Assert.assertTrue(CoachingStore(context).responses().isEmpty())}
 @Test fun customFavouriteSurvivesReloadAndHasNoPlanMembership(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?");compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick();await("Custom workout");compose.onNodeWithText("Add step").performScrollTo().performClick();await("Workout step");compose.onNodeWithText("Save step").performClick();compose.onNodeWithText("Save favourite workout").performScrollTo().performClick();compose.waitUntil(20000){Release08Store(context).workouts().size==1};Assert.assertNull(CoachingStore(context).current());Assert.assertTrue(CoachingStore(context).responses().isEmpty())}
 @Test fun newUserCanChooseRecorderWithoutHealthOrGoalWall(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?");scenario?.close();SqliteUserPreferencesRepository(context).clear();scenario=ActivityScenario.launch(CoachActivity::class.java);await("Make today yours");compose.onNodeWithText("Record my runs").performClick();await("How am I doing?");Assert.assertFalse(SqliteUserPreferencesRepository(context).load().isPresent);compose.onNodeWithText("Strength").performClick();await("Log workout")}
}
