package org.racedaydreams.coach.strength

fun programmeLabel(value: String) =
    when (value) {
        "SUPPORT_RUNNING" -> "Support my running"
        "STRENGTH" -> "Get stronger"
        "MUSCLE" -> "Build muscle"
        "POWER" -> "Power / athletic performance"
        "GENERAL" -> "General strength & fitness"
        else -> value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
