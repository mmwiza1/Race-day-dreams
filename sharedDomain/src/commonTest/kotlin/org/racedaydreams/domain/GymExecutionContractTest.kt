package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymExecution.*
import org.racedaydreams.domain.time.*

/** The real execution kernel, exercised on JVM and Apple with clocks supplied by the caller. */
class GymExecutionContractTest {
    private val now = Instants.ofEpochSecond(1793102400)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val row = ExerciseCatalog.definition("dumbbell-row")!!.exercise()

    private fun tick(millis: Long, epoch: String = "boot", wall: Instant = now) =
        Tick(epoch, millis, wall)

    private fun actual(id: String = "set", warmUp: Boolean = false) =
        StrengthSession.SetEntry(id, 6, 80.0, 7.0, warmUp)

    private fun target() =
        GymWorkoutDefinition.Target(
            2,
            4,
            6,
            null,
            null,
            GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 6.0, 8.0),
            180,
            80.0,
        )

    private fun workout() =
        GymWorkoutDefinition(
            "workout",
            1,
            now,
            "Upper",
            listOf(GymWorkoutDefinition.Slot("a", bench, target(), null)),
        )

    private fun start(prescription: GymWorkoutDefinition = workout()) =
        GymExecutionEngine.start(
            "execution",
            Origin.CUSTOM,
            prescription,
            null,
            Offsets.ofTotalSeconds(3600),
            tick(0),
        )

    @Test
    fun wallClockDoesNotControlTimingAndPausesDoNotBecomeActiveTime() {
        val engine = start()
        val original = engine.snapshot()
        engine.saveSet("a", actual(), tick(1000, wall = Instants.ofEpochSecond(0)))
        engine.pause(tick(2000))
        engine.observe(tick(10000, wall = Instants.ofEpochSecond(900000000)))
        engine.resume(tick(11000))
        val ended = engine.finish(tick(13000), null, null, false)
        assertEquals(13000L, ended.recordedElapsedMillis())
        assertEquals(4000L, ended.activeMillis())
        assertEquals(Durations.ofMillis(4000), ended.completedSession()!!.duration())
        assertTrue(ended.partial())
        assertNull(ended.completedSession()!!.rpe())
        assertTrue(original.actualSets().isEmpty())
        assertNull(original.completedSession())
    }

    @Test
    fun recoveryExcludesUnknownDowntimeAndRequiresExplicitResume() {
        val engine = start()
        engine.saveSet("a", actual(), tick(2000))
        val recovered = GymExecutionEngine.recover(engine.snapshot(), tick(900000, "new"))
        assertEquals(Status.RECOVERY_REQUIRED, recovered.snapshot().status())
        assertEquals(2000L, recovered.observe(tick(999000, "new")).activeMillis())
        assertFailsWith<IllegalArgumentException> {
            recovered.saveSet("a", actual("blocked"), tick(999001, "new"))
        }
        recovered.resume(tick(1000000, "new"))
        val finished = recovered.finish(tick(1001000, "new"), 7.0, "", false)
        assertEquals(3000L, finished.activeMillis())
        assertEquals(3000L, finished.recordedElapsedMillis())
        val paused = start()
        paused.pause(tick(100))
        val pausedRecovery = GymExecutionEngine.recover(paused.snapshot(), tick(50, "restart"))
        assertEquals(Status.PAUSED, pausedRecovery.snapshot().status())
        assertEquals(100L, pausedRecovery.observe(tick(1000, "restart")).activeMillis())
        assertEquals(1050L, pausedRecovery.snapshot().recordedElapsedMillis())
    }

    @Test
    fun stableIdRetriesAndFailedCommandsNeverOverwriteEvidence() {
        val engine = start()
        val entered = GymExecutionSet("a", bench, actual())
        val saved = engine.saveSet(entered, tick(500))
        assertSame(saved, engine.saveSet(entered, null))
        assertFailsWith<IllegalArgumentException> {
            engine.saveSet(
                GymExecutionSet("a", bench, StrengthSession.SetEntry("set", 5, 80.0, 7.0, false)),
                tick(600),
            )
        }
        assertSame(saved, engine.snapshot())
        assertFailsWith<IllegalArgumentException> { engine.observe(tick(400)) }
        assertSame(saved, engine.snapshot())
        assertFailsWith<IllegalArgumentException> { engine.observe(tick(900, "other")) }
        assertSame(saved, engine.snapshot())
        val ended = engine.finish(tick(700), 7.0, "notes", true)
        assertSame(ended, engine.saveSet(entered, null))
        assertSame(ended, engine.finish(null, 7.0, "notes", false))
        assertFailsWith<IllegalArgumentException> { engine.finish(null, 8.0, "notes", true) }
        assertSame(ended, engine.snapshot())
    }

    @Test
    fun warmupsAndTargetsNeverBecomePerformedWorkingSets() {
        val engine = start()
        assertFailsWith<IllegalArgumentException> { engine.finish(tick(10), null, null, false) }
        engine.saveSet("a", actual("warm", true), tick(100))
        assertEquals(0, engine.workingSets("a"))
        assertTrue(engine.snapshot().hasUnfinishedTargets())
        engine.saveSet("a", actual("one"), tick(200))
        engine.saveSet("a", actual("two"), tick(300))
        assertEquals(2, engine.workingSets("a"))
        assertFalse(engine.snapshot().hasUnfinishedTargets())
        val ended = engine.finish(tick(500), null, null, false)
        assertFalse(ended.partial())
        assertEquals(3, ended.completedSession()!!.exercises().single().sets().size)
        val repeat = GymWorkoutAdapter.repeatExecution("repeat", 1, now, "Repeat", ended)
        assertNull(repeat.definition().slots().single().target()!!.referenceLoadKg())
        val restarted =
            GymExecutionEngine.start(
                "again",
                Origin.REPEATED,
                repeat.definition(),
                repeat.source(),
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        assertTrue(restarted.snapshot().actualSets().isEmpty())
        assertNull(restarted.snapshot().plannedAssociation())
    }

    @Test
    fun substitutionKeepsFrozenOriginalsAndIndependentActualExerciseBlocks() {
        val engine = start()
        engine.saveSet("a", actual("old"), tick(100))
        engine.replaceExercise("a", row, tick(200))
        assertEquals(
            80.0,
            engine.snapshot().prescription()!!.slots().single().target()!!.referenceLoadKg(),
        )
        assertNull(engine.snapshot().slots().single().target()!!.referenceLoadKg())
        assertEquals(0, engine.workingSets("a"))
        engine.saveSet("a", actual("new"), tick(300))
        engine.replaceExercise("a", ExerciseCatalog.definition("plank")!!.exercise(), tick(400))
        assertNull(engine.snapshot().slots().single().target())
        val ended = engine.finish(tick(500), null, null, false)
        assertTrue(ended.partial())
        assertEquals(
            listOf(bench, row),
            ended.completedSession()!!.exercises().map { it.exercise() },
        )
        assertEquals(listOf("old", "new"), ended.actualSets().map { it.actual().id() })
    }

    @Test
    fun supersetRoundsSkipsAndUngroupingUseTheSameActualSetEngine() {
        val slots =
            GymSupersets.group(
                listOf(
                    GymWorkoutDefinition.Slot("a", bench, target(), null),
                    GymWorkoutDefinition.Slot("b", row, target(), null),
                ),
                0,
                2,
                "pair",
            )
        val engine = start(GymWorkoutDefinition("group", 1, now, "Pair", slots))
        engine.saveSet("a", actual("a1"), tick(100))
        assertEquals("b", GymSupersets.next(engine.snapshot())!!.slotId())
        assertFalse(GymSupersets.next(engine.snapshot())!!.restBeforeNext())
        engine.selectExercise("b", tick(110))
        engine.saveSet("b", actual("b1"), tick(200))
        val next = GymSupersets.next(engine.snapshot())!!
        assertEquals("a", next.slotId())
        assertEquals(2, next.round())
        assertTrue(next.restBeforeNext())
        engine.skipExercise("a", tick(300))
        assertEquals("b", GymSupersets.next(engine.snapshot())!!.slotId())
        engine.breakSuperset("b", tick(400))
        assertNull(GymSupersets.next(engine.snapshot()))
        assertTrue(engine.snapshot().slots().all { it.supersetGroup() == null })
        assertEquals(2, engine.snapshot().actualSets().size)
        assertTrue(engine.snapshot().prescription()!!.slots().all { it.supersetGroup() == "pair" })
    }

    @Test
    fun replayRejectsForgedActualsTimingAndCompletionBeforeUse() {
        val engine = start()
        val saved = engine.saveSet("a", actual(), tick(100))
        assertFailsWith<IllegalArgumentException> {
            saved.copy(events = saved.events().dropLast(1))
        }
        assertFailsWith<IllegalArgumentException> { saved.copy(status = Status.FINISHED) }
        assertFailsWith<IllegalArgumentException> { saved.copy(activeMillis = 101) }
        assertFailsWith<IllegalArgumentException> {
            saved.copy(
                events =
                    saved.events().map {
                        if (it.type() == EventType.SET_SAVED) it.copy(slotId = "missing") else it
                    }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            saved.copy(actualSets = saved.actualSets() + saved.actualSets())
        }
        assertFailsWith<IllegalArgumentException> {
            saved.copy(events = saved.events() + saved.events().last().copy(sequence = 2))
        }
        assertEquals(saved, engine.snapshot())
    }

    @Test
    fun freeWorkoutsAndFailedLimitsRetainTheLastGoodSnapshot() {
        val free =
            GymExecutionEngine.start(
                "free",
                Origin.FREE,
                null,
                null,
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        assertNull(free.snapshot().selectedSlotId())
        assertFailsWith<IllegalArgumentException> { free.finish(tick(100), null, null, false) }
        val saved = free.observe(tick(1))
        assertFailsWith<IllegalArgumentException> { free.observe(tick(Long.MAX_VALUE)) }
        assertSame(saved, free.snapshot())
        free.addExercises(workout().slots(), tick(100))
        assertEquals("a", free.snapshot().selectedSlotId())
        assertEquals(Status.DISCARDED, free.discard(tick(200)).status())
        assertFailsWith<IllegalArgumentException> {
            GymExecutionEngine.recover(free.snapshot(), tick(300))
        }
    }

    @Test
    fun originalProgrammeIdsIncludeRevisionUtf16LengthAndOriginalUtf8Replacement() {
        val ids =
            listOf("programme", "a:b:c", "\uD83C\uDFC3", "old\uD800id", "old\uDC00id", "équipe")
        val expected =
            listOf(
                "060e61fd15a20ff8cd7042c2272dffc3ada6eb757cbe939b65c12b5e1bf719f5",
                "dda06f6c8bef9a6b14ec662198e8b58da7a006f6abfb1a3d0874441c6118d812",
                "ab6093bebcd5eecf3fc86d31b5a38bb00e868f40e685f388bf00aff6d04d559a",
                "1ea9a20d35e8eaea6a03b5b739e6e72146a33ce0b24037ad80afb0866d1ba82c",
                "1ea9a20d35e8eaea6a03b5b739e6e72146a33ce0b24037ad80afb0866d1ba82c",
                "b83bd9e8b5e80388e0abf26979fdb8c5ee1019f2922d73ce1677cebddfe6a0d8",
            )
        for (i in ids.indices) {
            val source = SourceReference(SourceKind.PROGRAMME, ids[i], 3, "session:1")
            assertEquals(
                "programme-session-${expected[i]}",
                GymWorkoutAdapter.programmeWorkoutId(source),
            )
            val p =
                GymWorkoutDefinition(
                    GymWorkoutAdapter.programmeWorkoutId(source),
                    3,
                    now,
                    "Planned",
                    workout().slots(),
                )
            val engine =
                GymExecutionEngine.start(
                    "planned",
                    Origin.PLANNED,
                    p,
                    source,
                    Offsets.ofTotalSeconds(0),
                    tick(0),
                )
            assertNull(engine.snapshot().plannedAssociation())
            engine.saveSet("a", actual(), tick(10))
            assertEquals(source, engine.finish(tick(20), null, null, true).plannedAssociation())
            assertFailsWith<IllegalArgumentException> {
                engine.snapshot().copy(prescription = p.copy(revision = 4))
            }
        }
    }

    @Test
    fun finishRetryPreservesBoxedSignedZeroEffortSemantics() {
        val engine = start()
        engine.saveSet("a", actual(), tick(10))
        val saved = engine.finish(tick(20), -0.0, null, true)
        assertSame(saved, engine.finish(null, -0.0, "", false))
        assertFailsWith<IllegalArgumentException> { engine.finish(null, 0.0, "", false) }
    }
}
