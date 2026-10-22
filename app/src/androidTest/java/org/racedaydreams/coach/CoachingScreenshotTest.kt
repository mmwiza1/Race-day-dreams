package org.racedaydreams.coach

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.pressBack
import java.io.File
import java.time.*
import java.util.EnumSet
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.reminders.EveningReminders

/** Fixtures are compiled only into the instrumentation APK, never the installable app. */
@RunWith(AndroidJUnit4::class)
class CoachingScreenshotTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private var scenario:ActivityScenario<CoachActivity>?=null
    @Before fun seed() {
        EveningReminders(context,FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        val today=LocalDate.now()
        val profile=UserProfile(RaceGoal(RaceDistance.HALF_MARATHON,"Demo Riverside Half",today.plusWeeks(12),GoalMode.TARGET_TIME,Duration.ofSeconds(7199)),
            TrainingPreferences(TrainingStrategy.BALANCED,EnumSet.allOf(DayOfWeek::class.java),4,today.dayOfWeek,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false)
        SqliteUserPreferencesRepository(context).save(profile)
        val store=FeatureStore(context)
        store.putSetting("baseline.reviewed","true")
        store.saveManualBaseline(ManualBaselineInput(25.0,4.0,12.0,Duration.ofMinutes(27),null,null,390.0,8))
        store.saveShoe(Shoe("demo-shoe","Demo","Daily trainer",today.minusDays(80),120000.0,false,"Test-only screenshot fixture",true))
        val evidence=DevelopmentStore(context)
        for(i in 0..5) {val at=today.minusWeeks((5-i).toLong()).atTime(12,0).atOffset(ZoneOffset.UTC);evidence.saveWeight(org.racedaydreams.domain.body.BodyWeight("demo-weight-$i",75.0+i*.15,at.toInstant(),at.offset,"manual","demo-weight-$i",null))}
        evidence.saveHistorical(HistoricalStrengthRecord("demo-historical",StrengthExercise.starters().first(),75.0,5,null,HistoricalStrengthRecord.DatePrecision.UNKNOWN,"Before using the app; test-only fixture"))
        val now=Instant.now()
        val baseline=RunnerBaseline(25.0,4.0,12.0,45.0,390.0,8,Duration.ofMinutes(27),null,null,24,8,now.minus(Duration.ofDays(84)),now,now.minus(Duration.ofDays(1)),BaselineConfidence.STRONG,emptyMap(),emptyList(),emptyList(),emptyList())
        val plan=OriginalTrainingPlanGenerator().generate(profile,baseline,today.minusWeeks(2).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
        val version=PlanVersion("demo-v1",plan.id(),1,now,"Runner requested programme",profile,"Screenshot fixture: manually entered 25 km/week, 12 km longest run and eight consistent weeks. Not real health data.",plan)
        val coach=CoachingStore(context);coach.saveCurrent(version)
        plan.workouts().filter{it.date()<today}.forEachIndexed { i,w ->
            val date=w.date().atTime(8,0).atOffset(ZoneOffset.UTC)
            val km=w.distanceKm() ?: 5.0
            val duration=Duration.ofSeconds((km*390).toLong())
            val samples=(0..duration.seconds.toInt() step 10).map{second->DistanceSample(Duration.ofSeconds(second.toLong()),km*1000*second/duration.seconds,null,null,null,false)}
            val run=ActivityRecord("demo-run-$i",SourceIdentity("manual","demo-run-$i",null),date.toInstant(),date.toInstant().plus(duration),ZoneOffset.UTC,ZoneOffset.UTC,km*1000,null,null,null,null,null,3.0,"Deterministic screenshot fixture; manually entered example",samples,emptyList(),w.id(),"demo-shoe")
            store.upsertManualActivity(run)
            coach.saveResponse(RunExecution(run.id(),w.id(),version.id(),null,null,"Test-only fixture"))
        }
        val exercise=StrengthExercise.starters().first()
        evidence.saveHistorical(HistoricalStrengthRecord("demo-old",exercise,70.0,5,today.minusDays(90),HistoricalStrengthRecord.DatePrecision.EXACT,"Test-only historical evidence"))
        evidence.saveHistorical(HistoricalStrengthRecord("demo-approx",exercise,72.5,5,today.minusDays(60),HistoricalStrengthRecord.DatePrecision.APPROXIMATE,"Approximate date; test-only"))
        coach.saveStrength(StrengthSession("demo-strength",plan.workouts().filter{it.date()<today}.maxBy{it.date()}.date().atTime(18,0).toInstant(ZoneOffset.UTC),ZoneOffset.UTC,Duration.ofMinutes(50),7.0,"Test-only fixture",
            listOf(StrengthSession.ExerciseSets(exercise,listOf(StrengthSession.SetEntry("demo-set1",5,80.0,7.0,false),StrengthSession.SetEntry("demo-set2",5,80.0,7.0,false),StrengthSession.SetEntry("demo-set3",5,80.0,7.0,false))))))
    }
    @After fun clear(){scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    @Test fun capturesCurrentBuildScreensAndVerifiesCoachingNavigation() {
        scenario=ActivityScenario.launch(CoachActivity::class.java)
        await("Your running, your way");capture("home")
        click("Weekly coaching review");await("Recommendation");capture("weekly-review");pressBack()
        click("Plan");await("Your training programme");capture("training-plan")
        compose.onAllNodesWithText("View workout and purpose")[0].performScrollTo().performClick()
        await("Workout structure");capture("planned-workout")
        pressBack();click("Calendar");await("Activity calendar");capture("history")
        click("Week");await("Training calendar");capture("calendar-week")
        click("Month");await(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM uuuu",java.util.Locale.UK)));capture("calendar-month")
        click("List");await("Activity calendar")
        // Open the first real retained activity through the Calendar screen.
        compose.onAllNodesWithText("Recorded run")[0].performScrollTo().performClick()
        await("Run detail");capture("activity-detail")
        alignHeading("Run Score and feedback");capture("run-feedback")
        pressBack();click("Progress");await("Strength progress");capture("progress");alignHeading("Body weight");capture("body-weight")
        compose.onNodeWithText("View Strength progress").performScrollTo().performClick();await("Latest sessions");capture("strength");alignHeading("Exercise progress and PB history");compose.onAllNodesWithText("View progression")[0].performScrollTo().performClick();capture("strength-pbs")
        pressBack();click("⚙ Settings");await("Settings")
        capture("settings");alignHeading("Activity data source");capture("data-sources")
        compose.onNodeWithText("Edit race goal and availability").performScrollTo().performClick()
        compose.waitForIdle();capture("onboarding")
        pressBack();click("⚙ Settings");await("Settings")
        compose.onNodeWithText("Shoes and mileage").performScrollTo().performClick();await("Your shoes");capture("shoes")
    }
    private fun await(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun click(text:String){compose.onNodeWithText(text).performClick();compose.waitForIdle()}
    private fun alignHeading(text:String){
        compose.onNodeWithText(text).performScrollTo()
        // A slow real scroll gives the screenshot its own useful viewport instead
        // of merely making the heading one pixel visible at the bottom edge.
        compose.onRoot().performTouchInput {
            swipe(Offset(centerX,height*.8f),Offset(centerX,height*.35f),durationMillis=1000)
        }
        compose.waitForIdle()
    }
    private fun capture(name:String){
        compose.waitForIdle()
        // Compose idleness can precede SurfaceFlinger presenting the last frame.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(200)
        val semantics=compose.onRoot(useUnmergedTree=true).printToString()
        Assert.assertFalse("Raw ISO date in normal UI",Regex("Text = .*\\b[0-9]{4}-[0-9]{2}-[0-9]{2}").containsMatchIn(semantics))
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        Assert.assertNotNull(bitmap)
        val dir=File(context.getExternalFilesDir(null),"screenshots");dir.mkdirs()
        File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
        // AGP uninstalls the APK after connected tests, removing app-specific files.
        // Copy with instrumentation's shell identity before teardown; production has no such path.
        val shell=InstrumentationRegistry.getInstrumentation().uiAutomation
        shell.executeShellCommand("mkdir -p /data/local/tmp/racedaydreams-screenshots").use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
        shell.executeShellCommand("cp ${File(dir,"$name.png").absolutePath} /data/local/tmp/racedaydreams-screenshots/$name.png").use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }
}
