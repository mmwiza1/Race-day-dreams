package org.racedaydreams.domain.body

import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.ZoneOffset

/** Recorded mass, never readiness evidence or an automatically chosen nutrition basis. */
data class BodyWeight(
    private val id: String?,
    private val kilograms: Double,
    private val at: Instant?,
    private val offset: ZoneOffset?,
    private val provider: String?,
    private val sourceId: String?,
    private val origin: String?,
) {
    init {
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                kilograms.isFinite() &&
                kilograms > 0 &&
                kilograms <= 1000 &&
                provider != null &&
                sourceId != null
        ) {
            "Weight needs an identity, positive mass and provenance."
        }
        if (at == null) throw NullPointerException()
        if (offset == null) throw NullPointerException()
    }

    fun id(): String = id!!

    fun kilograms(): Double = kilograms

    fun at(): Instant = at!!

    fun offset(): ZoneOffset = offset!!

    fun provider(): String = provider!!

    fun sourceId(): String = sourceId!!

    fun origin(): String? = origin

    override fun toString(): String =
        "BodyWeight[id=$id, kilograms=$kilograms, at=$at, offset=$offset, provider=$provider, sourceId=$sourceId, origin=$origin]"
}
