package org.racedaydreams.domain

import org.racedaydreams.domain.time.*

/** Original evidence/choice contract, shared without changing validation or history. */
data class GymSession(
    private var id: String?,
    private val startedAt: Instant?,
    private val type: Type?,
    private val duration: Duration?,
    private val effort: Effort?,
    private val notes: String?,
    private val source: Source?,
    private val externalId: String?,
) {

    enum class Type {
        UPPER_BODY,
        LOWER_BODY,
        FULL_BODY,
        FUNCTIONAL_FITNESS,
        OTHER,
    }

    enum class Effort {
        EASY,
        MODERATE,
        HARD,
    }

    enum class Source {
        MANUAL,
        HEALTH_CONNECT,
        OTHER_PROVIDER,
    }

    init {

        id = DomainChecks.id(id, "gym session id")
        if (startedAt == null) throw NullPointerException("startedAt")
        if (type == null) throw NullPointerException("type")
        if (duration == null) throw NullPointerException("duration")
        DomainChecks.nonNegative(duration, "duration")
        if (effort == null) throw NullPointerException("effort")
        if (source == null) throw NullPointerException("source")
    }

    fun id(): String = id!!

    fun startedAt(): Instant = startedAt!!

    fun type(): Type = type!!

    fun duration(): Duration = duration!!

    fun effort(): Effort = effort!!

    fun notes(): String? = notes

    fun source(): Source = source!!

    fun externalId(): String? = externalId

    override fun toString(): String =
        "GymSession[id=$id, startedAt=$startedAt, type=$type, duration=$duration, effort=$effort, notes=$notes, source=$source, externalId=$externalId]"
}
