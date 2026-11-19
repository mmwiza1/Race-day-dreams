package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.RemovedRecordingDetail

class DetailRetentionArchiveJsonTest {
    private val at = StoredInstant.parse("2026-11-16T10:00:00.123456789Z")

    private fun receipt(id: String = "run", activity: String = "activity") =
        RemovedRecordingDetail(
            id,
            activity,
            "rdd",
            id,
            at,
            at,
            DetailRetention.Policy.LAST_24_MONTHS,
            at,
            9007199254740993L,
            9007199254741993L,
            2,
            1,
            "ab".repeat(32),
            "cd".repeat(32),
        )

    private fun snapshot() =
        DetailRetentionArchiveJson.Snapshot(
            DetailRetention.Preferences.acknowledged(
                DetailRetention.Policy.LAST_24_MONTHS,
                at,
                "choice",
            ),
            listOf(receipt()),
        )

    private fun root() = DetailRetentionArchiveJson.element(snapshot())

    private fun changed(root: JsonObject, key: String, value: JsonElement) =
        JsonObject(root + (key to value))

    @Test
    fun olderMissingSectionRemainsProtectedWithoutFabricatedChoice() {
        val old = DetailRetentionArchiveJson.decode(null)
        assertEquals(DetailRetention.Preferences.existing(), old.preferences)
        assertTrue(old.removals.isEmpty())
        assertEquals(
            old.preferences,
            DetailRetentionArchiveJson.decode(DetailRetentionArchiveJson.element(old)).preferences,
        )
    }

    @Test
    fun allPoliciesAndExplicitChoiceIdentitySurviveMetadataRoundTrip() {
        val preferences =
            DetailRetention.Policy.entries.map {
                DetailRetention.Preferences.acknowledged(it, at, "choice-${it.name}")
            } +
                listOf(
                    DetailRetention.Preferences.fresh(at, "installation"),
                    DetailRetention.Preferences.existing(),
                )
        for (p in preferences) {
            val element =
                DetailRetentionArchiveJson.element(
                    DetailRetentionArchiveJson.Snapshot(p, emptyList())
                )
            assertEquals(p, DetailRetentionArchiveJson.decode(element).preferences)
        }
    }

    @Test
    fun originalProofsPreciseTimesAndMonotonicLongsAreNotReconstructedOrRounded() {
        val decoded = DetailRetentionArchiveJson.decode(root())
        assertEquals(snapshot().preferences, decoded.preferences)
        assertEquals(listOf(receipt()), decoded.removals)
        assertEquals(
            "9007199254740993",
            (root()["removals"] as JsonArray)[0].jsonObject["mono"].toString(),
        )
        assertEquals(root(), DetailRetentionArchiveJson.element(decoded))
    }

    @Test
    fun unsupportedFieldsVersionsAndMalformedChoicesFailBeforeReplacement() {
        val original = root()
        for (bad in
            listOf(
                changed(original, "version", JsonPrimitive(2)),
                changed(original, "version", JsonPrimitive(1.5)),
                changed(original, "version", JsonPrimitive("1")),
                changed(original, "future", JsonPrimitive(true)),
                JsonObject(original - "choiceId"),
                changed(original, "choice", JsonPrimitive("EXISTING_UNREVIEWED")),
                changed(original, "chosenAt", JsonNull),
                changed(original, "removals", JsonNull),
            )) assertFails { DetailRetentionArchiveJson.decode(bad) }
        assertEquals(1, DetailRetentionArchiveJson.decode(original).removals.size)
    }

    @Test
    fun removedEvidenceRequiresExactIntegersIdentityHashesAndValidDates() {
        val original = DetailRetentionArchiveJson.element(receipt())
        for (bad in
            listOf(
                changed(original, "mono", JsonPrimitive(9007199254740993.0)),
                changed(original, "points", JsonPrimitive("2")),
                changed(original, "record", JsonPrimitive("another")),
                changed(original, "provider", JsonPrimitive("health-connect")),
                changed(original, "policy", JsonPrimitive("KEEP_FOREVER")),
                changed(original, "pointsHash", JsonPrimitive("unknown")),
                changed(original, "end", JsonPrimitive("not a date")),
                changed(original, "extra", JsonNull),
                JsonObject(original - "eventsHash"),
            )) assertFails { DetailRetentionArchiveJson.removal(bad) }
    }

    @Test
    fun duplicateSessionsActivitiesAndExcessiveRowsNeverTruncate() {
        val p = snapshot().preferences
        for (rows in
            listOf(
                listOf(receipt(), receipt("run", "other")),
                listOf(receipt(), receipt("other", "activity")),
                List(100001) { receipt() },
            )) assertFails { DetailRetentionArchiveJson.Snapshot(p, rows) }
        val duplicates =
            changed(
                root(),
                "removals",
                JsonArray(
                    listOf(
                        DetailRetentionArchiveJson.element(receipt()),
                        DetailRetentionArchiveJson.element(receipt()),
                    )
                ),
            )
        assertFails { DetailRetentionArchiveJson.decode(duplicates) }
    }

    @Test
    fun snapshotCopiesCallerListAndDoesNotGrantFreshConsentForHistoricalRemovals() {
        val rows = mutableListOf(receipt())
        val snapshot =
            DetailRetentionArchiveJson.Snapshot(DetailRetention.Preferences.existing(), rows)
        rows.clear()
        val decoded =
            DetailRetentionArchiveJson.decode(DetailRetentionArchiveJson.element(snapshot))
        assertEquals(1, decoded.removals.size)
        assertEquals(DetailRetention.Preferences.existing(), decoded.preferences)
    }
}
