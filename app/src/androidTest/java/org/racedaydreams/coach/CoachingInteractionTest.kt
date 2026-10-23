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
        await("Programme details")
        compose.onNodeWithText("Programme details").performScrollTo().performClick()
        await("Rebuild remaining programme…")
        compose.onNodeWithText("Rebuild remaining programme…").performScrollTo().performClick()
        await("Keep current plan");compose.onNodeWithText("Keep current plan").performClick()
        Assert.assertEquals(current,CoachingStore(context).current())
    }
    @Test fun actualRunFormPreservesPrescriptionAndShowsPurposeScore(){
        seedToday();recordActualRun("Actual distance (km)","5")
        await("10/10 — Good execution")
        Assert.assertEquals(5.0,CoachingStore(context).current()!!.plan().workouts()[0].distanceKm()!!,0.00001)
        Assert.assertEquals("today-workout",FeatureStore(context).activities()[0].plannedWorkoutId())
        Assert.assertEquals("test-version",CoachingStore(context).responses()[0].planVersionId())
    }
    @Test fun strengthFormPersistsOrderedSetsAndPbEvidence(){
        launch();compose.onNodeWithText("Strength").performClick();await("Log workout")
        compose.onNodeWithText("Log workout").performScrollTo().performClick();await("Log strength workout")
        compose.onNodeWithText("Log strength workout").performClick()
        choosePastDate("Session date")
        enter("Start time (HH:mm, local)","00:01")
        enter("Duration (minutes)","50");enter("Session RPE 0–10 (optional)","7")
        enter("Weight (kg)","80");enter("Reps","5");enter("Set RPE 0–10 (optional)","7")
        compose.onNodeWithText("Add set").performScrollTo().performClick()
        enter("Weight (kg)","90");enter("Set RPE 0–10 (optional)","8")
        compose.onNodeWithText("Add set").performScrollTo().performClick()
        compose.onNodeWithText("Add exercise and sets").performScrollTo().performClick()
        compose.onNodeWithText("Save strength workout").performScrollTo().performClick()
        compose.waitUntil(20000){CoachingStore(context).strength().size==1}
        val sets=CoachingStore(context).strength()[0].exercises()[0].sets()
        Assert.assertEquals(80.0,sets[0].kilograms(),0.00001);Assert.assertEquals(90.0,sets[1].kilograms(),0.00001)
        Assert.assertTrue(org.racedaydreams.domain.strength.StrengthAnalytics.progression(CoachingStore(context).strength()).any{it.category()=="Load × 5"&&it.kilograms()==90.0})
    }
    @Test fun imperialActualDistanceConvertsOnceWithoutChangingPrescription(){
        val t=profile.preferences()
        profile=UserProfile(profile.raceGoal(),TrainingPreferences(t.strategy(),t.runningDays(),t.runsPerWeek(),t.longRunDay(),t.gymDays(),t.scheduleType(),UnitSystem.IMPERIAL),false)
        SqliteUserPreferencesRepository(context).save(profile)
        seedToday();recordActualRun("Actual distance (miles)","1")
        Assert.assertEquals(1609.344,FeatureStore(context).activities()[0].distanceMetres()!!,0.00001)
        Assert.assertEquals(5.0,CoachingStore(context).current()!!.plan().workouts()[0].distanceKm()!!,0.00001)
    }
    private fun recordActualRun(distanceLabel:String,distance:String){
        launch();compose.onNodeWithText("Plan").performClick();await("View workout and purpose")
        compose.onNodeWithText("View workout and purpose").performScrollTo().performClick();await("Record actual run")
        compose.onNodeWithText("Record actual run").performScrollTo().performClick()
        enter(distanceLabel,distance);enter("Actual duration (mm:ss or h:mm:ss)","32:30")
        choosePastDate("Start date")
        enter("Start time (HH:mm, local)","00:01")
        enter("RPE 0–10 (optional)","3")
        compose.onNodeWithText("Save actual run").performScrollTo().performClick()
        compose.waitUntil(20000){FeatureStore(context).activities().size==1}
        await("Run Score and feedback")
        compose.onNodeWithText("Run Score and feedback").performScrollTo()
    }
    private fun seedToday(){
        val today=LocalDate.now()
        val workout=PlannedWorkout("today-workout",today,WorkoutCategory.EASY_RUN,5.0,null,
            listOf(WorkoutSegment(WorkoutSegment.Kind.RUN,5.0,null,null,null,null,3.0,"Conversational effort")),"Test-only prescription for the real completion form")
        CoachingStore(context).saveCurrent(PlanVersion("test-version","test-plan",1,Instant.now(),"Runner-requested fixture",profile,"Test-only intentions",
            TrainingPlan("test-plan",today,today.plusWeeks(8),emptyList(),listOf(workout))))
    }
    private fun launch(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("Today")}
    private fun await(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun choosePastDate(label:String){
        compose.onNodeWithText("$label: ${friendlyDate(LocalDate.now())}").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Switch to text input mode").performClick()
        val pattern=android.text.format.DateFormat.getDateFormatOrder(context).joinToString(""){when(it){'M'->"MM";'d'->"dd";else->"yyyy"}}
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern(pattern)))
        compose.onNodeWithText("Choose date").performClick();compose.waitForIdle()
    }
    private fun enter(label:String,value:String){compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)}
}
