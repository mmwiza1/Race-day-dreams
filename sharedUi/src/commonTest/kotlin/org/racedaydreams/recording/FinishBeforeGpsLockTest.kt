package org.racedaydreams.recording

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.Instants

/**
 * Audit A-B1: Finish before a GPS lock must not stop recording into an unfinished run that recovery
 * cannot save. The decision is made before any state changes and never touches storage.
 */
class FinishBeforeGpsLockTest {
    private var storageCalls = 0

    private val repository =
        object : RecordingStageRepository {
            override fun append(batch: RecordingStageBatch) {
                storageCalls++
            }

            override fun unfinished(): RecordingStageSession? {
                storageCalls++
                return null
            }

            override fun finish(
                session: RecordingStageSession,
                movingMillis: Long?,
                recovered: Boolean,
            ): String {
                storageCalls++
                return "saved"
            }
        }

    private fun coordinator() =
        SharedRecordingSessionCoordinator(
            RecordingStageSession("s", "ACTIVE", Instants.ofEpochSecond(0), 0, 0),
            repository,
            Dispatchers.Unconfined,
            { 0L },
            { null },
        )

    private fun fix(t: Long, metres: Double) =
        RunMetricsEngine.Fix(
            Instants.ofEpochSecond(t / 1000),
            t,
            0.0,
            metres / 111194.92664455874,
            4.0,
            null,
            null,
            3.0,
        )

    @Test
    fun finishWithoutAGpsLockKeepsRecordingAndExplains() {
        val c = coordinator()
        assertFalse(c.canFinish())
        assertEquals(
            SharedRecordingSessionCoordinator.FinishRequest.NEEDS_GPS,
            c.requestFinish(1_000),
        )
        // A later tap outside the confirmation window explains again rather than discarding.
        assertEquals(
            SharedRecordingSessionCoordinator.FinishRequest.NEEDS_GPS,
            c.requestFinish(1_000 + SharedRecordingSessionCoordinator.DISCARD_CONFIRM_MILLIS + 1),
        )
        // One point is still not a saveable run.
        assertTrue(c.engine.accept(fix(30_000, 0.0)))
        assertEquals(
            SharedRecordingSessionCoordinator.FinishRequest.NEEDS_GPS,
            c.requestFinish(31_000),
        )
        assertTrue(
            SharedRecordingSessionCoordinator.NEEDS_GPS_MESSAGE.contains("Recording continues")
        )
        assertEquals(0, storageCalls)
    }

    @Test
    fun aConfirmingSecondFinishDiscardsOnlyAnEmptyRun() {
        val c = coordinator()
        assertEquals(
            SharedRecordingSessionCoordinator.FinishRequest.NEEDS_GPS,
            c.requestFinish(5_000),
        )
        assertEquals(
            SharedRecordingSessionCoordinator.FinishRequest.DISCARD_EMPTY,
            c.requestFinish(9_000),
        )
        assertEquals(0, storageCalls)
    }

    @Test
    fun onceTwoPointsExistFinishSavesEvenAfterAnEarlierWarning() {
        val c = coordinator()
        assertEquals(
            SharedRecordingSessionCoordinator.FinishRequest.NEEDS_GPS,
            c.requestFinish(1_000),
        )
        assertTrue(c.engine.accept(fix(2_000, 0.0)))
        assertTrue(c.engine.accept(fix(3_000, 3.0)))
        assertTrue(c.canFinish())
        // Within the window, but a real run is never discarded by a second tap.
        assertEquals(SharedRecordingSessionCoordinator.FinishRequest.SAVE, c.requestFinish(3_500))
        assertEquals(SharedRecordingSessionCoordinator.FinishRequest.SAVE, c.requestFinish(3_600))
    }
}
