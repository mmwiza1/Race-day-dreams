package org.racedaydreams.coach.reminders

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.*
import org.racedaydreams.domain.reminders.QuietWindow
import org.racedaydreams.domain.reminders.ReminderPlanEntry
import org.racedaydreams.domain.reminders.ReminderRules
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.reminders.KotlinxReminderCalendar
import org.racedaydreams.storage.SqlDatabase
import org.racedaydreams.storage.SqliteActivities
import org.racedaydreams.storage.SqlitePlannedWorkouts
import org.racedaydreams.storage.SqliteSettings

/** Uses the existing revisioned mirror and the same policy as Android. */
class AppleCoachReminders(private val database: SqlDatabase, private val io: CoroutineDispatcher) {
    private val notifications = AppleEveningNotifications()
    private val planned = SqlitePlannedWorkouts(database)
    private val activities = SqliteActivities(database, AppleRecordingClock::now)
    private val reconciliation = Mutex()
    private val settings = SqliteSettings(database)

    fun preferences(): ReminderSettings {
        val zone = TimeZone.currentSystemDefault().id
        val defaults = ReminderSettings.defaults(zone)
        val stored = settings.getSetting(PREFERENCES) ?: return defaults
        return try {
            val json = Json.parseToJsonElement(stored).jsonObject
            if (json["version"]?.jsonPrimitive?.intOrNull != 1) return defaults
            val quietStart = json["quietStart"]?.jsonPrimitive?.contentOrNull
            val quietEnd = json["quietEnd"]?.jsonPrimitive?.contentOrNull
            require((quietStart == null) == (quietEnd == null))
            ReminderSettings(
                ReminderSettings.Mode.valueOf(json.getValue("mode").jsonPrimitive.content),
                LocalTime.parse(json.getValue("eveningTime").jsonPrimitive.content)
                    .toNanosecondOfDay(),
                quietStart?.let {
                    QuietWindow(
                        LocalTime.parse(it).toNanosecondOfDay(),
                        LocalTime.parse(checkNotNull(quietEnd)).toNanosecondOfDay(),
                    )
                },
                zone,
            )
        } catch (_: IllegalArgumentException) {
            defaults
        } catch (_: NoSuchElementException) {
            defaults
        }
    }

    fun savePreferences(preferences: ReminderSettings) {
        val payload = buildJsonObject {
            put("version", 1)
            put("mode", preferences.mode.name)
            put(
                "eveningTime",
                LocalTime.fromNanosecondOfDay(preferences.eveningNanoOfDay).toString(),
            )
            put(
                "quietStart",
                preferences.quietHours?.let { LocalTime.fromNanosecondOfDay(it.start).toString() },
            )
            put(
                "quietEnd",
                preferences.quietHours?.let { LocalTime.fromNanosecondOfDay(it.end).toString() },
            )
        }
        settings.putSetting(PREFERENCES, payload.toString())
    }

    suspend fun requestPermission(): Boolean = notifications.requestPermission()

    /** Read current preferences inside the serialized refresh so an old request cannot win. */
    suspend fun reconcile() {
        reconciliation.withLock {
            val permitted = notifications.permitted()
            val reminders =
                withContext(io) {
                    database.transaction {
                        val selected = preferences()
                        if (!permitted || selected.mode == ReminderSettings.Mode.DISABLED) {
                            return@transaction emptyList()
                        }
                        val completed =
                            activities
                                .activitySummaries()
                                .mapNotNull { it.plannedWorkoutId() }
                                .toHashSet()
                        val entries =
                            planned.plannedWorkouts().mapNotNull { workout ->
                                val revision = planned.plannedWorkoutRevision(workout.id())
                                if (revision < 1) return@mapNotNull null
                                val status =
                                    if (workout.id() in completed)
                                        ReminderPlanEntry.Status.COMPLETED
                                    else ReminderPlanEntry.Status.PLANNED
                                ReminderPlanEntry(workout, revision, status)
                            }
                        ReminderRules.schedule(
                            entries,
                            selected,
                            AppleRecordingClock::now,
                            true,
                            KotlinxReminderCalendar,
                        )
                    }
                }
            notifications.replace(reminders)
        }
    }

    private companion object {
        const val PREFERENCES = "reminders.preferences.v1"
    }
}
