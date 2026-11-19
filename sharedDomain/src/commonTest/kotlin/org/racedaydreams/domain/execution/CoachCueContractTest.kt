package org.racedaydreams.domain.execution

import kotlin.test.*
import org.racedaydreams.domain.WorkoutSegment

class CoachCueContractTest {
    private val target =
        WorkoutExecutionEngine.Step(
            "Work",
            WorkoutSegment.Kind.RUN,
            5000.0,
            null,
            295.0,
            305.0,
            7.0,
        )

    private fun state(
        index: Int = 0,
        early: Boolean = false,
        step: WorkoutExecutionEngine.Step? = target,
    ) = WorkoutExecutionEngine.State(index, 2, step, null, null, null, false, early, false)

    private fun input(
        t: Long,
        pace: Double? = 270.0,
        reliable: Boolean = true,
        paused: Boolean = false,
    ) = WorkoutExecutionEngine.Input(t / 1000.0, t, t, pace, reliable, paused)

    @Test
    fun paceNeedsTransitionGraceAndSustainedEvidenceWithInclusiveBoundary() {
        val c = CoachCues()
        assertNull(c.pace(state(), input(0), 30000))
        assertNull(c.pace(state(), input(14999), 30000))
        assertNull(c.pace(state(), input(15000), 30000))
        assertNull(c.pace(state(), input(44999), 30000))
        val cue = assertNotNull(c.pace(state(), input(45000), 30000))
        assertEquals("pace-45000", cue.id())
        assertEquals("Slightly fast. Ease off.", cue.text())
        assertFalse(cue.haptic())
        assertNull(c.pace(state(), input(90000), 30000))
    }

    @Test
    fun missingUnreliablePausedEarlyAndUnprescribedPaceResetEvidence() {
        val unknowns =
            listOf(
                input(30000, pace = null),
                input(30000, reliable = false),
                input(30000, paused = true),
            )
        for (unknown in unknowns) {
            val c = CoachCues()
            c.pace(state(), input(0), 10000)
            c.pace(state(), input(20000), 10000)
            assertNull(c.pace(state(), unknown, 10000))
            assertNull(c.pace(state(), input(40000), 10000))
            assertNotNull(c.pace(state(), input(50000), 10000))
        }
        val c = CoachCues()
        assertNull(c.pace(state(early = true), input(0), 10000))
        assertNull(c.pace(state(step = null), input(30000), 10000))
        val openPace = target.copy(fastPace = null, slowPace = null)
        assertNull(c.pace(state(step = openPace), input(100000), 10000))
    }

    @Test
    fun hysteresisRequiresActualTargetReturnAndCooldownSurvivesReset() {
        val c = CoachCues()
        c.pace(state(), input(0), 10000)
        c.pace(state(), input(20000), 10000)
        assertNotNull(c.pace(state(), input(30000), 10000))
        assertNull(
            c.pace(state(), input(40000, 290.0), 10000)
        ) // Inside margin, still outside target.
        val returned = assertNotNull(c.pace(state(), input(50000, 300.0), 10000))
        assertEquals("Back in target range.", returned.text())
        assertNull(c.pace(state(), input(60000), 10000))
        assertNull(c.pace(state(), input(119999), 10000))
        assertNotNull(c.pace(state(), input(120000), 10000))
    }

    @Test
    fun stepChangeAndDirectionChangeRestartSustainedEvidence() {
        val c = CoachCues()
        c.pace(state(), input(0), 10000)
        c.pace(state(), input(15000), 10000)
        assertNull(c.pace(state(), input(20000, 330.0), 10000))
        assertNull(c.pace(state(), input(29999, 330.0), 10000))
        assertEquals(
            "A little below target pace. Run by effort if conditions require.",
            c.pace(state(), input(30000, 330.0), 10000)!!.text(),
        )
        assertNull(c.pace(state(index = 1), input(120000), 10000))
        assertNull(c.pace(state(index = 1), input(134999), 10000))
        assertNull(c.pace(state(index = 1), input(135000), 10000))
        assertNotNull(c.pace(state(index = 1), input(145000), 10000))
    }

    @Test
    fun arbitrationRetainsFreshnessPriorityRecencyIdAndOriginalFirstTie() {
        fun cue(id: String, at: Long, priority: Int = 4, text: String = id) =
            CoachCues.Cue(id, text, priority, at, false)
        val first = cue("same", 10000, 1, "First")
        val second = cue("same", 10000, 1, "Second")
        assertEquals(
            first,
            CoachCues.select(
                listOf(first, second, cue("future", 20001, 0), cue("stale", 9999, 0)),
                20000,
            ),
        )
        assertEquals(second, CoachCues.select(listOf(second, first), 20000))
        assertEquals(
            "new",
            CoachCues.select(listOf(cue("old", 10000), cue("new", 15000)), 20000)!!.id(),
        )
        assertEquals("a", CoachCues.select(listOf(cue("z", 15000), cue("a", 15000)), 20000)!!.id())
        assertNull(CoachCues.select(listOf(first), 20001))
        assertNull(CoachCues.select(listOf(first), -1))
    }

    @Test
    fun originalValidationAndSinkContractRemainExplicitWithoutAudioOwnership() {
        val c = CoachCues()
        for (bad in listOf(9999L, 120001L)) assertFailsWith<IllegalArgumentException> {
            c.pace(state(), input(0), bad)
        }
        assertFailsWith<IllegalArgumentException> { CoachCues.Cue(null, "Text", 1, 0, false) }
        assertFailsWith<IllegalArgumentException> { CoachCues.Cue("id", null, 1, 0, false) }
        assertFailsWith<IllegalArgumentException> { CoachCues.Cue("id", "Text", -1, 0, false) }
        val delivered = mutableListOf<CoachCues.Cue>()
        val sink = CoachCues.Sink { delivered.add(it) }
        val cue = CoachCues.Cue("id", "Text", 0, 0, true)
        sink.deliver(cue)
        assertEquals(listOf(cue), delivered)
    }
}
