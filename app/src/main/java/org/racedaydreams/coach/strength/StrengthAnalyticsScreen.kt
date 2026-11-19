package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.coach.decimal
import org.racedaydreams.domain.presentation.AthleteLanguage
import org.racedaydreams.domain.strength.StrengthMeasurements
import org.racedaydreams.domain.strength.StrengthSession

/** Platform clock and retained UK display formatting for the existing shared production screen. */
@Composable
fun StrengthAnalyticsScreen(
    sessions: List<StrengthSession>,
    onBack: () -> Unit,
    asOf: Instant = Instant.now(),
    today: LocalDate = LocalDate.now(),
) {
    val date = remember { DateTimeFormatter.ofPattern("d MMM", Locale.UK) }
    SharedStrengthAnalyticsScreen(
        sessions,
        onBack,
        asOf,
        today,
        { it.format(date) },
        ::decimal,
        StrengthMeasurements::duration,
        AthleteLanguage.WORKING_SETS,
    )
}
