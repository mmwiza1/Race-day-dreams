package org.racedaydreams.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.*
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.data.HealthIntelligenceStore
import org.racedaydreams.domain.health.HealthIntelligence.*
import java.time.*

/** Optional data cache. No training mutations. Steps use HC's deduplicating aggregate API. */
class HealthIntelligenceGateway(private val context:Context) {
    companion object {
        val permissions=setOf(HealthPermission.getReadPermission(SleepSessionRecord::class),HealthPermission.getReadPermission(RestingHeartRateRecord::class),HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),HealthPermission.getReadPermission(StepsRecord::class))
    }
    suspend fun sync():String=withContext(Dispatchers.IO){
        val client=HealthConnectClient.getOrCreate(context)
        val granted=client.permissionController.getGrantedPermissions()
        val now=Instant.now();val zone=ZoneId.systemDefault();val today=now.atZone(zone).toLocalDate()
        val from=today.minusDays(if(HealthConnectGateway.HISTORY_PERMISSION in granted)34 else 27)
        val start=from.atStartOfDay(zone).toInstant();val rows=mutableListOf<Observation>();val metrics=mutableSetOf<Metric>()
        suspend fun <T:Record> read(type:kotlin.reflect.KClass<T>):List<T>{
            val out=mutableListOf<T>();var page:String?=null;var n=0
            do{check(++n<=100){"Health history too large"};val r=client.readRecords(ReadRecordsRequest(type,TimeRangeFilter.between(start,now),pageSize=500,pageToken=page));out.addAll(r.records);check(out.size<=50000);page=r.pageToken}while(page!=null);return out
        }
        if(HealthPermission.getReadPermission(SleepSessionRecord::class) in granted){metrics.add(Metric.SLEEP_MINUTES);read(SleepSessionRecord::class).filter{it.endTime<=now}.forEach{r->val day=r.endTime.atOffset(r.endZoneOffset?:ZoneOffset.UTC).toLocalDate();if(day>=from&&day<=today)rows.add(Observation("sleep:${r.metadata.id}",Metric.SLEEP_MINUTES,day,r.startTime,r.endTime,Duration.between(r.startTime,r.endTime).toMillis()/60000.0,r.metadata.dataOrigin.packageName,now))}}
        if(HealthPermission.getReadPermission(RestingHeartRateRecord::class) in granted){metrics.add(Metric.RESTING_HR);read(RestingHeartRateRecord::class).forEach{r->rows.add(Observation("rhr:${r.metadata.id}",Metric.RESTING_HR,r.time.atOffset(r.zoneOffset?:ZoneOffset.UTC).toLocalDate(),r.time,r.time,r.beatsPerMinute.toDouble(),r.metadata.dataOrigin.packageName,now))}}
        if(HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) in granted){metrics.add(Metric.HRV_RMSSD);read(HeartRateVariabilityRmssdRecord::class).forEach{r->rows.add(Observation("hrv:${r.metadata.id}",Metric.HRV_RMSSD,r.time.atOffset(r.zoneOffset?:ZoneOffset.UTC).toLocalDate(),r.time,r.time,r.heartRateVariabilityMillis,r.metadata.dataOrigin.packageName,now))}}
        if(HealthPermission.getReadPermission(StepsRecord::class) in granted){metrics.add(Metric.STEPS);var day=from;while(day<today){val a=day.atStartOfDay(zone).toInstant();val b=day.plusDays(1).atStartOfDay(zone).toInstant();val total=client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),TimeRangeFilter.between(a,b)))[StepsRecord.COUNT_TOTAL];if(total!=null)rows.add(Observation("steps:$day",Metric.STEPS,day,a,b,total.toDouble(),"Health Connect aggregate",now));day=day.plusDays(1)}}
        check(client.permissionController.getGrantedPermissions().containsAll(granted.intersect(permissions))){"Permissions changed; no health data replaced"}
        HealthIntelligenceStore(context).replace(metrics,from,today,rows.filter{it.date()>=from&&it.date()<=today})
        "Updated ${metrics.size} optional health signals. Missing measurements remain unavailable."
    }
}
