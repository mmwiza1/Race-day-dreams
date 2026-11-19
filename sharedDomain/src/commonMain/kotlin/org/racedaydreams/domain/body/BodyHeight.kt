package org.racedaydreams.domain.body

import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.ZoneOffset

/** Optional retained observation. Missing source offset/import time is unknown, not fabricated. */
data class BodyHeight(
    private val id: String?,
    private val metres: Double,
    private val at: Instant?,
    private val offset: ZoneOffset?,
    private val provider: String?,
    private val sourceId: String?,
    private val origin: String?,
    private val importedAt: Instant?,
) {
    init {
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                id.length <= 256 &&
                metres.isFinite() &&
                metres > 0 &&
                metres <= 3 &&
                provider != null &&
                provider.trim { it <= ' ' }.isNotEmpty() &&
                provider.length <= 256 &&
                sourceId != null &&
                sourceId.trim { it <= ' ' }.isNotEmpty() &&
                sourceId.length <= 512 &&
                (origin == null || origin.length <= 512)
        ) {
            "Height needs a valid measurement and source identity."
        }
        if (at == null) throw NullPointerException()
    }

    fun id(): String = id!!

    fun metres(): Double = metres

    fun at(): Instant = at!!

    fun offset(): ZoneOffset? = offset

    fun provider(): String = provider!!

    fun sourceId(): String = sourceId!!

    fun origin(): String? = origin

    fun importedAt(): Instant? = importedAt

    override fun toString(): String =
        "BodyHeight[id=$id, metres=$metres, at=$at, offset=$offset, provider=$provider, sourceId=$sourceId, origin=$origin, importedAt=$importedAt]"
}
