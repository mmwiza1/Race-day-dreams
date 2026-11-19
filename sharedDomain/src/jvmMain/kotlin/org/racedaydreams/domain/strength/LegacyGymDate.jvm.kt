package org.racedaydreams.domain.strength

import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.domain.time.LocalDate

/** Keep the exact existing Android/JVM formatter and platform UK locale data. */
internal actual fun formatGymProgrammeDate(day: LocalDate): String =
    day.format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK))
