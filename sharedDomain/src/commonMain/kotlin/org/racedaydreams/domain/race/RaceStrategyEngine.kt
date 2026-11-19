package org.racedaydreams.domain.race

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable

/** Original target-based strategy, independent of predictions and never a goal mutation. */
object RaceStrategyEngine {
    data class Stage(
        private val from: Double,
        private val to: Double,
        private val fastPace: Double?,
        private val slowPace: Double?,
        private val guidance: String?,
    ) {
        fun from(): Double = from

        fun to(): Double = to

        fun fastPace(): Double? = fastPace

        fun slowPace(): Double? = slowPace

        fun guidance(): String? = guidance

        override fun toString(): String =
            "Stage[from=$from, to=$to, fastPace=$fastPace, slowPace=$slowPace, guidance=$guidance]"
    }

    @JvmStatic
    fun create(metres: Double, targetSeconds: Long?, hilly: Boolean): List<Stage> {
        require(metres.isFinite() && metres > 0 && (targetSeconds == null || targetSeconds > 0)) {
            "Invalid race target"
        }
        if (hilly || targetSeconds == null)
            return Immutable.list(
                Stage(
                    0.0,
                    metres,
                    null,
                    null,
                    "Run by effort. Keep the opening controlled; progress late only if comfortable. No grade-adjusted pace is inferred.",
                )
            )
        val pace = targetSeconds / (metres / 1000)
        return arrayOf(
                Stage(
                    0.0,
                    metres * 0.15,
                    pace * 1.01,
                    pace * 1.035,
                    "Controlled opening; avoid chasing seconds.",
                ),
                Stage(
                    metres * 0.15,
                    metres * 0.85,
                    pace * 0.995,
                    pace * 1.01,
                    "Settle into your chosen target. Conditions and effort take precedence.",
                ),
                Stage(
                    metres * 0.85,
                    metres,
                    null,
                    null,
                    "Hold rhythm. Progress only if comfortable; no sprint is required.",
                ),
            )
            .asList()
    }
}
