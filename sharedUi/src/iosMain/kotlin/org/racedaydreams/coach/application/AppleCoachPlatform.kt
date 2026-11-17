package org.racedaydreams.coach.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.racedaydreams.coach.health.AppleHealthGateway
import org.racedaydreams.coach.reminders.AppleCoachReminders
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.time.*
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.recording.AppleRunRecorder
import org.racedaydreams.reminders.KotlinxReminderCalendar
import platform.Foundation.NSUUID

/** Native snapshot and reminder effects shared by the iOS application's screens. */
class AppleCoachPlatform<File>(
    scope: CoroutineScope,
    private val health: AppleHealthGateway,
    private val recording: AppleRunRecorder,
    private val reminders: AppleCoachReminders,
    private val readWindow: () -> BodyReadWindow,
    private val documents: SettingsPlatform<File>,
    private val shortDate: (LocalDate) -> String,
) {
    private val pendingReminders = Channel<Unit>(Channel.CONFLATED)
    private val reminderError = MutableStateFlow<String?>(null)
    val reminderFailure = reminderError.asStateFlow()

    init {
        scope.launch {
            for (request in pendingReminders) {
                try {
                    reminders.reconcile()
                    reminderError.value = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    reminderError.value = "Could not update workout reminders."
                }
            }
        }
    }

    fun reconcileReminders() {
        pendingReminders.trySend(Unit)
    }

    fun bindings(): CoachPlatformBindings<File> =
        CoachPlatformBindings(
            object : CoachSnapshotPlatform {
                override fun importWindow() = readWindow().let { it.from to it.until }

                override fun reconcileReminders() = this@AppleCoachPlatform.reconcileReminders()

                override fun reminderPreferences() = reminders.preferences()

                override fun healthAvailability() = health.availability()

                override suspend fun grantedHealthPermissions() = health.grantedPermissions()
            },
            health,
            emptySet(),
            { health.syncBodyMeasurements().message },
            object : SettingsPlatform<File> by documents {
                override fun saveReminderPreferences(preferences: ReminderSettings) {
                    reminders.savePreferences(preferences)
                    reconcileReminders()
                }
            },
            SharedGymProgrammeOperations(
                clockForAction = {
                    val at = AppleRecordingClock.now()
                    val local = KotlinxReminderCalendar.at(at, TimeZone.currentSystemDefault().id)
                    object : GymProgrammeActionClock {
                        override fun instant() = at

                        override fun today() = local.date
                    }
                },
                newId = { NSUUID().UUIDString },
            ),
            { date ->
                val zone = TimeZone.currentSystemDefault()
                val noon =
                    KotlinxReminderCalendar.resolve(date, 43_200_000_000_000L, zone.id).instant
                noon to offset(noon, zone)
            },
            { date, time, duration ->
                val zone = TimeZone.currentSystemDefault()
                val start =
                    KotlinxReminderCalendar.resolve(
                            date,
                            LocalTime.parse(time).toNanosecondOfDay(),
                            zone.id,
                        )
                        .instant
                val end =
                    Instants.ofEpochSecond(
                        start.getEpochSecond() + duration.getSeconds(),
                        start.getNano().toLong() + duration.getNano(),
                    )
                RunningEvidenceCoordinator.Timing(
                    start,
                    end,
                    offset(start, zone),
                    offset(end, zone),
                )
            },
            shortDate,
            { recording.status.value != null },
        )

    private fun offset(at: Instant, zone: TimeZone): ZoneOffset {
        val instant = kotlinx.datetime.Instant.fromEpochSeconds(at.getEpochSecond(), at.getNano())
        return Offsets.ofTotalSeconds(zone.offsetAt(instant).totalSeconds)
    }
}
