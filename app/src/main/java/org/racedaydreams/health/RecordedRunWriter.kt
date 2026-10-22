package org.racedaydreams.health
import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.*
import androidx.health.connect.client.units.Length
import org.racedaydreams.domain.activity.ActivityRecord

/** Explicit per-run write, stable client IDs. No sleep/HRV writes or fabricated route/HR payloads. */
class RecordedRunWriter(private val context:Context){
    companion object {val permissions=setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class),HealthPermission.getWritePermission(DistanceRecord::class))}
    suspend fun write(activity:ActivityRecord){
        require(activity.source().provider()=="rdd"){"Only this app's native recordings may be written"}
        val client=HealthConnectClient.getOrCreate(context);check(client.permissionController.getGrantedPermissions().containsAll(permissions)){"Optional write permission required"}
        val device=Device(type=Device.TYPE_PHONE,manufacturer=Build.MANUFACTURER,model=Build.MODEL)
        fun metadata(part:String)=Metadata.activelyRecorded(device=device,clientRecordId="rdd:${activity.source().recordId()}:$part",clientRecordVersion=1)
        val rows=mutableListOf<Record>(ExerciseSessionRecord(startTime=activity.start(),endTime=activity.end(),startZoneOffset=activity.startOffset(),endZoneOffset=activity.endOffset(),exerciseType=ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,title="Race Day Dreams run",metadata=metadata("session")))
        activity.distanceMetres()?.let{rows.add(DistanceRecord(startTime=activity.start(),endTime=activity.end(),startZoneOffset=activity.startOffset(),endZoneOffset=activity.endOffset(),distance=Length.meters(it),metadata=metadata("distance")))}
        client.insertRecords(rows)
    }
}
