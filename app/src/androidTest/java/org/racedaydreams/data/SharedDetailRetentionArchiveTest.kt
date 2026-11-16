package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.RemovedRecordingDetail
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedDetailRetentionArchiveTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-16T10:00:00.123456789Z")
    private lateinit var bridge: AndroidSqlTestConnection

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        bridge = AndroidSqlTestConnection(ProfileDatabase.get(context).writableDatabase)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun element(json: JSONObject) = Json.parseToJsonElement(json.toString()) as JsonObject

    private fun retainReceipt(): RemovedRecordingDetail {
        val activity =
            ActivityRecord(
                "activity",
                SourceIdentity("rdd", "run", null),
                at,
                at.plusSeconds(60),
                ZoneOffset.UTC,
                null,
                100.0,
                Duration.ofMinutes(1),
                null,
                null,
                null,
                null,
                null,
                "Retained summary",
                emptyList(),
                emptyList(),
                null,
                null,
            )
        SqliteActivities(bridge) { at }.upsertManualActivity(activity)
        val proof =
            RemovedRecordingDetail(
                "run",
                "activity",
                "rdd",
                "run",
                activity.end(),
                at.plusSeconds(120),
                DetailRetention.Policy.LAST_24_MONTHS,
                at,
                9007199254740993L,
                9007199254741993L,
                2,
                1,
                "ab".repeat(32),
                "cd".repeat(32),
            )
        bridge.execute(
            "INSERT INTO recording_detail_removals VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text("run"),
                SqlValue.Text("activity"),
                SqlValue.Text("rdd"),
                SqlValue.Text("run"),
                SqlValue.Text(proof.endedAt().toString()),
                SqlValue.Text(proof.removedAt().toString()),
                SqlValue.Text(proof.policy().name),
                SqlValue.Text(at.toString()),
                SqlValue.Integer(proof.startedMono()),
                SqlValue.Integer(proof.lastMono()),
                SqlValue.Integer(2),
                SqlValue.Integer(1),
                SqlValue.Text(proof.pointsHash()),
                SqlValue.Text(proof.eventsHash()),
            ),
        )
        return proof
    }

    @Test
    fun originalAndSharedArchivesPreserveEveryChoiceAndExactRemovedEvidence() {
        val proof = retainReceipt()
        val db = ProfileDatabase.get(context).writableDatabase
        val original = SqliteDetailRetentionRepository(context)
        val shared = SqliteRecordingDetailReceipts(bridge)
        for (policy in DetailRetention.Policy.entries) {
            original.acknowledge(policy, at)
            val before = SqliteDetailRetentionRepository.exportData(db)
            val decoded = DetailRetentionArchiveJson.decode(element(before))
            assertEquals(original.preferences(), decoded.preferences)
            assertEquals(listOf(proof), decoded.removals)
            assertEquals(element(before), DetailRetentionArchiveJson.element(shared.snapshot()))
            val old =
                SqliteDetailRetentionRepository.validate(
                    JSONObject(DetailRetentionArchiveJson.element(decoded).toString())
                )
            assertEquals(decoded.preferences, old.preferences())
            assertEquals(decoded.removals, old.removals())
            assertEquals(
                before.toString(),
                SqliteDetailRetentionRepository.exportData(db).toString(),
            )
        }
        bridge.execute(
            "UPDATE detail_retention_preferences SET policy='KEEP_FOREVER',choice='EXISTING_UNREVIEWED',chosen_at=NULL,choice_id=NULL"
        )
        val old = SqliteDetailRetentionRepository.validate(null)
        assertEquals(old.preferences(), DetailRetentionArchiveJson.decode(null).preferences)
        assertEquals(DetailRetention.Preferences.existing(), shared.snapshot().preferences)
        assertEquals(listOf(proof), shared.snapshot().removals)
    }

    @Test
    fun corruptMetadataIsRejectedByBothValidatorsWithoutChangingSummariesOrConsent() {
        retainReceipt()
        val db = ProfileDatabase.get(context).writableDatabase
        val original = SqliteDetailRetentionRepository.exportData(db)
        val row = original.getJSONArray("removals").getJSONObject(0)
        val invalid =
            listOf(
                JSONObject(original.toString()).put("version", 2),
                JSONObject(original.toString()).put("unsupported", true),
                JSONObject(original.toString()).put("removals", JSONArray().put(row).put(row)),
                JSONObject(original.toString())
                    .put("removals", JSONArray().put(JSONObject(row.toString()).put("mono", 1.5))),
                JSONObject(original.toString())
                    .put(
                        "removals",
                        JSONArray().put(JSONObject(row.toString()).put("pointsHash", "missing")),
                    ),
                JSONObject(original.toString()).put("choice", "EXISTING_UNREVIEWED"),
            )
        val summaries = SqliteActivities(bridge) { at }.activitySummaries()
        for (bad in invalid) {
            assertTrue(runCatching { SqliteDetailRetentionRepository.validate(bad) }.isFailure)
            assertTrue(runCatching { DetailRetentionArchiveJson.decode(element(bad)) }.isFailure)
        }
        assertEquals(original.toString(), SqliteDetailRetentionRepository.exportData(db).toString())
        assertEquals(summaries, SqliteActivities(bridge) { at }.activitySummaries())
    }
}
