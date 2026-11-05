package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.*
import org.junit.runner.RunWith
import org.racedaydreams.coach.body.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.settings.PrimaryFocus

@RunWith(AndroidJUnit4::class)
class BodyMeasurementsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-11-05T12:00:00Z")

    @Test fun lightGuidanceHasExplicitBasisAndDisclosure() = verify(false, 1f)

    @Test fun darkGuidanceHasExplicitBasisAndDisclosure() = verify(true, 1f)

    @Test fun doubleTextGuidanceAndManualEntryAreScrollable() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        val row =
            BodyWeight(
                "chosen",
                87.0,
                now.minusSeconds(3600),
                ZoneOffset.UTC,
                "manual",
                "chosen",
                null,
            )
        val prefs =
            BodyMeasurementsRepository.Preferences(
                true,
                HeightUnit.CENTIMETRES,
                ProteinGuidance.Basis.from(row),
            )
        val value =
            BodyMeasurementsState(
                false,
                weights = listOf(row),
                preferences = prefs,
                advice =
                    ProteinGuidance.evaluate(
                        true,
                        prefs.basis(),
                        listOf(row),
                        PrimaryFocus.STRENGTH,
                        now,
                    ),
            )
        var heightEntry: String? = null
        var weightEntry: String? = null
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    BodyMeasurementsContent(
                        value,
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        { v, _ -> heightEntry = v },
                        { v, _ -> weightEntry = v },
                        {},
                        {},
                    )
                }
            }
        }
        compose
            .onNodeWithText("A general daily range is about 120–175 grams of protein.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Source: entered by you").assertExists()
        compose.onNodeWithText(ProteinGuidance.sources()).assertDoesNotExist()
        compose
            .onNodeWithText("Calculation & sources ▾")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .performClick()
        compose.onNodeWithText(ProteinGuidance.sources()).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Calculation & sources ▴").performScrollTo().performClick()
        compose.onNodeWithText(ProteinGuidance.sources()).assertDoesNotExist()
        compose
            .onNodeWithText("Measurement history & manual entry")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Height (cm)").performScrollTo().performTextReplacement("178")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save height measurement").performScrollTo().performClick()
        compose.onNodeWithText("Body weight (kg)").performScrollTo().performTextReplacement("87")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save body-weight measurement").performScrollTo().performClick()
        compose.runOnIdle {
            Assert.assertEquals("178", heightEntry)
            Assert.assertEquals("87", weightEntry)
        }
        compose.onNodeWithText("Chosen for general guidance").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                "Health Connect is unavailable here. Manual measurements and training remain available."
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun realManualMeasurementsNeedNoRunningGoalOrProviderAccess() {
        val app =
            androidx.test.core.app.ApplicationProvider.getApplicationContext<
                android.app.Application
            >()
        fun <T> io(work: () -> T): T =
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { work() }
        io { org.racedaydreams.data.SqliteUserPreferencesRepository(app).clear() }
        val store = androidx.lifecycle.ViewModelStore()
        lateinit var model: BodyMeasurementsViewModel
        try {
            compose.runOnIdle {
                model =
                    androidx.lifecycle
                        .ViewModelProvider(
                            store,
                            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
                                .getInstance(app),
                        )[BodyMeasurementsViewModel::class.java]
            }
            compose.setContent { MaterialTheme { BodyMeasurementsScreen(model, {}, {}) } }
            fun ready() {
                compose.waitUntil(20_000) { !model.state.value.busy }
                Assert.assertNull(model.state.value.error)
            }
            ready()
            compose
                .onNodeWithText("Measurement history & manual entry")
                .performScrollTo()
                .performClick()
            compose.onNodeWithText("in").performScrollTo().performClick()
            ready()
            compose.onNodeWithText("Height (in)").performScrollTo().performTextReplacement("70")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Save height measurement").performScrollTo().performClick()
            ready()
            Assert.assertEquals(1.778, model.state.value.heights.single().metres(), 0.000001)
            compose.onNodeWithText("lb").performScrollTo().performClick()
            compose
                .onNodeWithText("Body weight (lb)")
                .performScrollTo()
                .performTextReplacement("191.8")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Save body-weight measurement").performScrollTo().performClick()
            ready()
            Assert.assertEquals(87.0, model.state.value.weights.single().kilograms(), 0.01)
            Assert.assertNull(model.state.value.preferences.basis())
            Assert.assertFalse(model.state.value.preferences.proteinEnabled())
            Assert.assertEquals("manual", model.state.value.heights.single().provider())
            Assert.assertTrue(
                io { !org.racedaydreams.data.SqliteUserPreferencesRepository(app).load().isPresent }
            )
            Assert.assertTrue(io { org.racedaydreams.data.CoachingStore(app).strength().isEmpty() })
            Assert.assertTrue(
                io { org.racedaydreams.data.FeatureStore(app).activities().isEmpty() }
            )
        } finally {
            compose.waitUntil(20_000) { !model.state.value.busy }
            compose.runOnIdle { store.clear() }
            io { org.racedaydreams.data.SqliteUserPreferencesRepository(app).clear() }
        }
    }

    @Test
    fun noSelectedWeightNeverUsesNewProviderMeasurementAutomatically() {
        val imported =
            BodyWeight("hc", 87.0, now, ZoneOffset.UTC, "health-connect", "source", "provider.app")
        val prefs = BodyMeasurementsRepository.Preferences(true, HeightUnit.CENTIMETRES, null)
        val state =
            BodyMeasurementsState(
                false,
                weights = listOf(imported),
                preferences = prefs,
                advice =
                    ProteinGuidance.evaluate(true, null, listOf(imported), PrimaryFocus.HYBRID, now),
            )
        compose.setContent {
            MaterialTheme {
                BodyMeasurementsContent(
                    state,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }
        compose
            .onNodeWithText(
                "Choose a recorded body weight if you would like a general protein range."
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("A general daily range", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Choose or record a weight").performScrollTo().performClick()
        compose
            .onNodeWithText("Retained import; permission is not currently granted.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Use this weight for guidance").performScrollTo().assertIsDisplayed()
    }
}
