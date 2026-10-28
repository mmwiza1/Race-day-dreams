package org.racedaydreams.coach

import android.content.Context
import android.graphics.Bitmap
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
import org.junit.runner.RunWith
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
class CoachingScreenshotTest {
    @get:Rule val compose = createEmptyComposeRule()
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
                    "GPS available",
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
            scenario?.close()
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
                    "City ${route.id()}, activity ${recorded.id()}, source ${recorded.source()}, retained points ${retainedPoints.size}, expected story $retainedStory. UI: ${compose.onRoot(useUnmergedTree=true).printToString().take(14000)}",
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

    private fun capture(name: String, anchor: String? = null) {
        if (
            name in
                setOf(
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
            if (name == "gym-equipment-alternatives") {
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
