package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.GymRestTimer
import org.racedaydreams.domain.time.Instant

/**
 * Complete original store-v1 parent graph. Validating a file is never permission to replace data.
 */
object UserBackupDataJson {
    class Snapshot
    internal constructor(
        val core: CoreBackupDataJson.Snapshot,
        val coaching: CoachingArchiveJson.Snapshot?,
        val development: DevelopmentArchiveJson.Snapshot?,
        val choices: StrengthChoicesArchiveJson.Snapshot?,
        val legacy: LegacyGymProgrammeArchiveJson.Snapshot?,
        val support: SupportArchiveJson.Snapshot?,
        val release08: Release08ArchiveJson.Snapshot?,
        programming: StrengthProgrammingArchiveJson.Snapshot?,
        val engine: GymExecutionArchiveJson.Snapshot?,
        tools: GymToolsPayloadJson.Snapshot,
        val achievements: AchievementsArchiveJson.Snapshot,
        val body: BodyMeasurementsArchiveJson.Snapshot,
        val retention: DetailRetentionArchiveJson.Snapshot,
        /** Bouldering (root 14+); older backups restore with no climbing history. */
        val climbing: ClimbingArchiveJson.Snapshot,
        internal val payload: JsonObject,
    ) {
        val programming =
            programming?.copy(
                programmes = Immutable.copyMap(programming.programmes),
                drafts = Immutable.copyList(programming.drafts),
                concreteDrafts = Immutable.copyList(programming.concreteDrafts),
                custom = Immutable.copyList(programming.custom),
                decisions = Immutable.copyList(programming.decisions),
            )
        val tools =
            GymToolsPayloadJson.Snapshot(tools.preferences, Immutable.copyList(tools.timers))
    }

    /**
     * File callers must enter through inspect; DOM callers must validate parent syntax/size first.
     */
    fun decode(data: JsonObject): Snapshot {
        fun section(name: String): JsonObject? =
            if (name !in data) null
            else
                data.getValue(name) as? JsonObject
                    ?: throw IllegalArgumentException("Expected backup section: $name")
        val core = CoreBackupDataJson.decode(data)
        val coaching = section("coaching")?.let { CoachingArchiveJson.decode(it) }
        if (coaching != null) CoreBackupDataJson.validateRunningMirror(core, coaching)
        val development = section("development")?.let { DevelopmentArchiveJson.decode(it) }
        val choices = section("strengthState")?.let { StrengthChoicesArchiveJson.decode(it) }
        val legacy = section("gymProgramming")?.let { LegacyGymProgrammeArchiveJson.decode(it) }
        val programming =
            section("strengthProgramming")?.let {
                StrengthProgrammingArchiveJson.decode(
                    it,
                    legacy?.programmes?.values?.toList().orEmpty(),
                    legacy?.current,
                )
            }
        if (programming != null)
            StrengthProgrammingArchiveJson.validateCoaching(
                programming,
                coaching?.exercises.orEmpty(),
            )
        LegacyGymProgrammeArchiveJson.validateLinks(
            legacy ?: LegacyGymProgrammeArchiveJson.decode(null),
            development,
            coaching?.sessions?.keys.orEmpty(),
            programming?.programmes.orEmpty(),
        )
        val engine =
            section("strengthEngine")?.let {
                GymExecutionArchiveJson.decode(
                    it,
                    coaching?.sessions.orEmpty(),
                    legacy?.programmes.orEmpty(),
                    GymExecutionArchiveJson.links(development),
                    programming?.programmes.orEmpty(),
                    PortableLegacyStrengthFingerprint,
                )
            }
        val tools =
            section("gymTools")?.let {
                GymToolsPayloadJson.decode(it, engine?.executions?.keys.orEmpty())
            } ?: GymToolsPayloadJson.decode(null, emptySet())
        val achievements = AchievementsArchiveJson.decode(section("achievements"))
        val body = BodyMeasurementsArchiveJson.decode(section("bodyMeasurements"))
        val retention = DetailRetentionArchiveJson.decode(section("detailRetention"))
        CoreBackupDataJson.validateRetention(core, retention)
        val climbing = ClimbingArchiveJson.decode(section("climbing"))
        val support = section("support")?.let { SupportArchiveJson.decode(it) }
        val release = section("release08")?.let { Release08ArchiveJson.decode(it) }
        // Protect original optional fields, payload strings, fingerprints and retained row
        // ordering.
        return Snapshot(
            core,
            coaching,
            development,
            choices,
            legacy,
            support,
            release,
            programming,
            engine,
            tools,
            achievements,
            body,
            retention,
            climbing,
            Json.parseToJsonElement(data.toString()).jsonObject,
        )
    }

    fun inspect(bytes: ByteArray): BackupEnvelopeJson.Checked<Snapshot> =
        BackupEnvelopeJson.inspect(bytes, ::decode)

    fun element(snapshot: Snapshot): JsonObject = snapshot.payload

    /**
     * Missing old sections become only empty original defaults, never synthetic workouts/consent.
     * Current user-owned exports omit imported weight caches without changing inspected history.
     */
    fun currentElement(snapshot: Snapshot): JsonObject {
        val values = snapshot.payload.toMutableMap()
        val development = snapshot.development
        if (development != null) {
            val owned = DevelopmentArchiveJson.userOwnedElement(development)
            val original = values.getValue("development").jsonObject
            values["development"] = JsonObject(original + ("records" to owned.getValue("records")))
        }

        fun fill(name: String, create: () -> JsonObject) {
            if (name !in values) values[name] = create()
        }
        fun rows() = buildJsonObject {
            put("version", 1)
            put("rows", JsonArray(emptyList()))
        }
        fill("coaching") {
            buildJsonObject {
                put("version", 1)
                for (t in CoachingArchiveJson.Table.entries) put(t.sqlName, JsonArray(emptyList()))
            }
        }
        fill("development") {
            buildJsonObject {
                put("version", 1)
                put("records", JsonArray(emptyList()))
            }
        }
        fill("strengthState") {
            buildJsonObject {
                put("version", 2)
                put("choices", JsonArray(emptyList()))
                put("acknowledged", JsonArray(emptyList()))
                put(
                    "exclusions",
                    buildJsonObject {
                        put("version", 1)
                        put("entries", JsonArray(emptyList()))
                    },
                )
            }
        }
        fill("gymProgramming") {
            LegacyGymProgrammeArchiveJson.element(LegacyGymProgrammeArchiveJson.decode(null))
        }
        fill("support") { rows() }
        fill("release08") { rows() }
        fill("strengthProgramming") {
            StrengthProgrammingArchiveJson.element(
                StrengthProgrammingArchiveJson.Snapshot(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    null,
                )
            )
        }
        fill("strengthEngine") {
            buildJsonObject {
                put("version", 2)
                put("workouts", JsonArray(emptyList()))
                put("preferences", JsonArray(emptyList()))
                put("executions", JsonArray(emptyList()))
                put(
                    "associations",
                    GymAssociationArchiveJson.element(
                        GymAssociationArchiveJson.Archive(emptyList(), emptyMap())
                    ),
                )
            }
        }
        fill("gymTools") {
            GymToolsPayloadJson.element(
                GymToolsPayloadJson.Snapshot(GymRestTimer.Preferences.defaults(), emptyList())
            )
        }
        fill("achievements") {
            AchievementsArchiveJson.element(AchievementsArchiveJson.decode(null))
        }
        fill("bodyMeasurements") {
            BodyMeasurementsArchiveJson.element(BodyMeasurementsArchiveJson.decode(null))
        }
        fill("detailRetention") {
            DetailRetentionArchiveJson.element(DetailRetentionArchiveJson.decode(null))
        }
        fill("climbing") { ClimbingArchiveJson.element(ClimbingArchiveJson.Snapshot.empty()) }
        return JsonObject(values)
    }

    /**
     * Full v14 write validation; no database replacement, raw-detail restore or pruning command.
     */
    fun encodeCurrent(snapshot: Snapshot, createdAt: Instant): ByteArray =
        BackupEnvelopeJson.encode(currentElement(snapshot), createdAt) { decode(it) }
}
