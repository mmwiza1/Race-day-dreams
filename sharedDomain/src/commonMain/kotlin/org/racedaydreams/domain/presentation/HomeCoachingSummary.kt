package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.*

/** Reuses weekly evidence; no competing readiness score. Raw constructor aliases are retained. */
data class HomeCoachingSummary(
    private val title: String?,
    private val observations: List<String>?,
    private val recommendation: String?,
) {
    fun title(): String? = title

    fun observations(): List<String>? = observations

    fun recommendation(): String? = recommendation

    override fun toString(): String =
        "HomeCoachingSummary[title=$title, observations=$observations, recommendation=$recommendation]"

    companion object {
        @JvmStatic
        fun from(review: WeeklyCoaching.Review?): HomeCoachingSummary {
            if (review == null)
                return HomeCoachingSummary(
                    "Still learning your training",
                    Immutable.list("Record training and review your running baseline."),
                    "Continue gathering evidence",
                )
            val attention =
                review.channels().any { it.state() == WeeklyCoaching.State.NEEDS_ATTENTION }
            val known =
                review.channels().any { it.state() != WeeklyCoaching.State.INSUFFICIENT_EVIDENCE }
            val notes =
                mutableListOf("${review.completed()} / ${review.planned()} planned runs completed")
            for (c in review.channels()) if (
                c.state() != WeeklyCoaching.State.INSUFFICIENT_EVIDENCE && notes.size < 4
            )
                notes.add("${c.name()}: ${c.state()!!.name.lowercase().replace('_', ' ')}")
            return HomeCoachingSummary(
                if (attention) "Something may need adjusting"
                else if (known) "Training is on track" else "Still learning your training",
                Immutable.copyList(notes),
                review.recommendation(),
            )
        }
    }
}
