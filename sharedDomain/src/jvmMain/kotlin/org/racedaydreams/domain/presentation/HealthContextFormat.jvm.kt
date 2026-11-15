package org.racedaydreams.domain.presentation

import java.util.Locale

/** Retains the original Android formatter exactly. */
internal actual fun healthWhole(value: Double): String = String.format(Locale.UK, "%.0f", value)
