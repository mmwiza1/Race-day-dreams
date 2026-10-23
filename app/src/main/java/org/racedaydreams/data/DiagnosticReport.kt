package org.racedaydreams.data
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
/** Privacy allow-list, independent of user databases and sample stores. No logs collected. */
object DiagnosticReport {
 fun build(context:Context,availability:String,healthPermissions:Set<String>):JSONObject {
    val info=context.packageManager.getPackageInfo(context.packageName,0)
    return JSONObject().put("format","rdd-diagnostic").put("version",1).put("appVersion",info.versionName).put("versionCode",info.longVersionCode).put("schema",ProfileDatabase.VERSION).put("backupEnvelope",org.racedaydreams.portability.BackupManager.VERSION).put("recordingArchive",2).put("completePackage",1).put("androidApi",Build.VERSION.SDK_INT).put("deviceModel",Build.MODEL).put("healthConnect",availability).put("healthPermissions",JSONArray(healthPermissions.sorted())).put("locationGranted",context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED).put("notificationGranted",Build.VERSION.SDK_INT<33||context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
 }
}
