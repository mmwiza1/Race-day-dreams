package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Session effort supplies context, never an injury prediction. Unknown remains unknown. */
data class TrainingLoadEvidence(
    private val runningLoad: Double?,
    private val runsWithRpe: Int,
    private val runsObserved: Int,
    private val strengthLoad: Double?,
    private val strengthWithRpe: Int,
    private val strengthObserved: Int,
    private val from: Instant?,
    private val to: Instant?,
) {
    fun runningLoad(): Double? = runningLoad

    fun runsWithRpe(): Int = runsWithRpe

    fun runsObserved(): Int = runsObserved

    fun strengthLoad(): Double? = strengthLoad

    fun strengthWithRpe(): Int = strengthWithRpe

    fun strengthObserved(): Int = strengthObserved

    fun from(): Instant? = from

    fun to(): Instant? = to

    override fun toString(): String =
        "TrainingLoadEvidence[runningLoad=$runningLoad, runsWithRpe=$runsWithRpe, runsObserved=$runsObserved, strengthLoad=$strengthLoad, strengthWithRpe=$strengthWithRpe, strengthObserved=$strengthObserved, from=$from, to=$to]"

    companion object {
        @JvmStatic
        fun window(
            runs: List<ActivityRecord>,
            strength: List<StrengthSession>,
            from: Instant,
            to: Instant,
        ): TrainingLoadEvidence {
            require(to.isAfter(from)) { "Workload window must have positive duration." }
            var running = 0.0
            var gym = 0.0
            var n = 0
            var k = 0
            var observed = 0
            var gymObserved = 0
            for (r in runs) if (!r.end().isBefore(from) && r.end().isBefore(to)) {
                observed++
                val rpe = r.rpe()
                if (rpe != null) {
                    n++
                    running += r.elapsedTime().toMillis() / 60000.0 * rpe
                }
            }
            for (s in strength) if (!s.startedAt().isBefore(from) && s.startedAt().isBefore(to)) {
                gymObserved++
                val load = StrengthAnalytics.sessionLoad(s)
                if (load != null) {
                    k++
                    gym += load
                }
            }
            return TrainingLoadEvidence(
                if (n == 0) null else running,
                n,
                observed,
                if (k == 0) null else gym,
                k,
                gymObserved,
                from,
                to,
            )
        }
    }
}
