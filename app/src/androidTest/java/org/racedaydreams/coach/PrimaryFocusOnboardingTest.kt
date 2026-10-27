package org.racedaydreams.coach

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.EnumSet
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.R
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.SqlitePrimaryFocusRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.GoalMode
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.RaceGoal
import org.racedaydreams.domain.ScheduleType
import org.racedaydreams.domain.TrainingPreferences
import org.racedaydreams.domain.TrainingStrategy
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.domain.coaching.OriginalTrainingPlanGenerator
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymProgrammeGenerator
import org.racedaydreams.reminders.EveningReminders

/** Exercises the real entry, settings and navigation paths without creating a race profile. */
@RunWith(AndroidJUnit4::class)
class PrimaryFocusOnboardingTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private var scenario: ActivityScenario<CoachActivity>? = null

    @Before
    fun clearFixture() {
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
    }

    @After
    fun closeFixture() {
        scenario?.close()
        scenario = null
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
    }

    @Test
    fun strengthEntryCreatesAProgrammeWithoutRaceAndSurvivesRotationAndRelaunch() {
        launch()
        await("Make today yours")
        compose.onNodeWithText("Focus on strength").performScrollTo().performClick()
        awaitFocus(PrimaryFocus.STRENGTH)
        await("Create a gym programme")
        assertFalse(SqliteUserPreferencesRepository(context).load().isPresent)
        assertNull(CoachingStore(context).current())
        compose.onNodeWithText("Create a gym programme").performScrollTo().performClick()
        compose.onNodeWithText("Mon").performScrollTo().performClick()
        compose.onNodeWithText("Thu").performScrollTo().performClick()

        scenario!!.recreate()
        await("Your gym preferences")
        compose.onNodeWithText("Mon").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Thu").performScrollTo().assertIsSelected()
        compose.onNodeWithText("General strength & fitness").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Strength first").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Preview six-week block").performScrollTo().performClick()
        await("Preview — not applied")
        assertNull(GymProgrammeStore(context).current())
        compose.onNodeWithText("Accept this programme").performScrollTo().performClick()
        awaitCondition("accepted gym programme to be persisted") {
            GymProgrammeStore(context).current() != null
        }
        await("Change programme settings")
        val programme = GymProgrammeStore(context).current()!!
        assertEquals(GymProgramme.Goal.GENERAL, programme.settings().goal())
        assertEquals(GymProgramme.Priority.STRENGTH_FIRST, programme.settings().priority())
        assertEquals(12, programme.sessions().size)
        assertFalse(SqliteUserPreferencesRepository(context).load().isPresent)
        assertNull(CoachingStore(context).current())

        scenario!!.close()
        scenario = null
        launch()
        await("Your strength training")
        compose.onNodeWithText("Plan").performClick()
        await("Change programme settings")
        compose.onNodeWithText("Gym programme").performScrollTo().assertIsDisplayed()
        assertEquals(programme, GymProgrammeStore(context).current())
        assertFalse(SqliteUserPreferencesRepository(context).load().isPresent)
        assertNull(CoachingStore(context).current())
    }

    @Test
    fun settingsFocusChoicesRetainBothProgrammesTheRaceProfileAndUnfinishedDraft() {
        val profile = profile()
        val profiles = SqliteUserPreferencesRepository(context)
        profiles.save(profile)
        val draft = raceDraft()
        profiles.saveDraft(draft)
        val store = FeatureStore(context)
        store.putSetting("starting.path", "race")
        val now = Instant.now()
        val generated =
            OriginalTrainingPlanGenerator()
                .generate(
                    profile,
                    RunnerBaseline.empty(now.minus(Duration.ofDays(84)), now),
                    LocalDate.now(),
                )
        val running =
            PlanVersion(
                "focus-running-fixture",
                generated.id(),
                1,
                now,
                "Explicit test-only running programme",
                profile,
                "Test-only empty baseline",
                generated,
            )
        CoachingStore(context).saveCurrent(running)
        val gym = programme()
        GymProgrammeStore(context).save(gym)
        launch()
        await("Today")
        compose.onNodeWithText("⚙ Settings").performClick()
        await("Primary focus")
        for (focus in listOf(PrimaryFocus.STRENGTH, PrimaryFocus.HYBRID, PrimaryFocus.RUNNING)) {
            compose
                .onNodeWithTag("primary-focus-choice-${focus.storageValue()}")
                .performScrollTo()
                .performClick()
            compose.onNodeWithText("Save primary focus").performScrollTo().performClick()
            awaitFocus(focus)
            val label =
                when (focus) {
                    PrimaryFocus.RUNNING -> "Running"
                    PrimaryFocus.STRENGTH -> "Strength"
                    PrimaryFocus.HYBRID -> "Hybrid"
                }
            compose
                .onNodeWithText("Current primary focus: $label")
                .performScrollTo()
                .assertIsDisplayed()
            compose
                .onNodeWithTag("primary-focus-choice-${focus.storageValue()}")
                .performScrollTo()
                .assertIsSelected()
            assertEquals(profile, profiles.load().get())
            assertEquals(draft, profiles.loadDraft().get())
            assertEquals(running, CoachingStore(context).current())
            assertEquals(gym, GymProgrammeStore(context).current())
            assertEquals(listOf(gym), GymProgrammeStore(context).versions())
            assertEquals("race", store.getSetting("starting.path"))
        }

        scenario!!.recreate()
        await("Primary focus")
        compose.onNodeWithTag("primary-focus-choice-running").performScrollTo().assertIsSelected()
        compose
            .onNodeWithText("Edit race goal and availability")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(draft, profiles.loadDraft().get())
    }

    @Test
    fun legacyRacePathResumesItsUnfinishedGoalWithoutForcingGymSetup() {
        val store = FeatureStore(context)
        store.putSetting("starting.path", "race")
        store.putSetting("baseline.reviewed", "true")
        SqliteUserPreferencesRepository(context).saveDraft(raceDraft())
        launch()
        awaitDraftTarget()
        onView(withHint(R.string.onboarding_target_hint)).check(matches(withText("1:")))
        assertEquals(PrimaryFocus.RUNNING, SqlitePrimaryFocusRepository(context).load().get())
        assertNull(store.getSetting("primary.focus"))
        assertFalse(SqliteUserPreferencesRepository(context).load().isPresent)
        assertNull(GymProgrammeStore(context).current())

        scenario!!.recreate()
        awaitDraftTarget()
        onView(withHint(R.string.onboarding_target_hint)).check(matches(withText("1:")))
        assertNull(GymProgrammeStore(context).current())
        assertEquals("race", store.getSetting("starting.path"))
    }

    private fun launch() {
        scenario = ActivityScenario.launch(CoachActivity::class.java)
    }

    private fun await(text: String) {
        awaitCondition("Compose text '$text'") {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitFocus(focus: PrimaryFocus) {
        var message: String? = null
        awaitCondition("published and persisted focus $focus") {
            var published = false
            scenario!!.onActivity { activity ->
                val state = ViewModelProvider(activity)[CoachViewModel::class.java].state.value
                published = !state.busy && state.primaryFocus == focus
                message = state.message
            }
            published && SqlitePrimaryFocusRepository(context).load().orElse(null) == focus
        }
        assertNull(
            "Focus saves use persistent status without a message covering the next action.",
            message,
        )
    }

    private fun awaitDraftTarget() {
        awaitCondition("unfinished native race target") {
            var shown = false
            scenario!!.onActivity { activity -> shown = hasDraftTarget(activity.window.decorView) }
            shown
        }
    }

    private fun awaitCondition(description: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(20_000, condition)
        } catch (failure: AssertionError) {
            throw AssertionError("While waiting for $description\n${failureDiagnostics()}", failure)
        } catch (failure: Exception) {
            throw AssertionError("While waiting for $description\n${failureDiagnostics()}", failure)
        }
    }

    /** Only synthetic instrumentation fixtures are reported; production code does not log state. */
    private fun failureDiagnostics(): String {
        val observations = StringBuilder()
        runCatching {
                scenario?.onActivity { activity ->
                    val state = ViewModelProvider(activity)[CoachViewModel::class.java].state.value
                    observations
                        .append("Published state: loading=${state.loading}, busy=${state.busy}, ")
                        .append("focus=${state.primaryFocus}, startingPath=${state.startingPath}, ")
                        .append(
                            "profilePresent=${state.profile != null}, gym=${state.gymProgramme?.id()}, "
                        )
                        .append(
                            "draftPresent=${state.draft != null}, message=${state.message}, error=${state.error}\n"
                        )
                }
            }
            .onFailure { observations.append("Published state unavailable: ${it.message}\n") }
        runCatching {
                val store = FeatureStore(context)
                observations
                    .append("Database: primary.focus=${store.getSetting("primary.focus")}, ")
                    .append("starting.path=${store.getSetting("starting.path")}, ")
                    .append("resolvedFocus=${SqlitePrimaryFocusRepository(context).load()}, ")
                    .append("gym=${GymProgrammeStore(context).current()?.id()}\n")
            }
            .onFailure { observations.append("Database observations unavailable: ${it.message}\n") }
        val semantics =
            runCatching { compose.onRoot(useUnmergedTree = true).printToString(maxDepth = 12) }
                .getOrElse { "Compose hierarchy unavailable: ${it.message}" }
        return "$observations\nCompose hierarchy:\n$semantics"
    }

    private fun hasDraftTarget(view: View): Boolean {
        if (
            view is EditText &&
                view.isShown &&
                view.hint?.toString() == context.getString(R.string.onboarding_target_hint) &&
                view.text.toString() == "1:"
        )
            return true
        if (view is ViewGroup)
            for (index in 0 until view.childCount) {
                if (hasDraftTarget(view.getChildAt(index))) return true
            }
        return false
    }

    private fun profile() =
        UserProfile(
            RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                TrainingStrategy.DISTANCE_FIRST,
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                3,
                DayOfWeek.SATURDAY,
                emptySet(),
                ScheduleType.FLEXIBLE,
                UnitSystem.METRIC,
            ),
            false,
        )

    private fun raceDraft(): String =
        JSONObject()
            .put("version", 1)
            .put("step", 1)
            .put("distance", "HALF_MARATHON")
            .put("event", false)
            .put("raceName", "Unfinished race goal")
            .put("raceDate", JSONObject.NULL)
            .put("goalMode", "TARGET_TIME")
            .put("targetTime", "1:")
            .put("strategy", "DISTANCE_FIRST")
            .put("runningDays", JSONArray(listOf("TUESDAY", "THURSDAY", "SATURDAY")))
            .put("runsPerWeek", 3)
            .put("longRunDay", "SATURDAY")
            .put("gymDays", JSONArray())
            .put("scheduleType", "FLEXIBLE")
            .put("units", "METRIC")
            .toString()

    private fun programme(): GymProgramme =
        GymProgrammeGenerator()
            .generate(
                "focus-gym-fixture",
                1,
                Instant.now(),
                "Explicit test-only gym programme",
                GymProgramme.Settings(
                    GymProgramme.Goal.GENERAL,
                    GymProgramme.Priority.STRENGTH_FIRST,
                    GymProgramme.Experience.BEGINNER,
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                    2,
                    setOf("bodyweight"),
                    45,
                    GymProgramme.Effort.RPE,
                ),
                LocalDate.now(),
                emptyList(),
                emptyList(),
            )
}
