package org.racedaydreams.domain.body

import kotlin.math.floor
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.time.Instant

/** An exact evidence snapshot. Source metadata participates in duplicate/conflict comparisons. */
data class ProteinWeightEvidence(
    val id: String,
    val at: Instant,
    val kilograms: Double,
    val offsetSeconds: Int?,
    val provider: String,
    val sourceId: String,
    val origin: String?,
) {
    init {
        require(
            id.trim { it <= ' ' }.isNotEmpty() &&
                kilograms.isFinite() &&
                kilograms > 0 &&
                kilograms <= 1000
        ) {
            "Weight needs an identity, positive mass and provenance."
        }
    }
}

data class ProteinSelection(val id: String, val at: Instant, val kilograms: Double) {
    init {
        require(
            id.trim { it <= ' ' }.isNotEmpty() &&
                id.length <= 256 &&
                kilograms.isFinite() &&
                kilograms > 0 &&
                kilograms <= 1000
        ) {
            "Select a recorded weight."
        }
    }

    fun matches(weight: ProteinWeightEvidence): Boolean =
        id == weight.id && at == weight.at && kilograms == weight.kilograms
}

data class ProteinAdvice(
    val minimumGrams: Int?,
    val maximumGrams: Int?,
    val minimumPerKg: Double?,
    val maximumPerKg: Double?,
    val basis: ProteinWeightEvidence?,
    val instruction: String,
    val explanation: String,
    val limitation: String,
) {
    fun available(): Boolean = minimumGrams != null && maximumGrams != null && basis != null
}

/**
 * Existing adult guidance, freshness boundary and rounding. This is a port, not a new prescription.
 */
object ProteinPolicy {
    fun evaluate(
        enabled: Boolean,
        selection: ProteinSelection?,
        weights: List<ProteinWeightEvidence>,
        focus: PrimaryFocus?,
        now: Instant,
    ): ProteinAdvice {
        if (!enabled)
            return unavailable(
                "Protein guidance is off. It is optional and does not affect your training."
            )
        if (selection == null)
            return unavailable(
                "Choose a recorded body weight if you would like a general protein range."
            )
        var match: ProteinWeightEvidence? = null
        for (row in weights) if (row.id == selection.id) {
            require(match == null || match == row) { "Conflicting weight observations." }
            match = row
        }
        if (match == null || !selection.matches(match))
            return unavailable(
                "Your selected weight is no longer available or has changed. Review and choose a measurement again; no replacement was assumed."
            )
        if (match.at > now)
            return unavailable(
                "Your selected measurement is dated in the future. Review its date before using it."
            )
        if (olderThanNinetyDays(match.at, now))
            return unavailable(
                "Your selected weight is over 90 days old. Review a recent measurement before using a daily range."
            )
        if (match.kilograms < 30 || match.kilograms > 250)
            return unavailable(
                "This general adult training range may not be useful for your selected measurement. Individual nutrition advice may be more appropriate."
            )
        val low = if (focus == null || focus == PrimaryFocus.RUNNING) 1.2 else 1.4
        val high = if (focus == PrimaryFocus.RUNNING) 1.6 else 2.0
        val minimum = (floor(match.kilograms * low / 5.0 + 0.5).toLong() * 5).toInt()
        val maximum = (floor(match.kilograms * high / 5.0 + 0.5).toLong() * 5).toInt()
        val emphasis =
            when (focus) {
                null -> "general training"
                PrimaryFocus.RUNNING -> "running"
                PrimaryFocus.STRENGTH -> "strength"
                PrimaryFocus.HYBRID -> "running and strength"
            }
        return ProteinAdvice(
            minimum,
            maximum,
            low,
            high,
            match,
            "A general daily range is about $minimum–$maximum grams of protein.",
            "Based on your chosen body weight and $emphasis focus. This is general guidance, not a requirement or a weight-loss programme.",
            limitation(),
        )
    }

    private fun olderThanNinetyDays(at: Instant, now: Instant): Boolean {
        // The full supported instant span fits in Long seconds. Preserve the strict boundary
        // without routing retained evidence through a narrower, saturating timestamp factory.
        val seconds = now.getEpochSecond() - at.getEpochSecond()
        return seconds > 90 * 86400L || seconds == 90 * 86400L && now.getNano() > at.getNano()
    }

    private fun unavailable(why: String) =
        ProteinAdvice(
            null,
            null,
            null,
            null,
            null,
            why,
            "No numerical range is inferred from height, missing or stale weight, or an unreviewed provider measurement.",
            limitation(),
        )

    fun limitation(): String =
        "General guidance for adults training recreationally. Medical needs, pregnancy, age and dietary circumstances can change protein requirements; seek individual advice when appropriate. No supplement is required. The range is approximate, not a medical prescription."

    fun sources(): String =
        "ACSM/Academy of Nutrition and Dietetics/Dietitians of Canada (2016): athletes commonly need 1.2–2.0 g/kg/day. ISSN position stand (2017): 1.4–2.0 g/kg/day for most exercising adults. Focus-specific subranges, nearest-5-gram rounding and a 90-day freshness boundary are conservative original product choices, not individually measured requirements."
}
