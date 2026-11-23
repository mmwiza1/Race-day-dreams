package org.racedaydreams.coach

import org.racedaydreams.domain.activity.ActivityDates

fun displayActivityDate(activity: org.racedaydreams.domain.activity.ActivityRecord): String =
    friendlyDate(ActivityDates.localDate(activity))
