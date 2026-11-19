package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.domain.climbing.*
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
        for (table in listOf("climbing_sessions", "climbing_session_draft", "climbing_preferences"))
            database.execute("DELETE FROM $table")
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

    private fun addProblem(grade: String?, extraAttempts: Int, outcome: ClimbOutcome) {
        compose.onNodeWithTag("climbing-add-problem").performScrollTo().performClick()
        if (grade != null) compose.onNodeWithTag("grade-$grade").performScrollTo().performClick()
        repeat(extraAttempts) {
            compose.onNodeWithContentDescription("More attempts").performScrollTo().performClick()
        }
        if (extraAttempts > 0)
            compose.onNodeWithTag("outcome-FLASH").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("outcome-${outcome.name}").performScrollTo().performClick()
        compose.onNodeWithTag("climbing-save-problem").performClick()
        compose.waitForIdle()
    }

    @Test
    fun logResumeReviewCorrectAndDeleteASessionKeepingOriginalGrades() {
        var model = model()
        var page by mutableStateOf("home")
        var detail by mutableStateOf("")
        val today = LocalDate.now()
        compose.setContent {
            MaterialTheme {
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
                    else -> {}
                }
            }
        }
        // New climber: honest empty state.
        compose.onNodeWithText("Start bouldering session").assertExists()
        compose.onNodeWithTag("kpi-Highest send").assert(hasText("None yet"))
        compose.onNodeWithText("No bouldering sessions recorded yet.").performScrollTo().assertExists()

        compose.onNodeWithText("Start bouldering session").performClick()
        compose.onNodeWithTag("climbing-elapsed").assertExists()
        addProblem("V4", 0, ClimbOutcome.FLASH)
        addProblem("V5", 3, ClimbOutcome.SEND)
        addProblem(null, 1, ClimbOutcome.NOT_SENT)
        compose.onNodeWithTag("climbing-problem-summary").assert(hasText("3 problems · 2 sent · 1 flashed"))

        // An interrupted app: a new coordinator over the same database finds the unsaved session.
        compose.waitUntil(5_000) { SqliteClimbing(database).draft()?.problems?.size == 3 }
        val resumed = model()
        resumed.refresh()
        compose.waitUntil(5_000) { resumed.state.value.draft?.problems?.size == 3 }
        model = resumed
        page = "home"
        compose.onNodeWithText("Resume session").performClick()

        compose.onNodeWithText("Finish and review").performScrollTo().performClick()
        compose.onNodeWithTag("climbing-minutes").performTextReplacement("75")
        compose.onNodeWithTag("climbing-save-session").performScrollTo().performClick()
        compose.waitUntil(5_000) { page == "detail" }
        compose.onNodeWithText("1 h 15 min", substring = true).assertExists()
        compose.onNodeWithTag("kpi-Sends").assert(hasText("2"))
        assertEquals(null, SqliteClimbing(database).draft())

        compose.onNodeWithText("Back").performClick()
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
            listOf(ClimbingGrade(GradeSystem.V_SCALE, "V4"), ClimbingGrade(GradeSystem.V_SCALE, "V5"), null),
            saved.problems.map { it.grade },
        )

        // Correct the saved session, then delete it.
        detail = saved.id
        page = "detail"
        compose.onNodeWithText("Correct session").performScrollTo().performClick()
        compose.onNodeWithText("Correct session").assertExists()
        compose.onNodeWithText("Climbing gym or location (optional)").performTextReplacement("North wall")
        compose.onNodeWithText("Review corrections").performScrollTo().performClick()
        compose.onNodeWithTag("climbing-save-session").performScrollTo().performClick()
        compose.waitUntil(5_000) { SqliteClimbing(database).sessions().single().location == "North wall" }
        compose.onNodeWithText("Delete session").performScrollTo().performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(5_000) { SqliteClimbing(database).sessions().isEmpty() }
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
                    ClimbingProblem("p", ClimbingGrade(GradeSystem.V_SCALE, "V3"), 2, ClimbOutcome.SEND)
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
        compose.onNodeWithTag("strength-bouldering-card").assert(hasText("hardest V3", substring = true))
        // Climbing never becomes working sets, workouts or records.
        compose.onNodeWithTag("kpi-Working sets").assert(hasText("0"))
        compose.onNodeWithTag("kpi-Workouts").assert(hasText("0"))
        compose.onNodeWithTag("kpi-New records").assert(hasText("0"))
        compose.onNodeWithTag("strength-bouldering-card").performScrollTo().performClick()
        assertEquals(StrengthLink.BOULDERING, opened.last())
    }
}
