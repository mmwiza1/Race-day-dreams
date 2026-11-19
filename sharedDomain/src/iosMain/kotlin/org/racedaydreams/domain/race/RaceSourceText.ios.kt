package org.racedaydreams.domain.race

import org.racedaydreams.domain.activity.ResultSource

internal actual fun raceSourceText(source: ResultSource): String =
    source.name.lowercase().replace('_', ' ')
