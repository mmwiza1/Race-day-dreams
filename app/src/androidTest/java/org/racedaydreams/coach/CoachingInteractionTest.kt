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
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.reminders.EveningReminders

/** Real form/navigation tests: fixtures seed intentions only, actions create actual app records. */
@RunWith(AndroidJUnit4::class)
class CoachingInteractionTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private var scenario:ActivityScenario<CoachActivity>?=null
    private lateinit var profile:UserProfile
    @Before fun setup(){
        EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()
        profile=UserProfile(RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),TrainingPreferences(TrainingStrategy.ZONE_2,
            EnumSet.allOf(DayOfWeek::class.java),3,DayOfWeek.SUNDAY,EnumSet.noneOf(DayOfWeek::class.java),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false)
        SqliteUserPreferencesRepository(context).save(profile)
        FeatureStore(context).putSetting("baseline.reviewed","true")
    }
    @After fun cleanup(){scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    @Test fun runnerCanGenerateOneCurrentProgrammeWithoutHealthData(){
        launch();compose.onNodeWithText("Plan").performClick();await("Generate my programme")
        compose.onNodeWithText("Generate my programme").performScrollTo().performClick()
        compose.waitUntil(20000){CoachingStore(context).current()!=null}
        val current=CoachingStore(context).current()!!
        Assert.assertTrue(current.plan().workouts().isNotEmpty())
        Assert.assertEquals(1,CoachingStore(context).versions().size)
        Assert.assertTrue(current.plan().workouts().all{it.distanceKm()==null&&it.duration()!=null})
        Assert.assertEquals(profile.raceGoal(),SqliteUserPreferencesRepository(context).load().get().raceGoal())
        await("Rebuild remaining programme…")
        compose.onNodeWithText("Rebuild remaining programme…").performScrollTo().performClick()
        await("Keep current plan");compose.onNodeWithText("Keep current plan").performClick()
        Assert.assertEquals(current,CoachingStore(context).current())
    }
    @Test fun actualRunFormPreservesPrescriptionAndShowsPurposeScore(){
        seedToday();launch();compose.onNodeWithText("Plan").performClick();await("View workout and purpose")
        compose.onNodeWithText("View workout and purpose").performScrollTo().performClick();await("Record actual run")
        compose.onNodeWithText("Record actual run").performScrollTo().performClick()
        enter("Actual distance (km)","5");enter("Actual duration (mm:ss or h:mm:ss)","32:30")
        enter("Start date (YYYY-MM-DD)",LocalDate.now().minusDays(1).toString());enter("Start time (HH:mm, local)","08:00")
        enter("RPE 0–10 (optional)","3")
        compose.onNodeWithText("Save actual run").performScrollTo().performClick()
        compose.waitUntil(20000){FeatureStore(context).activities().size==1}
        await("Run Score and feedback")
        compose.onNodeWithText("Run Score and feedback").performScrollTo()
        await("10/10 — Good execution")
        Assert.assertEquals(5.0,CoachingStore(context).current()!!.plan().workouts()[0].distanceKm()!!,0.00001)
        Assert.assertEquals("today-workout",FeatureStore(context).activities()[0].plannedWorkoutId())
        Assert.assertEquals("test-version",CoachingStore(context).responses()[0].planVersionId())
    }
    @Test fun strengthFormPersistsOrderedSetsAndPbEvidence(){
        launch();compose.onNodeWithText("Progress").performClick();await("Optional strength training")
        compose.onNodeWithText("Optional strength training").performScrollTo().performClick();await("Log strength workout")
        compose.onNodeWithText("Log strength workout").performClick()
        enter("Date (YYYY-MM-DD)",LocalDate.now().minusDays(1).toString());enter("Start time (HH:mm, local)","12:00")
        enter("Duration (minutes)","50");enter("Session RPE 0–10 (optional)","7")
        enter("Working sets (e.g. 80,5,7)","80,5,7\n90,5,8")
        compose.onNodeWithText("Add exercise and sets").performScrollTo().performClick()
        compose.onNodeWithText("Save strength workout").performScrollTo().performClick()
        compose.waitUntil(20000){CoachingStore(context).strength().size==1}
        val sets=CoachingStore(context).strength()[0].exercises()[0].sets()
        Assert.assertEquals(80.0,sets[0].kilograms(),0.00001);Assert.assertEquals(90.0,sets[1].kilograms(),0.00001)
        Assert.assertTrue(org.racedaydreams.domain.strength.StrengthAnalytics.progression(CoachingStore(context).strength()).any{it.category()=="Load × 5"&&it.kilograms()==90.0})
    }
    private fun seedToday(){
        val today=LocalDate.now()
        val workout=PlannedWorkout("today-workout",today,WorkoutCategory.EASY_RUN,5.0,null,
            listOf(WorkoutSegment(WorkoutSegment.Kind.RUN,5.0,null,null,null,null,3.0,"Conversational effort")),"Test-only prescription for the real completion form")
        CoachingStore(context).saveCurrent(PlanVersion("test-version","test-plan",1,Instant.now(),"Runner-requested fixture",profile,"Test-only intentions",
            TrainingPlan("test-plan",today,today.plusWeeks(8),emptyList(),listOf(workout))))
    }
    private fun launch(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("Your running, your way")}
    private fun await(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun enter(label:String,value:String){compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)}
}
