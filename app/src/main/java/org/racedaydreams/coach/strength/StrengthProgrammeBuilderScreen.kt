package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.*
import java.time.format.*
import java.util.Locale
import org.racedaydreams.coach.friendlyDate
import org.racedaydreams.domain.strength.*

/** Original Android lifecycle, strict UK input parser and exact labels for shared authoring UI. */
@Composable
fun StrengthProgrammeBuilderScreen(
    model: StrengthProgrammeViewModel,
    onBack: () -> Unit,
    onActivated: () -> Unit,
    onCreateCustom: (() -> Unit)? = null,
    onTemplates: (() -> Unit)? = null,
) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedStrengthProgrammeBuilderScreen(
        model.shared,
        state,
        programmePresentation(),
        { action -> BackHandler(onBack = action) },
        onBack,
        onActivated,
        onCreateCustom,
        onTemplates,
    )
}

internal fun programmePresentation() =
    ProgrammeScreenPresentation(
        ::friendlyDate,
        { it.format(PROGRAMME_DATE) },
        { LocalDate.parse(it, PROGRAMME_DATE) },
        DayOfWeek::of,
        { day, short ->
            day.getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, Locale.UK)
        },
        ::gymExerciseContext,
        ::gymTargetDescription,
        { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() },
    )

private val PROGRAMME_DATE =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.UK).withResolverStyle(ResolverStyle.STRICT)

@Composable
internal fun ProgrammePreviewContent(programme: StrengthProgramme) =
    SharedProgrammePreviewContent(
        programme,
        ::friendlyDate,
        ::programmeLabel,
        ::gymTargetDescription,
    )
