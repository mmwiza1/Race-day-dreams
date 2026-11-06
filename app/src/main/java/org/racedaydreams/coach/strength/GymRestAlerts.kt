package org.racedaydreams.coach.strength

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.AtomicFile
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.racedaydreams.coach.CoachActivity
import org.racedaydreams.data.SqliteGymToolsRepository
import org.racedaydreams.domain.strength.GymRestTimer
import org.racedaydreams.recording.RunRecordingService

/**
 * Private identity file plus boot epoch. Backed-up opaque timer anchors cannot adopt another
 * installation identity.
 */
internal class GymRestClock(private val context: Context) {
    private val install by lazy {
        synchronized(GymRestClock::class.java) {
            val file = AtomicFile(File(context.noBackupFilesDir, "gym-rest-clock"))
            if (file.baseFile.exists()) file.openRead().bufferedReader().use { it.readText() }
            else {
                val id = UUID.randomUUID().toString()
                val output = file.startWrite()
                try {
                    output.write(id.toByteArray())
                    file.finishWrite(output)
                } catch (error: Exception) {
                    file.failWrite(output)
                    throw error
                }
                id
            }
        }
    }

    // First access is on the repository dispatcher; reading settings once avoids per-tick I/O.
    private val epoch by lazy {
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        "$install:${if (boot >= 0) boot.toString() else PROCESS}"
    }

    fun now(): GymRestTimer.Clock = GymRestTimer.Clock(epoch, SystemClock.elapsedRealtime())

    companion object {
        private val PROCESS = UUID.randomUUID().toString()
    }
}

/**
 * One inexact alarm, no background service. Android may delay alerts; stored deadlines stay honest.
 */
internal class GymRestAlerts(private val context: Context) {
    private fun intent(timer: GymRestTimer): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GymRestAlarmReceiver::class.java)
                .setData(
                    Uri.Builder()
                        .scheme("rdd-rest")
                        .authority("timer")
                        .appendPath(timer.executionId())
                        .appendPath(timer.triggerId())
                        .build()
                )
                .putExtra("execution", timer.executionId())
                .putExtra("trigger", timer.triggerId()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun sync(timer: GymRestTimer?, preferences: GymRestTimer.Preferences) {
        if (timer == null) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = intent(timer)
        alarm.cancel(pending)
        if (preferences.notifications() && timer.phase() == GymRestTimer.Phase.RUNNING) {
            val deadline = timer.observedMillis() + timer.remainingMillis()
            alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pending)
        }
    }
}

class GymRestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val execution = intent.getStringExtra("execution") ?: return
        val trigger = intent.getStringExtra("trigger") ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = SqliteGymToolsRepository(context)
                if (!store.completeIfDue(execution, trigger, GymRestClock(context).now()))
                    return@launch
                val prefs = store.preferences()
                // Do not add gym audio/vibration over an ongoing Live Coach/recording session.
                if (!prefs.notifications() || RunRecordingService.status.value != null)
                    return@launch
                if (
                    Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                )
                    return@launch
                val manager = context.getSystemService(NotificationManager::class.java)
                if (!manager.areNotificationsEnabled()) return@launch
                val channelId = "gym-rest-${prefs.audio()}-${prefs.vibration()}"
                val channel =
                    NotificationChannel(
                        channelId,
                        "Optional gym rest alerts",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                channel.enableVibration(prefs.vibration())
                channel.setSound(
                    if (prefs.audio()) Settings.System.DEFAULT_NOTIFICATION_URI else null,
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                )
                manager.createNotificationChannel(channel)
                val open =
                    PendingIntent.getActivity(
                        context,
                        61011,
                        Intent(context, CoachActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                manager.notify(
                    61011,
                    Notification.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("Rest time complete")
                        .setContentText(
                            "Continue when you are ready. Rest is guidance, not a requirement."
                        )
                        .setContentIntent(open)
                        .setAutoCancel(true)
                        .setVisibility(Notification.VISIBILITY_PRIVATE)
                        .build(),
                )
            } catch (error: RuntimeException) {
                android.util.Log.w(
                    "GymRest",
                    "Optional rest alert unavailable; timer can be reviewed in app.",
                )
            } finally {
                pending.finish()
            }
        }
    }
}
