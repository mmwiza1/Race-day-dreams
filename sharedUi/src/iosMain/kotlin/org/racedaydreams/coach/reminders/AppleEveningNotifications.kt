@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.coach.reminders

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.reminders.EveningReminder
import org.racedaydreams.recording.AppleRecordingClock
import platform.UserNotifications.*

/** Schedules only the app's own evening reminders. Gym and recording alerts are left alone. */
class AppleEveningNotifications {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val replacements = Mutex()

    /** Call from a user action. Reconciliation only checks the current system settings. */
    suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, error ->
            if (error == null) continuation.resume(granted)
            else continuation.resumeWithException(IllegalStateException(error.localizedDescription))
        }
    }

    suspend fun permitted(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(
                status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional
            )
        }
    }

    suspend fun replace(reminders: List<EveningReminder>) {
        require(reminders.map { it.workoutId() }.distinct().size == reminders.size)
        replacements.withLock {
            // Complete this replacement before a later caller inspects or changes pending requests.
            withContext(NonCancellable) {
                val pending = pendingIdentifiers()
                val owned = pending.filter { it.startsWith(PREFIX) }
                if (owned.isNotEmpty())
                    center.removePendingNotificationRequestsWithIdentifiers(owned)
                if (!permitted()) return@withContext
                val room = (MAX_PENDING - (pending.size - owned.size)).coerceAtLeast(0)
                val added = mutableListOf<String>()
                try {
                    val ordered =
                        reminders.sortedWith(
                            compareBy<EveningReminder> { it.triggerAt() }.thenBy { it.workoutId() }
                        )
                    for (reminder in ordered) {
                        if (added.size >= room) break
                        val now = AppleRecordingClock.now()
                        val at = reminder.triggerAt()
                        val seconds =
                            (at.getEpochSecond() - now.getEpochSecond()).toDouble() +
                                (at.getNano() - now.getNano()) / 1_000_000_000.0
                        if (seconds < 1) continue
                        val identifier = PREFIX + reminder.workoutId()
                        val content =
                            UNMutableNotificationContent().apply {
                                setTitle(reminder.title())
                                setBody(reminder.message())
                                setSound(UNNotificationSound.defaultSound())
                            }
                        val trigger =
                            UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                                seconds,
                                false,
                            )
                        val request =
                            UNNotificationRequest.requestWithIdentifier(
                                identifier,
                                content,
                                trigger,
                            )
                        add(request)
                        added += identifier
                    }
                } catch (error: Exception) {
                    if (added.isNotEmpty())
                        center.removePendingNotificationRequestsWithIdentifiers(added)
                    throw error
                }
            }
        }
    }

    private suspend fun pendingIdentifiers(): List<String> =
        suspendCancellableCoroutine { continuation ->
            center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                continuation.resume(
                    requests.orEmpty().filterIsInstance<UNNotificationRequest>().map {
                        it.identifier
                    }
                )
            }
        }

    private suspend fun add(request: UNNotificationRequest): Unit =
        suspendCancellableCoroutine { continuation ->
            center.addNotificationRequest(request) { error ->
                if (error == null) continuation.resume(Unit)
                else
                    continuation.resumeWithException(
                        IllegalStateException(error.localizedDescription)
                    )
            }
        }

    private companion object {
        const val PREFIX = "org.racedaydreams.evening."
        const val MAX_PENDING = 64
    }
}
