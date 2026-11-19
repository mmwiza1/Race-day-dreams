package org.racedaydreams.domain.baseline

/** Runner-selected assessment; recommendations never restrict the athlete's choice. */
data class BaselineAssessment(private val distanceKm: Int, private val mode: AssessmentMode?) {
    init {
        require(distanceKm in 1..5) { "Choose an assessment from 1K to 5K." }
        if (mode == null) throw NullPointerException("Assessment mode")
    }

    fun distanceKm(): Int = distanceKm

    fun mode(): AssessmentMode = mode!!

    override fun toString(): String = "BaselineAssessment[distanceKm=$distanceKm, mode=$mode]"
}
