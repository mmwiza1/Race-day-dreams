package org.racedaydreams.coach

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.R
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.reminders.EveningReminders

/** Tests the real Compose/retained-View boundary with isolated synthetic local fixtures. */
@RunWith(AndroidJUnit4::class)
class CoachOnboardingTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<CoachActivity>? = null

    @Before fun clearFixture() {
        // JUnit runs on the instrumentation thread, not the UI thread. No health provider is
        // queried or seeded, and the app remains usable regardless of provider availability.
        val store = FeatureStore(context)
        EveningReminders(context, store).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
    }

    @After fun closeFixture() {
        scenario?.close()
        scenario = null
        EveningReminders(context, FeatureStore(context)).cancelAllScheduled()
        SqliteUserPreferencesRepository(context).clear()
    }

    @Test fun canContinueWithoutHealthAndSaveGoalThroughExistingForm() {
        launch()
        continueToGoals()
        repeat(3) { nativeContinue() }
        waitNativeText(R.string.onboarding_save)
        onView(withText(R.string.onboarding_save)).perform(scrollTo(), click())
        waitForText("Review running baseline")
        val profile = SqliteUserPreferencesRepository(context).load().get()
        assertEquals(RaceDistance.FIVE_K, profile.raceGoal().distance())
        assertEquals(3, profile.preferences().runsPerWeek())

        scenario!!.recreate()
        waitForText("Review running baseline")
        compose.onNodeWithText("5K").assertIsDisplayed()
        assertEquals(profile, SqliteUserPreferencesRepository(context).load().get())
    }

    @Test fun optionalManualBaselineSurvivesRecreationAndKeepsItsProvenance() {
        launch()
        compose.onNodeWithText("Fill gaps or correct this baseline").performScrollTo().performClick()
        waitForText("Optional current-running details")
        field("Current weekly distance (km)").performScrollTo().performTextInput("24.5")
        field("Current usual runs/week").performScrollTo().performTextInput("3")
        field("Longest recent run (km)").performScrollTo().performTextInput("10")

        scenario!!.recreate()
        waitForText("Optional current-running details")
        field("Current weekly distance (km)").assertTextContains("24.5")
        field("Current usual runs/week").assertTextContains("3")
        field("Longest recent run (km)").assertTextContains("10")
        compose.onNodeWithText("Save optional baseline").performScrollTo().performClick()
        waitForText("Start with the running you already do")
        compose.onNodeWithText("Average recorded weekly distance: 24.5 km · manual")
            .performScrollTo().assertIsDisplayed()
        val stored = FeatureStore(context).savedManualBaseline()
        assertEquals(24.5, stored.weeklyDistanceKm()!!, 0.0)
        assertTrue(FeatureStore(context).manualBaselineRecordedAt() != null)
        assertTrue(FeatureStore(context).activities().isEmpty())
    }

    @Test fun unfinishedTargetTimeFormSurvivesRecreationWithoutChangingGoal() {
        launch()
        continueToGoals()
        onView(withText("Half Marathon")).perform(scrollTo(), click())
        nativeContinue()
        onView(withText("Target Time")).perform(scrollTo(), click())
        onView(withHint(R.string.onboarding_target_hint)).perform(scrollTo(), replaceText("1:59:59"))
        closeSoftKeyboard()

        scenario!!.recreate()
        onView(withHint(R.string.onboarding_target_hint)).check(matches(withText("1:59:59")))
        onView(withText("Target race pace: 5:41/km")).perform(scrollTo()).check(matches(isDisplayed()))
        assertTrue("An unfinished form must not create a saved goal", !SqliteUserPreferencesRepository(context).load().isPresent)
    }

    @Test fun existingHistoryIsReviewedBeforeOptionalGapQuestions() {
        val monday = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val records = (1L..8L).flatMap { week ->
            listOf(0L, 2L, 5L).map { day ->
                val start = monday.minusWeeks(week).plusDays(day).atTime(LocalTime.of(7, 0)).toInstant(ZoneOffset.UTC)
                fixtureRun("run-$week-$day", start)
            }
        }
        FeatureStore(context).applyImport("health-connect", records, emptyList(), null)
        launch()

        compose.onNodeWithText("Found 24 recorded runs in the available history window.")
            .performScrollTo().assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("Fill gaps or correct this baseline").performScrollTo().performClick()
        waitForText("Optional current-running details")
        field("Current weekly distance (km)").assertDoesNotExist()
        field("Current usual runs/week").assertDoesNotExist()
        field("Longest recent run (km)").assertDoesNotExist()
        field("Approximate easy pace (MM:SS/km)").performScrollTo().assertIsDisplayed()
    }

    private fun fixtureRun(id: String, start: Instant) = ActivityRecord(
        id, SourceIdentity("health-connect", id, "org.racedaydreams.synthetic.test"),
        start, start.plus(Duration.ofMinutes(40)), ZoneOffset.UTC, ZoneOffset.UTC,
        6_000.0, null, null, null, null, null, null,
        "Synthetic instrumentation fixture", emptyList(), emptyList(), null, null
    )

    private fun launch() {
        scenario = ActivityScenario.launch(CoachActivity::class.java)
        waitForText("Start with the running you already do")
    }

    private fun continueToGoals() {
        compose.onNodeWithText("Looks right / continue to my goals").performScrollTo().performClick()
        waitNativeText(R.string.onboarding_title)
        onView(withText(R.string.onboarding_title)).check(matches(isDisplayed()))
    }

    private fun nativeContinue() {
        waitNativeText(R.string.onboarding_continue)
        onView(withText(R.string.onboarding_continue)).perform(scrollTo(), click())
    }

    private fun waitNativeText(resource: Int) {
        val expected = context.getString(resource)
        compose.waitUntil(15_000) {
            var found = false
            scenario!!.onActivity { activity -> found = hasEnabledText(activity.window.decorView, expected) }
            found
        }
    }

    private fun hasEnabledText(view: View, text: String): Boolean {
        if (view is TextView && view.text.toString() == text && view.isShown && view.isEnabled) return true
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            if (hasEnabledText(view.getChildAt(index), text)) return true
        }
        return false
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    private fun waitForText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}
