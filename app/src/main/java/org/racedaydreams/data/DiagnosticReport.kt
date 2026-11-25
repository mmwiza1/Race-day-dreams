package org.racedaydreams.data

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import org.json.JSONArray
import org.json.JSONObject
import org.racedaydreams.portability.BackupManager

/** Basic diagnostics read only the device and the existing Health Connect permission state. */
object DiagnosticReport {
    fun build(context: Context, availability: String, healthPermissions: Set<String>): JSONObject {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        fun permission(name: String) =
            context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED

        val precise = permission(Manifest.permission.ACCESS_FINE_LOCATION)
        val approximate = permission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val power = context.getSystemService(PowerManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        return JSONObject()
            .put("format", "rdd-diagnostic")
            .put("version", 2)
            .put("appVersion", info.versionName)
            .put("versionCode", info.longVersionCode)
            .put("schema", ProfileDatabase.VERSION)
            .put("backupEnvelope", BackupManager.VERSION)
            .put("recordingArchive", RecordingArchive.VERSION)
            .put("completePackage", 1)
            .put("androidApi", Build.VERSION.SDK_INT)
            .put("androidVersion", Build.VERSION.RELEASE)
            .put("deviceModel", Build.MODEL)
            .put("healthConnect", availability)
            .put("healthPermissions", JSONArray(healthPermissions.sorted()))
            .put("locationGranted", precise)
            .put("preciseLocationGranted", precise)
            .put("approximateLocationGranted", approximate)
            .put(
                "notificationGranted",
                Build.VERSION.SDK_INT < 33 || permission(Manifest.permission.POST_NOTIFICATIONS),
            )
            .put(
                "notificationsEnabled",
                notifications?.areNotificationsEnabled() ?: JSONObject.NULL,
            )
            .put("batterySaver", power?.isPowerSaveMode ?: JSONObject.NULL)
            .put(
                "batteryOptimisationExempt",
                power?.isIgnoringBatteryOptimizations(context.packageName) ?: JSONObject.NULL,
            )
            .put(
                "backgroundRestricted",
                context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted
                    ?: JSONObject.NULL,
            )
    }
}
