package org.racedaydreams.storage

import org.racedaydreams.domain.body.BodyHeight
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.time.Instant

/**
 * HealthKit samples have immutable source UUIDs. Merge only visible evidence, never infer deletion
 * or replace a read window. Retained IDs, optional metadata and first import dates survive repeat
 * reads, including compatible old backups. This is not the authoritative Health Connect policy.
 */
class SqliteHealthKitBodyImport(database: SqlDatabase, now: () -> Instant, newId: () -> String) {
    private val database = database
    private val heights = SqliteBodyHeights(database)
    private val context = SqliteCoachingContext(database, now, newId)

    data class Result(val newHeights: Int, val newWeights: Int)

    fun merge(visibleHeights: List<BodyHeight>, visibleWeights: List<BodyWeight>): Result {
        require(visibleHeights.size <= 10000 && visibleWeights.size <= 10000) {
            "Body history exceeds this read limit. Nothing was truncated."
        }
        // Reject a changed source observation before any write. Duplicate identical SDK results
        // are harmless; conflicting identities are not silently resolved by list order.
        val incomingHeights = linkedMapOf<String, BodyHeight>()
        for (value in visibleHeights) {
            require(value.provider() == PROVIDER && value.offset() == null) {
                "A visible Apple Health height with its original unknown offset is required."
            }
            val prior = incomingHeights[value.sourceId()]
            require(prior == null || prior == value) { "Conflicting Apple Health height samples." }
            incomingHeights[value.sourceId()] = value
        }
        val incomingWeights = linkedMapOf<String, BodyWeight>()
        for (value in visibleWeights) {
            require(value.provider() == PROVIDER && value.offset().getTotalSeconds() == 0) {
                "A visible Apple Health weight with the existing UTC display fallback is required."
            }
            val prior = incomingWeights[value.sourceId()]
            require(prior == null || prior == value) { "Conflicting Apple Health weight samples." }
            incomingWeights[value.sourceId()] = value
        }
        require(
            incomingHeights.values.map { it.id() }.distinct().size == incomingHeights.size &&
                incomingWeights.values.map { it.id() }.distinct().size == incomingWeights.size
        ) {
            "Different Apple Health samples cannot share a retained identity."
        }
        if (incomingHeights.isEmpty() && incomingWeights.isEmpty()) return Result(0, 0)
        return database.transaction {
            val addHeights =
                incomingHeights.values.filter { value ->
                    val retained = heights.bySource(PROVIDER, value.sourceId())
                    if (retained != null) {
                        require(
                            retained.metres() == value.metres() &&
                                retained.at() == value.at() &&
                                compatibleOrigin(retained.origin(), value.origin())
                        ) {
                            "A retained height conflicts with its Apple Health source. History is unchanged."
                        }
                        // The SDK does not supply a source offset. Keep any legitimate older
                        // metadata
                        // and the original ID/import time rather than treating absence as a new
                        // fact.
                        false
                    } else {
                        require(heights.byId(value.id()) == null) {
                            "Height identity already belongs to another observation."
                        }
                        true
                    }
                }
            val retainedWeights = if (incomingWeights.isEmpty()) emptyList() else context.weights()
            val sourceWeights = retainedWeights.filter { it.provider() == PROVIDER }
            require(sourceWeights.map { it.sourceId() }.distinct().size == sourceWeights.size) {
                "Retained Apple Health weight identities are ambiguous. History is unchanged."
            }
            val bySource = sourceWeights.associateBy { it.sourceId() }
            val byId = retainedWeights.associateBy { it.id() }
            val addWeights =
                incomingWeights.values.filter { value ->
                    val retained = bySource[value.sourceId()]
                    if (retained != null) {
                        require(
                            retained.kilograms() == value.kilograms() &&
                                retained.at() == value.at() &&
                                compatibleOrigin(retained.origin(), value.origin())
                        ) {
                            "A retained weight conflicts with its Apple Health source. History is unchanged."
                        }
                        false
                    } else {
                        require(byId[value.id()] == null) {
                            "Weight identity already belongs to another observation."
                        }
                        true
                    }
                }
            addHeights.forEach(heights::saveHeight)
            addWeights.forEach(context::saveWeight)
            Result(addHeights.size, addWeights.size)
        }
    }

    private fun compatibleOrigin(retained: String?, visible: String?) =
        retained == null || visible == null || retained == visible

    companion object {
        const val PROVIDER = "health-kit"
    }
}
