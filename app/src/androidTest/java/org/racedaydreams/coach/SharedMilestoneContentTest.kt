package org.racedaydreams.coach

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.coach.theme.RaceDayTheme
import org.racedaydreams.domain.achievements.TrainingMilestones

@RunWith(AndroidJUnit4::class)
class SharedMilestoneContentTest {
    @get:Rule val compose=createComposeRule()
    private val at=Instant.parse("2026-11-15T12:00:00Z")
    private val proof=TrainingMilestones.Proof(TrainingMilestones.Source.RUN,"run/raw-id",at,ZoneOffset.UTC,5000.0)
    private val award=TrainingMilestones.Award(TrainingMilestones.Kind.FIRST_FIVE_KM_RUN,at,listOf(proof))
    private val actions=mutableListOf<String>()
    private var opened:TrainingMilestones.Proof?=null
    private fun show(known:Boolean,large:Boolean=false) {
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large)2f else 1f)) {
                RaceDayTheme(darkTheme=large,dynamicColour=false) {
                    SharedAchievementsScreen(false,listOf(SharedRecognizedMilestone(award,"15 Nov 2026")),false,if(known)setOf(proof.source() to proof.recordId())else emptySet(),null,null,{"15 Nov 2026"},{opened=it},{actions.add("back")},{actions.add("dismiss")},{actions.add("refresh:$it")})
                }
            }
        }
    }
    @Test fun milestoneTitleIsReadWithItsRecognitionWithoutATickGlyph() {
        show(true)
        val title=requireNotNull(award.kind().title())
        compose.onNodeWithText("✓ $title").assertDoesNotExist()
        compose.onNodeWithTag("milestone-${award.kind().name}").assertIsDisplayed()
            .assert(hasText(title)).assert(hasText("Recognised 15 Nov 2026"))
    }
    @Test fun disclosureKeepsOriginalSourceIdentityAndNeverAutomaticallyOpensOrRefreshes() {
        show(true)
        compose.onNodeWithText("View milestone evidence").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Collapsed"))
        compose.onNodeWithText("Open recorded run").assertDoesNotExist()
        compose.runOnIdle{assertTrue(actions.isEmpty());assertNull(opened)}
        compose.onNodeWithText("View milestone evidence").performScrollTo().performClick()
        compose.onNodeWithText("Hide milestone evidence").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Expanded"))
        compose.onNodeWithText("Open recorded run").performScrollTo().performClick()
        compose.runOnIdle{assertSame(proof,opened)}
        compose.onNodeWithText("Hide milestone evidence").performScrollTo().performClick()
        compose.onNodeWithText("Open recorded run").assertDoesNotExist()
    }
    @Test fun unavailableSourcesRemainHonestAndControlsScrollAtDoubleTextInDarkMode() {
        show(false,true)
        compose.onNodeWithText("View milestone evidence").performScrollTo().performClick()
        compose.onNodeWithText("Retained source reference; this source is not currently available on this device.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open recorded run").assertDoesNotExist()
        compose.onNodeWithContentDescription("Celebrate new milestones in app").performScrollTo().performClick()
        compose.onNodeWithText("Refresh recorded evidence").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(listOf("refresh:true","refresh:null"),actions);assertNull(opened)}
    }
}
