package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class GymExecutionSnapshotJsonTest {
    private val now = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val rowExercise = ExerciseCatalog.definition("dumbbell-row")!!.exercise()
    private val clock = 9007199254741017L

    private fun tick(delta: Long, epoch: String = "boot") =
        GymExecution.Tick(
            epoch,
            clock + delta,
            Instants.ofEpochSecond(now.getEpochSecond() + delta / 1000, now.getNano().toLong()),
        )

    private fun target() =
        GymWorkoutDefinition.Target(
            2,
            4,
            6,
            null,
            null,
            GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.5, 4.5),
            180,
            82.25,
        )

    private fun start(
        grouped: Boolean = false,
        source: GymExecution.SourceReference? = null,
    ): GymExecutionEngine {
        val slots =
            if (grouped)
                listOf(
                    GymWorkoutDefinition.Slot("a", bench, target(), "pair"),
                    GymWorkoutDefinition.Slot("b", rowExercise, target(), "pair"),
                )
            else listOf(GymWorkoutDefinition.Slot("a", bench, target(), null))
        val workout =
            GymWorkoutDefinition(
                if (source == null) "workout" else GymWorkoutAdapter.programmeWorkoutId(source),
                source?.revision() ?: 1,
                now,
                "Upper",
                slots,
            )
        return GymExecutionEngine.start(
            "execution",
            if (source == null) GymExecution.Origin.CUSTOM else GymExecution.Origin.PLANNED,
            workout,
            source,
            Offsets.ofTotalSeconds(19815),
            tick(0),
        )
    }

    private fun actual(id: String = "actual", warm: Boolean = false) =
        StrengthSession.SetEntry(id, 6, 80.0, null, warm, null, null, 2.5)

    private fun roundTrip(snapshot: GymExecution): GymExecution =
        GymExecutionSnapshotJson.decode(GymExecutionSnapshotJson.encode(snapshot)).also {
            assertEquals(snapshot, it)
        }

    private fun mutate(snapshot: GymExecution, change: (JsonObject) -> JsonObject): String =
        change(Json.parseToJsonElement(GymExecutionSnapshotJson.encode(snapshot)).jsonObject)
            .toString()

    @Test
    fun activePausedAndFinishedSnapshotsKeepExactClocksAndActualsSeparateFromTargets() {
        val engine = start()
        roundTrip(engine.snapshot())
        engine.saveSet("a", actual("warm", true), tick(100))
        engine.saveSet("a", actual(), tick(1000))
        roundTrip(engine.pause(tick(2000)))
        engine.resume(tick(3000))
        val ended = roundTrip(engine.finish(tick(5000), null, "entered 🏋️\u0000 notes", true))
        assertEquals(4000L, ended.activeMillis())
        assertEquals(5000L, ended.recordedElapsedMillis())
        assertEquals(clock + 5000, ended.lastTick().monotonicMillis())
        assertNull(ended.completedSession()!!.rpe())
        assertTrue(ended.actualSets()[0].actual().warmUp())
        assertEquals(2.5, ended.actualSets()[1].actual().repetitionsInReserve())
        assertEquals(82.25, ended.prescription()!!.slots().single().target()!!.referenceLoadKg())
    }

    @Test
    fun recoveryNeverInventsDowntimeOrResume() {
        val engine = start()
        engine.saveSet("a", actual(), tick(1000))
        val recovered =
            GymExecutionEngine.recover(roundTrip(engine.snapshot()), tick(100000, "new"))
        val snapshot = roundTrip(recovered.snapshot())
        assertEquals(GymExecution.Status.RECOVERY_REQUIRED, snapshot.status())
        assertEquals(1000L, snapshot.activeMillis())
        assertNull(snapshot.completedSession())
        recovered.resume(tick(200000, "new"))
        roundTrip(recovered.finish(tick(201000, "new"), 7.5, "", true))
    }

    @Test
    fun substitutionKeepsOriginalPrescriptionActualExerciseAndSupersetHistory() {
        val engine = start(true)
        engine.saveSet("a", actual("original"), tick(100))
        engine.replaceExercise("a", rowExercise, tick(200))
        engine.saveSet("a", actual("replacement"), tick(300))
        engine.breakSuperset("b", tick(400))
        val ended = roundTrip(engine.finish(tick(1000), null, "", true))
        assertEquals(bench, ended.prescription()!!.slots()[0].exercise())
        assertEquals(listOf(bench, rowExercise), ended.actualSets().map { it.exercise() })
        assertNull(ended.slots()[0].target()!!.referenceLoadKg())
        assertTrue(ended.slots().all { it.supersetGroup() == null })
        assertTrue(ended.prescription()!!.slots().all { it.supersetGroup() == "pair" })
    }

    @Test
    fun sourceProvenanceAndFreeUnfinishedStateRemainHonest() {
        val source =
            GymExecution.SourceReference(
                GymExecution.SourceKind.PROGRAMME,
                "programme",
                3,
                "planned-session",
            )
        assertEquals(source, roundTrip(start(source = source).snapshot()).source())
        val free =
            GymExecutionEngine.start(
                    "free",
                    GymExecution.Origin.FREE,
                    null,
                    null,
                    Offsets.ofTotalSeconds(0),
                    tick(0),
                )
                .snapshot()
        val decoded = roundTrip(free)
        assertNull(decoded.prescription())
        assertNull(decoded.source())
        assertTrue(decoded.slots().isEmpty())
        assertTrue(decoded.actualSets().isEmpty())
        assertNull(decoded.completedSession())
    }

    @Test
    fun forgedTimingEventsAndCompletionFailDomainReplayInsteadOfCreatingEvidence() {
        val engine = start()
        engine.saveSet("a", actual(), tick(1000))
        val snapshot = engine.snapshot()
        val invalid =
            listOf(
                mutate(snapshot) { JsonObject(it + ("activeMillis" to JsonPrimitive(2000))) },
                mutate(snapshot) { JsonObject(it + ("status" to JsonPrimitive("FINISHED"))) },
                mutate(snapshot) { JsonObject(it + ("events" to JsonArray(emptyList()))) },
                mutate(snapshot) { JsonObject(it + ("actualSets" to JsonArray(emptyList()))) },
            )
        for (payload in invalid) {
            assertFailsWith<IllegalArgumentException> { GymExecutionSnapshotJson.decode(payload) }
        }
    }

    @Test
    fun strictClockTokensUnknownFieldsAndOversizeEventCollectionsAreRejected() {
        val snapshot = start().snapshot()
        val encoded = GymExecutionSnapshotJson.encode(snapshot)
        assertFailsWith<IllegalArgumentException> {
            GymExecutionSnapshotJson.decode(
                encoded.replace("\"activeMillis\":0", "\"activeMillis\":0e0")
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymExecutionSnapshotJson.decode(
                mutate(snapshot) { JsonObject(it + ("unknown" to JsonNull)) }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymExecutionSnapshotJson.decode(
                mutate(snapshot) {
                    JsonObject(it + ("events" to JsonArray(List(50001) { JsonNull })))
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymExecutionSnapshotJson.decode(
                mutate(snapshot) {
                    JsonObject(it + ("actualSets" to JsonArray(List(10001) { JsonNull })))
                }
            )
        }
    }
}
