package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.*
import java.util.zip.ZipInputStream
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

    // Compare through the actual original reader: JsonPrimitive equality alone treats 100 and
    // 100.0 as different text even though JSONObject retains the same numeric measurement.
    private fun originalRow(line: String) = element(JSONObject(JSONObject(line).toString()))

    @Test
    fun originalReaderParityKeepsExactClocksTypesAndChangedMeasurementsDistinct() {
        val original = originalRow("""{"measurement":100,"clock":9007199254740993}""")
        assertEquals(original, originalRow("""{"measurement":100.0,"clock":9007199254740993}"""))
        assertNotEquals(original, originalRow("""{"measurement":"100","clock":9007199254740993}"""))
        assertNotEquals(original, originalRow("""{"measurement":100,"clock":9007199254740992}"""))
        assertNotEquals(
            original,
            originalRow("""{"measurement":100.00000000000001,"clock":9007199254740993}"""),
        )
        assertNotEquals(
            original,
            originalRow("""{"measurement":100,"clock":9007199254740993,"extra":null}"""),
        )
        // Legacy output spells negative zero as -0, then reads it as integral zero.
        assertEquals(originalRow("""{"measurement":-0}"""), originalRow("""{"measurement":-0.0}"""))
    }

    private fun rawArchive(): Pair<ByteArray, List<String>> {
        val out = ByteArrayOutputStream()
        RecordingArchive(context).exportTo(out)
        val archive = out.toByteArray()
        var manifest: JSONObject? = null
        var raw: ByteArray? = null
        ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val bytes = zip.readBytes()
                when (entry.name) {
                    "manifest.json" -> manifest = JSONObject(bytes.decodeToString())
                    "recordings.jsonl" -> raw = bytes
                    else -> fail("Unexpected original raw archive entry")
                }
                zip.closeEntry()
            }
        }
        val bytes = requireNotNull(raw)
        val m = requireNotNull(manifest)
        assertEquals(3, m.getInt("version"))
        assertEquals(bytes.size.toLong(), m.getLong("bytes"))
        assertEquals(
            m.getString("sha256"),
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
            },
        )
        assertTrue(bytes.decodeToString().endsWith("\n"))
        val lines = bytes.decodeToString().dropLast(1).split('\n')
        val streamed =
            RecordingArchiveReader(
                RecordingArchiveManifest.decode(m.toString().encodeToByteArray())
            )
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(7, bytes.size - offset)
            streamed.accept(bytes, offset, length)
            offset += length
        }
        val rows = RecordingArchiveRows(3)
        lines.forEach(rows::acceptLine)
        val expected = rows.finish().sessions
        assertEquals(expected, streamed.finish().sessions)
        val checkedZip =
            BackupZipArchive.inspect(
                sharedBackupZipSource(archive),
                BackupZipArchive.Kind.RECORDINGS,
            )
        val extractedManifest = ByteArrayOutputStream()
        checkedZip.extract("manifest.json") { extractedManifest.write(it) }
        val checkedStream =
            RecordingArchiveReader(RecordingArchiveManifest.decode(extractedManifest.toByteArray()))
        checkedZip.extract("recordings.jsonl") { checkedStream.accept(it) }
        assertEquals(expected, checkedStream.finish().sessions)
        val before = bridge.query("SELECT total_changes()")
        val sharedExport = SqliteRecordingArchive(bridge).export(context.cacheDir.absolutePath)
        try {
            val sharedZip = BackupZipArchive.inspect(sharedExport, BackupZipArchive.Kind.RECORDINGS)
            val sharedRaw = ByteArrayOutputStream()
            sharedZip.extract("recordings.jsonl") { sharedRaw.write(it) }
            val sharedLines = sharedRaw.toByteArray().decodeToString().dropLast(1).split('\n')
            assertEquals(lines.map(::originalRow), sharedLines.map(::originalRow))
            val sharedManifest = ByteArrayOutputStream()
            sharedZip.extract("manifest.json") { sharedManifest.write(it) }
            val sharedRows =
                RecordingArchiveReader(
                    RecordingArchiveManifest.decode(sharedManifest.toByteArray())
                )
            sharedRows.accept(sharedRaw.toByteArray())
            assertEquals(expected, sharedRows.finish().sessions)
        } finally {
            sharedExport.close()
        }
        assertEquals(before, bridge.query("SELECT total_changes()"))
        return archive to lines
    }

    @Test
    fun sharedRawRowsAcceptOriginalGpsGapsAndRecoveryWithoutChangingCoordinatesOrTotals() {
        val store = RecordingStore(context)
        val engine = org.racedaydreams.domain.recording.RunMetricsEngine(0, at)
        for (i in 0..70) engine.accept(
            org.racedaydreams.domain.recording.RunMetricsEngine.Fix(
                at.plusSeconds(i.toLong()),
                i * 1000L,
                0.0,
                i * 0.00003,
                3.0,
                null,
                null,
                3.0,
            )
        )
        engine.missingFix(100000, at.plusSeconds(100))
        for (i in 101..110) engine.accept(
            org.racedaydreams.domain.recording.RunMetricsEngine.Fix(
                at.plusSeconds(i.toLong()),
                i * 1000L,
                0.0,
                0.01 + (i - 101) * 0.00003,
                3.0,
                null,
                null,
                3.0,
            )
        )
        store.start("raw-shared", at, 0)
        store.flush("raw-shared", engine.points(), engine.events(), 0, 0, 110000, "RECOVERABLE")
        val actualId = store.finish(requireNotNull(store.unfinished()), null)
        val points = store.points("raw-shared")
        assertTrue(points.any { it.discontinuity() })
        val summary = FeatureStore(context).activities().first { it.id() == actualId }
        val db = ProfileDatabase.get(context).writableDatabase
        val receipt =
            RemovedRecordingDetail(
                "raw-shared",
                actualId,
                summary.source().provider(),
                "raw-shared",
                summary.end(),
                at.plusSeconds(200),
                DetailRetention.Policy.LAST_24_MONTHS,
                at,
                0,
                110000,
                points.size.toLong(),
                engine.events().size.toLong(),
                SqliteDetailRetentionRepository.evidenceHash(db, "recording_points", "raw-shared"),
                SqliteDetailRetentionRepository.evidenceHash(db, "recording_events", "raw-shared"),
            )
        val beforeHash = bridge.query("SELECT total_changes()")
        SqliteDetailRetentionRepository.requireMatches(db, receipt)
        SqliteRecordingEvidence(bridge).requireMatches(receipt)
        assertEquals(beforeHash, bridge.query("SELECT total_changes()"))
        // Hash receipts describe ORIGINAL stored bytes, even when alternate JSON is equivalent.
        db.beginTransaction()
        try {
            bridge.execute(
                "UPDATE recording_points SET payload=payload||' ' WHERE session_id='raw-shared' AND sequence=0"
            )
            assertThrows(IllegalArgumentException::class.java) {
                SqliteDetailRetentionRepository.requireMatches(db, receipt)
            }
            assertThrows(IllegalArgumentException::class.java) {
                SqliteRecordingEvidence(bridge).requireMatches(receipt)
            }
        } finally {
            db.endTransaction() // Roll back the deliberately changed source bytes.
        }
        SqliteRecordingEvidence(bridge).requireMatches(receipt)
        val (archive, lines) = rawArchive()
        for (version in 1..3) {
            val reader = RecordingArchiveRows(version)
            lines.forEach(reader::acceptLine)
            val kept = reader.finish().sessions.getValue("raw-shared")
            assertEquals(points.size, kept.points)
            assertEquals(engine.events().size.toLong(), kept.events)
            assertEquals(actualId, kept.activityId)
            assertTrue(kept.hasActivity)
            assertNull(kept.removal)
        }
        RecordingArchive(context).restoreFrom(ByteArrayInputStream(archive))
        assertEquals(points, store.points("raw-shared"))
        assertEquals(summary, FeatureStore(context).activities().first { it.id() == actualId })
    }

    @Test
    fun sharedRawRowsKeepOriginalRemovedDetailProvenanceWithoutInventingRouteEvidence() {
        val proof = retainReceipt()
        bridge.execute(
            "INSERT INTO recording_sessions(id,state,started_at,started_mono,last_mono,activity_id) VALUES(?,'FINISHED',?,?,?,?)",
            listOf(
                SqlValue.Text(proof.sessionId()),
                SqlValue.Text(proof.startedAt().toString()),
                SqlValue.Integer(proof.startedMono()),
                SqlValue.Integer(proof.lastMono()),
                SqlValue.Text(proof.activityId()),
            ),
        )
        val (archive, lines) = rawArchive()
        val reader = RecordingArchiveRows(3)
        lines.forEach(reader::acceptLine)
        val kept = reader.finish().sessions.getValue(proof.sessionId())
        assertEquals(proof, kept.removal)
        assertEquals(0, kept.points)
        assertEquals(0L, kept.events)
        assertTrue(kept.hasActivity)
        val before = SqliteActivities(bridge) { at }.activitySummaries()
        RecordingArchive(context).restoreFrom(ByteArrayInputStream(archive))
        assertEquals(proof, SqliteRecordingDetailReceipts(bridge).removal(proof.sessionId()))
        assertEquals(before, SqliteActivities(bridge) { at }.activitySummaries())
        assertTrue(RecordingStore(context).points(proof.sessionId()).isEmpty())
    }

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
