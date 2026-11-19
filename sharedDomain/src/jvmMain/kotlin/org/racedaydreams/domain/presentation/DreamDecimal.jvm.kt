package org.racedaydreams.domain.presentation

import java.util.Locale

internal actual fun dreamDecimalTwo(value: Double): String = String.format(Locale.UK, "%.2f", value)
