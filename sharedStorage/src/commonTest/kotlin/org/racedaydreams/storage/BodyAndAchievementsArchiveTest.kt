package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.time.*

class BodyAndAchievementsArchiveTest {
    private val at = StoredInstant.parse("2026-11-16T10:00:00.123456789Z")

    private fun height(id: String = "height", provider: String = "manual", source: String = id) =
        BodyHeight(id, 1.83, at, null, provider, source, null, null)

    private fun body() =
        BodyMeasurementsArchiveJson.Snapshot(
            listOf(height(), height("imported", "health-kit", "provider-id")),
            Preferences(
                true,
                HeightUnit.CENTIMETRES,
                ProteinGuidance.Basis("retained-weight", at, 87.25),
            ),
        )

    private fun change(p: JsonObject, key: String, value: JsonElement) =
        JsonObject(p + (key to value))

    private fun award() =
        AchievementsArchiveJson.Snapshot(
            false,
            listOf(
                SqliteTrainingMilestones.Stored(
                    Award(
                        Kind.FIRST_FIVE_KM_RUN,
                        at,
                        listOf(Proof(Source.RUN, "retained-run", at, null, 5000.5)),
                    ),
                    at,
                )
            ),
        )

    @Test
    fun bodyRoundTripPreservesOptionalProvenanceAndReferenceRatherThanInventingWeights() {
        val before = body()
        val after = BodyMeasurementsArchiveJson.decode(BodyMeasurementsArchiveJson.element(before))
        assertEquals(before.heights, after.heights)
        assertEquals(before.preferences, after.preferences)
        assertNull(after.heights.first().offset())
        assertNull(after.heights.last().importedAt())
        assertEquals("health-kit", after.heights.last().provider())
        assertEquals("retained-weight", after.preferences.basis()!!.id())
        assertEquals(87.25, after.preferences.basis()!!.kilograms())
    }

    @Test
    fun missingOlderSectionsDefaultToNoGuidanceAndNoCelebrations() {
        val body = BodyMeasurementsArchiveJson.decode(null)
        assertEquals(Preferences.defaults(), body.preferences)
        assertTrue(body.heights.isEmpty())
        val milestones = AchievementsArchiveJson.decode(null)
        assertFalse(milestones.celebrations)
        assertTrue(milestones.awards.isEmpty())
    }

    @Test
    fun heightIdentityAndSourceDuplicatesFailWithoutDroppingHistory() {
        assertFails {
            BodyMeasurementsArchiveJson.Snapshot(listOf(height(), height()), Preferences.defaults())
        }
        assertFails {
            BodyMeasurementsArchiveJson.Snapshot(
                listOf(height(), height("other", "manual", "height")),
                Preferences.defaults(),
            )
        }
        assertFails {
            BodyMeasurementsArchiveJson.Snapshot(List(100001) { height() }, Preferences.defaults())
        }
    }

    @Test
    fun bodyFieldsVersionsAndInvalidMeasurementsRejectBeforeUse() {
        val original = BodyMeasurementsArchiveJson.element(body())
        val row = original.getValue("heights").jsonArray.first().jsonObject
        for (bad in
            listOf(
                change(row, "metres", JsonPrimitive(0)),
                change(row, "metres", JsonPrimitive("1.83")),
                change(row, "provider", JsonPrimitive("")),
                change(row, "at", JsonPrimitive("unknown")),
                change(row, "extra", JsonNull),
            )) assertFails {
            BodyMeasurementsArchiveJson.decode(change(original, "heights", JsonArray(listOf(bad))))
        }
        assertFails {
            BodyMeasurementsArchiveJson.decode(change(original, "version", JsonPrimitive(2)))
        }
        assertFails { BodyMeasurementsArchiveJson.decode(change(original, "extra", JsonNull)) }
        val p = original.getValue("preferences").jsonObject
        assertFails {
            BodyMeasurementsArchiveJson.decode(
                change(original, "preferences", JsonObject(p - "basis"))
            )
        }
        assertFails {
            BodyMeasurementsArchiveJson.decode(
                change(original, "preferences", change(p, "proteinEnabled", JsonPrimitive("true")))
            )
        }
    }

    @Test
    fun canonicalSecondOffsetsAndImportedTimestampsRemainPrecise() {
        val h =
            BodyHeight(
                "height",
                1.83,
                at,
                Offsets.ofTotalSeconds(19815),
                "health-connect",
                "provider-source",
                "provider.package",
                at,
            )
        val original = BodyMeasurementsArchiveJson.Snapshot(listOf(h), Preferences.defaults())
        assertEquals(
            h,
            BodyMeasurementsArchiveJson.decode(BodyMeasurementsArchiveJson.element(original))
                .heights
                .single(),
        )
    }

    @Test
    fun retainedAchievementsKeepProofAndRecognitionTimeWithoutReawarding() {
        val original = award()
        val parsed = AchievementsArchiveJson.decode(AchievementsArchiveJson.element(original))
        assertEquals(original.awards, parsed.awards)
        assertFalse(parsed.celebrations)
        assertEquals("retained-run", parsed.awards.single().award.evidence().single().recordId())
        assertNull(parsed.awards.single().award.evidence().single().offset())
    }

    @Test
    fun milestoneIdentityChronologyAndEvidenceCannotBeForged() {
        val original = AchievementsArchiveJson.element(award())
        val row = original.getValue("awards").jsonArray.single().jsonObject
        assertFails {
            AchievementsArchiveJson.decode(change(original, "awards", JsonArray(listOf(row, row))))
        }
        for (bad in
            listOf(
                change(row, "recognizedAt", JsonPrimitive("2020-01-01T00:00:00Z")),
                change(row, "earnedAt", JsonPrimitive("2020-01-01T00:00:00Z")),
                change(row, "kind", JsonPrimitive("FIFTY_GYM")),
                change(row, "evidence", JsonArray(emptyList())),
            )) assertFails {
            AchievementsArchiveJson.decode(change(original, "awards", JsonArray(listOf(bad))))
        }
        val proof = row.getValue("evidence").jsonArray.single().jsonObject
        for (bad in
            listOf(
                change(proof, "source", JsonPrimitive("GYM")),
                change(proof, "distance", JsonPrimitive(4999)),
                change(proof, "offset", JsonPrimitive(64801)),
                change(proof, "extra", JsonNull),
            )) assertFails {
            AchievementsArchiveJson.decode(
                change(
                    original,
                    "awards",
                    JsonArray(listOf(change(row, "evidence", JsonArray(listOf(bad))))),
                )
            )
        }
        assertFails {
            AchievementsArchiveJson.decode(change(original, "version", JsonPrimitive(2)))
        }
        assertFails {
            AchievementsArchiveJson.decode(change(original, "celebrations", JsonPrimitive("false")))
        }
    }

    @Test
    fun allOriginalMilestoneKindsReuseExistingProofEligibilityRules() {
        for (kind in Kind.entries) {
            val evidence =
                (0 until kind.count()).map { index ->
                    Proof(
                        if (kind in listOf(Kind.FIRST_GYM, Kind.FIVE_GYM, Kind.FIFTY_GYM))
                            Source.GYM
                        else Source.RUN,
                        "actual-$index",
                        at.plusSeconds(index * 7L * 86400),
                        Offsets.ofTotalSeconds(0),
                        if (kind in listOf(Kind.FIRST_GYM, Kind.FIVE_GYM, Kind.FIFTY_GYM)) null
                        else 5000.0,
                    )
                }
            val earned = evidence.last().at()
            val archive =
                AchievementsArchiveJson.Snapshot(
                    true,
                    listOf(SqliteTrainingMilestones.Stored(Award(kind, earned, evidence), earned)),
                )
            assertEquals(
                archive.awards,
                AchievementsArchiveJson.decode(AchievementsArchiveJson.element(archive)).awards,
            )
        }
    }

    @Test
    fun archiveListsDoNotExposeMutableCallerCollections() {
        val heights = mutableListOf(height())
        val body = BodyMeasurementsArchiveJson.Snapshot(heights, Preferences.defaults())
        heights.clear()
        assertEquals(1, body.heights.size)
        assertFails { (body.heights as MutableList).clear() }
        val awards = award().awards.toMutableList()
        val snapshot = AchievementsArchiveJson.Snapshot(false, awards)
        awards.clear()
        assertEquals(1, snapshot.awards.size)
        assertFails { (snapshot.awards as MutableList).clear() }
    }
}
