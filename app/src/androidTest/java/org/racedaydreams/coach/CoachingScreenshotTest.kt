package org.racedaydreams.coach

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.*
import java.util.EnumSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    // Track only actual Activity/Dialog windows created by our LifecycleAware delegate.
    // The outer test rule owns a different, long-lived recomposer and must never be joined here.
    private val windowRecomposers = mutableMapOf<Lifecycle, MutableList<Recomposer>>()

    private data class ClosingHost(val jobs: List<Job>, val windows: List<Recomposer>)

    private companion object {
        // A failed barrier must also prevent the following fixture from deleting a live DB.
        val pendingHostShutdown = mutableListOf<ClosingHost>()
    }

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
                                val lifecycle =
                                    checkNotNull(view.findViewTreeLifecycleOwner()).lifecycle
                                WindowRecomposerFactory.LifecycleAware.createRecomposer(view).also {
                                    windowRecomposers
                                        .getOrPut(lifecycle) { mutableListOf() }
                                        .add(it)
                                }
                            }
                            WindowRecomposerPolicy.withFactory(mainWindows) { base.evaluate() }
                        }
                    }
                }
            )
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private var scenario: ActivityScenario<CoachActivity>? = null
    private var fixtureFontScale: String? = null

    @Before
    fun seed() {
        awaitClosedHosts()
        fixtureFontScale = shell("settings get system font_scale").trim()
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
            trace.heartRate(150.0 + (i % 60) / 4.0, i * 1000L)
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
        // Retained, independently entered results exercise real ranking/source presentation.
        // These records exist only in the instrumentation fixture, never in production startup.
        listOf(3380L, 3250L, 3160L).forEachIndexed { index, seconds ->
            store.saveResult(
                RecordedResult(
                    "demo-ranked-10k-$index",
                    PerformanceDistance.TEN_K,
                    Duration.ofSeconds(seconds),
                    today.minusDays(45L - index * 14L),
                    "Riverside 10K",
                    if (index == 2) ResultSource.OFFICIAL_CHIP else ResultSource.MANUAL,
                    null,
                    "Deterministic test-only entered result; not independently verified",
                )
            )
        }
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
        org.racedaydreams.recording.RunRecordingService.heartRate.value = null
        try {
            dismissCaptureDialogs()
            closeScenario()
        } finally {
            fixtureFontScale?.let { restoreFontScale(it) }
        }
        awaitClosedHosts()
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
        // The detail title is available before the retained GPS projection is published.
        // Wait for the real route/story content, as in the long-route flow below, before
        // capturing or scrolling its merged semantics. Do not capture a loading frame.
        await("Route story")
        await("Your recorded route")
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
        await("Strength records")
        compose
            .onNodeWithText("Historical entries & full PB history")
            .performScrollTo()
            .performClick()
        await("Exercise progress and PB history")
        compose.onAllNodesWithText("View progression")[0].performScrollTo().performClick()
        alignHeading("Exercise progress and PB history")
        capture("strength-pbs")
        back()
        back()
        compose.onNodeWithText("Exercises").performScrollTo().performClick()
        awaitExerciseSearch()
        capture("exercise-browse")
        compose.onNodeWithTag("exercise-search").performTextReplacement("de")
        compose.waitForIdle()
        capture("exercise-search")
        compose.onNodeWithTag("exercise-search").performTextReplacement("RDL")
        compose.onNodeWithText("Romanian Deadlift").performClick()
        await("Overview")
        capture("exercise-rdl")
        compose.onNodeWithText("Exclude from recommendations").performScrollTo().performClick()
        click("Exclude exercise")
        await("Restore exercise")
        back()
        awaitExerciseSearch()
        compose.onNodeWithTag("exercise-search").performTextReplacement("")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("exercise-shortcut-Excluded").performScrollTo().performClick()
        // This mode's title is the real library header. Bring the lazy header into view
        // rather than expecting a redundant body title to remain composed at any scroll.
        compose
            .onNodeWithTag("exercise-browser-list")
            .performScrollToNode(hasText("Excluded exercises"))
        await("Excluded exercises")
        alignHeading("Excluded exercises")
        capture("exercise-excluded")
        compose.onNodeWithText("Restore exercise").performScrollTo().performClick()
        compose.waitUntil(20000) {
            compose.onAllNodesWithText("Restore exercise").fetchSemanticsNodes().isEmpty()
        }
        // Restore removes the only Excluded row; leave that explicit filter before searching.
        compose.onNodeWithTag("exercise-shortcut-For you").performScrollTo().performClick()
        compose.onNodeWithTag("exercise-search").performScrollTo().performTextReplacement("RDL")
        compose.onNodeWithTag("exercise-row-romanian-deadlift").performScrollTo().performClick()
        await("Overview")
        click("How to do it")
        compose.waitForIdle()
        capture("exercise-rdl-howto")
        back()
        compose.onNodeWithTag("exercise-search").performTextReplacement("Bench")
        compose.onNodeWithText("Bench Press").performClick()
        await("Overview")
        click("How to do it")
        capture("exercise-bench-howto")
        back()
        compose.onNodeWithTag("exercise-search").performTextReplacement("BSS")
        compose.onNodeWithText("Bulgarian Split Squat").performClick()
        await("Overview")
        capture("exercise-bulgarian-split-squat")
        click("How to do it")
        capture("exercise-bulgarian-howto")
        back()
        compose.onNodeWithTag("exercise-search").performTextReplacement("Seated Cable Row")
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
            compose.onNodeWithTag("exercise-search").performTextReplacement(exercise)
            compose
                .onNode(hasText(exercise) and hasClickAction() and !hasSetTextAction())
                .performClick()
            await("Overview")
            click("How to do it")
            capture(filename)
            back()
        }
        compose.onNodeWithTag("exercise-search").performTextReplacement("Plank")
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
        compose.onNodeWithTag("exercise-search").performTextReplacement("Plank")
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
        compose.onNodeWithTag("exercise-search").performTextReplacement("Farmer")
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
        click("Settings")
        await("Activity data source")
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
        click("Settings")
        await("Activity data source")
        compose.onNodeWithText("Edit race goal and availability").performScrollTo().performClick()
        compose.waitForIdle()
        capture("onboarding")
        back()
        click("Settings")
        await("Activity data source")
        compose.onNodeWithText("Shoes and mileage").performScrollTo().performClick()
        await("Your shoes")
        capture("shoes")
        closeScenario()
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
            closeScenario()
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
            closeScenario()
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
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
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
            closeScenario()
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
            closeScenario()
            scenario = null
            shell("settings put system font_scale 2.0")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            await("Your strength training")
            Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
            capture("strength-home-large-font")
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesRecordedStrengthTrendsAndMeasuredVersusEstimatedRecords() {
        val today = LocalDate.now()
        val bench = ExerciseCatalog.definition("bench-press").exercise()
        for (week in 0..11) {
            if (week == 3) continue // An explicitly missing week, never interpolated training.
            CoachingStore(context)
                .saveStrength(
                    StrengthSession(
                        "volume-week-$week",
                        today
                            .minusDays(1)
                            .minusWeeks(week.toLong())
                            .atTime(12, 0)
                            .toInstant(ZoneOffset.UTC),
                        ZoneOffset.UTC,
                        Duration.ofMinutes(35),
                        null,
                        "Screenshot fixture, not user evidence",
                        listOf(
                            StrengthSession.ExerciseSets(
                                bench,
                                listOf(
                                    StrengthSession.SetEntry("warm", 5, 20.0, null, true),
                                    StrengthSession.SetEntry("work-a", 6, 65.0 - week, 7.0, false),
                                    StrengthSession.SetEntry("work-b", 6, 65.0 - week, 7.0, false),
                                ),
                            )
                        ),
                    )
                )
        }
        val before = CoachingStore(context).strength().toSet()
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
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                compose
                    .onNode(
                        (hasText("Strength") or hasContentDescription("Strength")) and
                            hasClickAction()
                    )
                    .performClick()
                await("Log workout")
                compose.onNodeWithText("Strength trends").performScrollTo().performClick()
                await("See what you recorded, rather than what your programme planned.")
                await("This week so far")
                alignHeading("Strength trends")
                capture("strength-analytics$suffix")
                compose.onNodeWithText("How are sets attributed?").performScrollTo().performClick()
                alignHeading("Primary areas this week")
                capture("strength-analytics-details$suffix")
                back()
                compose.onNodeWithText("PBs & progression").performScrollTo().performClick()
                await("Recorded performance")
                alignHeading("Strength records")
                capture("strength-records$suffix")
                compose
                    .onAllNodesWithText("View measured history")[0]
                    .performScrollTo()
                    .performClick()
                alignHeading("Repetition records and progression")
                capture("strength-records-history$suffix")
                back()
                compose.onNodeWithText("Recorded milestones").performScrollTo().performClick()
                await("✓ First recorded strength workout")
                alignHeading("Recorded milestones")
                capture("training-milestones$suffix")
                compose
                    .onAllNodesWithText("View milestone evidence")[0]
                    .performScrollTo()
                    .performClick()
                alignHeading("Hide milestone evidence")
                capture("training-milestones-evidence$suffix")
                Assert.assertEquals(before, CoachingStore(context).strength().toSet())
                if (scale == "2.0")
                    Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
                if (night == "yes")
                    Assert.assertEquals(
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                        context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK,
                    )
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesStoragePolicyPreviewAndExplicitConfirmationFromProductionNavigation() {
        val old = Instant.parse("2020-01-01T00:00:00Z")
        val engine = org.racedaydreams.domain.recording.RunMetricsEngine(0, old)
        for (i in 0..100) engine.accept(
            org.racedaydreams.domain.recording.RunMetricsEngine.Fix(
                old.plusSeconds(i.toLong()),
                i * 1000L,
                0.0,
                i * .00003,
                3.0,
                null,
                null,
                3.0,
            )
        )
        val store = RecordingStore(context)
        store.start("retention-capture", old, 0)
        store.appendBatch(
            "retention-capture",
            engine.points(),
            engine.events(),
            0,
            0,
            100000,
            "ACTIVE",
        )
        store.finish(store.unfinished()!!, null, "rdd", false)
        val retention = SqliteDetailRetentionRepository(context)
        retention.acknowledge(
            org.racedaydreams.domain.storage.DetailRetention.Policy.LAST_24_MONTHS,
            Instant.now(),
        )
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
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                click("Settings")
                await("Storage & recording detail")
                compose
                    .onNodeWithText("Storage & recording detail")
                    .performScrollTo()
                    .performClick()
                await("Your history stays")
                // Require the feature-owned snapshot, not only its static initial heading.
                await("Saved choice: 24 months")
                alignHeading("Storage & recording detail")
                capture("storage-management$suffix")
                alignHeading("Detailed recording retention")
                capture("storage-retention$suffix")
                compose.onNodeWithText("Review eligible detail").performScrollTo().performClick()
                await("Cleanup preview")
                alignHeading("Cleanup preview")
                capture("storage-preview$suffix")
                compose
                    .onNodeWithText("Review cleanup confirmation")
                    .performScrollTo()
                    .performClick()
                await("Remove this raw detail?")
                capture("storage-confirmation$suffix")
                compose
                    .onNodeWithText(
                        "Cleanup may continue while the app is running. If interrupted, refresh to review remaining detail."
                    )
                    .performScrollTo()
                    .assertIsDisplayed()
                compose.onNodeWithText("Remove previewed raw detail").assertIsDisplayed()
                compose.onNodeWithText("Keep my recording detail").assertIsDisplayed()
                capture("storage-confirmation-details$suffix")
                compose.onNodeWithText("Keep my recording detail").performClick()
                Assert.assertEquals(engine.points(), store.points("retention-capture"))
                Assert.assertNull(retention.removal("retention-capture"))
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun restoresUnsetFontAfterLargeTextBeforeNormalNavigation() {
        shell("settings delete system font_scale")
        repeat(3) {
            shell("settings put system font_scale 2.0")
            Assert.assertTrue(context.resources.configuration.fontScale >= 1.99f)
            restoreFontScale("null")
            Assert.assertEquals(1f, context.resources.configuration.fontScale, .01f)
        }
        scenario = ActivityScenario.launch(CoachActivity::class.java)
        awaitHomeReady(false)
        compose.onNodeWithText("Plan").assertIsDisplayed()
        compose.onNodeWithText("Calendar").assertIsDisplayed()
        compose.onNodeWithText("Progress").assertIsDisplayed()
    }

    @Test
    fun capturesExpandedCatalogueGuidanceInAllDisplayModes() {
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
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                compose
                    .onNode(
                        (hasText("Strength") or hasContentDescription("Strength")) and
                            hasClickAction()
                    )
                    .performClick()
                await("Log workout")
                compose.onNodeWithText("Exercises").performScrollTo().performClick()
                awaitExerciseSearch()
                for (id in ExpandedExerciseGuides.identities().sorted()) {
                    val exercise = ExerciseCatalog.definition(id).exercise()
                    compose
                        .onNodeWithTag("exercise-search")
                        .performScrollTo()
                        .performTextReplacement(exercise.name())
                    androidx.test.espresso.Espresso.closeSoftKeyboard()
                    compose.onNodeWithTag("exercise-row-$id").performScrollTo().performClick()
                    await("Overview")
                    click("How to do it")
                    capture("exercise-$id-howto$suffix")
                    if (
                        suffix == "-large-font" &&
                            id in
                                setOf(
                                    "side-lying-dumbbell-external-rotation",
                                    "back-extension-forty-five-degree",
                                    "glute-kickback-machine",
                                )
                    ) {
                        val mistakes =
                            TechniqueGuide.forExercise(id)!!.mistakes().joinToString("\n") {
                                "• $it"
                            }
                        val body = compose.onNodeWithText(mistakes)
                        // This is near the page end: max scroll may prevent an arbitrary
                        // upper heading position. Require the complete actual content instead.
                        body.performScrollTo().assertIsDisplayed()
                        compose.onNodeWithText("Common mistakes").assertIsDisplayed()
                        val viewport =
                            compose
                                .onNode(hasScrollAction() and hasAnyDescendant(hasText(mistakes)))
                                .fetchSemanticsNode()
                        val detail = body.fetchSemanticsNode()
                        Assert.assertTrue(
                            "Complete mistakes text must fit the scroll viewport for $id",
                            detail.positionInRoot.y >= viewport.positionInRoot.y - 1f &&
                                detail.positionInRoot.y + detail.size.height <=
                                    viewport.positionInRoot.y + viewport.size.height + 1f,
                        )
                        capture("exercise-$id-howto-details$suffix", "Common mistakes")
                        compose
                            .onNodeWithText(
                                "Alternatives: ${org.racedaydreams.domain.strength.TechniqueGuide.forExercise(id)!!.alternatives().joinToString()}"
                            )
                            .performScrollTo()
                            .assertIsDisplayed()
                    }
                    back()
                    awaitExerciseSearch()
                }
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesOptionalBodyGuidanceAndSourceHistory() {
        val body = org.racedaydreams.data.SqliteBodyMeasurementsRepository(context)
        val now = Instant.now()
        val weight =
            org.racedaydreams.domain.body.BodyWeight(
                "body-guidance-fixture",
                87.0,
                now.minusSeconds(86400),
                ZoneOffset.UTC,
                "manual",
                "body-guidance-fixture",
                null,
            )
        body.saveWeight(weight)
        body.saveHeight(
            org.racedaydreams.domain.body.BodyHeight(
                "body-height-fixture",
                1.78,
                now.minusSeconds(86400),
                ZoneOffset.UTC,
                "manual",
                "body-height-fixture",
                null,
                null,
            )
        )
        body.preferences(
            org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences(
                true,
                org.racedaydreams.domain.body.HeightUnit.CENTIMETRES,
                org.racedaydreams.domain.body.ProteinGuidance.Basis.from(weight),
            )
        )
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.HYBRID)
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
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                compose
                    .onNode(
                        (hasText("Progress") or hasContentDescription("Progress")) and
                            hasClickAction()
                    )
                    .performClick()
                await("Body & protein guidance")
                compose.onNodeWithText("Body & protein guidance").performScrollTo().performClick()
                await("A general daily range is about 120–175 grams of protein.")
                alignHeading("Body & protein guidance")
                capture("body-protein$suffix")
                compose.onNodeWithText("Calculation & sources ▾").performScrollTo().performClick()
                alignHeading("Calculation & sources ▴")
                capture("body-protein-details$suffix")
                compose.onNodeWithText("Choose or record a weight").performScrollTo().performClick()
                alignHeading("Record a measurement")
                capture("body-measurements$suffix")
                Assert.assertEquals(weight, body.weights().first { it.id() == weight.id() })
                Assert.assertTrue(requireNotNull(body.preferences().basis()).matches(weight))
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesRecoveryGuidanceFromActualRecordedEvidence() {
        val today = LocalDate.now()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        // Today's authored session makes the Strength Home action an actual Start workout.
        val programmes = SqliteStrengthProgrammingRepository(context)
        val programme =
            StrengthProgrammeGenerator()
                .generate(
                    StrengthProgrammeGenerator.Request(
                        "recovery-capture",
                        Instant.now(),
                        "Strength foundation",
                        StrengthProgramme.Settings(
                            GymProgramme.Goal.STRENGTH,
                            GymProgramme.Priority.STRENGTH_FIRST,
                            GymProgramme.Experience.INTERMEDIATE,
                            DayOfWeek.values().toSet(),
                            3,
                            60,
                            ExerciseMetadata.Equipment.values().toSet(),
                        ),
                        today,
                        6,
                        "full-body-3",
                        org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        false,
                    )
                )
        Assert.assertTrue(programme.workouts().any { it.date() == today })
        programmes.activate(
            programme,
            programmes.currentReference(),
            "recovery-capture-accept",
            emptyList(),
        )
        val bench = ExerciseCatalog.definition("bench-press").exercise()
        for (days in listOf(1L, 5L, 8L)) CoachingStore(context)
            .saveStrength(
                StrengthSession(
                    "recovery-$days",
                    today.minusDays(days).atTime(12, 0).toInstant(ZoneOffset.UTC),
                    ZoneOffset.UTC,
                    Duration.ofMinutes(40),
                    null,
                    "Screenshot fixture, not user evidence",
                    listOf(
                        StrengthSession.ExerciseSets(
                            bench,
                            listOf(
                                StrengthSession.SetEntry("work-a", 5, 60.0, 9.0, false),
                                StrengthSession.SetEntry("work-b", 5, 60.0, 9.0, false),
                            ),
                        )
                    ),
                )
            )
        val before = CoachingStore(context).strength().toSet()
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
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(true, "Start workout")
                compose
                    .onNode(
                        (hasText("Strength") or hasContentDescription("Strength")) and
                            hasClickAction()
                    )
                    .performClick()
                await("Log workout")
                compose
                    .onNodeWithText("Recovery & training balance")
                    .performScrollTo()
                    .performClick()
                await("An optional lighter week is available for review")
                alignHeading("Recovery & training balance")
                capture("training-recovery$suffix")
                compose.onNodeWithText("Supporting evidence ▾").performScrollTo().performClick()
                alignHeading("Why this guidance?")
                capture("training-recovery-evidence$suffix")
                compose.onNodeWithText("Keep my current programme").performScrollTo().performClick()
                await("Your current programme is unchanged.")
                Assert.assertEquals(before, CoachingStore(context).strength().toSet())
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesOptionalGymPreparationAndSupersetFromProductionScreens() {
        // One staged workout across all modes: never delete a database beneath live feature owners.
        SqliteUserPreferencesRepository(context).clear()
        SqlitePrimaryFocusRepository(context)
            .save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        var executionId: String? = null
        try {
            for ((suffix, night, scale) in
                listOf(
                    Triple("", "no", "1.0"),
                    Triple("-dark", "yes", "1.0"),
                    Triple("-large-font", "no", "2.0"),
                )) {
                shell("settings put system font_scale $scale")
                shell("cmd uimode night $night")
                val savedId = executionId
                if (savedId == null) {
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
                    executionId = gymNowModel().state.value.view!!.observed()!!.id()
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
                } else reopenAndRecoverGym(savedId, 0)
                Assert.assertEquals(
                    org.racedaydreams.domain.strength.GymRestTimer.Phase.PAUSED,
                    gymNowModel().state.value.restTimer!!.phase(),
                )
                alignHeading("Optional rest timer")
                capture("gym-rest-timer$suffix")
                compose.onNodeWithText("Timer settings ▾").performScrollTo().performClick()
                alignHeading("Default rest when no target is prescribed")
                capture("gym-rest-settings$suffix")
                compose.onNodeWithText("Timer settings ▴").performScrollTo().performClick()
                compose.onNodeWithText("Optional warm-up ▾").performScrollTo().performClick()
                alignHeading("Optional warm-up ▴")
                capture("gym-warmup$suffix")
                compose.onNodeWithText("Why this preparation?").performScrollTo().performClick()
                alignHeading("Hide preparation evidence")
                capture("gym-warmup-evidence$suffix")
                compose.onNodeWithText("Hide preparation evidence").performScrollTo().performClick()
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
                pauseGymForRelaunch()
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
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
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
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
            closeScenario()
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
            closeScenario()
            scenario = null
            shell("cmd uimode night yes")
            openCity(london)
            capture("dream-route-dark")
            alignHeading("Key landmarks on this route")
            capture("dream-route-dark-landmarks", "Key landmarks on this route")
            closeScenario()
            scenario = null
            shell("settings put system font_scale 2.0")
            openCity(london)
            capture("dream-route-large-font")
            alignHeading("Key landmarks on this route")
            capture("dream-route-large-font-landmarks", "Key landmarks on this route")
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
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
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
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
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
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
                closeScenario()
                scenario = null
            }
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
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
        var originalFailure: Throwable? = null
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
                            .onNode(
                                hasText("Complete 2 sets of 4–6 repetitions.") and
                                    hasAnyAncestor(hasTestTag("progression-review"))
                            )
                            .assertIsDisplayed()
                        compose
                            .onNode(
                                hasText("Rest for 3 minutes between sets.") and
                                    hasAnyAncestor(hasTestTag("progression-review"))
                            )
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
                dismissCaptureDialogs()
                closeScenario()
                scenario = null
            }
        } catch (failure: Throwable) {
            originalFailure = failure
            android.util.Log.i("RddGate", "CAPTURE_FAILURE $kind ${failure.javaClass.name}")
            failure.stackTrace.take(16).forEach { android.util.Log.i("RddGate", "  at $it") }
            throw failure
        } finally {
            try {
                dismissCaptureDialogs()
                closeScenario()
                restoreFontScale(originalFont)
                shell("cmd uimode night $originalNight")
            } catch (cleanupFailure: Throwable) {
                val primary = originalFailure
                if (primary != null) primary.addSuppressed(cleanupFailure) else throw cleanupFailure
            }
        }
    }

    /** Closing an Activity cancels its scopes; joining proves their blocking IO has returned. */
    private fun closeScenario() {
        val host = scenario
        if (host != null) {
            var closing: ClosingHost? = null
            host.onActivity { activity ->
                val coach = ViewModelProvider(activity)[CoachViewModel::class.java]
                // Configuration recreation destroys the old window while retaining its
                // ViewModel. Its composition-owned IO must finish before fixture reset too.
                val lifecycles =
                    windowRecomposers.keys.filter {
                        it == activity.lifecycle || it.currentState == Lifecycle.State.DESTROYED
                    }
                val windows = lifecycles.flatMap { windowRecomposers.remove(it).orEmpty() }
                closing =
                    ClosingHost(
                        listOfNotNull(
                            coach.viewModelScope.coroutineContext[Job],
                            activity.lifecycleScope.coroutineContext[Job],
                        ),
                        windows,
                    )
            }
            synchronized(pendingHostShutdown) { pendingHostShutdown.add(checkNotNull(closing)) }
            try {
                host.close()
            } finally {
                scenario = null
            }
        }
        awaitClosedHosts()
    }

    private fun awaitClosedHosts() {
        val closing = synchronized(pendingHostShutdown) { pendingHostShutdown.toList() }
        if (closing.isEmpty()) return
        runBlocking {
            withTimeout(20000) {
                closing.forEach { host ->
                    host.jobs.forEach { it.join() }
                    host.windows.forEach { it.join() }
                }
            }
        }
        synchronized(pendingHostShutdown) { pendingHostShutdown.removeAll(closing.toSet()) }
    }

    /** Dismiss actual popup windows before synchronously destroying their host renderer. */
    private fun dismissCaptureDialogs() {
        if (scenario == null) return
        repeat(3) {
            val count =
                compose
                    .onAllNodes(isDialog(), useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .size
            if (count == 0) return
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            androidx.test.espresso.Espresso.pressBack()
            compose.waitUntil(20000) {
                compose
                    .onAllNodes(isDialog(), useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .size < count
            }
        }
        compose.onAllNodes(isDialog(), useUnmergedTree = true).assertCountEquals(0)
    }

    private fun gymNowModel(): GymNowViewModel {
        lateinit var result: GymNowViewModel
        scenario?.onActivity { activity ->
            result = ViewModelProvider(activity)[GymNowViewModel::class.java]
        }
        return result
    }

    @Test
    fun capturesRefinedLiveStatisticsRouteAndPausedStatesWithoutChangingEvidence() {
        val store = RecordingStore(context)
        withRefinementCaptureModes { suffix ->
            val sessionId = "refinement-live-$suffix"
            val started = Instant.now().minusSeconds(1800)
            val engine = org.racedaydreams.domain.recording.RunMetricsEngine(0, started)
            for (second in 0..1800) {
                if (second in 850..900) {
                    if (second == 850)
                        engine.missingFix(second * 1000L, started.plusSeconds(second.toLong()))
                    continue
                }
                val angle = second * 2 * Math.PI / 1800
                engine.heartRate(150.0 + second % 12, second * 1000L)
                engine.accept(
                    org.racedaydreams.domain.recording.RunMetricsEngine.Fix(
                        started.plusSeconds(second.toLong()),
                        second * 1000L,
                        51.5 + .01 * kotlin.math.sin(angle),
                        -.1 + .017 * kotlin.math.cos(angle),
                        3.0,
                        null,
                        null,
                        3.0,
                    )
                )
            }
            val originalPoints = engine.points().toList()
            Assert.assertTrue(originalPoints.any { it.discontinuity() })
            store.start(sessionId, started, 0)
            store.flush(sessionId, engine.points(), engine.events(), 0, 0, 1800000, "ACTIVE")
            try {
                val status = engine.status(1800000)
                val workout =
                    org.racedaydreams.domain.execution
                        .WorkoutExecutionEngine(
                            listOf(
                                org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step(
                                    "Steady run",
                                    WorkoutSegment.Kind.RUN,
                                    8000.0,
                                    null,
                                    null,
                                    null,
                                    4.0,
                                )
                            )
                        )
                        .update(
                            org.racedaydreams.domain.execution.WorkoutExecutionEngine.Input(
                                status.metres(),
                                status.elapsedMillis(),
                                status.movingMillis(),
                                status.secondsPerKm(),
                                true,
                                false,
                            )
                        )
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    org.racedaydreams.recording.RunRecordingService.status.value = status
                    org.racedaydreams.recording.RunRecordingService.workoutState.value = workout
                    org.racedaydreams.recording.RunRecordingService.heartRate.value = 150.0
                }
                openRefinementRecording()
                compose.onNodeWithTag("live-elapsed").performScrollTo().assertIsDisplayed()
                compose.onNodeWithText("Pause").assertIsDisplayed()
                compose.onNodeWithText("Finish & save").assertIsDisplayed()
                capture("live-refined-statistics$suffix")

                compose.onNodeWithTag("live-route-tab").performScrollTo().performClick()
                compose.waitUntil(20000) {
                    compose
                        .onAllNodesWithTag("live-route-canvas")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                compose.onNodeWithTag("live-route-canvas").performScrollTo().assertIsDisplayed()
                compose.onNodeWithText("GPS gaps remain blank.").assertExists()
                compose.onNodeWithText("Pause").assertIsDisplayed()
                compose.onNodeWithText("Finish & save").assertIsDisplayed()
                capture("live-refined-route$suffix")
                Assert.assertEquals(originalPoints, store.points(sessionId))
                Assert.assertEquals("ACTIVE", store.unfinished()!!.state)

                compose.onNodeWithTag("live-statistics-tab").performScrollTo().performClick()
                val previousEvents = engine.events().size
                engine.pause(1800000, started.plusSeconds(1800))
                store.flush(
                    sessionId,
                    engine.points(),
                    engine.events(),
                    originalPoints.size,
                    previousEvents,
                    1800000,
                    "PAUSED",
                )
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    org.racedaydreams.recording.RunRecordingService.status.value =
                        engine.status(1800000)
                }
                await("PAUSED")
                compose.onNodeWithText("Resume").assertIsDisplayed()
                compose.onNodeWithText("Finish & save").assertIsDisplayed()
                compose.onNodeWithTag("live-elapsed").performScrollTo()
                capture("live-refined-paused$suffix")
                Assert.assertEquals(originalPoints, store.points(sessionId))
            } finally {
                closeScenario()
                scenario = null
                clearRefinementLiveState()
                store.discardUnfinished(sessionId)
            }
            if (suffix == "-large-font") {
                val emptyId = "refinement-live-empty"
                store.start(emptyId, Instant.now(), 0)
                try {
                    scenario = ActivityScenario.launch(CoachActivity::class.java)
                    awaitHomeReady(false)
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        org.racedaydreams.recording.RunRecordingService.status.value =
                            org.racedaydreams.domain.recording.RunMetricsEngine.Status(
                                0.0,
                                60000,
                                60000,
                                null,
                                "Waiting for GPS",
                                false,
                            )
                    }
                    openRefinementRecording()
                    compose.onNodeWithTag("live-route-tab").performScrollTo().performClick()
                    await(
                        "Your route will appear after two usable GPS observations have been saved."
                    )
                    alignHeading("Your recorded route")
                    compose.onNodeWithText("Pause").assertIsDisplayed()
                    compose.onNodeWithText("Finish & save").assertIsDisplayed()
                    capture("live-refined-route-empty-large-font")
                    Assert.assertTrue(store.points(emptyId).isEmpty())
                } finally {
                    closeScenario()
                    scenario = null
                    clearRefinementLiveState()
                    store.discardUnfinished(emptyId)
                }
            }
        }
    }

    @Test
    fun capturesRefinedRunningRecordsTrendsHeartRateAndRunSummaries() {
        withRefinementCaptureModes { suffix ->
            assertOriginalBottomNavigationOrder()
            clickOriginalNavigationTab("Progress")
            await("Running PBs")
            click("Running PBs")
            await("How PBs are calculated")
            alignHeading("10K")
            compose.onNodeWithTag("running-record-10k-1").assertExists()
            compose.onNodeWithTag("running-record-10k-2").assertExists()
            compose.onNodeWithTag("running-record-10k-3").assertExists()
            capture("running-records-refined$suffix")
            if (suffix == "-large-font") {
                val third = compose.onNodeWithTag("running-record-10k-3")
                third.performScrollTo()
                assertFullyVisibleInScroll(third, "running-record-10k-3")
                capture("running-records-third-large-font")
            }
            back()
            click("Training trends")
            await("Weekly recorded distance")
            compose.onNodeWithText("4 weeks").performScrollTo().performClick().assertIsSelected()
            alignHeading("Weekly recorded distance")
            capture("running-trends-distance$suffix")
            compose.onNodeWithText("Pace").performScrollTo().performClick().assertIsSelected()
            await("Weekly elapsed average pace")
            alignHeading("Weekly elapsed average pace")
            capture("running-trends-pace$suffix")
            compose
                .onNodeWithText("Training time")
                .performScrollTo()
                .performClick()
                .assertIsSelected()
            await("Weekly recorded running time")
            alignHeading("Weekly recorded running time")
            capture("running-trends-time$suffix")
            compose.onNodeWithText("Heart rate").performScrollTo().performClick()
            await("Recorded heart rate")
            alignHeading("Recorded heart rate")
            compose.onNodeWithText("Average heart rate").assertExists()
            compose.onNodeWithText("Maximum heart rate").assertExists()
            capture("running-heart-rate$suffix")
            if (suffix == "-large-font") {
                val chart = compose.onNodeWithTag("running-heart-rate-chart")
                chart.performScrollTo()
                alignHeading("Recent recorded readings")
                assertFullyVisibleInScroll(chart, "running-heart-rate-chart")
                capture("running-heart-rate-chart-large-font")
            }
            compose.onNodeWithText("View earlier run").performScrollTo().performClick()
            await("No heart-rate measurements recorded for this run.")
            alignHeading("Recorded heart rate")
            capture("running-heart-rate-empty$suffix")
            back()
            clickOriginalNavigationTab("Calendar")
            await("Activity calendar")
            compose.waitUntil(20000) {
                compose
                    .onAllNodes(hasStateDescription("Recorded history ready"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Recorded run"))
            compose.onAllNodesWithText("Recorded run")[0].performClick()
            await("Run detail")
            await("Your recorded route")
            alignHeading("Recorded summary")
            compose.onNodeWithTag("run-summary-metrics").assertIsDisplayed()
            capture("run-summary-refined$suffix")
            alignHeading("Distance splits")
            compose
                .onNodeWithTag("run-split-2")
                .performScrollTo()
                .performClick()
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.StateDescription,
                        "Selected split",
                    )
                )
            val selectedSplit = compose.onNodeWithTag("run-split-2")
            selectedSplit.performScrollTo()
            assertFullyVisibleInScroll(selectedSplit, "run-split-2")
            capture("run-splits-refined$suffix")
        }
    }

    @Test
    fun capturesNarrowLongElapsedAtDoubleTextWithCompleteControlsAndParagraph() {
        val prefs =
            context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
        val dynamic = prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true)
        val font = shell("settings get system font_scale").trim()
        val night =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        val originalWidth = context.resources.configuration.screenWidthDp
        val originalSize =
            Regex("Override size: ([0-9]+x[0-9]+)").find(shell("wm size"))?.groupValues?.get(1)
        // Keep the existing density. At the CI210dpi this is420x945 physical pixels,
        // exercising a real320dp phone rather than pretending540 pixels means540dp.
        val density = context.resources.displayMetrics.density
        val narrowWidth = kotlin.math.round(320f * density).toInt()
        val narrowHeight = kotlin.math.round(720f * density).toInt()
        try {
            closeScenario()
            scenario = null
            clearRefinementLiveState()
            prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, false).commit()
            shell("wm size ${narrowWidth}x${narrowHeight}")
            shell("cmd uimode night no")
            shell("settings put system font_scale 2.0")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            awaitHomeReady(false)
            compose.waitUntil(20000) {
                actualMaterialWindow()?.let { window ->
                    window.widthDp in 319..321 &&
                        window.decorWidthDp in 319f..321f &&
                        window.fontScale >= 1.99f &&
                        window.focused
                } == true
            }
            awaitMaterialSystemBars(false, "narrow200")
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                org.racedaydreams.recording.RunRecordingService.status.value =
                    org.racedaydreams.domain.recording.RunMetricsEngine.Status(
                        42195.0,
                        Duration.ofHours(10).plusMinutes(28).plusSeconds(14).toMillis(),
                        Duration.ofHours(4).plusMinutes(23).toMillis(),
                        375.0,
                        "GPS ready",
                        false,
                    )
                org.racedaydreams.recording.RunRecordingService.heartRate.value = 153.0
            }
            openRefinementRecording()
            val statistics = compose.onNodeWithTag("live-statistics-tab")
            statistics.performScrollTo().assertIsSelected()
            assertFullyVisibleInScroll(statistics, "live-statistics-tab")
            val route = compose.onNodeWithTag("live-route-tab")
            route.performScrollTo().assertIsNotSelected()
            assertFullyVisibleInScroll(route, "live-route-tab")
            val statisticsBounds = statistics.fetchSemanticsNode()
            val routeBounds = route.fetchSemanticsNode()
            Assert.assertTrue(
                "Narrow large-text mode controls must have distinct, non-overlapping hit areas",
                statisticsBounds.positionInRoot.y + statisticsBounds.size.height <=
                    routeBounds.positionInRoot.y + 1f,
            )
            val clock = compose.onNodeWithText("10:28:14", useUnmergedTree = true)
            clock.performScrollTo()
            val paragraphs = mutableListOf<TextLayoutResult>()
            clock.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                Assert.assertTrue(action(paragraphs))
            }
            val paragraph = paragraphs.single()
            Assert.assertEquals("10:28:14", paragraph.layoutInput.text.text)
            Assert.assertFalse(
                "Long elapsed time must not overflow horizontally",
                paragraph.didOverflowWidth,
            )
            Assert.assertFalse(
                "Long elapsed time must not overflow vertically",
                paragraph.didOverflowHeight,
            )
            Assert.assertTrue(paragraph.lineCount >= 1)
            Assert.assertEquals(8, paragraph.getLineEnd(paragraph.lineCount - 1, visibleEnd = true))
            compose
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
                )
                .assertCountEquals(0)
            alignHeading("Elapsed time")
            assertFullyVisibleInScroll(clock, "live-elapsed")
            assertNarrowLiveControlsFullyVisible()
            capture("live-refined-narrow-large-font")
            compose.onNodeWithText("Distance").performScrollTo()
            alignHeading("Distance")
            compose.onNodeWithText("42.20 km").assertExists()
            assertNarrowLiveControlsFullyVisible()
            capture("live-refined-narrow-metrics-large-font")
        } finally {
            closeScenario()
            scenario = null
            clearRefinementLiveState()
            prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, dynamic).commit()
            restoreFontScale(font)
            shell("wm size ${originalSize ?: "reset"}")
            shell("cmd uimode night $night")
            compose.waitUntil(20000) {
                context.resources.configuration.screenWidthDp == originalWidth
            }
        }
    }

    private fun clickOriginalNavigationTab(label: String) {
        compose
            .onNode((hasText(label) or hasContentDescription(label)) and hasClickAction())
            .assertIsDisplayed()
            .performClick()
        compose.waitForIdle()
    }

    private fun assertFullyVisibleInScroll(node: SemanticsNodeInteraction, descendantTag: String) {
        node.assertIsDisplayed()
        val viewport =
            compose
                .onNode(hasScrollAction() and hasAnyDescendant(hasTestTag(descendantTag)))
                .fetchSemanticsNode()
        assertFullyInside(node, viewport.boundsInRoot)
    }

    private fun assertNarrowLiveControlsFullyVisible() {
        val panel = compose.onNodeWithTag("live-running-panel").fetchSemanticsNode()
        listOf("Pause", "Finish & save").forEach { label ->
            val control = compose.onNodeWithText(label).assertIsDisplayed()
            assertFullyInside(control, panel.boundsInRoot)
        }
    }

    private fun assertFullyInside(
        node: SemanticsNodeInteraction,
        viewport: androidx.compose.ui.geometry.Rect,
    ) {
        val target = node.fetchSemanticsNode()
        val origin = target.positionInRoot
        Assert.assertTrue(
            "Complete content must fit its actual viewport: $origin / ${target.size} in $viewport",
            origin.x >= viewport.left - 1f &&
                origin.y >= viewport.top - 1f &&
                origin.x + target.size.width <= viewport.right + 1f &&
                origin.y + target.size.height <= viewport.bottom + 1f,
        )
    }

    private fun openRefinementRecording() {
        compose.onNode(hasText("Run now") and hasClickAction()).performScrollTo().performClick()
        await("Custom workout")
        compose.onNodeWithText("Free run").performScrollTo().performClick()
        await("Elapsed time")
    }

    private fun clearRefinementLiveState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            org.racedaydreams.recording.RunRecordingService.status.value = null
            org.racedaydreams.recording.RunRecordingService.workoutState.value = null
            org.racedaydreams.recording.RunRecordingService.heartRate.value = null
        }
    }

    private fun assertOriginalBottomNavigationOrder() {
        val role =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.Tab,
            )
        val tabs =
            listOf("Home", "Plan", "Calendar", "Progress", "Strength").map { label ->
                compose
                    .onNode(role and (hasText(label) or hasContentDescription(label)))
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
            }
        Assert.assertTrue(
            "Established bottom destinations retain their order",
            tabs.zipWithNext().all { (a, b) -> a.boundsInRoot.center.x < b.boundsInRoot.center.x },
        )
        Assert.assertTrue(tabs.first().config[SemanticsProperties.Selected])
    }

    @Test
    fun capturesCompactLibraryFiltersAndRealDetailTabsInAllModes() {
        val originalWidth = context.resources.configuration.screenWidthDp
        val originalSize =
            Regex("Override size: ([0-9]+x[0-9]+)").find(shell("wm size"))?.groupValues?.get(1)
        val density = context.resources.displayMetrics.density
        try {
            withRefinementCaptureModes { suffix ->
                if (suffix == "-large-font") {
                    closeScenario()
                    scenario = null
                    shell(
                        "wm size ${kotlin.math.round(320f * density).toInt()}x${kotlin.math.round(720f * density).toInt()}"
                    )
                    scenario = ActivityScenario.launch(CoachActivity::class.java)
                    awaitHomeReady(false)
                    compose.waitUntil(20000) {
                        actualMaterialWindow()?.let {
                            it.widthDp in 319..321 &&
                                it.decorWidthDp in 319f..321f &&
                                it.fontScale >= 1.99f &&
                                it.focused
                        } == true
                    }
                    awaitMaterialSystemBars(false, "library-narrow200")
                }
                clickOriginalNavigationTab("Strength")
                compose.onNodeWithText("Exercises").performScrollTo().performClick()
                awaitExerciseSearch()
                capture("exercise-library-refined$suffix")
                compose.onNodeWithTag("exercise-filters").performScrollTo().performClick()
                compose.onNodeWithTag("exercise-browse-Body area").performScrollTo().performClick()
                compose.onNodeWithTag("exercise-area-CHEST").performScrollTo().performClick()
                capture("exercise-filters-refined$suffix")
                compose
                    .onNodeWithTag("exercise-search")
                    .performScrollTo()
                    .performTextReplacement("Bench Press")
                androidx.test.espresso.Espresso.closeSoftKeyboard()
                compose
                    .onNodeWithTag("exercise-browser-list")
                    .performScrollToNode(hasTestTag("exercise-row-bench-press"))
                compose.waitUntil(20000) {
                    compose
                        .onAllNodesWithTag(
                            "exercise-thumbnail-ready-bench-press",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                capture("exercise-library-rows-refined$suffix")
                compose.onNodeWithTag("exercise-row-bench-press").performClick()
                await("Overview")
                capture("exercise-detail-tabs-refined$suffix")
            }
        } finally {
            closeScenario()
            scenario = null
            shell("wm size ${originalSize ?: "reset"}")
            compose.waitUntil(20000) {
                context.resources.configuration.screenWidthDp == originalWidth
            }
        }
    }

    @Test
    fun capturesPlainBackupExplanationInAllModes() {
        withRefinementCaptureModes { suffix ->
            click("Settings")
            await("Local backup and restore")
            alignHeading("Local backup and restore")
            capture("settings-backup-plain$suffix")
            compose.onNodeWithText("Choose backup to restore").performScrollTo().assertIsDisplayed()
            compose
                .onNodeWithText(
                    "Restore validates the file before replacing local app data. Merge restore is not currently supported."
                )
                .performScrollTo()
                .assertIsDisplayed()
            capture("settings-backup-actions-plain$suffix")
        }
    }

    @Test
    fun capturesPlainHealthAndRunningInstructionsInAllModes() {
        withRefinementCaptureModes { suffix ->
            click("Settings")
            compose.onNodeWithText("Personal health context").performScrollTo().performClick()
            await("Heart-rate variability")
            alignHeading("Heart-rate variability")
            capture("health-context-plain$suffix")
            compose
                .onNodeWithText("About this measurement")
                .performScrollTo()
                .assert(
                    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed")
                )
                .performClick()
            compose
                .onNode(hasText("HRV means heart-rate variability.", substring = true))
                .performScrollTo()
                .assertIsDisplayed()
            capture("health-measurement-plain$suffix")
            back()
            back()
            awaitHomeReady(false)
            clickOriginalNavigationTab("Plan")
            await("This week")
            compose
                .onAllNodesWithContentDescription("View workout and purpose")[0]
                .performScrollTo()
                .performClick()
            await("Workout structure")
            capture("running-task-plain$suffix")
            compose.onAllNodesWithText("Exact effort target")[0].performScrollTo().performClick()
            compose
                .onNode(hasText("This is the effort target saved in this plan.", substring = true))
                .performScrollTo()
                .assertIsDisplayed()
            capture("running-target-plain$suffix")
        }
    }

    private fun awaitExerciseSearch() {
        compose.waitUntil(20000) {
            compose.onAllNodesWithTag("exercise-search").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun withRefinementCaptureModes(block: (String) -> Unit) {
        val prefs =
            context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
        val dynamic = prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true)
        val font = shell("settings get system font_scale").trim()
        val night =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            for ((suffix, useDynamic, uiNight, scale) in
                listOf(
                    MaterialCapture("", true, "no", 1f),
                    MaterialCapture("-dark", true, "yes", 1f),
                    MaterialCapture("-large-font", false, "no", 2f),
                )) {
                closeScenario()
                scenario = null
                clearRefinementLiveState()
                prefs
                    .edit()
                    .putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, useDynamic)
                    .commit()
                shell("cmd uimode night $uiNight")
                shell("settings put system font_scale $scale")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                awaitMaterialSystemBars(uiNight == "yes")
                block(suffix)
            }
        } finally {
            closeScenario()
            scenario = null
            clearRefinementLiveState()
            prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, dynamic).commit()
            restoreFontScale(font)
            shell("cmd uimode night $night")
        }
    }

    @Test
    fun capturesMaterialDeviceAndBrandColoursWithAccessibleLiveControls() {
        val prefs =
            context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
        val originalDynamic = prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true)
        val originalFont = shell("settings get system font_scale").trim()
        val originalNight =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            for ((suffix, dynamic, night, scale) in
                listOf(
                    MaterialCapture("brand-light", false, "no", 1f),
                    MaterialCapture("device-light", true, "no", 1f),
                    MaterialCapture("brand-dark", false, "yes", 1f),
                    MaterialCapture("device-dark", true, "yes", 1f),
                    MaterialCapture("brand-large-font", false, "no", 2f),
                    MaterialCapture("device-large-font", true, "yes", 2f),
                )) {
                closeScenario()
                scenario = null
                prefs
                    .edit()
                    .putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, dynamic)
                    .commit()
                shell("cmd uimode night $night")
                shell("settings put system font_scale $scale")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                if (suffix == "brand-light" && android.os.Build.VERSION.SDK_INT >= 31) {
                    // Exercise the real preference control, not only fixture writes.
                    click("Settings")
                    await("Appearance")
                    val deviceColours = compose.onNodeWithText("Use device colours")
                    deviceColours.performScrollTo().performClick()
                    compose.waitUntil(20000) {
                        prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, false)
                    }
                    deviceColours.performClick()
                    compose.waitUntil(20000) {
                        !prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true)
                    }
                    back()
                    awaitHomeReady(false)
                }
                awaitMaterialSystemBars(dark = night == "yes")
                capture("home-material-$suffix")
                compose
                    .onNode(hasText("Run now") and hasClickAction())
                    .performScrollTo()
                    .performClick()
                await("Custom workout")
                compose.onNodeWithText("Free run").performScrollTo().performClick()
                await("Start GPS recording")
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    org.racedaydreams.recording.RunRecordingService.status.value =
                        org.racedaydreams.domain.recording.RunMetricsEngine.Status(
                            5020.0,
                            1694000,
                            1694000,
                            337.0,
                            "GPS ready",
                            false,
                        )
                }
                await("Distance")
                compose.onNodeWithText("Pause").assertIsDisplayed()
                compose.onNodeWithText("Finish & save").assertIsDisplayed()
                capture("live-material-$suffix")
                if (scale == 2f) {
                    compose.onNodeWithText("Finish & save").performClick()
                    await("Finish this run?")
                    capture("live-material-finish-$suffix")
                    compose.onNodeWithText("Keep recording").performClick()
                }
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    org.racedaydreams.recording.RunRecordingService.status.value = null
                }
            }
        } finally {
            closeScenario()
            scenario = null
            prefs
                .edit()
                .putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, originalDynamic)
                .commit()
            restoreFontScale(originalFont)
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun capturesRetainedGoalFormInDarkAndLargeText() {
        val prefs =
            context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
        val dynamic = prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true)
        val font = shell("settings get system font_scale").trim()
        val night =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        try {
            for (mode in
                listOf(
                    MaterialCapture("dark", false, "yes", 1f),
                    MaterialCapture("large-font", false, "no", 2f),
                )) {
                closeScenario()
                scenario = null
                prefs
                    .edit()
                    .putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, false)
                    .commit()
                shell("cmd uimode night ${mode.night}")
                shell("settings put system font_scale ${mode.scale}")
                scenario = ActivityScenario.launch(CoachActivity::class.java)
                awaitHomeReady(false)
                click("Settings")
                await("Activity data source")
                compose
                    .onNodeWithText("Edit race goal and availability")
                    .performScrollTo()
                    .performClick()
                androidx.test.espresso.Espresso.onView(
                        androidx.test.espresso.matcher.ViewMatchers.withText(
                            org.racedaydreams.R.string.onboarding_edit_title
                        )
                    )
                    .check(
                        androidx.test.espresso.assertion.ViewAssertions.matches(
                            androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
                        )
                    )
                capture("onboarding-material-${mode.suffix}")
            }
        } finally {
            closeScenario()
            scenario = null
            prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, dynamic).commit()
            restoreFontScale(font)
            shell("cmd uimode night $night")
        }
    }

    private data class MaterialCapture(
        val suffix: String,
        val dynamic: Boolean,
        val night: String,
        val scale: Float,
    )

    @Test
    fun capturesPublicPermissionRationaleInLightDarkAndLargeText() {
        val prefs =
            context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
        val dynamic = prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true)
        val font = shell("settings get system font_scale").trim()
        val night =
            shell("cmd uimode night").let {
                if (it.contains("yes")) "yes" else if (it.contains("auto")) "auto" else "no"
            }
        val heading = "Your health data stays on this device"
        try {
            prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, false).commit()
            for (mode in
                listOf(
                    MaterialCapture("light", false, "no", 1f),
                    MaterialCapture("dark", false, "yes", 1f),
                    MaterialCapture("large-font", false, "no", 2f),
                )) {
                shell("cmd uimode night ${mode.night}")
                shell("settings put system font_scale ${mode.scale}")
                val native =
                    ActivityScenario.launch(
                        org.racedaydreams.health.HealthPermissionRationaleActivity::class.java
                    )
                try {
                    compose.waitUntil(20000) {
                        var ready = false
                        native.onActivity { activity ->
                            val dark =
                                activity.resources.configuration.uiMode and
                                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                            val bars =
                                androidx.core.view.WindowCompat.getInsetsController(
                                    activity.window,
                                    activity.window.decorView,
                                )
                            ready =
                                activity.window.decorView.hasWindowFocus() &&
                                    dark == (mode.night == "yes") &&
                                    activity.resources.configuration.fontScale >=
                                        mode.scale - .01f &&
                                    bars.isAppearanceLightStatusBars == !dark &&
                                    bars.isAppearanceLightNavigationBars == !dark
                        }
                        ready
                    }
                    // A native Activity must be checked through its real View hierarchy.
                    androidx.test.espresso.Espresso.onView(
                            androidx.test.espresso.matcher.ViewMatchers.withText(heading)
                        )
                        .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                        .check(
                            androidx.test.espresso.assertion.ViewAssertions.matches(
                                androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
                            )
                        )
                    var nativeSurface = 0
                    native.onActivity { activity ->
                        val root =
                            activity
                                .findViewById<android.view.ViewGroup>(android.R.id.content)
                                .getChildAt(0)
                        val surface =
                            (root.background as android.graphics.drawable.ColorDrawable).color
                        nativeSurface = surface
                        if (android.os.Build.VERSION.SDK_INT < 35) {
                            Assert.assertEquals(
                                "Native navigation background must match its page",
                                surface,
                                activity.window.navigationBarColor,
                            )
                            Assert.assertEquals(
                                "Native status background must match its page",
                                surface,
                                activity.window.statusBarColor,
                            )
                        }
                    }
                    captureNativePermission("permission-rationale-${mode.suffix}", nativeSurface)
                    val close =
                        androidx.test.espresso.Espresso.onView(
                            androidx.test.espresso.matcher.ViewMatchers.withText("Close")
                        )
                    close
                        .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                        .check(
                            androidx.test.espresso.assertion.ViewAssertions.matches(
                                androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed()
                            )
                        )
                        .check { view, missing ->
                            if (missing != null) throw missing
                            val button = checkNotNull(view)
                            val visible = android.graphics.Rect()
                            Assert.assertTrue(button.getGlobalVisibleRect(visible))
                            Assert.assertEquals(button.height, visible.height())
                            Assert.assertEquals(button.width, visible.width())
                            android.util.Log.i(
                                "RddGate",
                                "NATIVE_PERMISSION_CLOSE ${mode.suffix} full=$visible button=${button.width}x${button.height}",
                            )
                        }
                    if (mode.scale == 2f)
                        captureNativePermission(
                            "permission-rationale-actions-large-font",
                            nativeSurface,
                        )
                    close.perform(androidx.test.espresso.action.ViewActions.click())
                    compose.waitUntil(20000) { native.state == Lifecycle.State.DESTROYED }
                } finally {
                    native.close()
                }
            }
        } finally {
            prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, dynamic).commit()
            restoreFontScale(font)
            shell("cmd uimode night $night")
        }
    }

    private fun captureNativePermission(name: String, expectedSurface: Int) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(200, 20000)
        if (android.os.Build.VERSION.SDK_INT == 28) {
            // Window flags settle before Android9 SystemUI necessarily renders new icons.
            // Await the real contrast condition, not a fixed delay or an edited screenshot.
            compose.waitUntil(20000) {
                saveCurrentScreen(name)
                nativeStatusBarReady(name, expectedSurface)
            }
        } else saveCurrentScreen(name)
    }

    private fun nativeStatusBarReady(name: String, expectedSurface: Int): Boolean {
        val frame =
            checkNotNull(
                android.graphics.BitmapFactory.decodeFile(
                    File(context.getExternalFilesDir(null), "screenshots/$name.png").absolutePath
                )
            )
        try {
            val logical = android.graphics.Point()
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                .defaultDisplay
                .getRealSize(logical)
            val scale = minOf(frame.width.toFloat() / logical.x, frame.height.toFloat() / logical.y)
            val left = ((frame.width - logical.x * scale) / 2).toInt()
            val top = ((frame.height - logical.y * scale) / 2).toInt()
            val right = frame.width - left
            val statusId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            Assert.assertTrue("A real status-bar band is required", statusId != 0)
            val bottom = top + (context.resources.getDimensionPixelSize(statusId) * scale).toInt()
            val background = frame.getPixel(frame.width / 2, top + 2)
            for (channel in
                listOf<(Int) -> Int>(
                    android.graphics.Color::red,
                    android.graphics.Color::green,
                    android.graphics.Color::blue,
                )) {
                if (kotlin.math.abs(channel(background) - channel(expectedSurface)) > 18)
                    return false
            }
            var legiblePixels = 0
            for (y in top + 2 until bottom - 2) for (x in left + 2 until right - 2) {
                if (
                    androidx.core.graphics.ColorUtils.calculateContrast(
                        frame.getPixel(x, y),
                        expectedSurface,
                    ) >= 3.0
                )
                    legiblePixels++
            }
            if (legiblePixels < 20)
                android.util.Log.i(
                    "RddGate",
                    "NATIVE_STATUS_NOT_READY $name foreground=$legiblePixels",
                )
            return legiblePixels >= 20
        } finally {
            frame.recycle()
        }
    }

    @Test
    fun capturesWideWindowNavigationAndBoundedContent() {
        val originalFont = shell("settings get system font_scale").trim()
        val originalWidth = context.resources.configuration.screenWidthDp
        val size = shell("wm size")
        val originalOverride =
            Regex("Override size: ([0-9]+x[0-9]+)").find(size)?.groupValues?.get(1)
        try {
            shell("wm size 1400x1800")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            awaitHomeReady(false)
            awaitMaterialWideWindow()
            compose.onNodeWithText("Calendar").assertIsDisplayed()
            capture("home-material-wide")
            click("Plan")
            await("This week")
            capture("plan-material-wide")
            closeScenario()
            scenario = null
            shell("settings put system font_scale 2.0")
            shell("wm size 1400x720")
            scenario = ActivityScenario.launch(CoachActivity::class.java)
            awaitHomeReady(false)
            awaitMaterialWideWindow(minimumFontScale = 1.99f)
            // Every destination remains reachable when enlarged labels exceed the rail viewport.
            compose.onNodeWithText("Strength").performScrollTo().assertIsDisplayed().performClick()
            await("Log workout")
            compose.onNodeWithText("Home").performScrollTo().performClick()
            awaitHomeReady(false)
            capture("home-material-wide-large-font")
            compose.onNodeWithText("Plan").performScrollTo().performClick()
            await("This week")
            capture("plan-material-wide-large-font")
        } finally {
            closeScenario()
            scenario = null
            restoreFontScale(originalFont)
            shell("wm size ${originalOverride ?: "reset"}")
            compose.waitUntil(20000) {
                context.resources.configuration.screenWidthDp == originalWidth
            }
        }
    }

    private data class MaterialWindowSnapshot(
        val widthDp: Int,
        val heightDp: Int,
        val fontScale: Float,
        val density: Float,
        val decorWidth: Int,
        val decorHeight: Int,
        val dark: Boolean,
        val lightStatusBarAppearance: Boolean,
        val lightNavigationBarAppearance: Boolean,
        val focused: Boolean,
        val navigationInsets: String,
        val legacyVisibility: Int,
        val windowFlags: Int,
        val imeVisible: Boolean?,
        val navigationContrastEnforced: Boolean?,
    ) {
        val decorWidthDp: Float
            get() = if (density.isFinite() && density > 0f) decorWidth / density else 0f
    }

    private fun actualMaterialWindow(): MaterialWindowSnapshot? {
        var snapshot: MaterialWindowSnapshot? = null
        scenario?.onActivity { activity ->
            val configuration = activity.resources.configuration
            val decor = activity.window.decorView
            val bars = androidx.core.view.WindowCompat.getInsetsController(activity.window, decor)
            val insets = androidx.core.view.ViewCompat.getRootWindowInsets(decor)
            val navigation =
                insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            snapshot =
                MaterialWindowSnapshot(
                    configuration.screenWidthDp,
                    configuration.screenHeightDp,
                    configuration.fontScale,
                    activity.resources.displayMetrics.density,
                    decor.width,
                    decor.height,
                    configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES,
                    bars.isAppearanceLightStatusBars,
                    bars.isAppearanceLightNavigationBars,
                    decor.hasWindowFocus(),
                    navigation.toString(),
                    decor.systemUiVisibility,
                    activity.window.attributes.flags,
                    insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()),
                    if (android.os.Build.VERSION.SDK_INT >= 29)
                        activity.window.isNavigationBarContrastEnforced
                    else null,
                )
        }
        return snapshot
    }

    private fun awaitMaterialSystemBars(dark: Boolean, point: String = "home") {
        var last: MaterialWindowSnapshot? = null
        try {
            compose.waitUntil(20000) {
                last = actualMaterialWindow()
                last?.let {
                    it.dark == dark &&
                        it.focused &&
                        it.navigationContrastEnforced != true &&
                        it.lightStatusBarAppearance == !dark &&
                        it.lightNavigationBarAppearance == !dark
                } == true
            }
            android.util.Log.i("RddGate", "MATERIAL_SYSTEM_BARS_READY $point $last")
        } catch (failure: Throwable) {
            android.util.Log.i(
                "RddGate",
                "MATERIAL_SYSTEM_BARS_FAILURE $point expectedDark=$dark actual=$last",
            )
            throw failure
        }
    }

    private fun awaitMaterialWideWindow(minimumFontScale: Float? = null) {
        var last: MaterialWindowSnapshot? = null
        try {
            compose.waitUntil(20000) {
                last = actualMaterialWindow()
                last?.let { window ->
                    window.widthDp >= 840 &&
                        window.heightDp >= 480 &&
                        window.decorWidthDp >= 840f &&
                        window.decorHeight > 0 &&
                        (minimumFontScale == null || window.fontScale >= minimumFontScale)
                } == true
            }
            android.util.Log.i("RddGate", "MATERIAL_WINDOW_READY $last")
        } catch (failure: Throwable) {
            // Production uses this Activity's LocalConfiguration, not the application context.
            // Keep the original failure and expose public window/configuration values only.
            val app = context.resources.configuration
            android.util.Log.i(
                "RddGate",
                "MATERIAL_WINDOW_FAILURE expectedWidthDp>=840 expectedHeightDp>=480 " +
                    "minimumFontScale=$minimumFontScale actual=$last " +
                    "app=${app.screenWidthDp}x${app.screenHeightDp} font=${app.fontScale}",
            )
            runCatching {
                android.util.Log.i(
                    "RddGate",
                    "MATERIAL_WINDOW_DISPLAY size=${shell("wm size").replace('\n', ' ').take(180)} " +
                        "density=${shell("wm density").replace('\n', ' ').take(180)}",
                )
            }
            throw failure
        }
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
        closeScenario()
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

    private fun restoreFontScale(original: String) {
        // An unset value means the normal scale, not a literal "null" to write. Deleting
        // the setting after a large-text capture is not a portable configuration reset:
        // API35 was observed retaining/re-persisting 2.0 after that delete. Write the
        // explicit effective default and still await the actual application configuration.
        val scale = original.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: 1f
        shell("settings put system font_scale $scale")
    }

    private fun shell(command: String): String {
        val expectedScale =
            when {
                command.startsWith("settings put system font_scale ") ->
                    command.substringAfterLast(' ').toFloat()
                command == "settings delete system font_scale" -> 1f
                else -> null
            }
        val alreadyApplied =
            expectedScale == null ||
                kotlin.math.abs(context.resources.configuration.fontScale - expectedScale) < .01f
        val acknowledged = java.util.concurrent.atomic.AtomicBoolean(alreadyApplied)
        val receiver =
            if (expectedScale != null && !alreadyApplied)
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(
                        receivingContext: Context,
                        intent: android.content.Intent,
                    ) {
                        if (
                            intent.action == android.content.Intent.ACTION_CONFIGURATION_CHANGED &&
                                kotlin.math.abs(
                                    context.resources.configuration.fontScale - expectedScale
                                ) < .01f
                        ) {
                            acknowledged.set(true)
                        }
                    }
                }
            else null
        if (receiver != null) {
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                receiver,
                android.content.IntentFilter(android.content.Intent.ACTION_CONFIGURATION_CHANGED),
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
            )
        }
        try {
            val output =
                InstrumentationRegistry.getInstrumentation()
                    .uiAutomation
                    .executeShellCommand(command)
                    .use { descriptor ->
                        java.io.FileInputStream(descriptor.fileDescriptor).use {
                            it.readBytes().toString(Charsets.UTF_8)
                        }
                    }
            if (expectedScale != null) {
                // Android sends the app's configuration callback before its queued persistent
                // settings write. A rapid next write can otherwise be overwritten by that old
                // configuration. The system configuration broadcast follows the persistence
                // message (AMS on28, ATMS on35); await it AND real settings/resources.
                try {
                    compose.waitUntil(20000) {
                        acknowledged.get() &&
                            kotlin.math.abs(
                                context.resources.configuration.fontScale - expectedScale
                            ) < .01f &&
                            kotlin.math.abs(
                                android.provider.Settings.System.getFloat(
                                    context.contentResolver,
                                    android.provider.Settings.System.FONT_SCALE,
                                    1f,
                                ) - expectedScale
                            ) < .01f
                    }
                } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
                    throw AssertionError(
                        "Font configuration after $command: expected $expectedScale, actual ${context.resources.configuration.fontScale}, stored ${shell("settings get system font_scale").trim()}, broadcast ${acknowledged.get()}",
                        failure,
                    )
                }
                Assert.assertEquals(
                    "Applied font scale after $command",
                    expectedScale,
                    context.resources.configuration.fontScale,
                    .01f,
                )
            }
            return output
        } finally {
            if (receiver != null) context.unregisterReceiver(receiver)
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
                    name.startsWith("live-material-finish-") ||
                    name.startsWith("storage-confirmation") ||
                    name.startsWith("strength-programme-alternatives") ||
                    name.startsWith("strength-programme-progression") ||
                    name.startsWith("strength-programme-history")
            ) {
                // Production dialogs and their covered activities are separate roots. Check the
                // visible dialog, including the storage confirmation, rather than an ambiguous
                // root.
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
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (name.contains("-material-") && !name.startsWith("live-material-finish-")) {
            val window = checkNotNull(actualMaterialWindow())
            awaitMaterialSystemBars(window.dark, name)
        }
        // The app looper is only one window. Await the real system-window event stream too,
        // rather than assuming a fixed delay means SystemUI finished a configuration update.
        automation.waitForIdle(200, 20000)
        saveCurrentScreen(name)
    }

    private fun saveCurrentScreen(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val dir = File(context.getExternalFilesDir(null), "screenshots")
        dir.mkdirs()
        val image = File(dir, "$name.png")
        if (android.os.Build.VERSION.SDK_INT == 28) {
            // Android9 UiAutomation crops to getRealSize(), which is the overridden logical
            // size. On a letterboxed emulator this clips the physical compositor frame.
            // Native screencap requests the entire physical frame. Preserve it unchanged,
            // including real letterboxing; never rescale, crop or repaint the captured UI.
            automation.executeShellCommand("screencap -p").use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { input ->
                    image.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val bounds =
                android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(image.absolutePath, bounds)
            val physical =
                checkNotNull(Regex("Physical size: ([0-9]+)x([0-9]+)").find(shell("wm size")))
            val width = physical.groupValues[1].toInt()
            val height = physical.groupValues[2].toInt()
            Assert.assertTrue(
                "API28 capture must contain the whole physical display, not an overridden crop: ${bounds.outWidth}x${bounds.outHeight}, physical ${width}x$height",
                (bounds.outWidth == width && bounds.outHeight == height) ||
                    (bounds.outWidth == height && bounds.outHeight == width),
            )
            android.util.Log.i(
                "RddGate",
                "CAPTURE_PHYSICAL_FRAME $name ${bounds.outWidth}x${bounds.outHeight}",
            )
        } else {
            val bitmap = checkNotNull(automation.takeScreenshot())
            try {
                image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally {
                bitmap.recycle()
            }
        }
        // AGP uninstalls the APK after connected tests, removing app-specific files.
        // Copy with instrumentation's shell identity before teardown; production has no such path.
        val shell = InstrumentationRegistry.getInstrumentation().uiAutomation
        shell.executeShellCommand("mkdir -p /data/local/tmp/racedaydreams-screenshots").use {
            descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
        shell
            .executeShellCommand(
                "cp ${image.absolutePath} /data/local/tmp/racedaydreams-screenshots/$name.png"
            )
            .use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
    }

    private fun awaitHomeReady(
        strength: Boolean,
        expectedAction: String = if (strength) "Log today's workout" else "Run now",
    ) {
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
                    .onAllNodes(hasText(expectedAction) and hasClickAction())
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose
            .onNodeWithText(if (strength) "Your strength training" else "How am I doing?")
            .assertExists()
    }
}
