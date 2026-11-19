package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.*

/** Original descriptive halves, never an injury prediction or an invented recovery score. */
data class LongRunDurability(
    private val sufficient: Boolean,
    private val paceFadePercent: Double?,
    private val heartRateDriftPercent: Double?,
    private val cadenceChangePercent: Double?,
    private val explanation: String?,
) {
    fun sufficient(): Boolean = sufficient

    fun paceFadePercent(): Double? = paceFadePercent

    fun heartRateDriftPercent(): Double? = heartRateDriftPercent

    fun cadenceChangePercent(): Double? = cadenceChangePercent

    fun explanation(): String? = explanation

    override fun toString(): String =
        "LongRunDurability[sufficient=$sufficient, paceFadePercent=$paceFadePercent, heartRateDriftPercent=$heartRateDriftPercent, cadenceChangePercent=$cadenceChangePercent, explanation=$explanation]"

    companion object {
        @JvmStatic
        fun analyse(run: ActivityRecord): LongRunDurability {
            val series = SampleSeries(run)
            if (
                series.quality != SampleQuality.QUALIFIED ||
                    run.samples().size < 4 ||
                    series.totalsDisagree
            )
                return LongRunDurability(
                    false,
                    null,
                    null,
                    null,
                    "Insufficient reliable continuous samples for durability analysis.",
                )
            val samples = run.samples()
            val first = samples.first()
            val last = samples.last()
            val seconds = last.elapsed().minus(first.elapsed()).toMillis() / 1000.0
            if (
                seconds < 1800 ||
                    first.elapsed().toMillis() > 30000 ||
                    last.elapsed().toMillis() < run.elapsedTime().toMillis() - 30000
            )
                return LongRunDurability(
                    false,
                    null,
                    null,
                    null,
                    "At least 30 minutes with samples covering the run are needed.",
                )
            val half = first.elapsed().toMillis() / 1000.0 + seconds / 2
            val distance = nullDistance(samples, half)
            val d1 = distance - first.distanceMetres()
            val d2 = last.distanceMetres() - distance
            if (d1 <= 0 || d2 <= 0)
                return LongRunDurability(
                    false,
                    null,
                    null,
                    null,
                    "Insufficient distance in both halves.",
                )
            val pace1 = seconds / 2 / d1
            val pace2 = seconds / 2 / d2
            val h1 = average(samples, half, true, true)
            val h2 = average(samples, half, false, true)
            val c1 = average(samples, half, true, false)
            val c2 = average(samples, half, false, false)
            val fade = (pace2 / pace1 - 1) * 100
            val drift =
                if (h1 == null || h2 == null) null else (h2 * pace2 / (h1 * pace1) - 1) * 100
            return LongRunDurability(
                true,
                fade,
                drift,
                if (c1 == null || c2 == null) null else (c2 / c1 - 1) * 100,
                if (fade > 8)
                    "The second half was slower. Terrain, conditions and intention can also explain this; review alongside effort."
                else
                    "Late-run pace was relatively stable or faster. This alone does not prove improved fitness.",
            )
        }

        private fun nullDistance(samples: List<DistanceSample>, at: Double): Double {
            for (i in 1 until samples.size) {
                val t = samples[i].elapsed().toMillis() / 1000.0
                if (t >= at) {
                    val a = samples[i - 1]
                    val f =
                        (at - a.elapsed().toMillis() / 1000.0) /
                            (t - a.elapsed().toMillis() / 1000.0)
                    return a.distanceMetres() +
                        f * (samples[i].distanceMetres() - a.distanceMetres())
                }
            }
            return samples.last().distanceMetres()
        }

        private fun average(
            samples: List<DistanceSample>,
            half: Double,
            early: Boolean,
            hr: Boolean,
        ): Double? {
            var sum = 0.0
            var coverage = 0.0
            var total = 0.0
            for (i in 1 until samples.size) {
                val a = samples[i - 1]
                val b = samples[i]
                val from = a.elapsed().toMillis() / 1000.0
                val to = b.elapsed().toMillis() / 1000.0
                val w =
                    if (early) max(0.0, min(to, half) - from) else max(0.0, to - max(from, half))
                total += w
                val value = if (hr) b.heartRate() else b.cadence()
                if (value != null && value > 0) {
                    sum += value * w
                    coverage += w
                }
            }
            return if (coverage == 0.0 || coverage < total * .8) null else sum / coverage
        }
    }
}
