package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*

abstract class DevelopmentArchiveRepositoryContract {
    protected abstract fun database(): SqlDatabase

    private val at = "2026-11-16T10:00:00.123456789Z"

    private fun <T> storage(block: (SqlDatabase) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) { ApplicationSchema.NewInstallation(at, "development-test") }
            return block(db)
        } finally {
            db.close()
        }
    }

    private fun weight(id: String, provider: String) = buildJsonObject {
        put("id", id)
        put("kg", 87.25)
        put("at", at)
        put("offset", "Z")
        put("provider", provider)
        put("sourceId", id)
        put("origin", JsonNull)
        put("legacyExtra", "Retain original / metadata")
    }

    private fun insert(db: SqlDatabase, kind: String, id: String, payload: JsonObject) {
        db.execute(
            "INSERT INTO coach_evidence(kind,id,payload) VALUES(?,?,?)",
            listOf(SqlValue.Text(kind), SqlValue.Text(id), SqlValue.Text(payload.toString())),
        )
    }

    @Test
    fun cachesBeyondBothOwnedLimitsDoNotPreventManualEvidenceExportOrDisappearFromDatabase() =
        storage { db ->
            val prefix = "{\"id\":\"cache-"
            val middle =
                "\",\"kg\":87.25,\"at\":\"$at\",\"offset\":\"Z\",\"provider\":\"health-connect\",\"sourceId\":\"cache-"
            val suffix = "\",\"origin\":\"" + "p".repeat(220) + "\"}"
            db.transaction {
                db.execute(
                    "WITH RECURSIVE n(v) AS (SELECT 1 UNION ALL SELECT v+1 FROM n WHERE v<100001) INSERT INTO coach_evidence(kind,id,payload) SELECT 'WEIGHT','cache-'||v,?||v||?||v||? FROM n",
                    listOf(SqlValue.Text(prefix), SqlValue.Text(middle), SqlValue.Text(suffix)),
                )
                insert(db, "WEIGHT", "manual", weight("manual", "manual"))
            }
            val before =
                db.query("SELECT COUNT(*),SUM(LENGTH(CAST(payload AS BLOB))) FROM coach_evidence")
                    .single()
            assertEquals(100002L, before[0].sqlLong())
            assertTrue(before[1].sqlLong() > BackupEnvelopeJson.MAX_BYTES)
            val snapshot = SqliteDevelopmentArchive(db).snapshot()
            assertEquals(listOf("manual"), snapshot.records.map { it.id })
            assertEquals(weight("manual", "manual"), snapshot.records.single().payload)
            assertEquals(
                "manual",
                (snapshot.records.single().evidence as DevelopmentArchiveJson.Evidence.Weight)
                    .value
                    .provider(),
            )
            assertEquals(
                before,
                db.query("SELECT COUNT(*),SUM(LENGTH(CAST(payload AS BLOB))) FROM coach_evidence")
                    .single(),
            )
        }

    @Test
    fun originalProviderFilterPrecedesOwnedPayloadValidationButManualCorruptionFailsWithoutWrites() =
        storage { db ->
            val cache = buildJsonObject {
                put("provider", "health-kit")
                put("unknownCachedField", true)
            }
            insert(db, "WEIGHT", "cache", cache)
            assertTrue(SqliteDevelopmentArchive(db).snapshot().records.isEmpty())
            val broken = buildJsonObject {
                put("id", "manual")
                put("provider", "manual")
            }
            insert(db, "WEIGHT", "manual", broken)
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            assertFails { SqliteDevelopmentArchive(db).snapshot() }
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
        }

    @Test
    fun ownedLegacyLinksAndUnknownFieldsRetainOriginalOrderAndAreNeverReinterpretedAsActualSets() =
        storage { db ->
            val link = buildJsonObject {
                put("id", "actual")
                put("plannedDate", "2026-11-16")
                put("gymVersion", JsonNull)
                put("gymSession", JsonNull)
                put("legacyExtra", "Original link, not a new association decision")
            }
            insert(db, "WEIGHT", "manual", weight("manual", "manual"))
            insert(db, "STRENGTH_LINK", "actual", link)
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            val snapshot = SqliteDevelopmentArchive(db).snapshot()
            assertEquals(listOf("actual", "manual"), snapshot.records.map { it.id })
            assertEquals(link, snapshot.records.first().payload)
            val evidence =
                snapshot.records.first().evidence as DevelopmentArchiveJson.Evidence.StrengthLink
            assertNull(evidence.gymSession)
            assertNull(evidence.gymVersion)
            assertEquals(
                snapshot.records.map { it.payload },
                DevelopmentArchiveJson.decode(DevelopmentArchiveJson.element(snapshot))
                    .records
                    .map { it.payload },
            )
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            db.transaction {
                for (i in 0 until 512) {
                    val id = "older-$i"
                    insert(db, "STRENGTH_LINK", id, JsonObject(link + ("id" to JsonPrimitive(id))))
                }
            }
            val headers = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            val paged = SqliteDevelopmentArchive(db).snapshot()
            assertEquals(514, paged.records.size)
            assertEquals(headers.map { it[1].sqlText() }, paged.records.map { it.id })
            assertEquals("manual", paged.records.last().id)
            assertEquals(
                headers,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
        }
}
