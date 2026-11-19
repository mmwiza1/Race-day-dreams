package org.racedaydreams.domain.race

import java.util.Locale
import org.racedaydreams.domain.activity.ResultSource

internal actual fun raceSourceText(source: ResultSource): String =
    source.name.lowercase(Locale.getDefault()).replace('_', ' ')
