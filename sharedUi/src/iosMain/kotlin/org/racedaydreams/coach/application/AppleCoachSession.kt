package org.racedaydreams.coach.application

import kotlinx.coroutines.*
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.documents.AppleBackupDocuments
import org.racedaydreams.coach.health.AppleHealthGateway
import org.racedaydreams.coach.reminders.AppleCoachReminders
import org.racedaydreams.coach.strength.*
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.health.body.BodyMetric
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.recording.AppleRunRecorder
import org.racedaydreams.recording.SqliteRecordingStageRepository
import org.racedaydreams.reminders.KotlinxReminderCalendar
import org.racedaydreams.storage.*
import platform.Foundation.NSThread
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

/** The app owns one database and one scope across navigation and scene changes. */
class AppleCoachSession<File>
private constructor(
    private val database: AppleSqliteDatabase,
    private val owner: CompletableJob,
    val services: SharedCoachApplicationServices<File>,
    val health: AppleHealthGateway,
    val activities: SqliteActivities,
    val recording: AppleRunRecorder,
    val gym: SharedGymNowCoordinator,
    val authoring: SharedStrengthProgrammeCoordinator,
    val generation: SharedStrengthGenerationCoordinator,
    val reminders: AppleCoachReminders,
    val platform: AppleCoachPlatform<File>,
    val backupDocuments: AppleBackupDocuments,
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
            documents: (LocalBackupExports) -> SettingsPlatform<File>,
            shortDate: (LocalDate) -> String,
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
                    KotlinxReminderCalendar.at(now(), TimeZone.currentSystemDefault().id).date
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
                val exports = LocalBackupExports(database, now)
                val backupDocuments = AppleBackupDocuments(exports, io)
                val platform =
                    AppleCoachPlatform(
                        scope,
                        health,
                        recording,
                        reminders,
                        readWindow,
                        documents(exports),
                        shortDate,
                    )
                val programming = SqliteStrengthProgramming(database, now, today)
                val coaching =
                    SqliteCoachingRepository(
                        database,
                        now,
                        newId,
                        PortableLegacyStrengthFingerprint,
                    )
                val gym = appleGymCoordinator(database, scope, programming, coaching, newId)
                val defaults = NSUserDefaults.standardUserDefaults
                val executions =
                    SqliteGymExecutions(database, now, PortableLegacyStrengthFingerprint)
                val authoring =
                    SharedStrengthProgrammeCoordinator(
                        database,
                        programming,
                        coaching,
                        executions::recentActual,
                        scope,
                        { defaults.stringForKey("strength.manual.draft") },
                        { defaults.setObject(it, "strength.manual.draft") },
                        now,
                        today,
                        newId,
                    )
                val generation =
                    SharedStrengthGenerationCoordinator(
                        database,
                        SqliteConcreteStrengthProgrammes(database, now, today),
                        programming,
                        coaching,
                        executions,
                        executions::recentActual,
                        scope,
                        { defaults.stringForKey("strength.concrete.draft") },
                        { defaults.setObject(it, "strength.concrete.draft") },
                        now,
                        today,
                        newId,
                    )
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
                            coaching,
                            programming,
                        ),
                        platform.bindings(),
                        now,
                        today,
                        newId,
                    )
                recording.restoreInterrupted()
                services.operations.refresh()
                return AppleCoachSession(
                    database,
                    owner,
                    services,
                    health,
                    activities,
                    recording,
                    gym,
                    authoring,
                    generation,
                    reminders,
                    platform,
                    backupDocuments,
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
