package org.racedaydreams.domain.activity

/**
 * Same provider-neutral identity and Java-callable accessors; no provider ID becomes an activity
 * ID.
 */
class SourceIdentity(provider: String?, recordId: String?, private val origin: String?) {
    private val source = requiredEvidenceText(provider, "Source provider")
    private val identifier = requiredEvidenceText(recordId, "Source record ID")

    fun provider(): String = source

    fun recordId(): String = identifier

    fun originPackage(): String? = origin

    override fun equals(other: Any?): Boolean =
        other is SourceIdentity &&
            source == other.source &&
            identifier == other.identifier &&
            origin == other.origin

    override fun hashCode(): Int =
        (stableTextHash(source) * 31 + stableTextHash(identifier)) * 31 +
            (origin?.let(::stableTextHash) ?: 0)

    override fun toString(): String =
        "SourceIdentity[provider=$source, recordId=$identifier, originPackage=$origin]"
}

/** Existing Java validation checks ASCII whitespace but deliberately does not normalise IDs. */
internal fun requiredEvidenceText(value: String?, name: String): String {
    require(value != null && value.trim { it <= ' ' }.isNotEmpty()) { "$name is required." }
    return value
}

/** Java-record String hashing, explicitly stable across JVM and Native. */
internal fun stableTextHash(value: String): Int =
    value.fold(0) { hash, character -> hash * 31 + character.code }
