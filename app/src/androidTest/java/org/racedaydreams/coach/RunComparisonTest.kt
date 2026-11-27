package org.racedaydreams.coach

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.racedaydreams.coach.theme.RaceDayTheme
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
        val dark = InstrumentationRegistry.getArguments().getString("part2Evidence") == "dark"
        compose.setContent {
            RaceDayTheme(darkTheme = dark, dynamicColour = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
        }
        compose.onNodeWithText("Compare recordings").performScrollTo().assertIsDisplayed()
        capturePart2(context, "run-comparison-collapsed")
        compose.onNodeWithText("Compare recordings").performClick()
        val nativeOption = compose.onNode(hasText("1.50 km") and hasClickAction())
        val fitbitOption = compose.onNode(hasText("5.10 km") and hasClickAction())
        nativeOption.assert(hasText("32:57 elapsed · 25 November 2026 · 11:55")).assertIsNotSelected()
        fitbitOption.assert(hasText("33:30 elapsed · 25 November 2026 · 11:55")).assertIsNotSelected()
        compose.onNodeWithText("Use automatic selection").assertIsSelected()
        fitbitOption.performScrollTo().performClick().assertIsSelected().assert(hasText("Counted"))
        capturePart2(context, "run-comparison-selected")
        nativeOption.assert(hasText("Kept in history"))
        assertEquals(
            "fitbit",
            PhysicalRunSessions.counted(repository.activitySummaries()).single().id(),
        )
        compose.onNodeWithText("Saved · One run counts").performScrollTo().assertIsDisplayed()
        capturePart2(context, "run-comparison-review")
        compose.onNodeWithText("Remove uncounted local copy").performScrollTo().performClick()
        compose.onNodeWithText("Remove local recording?").assertIsDisplayed()
        compose.onNodeWithText("Remove copy").assertIsDisplayed()
        capturePart2(context, "run-comparison-confirmation")
        compose.onNodeWithText("Keep both copies").performClick()
        assertEquals(2, repository.activitySummaries().size)
        nativeOption.performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Remove uncounted local copy").assertDoesNotExist()
        fitbitOption.performScrollTo().performClick().assertIsSelected()
        repository.applyImport("health-connect", listOf(fitbit), emptyList(), "again")
        assertEquals(
            "fitbit",
            PhysicalRunSessions.counted(repository.activitySummaries()).single().id(),
        )
        compose.onNodeWithText("Count both separately").performScrollTo().performClick().assertIsSelected()
        assertEquals(2, PhysicalRunSessions.counted(repository.activitySummaries()).size)
        compose.onNodeWithText("Use automatic selection").performScrollTo().performClick().assertIsSelected()
        assertEquals(
            "native",
            PhysicalRunSessions.counted(repository.activitySummaries()).single().id(),
        )
        assertEquals(2, repository.activitySummaries().size)
    }
}
