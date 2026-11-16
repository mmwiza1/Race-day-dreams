package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.storage.*
import org.racedaydreams.domain.time.*

class CoreBackupDataJsonTest {
    private val at = CoachingArchiveFixtures.at

    private fun empty() = buildJsonObject {
        put("storeVersion", 1)
        for (key in listOf("profile", "draft", "manualBaseline", "manualBaselineRecordedAt")) put(
            key,
            JsonNull,
        )
        put("settings", buildJsonObject {})
        for (key in
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
            )) put(key, JsonArray(emptyList()))
    }

    private fun activity(id: String = "actual", provider: String = "rdd") =
        ActivityRecord(
            id,
            SourceIdentity(provider, "record-$id", null),
            at,
            Instants.ofEpochSecond(at.getEpochSecond() + 600, at.getNano().toLong()),
            Offsets.ofTotalSeconds(0),
            null,
            3000.0,
            Durations.ofSeconds(600),
            null,
            null,
            null,
            null,
            6.0,
            "Retained actual",
            emptyList(),
            emptyList(),
            null,
            null,
        )

    private fun result(
        id: String = "race",
        source: ResultSource = ResultSource.OFFICIAL_CHIP,
        distance: PerformanceDistance = PerformanceDistance.FIVE_K,
    ) =
        RecordedResult(
            id,
            distance,
            Durations.ofMinutes(25),
            CoachingArchiveFixtures.start,
            "Race",
            source,
            if (source == ResultSource.ROLLING) "actual" else null,
            "Recorded",
        )

    private fun objectOf(text: String) = Json.parseToJsonElement(text) as JsonObject

    private fun rows(root: JsonObject, key: String, vararg rows: JsonObject) =
        JsonObject(root + (key to JsonArray(rows.toList())))

    private fun base() = rows(empty(), "activities", objectOf(ActivityJson.encode(activity())))

    private fun effort(
        result: RecordedResult = result("best", ResultSource.ROLLING),
        provider: String = "rdd",
        record: String = "record-actual",
    ) = buildJsonObject {
        put("result", objectOf(ResultJson.encode(result)))
        put(
            "source",
            buildJsonObject {
                put("provider", provider)
                put("recordId", record)
                put("originPackage", JsonNull)
            },
        )
    }

    @Test
    fun emptyAndRecordedCorePreserveUnknownsActualTotalsAndPreciseRevisionNumbers() {
        val empty = CoreBackupDataJson.decode(empty())
        assertNull(empty.profile)
        assertNull(empty.baseline)
        assertTrue(empty.activities.isEmpty())
        val w = CoachingArchiveFixtures.workout()
        val root =
            rows(
                rows(base(), "results", objectOf(ResultJson.encode(result()))),
                "plannedWorkouts",
                buildJsonObject {
                    put("workout", PlannedWorkoutJson.element(w))
                    put("revision", 9007199254740993L)
                },
            )
        val decoded = CoreBackupDataJson.decode(root)
        assertEquals(activity(), decoded.activities.single())
        assertEquals(result(), decoded.results.single())
        assertEquals(9007199254740993L, decoded.planned.single().revision)
        assertEquals(3000.0, decoded.activities.single().distanceMetres())
    }

    @Test
    fun duplicateActivitiesProviderSourcesAndExternalHealthMeasurementsAreRejected() {
        val a = activity()
        for (values in
            listOf(
                listOf(a, a),
                listOf(a, a.copy(id = "other")),
                listOf(activity(provider = "health-connect")),
            )) assertFails {
            CoreBackupDataJson.decode(
                rows(
                    empty(),
                    "activities",
                    *values.map { objectOf(ActivityJson.encode(it)) }.toTypedArray(),
                )
            )
        }
    }

    @Test
    fun manualAndRollingResultsKeepDifferentProvenanceAndUniqueDistanceDefinitions() {
        val root = rows(base(), "bestEfforts", effort())
        assertEquals(
            ResultSource.ROLLING,
            CoreBackupDataJson.decode(root).bestEfforts.single().result.source(),
        )
        assertFails {
            CoreBackupDataJson.decode(
                rows(
                    base(),
                    "results",
                    objectOf(ResultJson.encode(result("best", ResultSource.ROLLING))),
                )
            )
        }
        assertFails { CoreBackupDataJson.decode(rows(base(), "bestEfforts", effort(result()))) }
        assertFails {
            CoreBackupDataJson.decode(
                rows(root, "results", objectOf(ResultJson.encode(result("best"))))
            )
        }
        assertFails {
            CoreBackupDataJson.decode(
                rows(
                    base(),
                    "bestEfforts",
                    effort(),
                    effort(result("another", ResultSource.ROLLING)),
                )
            )
        }
        assertFails {
            CoreBackupDataJson.decode(rows(base(), "bestEfforts", effort(provider = "gpx")))
        }
        val conflict =
            result(
                "conflict",
                distance =
                    PerformanceDistance(
                        PerformanceDistance.FIVE_K.code(),
                        6000.0,
                        "Conflicting distance",
                    ),
            )
        assertFails {
            CoreBackupDataJson.decode(
                rows(base(), "results", objectOf(ResultJson.encode(conflict)))
            )
        }
    }

    @Test
    fun malformedSettingsRevisionsDraftsAndOpaqueRowDuplicatesFailClosed() {
        for (root in
            listOf(
                JsonObject(empty() + ("settings" to buildJsonObject { put("invalid", 42) })),
                JsonObject(empty() + ("draft" to JsonPrimitive("not an object"))),
                JsonObject(empty() + ("storeVersion" to JsonPrimitive(2))),
                rows(
                    empty(),
                    "trainingPlans",
                    buildJsonObject { put("id", "same") },
                    buildJsonObject { put("id", "same") },
                ),
                rows(
                    empty(),
                    "plannedWorkouts",
                    buildJsonObject {
                        put(
                            "workout",
                            PlannedWorkoutJson.element(CoachingArchiveFixtures.workout()),
                        )
                        put("revision", 0)
                    },
                ),
            )) assertFails { CoreBackupDataJson.decode(root) }
    }

    @Test
    fun defaultShoesAnnotationsAndEffortValuesRemainExplicit() {
        val shoe = Shoe("trainer", "Brand", "Daily", null, 0.0, false, null, true)
        val annotation = buildJsonObject {
            put("provider", "rdd")
            put("recordId", "record-actual")
            put("notes", JsonNull)
            put("rpe", 7.5)
            put("shoeId", "trainer")
        }
        val root =
            rows(rows(base(), "shoes", objectOf(ShoeJson.encode(shoe))), "annotations", annotation)
        assertEquals(7.5, CoreBackupDataJson.decode(root).annotations.single().rpe)
        assertNull(CoreBackupDataJson.decode(root).annotations.single().notes)
        assertFails { CoreBackupDataJson.decode(rows(root, "shoes")) }
        assertFails {
            CoreBackupDataJson.decode(rows(root, "annotations", JsonObject(annotation - "notes")))
        }
        assertFails { CoreBackupDataJson.decode(rows(root, "annotations", annotation, annotation)) }
        assertFails {
            CoreBackupDataJson.decode(
                rows(
                    root,
                    "shoes",
                    objectOf(ShoeJson.encode(shoe)),
                    objectOf(ShoeJson.encode(shoe.copy(id = "another"))),
                )
            )
        }
    }

    @Test
    fun currentRunningMirrorUsesActualStructuredPrescriptionsAndLeavesHistoryUntouched() {
        val v = CoachingArchiveFixtures.version()
        val coaching = buildJsonObject {
            put("version", 1)
            for (t in CoachingArchiveJson.Table.entries) put(t.sqlName, JsonArray(emptyList()))
            put(
                "coach_plan_versions",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("id", "v1")
                            put("plan_id", "plan")
                            put("version", "1")
                            put("payload", RunningCoachingJson.encodeVersion(v))
                        }
                    )
                },
            )
            put(
                "coach_current_plan",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("id", "1")
                            put("version_id", "v1")
                        }
                    )
                },
            )
        }
        val archive = CoachingArchiveJson.decode(coaching)
        val root =
            rows(
                empty(),
                "plannedWorkouts",
                *v.plan()
                    .workouts()
                    .map { w ->
                        buildJsonObject {
                            put("workout", PlannedWorkoutJson.element(w))
                            put("revision", 1)
                        }
                    }
                    .toTypedArray(),
            )
        CoreBackupDataJson.validateRunningMirror(CoreBackupDataJson.decode(root), archive)
        assertFails {
            CoreBackupDataJson.validateRunningMirror(CoreBackupDataJson.decode(empty()), archive)
        }
        assertEquals(v, archive.versions.getValue("v1"))
    }

    @Test
    fun removalReceiptsRequireMatchingRetainedActivityOrBestEffortProvenance() {
        val removal =
            RemovedRecordingDetail(
                "record-actual",
                "actual",
                "rdd",
                "record-actual",
                at,
                at,
                DetailRetention.Policy.LAST_24_MONTHS,
                at,
                0,
                1000,
                2,
                1,
                "ab".repeat(32),
                "cd".repeat(32),
            )
        val archive =
            DetailRetentionArchiveJson.Snapshot(
                DetailRetention.Preferences.existing(),
                listOf(removal),
            )
        CoreBackupDataJson.validateRetention(CoreBackupDataJson.decode(base()), archive)
        CoreBackupDataJson.validateRetention(
            CoreBackupDataJson.decode(rows(empty(), "bestEfforts", effort())),
            archive,
        )
        assertFails {
            CoreBackupDataJson.validateRetention(CoreBackupDataJson.decode(empty()), archive)
        }
        assertFails {
            CoreBackupDataJson.validateRetention(
                CoreBackupDataJson.decode(
                    rows(
                        empty(),
                        "activities",
                        objectOf(ActivityJson.encode(activity(provider = "gpx"))),
                    )
                ),
                archive,
            )
        }
    }
}
