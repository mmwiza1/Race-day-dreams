package org.racedaydreams.data

import android.content.ContentValues
import android.content.Context
import org.json.JSONObject
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import java.time.*

/** Staging and canonical finalization share one database transaction. All calls on IO. */
class RecordingStore(context:Context) {
    private val helper=ProfileDatabase.get(context)
    data class Session(val id:String,val state:String,val start:Instant,val mono:Long,val last:Long)
    fun unfinished():Session?=helper.readableDatabase.rawQuery("SELECT id,state,started_at,started_mono,last_mono FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE')",null).use{c->if(c.moveToFirst())Session(c.getString(0),c.getString(1),Instant.parse(c.getString(2)),c.getLong(3),c.getLong(4))else null}
    fun start(id:String,wall:Instant,mono:Long){helper.writableDatabase.insertOrThrow("recording_sessions",null,ContentValues().apply{put("id",id);put("state","ACTIVE");put("started_at",wall.toString());put("started_mono",mono);put("last_mono",mono)})}
    fun flush(id:String,points:List<RunMetricsEngine.Point>,events:List<RunMetricsEngine.Event>,fromPoint:Int,fromEvent:Int,last:Long,state:String){
        val db=helper.writableDatabase;db.beginTransaction();try{
            for(i in fromPoint until points.size)db.insertOrThrow("recording_points",null,ContentValues().apply{put("session_id",id);put("sequence",i);put("payload",encode(points[i]).toString())})
            for(i in fromEvent until events.size)db.insertOrThrow("recording_events",null,ContentValues().apply{put("session_id",id);put("sequence",i);put("payload",JSONObject().put("type",events[i].type()).put("mono",events[i].monotonicMillis()).put("wall",events[i].timestamp().toString()).put("value",events[i].value()).toString())})
            db.update("recording_sessions",ContentValues().apply{put("last_mono",last);put("state",state)},"id=?",arrayOf(id));db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    fun points(id:String):List<RunMetricsEngine.Point>{val out=mutableListOf<RunMetricsEngine.Point>();helper.readableDatabase.rawQuery("SELECT payload FROM recording_points WHERE session_id=? ORDER BY sequence",arrayOf(id)).use{c->while(c.moveToNext())out.add(decode(JSONObject(c.getString(0))))};return out}
    fun discardUnfinished(id:String){helper.writableDatabase.delete("recording_sessions","id=? AND state IN ('ACTIVE','PAUSED','RECOVERABLE')",arrayOf(id))}
    fun recoverable(){helper.writableDatabase.execSQL("UPDATE recording_sessions SET state='RECOVERABLE' WHERE state IN ('ACTIVE','PAUSED')")}
    fun finish(session:Session,movingMillis:Long?=null,provider:String=if(session.id.startsWith("gpx-"))"gpx"else"rdd"):String {
        val db=helper.writableDatabase;db.beginTransaction();try{
            val points=points(session.id);require(points.size>=2){"At least two usable GPS points are needed. Staged evidence is retained."}
            val elapsed=(session.last-session.mono).coerceAtLeast(1);val samples=points.map{p->DistanceSample(Duration.ofMillis(p.fix().monotonicMillis()-session.mono),p.cumulativeMetres(),p.heartRate(),p.cadence(),p.fix().altitude(),p.discontinuity())}
            val hr=points.mapNotNull{it.heartRate()};val cad=points.mapNotNull{it.cadence()};val id="$provider:${session.id}"
            val a=ActivityRecord(id,SourceIdentity(provider,session.id,"org.racedaydreams"),session.start,session.start.plusMillis(elapsed),ZoneOffset.UTC,ZoneOffset.UTC,points.last().cumulativeMetres(),Duration.ofMillis((movingMillis?:points.last().activeMillis()).coerceIn(0,elapsed)),hr.takeIf{it.isNotEmpty()}?.average(),hr.maxOrNull(),null,cad.takeIf{it.isNotEmpty()}?.average(),null,"${if(provider=="rdd")"Recorded on this phone"else"Imported GPX (accuracy not supplied by file)"}. Distance excludes gaps; raw timestamps and events retained.",samples,emptyList(),null,null)
            FeatureStore.putActivity(db,a,true)
            db.update("recording_sessions",ContentValues().apply{put("state","FINISHED");put("activity_id",id)},"id=?",arrayOf(session.id));db.setTransactionSuccessful();return id
        }finally{db.endTransaction()}
    }
    fun existingActivity(id:String):String?=helper.readableDatabase.rawQuery("SELECT activity_id FROM recording_sessions WHERE id=?",arrayOf(id)).use{if(it.moveToFirst()&&!it.isNull(0))it.getString(0)else null}
    fun importRecording(id:String,wall:Instant,last:Long,points:List<RunMetricsEngine.Point>,events:List<RunMetricsEngine.Event>){val db=helper.writableDatabase;db.beginTransaction();try{start(id,wall,0);flush(id,points,events,0,0,last,"ACTIVE");db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun finished():List<Pair<String,String>>{val out=mutableListOf<Pair<String,String>>();helper.readableDatabase.rawQuery("SELECT id,started_at FROM recording_sessions WHERE state='FINISHED' ORDER BY started_at DESC",null).use{while(it.moveToNext())out.add(it.getString(0) to it.getString(1))};return out}
    companion object {
        fun encode(p:RunMetricsEngine.Point):JSONObject=JSONObject().put("wall",p.fix().timestamp().toString()).put("mono",p.fix().monotonicMillis()).put("lat",p.fix().latitude()).put("lon",p.fix().longitude()).put("accuracy",p.fix().accuracy()).put("alt",p.fix().altitude()).put("vertical",p.fix().verticalAccuracy()).put("speed",p.fix().speed()).put("distance",p.cumulativeMetres()).put("active",p.activeMillis()).put("segment",p.segment()).put("gap",p.discontinuity()).put("hr",p.heartRate()).put("cadence",p.cadence())
        private fun JSONObject.optional(key:String):Double?=if(has(key)&&!isNull(key))getDouble(key)else null
        fun decode(j:JSONObject)=RunMetricsEngine.Point(RunMetricsEngine.Fix(Instant.parse(j.getString("wall")),j.getLong("mono"),j.getDouble("lat"),j.getDouble("lon"),j.optional("accuracy"),j.optional("alt"),j.optional("vertical"),j.optional("speed")),j.getDouble("distance"),j.getLong("active"),j.getInt("segment"),j.getBoolean("gap"),j.optional("hr"),j.optional("cadence"))
    }
}
