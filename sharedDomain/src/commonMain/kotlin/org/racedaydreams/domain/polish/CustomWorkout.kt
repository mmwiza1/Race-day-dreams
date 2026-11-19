package org.racedaydreams.domain.polish

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.execution.WorkoutExecutionEngine

/** Original athlete-authored prescription; never programme membership or actual completion. */
data class CustomWorkout(
    private val id: String?,
    private val title: String?,
    private var groups: List<Group>?,
) {
    data class Group(
        private val repeats: Int,
        private var steps: List<WorkoutExecutionEngine.Step>?,
    ) {
        init {
            require(repeats in 1..100 && steps != null && steps!!.isNotEmpty()) {
                "Choose steps and 1–100 repeats"
            }
            steps = Immutable.copyList(steps!!)
        }

        fun repeats(): Int = repeats

        fun steps(): List<WorkoutExecutionEngine.Step> = steps!!

        override fun toString(): String = "Group[repeats=$repeats, steps=$steps]"
    }

    data class Totals(
        private val knownMetres: Double,
        private val knownMillis: Long,
        private val distanceComplete: Boolean,
        private val durationComplete: Boolean,
    ) {
        fun knownMetres(): Double = knownMetres

        fun knownMillis(): Long = knownMillis

        fun distanceComplete(): Boolean = distanceComplete

        fun durationComplete(): Boolean = durationComplete

        override fun toString(): String =
            "Totals[knownMetres=$knownMetres, knownMillis=$knownMillis, distanceComplete=$distanceComplete, durationComplete=$durationComplete]"
    }

    init {
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                id.length <= 256 &&
                title != null &&
                title.trim { it <= ' ' }.isNotEmpty() &&
                title.length <= 120 &&
                groups != null &&
                groups!!.isNotEmpty()
        ) {
            "Name the workout and add a step"
        }
        groups = Immutable.copyList(groups!!)
        for (g in groups!!) for (s in g.steps()) require(
            !(s.metres() != null && s.metres()!! > 300000 ||
                s.millis() != null && s.millis()!! > 21600000)
        ) {
            "A step must be at most 300 km or six hours"
        }
        var n = 0
        for (g in groups!!) n += g.repeats() * g.steps().size
        require(n <= 500) { "At most 500 executed steps" }
    }

    fun id(): String = id!!

    fun title(): String = title!!

    fun groups(): List<Group> = groups!!

    override fun toString(): String = "CustomWorkout[id=$id, title=$title, groups=$groups]"

    fun flattened(): List<WorkoutExecutionEngine.Step> {
        val out = mutableListOf<WorkoutExecutionEngine.Step>()
        for (g in groups!!) repeat(g.repeats()) { out.addAll(g.steps()) }
        return Immutable.copyList(out)
    }

    fun totals(): Totals {
        var metres = 0.0
        var millis = 0L
        var allMetres = true
        var allMillis = true
        for (s in flattened()) {
            val m = s.metres()
            if (m != null) metres += m else allMetres = false
            val t = s.millis()
            if (t != null) millis += t else allMillis = false
        }
        return Totals(metres, millis, allMetres, allMillis)
    }

    fun move(from: Int, to: Int): CustomWorkout {
        val rows = groups!!.toMutableList()
        val group = rows.removeAt(from)
        rows.add(to, group)
        return CustomWorkout(id, title, rows)
    }
}
