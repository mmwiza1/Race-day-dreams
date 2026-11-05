package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.storage.*
import org.racedaydreams.data.SqliteRecordingInventoryRepository
import org.racedaydreams.domain.storage.DetailRetention

@RunWith(AndroidJUnit4::class)
class StorageManagementScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightReviewRequiresSeparateConfirmation() = verify(false, 1f)

    @Test fun darkReviewRequiresSeparateConfirmation() = verify(true, 1f)

    @Test fun doubleTextReviewRequiresSeparateConfirmation() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        val now = Instant.parse("2026-11-05T12:00:00Z")
        val row =
            DetailRetention.Recording(
                "s",
                "a",
                "rdd",
                "s",
                now.minusSeconds(90000000),
                true,
                4000,
                25,
                0L,
            )
        val preview =
            DetailRetention.preview(
                DetailRetention.Policy.LAST_24_MONTHS,
                listOf(row),
                now,
                "epoch",
            )
        var cleaned = 0
        var choices = 0
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    StorageManagementContent(
                        StorageManagementState(
                            false,
                            DetailRetention.Preferences.acknowledged(
                                DetailRetention.Policy.LAST_24_MONTHS,
                                now,
                                "choice",
                            ),
                            SqliteRecordingInventoryRepository.Inventory(
                                listOf(row),
                                false,
                                SqliteRecordingInventoryRepository.Usage(
                                    4000,
                                    25,
                                    1000000,
                                    1,
                                    400,
                                    5000000,
                                ),
                            ),
                            preview,
                        ),
                        {},
                        {},
                        { choices++ },
                        {},
                        { cleaned++ },
                    )
                }
            }
        }
        compose.onNodeWithText("5 years").performScrollTo().performClick()
        compose
            .onNodeWithText("✓ 5 years")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        Assert.assertEquals(0, choices)
        Assert.assertEquals(0, cleaned)
        compose
            .onNodeWithText("I understand — save retention choice")
            .performScrollTo()
            .performClick()
        Assert.assertEquals(1, choices)
        Assert.assertEquals(0, cleaned)
        compose.onNodeWithText("Storage details ▾").performScrollTo().performClick()
        compose
            .onNodeWithText("Storage details ▴")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        compose.onNodeWithText("Storage details ▴").performClick()
        compose.onNodeWithText("Review cleanup confirmation").performScrollTo().performClick()
        compose.onNodeWithText("Remove this raw detail?").assertIsDisplayed()
        Assert.assertEquals(0, cleaned)
        compose.onNodeWithText("Keep my recording detail").performClick()
        Assert.assertEquals(0, cleaned)
        compose.onNodeWithText("Review cleanup confirmation").performScrollTo().performClick()
        compose.onNodeWithText("Remove previewed raw detail").performClick()
        Assert.assertEquals(1, cleaned)
    }

    @Test
    fun ExistingUsersAndUnfinishedRunsCannotStartCleanup() {
        compose.setContent {
            MaterialTheme {
                StorageManagementContent(
                    StorageManagementState(
                        false,
                        DetailRetention.Preferences.existing(),
                        SqliteRecordingInventoryRepository.Inventory(
                            emptyList(),
                            true,
                            SqliteRecordingInventoryRepository.Usage(0, 0, 0, 0, 0, 0),
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { Assert.fail("Cleanup must stay unavailable") },
                )
            }
        }
        compose.onNodeWithText("Review eligible detail").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Review cleanup confirmation").assertDoesNotExist()
    }
}
