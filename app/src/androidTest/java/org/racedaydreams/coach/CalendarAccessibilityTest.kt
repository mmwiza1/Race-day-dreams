package org.racedaydreams.coach

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.presentation.CalendarData

@RunWith(AndroidJUnit4::class)
class CalendarAccessibilityTest {
    @get:Rule val compose=createComposeRule()
    @Test fun darkLargeTextRetainsCategoryLegendAndCompletionMeaning(){
        compose.setContent{MaterialTheme(colorScheme=darkColorScheme()){CompositionLocalProvider(LocalDensity provides Density(1f,2f)){Column{CalendarLegend();CalendarMarker(CalendarData.Entry(CalendarData.Category.LONG,CalendarData.State.COMPLETED,"Long run",null,null))}}}}
        listOf("Easy","Quality","Long","Strength","Race","Rest").forEach{compose.onNodeWithText(it).assertExists()}
        compose.onNodeWithContentDescription("Long run: completed").assertExists()
        compose.onNodeWithText("Legend: outline planned",substring=true).assertExists()
    }
    @Test fun explicitStatesHaveReadableDescriptions(){
        compose.setContent{MaterialTheme{Column{CalendarData.State.entries.forEach{CalendarMarker(CalendarData.Entry(CalendarData.Category.QUALITY,it,"Tempo run",null,null))}}}}
        listOf("planned","completed","not completed","moved","skipped").forEach{compose.onNodeWithContentDescription("Tempo run: $it").assertExists()}
    }
}
