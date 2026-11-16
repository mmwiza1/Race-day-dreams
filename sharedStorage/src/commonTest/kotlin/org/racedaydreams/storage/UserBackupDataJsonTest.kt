package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.storage.DetailRetention

class UserBackupDataJsonTest {
    private fun base() = buildJsonObject {
        put("storeVersion", 1)
        for (k in listOf("profile", "draft", "manualBaseline", "manualBaselineRecordedAt")) put(
            k,
            JsonNull,
        )
        put("settings", buildJsonObject {})
        for (k in
            listOf(
                "activities",
                "shoes",
                "results",
                "bestEfforts",
                "annotations",
                "plannedWorkouts",
                "assessments",
                "trainingPlans",
                "planAdjustments",
            )) put(k, JsonArray(emptyList()))
    }

    private fun full() = UserBackupDataJson.currentElement(UserBackupDataJson.decode(base()))

    private fun change(p: JsonObject, k: String, v: JsonElement) = JsonObject(p + (k to v))

    private val introduced =
        mapOf(
            "coaching" to 2,
            "development" to 3,
            "strengthState" to 4,
            "gymProgramming" to 4,
            "support" to 6,
            "release08" to 7,
            "strengthEngine" to 8,
            "strengthProgramming" to 9,
            "gymTools" to 10,
            "achievements" to 11,
            "bodyMeasurements" to 12,
            "detailRetention" to 13,
        )

    @Test
    fun currentOwnedExportOmitsImportedWeightCachesWithoutChangingInspectedHistory() {
        fun weight(id: String, provider: String) = buildJsonObject {
            put("kind", "WEIGHT")
            put("id", id)
            put(
                "payload",
                buildJsonObject {
                    put("id", id)
                    put("kg", 87.25)
                    put("at", CoachingArchiveFixtures.at.toString())
                    put("offset", "Z")
                    put("provider", provider)
                    put("sourceId", id)
                    put("origin", JsonNull)
                },
            )
        }
        val development = buildJsonObject {
            put("version", 1)
            put(
                "records",
                buildJsonArray {
                    add(weight("manual", "manual"))
                    add(weight("imported", "health-kit"))
                },
            )
            put("retainedExtra", "legacy metadata")
        }
        val root = change(full(), "development", development)
        val inspected = UserBackupDataJson.decode(root)
        val exported =
            UserBackupDataJson.inspect(
                    UserBackupDataJson.encodeCurrent(inspected, CoachingArchiveFixtures.at)
                )
                .snapshot
        assertEquals(listOf("manual"), exported.development!!.records.map { it.id })
        assertEquals(2, inspected.development!!.records.size)
        assertEquals(development, UserBackupDataJson.element(inspected).getValue("development"))
        assertEquals(
            JsonPrimitive("legacy metadata"),
            UserBackupDataJson.element(exported).getValue("development").jsonObject["retainedExtra"],
        )
    }

    @Test
    fun olderLegacyCurrentSurvivesCurrentFormatNormalisationWithoutInventingAuthoredProgramming() {
        val legacy = buildJsonObject {
            put("version", 1)
            put(
                "versions",
                buildJsonArray {
                    add(LegacyGymProgrammePayloadJson.element(LegacyGymArchiveFixtures.programme()))
                },
            )
            put("current", "programme:v1")
        }
        val older = change(base(), "gymProgramming", legacy)
        val old = UserBackupDataJson.decode(older)
        val current =
            UserBackupDataJson.inspect(
                    UserBackupDataJson.encodeCurrent(old, CoachingArchiveFixtures.at)
                )
                .snapshot
        assertEquals("programme:v1", current.legacy!!.current)
        assertEquals(old.legacy!!.programmes, current.legacy!!.programmes)
        assertNull(current.programming!!.current)
        assertTrue(current.programming!!.programmes.isEmpty())
        assertTrue(current.coaching!!.sessions.isEmpty())
    }

    @Test
    fun completeParentRejectsRunningReminderMirrorMismatchAndPreservesExactPrescriptions() {
        val v = CoachingArchiveFixtures.version()
        val row = buildJsonObject {
            put("id", v.id())
            put("plan_id", v.planId())
            put("version", v.version().toString())
            put("payload", RunningCoachingJson.encodeVersion(v))
        }
        val coaching =
            JsonObject(
                full().getValue("coaching").jsonObject +
                    mapOf(
                        "coach_plan_versions" to JsonArray(listOf(row)),
                        "coach_current_plan" to
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("id", "1")
                                        put("version_id", v.id())
                                    }
                                )
                            },
                    )
            )
        val mismatch = change(full(), "coaching", coaching)
        assertFails { UserBackupDataJson.decode(mismatch) }
        val p =
            change(
                mismatch,
                "plannedWorkouts",
                buildJsonArray {
                    for (w in v.plan().workouts()) add(
                        buildJsonObject {
                            put("workout", PlannedWorkoutJson.element(w))
                            put("revision", 9007199254740993L)
                        }
                    )
                },
            )
        val checked = UserBackupDataJson.decode(p)
        assertEquals(v.plan().workouts().toSet(), checked.core.planned.map { it.workout }.toSet())
        assertTrue(checked.core.planned.all { it.revision == 9007199254740993L })
    }

    @Test
    fun everyOriginalEnvelopeVersionUsesCompleteParentValidationAndCanExportCurrentEmptyDefaults() {
        for (version in 1..13) {
            val root = JsonObject(full().filterKeys { (introduced[it] ?: 0) <= version })
            val bytes =
                buildJsonObject {
                        put("format", BackupEnvelopeJson.FORMAT)
                        put("version", version)
                        put("createdAt", CoachingArchiveFixtures.at.toString())
                        put("data", root)
                    }
                    .toString()
                    .encodeToByteArray()
            val s = UserBackupDataJson.inspect(bytes)
            assertEquals(version, s.version)
            assertEquals(root, UserBackupDataJson.element(s.snapshot))
            val current =
                UserBackupDataJson.inspect(
                    UserBackupDataJson.encodeCurrent(s.snapshot, CoachingArchiveFixtures.at)
                )
            assertEquals(13, current.version)
            assertTrue(current.snapshot.core.activities.isEmpty())
            assertTrue(current.snapshot.coaching!!.sessions.isEmpty())
            assertEquals(
                DetailRetention.Preferences.existing(),
                current.snapshot.retention.preferences,
            )
        }
    }

    @Test
    fun presentNullOrMalformedFeatureSectionsNeverBecomeMissingDefaults() {
        for (name in introduced.keys) {
            assertFails { UserBackupDataJson.decode(change(full(), name, JsonNull)) }
            assertFails {
                UserBackupDataJson.decode(change(full(), name, JsonPrimitive("not an archive")))
            }
        }
    }

    @Test
    fun noFeatureSectionIsSkippedWhenAnotherSectionLooksValid() {
        for (name in introduced.keys) {
            val p = full().getValue(name).jsonObject
            assertFails {
                UserBackupDataJson.decode(
                    change(full(), name, change(p, "version", JsonPrimitive(999)))
                )
            }
        }
    }

    @Test
    fun executionAndAcceptedCoachingActualMustMatchAcrossSections() {
        val e = GymEngineArchiveFixtures.finish()
        val actual = e.completedSession()!!
        val engine = GymEngineArchiveFixtures.root(listOf(GymEngineArchiveFixtures.entry(e)))
        val coaching = full().getValue("coaching").jsonObject
        val row = buildJsonObject {
            put("id", actual.id())
            put("started_at", actual.startedAt().toString())
            put("payload", StrengthEvidenceJson.encodeSession(actual))
        }
        val p =
            change(
                change(full(), "strengthEngine", engine),
                "coaching",
                change(coaching, "strength_sessions", JsonArray(listOf(row))),
            )
        val checked = UserBackupDataJson.decode(p)
        assertEquals(actual, checked.engine!!.executions.getValue(actual.id()).currentActual)
        assertFails { UserBackupDataJson.decode(change(p, "coaching", coaching)) }
        val encoded = UserBackupDataJson.encodeCurrent(checked, CoachingArchiveFixtures.at)
        assertEquals(
            actual,
            UserBackupDataJson.inspect(encoded).snapshot.coaching!!.sessions.getValue(actual.id()),
        )
    }

    @Test
    fun orphanTimerAndUnboundProgrammeLinkFailWholeParentEvenWhenEachSectionIsValid() {
        val timer =
            org.racedaydreams.domain.strength.GymRestTimer.start(
                "missing-execution",
                "set",
                90,
                org.racedaydreams.domain.strength.GymRestTimer.Clock("boot", 0),
            )
        val tools =
            GymToolsPayloadJson.element(
                GymToolsPayloadJson.Snapshot(
                    org.racedaydreams.domain.strength.GymRestTimer.Preferences.defaults(),
                    listOf(timer),
                )
            )
        assertFails { UserBackupDataJson.decode(change(full(), "gymTools", tools)) }
        val development = buildJsonObject {
            put("version", 1)
            put(
                "records",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("kind", "STRENGTH_LINK")
                            put("id", "missing-actual")
                            put(
                                "payload",
                                buildJsonObject {
                                    put("id", "missing-actual")
                                    put("gymVersion", "missing:v1")
                                    put("gymSession", "session")
                                    put("plannedDate", "2026-11-17")
                                },
                            )
                        }
                    )
                },
            )
        }
        assertFails { UserBackupDataJson.decode(change(full(), "development", development)) }
    }

    @Test
    fun malformedFutureFilesAndMissingCurrentSectionsDoNotReturnAValidatedSnapshot() {
        val valid =
            UserBackupDataJson.encodeCurrent(
                UserBackupDataJson.decode(full()),
                CoachingArchiveFixtures.at,
            )
        val p = Json.parseToJsonElement(valid.decodeToString()).jsonObject
        for (bad in
            listOf(
                change(p, "version", JsonPrimitive(14)),
                change(p, "data", JsonObject(full() - "strengthEngine")),
            )) assertFails { UserBackupDataJson.inspect(bad.toString().encodeToByteArray()) }
        assertFails { UserBackupDataJson.inspect(valid.copyOf(valid.size - 1)) }
    }

    @Test
    fun normalisationOnlyAddsMissingSectionsAndNeverChangesRetainedEvidenceOrConsent() {
        val older = change(base(), "legacyExtra", JsonPrimitive("preserve this original field"))
        val snapshot = UserBackupDataJson.decode(older)
        val current = UserBackupDataJson.currentElement(snapshot)
        assertEquals(older, UserBackupDataJson.element(snapshot))
        assertEquals(JsonPrimitive("preserve this original field"), current["legacyExtra"])
        assertFalse(UserBackupDataJson.decode(current).body.preferences.proteinEnabled())
        assertEquals(
            DetailRetention.Choice.EXISTING_UNREVIEWED,
            UserBackupDataJson.decode(current).retention.preferences.choice(),
        )
    }
}
