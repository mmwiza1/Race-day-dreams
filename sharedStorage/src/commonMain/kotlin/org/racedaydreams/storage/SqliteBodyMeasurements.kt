package org.racedaydreams.storage

import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.time.Instant

/** Existing body interface over the application's one pool; no second history or import owner. */
class SqliteBodyMeasurements(
    private val database: SqlDatabase,
    now: () -> Instant,
    newId: () -> String,
) : BodyMeasurementsRepository {
    private val heights = SqliteBodyHeights(database)
    private val context = SqliteCoachingContext(database, now, newId)

    override fun heights(limit: Int, offset: Int): List<BodyHeight> = heights.heights(limit, offset)

    override fun weights(): List<BodyWeight> = context.weights()

    override fun saveHeight(value: BodyHeight) = heights.saveHeight(value)

    override fun deleteHeight(id: String) = heights.deleteHeight(id)

    override fun saveWeight(value: BodyWeight) = context.saveWeight(value)

    override fun preferences(): Preferences = heights.preferences()

    override fun preferences(value: Preferences) = heights.preferences(value)

    /**
     * Only completed authoritative Health Connect windows may replace evidence. Apple Health's
     * selected visible-only reads already merge through SqliteHealthKitBodyImport in the gateway.
     */
    override fun replaceImportedWindow(
        provider: String,
        from: Instant,
        until: Instant,
        heights: List<BodyHeight>?,
        weights: List<BodyWeight>?,
    ) {
        require(provider == "health-connect") {
            "Only an authorised Health Connect window can replace body history. Apple Health reads must merge visible observations without removing retained measurements."
        }
        require(from.isBefore(until)) { "Invalid import window." }
        if (heights != null) {
            for (row in heights) require(
                row.provider() == provider && !row.at().isBefore(from) && row.at().isBefore(until)
            ) {
                "Height is outside its authorised window."
            }
            BodyMeasurementsArchiveJson.Snapshot(heights, Preferences.defaults())
        }
        if (weights != null) {
            val ids = mutableSetOf<String>()
            for (row in weights) require(
                row.provider() == provider &&
                    !row.at().isBefore(from) &&
                    row.at().isBefore(until) &&
                    ids.add(row.id())
            ) {
                "Weight is outside its authorised window or duplicated."
            }
        }
        database.transaction {
            if (heights != null) {
                val remove =
                    database
                        .query(
                            "SELECT id,observed_at FROM body_heights WHERE provider=?",
                            listOf(SqlValue.Text(provider)),
                        )
                        .filter {
                            val at = StoredInstant.parse(it[1].sqlText())
                            !at.isBefore(from) && at.isBefore(until)
                        }
                        .map { it[0].sqlText() }
                for (id in remove) this.heights.deleteHeight(id)
                for (row in heights) this.heights.saveHeight(row)
            }
            if (weights != null) context.replaceWeightWindow(provider, from, until, weights)
        }
    }
}
