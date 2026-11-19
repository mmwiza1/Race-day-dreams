package org.racedaydreams.domain

enum class GoalMode(private val displayLabel: String) {
    FINISH("Finish"),
    TARGET_TIME("Target Time"),
    MAXIMUM_PERFORMANCE("Maximum Performance");

    fun label(): String = displayLabel
}
