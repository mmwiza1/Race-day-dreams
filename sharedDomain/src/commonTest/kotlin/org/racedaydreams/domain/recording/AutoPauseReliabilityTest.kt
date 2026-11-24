package org.racedaydreams.domain.recording

import kotlin.test.*
import org.racedaydreams.domain.time.*

/**
 * Audit A-B2 (auto-resume under the accuracy the recorder accepts) and A-B3 (pause and resume dated
 * when they happened; no distance lost while the resume is confirmed).
 */
class AutoPauseReliabilityTest {
    private fun wall(t: Long) = Instants.ofEpochSecond(t / 1000, t % 1000 * 1000000)

    private fun fix(t: Long, metres: Double, accuracy: Double?, speed: Double?) =
        RunMetricsEngine.Fix(
            wall(t),
            t,
            0.0,
            metres / 111194.92664455874,
            accuracy,
            null,
            null,
            speed,
        )

    private val urban = listOf(12.0, 20.0, 35.0, 9.0, 28.0)

    /** Pauses the policy at 30 s of accurate stillness at the origin. */
    private fun pausedPolicy(): AutoPausePolicy {
        val p = AutoPausePolicy()
        for (i in 0..29) p.observe(fix(i * 1000L, 0.0, 3.0, 0.0))
        assertEquals(AutoPausePolicy.Decision.PAUSE, p.observe(fix(30_000, 0.0, 3.0, 0.0)))
        assertEquals(0L, p.transitionMillis())
        return p
    }

    @Test
    fun runningAwayUnderEightToThirtyFiveMetreAccuracyResumes() {
        val p = pausedPolicy()
        var resumedAt = -1L
        for (k in 1..60) {
            val t = 30_000 + k * 1000L
            val d = p.observe(fix(t, k * 3.0, urban[k % urban.size], 3.0))
            if (d == AutoPausePolicy.Decision.RESUME) {
                resumedAt = t
                break
            }
        }
        assertTrue(resumedAt in 35_000..45_000, "resumed at $resumedAt")
        // Dated from the first moving fix, not from the confirmation.
        assertEquals(31_000L, p.transitionMillis())
    }

    @Test
    fun runningWithoutReportedSpeedResumesOnDisplacement() {
        val p = pausedPolicy()
        val decisions = (1..20).map { k -> p.observe(fix(30_000 + k * 1000L, k * 3.0, 20.0, null)) }
        assertTrue(AutoPausePolicy.Decision.RESUME in decisions, decisions.toString())
    }

    @Test
    fun poorAccuracyJitterWhileStandingStillNeverResumes() {
        val p = pausedPolicy()
        for (k in 1..120) {
            val jitter = if (k % 2 == 0) 15.0 else -15.0
            // Even a spurious Doppler speed cannot resume without leaving the pause position.
            val speed = if (k % 3 == 0) 1.5 else null
            assertEquals(
                AutoPausePolicy.Decision.NONE,
                p.observe(fix(30_000 + k * 1000L, jitter, urban[k % urban.size], speed)),
                "fix $k",
            )
        }
    }

    @Test
    fun fixesTheRecorderRejectsDoNotResetAResumeInProgress() {
        val p = pausedPolicy()
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(31_000, 3.0, 5.0, 3.0)))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(32_000, 6.0, 60.0, 3.0)))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(33_000, 9.0, null, 3.0)))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(34_000, 12.0, 5.0, 3.0)))
        assertEquals(AutoPausePolicy.Decision.NONE, p.observe(fix(35_000, 15.0, 5.0, 3.0)))
        assertEquals(AutoPausePolicy.Decision.RESUME, p.observe(fix(36_000, 18.0, 5.0, 3.0)))
        assertEquals(31_000L, p.transitionMillis())
    }

    /** Drives the engine and policy exactly as the Android recording service does. */
    private class Recorder {
        val engine = RunMetricsEngine(0, Instants.ofEpochSecond(0))
        val policy = AutoPausePolicy()
        val moving = mutableListOf<Long>()

        fun deliver(f: RunMetricsEngine.Fix) {
            val t = f.monotonicMillis()
            if (engine.qualifyDelivery(f, t, f.timestamp())) {
                when (policy.observe(f)) {
                    AutoPausePolicy.Decision.PAUSE ->
                        engine.pause(t, f.timestamp(), true, policy.transitionMillis())
                    AutoPausePolicy.Decision.RESUME ->
                        engine.resume(t, f.timestamp(), true, policy.transitionMillis())
                    else -> Unit
                }
                engine.accept(f)
            }
            moving.add(engine.status(t).movingMillis())
        }
    }

    @Test
    fun aStopAtTheLightsKeepsDistanceAndCountsOnlyRealStillTimeAsPaused() {
        val r = Recorder()
        // 0–20 s: running 3 m/s with good accuracy (60 m).
        for (s in 0..20) r.deliver(fix(s * 1000L, s * 3.0, 3.0, 3.0))
        // 21–55 s: standing at 60 m; the pause is detected at 51 s for a stop that began at 21 s.
        for (s in 21..55) r.deliver(fix(s * 1000L, 60.0, 3.0, 0.0))
        assertTrue(r.engine.status(55_000).paused())
        // 56–100 s: running again at 3 m/s under urban 9–35 m accuracy.
        for (s in 56..100) r.deliver(fix(s * 1000L, 60.0 + (s - 55) * 3.0, urban[s % 5], 3.0))
        val status = r.engine.status(100_000)
        assertFalse(status.paused())
        // Every metre from the first moving fix is kept: 60 m + 44 × 3 m.
        assertEquals(192.0, status.metres(), 0.01)
        // Paused from 21 s (still) to 56 s (moving): 35 s of the 100 s.
        assertEquals(65_000L, r.engine.recordedMovingMillis(100_000))
        assertEquals(
            r.engine.recordedMovingMillis(100_000),
            RecordingTimeline.activeMillis(0, 100_000, r.engine.events()),
        )
        // Live moving time never moves backwards (structured workouts require it).
        assertEquals(r.moving, r.moving.sorted())
        val types = r.engine.events().map { it.type() to it.monotonicMillis() }
        assertTrue("AUTO_PAUSE" to 21_000L in types, types.toString())
        assertTrue("AUTO_RESUME" to 56_000L in types, types.toString())
    }

    @Test
    fun manualPauseAndResumeAreUnchanged() {
        val e = RunMetricsEngine(0, Instants.ofEpochSecond(0))
        for (s in 0..10) e.accept(fix(s * 1000L, s * 3.0, 3.0, 3.0))
        e.pause(11_000, wall(11_000))
        for (s in 12..20) e.accept(fix(s * 1000L, 30.0 + (s - 11) * 3.0, 3.0, 3.0))
        e.resume(21_000, wall(21_000))
        // A manual resume replays nothing recorded while paused.
        assertEquals(30.0, e.status(21_000).metres(), 0.01)
        assertEquals(11_000L, e.recordedMovingMillis(21_000))
        assertEquals(11_000L, e.status(21_000).movingMillis())
    }
}
