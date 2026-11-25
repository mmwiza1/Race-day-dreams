package org.racedaydreams.coach

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.data.SqliteActivityRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.activity.*

class RunComparisonTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun comparesFitbitAndNativeAndReversesTheCountedChoice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SqliteUserPreferencesRepository(context).clear()
        val repository = SqliteActivityRepository(context)
        val start = Instant.parse("2026-11-25T11:55:00Z")
        val native =
            ActivityRecord(
                "native",
                SourceIdentity("rdd", "native-record", "org.racedaydreams"),
                start,
                start.plusSeconds(1977),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                1500.0,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                emptyList(),
                emptyList(),
                null,
                null,
            )
        val fitbit =
            native.copy(
                id = "fitbit",
                source = SourceIdentity("health-connect", "watch", "com.fitbit.FitbitMobile"),
                distanceMetres = 5100.0,
                end = start.plusSeconds(2010),
            )
        repository.upsertManualActivity(native)
        repository.applyImport("health-connect", listOf(fitbit), emptyList(), "first")
        val retained = mutableStateOf(repository.activitySummaries())
        compose.setContent {
            MaterialTheme {
                CoachPage {
                    SharedRunComparison(
                        native,
                        retained.value,
                        false,
                        false,
                        { "25 November 2026 · 11:55" },
                        { id, other, choice ->
                            retained.value =
                                runBlocking(Dispatchers.IO) {
                                    repository.reviewDuplicateRun(id, other, choice)
                                    repository.activitySummaries()
                                }
                        },
                        { _, _ -> fail("No recording should be removed in this test") },
                    )
                }
            }
        }
        compose.onNodeWithText("Compare recordings").performScrollTo().performClick()
        compose.onNodeWithText("Compare with Fitbit · 5.10 km").performScrollTo().performClick()
        compose
            .onNodeWithText("1.50 km · 32 min 57 s elapsed")
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText("5.10 km · 33 min 30 s elapsed")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Count the other run").performScrollTo().performClick()
        assertEquals(
            "fitbit",
            PhysicalRunSessions.counted(repository.activitySummaries()).single().id(),
        )
        compose
            .onNodeWithText("Your choice: count the other run")
            .performScrollTo()
            .assertIsDisplayed()
        capturePart2(context, "run-comparison-review")
        repository.applyImport("health-connect", listOf(fitbit), emptyList(), "again")
        assertEquals(
            "fitbit",
            PhysicalRunSessions.counted(repository.activitySummaries()).single().id(),
        )
        compose.onNodeWithText("Count both separately").performScrollTo().performClick()
        assertEquals(2, PhysicalRunSessions.counted(repository.activitySummaries()).size)
        compose.onNodeWithText("Use automatic selection").performScrollTo().performClick()
        assertEquals(
            "native",
            PhysicalRunSessions.counted(repository.activitySummaries()).single().id(),
        )
        assertEquals(2, repository.activitySummaries().size)
    }
}
