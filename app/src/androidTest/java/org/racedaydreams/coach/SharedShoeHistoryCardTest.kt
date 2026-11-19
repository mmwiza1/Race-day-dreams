package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.activity.ShoeHistoryCard
import org.racedaydreams.domain.activity.*

/** Real shared production card; same evidence, disclosure and edit action in supported modes. */
@RunWith(AndroidJUnit4::class)
class SharedShoeHistoryCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun lightCardRetainsUnknownEarlierMileageAndOptionalEvidence() = verify(false, 1f)
    @Test fun darkCardRetainsUnknownEarlierMileageAndOptionalEvidence() = verify(true, 1f)
    @Test fun doubledTextKeepsDisclosureAndEditReachable() = verify(false, 2f)

    private fun verify(dark: Boolean, scale: Float) {
        val edited = AtomicInteger()
        val insights = AtomicInteger()
        val shoe = Shoe("retained", "Brand", "Everyday", LocalDate.of(2026,11,1),null,false,"Retained notes",true)
        val summary = ShoeMileage.calculate(shoe,emptyList(),LocalDate.of(2026,11,13))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    CoachPage {
                        ShoeHistoryCard(shoe,summary,false,{"${it / 1000} km"},{"fixture date"},{insights.incrementAndGet();listOf("Retained optional insight")},{edited.incrementAndGet()})
                    }
                }
            }
        }
        compose.onNodeWithText("Brand Everyday").assertExists()
        compose.onNodeWithText("0.0 km recorded").assertExists()
        compose.onNodeWithText("Retained notes").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0,insights.get()) }
        compose.onNodeWithText("View shoe details").performScrollTo().performClick()
        compose.onNodeWithText("Earlier mileage was not supplied; recorded distance is a known lower bound, not lifetime shoe mileage.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Retained notes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Retained optional insight").assertExists()
        compose.runOnIdle { assertTrue(insights.get() > 0) }
        compose.onNodeWithText("Hide shoe details").performScrollTo().performClick()
        compose.onNodeWithText("Retained notes").assertDoesNotExist()
        compose.onNodeWithText("Edit shoes").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,edited.get()) }
    }
}
