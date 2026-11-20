package org.racedaydreams.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.AppUpdateActions
import org.racedaydreams.coach.AppUpdatesCard
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.UpdatePreferences

/** The Settings card's controls for each update state, without network or installation. */
@RunWith(AndroidJUnit4::class)
class AppUpdatesCardTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val installed = UpdateFixtures.olderReleaseInstall(context)
    private val update = UpdateFixtures.announcedCurrentBuild(context)

    @Test
    fun anUpdateCanBeDownloadedPostponedOrSkippedButNotDuringAWorkout() {
        val calls = mutableListOf<String>()
        var state by
            mutableStateOf(AppUpdateState(installed, UpdatePreferences(), UpdateStatus.Available(update, true)))
        compose.setContent {
            // Hosted in a scrolling page, as in Settings.
            MaterialTheme { Page {
                AppUpdatesCard(
                    state,
                    AppUpdateActions(
                        download = { calls += "download" },
                        later = { calls += "later" },
                        skip = { calls += "skip" },
                        check = { calls += "check" },
                    ),
                )
            } }
        }
        compose.onNodeWithText("Installed: ${installed.versionName} (build ${installed.versionCode})", substring = true).assertExists()
        compose.onNodeWithTag("app-updates-status").assert(hasText("Update available: ${update.versionName}", substring = true))
        compose.onNodeWithText("What's new in ${update.versionName}").performClick()
        compose.onNodeWithTag("app-updates-notes").assert(hasText("Settings › App updates", substring = true))
        compose.onNodeWithTag("app-updates-download").performClick()
        compose.onNodeWithText("Remind me later").performClick()
        compose.onNodeWithText("Skip this version").performClick()
        assertEquals(listOf("download", "later", "skip"), calls)

        // Once postponed or skipped, the update stays listed without asking again.
        state = state.copy(status = UpdateStatus.Available(update, false))
        compose.onNodeWithText("Skip this version").assertDoesNotExist()
        compose.onNodeWithTag("app-updates-download").assertIsEnabled()

        // An active run, workout or bouldering session blocks downloading and installing.
        state = state.copy(activeWork = "A gym workout is in progress.")
        compose.onNodeWithTag("app-updates-active-work").assert(hasText("nothing is interrupted", substring = true))
        compose.onNodeWithTag("app-updates-download").assertIsNotEnabled()
        state = state.copy(status = UpdateStatus.Ready(update))
        compose.onNodeWithTag("app-updates-install").assertIsNotEnabled()
        state = state.copy(activeWork = null)
        compose.onNodeWithTag("app-updates-install").assertIsEnabled()
    }

    @Test
    fun failuresOfferTheMatchingRetryAndSigningConflictsExplainTheSafeMove() {
        val calls = mutableListOf<String>()
        var state by
            mutableStateOf(
                AppUpdateState(
                    installed,
                    UpdatePreferences(),
                    UpdateStatus.Failed(UpdateFailure.Interrupted.message, update, UpdateStatus.Retry.DOWNLOAD),
                )
            )
        compose.setContent {
            // Hosted in a scrolling page, as in Settings.
            MaterialTheme { Page {
                AppUpdatesCard(
                    state,
                    AppUpdateActions(
                        download = { calls += "download" },
                        install = { calls += "install" },
                        check = { calls += "check" },
                        openReleasePage = { calls += "release" },
                    ),
                )
            } }
        }
        compose.onNodeWithTag("app-updates-status").assert(hasText("interrupted", substring = true))
        compose.onNodeWithText("Try again").performClick()
        state = state.copy(status = UpdateStatus.Failed(UpdateFailure.Offline.message, null, UpdateStatus.Retry.CHECK))
        compose.onNodeWithText("Try again").performClick()
        assertEquals(listOf("download", "check"), calls)

        state = state.copy(status = UpdateStatus.Incompatible(update, AppUpdates.Incompatibility.SIGNING))
        compose.onNodeWithTag("app-updates-status").assert(hasText("can't install it as an update", substring = true))
        compose.onNodeWithText("How to move to the update-ready version safely").performClick()
        compose.onNodeWithText("Complete backup (app + recordings)", substring = true).assertExists()
        compose.onNodeWithText("Validate and restore", substring = true).assertExists()
        compose.onNodeWithText("Open the release page").performScrollTo().performClick()
        assertEquals("release", calls.last())
        // Never offers a download it cannot install.
        compose.onNodeWithTag("app-updates-download").assertDoesNotExist()
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) =
    Column(Modifier.verticalScroll(rememberScrollState())) { content() }
