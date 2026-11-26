package org.racedaydreams.coach.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.CoachFilterChip
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.domain.settings.AccentColour
import org.racedaydreams.domain.settings.ColourDiscipline

internal data class AccentRoles(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
)

internal fun AccentColour.roles(dark: Boolean): AccentRoles {
    val (lightTone, darkTone, lightContainer, darkContainer) =
        when (this) {
            AccentColour.BLUE -> listOf(0xFF2259A7, 0xFFAAC7FF, 0xFFD7E3FF, 0xFF16447F)
            AccentColour.TEAL -> listOf(0xFF006A60, 0xFF72D7C9, 0xFF94F2E2, 0xFF005048)
            AccentColour.GREEN -> listOf(0xFF356534, 0xFFA4D49B, 0xFFC0EEB7, 0xFF244E23)
            AccentColour.VIOLET -> listOf(0xFF594695, 0xFFD0BCFF, 0xFFEADDFF, 0xFF433178)
            AccentColour.PLUM -> listOf(0xFF783B72, 0xFFF3B2E8, 0xFFFFD7F4, 0xFF60295C)
            AccentColour.AMBER -> listOf(0xFF755600, 0xFFF5C768, 0xFFFFE1A5, 0xFF584000)
        }
    return if (dark)
        AccentRoles(Color(darkTone), Color(0xFF161616), Color(darkContainer), Color.White)
    else AccentRoles(Color(lightTone), Color.White, Color(lightContainer), Color(0xFF161616))
}

private val LocalBaseScheme = compositionLocalOf<ColorScheme?> { null }

@Composable
internal fun disciplineBaseScheme(): ColorScheme =
    LocalBaseScheme.current ?: MaterialTheme.colorScheme

private val LocalAccents = compositionLocalOf<Map<ColourDiscipline, AccentColour>> { emptyMap() }

@Composable
internal fun currentAccent(discipline: ColourDiscipline): AccentRoles? =
    (LocalAccents.current[discipline]
            ?: AccentColour.GREEN.takeIf { discipline == ColourDiscipline.SETTINGS })
        ?.roles(MaterialTheme.colorScheme.surface.luminance() < 0.5f)

@Composable
fun DisciplineAppearance(
    colours: Map<ColourDiscipline, AccentColour>,
    page: ColourDiscipline?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val base = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalAccents provides colours, LocalBaseScheme provides base) {
        val accent = page?.let { currentAccent(it) }
        MaterialTheme(
            colorScheme =
                if (accent == null) base
                else
                    base.copy(
                        primary = accent.primary,
                        onPrimary = accent.onPrimary,
                        primaryContainer = accent.container,
                        onPrimaryContainer = accent.onContainer,
                        secondaryContainer = accent.container,
                        onSecondaryContainer = accent.onContainer,
                    ),
            content = { Box(modifier) { content() } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisciplineColourSettings(
    colours: Map<ColourDiscipline, AccentColour>,
    busy: Boolean,
    select: (ColourDiscipline, AccentColour?) -> Unit,
) {
    var active by rememberSaveable { mutableStateOf(ColourDiscipline.RUNNING.name) }
    val discipline = ColourDiscipline.valueOf(active)
    val selected = colours[discipline]
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Text("Discipline colours", style = MaterialTheme.typography.titleMedium)
    FlowRow(
        Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
    ) {
        ColourDiscipline.entries.forEach {
            CoachFilterChip(active == it.name, { active = it.name }, { Text(it.label) })
        }
    }
    Text("${discipline.label} accent", style = MaterialTheme.typography.labelLarge)
    FlowRow(
        Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
    ) {
        CoachFilterChip(
            selected == null,
            { if (selected != null) select(discipline, null) },
            { Text(if (discipline == ColourDiscipline.SETTINGS) "Default (green)" else "Default") },
            enabled = !busy,
        )
        AccentColour.entries.forEach { colour ->
            CoachFilterChip(
                selected == colour,
                onClick = { if (selected != colour) select(discipline, colour) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Tiny),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(12.dp).background(colour.roles(dark).primary, CircleShape)
                        )
                        Text(colour.label)
                    }
                },
                enabled = !busy,
            )
        }
    }
}
