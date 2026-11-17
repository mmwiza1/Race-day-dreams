package org.racedaydreams.coach.application

import kotlinx.coroutines.*
import org.racedaydreams.coach.health.AppleHealthGateway
import org.racedaydreams.coach.reminders.AppleCoachReminders
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.Offsets
import org.racedaydreams.health.body.BodyMetric
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.recording.AppleRunRecorder
import org.racedaydreams.recording.SqliteRecordingStageRepository
import org.racedaydreams.storage.*
import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.NSTimeZone
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

/** The app owns one database and one scope across navigation and scene changes. */
class AppleCoachSession<File>
private constructor(
    private val database: AppleSqliteDatabase,
    private val owner: CompletableJob,
    val services: SharedCoachApplicationServices<File>,
    val health: AppleHealthGateway,
    val recording: AppleRunRecorder,
    val reminders: AppleCoachReminders,
    val interruptedRecording: SqliteRecordingStaging.Recovery?,
) {
    private var closed = false

    suspend fun close() {
        check(NSThread.isMainThread)
        if (closed) return
        closed = true
        withContext(NonCancellable) {
            owner.cancelAndJoin()
            withContext(applicationIoDispatcher()) { database.close() }
        }
    }

    companion object {
        suspend fun <File> open(
            path: String,
            bundleIdentifier: String,
            readWindow: () -> BodyReadWindow,
            selectedWorkouts: () -> Boolean,
            selectedBodyMetrics: () -> Set<BodyMetric>,
            savedDraft: () -> String?,
            bindPlatform:
                (
                    AppleSqliteDatabase, AppleHealthGateway, AppleRunRecorder, AppleCoachReminders,
                ) -> CoachPlatformBindings<File>,
        ): AppleCoachSession<File> {
            check(NSThread.isMainThread)
            val io = applicationIoDispatcher()
            var opened: AppleSqliteDatabase? = null
            val owner = SupervisorJob()
            try {
                withContext(io) { opened = AppleApplicationDatabase.open(path) }
                val database = checkNotNull(opened)
                val scope = CoroutineScope(owner + Dispatchers.Main.immediate)
                val now = AppleRecordingClock::now
                val newId = { NSUUID().UUIDString }
                val today = {
                    val wall = NSDate()
                    val offset = NSTimeZone.systemTimeZone.secondsFromGMTForDate(wall).toInt()
                    CalendarDates.atOffset(
                        AppleRecordingClock.instant(wall.timeIntervalSince1970),
                        Offsets.ofTotalSeconds(offset),
                    )
                }
                val activities = SqliteActivities(database, now)
                val health =
                    AppleHealthGateway(
                        database,
                        activities,
                        bundleIdentifier,
                        readWindow,
                        selectedWorkouts,
                        selectedBodyMetrics,
                        now,
                        newId,
                    )
                val recording =
                    AppleRunRecorder(
                        SqliteRecordingStageRepository(database, now, newId),
                        scope,
                        io,
                        newId,
                    )
                val reminders = AppleCoachReminders(database, io)
                val platform = bindPlatform(database, health, recording, reminders)
                check(platform.healthGateway === health) {
                    "The application must use its owned HealthKit gateway."
                }
                val services =
                    SharedCoachApplicationServices(
                        database,
                        scope,
                        savedDraft,
                        CoachRepositories(
                            activities,
                            SqliteResults(database),
                            SqliteShoes(database),
                            SqliteBaselines(database, now),
                            SqliteSettings(database),
                            SqlitePlannedWorkouts(database),
                            SqliteCoachingRepository(
                                database,
                                now,
                                newId,
                                PortableLegacyStrengthFingerprint,
                            ),
                            SqliteStrengthProgramming(database, now, today),
                        ),
                        platform,
                        now,
                        today,
                        newId,
                    )
                val interrupted = recording.restoreInterrupted()
                services.operations.refresh()
                return AppleCoachSession(
                    database,
                    owner,
                    services,
                    health,
                    recording,
                    reminders,
                    interrupted,
                )
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    owner.cancelAndJoin()
                    try {
                        withContext(io) { opened?.close() }
                    } catch (closeFailure: Throwable) {
                        failure.addSuppressed(closeFailure)
                    }
                }
                throw failure
            }
        }
    }
}
