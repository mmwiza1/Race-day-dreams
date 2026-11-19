package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.GymExecutionSnapshotJson

@RunWith(AndroidJUnit4::class)
class SharedGymExecutionCodecTest {
    private val now = Instant.parse("2026-11-15T09:00:00.123456789Z")
    private val clock = 9007199254741017L
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val row = ExerciseCatalog.definition("dumbbell-row")!!.exercise()

    private fun tick(delta: Long) = GymExecution.Tick("boot", clock + delta, now.plusMillis(delta))

    private fun actual(id: String, warm: Boolean = false) =
        StrengthSession.SetEntry(id, 6, 80.0, null, warm, null, null, 2.5)

    private fun start(grouped: Boolean = false): GymExecutionEngine {
        val source =
            GymExecution.SourceReference(
                GymExecution.SourceKind.PROGRAMME,
                "programme",
                3,
                "planned-session",
            )
        val target =
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
        val slots =
            if (grouped)
                listOf(
                    GymWorkoutDefinition.Slot("a", bench, target, "pair"),
                    GymWorkoutDefinition.Slot("b", row, target, "pair"),
                )
            else listOf(GymWorkoutDefinition.Slot("a", bench, target, null))
        val workout =
            GymWorkoutDefinition(
                GymWorkoutAdapter.programmeWorkoutId(source),
                3,
                now,
                "Upper",
                slots,
            )
        return GymExecutionEngine.start(
            "execution",
            GymExecution.Origin.PLANNED,
            workout,
            source,
            ZoneOffset.ofTotalSeconds(19815),
            tick(0),
        )
    }

    private fun verify(snapshot: GymExecution) {
        assertEquals(snapshot, GymExecutionSnapshotJson.decode(GymExecutionJson.write(snapshot)))
        assertEquals(snapshot, GymExecutionJson.read(GymExecutionSnapshotJson.encode(snapshot)))
    }

    @Test
    fun originalAndSharedCodecsPreserveLifecycleRecoverySourceAndSubstitutionEvidence() {
        val engine = start(true)
        verify(engine.snapshot())
        engine.saveSet("a", actual("warm", true), tick(100))
        engine.saveSet("a", actual("original"), tick(1000))
        verify(engine.pause(tick(2000)))
        engine.resume(tick(3000))
        engine.replaceExercise("a", row, tick(3100))
        engine.saveSet("a", actual("replacement"), tick(4000))
        engine.breakSuperset("b", tick(4500))
        val ended = engine.finish(tick(5000), null, "Entered 🏋️\u0000 notes", true)
        verify(ended)
        assertEquals(listOf(bench, bench, row), ended.actualSets().map { it.exercise() })
        assertNull(ended.slots().first().target()!!.referenceLoadKg())
        assertEquals(
            82.25,
            ended.prescription()!!.slots().first().target()!!.referenceLoadKg()!!,
            0.0,
        )
        val unfinished = start()
        unfinished.saveSet("a", actual("recorded"), tick(1000))
        val recovered =
            GymExecutionEngine.recover(
                    unfinished.snapshot(),
                    GymExecution.Tick("restart", 0, now.plusSeconds(100)),
                )
                .snapshot()
        verify(recovered)
        assertEquals(1000L, recovered.activeMillis())
        assertNull(recovered.completedSession())
    }

    @Test
    fun bothReadersRejectForgedEvidenceUnknownFieldsAndExponentClockTokens() {
        val engine = start()
        engine.saveSet("a", actual("recorded"), tick(1000))
        val original = GymExecutionJson.write(engine.snapshot())
        val json = GymExecutionJson.encode(engine.snapshot())
        json.put("actualSets", org.json.JSONArray())
        val invalid =
            listOf(
                original.replace("\"activeMillis\":1000", "\"activeMillis\":1e3"),
                original.dropLast(1) + ",\"unknown\":null}",
                json.toString(),
            )
        for (payload in invalid) {
            assertNotEquals(original, payload)
            assertThrows(IllegalArgumentException::class.java) { GymExecutionJson.read(payload) }
            assertThrows(IllegalArgumentException::class.java) {
                GymExecutionSnapshotJson.decode(payload)
            }
        }
    }
}
