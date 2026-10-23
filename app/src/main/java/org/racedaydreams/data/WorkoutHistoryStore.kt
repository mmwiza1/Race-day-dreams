package org.racedaydreams.data

import android.content.Context
import android.os.Looper
import org.json.JSONObject
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.activity.WorkoutHistoryQuery.Kind
import org.racedaydreams.domain.strength.ExerciseSearch
import java.time.*

/** Keyset-paged metadata only. Never joins recording_points/events or hydrates routes. */
class WorkoutHistoryStore(context:Context) {
    private val helper=ProfileDatabase.get(context)
    data class Cursor(val millis:Long,val id:String,val kind:Int)
    data class Row(val id:String,val kind:Kind,val date:LocalDate,val label:String,val distanceMetres:Double?,val minutes:Long?,val exerciseCount:Int?)
    data class Page(val rows:List<Row>,val next:Cursor?)
    fun page(kind:Kind,type:WorkoutCategory?,exerciseQuery:String,from:LocalDate?,through:LocalDate?,prescriptions:List<PlannedWorkout>,after:Cursor?=null,limit:Int=30):Page {
        check(Looper.myLooper()!=Looper.getMainLooper()) { "History storage requires an IO thread." }
        require(limit in 1..100 && (from==null||through==null||!through.isBefore(from))) { "Check the selected date range." }
        val types=prescriptions.associate{it.id() to it.category()}
        val search=exerciseQuery.trim()
        val allowed=if(search.isEmpty())null else ExerciseSearch.search(CoachingStore(helperContext()).exercises(),search,emptySet(),emptyList(),emptySet()).map{it.id()}.toSet()
        if(allowed!=null&&allowed.isEmpty())return Page(emptyList(),null)
        val includeRuns=kind!=Kind.STRENGTH&&search.isEmpty()
        val includeStrength=kind!=Kind.RUNNING&&type==null
        if(!includeRuns&&!includeStrength)return Page(emptyList(),null)
        val rows=mutableListOf<Row>();var scanned=after;var lastIncluded:Cursor?=null
        val earliest=from?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.minusSeconds(18*3600)?.toEpochMilli()?:Long.MIN_VALUE
        val latest=through?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.plusSeconds(18*3600)?.toEpochMilli()?:Long.MAX_VALUE
        while(true) {
            val args=mutableListOf(earliest.toString(),latest.toString())
            val seek=scanned?.let { args.addAll(listOf(it.millis.toString(),it.millis.toString(),it.id,it.id,it.kind.toString()));" AND (at<? OR (at=? AND (id>? OR (id=? AND kind>?))))" }?:""
            val sql="SELECT * FROM ("+
                "SELECT a.id,0 kind,a.start_millis at,a.payload,r.planned_workout_id linked FROM activities a LEFT JOIN run_responses r ON r.activity_id=a.id WHERE ${if(includeRuns)"1"else"0"} UNION ALL "+
                "SELECT id,1 kind,CAST(ROUND((julianday(started_at)-2440587.5)*86400000) AS INTEGER) at,payload,NULL linked FROM strength_sessions WHERE ${if(includeStrength)"1"else"0"}"+
                ") WHERE at>=? AND at<=?"+seek+" ORDER BY at DESC,id,kind LIMIT 100"
            var count=0
            helper.readableDatabase.rawQuery(sql,args.toTypedArray()).use { c ->
                while(c.moveToNext()) {
                    count++;val cursor=Cursor(c.getLong(2),c.getString(0),c.getInt(1));scanned=cursor
                    val j=JSONObject(c.getString(3));val run=cursor.kind==0
                    val start=Instant.parse(j.getString(if(run)"start"else"startedAt"))
                    val offset=if(run)j.optString("startOffset").takeIf{it.isNotBlank()&&it!="null"}?.let(ZoneOffset::of)?:ZoneOffset.UTC else ZoneOffset.of(j.getString("offset"))
                    val date=start.atOffset(offset).toLocalDate()
                    if(from!=null&&date<from||through!=null&&date>through)continue
                    val category=if(run)types[if(c.isNull(4))j.optString("plannedWorkoutId")else c.getString(4)]else null
                    if(run&&type!=null&&category!=type)continue
                    val exercises=if(run)null else j.getJSONArray("exercises")
                    if(allowed!=null&&(0 until exercises!!.length()).none{exercises.getJSONObject(it).getJSONObject("exercise").getString("id") in allowed})continue
                    if(rows.size==limit)return Page(rows,lastIncluded)
                    rows.add(Row(cursor.id,if(run)Kind.RUNNING else Kind.STRENGTH,date,
                        if(!run)"Strength"else category?.name?.lowercase()?.replace('_',' ')?:"Run",
                        if(run&&!j.isNull("distanceMetres"))j.getDouble("distanceMetres")else null,
                        if(run)null else Duration.parse(j.getString("duration")).toMinutes(),exercises?.length()))
                    lastIncluded=cursor
                }
            }
            if(count<100)return Page(rows,null)
        }
    }
    // Keep only the application context; no Activity or UI can be retained by paging.
    private val appContext=context.applicationContext
    private fun helperContext()=appContext
}
