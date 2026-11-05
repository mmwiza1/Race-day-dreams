package org.racedaydreams.coach

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.*
import java.util.EnumSet
import org.junit.*
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.racedaydreams.coach.strength.GymNowState
import org.racedaydreams.coach.strength.GymNowViewModel
import org.racedaydreams.coach.strength.GymSlotInput
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.reminders.EveningReminders

/** Fixtures are compiled only into the instrumentation APK, never the installable app. */
@RunWith(AndroidJUnit4::class)
@OptIn(InternalComposeUiApi::class)
class CoachingScreenshotTest {
    private val compose = createEmptyComposeRule()

    // These captures launch real production Activities, including lifecycle/AndroidView work.
    // Keep their normal Android UI dispatcher and Choreographer instead of an unconfined
    // test recomposer (worker-thread draw race) or a queued test clock (host startup stall).
    // The outer Compose rule still checks roots, semantics and layout; explicit application
    // readiness/content assertions below remain authoritative for async work.
    @get:Rule
    val androidWindows: TestRule =
        RuleChain.outerRule(compose)
            .around(
                TestRule { base, _ ->
                    object : Statement() {
                        override fun evaluate() {
                            val mainWindows = WindowRecomposerFactory { view ->
                                Assert.assertSame(Looper.getMainLooper(), Looper.myLooper())
                                WindowRecomposerFactory.LifecycleAware.createRecomposer(view)
                            }
                            WindowRecomposerPolicy.withFactory(mainWindows) { base.evaluate() }
                        }
                    }
                }
            )
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private var scenario: ActivityScenario<CoachActivity>? = null

    @Before
    fun seed() {
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        val today = LocalDate.now()
        val profile =
            UserProfile(
                RaceGoal(
                    RaceDistance.HALF_MARATHON,
                    "Riverside Half",
                    today.plusWeeks(12),
                    GoalMode.TARGET_TIME,
                    Duration.ofSeconds(7199),
                ),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    EnumSet.allOf(DayOfWeek::class.java),
                    4,
                    today.dayOfWeek,
                    EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                    ScheduleType.FLEXIBLE,
                    UnitSystem.METRIC,
                ),
                false,
            )
        SqliteUserPreferencesRepository(context).save(profile)
        val store = FeatureStore(context)
        store.putSetting("baseline.reviewed", "true")
        store.saveManualBaseline(
            ManualBaselineInput(25.0, 4.0, 12.0, Duration.ofMinutes(27), null, null, 390.0, 8)
        )
        store.saveShoe(
            Shoe(
                "demo-shoe",
                "Demo",
                "Daily trainer",
                today.minusDays(80),
                120000.0,
                false,
                "Test-only screenshot fixture",
                true,
            )
        )
        val evidence = DevelopmentStore(context)
        for (i in 0..5) {
            val at = today.minusWeeks((5 - i).toLong()).atTime(12, 0).atOffset(ZoneOffset.UTC)
            evidence.saveWeight(
                org.racedaydreams.domain.body.BodyWeight(
                    "demo-weight-$i",
                    75.0 + i * .15,
                    at.toInstant(),
                    at.offset,
                    "manual",
                    "demo-weight-$i",
                    null,
                )
            )
        }
        evidence.saveHistorical(
            HistoricalStrengthRecord(
                "demo-historical",
                StrengthExercise.starters().first(),
                75.0,
                5,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                "Before using the app; test-only fixture",
            )
        )
        val observations =
            mutableListOf<org.racedaydreams.domain.health.HealthIntelligence.Observation>()
        for (i in 0..34) for (metric in
            org.racedaydreams.domain.health.HealthIntelligence.Metric.values()) {
            val day = today.minusDays(i.toLong())
            val time = day.atStartOfDay(ZoneOffset.UTC).toInstant()
            val value =
                when (metric) {
                    org.racedaydreams.domain.health.HealthIntelligence.Metric.SLEEP_MINUTES -> 450.0
                    org.racedaydreams.domain.health.HealthIntelligence.Metric.RESTING_HR -> 55.0
                    org.racedaydreams.domain.health.HealthIntelligence.Metric.HRV_RMSSD -> 48.0
                    else -> 8000.0
                }
            observations.add(
                org.racedaydreams.domain.health.HealthIntelligence.Observation(
                    "demo-health-$metric-$i",
                    metric,
                    day,
                    time,
                    time,
                    value,
                    "Test-only demo source",
                    Instant.now(),
                )
            )
        }
        HealthIntelligenceStore(context)
            .replace(
                org.racedaydreams.domain.health.HealthIntelligence.Metric.values().toSet(),
                today.minusDays(34),
                today,
                observations,
            )
        val now = Instant.now()
        val baseline =
            RunnerBaseline(
                25.0,
                4.0,
                12.0,
                45.0,
                390.0,
                8,
                Duration.ofMinutes(27),
                null,
                null,
                24,
                8,
                now.minus(Duration.ofDays(84)),
                now,
                now.minus(Duration.ofDays(1)),
                BaselineConfidence.STRONG,
                emptyMap(),
                emptyList(),
                emptyList(),
                emptyList(),
            )
        val plan =
            OriginalTrainingPlanGenerator()
                .generate(
                    profile,
                    baseline,
                    today
                        .minusWeeks(2)
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                )
        val version =
            PlanVersion(
                "demo-v1",
                plan.id(),
                1,
                now,
                "Runner requested programme",
                profile,
                "Screenshot fixture: manually entered 25 km/week, 12 km longest run and eight consistent weeks. Not real health data.",
                plan,
            )
        val coach = CoachingStore(context)
        coach.saveCurrent(version)
        val gymSettings =
            GymProgramme.Settings(
                GymProgramme.Goal.SUPPORT_RUNNING,
                GymProgramme.Priority.RUNNING_FIRST,
                GymProgramme.Experience.INTERMEDIATE,
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                2,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            )
        GymProgrammeStore(context)
            .save(
                GymProgrammeGenerator()
                    .generate(
                        "demo-gym",
                        1,
                        now,
                        "Test-only deterministic programme",
                        gymSettings,
                        today,
                        plan.workouts(),
                        emptyList(),
                    )
            )
        plan
            .workouts()
            .filter { it.date() < today }
            .forEachIndexed { i, w ->
                val date = w.date().atTime(8, 0).atOffset(ZoneOffset.UTC)
                val km = w.distanceKm() ?: 5.0
                val duration = Duration.ofSeconds((km * 390).toLong())
                val samples =
                    (0..duration.seconds.toInt() step 10).map { second ->
                        DistanceSample(
                            Duration.ofSeconds(second.toLong()),
                            km * 1000 * second / duration.seconds,
                            null,
                            null,
                            null,
                            false,
                        )
                    }
                val run =
                    ActivityRecord(
                        "demo-run-$i",
                        SourceIdentity("manual", "demo-run-$i", null),
                        date.toInstant(),
                        date.toInstant().plus(duration),
                        ZoneOffset.UTC,
                        ZoneOffset.UTC,
                        km * 1000,
                        null,
                        null,
                        null,
                        null,
                        null,
                        3.0,
                        "Deterministic screenshot fixture; manually entered example",
                        samples,
                        emptyList(),
                        w.id(),
                        "demo-shoe",
                    )
                store.upsertManualActivity(run)
                coach.saveResponse(
                    RunExecution(run.id(), w.id(), version.id(), null, null, "Test-only fixture")
                )
            }
        val exercise = StrengthExercise.starters().first()
        evidence.saveHistorical(
            HistoricalStrengthRecord(
                "demo-old",
                exercise,
                70.0,
                5,
                today.minusDays(90),
                HistoricalStrengthRecord.DatePrecision.EXACT,
                "Test-only historical evidence",
            )
        )
        evidence.saveHistorical(
            HistoricalStrengthRecord(
                "demo-approx",
                exercise,
                72.5,
                5,
                today.minusDays(60),
                HistoricalStrengthRecord.DatePrecision.APPROXIMATE,
                "Approximate date; test-only",
            )
        )
        coach.saveStrength(
            StrengthSession(
                "demo-strength",
                plan
                    .workouts()
                    .filter { it.date() < today }
                    .maxBy { it.date() }
                    .date()
                    .atTime(18, 0)
                    .toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
                Duration.ofMinutes(50),
                7.0,
                "Test-only fixture",
                listOf(
                    StrengthSession.ExerciseSets(
                        exercise,
                        listOf(
                            StrengthSession.SetEntry("demo-set1", 5, 80.0, 7.0, false),
                            StrengthSession.SetEntry("demo-set2", 5, 80.0, 7.0, false),
                            StrengthSession.SetEntry("demo-set3", 5, 80.0, 7.0, false),
                        ),
                    )
                ),
            )
        )
        // A deterministic native trace uses the real store and analysis, never production demo
        // data.
        val trace = org.racedaydreams.domain.recording.RunMetricsEngine(0, now.minusSeconds(4000))
        for (i in 0..1800) {
            val angle = i * 2 * Math.PI / 1800
            trace.accept(
                org.racedaydreams.domain.recording.RunMetricsEngine.Fix(
                    now.minusSeconds(4000).plusSeconds(i.toLong()),
                    i * 1000L,
                    51.5 + .01 * kotlin.math.sin(angle),
                    -.1 + .017 * kotlin.math.cos(angle),
                    3.0,
                    30 + 3 * kotlin.math.sin(angle),
                    3.0,
                    3.0,
                )
            )
        }
        val recordings = RecordingStore(context)
        recordings.start("demo-native-route", now.minusSeconds(4000), 0)
        recordings.flush(
            "demo-native-route",
            trace.points(),
            trace.events(),
            0,
            0,
            1800000,
            "ACTIVE",
        )
        recordings.finish(recordings.unfinished()!!, 1800000, recovered = false)
        // Fix only the test assignment; production still randomly assigns and persists once.
        store.activitySummaries().forEachIndexed { i, a ->
            a.distanceMetres()
                ?.takeIf { it > 0 }
                ?.let { metres ->
                    val route =
                        org.racedaydreams.domain.support.DreamRoutes.currentGeometry(
                            org.racedaydreams.domain.support.DreamRoutes.routes()[
                                    if (a.id() == "rdd:demo-native-route") 0 else i % 5]
                        )
                    SupportStore(context)
                        .save(
                            "dream",
                            a.id(),
                            org.json
                                .JSONObject()
                                .put("version", 1)
                                .put("route", route.id())
                                .put(
                                    "start",
                                    kotlin.math.min(10000.0, kotlin.math.max(0.0, 42195 - metres)),
                                )
                                .put("distance", metres),
                        )
                }
        }
    }

    @After
    fun clear() {
        org.racedaydreams.recording.RunRecordingService.status.value = null
        org.racedaydreams.recording.RunRecordingService.workoutState.value = null
        org.racedaydreams.recording.RunRecordingService.savedActivity.value = null
        scenario?.close()
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
    }

    @Test
    fun capturesCurrentBuildScreensAndVerifiesCoachingNavigation() {
        scenario = ActivityScenario.launch(CoachActivity::class.java)
        await("Today")
        capture("home")
        compose.onNodeWithText("Weekly coaching review").performScrollTo().performClick()
        await("Recommendation")
        capture("weekly-review")
        back()
        click("Plan")
        await("This week")
        capture("training-plan")
        compose
            .onAllNodesWithContentDescription("View workout and purpose")[0]
            .performScrollTo()
            .performClick()
        await("Workout structure")
        capture("planned-workout")
        compose.onNode(hasText("Warm-up ·", substring = true)).performScrollTo().performClick()
        compose.waitUntil(20000) {
            compose
                .onAllNodes(hasText("1 of ", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        capture("warm-up")
        click("Next")
        await("Ankle rocks")
        capture("exercise-ankle-rocks")
        back()

        back()
        click("Calendar")
        await("Activity calendar")
        compose.waitUntil(20000) {
            compose
                .onAllNodes(hasStateDescription("Recorded history ready"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        capture("history")
        click("Week")
        await("Training calendar")
        capture("calendar-week")
        click("Month")
        await(
            LocalDate.now()
                .format(
                    java.time.format.DateTimeFormatter.ofPattern("MMMM uuuu", java.util.Locale.UK)
                )
        )
        capture("calendar-month")
        click("List")
        await("Activity calendar")
        // Open the first real retained activity through the Calendar screen.
        compose.waitUntil(20000) {
            compose
                .onAllNodes(hasStateDescription("Recorded history ready"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Recorded run"))
        compose.onAllNodesWithText("Recorded run")[0].performClick()
        await("Run detail")
        capture("activity-detail")
        alignHeading("Your recorded route")
        capture("post-run-route")
        val dreamHeading =
            compose
                .onNode(hasText("Dream Route ·", substring = true))
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                .first()
                .text
        alignHeading(dreamHeading)
        capture("dream-route")
        compose.onNodeWithText("Compare this run").performScrollTo().performClick()
        alignHeading("Compare this run")
        capture("run-comparison")
        alignHeading("Run Analysis")
        capture("run-feedback")
        back()
        click("Progress")
        await("Running PBs")
        capture("progress")
        click("Body weight")
        await("Record a measurement")
        capture("body-weight")
        back()
        click("Running PBs")
        await("How PBs are calculated")
        capture("running-pbs")
        back()
        click("Training trends")
        await("Weekly recorded distance")
        capture("training-trends")
        back()
        click("Strength")
        await("Log workout")
        capture("strength")
        click("PBs & progression")
        await("Exercise progress and PB history")
        compose.onAllNodesWithText("View progression")[0].performScrollTo().performClick()
        alignHeading("Exercise progress and PB history")
        capture("strength-pbs")
        back()
        compose.onNodeWithText("Exercises").performScrollTo().performClick()
        await("Search exercises or aliases")
        capture("exercise-browse")
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("de")
        compose.waitForIdle()
        capture("exercise-search")
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("RDL")
        compose.onNodeWithText("Romanian Deadlift").performClick()
        await("Overview")
        capture("exercise-rdl")
        compose.onNodeWithText("Exclude from recommendations").performScrollTo().performClick()
        click("Exclude exercise")
        await("Restore exercise")
        back()
        await("Search exercises or aliases")
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("exercise-shortcut-Excluded").performScrollTo().performClick()
        await("Excluded exercises")
        alignHeading("Excluded exercises")
        capture("exercise-excluded")
        compose.onNodeWithText("Restore exercise").performScrollTo().performClick()
        compose.waitUntil(20000) {
            compose.onAllNodesWithText("Restore exercise").fetchSemanticsNodes().isEmpty()
        }
        // Restore removes the only Excluded row; leave that explicit filter before searching.
        compose.onNodeWithTag("exercise-shortcut-For you").performScrollTo().performClick()
        compose
            .onNodeWithText("Search exercises or aliases")
            .performScrollTo()
            .performTextReplacement("RDL")
        compose.onNodeWithTag("exercise-row-romanian-deadlift").performScrollTo().performClick()
        await("Overview")
        click("How to do it")
        compose.waitForIdle()
        capture("exercise-rdl-howto")
        back()
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Bench")
        compose.onNodeWithText("Bench Press").performClick()
        await("Overview")
        click("How to do it")
        capture("exercise-bench-howto")
        back()
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("BSS")
        compose.onNodeWithText("Bulgarian Split Squat").performClick()
        await("Overview")
        capture("exercise-bulgarian-split-squat")
        click("How to do it")
        capture("exercise-bulgarian-howto")
        back()
        compose
            .onNodeWithText("Search exercises or aliases")
            .performTextReplacement("Seated Cable Row")
        compose
            .onNode(hasText("Seated Cable Row") and hasClickAction() and !hasSetTextAction())
            .performClick()
        await("Overview")
        click("How to do it")
        capture("exercise-cable-row-howto")
        back()
        for ((exercise, filename) in
            listOf(
                "Front Squat" to "exercise-front-squat-howto",
                "Leg Press" to "exercise-leg-press-howto",
                "Lat Pulldown" to "exercise-lat-pulldown-howto",
                "Seated Leg Curl" to "exercise-seated-leg-curl-howto",
                "Farmer’s Carry" to "exercise-farmers-carry-howto",
                "Hack Squat" to "exercise-hack-squat-howto",
                "Weighted Wall Sit" to "exercise-weighted-wall-sit-howto",
                "Arnold Press" to "exercise-arnold-press-howto",
                "Cable Crunch" to "exercise-cable-crunch-howto",
                "Clean & Jerk" to "exercise-clean-jerk-howto",
            )) {
            compose.onNodeWithText("Search exercises or aliases").performTextReplacement(exercise)
            compose
                .onNode(hasText(exercise) and hasClickAction() and !hasSetTextAction())
                .performClick()
            await("Overview")
            click("How to do it")
            capture(filename)
            back()
        }
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Plank")
        compose.onNode(hasText("Plank") and hasClickAction() and !hasSetTextAction()).performClick()
        await("Overview")
        click("How to do it")
        capture("exercise-plank-howto")
        back()
        back()
        click("Log workout")
        await("Log strength workout")
        click("Log strength workout")
        alignHeading("Last time")
        capture("strength-log-previous")
        compose.onNodeWithText("Back Squat").performScrollTo().performClick()
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Plank")
        compose.onNode(hasText("Plank") and hasClickAction() and !hasSetTextAction()).performClick()
        compose
            .onNodeWithText("Duration (seconds or m:ss)")
            .performScrollTo()
            .performTextReplacement("1:30")
        compose.waitForIdle()
        capture("strength-log-timed")
        compose
            .onNode(hasText("Plank") and hasClickAction() and !hasSetTextAction())
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Search exercises or aliases").performTextReplacement("Farmer")
        compose.onNodeWithText("Farmer’s Carry").performClick()
        compose.onNodeWithText("Distance (metres)").performScrollTo().performTextReplacement("60")
        compose.waitForIdle()
        capture("strength-log-distance")
        back()
        compose.onNodeWithText("Gym programme").performScrollTo().performClick()
        await("Change programme settings")
        capture("gym-running-support")
        val next =
            GymProgrammeStore(context).current().sessions().first { it.date() >= LocalDate.now() }
        compose
            .onNodeWithText("${friendlyDate(next.date())} · ${next.title()}")
            .performScrollTo()
            .performClick()
        await("Why this session?")
        capture("gym-workout-detail")
        back()
        compose.onNodeWithText("Change programme settings").performScrollTo().performClick()
        await("Your gym preferences")
        alignHeading("Your gym preferences")
        capture("gym-programme-setup")
        compose.onNodeWithText("Get stronger").performScrollTo().performClick()
        compose.onNodeWithText("Strength first").performScrollTo().performClick()
        compose.onNodeWithText("Preview six-week block").performScrollTo().performClick()
        await("Preview — not applied")
        compose.onNodeWithText("Accept this programme").performScrollTo().performClick()
        compose.waitUntil(20000) {
            GymProgrammeStore(context).current().settings().priority() ==
                GymProgramme.Priority.STRENGTH_FIRST
        }
        compose.waitForIdle()
        capture("gym-strength-first")
        back()
        click("⚙ Settings")
        await("Settings")
        capture("settings")
        alignHeading("Activity data source")
        capture("data-sources")
        compose.onNodeWithText("Personal health context").performScrollTo().performClick()
        await("Personal health context")
        compose.waitForIdle()
        capture("health-context")
        back()
        back()
        click("Home")
        compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick()
        await("Custom workout")
        capture("run-now")
        compose.onNodeWithText("Free run").performScrollTo().performClick()
        await("Start GPS recording")
        capture("record-run")
        val live =
            org.racedaydreams.domain.execution.WorkoutExecutionEngine(
                listOf(
                    org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step(
                        "Controlled interval",
                        WorkoutSegment.Kind.RUN,
                        800.0,
                        null,
                        295.0,
                        305.0,
                        7.0,
                    )
                )
            )
        val liveState =
            live.update(
                org.racedaydreams.domain.execution.WorkoutExecutionEngine.Input(
                    200.0,
                    60000,
                    60000,
                    300.0,
                    true,
                    false,
                )
            )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            org.racedaydreams.recording.RunRecordingService.workoutState.value = liveState
            org.racedaydreams.recording.RunRecordingService.status.value =
                org.racedaydreams.domain.recording.RunMetricsEngine.Status(
                    200.0,
                    60000,
                    60000,
                    300.0,
                    "GPS ready",
                    false,
                )
        }
        await("600 m remaining")
        capture("live-coach")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            org.racedaydreams.recording.RunRecordingService.status.value = null
            org.racedaydreams.recording.RunRecordingService.workoutState.value = null
        }
        back()
        back()
        click("Home")
        compose.onNodeWithText("Race preparation").performScrollTo().performClick()
        await("Your pacing strategy")
        capture("race-day")
        back()
        click("Plan")
        compose.onNodeWithText("View routines").performScrollTo().performClick()
        await("Movement support")
        compose.onNodeWithText("Start Hips & glutes").performScrollTo().performClick()
        await("Hips & glutes")
        capture("mobility")
        click("Next")
        await("Walking lunges")
        capture("exercise-walking-lunge")
        click("Next")
        click("Next")
        await("Hip-flexor stretch")
        capture("exercise-static-stretch")
        back()
        back()
        click("⚙ Settings")
        await("Settings")
        compose.onNodeWithText("Edit race goal and availability").performScrollTo().performClick()
        compose.waitForIdle()
        capture("onboarding")
        back()
        click("⚙ Settings")
        await("Settings")
        compose.onNodeWithText("Shoes and mileage").performScrollTo().performClick()
        await("Your shoes")
        capture("shoes")
        scenario?.close()
        scenario = ActivityScenario.launch(CoachActivity::class.java)
        await("How am I doing?")
        compose.onNodeWithText("View activity history").performScrollTo().performClick()
        await("Search and date filters")
        click("Search and date filters")
        capture("history-filters")
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            scenario?.close()
            scenario = null
            shell("cmd uimode night yes")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("How am I doing?")
            Assert.assertEquals(
                android.content.res.Configuration.UI_MODE_NIGHT_YES,
                context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK,
            )
            capture("home-dark")
            click("Plan")
            await("This week")
            capture("plan-dark")
            compose.onNodeWithText("View routines").performScrollTo().performClick()
            await("Movement support")
            compose.onNodeWithText("Start Hips & glutes").performScrollTo().performClick()
            await("Hips & glutes")
            capture("mobility-dark")
            back()
            back()
            click("Home")
            await("How am I doing?")
            // Font changes already recreate Android activities. Close first so the
            // capture does not race recreate() against an automatically destroyed instance.
            scenario?.close()
            scenario = null
            shell("settings put system font_scale 2.0")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("How am I doing?")
            Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
            capture("home-large-font")
            compose.onNodeWithContentDescription("Plan").performClick()
            await("This week")
            capture("plan-large-font")
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesStrengthFirstHomeWithoutAnyRaceOrRunningProfile() {
        // Replace this class's running fixture with a real, race-free Strength installation.
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val today = LocalDate.now()
        val settings =
            GymProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(today.dayOfWeek, today.plusDays(3).dayOfWeek),
                2,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            )
        val programme =
            GymProgrammeGenerator()
                .generate(
                    "strength-home-block",
                    1,
                    Instant.now(),
                    "Athlete selected a general strength block",
                    settings,
                    today,
                    emptyList(),
                    emptyList(),
                )
        GymProgrammeStore(context).save(programme)
        val exercise = ExerciseCatalog.definition("bench-press").exercise()
        val now = Instant.now()
        for (daysAgo in listOf(3L, 6L)) {
            val id = "strength-home-$daysAgo"
            val sets = (1..3).map { StrengthSession.SetEntry("$id-$it", 10, 70.0, 7.0, false) }
            CoachingStore(context)
                .saveStrength(
                    StrengthSession(
                        id,
                        now.minus(Duration.ofDays(daysAgo)),
                        ZoneOffset.UTC,
                        Duration.ofMinutes(45),
                        7.0,
                        "Recorded working sets",
                        listOf(StrengthSession.ExerciseSets(exercise, sets)),
                    )
                )
        }
        Assert.assertNull(
            "Strength fixture must not depend on a running profile",
            SqliteUserPreferencesRepository(context).load().orElse(null),
        )
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            shell("settings put system font_scale 1.0")
            shell("cmd uimode night no")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("Your strength training")
            capture("strength-home")
            compose.onAllNodesWithText("How am I doing?").assertCountEquals(0)
            scenario?.close()
            scenario = null
            shell("cmd uimode night yes")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("Your strength training")
            Assert.assertEquals(
                android.content.res.Configuration.UI_MODE_NIGHT_YES,
                context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK,
            )
            capture("strength-home-dark")
            scenario?.close()
            scenario = null
            shell("settings put system font_scale 2.0")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("Your strength training")
            Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
            capture("strength-home-large-font")
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesOptionalGymPreparationAndSupersetFromProductionScreens() {
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            for ((suffix, night, scale) in
                listOf(
                    Triple("", "no", "1.0"),
                    Triple("-dark", "yes", "1.0"),
                    Triple("-large-font", "no", "2.0"),
                )) {
                scenario?.close()
                scenario = null
                SqliteUserPreferencesRepository(context).clear()
                SqlitePrimaryFocusRepository(context)
                    .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                openGymNow()
                compose.onNodeWithText("Build a workout").performScrollTo().performClick()
                await("Workout name")
                scenario?.onActivity { activity ->
                    val model = ViewModelProvider(activity)[GymNowViewModel::class.java]
                    model.builderTitle("Upper-body practice")
                    Assert.assertTrue(
                        model.builderPutSlot(
                            null,
                            ExerciseCatalog.definition("push-up").exercise(),
                            GymSlotInput(
                                sets = "2",
                                minimumReps = "6",
                                maximumReps = "10",
                                restSeconds = "90",
                            ),
                        )
                    )
                    Assert.assertTrue(
                        model.builderPutSlot(
                            null,
                            ExerciseCatalog.definition("plank").exercise(),
                            GymSlotInput(sets = "2", seconds = "30", restSeconds = "60"),
                        )
                    )
                    model.builderPair(model.state.value.builder.slots.first().id())
                }
                awaitGymReady {
                    it.builder.slots.size == 2 &&
                        it.builder.slots.all { slot -> slot.supersetGroup() != null }
                }
                compose.onNodeWithText("Start this workout").performScrollTo().performClick()
                awaitGymReady { it.view?.observed()?.status() == GymExecution.Status.ACTIVE }
                compose.onNodeWithText("Start rest timer").performScrollTo().performClick()
                awaitGymReady {
                    it.restTimer?.phase() ==
                        org.racedaydreams.domain.strength.GymRestTimer.Phase.RUNNING
                }
                compose.onNodeWithText("Pause rest").performScrollTo().performClick()
                awaitGymReady {
                    it.restTimer?.phase() ==
                        org.racedaydreams.domain.strength.GymRestTimer.Phase.PAUSED
                }
                alignHeading("Optional rest timer")
                capture("gym-rest-timer$suffix")
                compose.onNodeWithText("Optional warm-up ▾").performScrollTo().performClick()
                alignHeading("Optional warm-up ▴")
                capture("gym-warmup$suffix")
                compose.onNodeWithText("Skip preparation guidance").performScrollTo().performClick()
                alignHeading("Superset")
                capture("gym-superset$suffix")
                Assert.assertTrue(
                    gymNowModel().state.value.view!!.observed()!!.actualSets().isEmpty()
                )
                Assert.assertTrue(CoachingStore(context).strength().isEmpty())
                if (scale == "2.0")
                    Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
                if (night == "yes")
                    Assert.assertEquals(
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                        context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK,
                    )
            }
        } finally {
            scenario?.close()
            scenario = null
            shell("settings put system font_scale $originalFont")
            shell("cmd uimode night $originalNight")
            SqliteUserPreferencesRepository(context).clear()
        }
    }

    @Test
    fun capturesGymNowBuilderActualSetsAndExplicitRecoveryInAccessibleModes() {
        // Use the real installation/database and production navigation, with no running profile.
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            shell("settings put system font_scale 1.0")
            shell("cmd uimode night no")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            openGymNow()
            capture("gym-now")
            compose.onNodeWithText("Build a workout").performScrollTo().performClick()
            await("Workout name")
            scenario?.onActivity { activity ->
                val model = ViewModelProvider(activity)[GymNowViewModel::class.java]
                model.builderTitle("Upper-body strength")
                Assert.assertTrue(
                    model.builderPutSlot(
                        null,
                        ExerciseCatalog.definition("bench-press").exercise(),
                        GymSlotInput(
                            sets = "2",
                            minimumReps = "6",
                            maximumReps = "8",
                            minimumEffort = "6",
                            maximumEffort = "8",
                            restSeconds = "90",
                            referenceKilograms = "70",
                        ),
                    )
                )
                Assert.assertTrue(
                    model.builderPutSlot(
                        null,
                        ExerciseCatalog.definition("seated-cable-row").exercise(),
                        GymSlotInput(
                            sets = "2",
                            minimumReps = "10",
                            maximumReps = "12",
                            minimumEffort = "6",
                            maximumEffort = "8",
                            restSeconds = "60",
                        ),
                    )
                )
            }
            awaitGymReady { it.builder.slots.size == 2 }
            compose.onNodeWithText("Workout name").performScrollTo()
            capture("gym-workout-builder")
            compose.onNodeWithText("Save as a favourite").performScrollTo().performClick()
            awaitGymReady { it.builder.savedRevision == it.builder.revision }
            Assert.assertEquals(1, SqliteGymExecutionRepository(context).favouriteWorkouts(10).size)
            Assert.assertTrue(CoachingStore(context).strength().isEmpty())
            compose.onNodeWithText("Start this workout").performScrollTo().performClick()
            awaitGymReady {
                it.view?.accepted()?.execution()?.origin() == GymExecution.Origin.CUSTOM
            }
            await("Current set")
            val executionId = gymNowModel().state.value.view!!.accepted()!!.execution().id()
            Assert.assertEquals("", gymNowModel().state.value.form.kilograms)
            Assert.assertEquals("", gymNowModel().state.value.form.repetitions)
            enterGymActualSet("60", "8", 1)
            enterGymActualSet("60", "7", 2)
            Assert.assertTrue(CoachingStore(context).strength().isEmpty())
            alignHeading("Bench Press")
            capture("gym-workout")
            compose.onNodeWithText("Equipment busy?").performScrollTo().performClick()
            awaitGymReady { it.substitution != null }
            await("Why swap?")
            capture("gym-equipment-alternatives")
            scenario?.onActivity { activity ->
                ViewModelProvider(activity)[GymNowViewModel::class.java].closeSubstitutions()
            }
            awaitGymReady { it.substitution == null }
            pauseGymForRelaunch()

            shell("cmd uimode night yes")
            reopenAndRecoverGym(executionId, 2)
            Assert.assertEquals(
                android.content.res.Configuration.UI_MODE_NIGHT_YES,
                context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK,
            )
            alignHeading("Current set")
            capture("gym-workout-dark")
            pauseGymForRelaunch()

            shell("settings put system font_scale 2.0")
            reopenAndRecoverGym(executionId, 2)
            Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
            alignHeading("Current set")
            capture("gym-workout-large-font")
            pauseGymForRelaunch()

            shell("settings put system font_scale 1.0")
            shell("cmd uimode night no")
            reopenAndRecoverGym(executionId, 2)
            compose.onNodeWithText("Seated Cable Row").performScrollTo().performClick()
            awaitGymReady {
                val execution = it.view?.accepted()?.execution()
                execution
                    ?.slots()
                    ?.firstOrNull { slot -> slot.id() == execution.selectedSlotId() }
                    ?.exercise()
                    ?.id() == "seated-cable-row"
            }
            enterGymActualSet("45", "10", 3)
            enterGymActualSet("45", "10", 4)
            compose.onNodeWithText("Finish and save workout").performScrollTo().performClick()
            awaitGymReady {
                it.view?.accepted()?.execution()?.status() == GymExecution.Status.FINISHED
            }
            await("Workout saved")
            alignHeading("Workout saved")
            capture("gym-workout-saved")
            val retained = SqliteGymExecutionRepository(context).load(executionId).execution()
            Assert.assertFalse(retained.partial())
            Assert.assertEquals(4, retained.actualSets().size)
            Assert.assertNull(retained.plannedAssociation())
            Assert.assertNull(GymProgrammeStore(context).current())
            val actual = CoachingStore(context).strength().single()
            Assert.assertEquals(executionId, actual.id())
            Assert.assertEquals(60.0, actual.exercises().first().sets().first().kilograms(), 0.0)
            Assert.assertEquals(8, actual.exercises().first().sets().first().reps())
            Assert.assertEquals(2, actual.exercises().size)
            val programmeDay = LocalDate.now()
            val associationPlan =
                org.racedaydreams.domain.strength.GymProgramme(
                    "gym-association-capture",
                    1,
                    Instant.now(),
                    "Explicitly reviewed programme",
                    org.racedaydreams.domain.strength.GymProgramme.Settings(
                        org.racedaydreams.domain.strength.GymProgramme.Goal.STRENGTH,
                        org.racedaydreams.domain.strength.GymProgramme.Priority.STRENGTH_FIRST,
                        org.racedaydreams.domain.strength.GymProgramme.Experience.INTERMEDIATE,
                        setOf(programmeDay.dayOfWeek),
                        1,
                        setOf("full gym"),
                        45,
                        org.racedaydreams.domain.strength.GymProgramme.Effort.RPE,
                    ),
                    programmeDay,
                    programmeDay.plusDays(41),
                    listOf(
                        org.racedaydreams.domain.strength.GymProgramme.Session(
                            "review-upper",
                            programmeDay,
                            "Upper body strength",
                            listOf(
                                org.racedaydreams.domain.strength.GymProgramme.Prescription(
                                    "bench-press",
                                    2,
                                    6,
                                    8,
                                    null,
                                    null,
                                    6,
                                    8,
                                    120,
                                    "Controlled working sets",
                                ),
                                org.racedaydreams.domain.strength.GymProgramme.Prescription(
                                    "seated-cable-row",
                                    2,
                                    8,
                                    12,
                                    null,
                                    null,
                                    6,
                                    8,
                                    90,
                                    "Controlled pulling work",
                                ),
                            ),
                            "Actual evidence remains separate from this prescription.",
                        )
                    ),
                    emptyList(),
                )
            GymProgrammeStore(context).save(associationPlan)
            scenario?.onActivity { activity ->
                ViewModelProvider(activity)[GymNowViewModel::class.java].refreshAssociation()
            }
            awaitGymReady { it.association?.proposals()?.size == 1 }
            Assert.assertTrue(org.racedaydreams.data.DevelopmentStore(context).gymLinks().isEmpty())
            alignHeading("Keep extra or associate?")
            capture("gym-association-review")
            compose
                .onNodeWithTag("gym-association-count-review-upper")
                .performScrollTo()
                .performClick()
            compose.onNodeWithTag("gym-association-confirm").performClick()
            awaitGymReady {
                it.association?.current()?.choice() ==
                    org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
                        .COUNT_TOWARD_SESSION
            }
            alignHeading("Your programme association")
            capture("gym-association-saved")
            Assert.assertEquals(
                retained,
                SqliteGymExecutionRepository(context).load(executionId).execution(),
            )
            Assert.assertEquals(actual, CoachingStore(context).strength().single())
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesSourcedCityRoutesWithDarkAndLargeText() {
        fun openCity(city: org.racedaydreams.domain.support.DreamRoutes.Route) {
            // Drain the prior route's layout before destroying its Android window.
            // Closing during a pending frame is not the configuration scenario being captured.
            compose.waitForIdle()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario?.close()
            scenario = null
            // ActivityScenario.close waits for DESTROYED, not Compose test-root unregister.
            // Do not let the next launch/query adopt a departing window's empty semantics root.
            compose.waitUntil(20000) {
                compose
                    .onAllNodes(isRoot(), useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty()
            }
            val route = org.racedaydreams.domain.support.DreamRoutes.currentGeometry(city)
            val recorded =
                FeatureStore(context).activitySummaries().single {
                    it.id() == "rdd:demo-native-route"
                }
            val metres = recorded.distanceMetres()!!
            val nativeSource =
                "Source: ${org.racedaydreams.domain.activity.ActivitySources.friendly(recorded.source())}"
            val retainedPoints = RecordingStore(context).points(recorded.source().recordId())
            val retainedStory =
                org.racedaydreams.domain.polish.RoutePresentation.story(retainedPoints)
            Assert.assertTrue(
                "Native capture fixture must retain genuine kilometre evidence",
                retainedPoints.size > 2 &&
                    retainedStory.any { it.startsWith("Fastest complete kilometre:") },
            )
            Assert.assertTrue(
                "Native capture fixture must retain durability evidence",
                org.racedaydreams.domain.presentation.DurabilityData.of(
                        FeatureStore(context).activityDetail(recorded.id())
                    )
                    .evidence()
                    .sufficient(),
            )
            SupportStore(context)
                .save(
                    "dream",
                    "rdd:demo-native-route",
                    org.json
                        .JSONObject()
                        .put("version", 1)
                        .put("route", route.id())
                        .put("start", minOf(1000.0, maxOf(0.0, 42195.0 - metres)))
                        .put("distance", metres)
                        .put("assignedAt", Instant.now().toString()),
                )
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("Today")
            compose
                .onNode(
                    (hasText("Calendar") or hasContentDescription("Calendar")) and hasClickAction()
                )
                .performClick()
            await("Activity calendar")
            compose.waitUntil(20000) {
                compose
                    .onAllNodes(hasStateDescription("Recorded history ready"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            // Several fixture cards share "Recorded run". Select the native source
            // inside its clickable card, rather than whichever generic title happens
            // to be composed first while the lazy history is refreshing/scrolling.
            val nativeCard =
                hasClickAction() and
                    (hasText(nativeSource) or hasAnyDescendant(hasText(nativeSource)))
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(nativeCard)
            compose.onNode(nativeCard).performClick()
            await("Run detail")
            compose.onNodeWithText(nativeSource).assertExists()
            // This fixture has genuine retained samples. Wait for both asynchronous
            // analyses ABOVE Dream Routes before positioning the target card.
            try {
                await("Route story")
                compose.onNodeWithText("Route story").performScrollTo().assertIsDisplayed()
                compose.waitUntil(20000) {
                    compose
                        .onAllNodes(hasText("Fastest complete kilometre:", substring = true))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                compose.onNodeWithText("Long-run durability").performScrollTo().assertIsDisplayed()
                compose.waitUntil(20000) {
                    compose
                        .onAllNodes(hasText("Late-run pace change:", substring = true))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            } catch (failure: Throwable) {
                if (
                    failure !is AssertionError &&
                        failure !is androidx.compose.ui.test.ComposeTimeoutException
                )
                    throw failure
                throw AssertionError(
                    "City ${route.id()}, activity ${recorded.id()}, source ${recorded.source()}, retained points ${retainedPoints.size}, expected story $retainedStory, scenario ${scenario?.state}. UI: ${compose.onRoot(useUnmergedTree=true).printToString().take(14000)}",
                    failure,
                )
            }
            alignHeading("Dream Route · ${route.name()}")
        }
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            shell("cmd uimode night no")
            shell("settings put system font_scale 1.0")
            for (city in org.racedaydreams.domain.support.DreamRoutes.routes()) {
                val captureName = "dream-route-${city.id().removeSuffix("-v2")}"
                openCity(city)
                capture(captureName)
                alignHeading("Key landmarks on this route")
                capture("$captureName-landmarks", "Key landmarks on this route")
            }
            val london = org.racedaydreams.domain.support.DreamRoutes.routes().first()
            scenario?.close()
            scenario = null
            shell("cmd uimode night yes")
            openCity(london)
            capture("dream-route-dark")
            alignHeading("Key landmarks on this route")
            capture("dream-route-dark-landmarks", "Key landmarks on this route")
            scenario?.close()
            scenario = null
            shell("settings put system font_scale 2.0")
            openCity(london)
            capture("dream-route-large-font")
            alignHeading("Key landmarks on this route")
            capture("dream-route-large-font-landmarks", "Key landmarks on this route")
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesTypedProgrammeBuilderPreviewAndCustomEntryThroughProductionNavigation() {
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val today = LocalDate.now()
        val second = today.plusDays(3).dayOfWeek
        fun slot(id: String, exercise: String, low: Int, high: Int) =
            GymWorkoutDefinition.Slot(
                id,
                ExerciseCatalog.definition(exercise).exercise(),
                GymWorkoutDefinition.Target(
                    3,
                    low,
                    high,
                    null,
                    null,
                    GymWorkoutDefinition.EffortRange(
                        GymWorkoutDefinition.EffortScale.RIR,
                        2.0,
                        3.0,
                    ),
                    120,
                    null,
                ),
                null,
            )
        val draft =
            StrengthProgrammeDraft(
                "capture-strength-draft",
                1,
                "capture-strength-programme",
                0,
                "Strength foundation",
                StrengthProgramme.Settings(
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Priority.STRENGTH_FIRST,
                    GymProgramme.Experience.BEGINNER,
                    setOf(today.dayOfWeek, second),
                    2,
                    45,
                    setOf(
                        ExerciseMetadata.Equipment.BARBELL,
                        ExerciseMetadata.Equipment.BENCH,
                        ExerciseMetadata.Equipment.SQUAT_RACK,
                        ExerciseMetadata.Equipment.DUMBBELLS,
                    ),
                ),
                today,
                listOf(
                    StrengthProgrammeDraft.WeekBlock(
                        "foundation",
                        StrengthProgramme.Phase.FOUNDATION,
                        "Foundation",
                        4,
                        listOf(
                            StrengthProgrammeDraft.Day(
                                "upper",
                                today.dayOfWeek,
                                "Upper-body practice",
                                listOf(slot("bench", "bench-press", 5, 8)),
                                null,
                            ),
                            StrengthProgrammeDraft.Day(
                                "lower",
                                second,
                                "Lower-body practice",
                                listOf(slot("lunge", "reverse-lunge", 8, 10)),
                                null,
                            ),
                        ),
                    )
                ),
                StrengthProgramme.Origin.CUSTOM,
                null,
            )
        SqliteStrengthProgrammingRepository(context).saveDraft(draft, 0)
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            listOf("" to false, "-dark" to false, "-large-font" to true).forEach { (suffix, large)
                ->
                shell("settings put system font_scale ${if (large) "2.0" else "1.0"}")
                shell("cmd uimode night ${if (suffix == "-dark") "yes" else "no"}")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                await("Your strength training")
                if (large) Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
                if (suffix == "-dark")
                    Assert.assertEquals(
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                        context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK,
                    )
                compose.onNodeWithText("Create a gym programme").performScrollTo().performClick()
                await("Build or review programmes")
                compose
                    .onNodeWithText("Build or review programmes")
                    .performScrollTo()
                    .performClick()
                await("Saved drafts")
                compose.onNodeWithText("Open draft").performScrollTo().performClick()
                await("Programme builder")
                compose.onNodeWithText("Programme builder").assertIsDisplayed()
                capture("strength-programme-builder$suffix")
                compose.onNodeWithText("Preview programme").performScrollTo().performClick()
                await("Review programme")
                compose.onNodeWithText("Review programme").assertIsDisplayed()
                capture("strength-programme-preview$suffix")
                if (suffix.isEmpty()) {
                    compose.onNodeWithText("Keep editing draft").performScrollTo().performClick()
                    await("Programme builder")
                    compose
                        .onNodeWithText("Create custom exercise")
                        .performScrollTo()
                        .performClick()
                    await("Custom exercise")
                    compose
                        .onNodeWithText("Exercise name")
                        .performScrollTo()
                        .performTextReplacement("Cable row variation")
                    androidx.test.espresso.Espresso.closeSoftKeyboard()
                    alignHeading("Custom exercise")
                    capture("strength-custom-exercise")
                }
                Assert.assertNull(SqliteStrengthProgrammingRepository(context).currentReference())
                Assert.assertTrue(CoachingStore(context).strength().isEmpty())
                scenario?.close()
                scenario = null
            }
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesConcreteTemplatesAndEveryPhaseThroughProductionNavigation() {
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val programme =
            StrengthProgrammeGenerator()
                .generate(
                    StrengthProgrammeGenerator.Request(
                        "capture-dated-development",
                        Instant.now(),
                        "Strength development",
                        StrengthProgramme.Settings(
                            GymProgramme.Goal.STRENGTH,
                            GymProgramme.Priority.BALANCED,
                            GymProgramme.Experience.INTERMEDIATE,
                            DayOfWeek.values().toSet(),
                            4,
                            60,
                            setOf(
                                ExerciseMetadata.Equipment.BARBELL,
                                ExerciseMetadata.Equipment.BENCH,
                                ExerciseMetadata.Equipment.SQUAT_RACK,
                                ExerciseMetadata.Equipment.DUMBBELLS,
                                ExerciseMetadata.Equipment.PULL_UP_BAR,
                            ),
                        ),
                        LocalDate.now(),
                        12,
                        "upper-lower-4",
                        org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        false,
                    )
                )
        val draft =
            ConcreteStrengthProgrammeDraft(
                "capture-concrete-preview",
                1,
                null,
                programme,
                emptyList(),
            )
        SqliteConcreteStrengthProgrammeDraftRepository(context).save(draft, 0)
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            listOf("", "-dark", "-large-font").forEach { suffix ->
                shell(
                    "settings put system font_scale ${if (suffix == "-large-font") "2.0" else "1.0"}"
                )
                shell("cmd uimode night ${if (suffix == "-dark") "yes" else "no"}")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                await("Your strength training")
                compose.onNodeWithText("Create a gym programme").performScrollTo().performClick()
                await("Build or review programmes")
                compose
                    .onNodeWithText("Build or review programmes")
                    .performScrollTo()
                    .performClick()
                await("Saved drafts")
                compose
                    .onNodeWithText("Templates & generated programmes")
                    .performScrollTo()
                    .performClick()
                await("Templates & generation")
                if (suffix == "-large-font")
                    Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
                if (suffix == "-dark")
                    Assert.assertEquals(
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                        context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK,
                    )
                compose.onNodeWithText("Templates & generation").assertIsDisplayed()
                capture("strength-templates$suffix")
                compose.onNodeWithText("Open Strength development").performScrollTo().performClick()
                await("Review every block")
                compose.onNodeWithText("Review every block").assertIsDisplayed()
                capture("strength-generated-preview$suffix")
                val workout = programme.workouts().minBy { it.date() }
                compose
                    .onNodeWithText(
                        "${friendlyDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min"
                    )
                    .performScrollTo()
                    .performClick()
                await("Dated workout")
                compose.onNodeWithText("Dated workout").assertIsDisplayed()
                capture("strength-generated-workout$suffix")
                compose.onNodeWithText("Programme preview").performScrollTo().performClick()
                await("Review every block")
                compose.onNodeWithText("Generate another preview").performScrollTo().performClick()
                await("Shape your programme")
                compose.onNodeWithText("Shape your programme").assertIsDisplayed()
                capture("strength-template-settings$suffix")
                Assert.assertNull(SqliteStrengthProgrammingRepository(context).currentReference())
                Assert.assertTrue(CoachingStore(context).strength().isEmpty())
                scenario?.close()
                scenario = null
            }
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesReviewedFutureRevisionAndRegenerationThroughProductionNavigation() {
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val programme =
            StrengthProgrammeGenerator()
                .generate(
                    StrengthProgrammeGenerator.Request(
                        "capture-future-source",
                        Instant.now(),
                        "Strength development",
                        StrengthProgramme.Settings(
                            GymProgramme.Goal.STRENGTH,
                            GymProgramme.Priority.BALANCED,
                            GymProgramme.Experience.INTERMEDIATE,
                            DayOfWeek.values().toSet(),
                            4,
                            60,
                            setOf(
                                ExerciseMetadata.Equipment.BARBELL,
                                ExerciseMetadata.Equipment.BENCH,
                                ExerciseMetadata.Equipment.SQUAT_RACK,
                                ExerciseMetadata.Equipment.DUMBBELLS,
                                ExerciseMetadata.Equipment.PULL_UP_BAR,
                            ),
                        ),
                        LocalDate.now(),
                        12,
                        "upper-lower-4",
                        org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        false,
                    )
                )
        val repository = SqliteStrengthProgrammingRepository(context)
        repository.activate(programme, null, "capture-initial-choice", emptyList())
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            listOf("", "-dark", "-large-font").forEach { suffix ->
                shell(
                    "settings put system font_scale ${if (suffix == "-large-font") "2.0" else "1.0"}"
                )
                shell("cmd uimode night ${if (suffix == "-dark") "yes" else "no"}")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                await("Your strength training")
                // Large-font navigation intentionally replaces wrapped labels with TalkBack names.
                compose
                    .onNode(
                        (hasText("Strength") or hasContentDescription("Strength")) and
                            hasClickAction()
                    )
                    .performClick()
                await("Gym programme")
                compose.onNodeWithText("Gym programme").performScrollTo().performClick()
                await("Build or review programmes")
                compose
                    .onNodeWithText("Build or review programmes")
                    .performScrollTo()
                    .performClick()
                await("Saved drafts")
                compose
                    .onNodeWithText("Templates & generated programmes")
                    .performScrollTo()
                    .performClick()
                await("Templates & generation")
                compose
                    .onNodeWithText("Review future programme changes")
                    .performScrollTo()
                    .performClick()
                await("Review every block")
                compose.onNodeWithText("Review every block").assertIsDisplayed()
                compose.onNodeWithText("Proposed revision 2", substring = true).assertExists()
                if (suffix == "-large-font")
                    Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
                if (suffix == "-dark")
                    Assert.assertEquals(
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                        context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK,
                    )
                capture("strength-future-preview$suffix")
                compose
                    .onNodeWithText("Regenerate future programme")
                    .performScrollTo()
                    .performClick()
                await("Shape your programme")
                compose.onNodeWithText("Shape your programme").assertIsDisplayed()
                compose.onNodeWithText("Start date · dd/mm/yyyy").assert(hasSetTextAction().not())
                capture("strength-future-settings$suffix")
                Assert.assertEquals(1, repository.currentReference()!!.revision())
                Assert.assertEquals(programme, repository.programme(programme.id(), 1))
                Assert.assertTrue(CoachingStore(context).strength().isEmpty())
                scenario?.close()
                scenario = null
            }
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesProgrammeReplacementChoicesInLightDarkAndLargeText() =
        captureAuthoredReviewScenes("alternatives")

    @Test
    fun capturesOptionalProgressionInLightDarkAndLargeText() =
        captureAuthoredReviewScenes("progression")

    @Test
    fun capturesPreservedProgrammeHistoryInLightDarkAndLargeText() =
        captureAuthoredReviewScenes("history")

    private fun captureAuthoredReviewScenes(kind: String) {
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val programme =
            StrengthProgrammeGenerator()
                .generate(
                    StrengthProgrammeGenerator.Request(
                        "capture-programme-replacement",
                        Instant.now(),
                        "Strength development",
                        StrengthProgramme.Settings(
                            GymProgramme.Goal.STRENGTH,
                            GymProgramme.Priority.BALANCED,
                            GymProgramme.Experience.INTERMEDIATE,
                            DayOfWeek.values().toSet(),
                            4,
                            60,
                            setOf(
                                ExerciseMetadata.Equipment.BARBELL,
                                ExerciseMetadata.Equipment.BENCH,
                                ExerciseMetadata.Equipment.SQUAT_RACK,
                                ExerciseMetadata.Equipment.DUMBBELLS,
                                ExerciseMetadata.Equipment.PULL_UP_BAR,
                            ),
                        ),
                        LocalDate.now(),
                        12,
                        "upper-lower-4",
                        org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        false,
                    )
                )
        val repository = SqliteStrengthProgrammingRepository(context)
        repository.activate(programme, null, "capture-replacement-base", emptyList())
        val actuals =
            if (kind == "progression") {
                val slot = programme.workouts().first().definition().slots().first()
                (1..2).map { day ->
                    StrengthSession(
                            "capture-progress-$day",
                            Instant.now().minus(Duration.ofDays(day.toLong())),
                            ZoneOffset.UTC,
                            Duration.ofMinutes(30),
                            null,
                            "",
                            listOf(
                                StrengthSession.ExerciseSets(
                                    slot.exercise(),
                                    (1..slot.target().sets()).map { index ->
                                        StrengthSession.SetEntry(
                                            "capture-set-$day-$index",
                                            slot.target().maximumReps(),
                                            80.0,
                                            7.0,
                                            false,
                                        )
                                    },
                                )
                            ),
                        )
                        .also { CoachingStore(context).saveStrength(it) }
                }
            } else emptyList()
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            listOf("", "-dark", "-large-font").forEach { suffix ->
                shell(
                    "settings put system font_scale ${if (suffix == "-large-font") "2.0" else "1.0"}"
                )
                shell("cmd uimode night ${if (suffix == "-dark") "yes" else "no"}")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                await("Your strength training")
                compose
                    .onNode(
                        (hasText("Strength") or hasContentDescription("Strength")) and
                            hasClickAction()
                    )
                    .performClick()
                await("Gym programme")
                compose.onNodeWithText("Gym programme").performScrollTo().performClick()
                await("Build or review programmes")
                compose
                    .onNodeWithText("Build or review programmes")
                    .performScrollTo()
                    .performClick()
                await("Saved drafts")
                compose
                    .onNodeWithText("Templates & generated programmes")
                    .performScrollTo()
                    .performClick()
                await("Templates & generation")
                if (kind == "history") {
                    compose
                        .onNodeWithText("Programme revision history")
                        .performScrollTo()
                        .performClick()
                    await("Review ${programme.name()} revision 1")
                    compose
                        .onNode(hasScrollToIndexAction())
                        .performScrollToNode(hasText("Review ${programme.name()} revision 1"))
                    compose.onNodeWithText("Review ${programme.name()} revision 1").performClick()
                    await("${programme.name()} · revision 1")
                    compose.onNodeWithText("${programme.name()} · revision 1").assertIsDisplayed()
                } else {
                    compose
                        .onNodeWithText("Review future programme changes")
                        .performScrollTo()
                        .performClick()
                    await("Review every block")
                    val workout = programme.workouts().first()
                    compose
                        .onNodeWithText(
                            "${friendlyDate(workout.date())} · ${workout.definition().title()} · about ${workout.expectedMinutes()} min"
                        )
                        .performScrollTo()
                        .performClick()
                    await("Dated workout")
                    compose
                        .onNodeWithText(
                            if (kind == "progression")
                                "Review progression for ${workout.definition().slots().first().exercise().name()}"
                            else
                                "Swap ${workout.definition().slots().first().exercise().name()} in programme"
                        )
                        .performScrollTo()
                        .performClick()
                    val heading =
                        if (kind == "progression") "Your workout suggestion"
                        else "Programme replacement"
                    await(heading)
                    compose.onNodeWithText(heading).assertIsDisplayed()
                    if (kind == "progression") {
                        compose.onNodeWithText("Here's what to do").assertIsDisplayed()
                        compose.onNodeWithText("Keep my current target").assertIsDisplayed()
                        // At 200% the entry viewport cannot contain every instruction. Capture
                        // the full task and weight cards at their real scroll positions as well.
                        compose
                            .onNodeWithTag("progression-review")
                            .performScrollToNode(
                                hasText(
                                    "Aim for moderate to challenging perceived effort.",
                                    substring = true,
                                )
                            )
                        compose.onNodeWithText("Here's what to do").assertIsDisplayed()
                        compose
                            .onNodeWithText("Complete 2 sets of 4–6 repetitions.")
                            .assertIsDisplayed()
                        compose
                            .onNodeWithText("Rest for 3 minutes between sets.")
                            .assertIsDisplayed()
                        capture("strength-programme-progression-instructions$suffix")
                        compose
                            .onNodeWithTag("progression-review")
                            .performScrollToNode(
                                hasText("suggested new weight reference is 82 kg", substring = true)
                            )
                        compose
                            .onNode(
                                hasText("suggested new weight reference is 82 kg", substring = true)
                            )
                            .assertIsDisplayed()
                        compose
                            .onNodeWithTag("progression-review")
                            .performScrollToNode(
                                hasText(
                                    "not a weight you must lift or a recorded result",
                                    substring = true,
                                )
                            )
                        compose.onNodeWithText("About the weight").assertIsDisplayed()
                        compose
                            .onNodeWithText("Your current target has no weight reference.")
                            .assertIsDisplayed()
                        compose
                            .onNode(
                                hasText("suggested new weight reference is 82 kg", substring = true)
                            )
                            .assertIsDisplayed()
                        capture("strength-programme-progression-weight$suffix")
                        compose
                            .onNode(hasScrollToIndexAction())
                            .performScrollToNode(hasText("Save this suggestion for review"))
                        compose
                            .onNodeWithText("Save this suggestion for review")
                            .assertIsDisplayed()
                        compose
                            .onNodeWithText("Advanced details")
                            .assertIsDisplayed()
                            .assert(
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.StateDescription,
                                    "Collapsed",
                                )
                            )
                        capture("strength-programme-progression-controls$suffix")
                        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
                    }
                }
                if (suffix == "-large-font")
                    Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
                if (suffix == "-dark")
                    Assert.assertEquals(
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                        context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK,
                    )
                capture("strength-programme-$kind$suffix")
                if (kind == "progression") {
                    compose
                        .onNodeWithTag("progression-review")
                        .performScrollToNode(hasText("Advanced details"))
                    compose.onNodeWithText("Advanced details").performClick()
                    compose
                        .onNodeWithTag("progression-review")
                        .performScrollToNode(hasText("What's changing?"))
                    compose.onNodeWithText("What's changing?").assertIsDisplayed()
                    compose.onNodeWithTag("change-weight-reference").assertExists()
                    capture("strength-programme-progression-details$suffix")
                    compose
                        .onNodeWithTag("progression-review")
                        .performScrollToNode(hasText("What's staying the same?"))
                    compose.onNodeWithText("What's staying the same?").assertIsDisplayed()
                    compose.onNodeWithTag("unchanged-sets").assertIsDisplayed()
                    compose.onNodeWithTag("unchanged-repetitions").assertIsDisplayed()
                    capture("strength-programme-progression-targets$suffix")
                    // Inspect the lower rows separately rather than requiring the full card
                    // and the following disclosure to fit a 200% font viewport together.
                    compose
                        .onNodeWithTag("progression-review")
                        .performScrollToNode(hasTestTag("unchanged-rest"))
                    compose.onNodeWithTag("unchanged-effort").performScrollTo()
                    compose.onNodeWithTag("unchanged-rest").assertIsDisplayed()
                    compose.onNodeWithTag("unchanged-effort").assertIsDisplayed()
                    capture("strength-programme-progression-unchanged$suffix")
                    compose
                        .onNodeWithTag("progression-review")
                        .performScrollToNode(hasText("View supporting workouts"))
                    compose
                        .onNodeWithText("View supporting workouts")
                        .assertIsDisplayed()
                        .assert(
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.StateDescription,
                                "Collapsed",
                            )
                        )
                    compose.onNodeWithText("Why this suggestion?").assertIsDisplayed()
                    compose.onNodeWithText("Actual logged working sets").assertDoesNotExist()
                    capture("strength-programme-progression-context$suffix")
                    compose.onNodeWithText("View supporting workouts").performClick()
                    compose
                        .onNodeWithTag("progression-review")
                        .performScrollToNode(hasText("Supporting workouts"))
                    compose
                        .onAllNodesWithText("80 kg recorded · 6 repetitions")
                        .onFirst()
                        .performScrollTo()
                        .assertIsDisplayed()
                    compose.onNodeWithText("82 kg recorded · 6 repetitions").assertDoesNotExist()
                    capture("strength-programme-progression-evidence$suffix")
                    // The evidence is one tall lazy item. Align its last actual set using
                    // measured content coordinates: scrolling to its final paragraph can
                    // legitimately leave the preceding heading above the viewport.
                    alignProgressionCapture(
                        compose.onAllNodesWithText("80 kg recorded · 6 repetitions").onLast()
                    )
                    compose
                        .onAllNodesWithText("80 kg recorded · 6 repetitions")
                        .onLast()
                        .assertIsDisplayed()
                    compose.onNodeWithText("Calculation & safeguards").assertIsDisplayed()
                    compose
                        .onNode(
                            hasText("Only recent exact-exercise working sets", substring = true)
                        )
                        .assertIsDisplayed()
                    capture("strength-programme-progression-safeguards$suffix")
                    val activationWarning =
                        compose.onNodeWithText(
                            "Saving this suggestion won't change your active programme. You can review and activate it separately."
                        )
                    alignProgressionCapture(activationWarning)
                    val saveAction = compose.onNodeWithText("Save this suggestion for review")
                    saveAction.assertIsDisplayed()
                    activationWarning.assertIsDisplayed()
                    assertProgressionFullyVisible(activationWarning)
                    assertProgressionFullyVisible(saveAction)
                    capture("strength-programme-progression-evidence-actions$suffix")
                }
                Assert.assertEquals(1, repository.currentReference()!!.revision())
                Assert.assertEquals(programme, repository.programme(programme.id(), 1))
                Assert.assertEquals(actuals.toSet(), CoachingStore(context).strength().toSet())
                scenario?.close()
                scenario = null
            }
        } finally {
            scenario?.close()
            scenario = null
            if (originalFont.matches(Regex("[0-9.]+")))
                shell("settings put system font_scale $originalFont")
            else shell("settings delete system font_scale")
            shell("cmd uimode night $originalNight")
        }
    }

    private fun gymNowModel(): GymNowViewModel {
        lateinit var result: GymNowViewModel
        scenario?.onActivity { activity ->
            result = ViewModelProvider(activity)[GymNowViewModel::class.java]
        }
        return result
    }

    private fun awaitGymReady(predicate: (GymNowState) -> Boolean = { true }) {
        val model = gymNowModel()
        compose.waitUntil(20000) {
            val state = model.state.value
            !state.loading &&
                !state.busy &&
                state.error == null &&
                state.view?.pending() == null &&
                predicate(state)
        }
        Assert.assertNull("A gym capture must not conceal a failed write", model.state.value.error)
        compose.waitForIdle()
    }

    private fun openGymNow() {
        await("Today")
        compose
            .onNode((hasText("Strength") or hasContentDescription("Strength")) and hasClickAction())
            .performClick()
        await("Log workout")
        compose.onNodeWithText("Gym now").performScrollTo().performClick()
        await("Train your way")
        awaitGymReady()
    }

    private fun enterGymActualSet(kilograms: String, reps: String, expectedSets: Int) {
        compose
            .onNodeWithText("Actual load · kg")
            .performScrollTo()
            .performTextReplacement(kilograms)
        compose.onNodeWithText("Actual repetitions").performScrollTo().performTextReplacement(reps)
        compose.onNodeWithText("RPE · optional").performScrollTo().performTextReplacement("7")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save actual set").performScrollTo().performClick()
        awaitGymReady { it.view?.accepted()?.execution()?.actualSets()?.size == expectedSets }
        Assert.assertEquals("", gymNowModel().state.value.form.kilograms)
        Assert.assertEquals("", gymNowModel().state.value.form.repetitions)
    }

    private fun pauseGymForRelaunch() {
        compose.onNodeWithText("Pause workout").performScrollTo().performClick()
        awaitGymReady { it.view?.accepted()?.execution()?.status() == GymExecution.Status.PAUSED }
        scenario?.close()
        scenario = null
    }

    private fun reopenAndRecoverGym(id: String, expectedSets: Int) {
        scenario = ActivityScenario.launch(CoachActivity::class.java)
        openGymNow()
        Assert.assertNull(
            "Opening the hub must not resume a staged workout",
            gymNowModel().state.value.view?.accepted(),
        )
        compose.onNodeWithText("Review unfinished workout").performScrollTo().performClick()
        awaitGymReady {
            val execution = it.view?.accepted()?.execution()
            execution?.id() == id &&
                execution.actualSets().size == expectedSets &&
                execution.status() == GymExecution.Status.PAUSED
        }
        compose.onNodeWithText("Resume workout").performScrollTo().performClick()
        awaitGymReady { it.view?.accepted()?.execution()?.status() == GymExecution.Status.ACTIVE }
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
            }

    private fun back() {
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
    }

    private fun await(text: String) {
        compose.waitUntil(20000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun alignHeading(text: String) {
        val heading = compose.onNodeWithText(text)
        heading.performScrollTo()
        val container = compose.onNode(hasScrollAction() and hasAnyDescendant(hasText(text)))
        repeat(8) {
            compose.waitForIdle()
            // boundsInRoot is CLIPPED: an offscreen node can report an empty Rect
            // at zero, which must never be accepted as an aligned screenshot.
            val top = heading.fetchSemanticsNode().positionInRoot.y
            val height = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
            val shift = (top - 160f).coerceIn(-height * .6f, height * .6f)
            if (kotlin.math.abs(shift) <= 24f) {
                heading.assertIsDisplayed()
                return
            }
            container.performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.ScrollBy
            ) {
                it(0f, shift)
            }
        }
        heading.assertIsDisplayed()
        val top = heading.fetchSemanticsNode().positionInRoot.y
        Assert.assertTrue(
            "Capture heading is not in the upper viewport: $text at $top",
            top in 100f..300f,
        )
    }

    private fun alignProgressionCapture(node: SemanticsNodeInteraction) {
        node.performScrollTo()
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("progression-review")
        val delta =
            node.fetchSemanticsNode().positionInRoot.y -
                viewport.fetchSemanticsNode().positionInRoot.y -
                16f * context.resources.displayMetrics.density
        viewport.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, delta) }
        compose.waitForIdle()
    }

    private fun assertProgressionFullyVisible(node: SemanticsNodeInteraction) {
        val viewport = compose.onNodeWithTag("progression-review").fetchSemanticsNode()
        val target = node.fetchSemanticsNode()
        val top = target.positionInRoot.y
        val bottom = top + target.size.height
        Assert.assertTrue(
            "The complete footer must fit, not merely a clipped sliver: $top..$bottom",
            top >= viewport.positionInRoot.y - 1f &&
                bottom <= viewport.positionInRoot.y + viewport.size.height + 1f,
        )
    }

    private fun capture(name: String, anchor: String? = null) {
        // Both exclude and restore acknowledge through normal app feedback. Never capture it
        // over instructional artwork or controls; wait for its real lifecycle, do not dismiss it.
        compose.waitUntil(20000) {
            compose
                .onAllNodesWithText(
                    "Exercise preference saved. Existing targets, history and PBs are unchanged."
                )
                .fetchSemanticsNodes()
                .isEmpty()
        }

        if (
            name in
                setOf(
                    "gym-association-review",
                    "gym-association-saved",
                    "gym-now",
                    "gym-workout-builder",
                    "gym-workout",
                    "gym-workout-saved",
                    "gym-workout-dark",
                    "gym-workout-large-font",
                )
        ) {
            awaitGymReady()
        }
        if (name == "home" || name.startsWith("home-") || name.startsWith("strength-home")) {
            awaitHomeReady(name.startsWith("strength-home"))
        }
        if (name.startsWith("dream-route")) {
            try {
                compose.waitUntil(20000) {
                    compose
                        .onAllNodes(
                            hasStateDescription("Offline city context ready"),
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                compose.waitUntil(20000) {
                    compose
                        .onAllNodes(
                            hasStateDescription("Offline Dream artwork ready"),
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError(
                    "Bundled city readiness failed for $name. UI: ${compose.onRoot(useUnmergedTree = true).printToString().take(20000)}",
                    failure,
                )
            }
        }
        // Idleness alone does not prove an asynchronous application snapshot is ready.
        // A valid PNG of the loading screen must never become a canonical app preview.
        compose.onAllNodesWithText("Loading your local training information…").assertCountEquals(0)
        if (name.startsWith("dream-route")) {
            val heading =
                compose.onNode(
                    if (anchor == null) hasText("Dream Route ·", substring = true)
                    else hasText(anchor)
                )
            heading.assertIsDisplayed()
            val top = heading.fetchSemanticsNode().positionInRoot.y
            Assert.assertTrue(
                "Dream Route capture must show its own heading: $name at $top",
                top in 100f..300f,
            )
        }
        val semantics =
            if (
                name == "gym-equipment-alternatives" ||
                    name.startsWith("strength-programme-alternatives") ||
                    name.startsWith("strength-programme-progression") ||
                    name.startsWith("strength-programme-history")
            ) {
                // The full-screen production dialog and its covered activity are separate roots.
                compose
                    .onNode(isDialog(), useUnmergedTree = true)
                    .assertIsDisplayed()
                    .printToString()
            } else compose.onRoot(useUnmergedTree = true).printToString()
        Assert.assertFalse(
            "Raw ISO date in normal UI",
            Regex("Text = .*\\b[0-9]{4}-[0-9]{2}-[0-9]{2}").containsMatchIn(semantics),
        )
        // Semantic queries above can advance Compose to a newer frame. Let that final
        // frame reach SurfaceFlinger before capturing the actual Android screen.
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(200)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        Assert.assertNotNull(bitmap)
        val dir = File(context.getExternalFilesDir(null), "screenshots")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        // AGP uninstalls the APK after connected tests, removing app-specific files.
        // Copy with instrumentation's shell identity before teardown; production has no such path.
        val shell = InstrumentationRegistry.getInstrumentation().uiAutomation
        shell.executeShellCommand("mkdir -p /data/local/tmp/racedaydreams-screenshots").use {
            descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
        shell
            .executeShellCommand(
                "cp ${File(dir,"$name.png").absolutePath} /data/local/tmp/racedaydreams-screenshots/$name.png"
            )
            .use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
    }

    private fun awaitHomeReady(strength: Boolean) {
        compose.waitUntil(20000) {
            // Use this scenario's model as well as semantics: a retiring activity's
            // Today node must not satisfy readiness while the new activity is loading.
            var snapshotReady = false
            scenario?.onActivity { activity ->
                val state = ViewModelProvider(activity)[CoachViewModel::class.java].state.value
                snapshotReady =
                    !state.loading &&
                        !state.busy &&
                        (state.primaryFocus ==
                            org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH) == strength
            }
            snapshotReady &&
                compose
                    .onAllNodesWithText("Loading your local training information…")
                    .fetchSemanticsNodes()
                    .isEmpty() &&
                compose.onAllNodesWithText("Today").fetchSemanticsNodes().isNotEmpty() &&
                compose
                    .onAllNodesWithText(
                        if (strength) "Your strength training" else "How am I doing?"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                compose
                    .onAllNodes(
                        hasText(if (strength) "Log today's workout" else "Run now") and
                            hasClickAction()
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose
            .onNodeWithText(if (strength) "Your strength training" else "How am I doing?")
            .assertExists()
    }
}
