package org.racedaydreams.coach
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.health.*
import java.time.*
import java.util.EnumSet

@RunWith(AndroidJUnit4::class)
class RecordingAvailabilityTest {
 @get:Rule val compose=createEmptyComposeRule()
 private val context:Context get()=ApplicationProvider.getApplicationContext()
 private var scenario:ActivityScenario<CoachActivity>?=null
 @Before fun seed(){
  SqliteUserPreferencesRepository(context).clear()
  SqliteUserPreferencesRepository(context).save(UserProfile(RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),TrainingPreferences(TrainingStrategy.ZONE_2,EnumSet.allOf(DayOfWeek::class.java),3,DayOfWeek.SUNDAY,EnumSet.noneOf(DayOfWeek::class.java),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false))
  FeatureStore(context).putSetting("baseline.reviewed","true")
  val wall=Instant.now().minusSeconds(3600);val engine=RunMetricsEngine(0,wall)
  engine.accept(RunMetricsEngine.Fix(wall,0,0.0,0.0,3.0,null,null,3.0));engine.accept(RunMetricsEngine.Fix(wall.plusSeconds(1),1000,0.0,0.00003,3.0,null,null,3.0))
  val store=RecordingStore(context);store.start("availability-fixture",wall,0);store.flush("availability-fixture",engine.points(),engine.events(),0,0,1000,"ACTIVE");store.finish(store.unfinished()!!,1000)
 }
 @After fun clean(){scenario?.close();SqliteUserPreferencesRepository(context).clear()}
 @Test fun writePermissionActionReflectsActualSdkAvailabilityWithoutBlockingRun(){
  val available=HealthConnectGateway(context).availability()==HealthAvailability.AVAILABLE
  scenario=ActivityScenario.launch(CoachActivity::class.java)
  compose.waitUntil(20000){compose.onAllNodesWithText("Today").fetchSemanticsNodes().isNotEmpty()}
  compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick();compose.onNodeWithText("Free run").performScrollTo().performClick()
  compose.waitUntil(20000){compose.onAllNodesWithText("Start GPS recording").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Start GPS recording").assertIsEnabled()
  compose.waitUntil(20000){compose.onAllNodes(hasText("to Health Connect",substring=true) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()}
  val write=compose.onNode(hasText("to Health Connect",substring=true) and hasClickAction()).performScrollTo()
  if(available)write.assertIsEnabled()else{write.assertIsNotEnabled();compose.onNodeWithText("Health Connect is not currently available. Local recording and route export still work.").assertExists()}
 }
}
