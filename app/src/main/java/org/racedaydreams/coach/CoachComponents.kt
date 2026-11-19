package org.racedaydreams.coach

import java.time.ZoneOffset

fun displayActivityDate(activity: org.racedaydreams.domain.activity.ActivityRecord): String =
    activity
        .start()
        .atOffset(activity.startOffset() ?: ZoneOffset.UTC)
        .toLocalDate()
        .let(::friendlyDate)
