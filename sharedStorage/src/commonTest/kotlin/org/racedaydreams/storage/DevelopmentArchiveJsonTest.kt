package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.DevelopmentArchiveJson.Evidence
import org.racedaydreams.storage.DevelopmentArchiveJson.Kind

class DevelopmentArchiveJsonTest {
    private fun payload(kind: Kind, id: String = kind.name): JsonObject = buildJsonObject {
        put("id", id)
        when (kind) {
            Kind.WEIGHT -> {
                put("kg", 87.25)
                put("at", CoachingArchiveFixtures.at.toString())
                put("offset", "+05:30:15")
                put("provider", "manual")
                put("sourceId", "source")
                put("origin", JsonNull)
            }
            Kind.HISTORICAL_PB -> {
                put(
                    "exercise",
                    StrengthEvidenceJson.exercise(
                        LegacyFingerprintFixtures.session().exercises().first().exercise()
                    ),
                )
                put("kg", 82.25)
                put("reps", 6)
                put("date", JsonNull)
                put("precision", "UNKNOWN")
                put("notes", JsonNull)
                put("seconds", JsonNull)
                put("metres", JsonNull)
            }
            Kind.INTERRUPTION -> {
                put("from", "2026-10-01")
                put("returned", "2026-10-20")
                put("reason", "Time away")
            }
            Kind.SESSION_DECISION -> {
                put("workoutId", "workout")
                put("versionId", "retained-v1")
                put("originalDate", "2026-11-16")
                put("movedTo", JsonNull)
                put("kind", "SKIPPED")
                put("at", CoachingArchiveFixtures.at.toString())
            }
            Kind.STRENGTH_LINK -> {
                put("plannedDate", "2026-11-16")
                put("gymVersion", JsonNull)
                put("gymSession", JsonNull)
            }
            Kind.REVIEW_DECISION -> {
                put("base", "retained-v1")
                put("choice", "keep")
                put("result", "retained-v1")
            }
            Kind.WEEKLY_REVIEW -> {
                put("base", "retained-v1")
                put("week", "2026-11-16")
                put("planned", 3)
                put("completed", 4)
                put("km", 12.5)
                put("observations", buildJsonArray { add("Historic observation") })
                put("channels", buildJsonArray { add("legacy channel") })
                put(
                    "choices",
                    buildJsonArray {
                        add(buildJsonObject { put("replacements", JsonArray(emptyList())) })
                    },
                )
                put("legacyExtra", "retained exactly")
            }
        }
    }

    private fun row(kind: Kind, p: JsonObject = payload(kind)) = buildJsonObject {
        put("kind", kind.name)
        put("id", p.getValue("id"))
        put("payload", p)
    }

    private fun root(vararg rows: JsonObject) = buildJsonObject {
        put("version", 1)
        put("records", JsonArray(rows.toList()))
    }

    private fun change(p: JsonObject, key: String, value: JsonElement) =
        JsonObject(p + (key to value))

    @Test
    fun allKindsRoundTripTypedEvidenceWithoutInventingActualWorkoutsOrDecisions() {
        val root = root(*Kind.entries.map { row(it) }.toTypedArray())
        val snapshot = DevelopmentArchiveJson.decode(root)
        assertEquals(7, snapshot.records.size)
        assertEquals(root, DevelopmentArchiveJson.element(snapshot))
        assertEquals(
            87.25,
            (snapshot.records.first().evidence as Evidence.Weight).value.kilograms(),
        )
        assertEquals(
            HistoricalStrengthRecord.DatePrecision.UNKNOWN,
            (snapshot.records[1].evidence as Evidence.Historical).value.precision(),
        )
        assertNull((snapshot.records[1].evidence as Evidence.Historical).value.date())
    }

    @Test
    fun importedWeightsCanBeReadButOnlyManualWeightsAreIncludedInUserOwnedExport() {
        val imported =
            change(
                change(payload(Kind.WEIGHT, "imported"), "provider", JsonPrimitive("health-kit")),
                "origin",
                JsonPrimitive("local provider"),
            )
        val original = root(row(Kind.WEIGHT), row(Kind.WEIGHT, imported), row(Kind.HISTORICAL_PB))
        val snapshot = DevelopmentArchiveJson.decode(original)
        val owned = DevelopmentArchiveJson.decode(DevelopmentArchiveJson.userOwnedElement(snapshot))
        assertEquals(3, snapshot.records.size)
        assertEquals(listOf(Kind.WEIGHT, Kind.HISTORICAL_PB), owned.records.map { it.kind })
        assertEquals(original, DevelopmentArchiveJson.element(snapshot))
    }

    @Test
    fun duplicateIdentitiesUnknownKindsFutureVersionsAndExcessiveRowsFailClosed() {
        assertFails { DevelopmentArchiveJson.decode(root(row(Kind.WEIGHT), row(Kind.WEIGHT))) }
        assertEquals(
            2,
            DevelopmentArchiveJson.decode(
                    root(
                        row(Kind.WEIGHT, payload(Kind.WEIGHT, "same")),
                        row(Kind.INTERRUPTION, payload(Kind.INTERRUPTION, "same")),
                    )
                )
                .records
                .size,
        )
        assertFails { DevelopmentArchiveJson.decode(change(root(), "version", JsonPrimitive(2))) }
        assertFails {
            DevelopmentArchiveJson.decode(
                root(change(row(Kind.WEIGHT), "kind", JsonPrimitive("UNKNOWN")))
            )
        }
        assertFails {
            DevelopmentArchiveJson.decode(
                root(change(row(Kind.WEIGHT), "id", JsonPrimitive("another")))
            )
        }
        assertFails {
            DevelopmentArchiveJson.decode(
                change(root(), "records", JsonArray(List(100001) { row(Kind.WEIGHT) }))
            )
        }
    }

    @Test
    fun weightValuesAndProvenanceAreRequiredRatherThanAssumed() {
        for (bad in
            listOf(
                change(payload(Kind.WEIGHT), "kg", JsonPrimitive(0)),
                change(payload(Kind.WEIGHT), "kg", JsonPrimitive(1001)),
                change(payload(Kind.WEIGHT), "at", JsonPrimitive("unknown")),
                JsonObject(payload(Kind.WEIGHT) - "sourceId"),
            )) assertFails { DevelopmentArchiveJson.decode(root(row(Kind.WEIGHT, bad))) }
    }

    @Test
    fun historicalMeasurementProfilesAndDatePrecisionRemainIndependentEvidence() {
        for (profile in StrengthExercise.MeasurementProfile.entries) {
            val exercise =
                StrengthExercise(
                    "historical",
                    "Historical",
                    StrengthExercise.Region.FULL,
                    if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
                    else StrengthExercise.Mode.BODYWEIGHT,
                    false,
                    profile,
                )
            val p =
                JsonObject(
                    payload(Kind.HISTORICAL_PB) +
                        mapOf(
                            "exercise" to StrengthEvidenceJson.exercise(exercise),
                            "kg" to JsonPrimitive(if (profile.usesLoad()) 20.5 else 0.0),
                            "reps" to JsonPrimitive(if (profile.usesReps()) 6 else 0),
                            "seconds" to
                                (if (profile.usesDuration()) JsonPrimitive(45) else JsonNull),
                            "metres" to
                                (if (profile.usesDistance()) JsonPrimitive(123.5) else JsonNull),
                        )
                )
            val record =
                DevelopmentArchiveJson.decode(root(row(Kind.HISTORICAL_PB, p)))
                    .records
                    .single()
                    .evidence as Evidence.Historical
            assertEquals(profile, record.value.exercise().measurement())
            assertNull(record.value.date())
        }
        assertFails {
            DevelopmentArchiveJson.decode(
                root(
                    row(
                        Kind.HISTORICAL_PB,
                        change(payload(Kind.HISTORICAL_PB), "precision", JsonPrimitive("EXACT")),
                    )
                )
            )
        }
    }

    @Test
    fun moveAndGymLinkPairingRulesArePreservedWithoutActivatingChanges() {
        assertFails {
            DevelopmentArchiveJson.decode(
                root(
                    row(
                        Kind.SESSION_DECISION,
                        change(payload(Kind.SESSION_DECISION), "kind", JsonPrimitive("MOVED")),
                    )
                )
            )
        }
        assertFails {
            DevelopmentArchiveJson.decode(
                root(
                    row(
                        Kind.STRENGTH_LINK,
                        change(
                            payload(Kind.STRENGTH_LINK),
                            "gymVersion",
                            JsonPrimitive("programme:v1"),
                        ),
                    )
                )
            )
        }
        val linked =
            change(
                change(payload(Kind.STRENGTH_LINK), "gymVersion", JsonPrimitive("programme:v1")),
                "gymSession",
                JsonPrimitive("lower"),
            )
        assertEquals(
            "lower",
            (DevelopmentArchiveJson.decode(root(row(Kind.STRENGTH_LINK, linked)))
                    .records
                    .single()
                    .evidence as Evidence.StrengthLink)
                .gymSession,
        )
    }

    @Test
    fun legacyWeeklyReviewsKeepUnknownFieldsAndDoNotRequireNewChoiceOrChannelShapes() {
        val root = root(row(Kind.WEEKLY_REVIEW))
        val snapshot = DevelopmentArchiveJson.decode(root)
        assertEquals(4, (snapshot.records.single().evidence as Evidence.WeeklyReview).completed)
        assertEquals(root, DevelopmentArchiveJson.element(snapshot))
        for (bad in
            listOf(
                change(payload(Kind.WEEKLY_REVIEW), "km", JsonPrimitive(-1)),
                change(payload(Kind.WEEKLY_REVIEW), "channels", JsonNull),
                change(
                    payload(Kind.WEEKLY_REVIEW),
                    "choices",
                    buildJsonArray { add(buildJsonObject {}) },
                ),
            )) assertFails { DevelopmentArchiveJson.decode(root(row(Kind.WEEKLY_REVIEW, bad))) }
    }

    @Test
    fun snapshotsDoNotExposeCallerOwnedPayloadMapsOrMutableRecordLists() {
        val mutable = payload(Kind.WEIGHT).toMutableMap()
        val snapshot = DevelopmentArchiveJson.decode(root(row(Kind.WEIGHT, JsonObject(mutable))))
        mutable["kg"] = JsonPrimitive(50)
        assertEquals("87.25", snapshot.records.single().payload["kg"].toString())
        assertFails { (snapshot.records as MutableList).clear() }
    }
}
