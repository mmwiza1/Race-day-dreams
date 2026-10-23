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
        val profile=UserProfile(RaceGoal(RaceDistance.HALF_MARATHON,"Riverside Half",today.plusWeeks(12),GoalMode.TARGET_TIME,Duration.ofSeconds(7199)),
            TrainingPreferences(TrainingStrategy.BALANCED,EnumSet.allOf(DayOfWeek::class.java),4,today.dayOfWeek,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false)
        SqliteUserPreferencesRepository(context).save(profile)
        val store=FeatureStore(context)
        store.putSetting("baseline.reviewed","true")
        store.saveManualBaseline(ManualBaselineInput(25.0,4.0,12.0,Duration.ofMinutes(27),null,null,390.0,8))
        store.saveShoe(Shoe("demo-shoe","Demo","Daily trainer",today.minusDays(80),120000.0,false,"Test-only screenshot fixture",true))
        val evidence=DevelopmentStore(context)
        for(i in 0..5) {val at=today.minusWeeks((5-i).toLong()).atTime(12,0).atOffset(ZoneOffset.UTC);evidence.saveWeight(org.racedaydreams.domain.body.BodyWeight("demo-weight-$i",75.0+i*.15,at.toInstant(),at.offset,"manual","demo-weight-$i",null))}
        evidence.saveHistorical(HistoricalStrengthRecord("demo-historical",StrengthExercise.starters().first(),75.0,5,null,HistoricalStrengthRecord.DatePrecision.UNKNOWN,"Before using the app; test-only fixture"))
        val observations=mutableListOf<org.racedaydreams.domain.health.HealthIntelligence.Observation>()
        for(i in 0..34)for(metric in org.racedaydreams.domain.health.HealthIntelligence.Metric.values()){val day=today.minusDays(i.toLong());val time=day.atStartOfDay(ZoneOffset.UTC).toInstant();val value=when(metric){org.racedaydreams.domain.health.HealthIntelligence.Metric.SLEEP_MINUTES->450.0;org.racedaydreams.domain.health.HealthIntelligence.Metric.RESTING_HR->55.0;org.racedaydreams.domain.health.HealthIntelligence.Metric.HRV_RMSSD->48.0;else->8000.0};observations.add(org.racedaydreams.domain.health.HealthIntelligence.Observation("demo-health-$metric-$i",metric,day,time,time,value,"Test-only demo source",Instant.now()))}
        HealthIntelligenceStore(context).replace(org.racedaydreams.domain.health.HealthIntelligence.Metric.values().toSet(),today.minusDays(34),today,observations)
        val now=Instant.now()
        val baseline=RunnerBaseline(25.0,4.0,12.0,45.0,390.0,8,Duration.ofMinutes(27),null,null,24,8,now.minus(Duration.ofDays(84)),now,now.minus(Duration.ofDays(1)),BaselineConfidence.STRONG,emptyMap(),emptyList(),emptyList(),emptyList())
        val plan=OriginalTrainingPlanGenerator().generate(profile,baseline,today.minusWeeks(2).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
        val version=PlanVersion("demo-v1",plan.id(),1,now,"Runner requested programme",profile,"Screenshot fixture: manually entered 25 km/week, 12 km longest run and eight consistent weeks. Not real health data.",plan)
        val coach=CoachingStore(context);coach.saveCurrent(version)
        val gymSettings=GymProgramme.Settings(GymProgramme.Goal.SUPPORT_RUNNING,GymProgramme.Priority.RUNNING_FIRST,GymProgramme.Experience.INTERMEDIATE,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),2,setOf("full gym"),45,GymProgramme.Effort.RPE)
        GymProgrammeStore(context).save(GymProgrammeGenerator().generate("demo-gym",1,now,"Test-only deterministic programme",gymSettings,today,plan.workouts(),emptyList()))
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
        // A deterministic native trace uses the real store and analysis, never production demo data.
        val trace=org.racedaydreams.domain.recording.RunMetricsEngine(0,now.minusSeconds(4000))
        for(i in 0..1800){val angle=i*2*Math.PI/1800;trace.accept(org.racedaydreams.domain.recording.RunMetricsEngine.Fix(now.minusSeconds(4000).plusSeconds(i.toLong()),i*1000L,51.5+.01*kotlin.math.sin(angle),-.1+.017*kotlin.math.cos(angle),3.0,30+3*kotlin.math.sin(angle),3.0,3.0))}
        val recordings=RecordingStore(context);recordings.start("demo-native-route",now.minusSeconds(4000),0);recordings.flush("demo-native-route",trace.points(),trace.events(),0,0,1800000,"ACTIVE");recordings.finish(recordings.unfinished()!!,1800000,recovered=false)
        // Fix only the test assignment; production still randomly assigns and persists once.
        store.activitySummaries().forEachIndexed{i,a->a.distanceMetres()?.takeIf{it>0}?.let{metres->val route=org.racedaydreams.domain.support.DreamRoutes.routes()[i%5];SupportStore(context).save("dream",a.id(),org.json.JSONObject().put("version",1).put("route",route.id()).put("start",kotlin.math.min(10000.0,kotlin.math.max(0.0,42195-metres))).put("distance",metres))}}
    }
    @After fun clear(){org.racedaydreams.recording.RunRecordingService.status.value=null;org.racedaydreams.recording.RunRecordingService.workoutState.value=null;org.racedaydreams.recording.RunRecordingService.savedActivity.value=null;scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    @Test fun capturesCurrentBuildScreensAndVerifiesCoachingNavigation() {
        scenario=ActivityScenario.launch(CoachActivity::class.java)
        await("Today");capture("home")
        compose.onNodeWithText("Weekly coaching review").performScrollTo().performClick();await("Recommendation");capture("weekly-review");back()
        click("Plan");await("This week");capture("training-plan")
        compose.onAllNodesWithContentDescription("View workout and purpose")[0].performScrollTo().performClick()
        await("Workout structure");capture("planned-workout")
        compose.onNode(hasText("Warm-up ·",substring=true)).performScrollTo().performClick();compose.waitUntil(20000){compose.onAllNodes(hasText("1 of ",substring=true)).fetchSemanticsNodes().isNotEmpty()};capture("warm-up");click("Next");await("Ankle rocks");capture("exercise-ankle-rocks");back()

        back();click("Calendar");await("Activity calendar");compose.waitUntil(20000){compose.onAllNodes(hasStateDescription("Recorded history ready")).fetchSemanticsNodes().isNotEmpty()};capture("history")
        click("Week");await("Training calendar");capture("calendar-week")
        click("Month");await(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM uuuu",java.util.Locale.UK)));capture("calendar-month")
        click("List");await("Activity calendar")
        // Open the first real retained activity through the Calendar screen.
        compose.waitUntil(20000){compose.onAllNodes(hasStateDescription("Recorded history ready")).fetchSemanticsNodes().isNotEmpty()};compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Recorded run"));compose.onAllNodesWithText("Recorded run")[0].performClick()
        await("Run detail");capture("activity-detail");alignHeading("Your recorded route");capture("post-run-route")
        val dreamHeading=compose.onNode(hasText("Dream Route ·",substring=true)).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text;alignHeading(dreamHeading);capture("dream-route")
        compose.onNodeWithText("Compare this run").performScrollTo().performClick();alignHeading("Compare this run");capture("run-comparison")
        alignHeading("Run Analysis");capture("run-feedback")
        back();click("Progress");await("Running PBs");capture("progress")
        click("Body weight");await("Record a measurement");capture("body-weight");back()
        click("Running PBs");await("How PBs are calculated");capture("running-pbs");back()
        click("Training trends");await("Weekly recorded distance");capture("training-trends");back()
        click("Strength");await("Log workout");capture("strength")
        click("PBs & progression");await("Exercise progress and PB history");compose.onAllNodesWithText("View progression")[0].performScrollTo().performClick();alignHeading("Exercise progress and PB history");capture("strength-pbs");back()
        compose.onNodeWithText("Exercises").performScrollTo().performClick();await("Search exercises or aliases")
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("de");compose.waitForIdle();capture("exercise-search")
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("RDL");compose.onNodeWithText("Romanian Deadlift").performClick();await("Overview");capture("exercise-rdl")
        click("How to do it");compose.waitForIdle();capture("exercise-rdl-howto");back()
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Bench");compose.onNodeWithText("Bench Press").performClick();await("Overview");click("How to do it");capture("exercise-bench-howto");back()
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("BSS");compose.onNodeWithText("Bulgarian Split Squat").performClick();await("Overview");capture("exercise-bulgarian-split-squat");back();back()
        click("Log workout");await("Log strength workout");click("Log strength workout");alignHeading("Last time");capture("strength-log-previous")
        compose.onNodeWithText("Back Squat").performScrollTo().performClick();compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Plank");compose.onNode(hasText("Plank") and hasClickAction() and !hasSetTextAction()).performClick();compose.onNodeWithText("Duration (seconds or m:ss)").performScrollTo().performTextReplacement("1:30");compose.waitForIdle();capture("strength-log-timed")
        compose.onNode(hasText("Plank") and hasClickAction() and !hasSetTextAction()).performScrollTo().performClick();compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Farmer");compose.onNodeWithText("Farmer’s Carry").performClick();compose.onNodeWithText("Distance (metres)").performScrollTo().performTextReplacement("60");compose.waitForIdle();capture("strength-log-distance");back()
        compose.onNodeWithText("Gym programme").performScrollTo().performClick();await("Change programme settings");capture("gym-running-support")
        val next=GymProgrammeStore(context).current().sessions().first{it.date()>=LocalDate.now()};compose.onNodeWithText("${friendlyDate(next.date())} · ${next.title()}").performScrollTo().performClick();await("Why this session?");capture("gym-workout-detail");back()
        compose.onNodeWithText("Change programme settings").performScrollTo().performClick();await("Your gym preferences");alignHeading("Your gym preferences");capture("gym-programme-setup")
        compose.onNodeWithText("Get stronger").performScrollTo().performClick();compose.onNodeWithText("Strength first").performScrollTo().performClick();compose.onNodeWithText("Preview six-week block").performScrollTo().performClick();await("Preview — not applied");compose.onNodeWithText("Accept this programme").performScrollTo().performClick();compose.waitUntil(20000){GymProgrammeStore(context).current().settings().priority()==GymProgramme.Priority.STRENGTH_FIRST};compose.waitForIdle();capture("gym-strength-first");back()
        click("⚙ Settings");await("Settings")
        capture("settings");alignHeading("Activity data source");capture("data-sources")
        compose.onNodeWithText("Personal health context").performScrollTo().performClick();await("Personal health context");compose.waitForIdle();capture("health-context");back();back()
        click("Home");compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick();await("Custom workout");capture("run-now");compose.onNodeWithText("Free run").performScrollTo().performClick();await("Start GPS recording");capture("record-run")
        val live=org.racedaydreams.domain.execution.WorkoutExecutionEngine(listOf(org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step("Controlled interval",WorkoutSegment.Kind.RUN,800.0,null,295.0,305.0,7.0)))
        val liveState=live.update(org.racedaydreams.domain.execution.WorkoutExecutionEngine.Input(200.0,60000,60000,300.0,true,false))
        InstrumentationRegistry.getInstrumentation().runOnMainSync{org.racedaydreams.recording.RunRecordingService.workoutState.value=liveState;org.racedaydreams.recording.RunRecordingService.status.value=org.racedaydreams.domain.recording.RunMetricsEngine.Status(200.0,60000,60000,300.0,"GPS available",false)}
        await("600 m remaining");capture("live-coach")
        InstrumentationRegistry.getInstrumentation().runOnMainSync{org.racedaydreams.recording.RunRecordingService.status.value=null;org.racedaydreams.recording.RunRecordingService.workoutState.value=null}
        back();back();click("Home");compose.onNodeWithText("Race preparation").performScrollTo().performClick();await("Your pacing strategy");capture("race-day");back()
        click("Plan");compose.onNodeWithText("View routines").performScrollTo().performClick();await("Movement support");compose.onNodeWithText("Start Hips & glutes").performScrollTo().performClick();await("Hips & glutes");capture("mobility");click("Next");await("Walking lunges");capture("exercise-walking-lunge");click("Next");click("Next");await("Hip-flexor stretch");capture("exercise-static-stretch");back();back();click("⚙ Settings");await("Settings")
        compose.onNodeWithText("Edit race goal and availability").performScrollTo().performClick()
        compose.waitForIdle();capture("onboarding")
        back();click("⚙ Settings");await("Settings")
        compose.onNodeWithText("Shoes and mileage").performScrollTo().performClick();await("Your shoes");capture("shoes")
        scenario?.close();scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?")
        compose.onNodeWithText("View activity history").performScrollTo().performClick();await("Search and date filters");click("Search and date filters");capture("history-filters")
        val originalFont=shell("settings get system font_scale").trim()
        val originalNight=shell("cmd uimode night").let{if(it.contains("yes"))"yes"else if(it.contains("auto"))"auto"else "no"}
        try {
            shell("cmd uimode night yes")
            scenario?.close();scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?")
            Assert.assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES,context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            capture("home-dark")
            click("Plan");await("This week");capture("plan-dark")
            compose.onNodeWithText("View routines").performScrollTo().performClick();await("Movement support");compose.onNodeWithText("Start Hips & glutes").performScrollTo().performClick();await("Hips & glutes");capture("mobility-dark");back();back();click("Home");await("How am I doing?")
            shell("settings put system font_scale 2.0");scenario?.recreate();await("How am I doing?")
            Assert.assertTrue(context.resources.configuration.fontScale>=1.99f)
            capture("home-large-font")
            compose.onNodeWithContentDescription("Plan").performClick();await("This week");capture("plan-large-font")
        } finally {
            if(originalFont.matches(Regex("[0-9.]+")))shell("settings put system font_scale $originalFont")else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }

    }
    private fun shell(command:String):String=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use{descriptor->java.io.FileInputStream(descriptor.fileDescriptor).use{it.readBytes().toString(Charsets.UTF_8)}}
    private fun back(){androidx.test.espresso.Espresso.closeSoftKeyboard();androidx.test.espresso.Espresso.pressBack();compose.waitForIdle()}
    private fun await(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun click(text:String){compose.onNodeWithText(text).performClick();compose.waitForIdle()}
    private fun alignHeading(text:String){
        compose.onNodeWithText(text).performScrollTo()
        // Anchor the actual heading near the top, independent of content above it.
        repeat(4){
            val top=compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top
            val height=compose.onRoot().fetchSemanticsNode().boundsInRoot.height
            val shift=(top-160f).coerceAtMost(height*.6f)
            if(shift<=8f)return
            compose.onRoot().performTouchInput{swipe(Offset(centerX,height*.85f),Offset(centerX,height*.85f-shift),durationMillis=1000)}
            compose.waitForIdle()
        }
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
