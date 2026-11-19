@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.coach.application

import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.racedaydreams.coach.strength.GymNowRetainedState
import org.racedaydreams.coach.strength.SharedGymNowCoordinator
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.Offsets
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.storage.*
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.*

/** Gym actuals stay in SQLite. Only unsaved input is in the app's private defaults. */
internal fun appleGymCoordinator(
    database: SqlDatabase,
    scope: CoroutineScope,
    programming: StrengthProgrammingRepository,
    coaching: CoachingRepository,
    nextId: () -> String,
): SharedGymNowCoordinator {
    val now = AppleRecordingClock::now
    val executions = SqliteGymExecutions(database, now, PortableLegacyStrengthFingerprint)
    val legacy = SqliteLegacyGymProgrammes(database, now, nextId)
    val defaults = NSUserDefaults.standardUserDefaults
    val epoch = NSUUID().UUIDString
    val alerts = AppleGymRestAlerts()
    return SharedGymNowCoordinator(
        database,
        executions,
        SqliteGymAssociations(database, now),
        programming,
        coaching,
        legacy::current,
        legacy::version,
        executions::favourites,
        executions::recentActual,
        executions::finishedActualIds,
        object : GymNowRetainedState {
            private fun key(value: String) = "gym.input.$value"

            override fun get(key: String): Any? =
                when (val stored = defaults.objectForKey(key(key))) {
                    is platform.Foundation.NSNumber -> stored.intValue
                    else -> stored
                }

            override fun set(key: String, value: Any?) {
                defaults.setObject(value, key(key))
            }

            override fun remove(key: String) {
                defaults.removeObjectForKey(key(key))
            }

            override fun strings(key: String): List<String>? =
                defaults.arrayForKey(key(key))?.filterIsInstance<String>()
        },
        scope,
        GymWorkoutSessionCoordinator.TickSource {
            GymExecution.Tick(epoch, AppleRecordingClock.monotonicMillis(), now())
        },
        { GymRestTimer.Clock(epoch, AppleRecordingClock.monotonicMillis()) },
        alerts::sync,
        now,
        { instant ->
            val at =
                kotlinx.datetime.Instant.fromEpochSeconds(
                    instant.getEpochSecond(),
                    instant.getNano(),
                )
            Offsets.ofTotalSeconds(TimeZone.currentSystemDefault().offsetAt(at).totalSeconds)
        },
        nextId,
    )
}

private class AppleGymRestAlerts {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val identifier = "org.racedaydreams.gym.rest"

    fun sync(timer: GymRestTimer?, preferences: GymRestTimer.Preferences) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
        if (
            !preferences.notifications() ||
                timer == null ||
                timer.phase() != GymRestTimer.Phase.RUNNING
        )
            return
        if (timer.remainingMillis() <= 0) return
        val content =
            UNMutableNotificationContent().apply {
                setTitle("Rest complete")
                setBody("Start your next set when you are ready.")
                if (preferences.audio()) setSound(UNNotificationSound.defaultSound())
            }
        val trigger =
            UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                (timer.remainingMillis() / 1000.0).coerceAtLeast(1.0),
                false,
            )
        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(identifier, content, trigger),
            null,
        )
    }
}
