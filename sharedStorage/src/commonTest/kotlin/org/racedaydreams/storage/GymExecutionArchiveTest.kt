package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.GymEngineArchiveFixtures.at
import org.racedaydreams.storage.GymEngineArchiveFixtures.engine
import org.racedaydreams.storage.GymEngineArchiveFixtures.entry
import org.racedaydreams.storage.GymEngineArchiveFixtures.finish
import org.racedaydreams.storage.GymEngineArchiveFixtures.root
import org.racedaydreams.storage.GymEngineArchiveFixtures.tick

class GymExecutionArchiveTest {
    // Calls below use the real portable original renderer + platform SHA-256, never a hash lookup.
    private fun decode(
        root: JsonObject,
        actuals: Map<String, StrengthSession> = emptyMap(),
        legacy: Map<String, GymProgramme> = emptyMap(),
        links: Map<String, GymAssociationArchiveJson.Link> = emptyMap(),
    ) =
        GymExecutionArchiveJson.decode(
            root,
            actuals,
            legacy,
            links,
            emptyMap(),
            PortableLegacyStrengthFingerprint,
        )

    private fun change(p: JsonObject, k: String, v: JsonElement) = JsonObject(p + (k to v))

    @Test
    fun originalV1AndV2RetainAcceptedActualsAndPreciseRevisionsSeparatelyFromExecution() {
        val e = finish()
        for (v in 1..2) {
            val p = root(listOf(entry(e)), v = v)
            val s = decode(p, mapOf(e.id() to e.completedSession()!!))
            assertEquals(p, GymExecutionArchiveJson.element(s))
            assertEquals(9007199254740993L, s.executions.getValue(e.id()).original.revision())
            assertEquals(e, s.executions.getValue(e.id()).original.execution())
            assertEquals(e.completedSession(), s.executions.getValue(e.id()).currentActual)
            assertEquals(v == 1, s.associations == null)
        }
    }

    @Test
    fun unfinishedAndDiscardedSnapshotsCannotBecomeActualEvidenceOrClaimSeveralActiveWorkouts() {
        val active = engine().snapshot()
        decode(root(listOf(entry(active))))
        assertFails { decode(root(listOf(entry(active), entry(engine("another").snapshot())))) }
        assertFails {
            decode(root(listOf(entry(active))), mapOf("actual" to finish().completedSession()!!))
        }
        assertFails { decode(root(listOf(entry(active, finish().completedSession())))) }
        assertFails { decode(root(listOf(entry(active, edit = at.toString())))) }
        val one = engine("one").discard(tick(1000))
        val two = engine("two").discard(tick(1000))
        assertEquals(3, decode(root(listOf(entry(active), entry(one), entry(two)))).executions.size)
    }

    @Test
    fun explicitActualEditsRequireBothMatchingCoachingEvidenceAndTheirEditObservation() {
        val e = finish()
        val original = e.completedSession()!!
        val edited = original.copy(notes = "Athlete edited actual")
        assertFails { decode(root(listOf(entry(e))), emptyMap()) }
        assertFails { decode(root(listOf(entry(e, edited))), mapOf(e.id() to edited)) }
        assertFails {
            decode(root(listOf(entry(e, edited, at.toString()))), mapOf(e.id() to original))
        }
        val s = decode(root(listOf(entry(e, edited, at.toString()))), mapOf(e.id() to edited))
        assertEquals(
            original,
            s.executions.getValue(e.id()).original.execution().completedSession(),
        )
        assertEquals(edited, s.executions.getValue(e.id()).currentActual)
    }

    @Test
    fun duplicateIdentitiesBadVersionsUnknownFieldsAndNonPositiveRevisionsReject() {
        val e = engine().snapshot()
        val r = entry(e)
        val p = root(listOf(r))
        for (bad in
            listOf(
                change(p, "version", JsonPrimitive(3)),
                change(p, "extra", JsonNull),
                root(listOf(r, r)),
                root(listOf(change(r, "revision", JsonPrimitive(0)))),
                root(listOf(change(r, "revision", JsonPrimitive("1")))),
            )) assertFails { decode(bad) }
        assertFails { decode(change(p, "executions", JsonArray(List(100001) { r }))) }
    }

    @Test
    fun plannedExecutionNeedsItsExactFrozenPrescriptionProgrammeAndOriginalLink() {
        val legacy = LegacyGymArchiveFixtures.programme()
        val prepared = GymWorkoutAdapter.fromProgramme(legacy, "session")
        val e = finish(p = prepared)
        val w = FrozenGymWorkoutRecordJson.Record(prepared.definition(), prepared.source(), null)
        val p = root(listOf(entry(e)), listOf(w))
        val actuals = mapOf(e.id() to e.completedSession()!!)
        val plans = mapOf("programme:v1" to legacy)
        val link =
            GymAssociationArchiveJson.Link(
                e.id(),
                "programme:v1",
                "session",
                legacy.start().toString(),
            )
        assertEquals(1, decode(p, actuals, plans, mapOf(e.id() to link)).workouts.size)
        assertFails { decode(root(listOf(entry(e))), actuals, plans, mapOf(e.id() to link)) }
        assertFails { decode(p, actuals, emptyMap(), mapOf(e.id() to link)) }
        assertFails { decode(p, actuals, plans, emptyMap()) }
        assertFails {
            decode(p, actuals, plans, mapOf(e.id() to link.copy(plannedDate = "2026-11-18")))
        }
        assertEquals(50.0, e.completedSession()!!.exercises().single().sets().first().kilograms())
    }

    @Test
    fun preferenceMustNameAnExistingUniqueFrozenRevision() {
        val legacy = LegacyGymArchiveFixtures.programme()
        val p = GymWorkoutAdapter.fromProgramme(legacy, "session")
        val w = FrozenGymWorkoutRecordJson.Record(p.definition(), p.source(), null)
        val preference = buildJsonObject {
            put("id", p.definition().id())
            put("currentRevision", p.definition().revision())
            put("favourite", true)
        }
        val root = root(workouts = listOf(w))
        val plans = mapOf("programme:v1" to legacy)
        assertTrue(
            decode(change(root, "preferences", JsonArray(listOf(preference))), legacy = plans)
                .preferences
                .single()
                .favourite()
        )
        assertFails {
            decode(
                change(root, "preferences", JsonArray(listOf(preference, preference))),
                legacy = plans,
            )
        }
        assertFails {
            decode(
                change(
                    root,
                    "preferences",
                    JsonArray(listOf(change(preference, "currentRevision", JsonPrimitive(999)))),
                ),
                legacy = plans,
            )
        }
        assertFails { decode(root(workouts = listOf(w, w)), legacy = plans) }
    }

    @Test
    fun repeatedActualRetainsOriginalFingerprintAndRequiresCurrentEditOrDeletionReceipts() {
        val actual = finish("source").completedSession()!!
        val p = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
        val hash = PortableLegacyStrengthFingerprint.fingerprint(actual)
        val proof =
            FrozenGymWorkoutRecordJson.RepeatProof(
                FrozenGymWorkoutRecordJson.TemplateKind.ACTUAL,
                p.definition().slots(),
                hash,
                hash,
                null,
                null,
            )
        val w = FrozenGymWorkoutRecordJson.Record(p.definition(), p.source(), proof)
        decode(root(workouts = listOf(w)), mapOf(actual.id() to actual))
        val changed = actual.copy(notes = "Explicit edit")
        assertFails { decode(root(workouts = listOf(w)), mapOf(actual.id() to changed)) }
        val acknowledged =
            w.copy(
                sourceSnapshot =
                    proof.copy(
                        sourceEditedAt = at,
                        currentSourceFingerprint =
                            PortableLegacyStrengthFingerprint.fingerprint(changed),
                    )
            )
        assertEquals(
            hash,
            decode(root(workouts = listOf(acknowledged)), mapOf(actual.id() to changed))
                .workouts
                .values
                .single()
                .sourceSnapshot!!
                .originalSourceFingerprint,
        )
        val deleted =
            w.copy(
                sourceSnapshot = proof.copy(sourceDeletedAt = at, currentSourceFingerprint = null)
            )
        decode(root(workouts = listOf(deleted)))
        assertFails { decode(root(workouts = listOf(deleted)), mapOf(actual.id() to actual)) }
    }

    @Test
    fun repeatedExecutionRequiresTheOriginalRetainedStructure() {
        val source = finish("source")
        val actual = source.completedSession()!!
        val p = GymWorkoutAdapter.repeatExecution("repeat", 1, at, "Repeat", source)
        val hash = PortableLegacyStrengthFingerprint.fingerprint(actual)
        val w =
            FrozenGymWorkoutRecordJson.Record(
                p.definition(),
                p.source(),
                FrozenGymWorkoutRecordJson.RepeatProof(
                    FrozenGymWorkoutRecordJson.TemplateKind.EXECUTION,
                    source.slots(),
                    hash,
                    hash,
                    null,
                    null,
                ),
            )
        decode(root(listOf(entry(source)), listOf(w)), mapOf(actual.id() to actual))
        assertFails { decode(root(workouts = listOf(w)), mapOf(actual.id() to actual)) }
    }

    @Test
    fun associationHistoryKeepsExplicitCountOrExtraChoicesAndDoesNotDropUnassociatedLegacyRows() {
        val e = finish()
        val actual = e.completedSession()!!
        val legacy = LegacyGymArchiveFixtures.programme()
        assertTrue(
            GymWorkoutAssociation.proposals(actual, e.origin(), legacy, emptySet(), at).isEmpty()
        )
        val reviewedAt = tick(4000).wallTime()
        val proposal =
            GymWorkoutAssociation.proposals(actual, e.origin(), legacy, emptySet(), reviewedAt)
                .single()
        val count =
            GymWorkoutAssociation.Decision(
                "count",
                proposal,
                GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION,
                reviewedAt,
            )
        val extra = count.copy(id = "extra", choice = GymWorkoutAssociation.Choice.KEEP_EXTRA)
        val plans = mapOf("programme:v1" to legacy)
        val actuals = mapOf(actual.id() to actual)
        val counted =
            GymAssociationArchiveJson.Archive(listOf(count), mapOf(actual.id() to "count"))
        val link =
            GymAssociationArchiveJson.Link(
                actual.id(),
                "programme:v1",
                "session",
                legacy.start().toString(),
            )
        decode(
            root(listOf(entry(e)), associations = counted),
            actuals,
            plans,
            mapOf(actual.id() to link),
        )
        assertFails { decode(root(listOf(entry(e)), associations = counted), actuals, plans) }
        val independent =
            GymAssociationArchiveJson.Archive(listOf(extra), mapOf(actual.id() to "extra"))
        decode(root(listOf(entry(e)), associations = independent), actuals, plans)
        val unassociated =
            GymAssociationArchiveJson.Link(actual.id(), null, null, legacy.start().toString())
        assertFails {
            decode(
                root(listOf(entry(e)), associations = independent),
                actuals,
                plans,
                mapOf(actual.id() to unassociated),
            )
        }
    }

    @Test
    fun snapshotsDoNotExposeMutableCallerCollectionsOrRewriteRawReceipts() {
        val map = root().toMutableMap()
        val s = decode(JsonObject(map))
        map["version"] = JsonPrimitive(99)
        assertEquals(2, GymExecutionArchiveJson.element(s).getValue("version").jsonPrimitive.int)
        assertFails { (s.workouts as MutableMap).clear() }
        assertFails { (s.preferences as MutableList).clear() }
        assertFails { (s.executions as MutableMap).clear() }
        assertFails { (s.associations!!.decisions as MutableList).clear() }
        assertFails { (s.associations!!.current as MutableMap).clear() }
    }
}
