package org.racedaydreams.domain.recording

import kotlin.test.*

/**
 * The Android run notification is requested on every 1 s tick and every GPS fix. It must be posted
 * only when what it shows changes, without losing Pause/Resume updates.
 */
class NotificationUpdateGateTest {
    private data class Shown(val title: String, val paused: Boolean)

    /** Mirrors RunRecordingService.publish(): ask the gate, post, then record what was posted. */
    private class Recorder(private val canPost: () -> Boolean = { true }) {
        val gate = NotificationUpdateGate<Shown>()
        var posts = 0
        var paused = false

        fun foreground(title: String) {
            posts++
            gate.posted(Shown(title, paused))
        }

        fun publish() {
            val content = Shown(if (paused) "Run paused" else "Recording your run", paused)
            if (!gate.shouldPost(content) || !canPost()) return
            posts++
            gate.posted(content)
        }
    }

    /** One tick and one GPS fix per second, as during a normal run. */
    private fun Recorder.run(seconds: Int): Int {
        val before = posts
        repeat(seconds) {
            publish() // ticker
            publish() // location fix
        }
        return posts - before
    }

    @Test
    fun anUnchangedRecordingStateIsNotPostedAgain() {
        val recorder = Recorder()
        recorder.foreground("Preparing GPS recording")
        assertEquals(1, recorder.run(1), "the first running state replaces the preparing title")
        assertEquals(0, recorder.run(600), "ten unchanged minutes post nothing")
    }

    @Test
    fun pauseAndResumeAreAlwaysPosted() {
        val recorder = Recorder()
        recorder.run(60)
        recorder.paused = true
        assertEquals(1, recorder.run(120), "manual or auto pause shows Resume once")
        recorder.paused = false
        assertEquals(1, recorder.run(120), "resume shows Pause again once")
    }

    @Test
    fun aHeartRateForegroundUpgradeIsFollowedByTheRunningTitle() {
        val recorder = Recorder()
        recorder.run(5)
        recorder.foreground("Recording with heart-rate monitor")
        assertEquals(1, recorder.run(30))
    }

    @Test
    fun aDismissedNotificationIsShownAgain() {
        val recorder = Recorder()
        recorder.run(5)
        recorder.gate.invalidate()
        assertEquals(1, recorder.run(5))
    }

    @Test
    fun withoutNotificationPermissionNothingIsMarkedShownUntilAPostSucceeds() {
        var allowed = false
        val recorder = Recorder { allowed }
        assertEquals(0, recorder.run(30))
        allowed = true
        assertEquals(1, recorder.run(30), "posted as soon as permission is granted mid-run")
    }

    @Test
    fun simulatedRunsPostOnlyOnStateChanges() {
        // Before this change every request posted: 2 per second. Measured here for 5K, 10K,
        // half-marathon and marathon durations with one manual pause in the middle.
        for ((label, minutes) in
            listOf("5K" to 30, "10K" to 60, "half" to 120, "marathon" to 270)) {
            val recorder = Recorder()
            recorder.foreground("Preparing GPS recording")
            val seconds = minutes * 60
            recorder.run(seconds / 2)
            recorder.paused = true
            recorder.run(60)
            recorder.paused = false
            recorder.run(seconds / 2)
            val requests = (seconds + 60) * 2
            assertEquals(4, recorder.posts, "$label: foreground, running, paused, resumed")
            assertTrue(requests >= 3_720, "$label: $requests requests would each have posted")
        }
    }
}
