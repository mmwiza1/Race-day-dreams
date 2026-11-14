package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.GymRestTimer
import org.racedaydreams.domain.strength.StrengthMeasurementPolicy

class GymRestTimerTest {
    private fun clock(time: Long, epoch: String = "boot-one") = GymRestTimer.Clock(epoch, time)

    @Test
    fun observationsRemainImmutableAndFollowElapsedClockAcrossNavigation() {
        val original = GymRestTimer.start("workout", "set-one", 180, clock(1000))
        assertEquals(180L, original.secondsRemaining())
        assertEquals(179L, original.observe(clock(2000)).secondsRemaining())
        val paused = original.pause(clock(31000))
        assertEquals(150L, paused.observe(clock(9_000_000)).secondsRemaining())
        assertEquals(
            149L,
            paused.resume(clock(9_000_000)).observe(clock(9_001_000)).secondsRemaining(),
        )
        assertEquals(GymRestTimer.Phase.COMPLETE, original.observe(clock(999999)).phase())
        assertEquals(180L, original.secondsRemaining())
    }

    @Test
    fun unknownBootAndBackwardsClockRequireExplicitResume() {
        val original = GymRestTimer.start("w", "s", 90, clock(1000))
        for (now in listOf(clock(2000, "boot-two"), clock(500))) {
            val interrupted = original.observe(now)
            assertEquals(GymRestTimer.Phase.INTERRUPTED, interrupted.phase())
            assertEquals(90L, interrupted.observe(clock(900000)).secondsRemaining())
            assertEquals(
                89L,
                interrupted
                    .resume(now)
                    .observe(clock(now.millis() + 1000, now.epoch()))
                    .secondsRemaining(),
            )
        }
    }

    @Test
    fun persistedFieldsRestoreSameStateWithoutInventingElapsedTime() {
        val source = GymRestTimer.start("w", "s", 90, clock(1000)).pause(clock(5000))
        val restored =
            GymRestTimer(
                source.executionId(),
                source.triggerId(),
                source.phase(),
                source.epoch(),
                source.observedMillis(),
                source.remainingMillis(),
                source.durationMillis(),
            )
        assertEquals(source, restored)
        assertEquals(source.hashCode(), restored.hashCode())
        assertEquals(86L, restored.observe(clock(500000)).secondsRemaining())
        assertEquals(90L, restored.skip(clock(500001)).reset(clock(500002)).secondsRemaining())
    }

    @Test
    fun adjustingChangesResetPeriodAndPreservesPausedState() {
        val timer = GymRestTimer.start("w", "s", 180, clock(1000))
        assertEquals(209L, timer.adjust(30, clock(2000)).secondsRemaining())
        assertEquals(210L, timer.adjust(30, clock(2000)).reset(clock(3000)).secondsRemaining())
        assertEquals(150L, timer.adjust(-30, clock(61000)).reset(clock(62000)).secondsRemaining())
        assertEquals(
            GymRestTimer.Phase.PAUSED,
            timer.pause(clock(2000)).adjust(30, clock(4000)).phase(),
        )
        assertEquals(0L, timer.adjust(-180, clock(2000)).secondsRemaining())
    }

    @Test
    fun invalidAndOverflowStatesNeverCreateAnUnboundedTimer() {
        assertFailsWith<IllegalArgumentException> { GymRestTimer.start("w", "s", 1801, clock(0)) }
        assertFailsWith<IllegalArgumentException> {
            GymRestTimer.start("w", "s", 180, clock(Long.MAX_VALUE - 100))
        }
        assertFailsWith<IllegalArgumentException> { clock(-1) }
        assertFailsWith<IllegalArgumentException> { clock(0, " ") }
        assertFailsWith<IllegalArgumentException> {
            GymRestTimer.Preferences(-1, true, false, false, false)
        }
        assertFailsWith<IllegalArgumentException> {
            GymRestTimer("w", "s", GymRestTimer.Phase.COMPLETE, "boot", 1, 1, 10)
        }
        assertEquals(0L, GymRestTimer.start("w", "s", 0, clock(0)).secondsRemaining())
        assertEquals(90, GymRestTimer.suggestedSeconds(null, GymRestTimer.Preferences.defaults()))
        assertEquals(0, GymRestTimer.suggestedSeconds(0, GymRestTimer.Preferences.defaults()))
        assertEquals(180, GymRestTimer.suggestedSeconds(180, GymRestTimer.Preferences.defaults()))
    }

    @Test
    fun durationInputsKeepOriginalLimitsAndFormatting() {
        assertEquals(90L, StrengthMeasurementPolicy.seconds(" 1:30 "))
        assertEquals(43200L, StrengthMeasurementPolicy.seconds("720:00"))
        assertEquals(90L, StrengthMeasurementPolicy.seconds("٠١:٣٠"))
        assertEquals(90L, StrengthMeasurementPolicy.seconds("９０"))
        assertEquals("1 min 30 sec", StrengthMeasurementPolicy.duration(90))
        assertEquals("30 sec", StrengthMeasurementPolicy.duration(30))
        for (text in
            listOf(
                "",
                "0",
                "-1",
                "1:60",
                "1:",
                "1:2:3",
                "720:01",
                "9223372036854775807:01",
                "\u00a090\u00a0",
            )) {
            assertFailsWith<IllegalArgumentException>(text) {
                StrengthMeasurementPolicy.seconds(text)
            }
        }
    }
}
