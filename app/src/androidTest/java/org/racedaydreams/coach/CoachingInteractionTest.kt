package org.racedaydreams.coach

import android.content.Context
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
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
    @Test fun plannedTreadmillEntryKeepsItsPrescriptionAndNoRouteEvidence() {
        seedToday()
        launch()
        compose.onNodeWithText("Plan").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithContentDescription("View workout and purpose").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("View workout and purpose").performScrollTo().performClick()
        await("Record actual run")
        compose.onNodeWithText("Record actual run").performScrollTo().performClick()
        compose.onNodeWithText("Indoor treadmill run").performScrollTo().performClick()
        enter("Actual distance (km)", "5")
        enter("Actual duration (mm:ss or h:mm:ss)", "31:20")
        enter("Start time (HH:mm, local)", "00:01")
        compose.onNodeWithText("Save treadmill run").performScrollTo().performClick()
        compose.waitUntil(20_000) { FeatureStore(context).activitySummaries().size == 1 }
        val result = FeatureStore(context).activitySummaries().single()
        Assert.assertEquals(org.racedaydreams.domain.activity.RunEnvironment.TREADMILL, result.environment())
        Assert.assertEquals("manual", result.source().provider())
        Assert.assertEquals("today-workout", result.plannedWorkoutId())
        Assert.assertEquals(Duration.ofSeconds(1880), result.elapsedTime())
        Assert.assertTrue(result.samples().isEmpty())
        Assert.assertTrue(result.laps().isEmpty())
        Assert.assertNull(result.movingTime())
        Assert.assertEquals(5.0, CoachingStore(context).current()!!.plan().workouts().single().distanceKm()!!, 0.0)
        Assert.assertEquals("test-version", CoachingStore(context).responses().single().planVersionId())
    }

    @Test fun actualRunFormPreservesPrescriptionAndShowsPurposeScore(){
        seedToday();recordActualRun("Actual distance (km)","5")
        await("10/10 — Good execution")
        Assert.assertEquals(5.0,CoachingStore(context).current()!!.plan().workouts()[0].distanceKm()!!,0.00001)
        Assert.assertEquals("today-workout",FeatureStore(context).activities()[0].plannedWorkoutId())
        Assert.assertEquals("test-version",CoachingStore(context).responses()[0].planVersionId())
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun strengthFormPersistsOrderedSetsAndPbEvidence(){
        launch();compose.onNodeWithText("Strength").performClick();await("Log workout")
        compose.onNodeWithText("Log workout").performScrollTo().performClick();await("Log strength workout")
        compose.onNodeWithText("Log strength workout").performClick()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        compose.onNodeWithText("Session date: ${friendlyDate(today)}").performScrollTo()
            .assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick)
        await("Choose date")
        if (yesterday.month != today.month)
            compose.onNodeWithContentDescription("Change to previous month").performClick()
        val accessibleDate = requireNotNull(DatePickerDefaults.dateFormatter().formatDate(
            yesterday.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            java.util.Locale.getDefault(),
            forContentDescription = true,
        ))
        capturePart2(context, "historical-date-picker")
        compose.onNode(hasText(accessibleDate) and hasClickAction() and hasAnyAncestor(isDialog()))
            .assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText("Choose date").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Session date: ${friendlyDate(yesterday)}").assertExists()
        compose.onNodeWithText("Start time (HH:mm, local)").assertDoesNotExist()
        compose.onNodeWithText("Session details (optional)").performScrollTo().performClick()
        enter("Duration (minutes)","50");enter("Session RPE 0–10 (optional)","7")
        enter("Weight (kg)","80");enter("Reps","5")
        compose.onNodeWithText("Warm-up and effort (optional)").performScrollTo().performClick()
        enter("Set RPE 0–10 (optional)","7")
        compose.onNodeWithText("Add set").performScrollTo().performClick()
        enter("Weight (kg)","90");enter("Set RPE 0–10 (optional)","8")
        compose.onNodeWithText("Add set").performScrollTo().performClick()
        compose.onNodeWithText("Save strength workout").performScrollTo().performClick()
        compose.waitUntil(20000){CoachingStore(context).strength().size==1}
        val session=CoachingStore(context).strength().single()
        Assert.assertEquals(LocalDate.now().minusDays(1), session.startedAt().atOffset(session.offset()).toLocalDate())
        Assert.assertEquals(Duration.ofMinutes(50), session.duration())
        Assert.assertEquals(7.0, session.rpe()!!, 0.0)
        val sets=session.exercises().single().sets()
        Assert.assertEquals(80.0,sets[0].kilograms(),0.00001);Assert.assertEquals(90.0,sets[1].kilograms(),0.00001)
        Assert.assertTrue(org.racedaydreams.domain.strength.StrengthAnalytics.progression(CoachingStore(context).strength()).any{it.category()=="Load × 5"&&it.kilograms()==90.0})
    }
    @Test
    fun dateOnlyWorkoutCopiesDraftSetsAndSavesDistinctActuals() {
        val original = org.racedaydreams.domain.strength.StrengthSession(
            "yesterday", LocalDate.now().minusDays(1).atTime(12, 0)
                .atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault().rules.getOffset(Instant.now()), Duration.ofMinutes(30),
            null, "", listOf(org.racedaydreams.domain.strength.StrengthSession.ExerciseSets(
                requireNotNull(org.racedaydreams.domain.strength.ExerciseCatalog.definition("back-squat")).exercise(),
                listOf(org.racedaydreams.domain.strength.StrengthSession.SetEntry("original-set", 5, 80.0, null, false)),
            )),
        )
        CoachingStore(context).saveStrength(original)
        launch()
        compose.onNodeWithText("Strength").performClick()
        await("Log workout")
        compose.onNodeWithText("Log workout").performScrollTo().performClick()
        await("Log strength workout")
        compose.onNodeWithText("Log strength workout").performClick()

        compose.onNodeWithText("Use previous sets").assertIsDisplayed()
        compose.onNodeWithText("Session details (optional)").assertExists()
        compose.onNodeWithText("Duration (minutes)").assertDoesNotExist()
        capturePart2(context, "historical-entry")
        compose.onNodeWithText("Use last working set values").performScrollTo().performClick()
        compose.onNodeWithText("1: 80.0 kg × 5 reps").assertDoesNotExist()
        compose.onNodeWithText("Clear set entry").performScrollTo().performClick()
        compose.onNodeWithText("Use previous sets").performScrollTo().performClick()
        compose.onNodeWithText("Copied as a draft. Keep only the sets you actually did.").assertExists()
        compose.onNodeWithText("Duplicate previous set").performScrollTo().performClick()
        val setsBeforeSave = CoachingStore(context).strength().single()
        Assert.assertEquals(original, setsBeforeSave)
        compose.onNodeWithText("Save strength workout").performScrollTo().performClick()
        compose.waitUntil(20_000) { CoachingStore(context).strength().size == 2 }
        val logged = CoachingStore(context).strength().first()
        Assert.assertEquals(LocalDate.now(), logged.startedAt().atOffset(logged.offset()).toLocalDate())
        Assert.assertFalse(logged.hasRecordedDuration())
        Assert.assertNull(logged.rpe())
        val newSets = logged.exercises().single().sets()
        Assert.assertEquals(listOf(80.0, 80.0), newSets.map { it.kilograms() })
        Assert.assertEquals(2, newSets.map { it.id() }.toSet().size)
        Assert.assertTrue(newSets.none { it.id() == "original-set" })
        Assert.assertEquals(original, CoachingStore(context).strength().last())

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
        launch();compose.onNodeWithText("Plan").performClick();compose.waitUntil(20000){compose.onAllNodesWithContentDescription("View workout and purpose").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("View workout and purpose").performScrollTo().performClick();await("Record actual run")
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

internal fun capturePart2(context: Context, name: String) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val label = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("part2Evidence")
        if (label !in setOf("light", "dark")) return
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        java.io.File(context.filesDir, "$name-$label.png").outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
