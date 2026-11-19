package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.activity.roundedNanos
import org.racedaydreams.domain.support.DreamRoutes

/** Retained marathon-equivalent assignment; never changes geometry or physical GPS evidence. */
data class DreamRouteProgress(
    private val highlightedMetres: Double,
    private val fraction: Double,
    private val percentage: Int,
    private val extraMetres: Double,
    private val distanceLabel: String?,
) {
    fun highlightedMetres(): Double = highlightedMetres

    fun fraction(): Double = fraction

    fun percentage(): Int = percentage

    fun extraMetres(): Double = extraMetres

    fun distanceLabel(): String? = distanceLabel

    override fun toString(): String =
        "DreamRouteProgress[highlightedMetres=$highlightedMetres, fraction=$fraction, percentage=$percentage, extraMetres=$extraMetres, distanceLabel=$distanceLabel]"

    fun remainingMetres(): Double = max(0.0, MARATHON_METRES - highlightedMetres)

    fun totalMetres(): Double = MARATHON_METRES

    fun completedDistanceLabel(): String = kilometres(highlightedMetres)

    fun remainingDistanceLabel(): String = kilometres(remainingMetres())

    fun totalDistanceLabel(): String = kilometres(MARATHON_METRES)

    fun completionLabel(): String = "$percentage% complete"

    fun equivalenceStatement(courseName: String?): String {
        val statement =
            "You covered the equivalent of ${completedDistanceLabel()} of the ${courseName(courseName)} today."
        return if (extraMetres > 0)
            statement +
                " Your run also included ${kilometres(extraMetres)} beyond one marathon equivalent."
        else statement
    }

    fun accessibilitySummary(courseName: String?): String =
        courseName(courseName) +
            ". " +
            completionLabel() +
            ". " +
            completedDistanceLabel() +
            " completed; " +
            remainingDistanceLabel() +
            " remaining; " +
            totalDistanceLabel() +
            " total marathon-equivalent distance." +
            (if (extraMetres > 0)
                " Plus " + kilometres(extraMetres) + " beyond one marathon equivalent."
            else "") +
            " This is an equivalent section, not your physical location."

    private fun kilometres(metres: Double): String = dreamDecimalTwo(metres / 1000) + " km"

    private fun courseName(value: String?): String {
        require(value != null && value.trim { it <= ' ' }.isNotEmpty()) {
            "Missing saved course name"
        }
        return value.trim { it <= ' ' }
    }

    companion object {
        const val MARATHON_METRES = 42195.0

        @JvmStatic
        fun of(assignment: DreamRoutes.Assignment): DreamRouteProgress {
            val distance = assignment.coveredMetres()
            val highlighted = min(MARATHON_METRES, distance)
            val fraction = highlighted / MARATHON_METRES
            return DreamRouteProgress(
                highlighted,
                fraction,
                roundedNanos(fraction * 100).toInt(),
                max(0.0, distance - MARATHON_METRES),
                dreamDecimalTwo(highlighted / 1000) + " km / 42.20 km",
            )
        }
    }
}

internal expect fun dreamDecimalTwo(value: Double): String
