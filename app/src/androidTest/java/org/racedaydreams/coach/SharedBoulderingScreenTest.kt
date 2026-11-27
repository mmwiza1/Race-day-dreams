package org.racedaydreams.coach

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.climbing.*
import org.racedaydreams.coach.theme.DisciplineAppearance
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.settings.AccentColour
import org.racedaydreams.domain.settings.ColourDiscipline
import org.racedaydreams.storage.SqliteClimbing

/** Real shared bouldering screens over the real local database. */
@RunWith(AndroidJUnit4::class)
class SharedBoulderingScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = MainScope()
    private val database = AndroidSqlConnection.forApplication(context)

    private fun clear() {
        // Instrumentation runs off the main thread, as the storage bridge requires.
        for (table in
            listOf("climbing_sessions", "climbing_session_draft", "climbing_preferences")) database
            .execute("DELETE FROM $table")
    }

    @Before fun prepare() = clear()

    @After
    fun cleanup() {
        scope.cancel()
        clear()
    }

    private fun model() =
        SharedBoulderingCoordinator(
            database,
            scope,
            Instant::now,
            { ZoneOffset.UTC },
            { UUID.randomUUID().toString() },
        )

    /** Waits for [tag]; on failure reports what the screen and the coordinator actually held. */
    private fun awaitTag(tag: String, model: SharedBoulderingCoordinator) {
        try {
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError(
                "No '$tag' node. State: ${model.state.value}\n" +
                    compose.onRoot(useUnmergedTree = true).printToString(),
                timeout,
            )
        }
    }

    private fun addProblem(grade: String?, extraAttempts: Int, outcome: ClimbOutcome) {
        compose.onNodeWithTag("climbing-add-problem").performScrollTo().performClick()
        if (grade != null) {
            val choice = compose.onNodeWithTag("grade-$grade")
            choice.performScrollTo().performClick()
            choice.assertIsSelected()
            if (grade == "V4") {
                compose
                    .onNode(hasText("Selected grade") and hasText("V4"))
                    .performScrollTo()
                    .assertIsDisplayed()
                capturePart2(context, "piece5-grade")
            }
        }
        val flash = compose.onNodeWithTag("outcome-FLASH")
        if (outcome == ClimbOutcome.SEND && extraAttempts > 0)
            flash.performScrollTo().performClick().assertIsSelected()
        repeat(extraAttempts) {
            compose.onNodeWithContentDescription("More attempts").performScrollTo().performClick()
        }
        if (extraAttempts > 0) {
            flash.performScrollTo().assertIsNotEnabled().assertIsNotSelected()
            if (outcome == ClimbOutcome.SEND)
                compose.onNodeWithTag("outcome-SEND").assertIsSelected()
        }
        val selectedOutcome = compose.onNodeWithTag("outcome-${outcome.name}")
        selectedOutcome.performScrollTo().performClick()
        selectedOutcome.assertIsSelected()
        if (outcome == ClimbOutcome.SEND && extraAttempts > 0) {
            compose.onNodeWithTag("climbing-attempts").assert(hasContentDescription("4 attempts"))
            capturePart2(context, "piece5-attempts")
        }
        compose.onNodeWithTag("climbing-save-problem").assertIsDisplayed().performClick()
        compose.waitForIdle()
    }

    @Test
    fun logResumeReviewCorrectAndDeleteASessionKeepingOriginalGrades() {
        var model = model()
        var page by mutableStateOf("home")
        var detail by mutableStateOf("")
        val today = LocalDate.now()
        compose.setContent {
            val dark = InstrumentationRegistry.getArguments().getString("part2Evidence") == "dark"
            RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                DisciplineAppearance(
                    if (dark) mapOf(ColourDiscipline.BOULDERING to AccentColour.VIOLET)
                    else emptyMap(),
                    ColourDiscipline.BOULDERING,
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (page) {
                            "home" ->
                                SharedBoulderingHome(
                                    model,
                                    today,
                                    ::friendlyDate,
                                    onBack = {},
                                    openSession = { page = "session" },
                                    openDetail = {
                                        detail = it
                                        page = "detail"
                                    },
                                    openHistory = { page = "history" },
                                )
                            "session" ->
                                SharedBoulderingSession(
                                    model,
                                    Instant::now,
                                    ::friendlyDate,
                                    onBack = { page = "home" },
                                    openReview = { page = "review" },
                                )
                            "review" ->
                                SharedBoulderingReview(
                                    model,
                                    Instant::now,
                                    ::friendlyDate,
                                    onBack = { page = "session" },
                                    onSaved = {
                                        detail = it
                                        page = "detail"
                                    },
                                )
                            "detail" ->
                                SharedBoulderingDetail(
                                    model,
                                    detail,
                                    ::friendlyDate,
                                    onBack = { page = "home" },
                                    openCorrection = { page = "session" },
                                )
                            "history" ->
                                SharedBoulderingHistory(
                                    model,
                                    ::friendlyDate,
                                    onBack = { page = "home" },
                                    openDetail = {
                                        detail = it
                                        page = "detail"
                                    },
                                )
                        }
                    }
                }
            }
        }
        // New climber: honest empty state, offered only after the device has been read.
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithText("Start bouldering session")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("kpi-Highest send").assert(hasText("None yet"))
        // V3 Strength composition: one hero carries starting and logging a session.
        compose.onNodeWithTag("screen-hero").assert(hasAnyDescendant(hasText("Log a past session")))
        compose
            .onNodeWithText("No bouldering sessions recorded yet.")
            .performScrollTo()
            .assertExists()

        capturePart2(context, "piece5-empty")
        compose.onNodeWithText("Start bouldering session").performScrollTo().performClick()
        awaitTag("climbing-elapsed", model)
        capturePart2(context, "piece5-active")
        addProblem("V4", 0, ClimbOutcome.FLASH)
        addProblem("V5", 3, ClimbOutcome.SEND)
        addProblem(null, 1, ClimbOutcome.NOT_SENT)
        assertSessionFigures(7)
        compose.onNodeWithText("Recent problems").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not sent", substring = false).performScrollTo().assertIsDisplayed()
        capturePart2(context, "piece5-recent-problems")

        // An interrupted app: a new coordinator over the same database finds the unsaved session.
        compose.waitUntil(5_000) { SqliteClimbing(database).draft()?.problems?.size == 3 }
        val resumed = model()
        resumed.refresh()
        compose.waitUntil(5_000) { resumed.state.value.draft?.problems?.size == 3 }
        model = resumed
        page = "home"
        compose.onNodeWithText("Resume session").performScrollTo().performClick()

        compose.onNodeWithText("Finish and review").performScrollTo().performClick()
        compose.onNodeWithTag("climbing-minutes").performScrollTo().performTextReplacement("0")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("climbing-save-session").performScrollTo().performClick()
        compose
            .onNodeWithText("Climbing time must be between 1 minute and 24 hours.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("climbing-save-session").performScrollTo().assertIsDisplayed()
        capturePart2(context, "piece5-save-error")
        assertEquals(0, SqliteClimbing(database).sessions().size)
        compose.onNodeWithTag("climbing-minutes").performScrollTo().performTextReplacement("75")
        assertEquals(75L * 60, resumed.state.value.draft?.durationSeconds)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        saveAndAwaitDetail(resumed) { page == "detail" }
        compose.onNodeWithText("1 h 15 min", substring = true).assertExists()
        assertSessionFigures(7)
        compose.onNodeWithText("Bouldering session saved.").performScrollTo().assertIsDisplayed()
        capturePart2(context, "piece5-saved")
        assertEquals(null, SqliteClimbing(database).draft())

        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.waitUntil(5_000) { model.state.value.sessions.size == 1 }
        compose.onNodeWithTag("kpi-Sessions").assert(hasText("1"))
        compose.onNodeWithTag("kpi-Highest send").assert(hasText("V5"))
        compose.onNodeWithTag("kpi-Flashes").assert(hasText("1"))
        compose
            .onNodeWithText("Not converted or shown here: 1 ungraded problem.")
            .performScrollTo()
            .assertExists()

        // Preferring Fontainebleau never rewrites V-scale grades; they stay "as entered".
        compose.onNodeWithTag("grade-system-FONTAINEBLEAU").performScrollTo().performClick()
        compose.onNodeWithTag("kpi-Highest send").assert(hasText("V5"))
        compose.onNodeWithTag("kpi-Highest send").assert(hasText("V-scale (as entered)"))
        compose.waitUntil(5_000) {
            SqliteClimbing(database).gradeSystem() == GradeSystem.FONTAINEBLEAU
        }
        val saved = SqliteClimbing(database).sessions().single()
        assertEquals(
            listOf(
                ClimbingGrade(GradeSystem.V_SCALE, "V4"),
                ClimbingGrade(GradeSystem.V_SCALE, "V5"),
                null,
            ),
            saved.problems.map { it.grade },
        )

        // Visit history before reopening and correcting the recorded session.
        compose.onNodeWithText("See all").performScrollTo().performClick()
        compose.onNodeWithText("Bouldering history").assertIsDisplayed()
        val historyRow = compose.onNodeWithTag("climbing-session-${saved.id}")
        historyRow.performScrollTo().assertIsDisplayed().assertHasClickAction()
        val summary = hasText("2 / 3 sent", substring = true)
        historyRow.assert(summary or hasAnyDescendant(summary))
        capturePart2(context, "piece5-history")
        historyRow.performClick()
        assertEquals(saved.id, detail)
        compose.onNodeWithText("Correct session").performScrollTo().performClick()
        compose.onNodeWithText("Correct session").assertExists()
        compose.onNodeWithTag("climbing-session-details").performScrollTo().performClick()
        compose
            .onNodeWithText("Climbing gym or location (optional)")
            .performScrollTo()
            .performTextReplacement("North wall")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Review corrections").performScrollTo().performClick()
        saveAndAwaitDetail(resumed) { page == "detail" }
        assertEquals("North wall", SqliteClimbing(database).sessions().single().location)
        compose.onNodeWithText("Delete session").performScrollTo().performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(5_000) { SqliteClimbing(database).sessions().isEmpty() }
    }

    private fun assertSessionFigures(attempts: Int) {
        val summary = hasAnyAncestor(hasTestTag("climbing-problem-summary"))
        for ((label, value) in
            listOf("Attempts" to attempts.toString(), "Sends" to "2", "Flashes" to "1")) {
            compose.onNode(summary and hasText(label) and hasText(value)).assertExists()
        }
    }

    private fun saveAndAwaitDetail(model: SharedBoulderingCoordinator, inDetail: () -> Boolean) {
        compose
            .onNodeWithTag("climbing-save-session")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        try {
            compose.waitUntil(5_000, inDetail)
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError(
                "Session did not open after saving. State: ${model.state.value}\n" +
                    compose.onRoot(useUnmergedTree = true).printToString(),
                timeout,
            )
        }
    }

    @Test
    fun startingBeforeTheDeviceIsReadNeverReplacesAnInterruptedSession() {
        val interrupted =
            ClimbingDraft(
                "interrupted",
                Instant.now().minusSeconds(1_800),
                ZoneOffset.UTC,
                null,
                "North wall",
                "",
                listOf(
                    ClimbingProblem(
                        "p",
                        ClimbingGrade(GradeSystem.V_SCALE, "V2"),
                        1,
                        ClimbOutcome.FLASH,
                    )
                ),
            )
        SqliteClimbing(database).saveDraft(interrupted)
        val model = model()
        // Not yet read: a start request must not create a session over the saved one.
        model.startLive()
        model.startPast(LocalDate.now())
        model.refresh()
        compose.waitUntil(5_000) { !model.state.value.loading }
        assertEquals(interrupted, model.state.value.draft)
        assertEquals(interrupted, SqliteClimbing(database).draft())
    }

    @Test
    fun strengthDashboardShowsBoulderingOnlyForClimbersAndKeepsLiftingMetricsSeparate() {
        val today = LocalDate.now()
        val climb =
            ClimbingSession(
                "climb",
                today.atTime(18, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
                3600,
                "",
                "",
                listOf(
                    ClimbingProblem(
                        "p",
                        ClimbingGrade(GradeSystem.V_SCALE, "V3"),
                        2,
                        ClimbOutcome.SEND,
                    )
                ),
            )
        var climbing by mutableStateOf(emptyList<ClimbingSession>())
        val opened = mutableListOf<StrengthLink>()
        compose.setContent {
            MaterialTheme {
                SharedStrengthDashboard(
                    CoachUiState(loading = false),
                    today,
                    Instant.now(),
                    ::shortDate,
                    ::friendlyDate,
                    ::decimal,
                    StrengthLink.entries.toList(),
                    open = { opened.add(it) },
                    openWorkout = {},
                    climbing = climbing,
                )
            }
        }
        compose.onNodeWithTag("strength-bouldering-card").assertDoesNotExist()
        compose.onNodeWithText("Bouldering").performScrollTo().assertExists()
        climbing = listOf(climb)
        compose.onNodeWithTag("strength-bouldering-card").assertExists()
        compose
            .onNodeWithTag("strength-bouldering-card")
            .assert(hasText("hardest V3", substring = true))
        // Climbing never becomes working sets, workouts or records.
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("0"))
        compose.onNodeWithTag("kpi-Workouts").assert(hasText("0"))
        compose.onNodeWithTag("kpi-New records").assert(hasText("0"))
        compose.onNodeWithTag("strength-bouldering-card").performScrollTo().performClick()
        assertEquals(StrengthLink.BOULDERING, opened.last())
    }
}
