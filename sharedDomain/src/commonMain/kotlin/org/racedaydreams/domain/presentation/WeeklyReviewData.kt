package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.*

/** Existing groups for disclosure; never changes coaching outcomes. Raw record aliases retained. */
data class WeeklyReviewData(
    private val positives: List<String>?,
    private val attention: List<String>?,
    private val recommendation: String?,
    private val noChange: Boolean,
) {
    fun positives(): List<String>? = positives

    fun attention(): List<String>? = attention

    fun recommendation(): String? = recommendation

    fun noChange(): Boolean = noChange

    override fun toString(): String =
        "WeeklyReviewData[positives=$positives, attention=$attention, recommendation=$recommendation, noChange=$noChange]"

    companion object {
        @JvmStatic
        fun of(review: WeeklyCoaching.Review): WeeklyReviewData {
            val positive = mutableListOf<String>()
            val attention = mutableListOf<String>()
            for (c in review.channels()) if (
                c.state() == WeeklyCoaching.State.IMPROVING ||
                    c.state() == WeeklyCoaching.State.STABLE
            )
                positive.add("${c.name()}: ${c.state()!!.name.lowercase().replace('_', ' ')}")
            for (c in review.channels()) if (c.state() == WeeklyCoaching.State.NEEDS_ATTENTION)
                attention.add("${c.name()}: worth reviewing")
            return WeeklyReviewData(
                coachingReadOnlyList(positive),
                coachingReadOnlyList(attention),
                review.recommendation(),
                requireNotNull(review.recommendation()).startsWith("No adjustment required"),
            )
        }
    }
}
