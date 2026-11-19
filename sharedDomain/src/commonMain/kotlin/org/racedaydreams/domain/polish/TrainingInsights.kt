package org.racedaydreams.domain.polish

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/** Original retained-observation windows. An absent record is never a missed planned workout. */
object TrainingInsights {
    data class Insight(private val title: String?, private val evidence: String?) {
        fun title(): String? = title

        fun evidence(): String? = evidence

        override fun toString(): String = "Insight[title=$title, evidence=$evidence]"
    }

    @JvmStatic
    fun extraRunDemand(run: ActivityRecord): Boolean =
        (run.distanceMetres()?.let { it >= 8000 } ?: false) ||
            (run.rpe()?.let { it >= 7 } ?: false) ||
            (run.movingTime()?.let { it >= Durations.ofMinutes(45) } ?: false)

    @JvmStatic
    fun from(runs: List<ActivityRecord>, now: Instant): List<Insight> {
        val recent = mutableListOf<ActivityRecord>()
        val prior = mutableListOf<ActivityRecord>()
        for (r in runs) if (!r.start().isAfter(now) && r.distanceMetres() != null) {
            if (!r.start().isBefore(before(now, 28))) recent.add(r)
            else if (!r.start().isBefore(before(now, 56))) prior.add(r)
        }
        val out = mutableListOf<Insight>()
        if (recent.size < 4 || prior.size < 4) return out
        val a = coachingSum(recent.map { it.distanceMetres()!! }) / 1000
        val b = coachingSum(prior.map { it.distanceMetres()!! }) / 1000
        out.add(
            Insight(
                if (abs(a - b) < max(2.0, b * 0.05)) "Recorded running volume is broadly stable"
                else if (a > b) "Recorded running volume increased"
                else "Recorded running volume decreased",
                "Recent 28 days: ${performanceDecimal(a)} km across ${recent.size} runs. Previous 28 days: ${performanceDecimal(b)} km across ${prior.size} runs. Recording coverage can differ; this is not an injury-risk assessment.",
            )
        )
        val high = recent.count { it.rpe()?.let { v -> v >= 8 } ?: false }.toLong()
        if (high >= 3)
            out.add(
                Insight(
                    "Several high-effort runs recorded",
                    "$high runs at reported RPE 8–10 in the last 28 days. Purpose and your own response matter; no automatic change is made.",
                )
            )
        return coachingReadOnlyList(out)
    }

    @JvmStatic
    fun shoes(
        shoe: Shoe,
        runs: List<ActivityRecord>,
        now: Instant,
        discomfort: Set<String>,
    ): List<String> {
        var allLong = 0
        var shoeLong = 0
        var recent = 0
        var noted = 0
        var longest = 0.0
        for (r in runs) {
            val distance = r.distanceMetres()
            if (distance != null && distance >= 12000) {
                allLong++
                if (shoe.id() == r.shoeId()) shoeLong++
            }
            if (shoe.id() == r.shoeId()) {
                if (r.id() in discomfort) noted++
                if (!r.start().isBefore(before(now, 28)) && !r.start().isAfter(now)) {
                    recent++
                    if (distance != null) longest = max(longest, distance)
                }
            }
        }
        val out = mutableListOf<String>()
        out.add("$recent assigned runs in the last 28 days.")
        if (longest > 0)
            out.add("Longest recent assigned run: ${performanceDecimal(longest/1000)} km.")
        if (allLong > 0)
            out.add(
                "$shoeLong of $allLong recorded runs of at least 12 km used this shoe. The 12 km grouping is a display heuristic."
            )
        if (noted > 0)
            out.add(
                "You noted discomfort on $noted run${if(noted==1)"" else "s"} while wearing this shoe. This does not establish a cause."
            )
        return out
    }

    private fun before(now: Instant, days: Long): Instant =
        Instants.ofEpochSecond(now.getEpochSecond() - days * 86400, now.getNano().toLong())
}
