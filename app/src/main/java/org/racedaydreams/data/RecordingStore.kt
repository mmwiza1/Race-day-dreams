package org.racedaydreams.data

import android.content.ContentValues
import android.content.Context
import org.json.JSONObject
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import java.time.*

/** Staging and canonical finalization share one database transaction. All calls on IO. */
class RecordingStore(context:Context) {
    private val contextRef=context.applicationContext
    private val helper=ProfileDatabase.get(context)
    data class Session(val id:String,val state:String,val start:Instant,val mono:Long,val last:Long)
    fun unfinished():Session?=helper.readableDatabase.rawQuery("SELECT id,state,started_at,started_mono,last_mono FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE')",null).use{c->if(c.moveToFirst())Session(c.getString(0),c.getString(1),Instant.parse(c.getString(2)),c.getLong(3),c.getLong(4))else null}
    fun start(id:String,wall:Instant,mono:Long){helper.writableDatabase.insertOrThrow("recording_sessions",null,ContentValues().apply{put("id",id);put("state","ACTIVE");put("started_at",wall.toString());put("started_mono",mono);put("last_mono",mono)})}
    fun flush(id:String,points:List<RunMetricsEngine.Point>,events:List<RunMetricsEngine.Event>,fromPoint:Int,fromEvent:Int,last:Long,state:String,executionContext:JSONObject?=null){
        val db=helper.writableDatabase;db.beginTransaction();try{
            for(i in fromPoint until points.size)db.insertOrThrow("recording_points",null,ContentValues().apply{put("session_id",id);put("sequence",i);put("payload",encode(points[i]).toString())})
            for(i in fromEvent until events.size)db.insertOrThrow("recording_events",null,ContentValues().apply{put("session_id",id);put("sequence",i);put("payload",JSONObject().put("type",events[i].type()).put("mono",events[i].monotonicMillis()).put("wall",events[i].timestamp().toString()).put("value",events[i].value() as Any?).toString())})
            db.update("recording_sessions",ContentValues().apply{put("last_mono",last);put("state",state)},"id=?",arrayOf(id));executionContext?.let{SupportStore.put(db,"execution",id,it)};db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    fun points(id:String):List<RunMetricsEngine.Point>{val out=mutableListOf<RunMetricsEngine.Point>();helper.readableDatabase.rawQuery("SELECT payload FROM recording_points WHERE session_id=? ORDER BY sequence",arrayOf(id)).use{c->while(c.moveToNext())out.add(decode(JSONObject(c.getString(0))))};return out}
    data class Pause(val start:Long,val duration:Long,val metres:Double)
    fun pauses(id:String):List<Pause>{val points=points(id);val out=mutableListOf<Pause>();var start:Long?=null;helper.readableDatabase.rawQuery("SELECT payload FROM recording_events WHERE session_id=? ORDER BY sequence",arrayOf(id)).use{c->while(c.moveToNext()){val j=JSONObject(c.getString(0));val type=j.getString("type");if(type in listOf("PAUSE","AUTO_PAUSE"))start=j.getLong("mono");if(type in listOf("RESUME","AUTO_RESUME","FINISH")){start?.let{t->out.add(Pause(t,(j.getLong("mono")-t).coerceAtLeast(0),points.lastOrNull{it.fix().monotonicMillis()<=t}?.cumulativeMetres()?:0.0))};start=null}}};return out}
    fun discardUnfinished(id:String){val db=helper.writableDatabase;db.beginTransaction();try{if(db.delete("recording_sessions","id=? AND state IN ('ACTIVE','PAUSED','RECOVERABLE')",arrayOf(id))==1){db.delete("coach_support","kind='execution' AND id=?",arrayOf(id));db.delete("release08","kind='origin' AND id=?",arrayOf(id))};db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun recoverable(){helper.writableDatabase.execSQL("UPDATE recording_sessions SET state='RECOVERABLE' WHERE state IN ('ACTIVE','PAUSED')")}
    fun finish(session:Session,movingMillis:Long?=null,provider:String=if(session.id.startsWith("gpx-"))"gpx"else"rdd",recovered:Boolean=true):String {
        val db=helper.writableDatabase;db.beginTransaction();try{
            val points=points(session.id);require(points.size>=2){"At least two usable GPS points are needed. Staged evidence is retained."}
            val timeline=mutableListOf<RunMetricsEngine.Event>()
            db.rawQuery("SELECT payload FROM recording_events WHERE session_id=? ORDER BY sequence",arrayOf(session.id)).use{c->while(c.moveToNext()){val j=JSONObject(c.getString(0));timeline.add(RunMetricsEngine.Event(j.getString("type"),j.getLong("mono"),Instant.parse(j.getString("wall"))))}}
            val active=movingMillis?:org.racedaydreams.domain.recording.RecordingTimeline.activeMillis(session.mono,session.last,timeline)
            val elapsed=(session.last-session.mono).coerceAtLeast(1);val samples=points.map{p->DistanceSample(Duration.ofMillis(p.fix().monotonicMillis()-session.mono),p.cumulativeMetres(),p.heartRate(),p.cadence(),p.fix().altitude(),p.discontinuity())}
            val hr=points.mapNotNull{it.heartRate()};val cad=points.mapNotNull{it.cadence()};val id="$provider:${session.id}"
            val a=ActivityRecord(id,SourceIdentity(provider,session.id,"org.racedaydreams"),session.start,session.start.plusMillis(elapsed),ZoneOffset.UTC,ZoneOffset.UTC,points.last().cumulativeMetres(),if(provider=="gpx")null else Duration.ofMillis(active.coerceIn(0,elapsed)),hr.takeIf{it.isNotEmpty()}?.average(),hr.maxOrNull(),null,cad.takeIf{it.isNotEmpty()}?.average(),null,"${if(provider=="rdd")"Recorded on this phone"else"Imported GPX (accuracy not supplied by file)"}. Distance excludes gaps; raw timestamps and events retained.",samples,emptyList(),null,null)
            Release08Store(contextRef).get("origin",session.id)?.optString("dreamRoute")?.takeIf{it.isNotBlank()&&it!="null"}?.let{route->
                val distance=points.last().cumulativeMetres();if(distance>0)SupportStore(contextRef).save("dream",id,JSONObject().put("version",1).put("route",route).put("start",0).put("distance",distance).put("assignedAt",Instant.now().toString()))
            }
            FeatureStore.putActivity(db,a,true)
            db.update("recording_sessions",ContentValues().apply{put("state","FINISHED");put("activity_id",id)},"id=?",arrayOf(session.id));SupportStore(contextRef).linkFinished(session.id,id,recovered);db.setTransactionSuccessful();return id
        }finally{db.endTransaction()}
    }
    fun existingActivity(id:String):String?=helper.readableDatabase.rawQuery("SELECT activity_id FROM recording_sessions WHERE id=?",arrayOf(id)).use{if(it.moveToFirst()&&!it.isNull(0))it.getString(0)else null}
    fun importRecording(id:String,wall:Instant,last:Long,points:List<RunMetricsEngine.Point>,events:List<RunMetricsEngine.Event>){val db=helper.writableDatabase;db.beginTransaction();try{start(id,wall,0);flush(id,points,events,0,0,last,"ACTIVE");db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun finished():List<Pair<String,String>>{val out=mutableListOf<Pair<String,String>>();helper.readableDatabase.rawQuery("SELECT id,started_at FROM recording_sessions WHERE state='FINISHED' ORDER BY started_at DESC",null).use{while(it.moveToNext())out.add(it.getString(0) to it.getString(1))};return out}
    companion object {
        /** Native sample streams are normalized rows, not a multi-MiB SQLite cursor cell. */
        @JvmStatic fun withRetainedSamples(db:android.database.sqlite.SQLiteDatabase,a:ActivityRecord):ActivityRecord {
            if(a.samples().isNotEmpty()||a.source().provider() !in listOf("rdd","gpx"))return a
            var id:String?=null;var mono=0L
            db.rawQuery("SELECT id,started_mono FROM recording_sessions WHERE activity_id=? AND state='FINISHED'",arrayOf(a.id())).use{c->if(c.moveToFirst()){id=c.getString(0);mono=c.getLong(1)}}
            val key=id?:return a;val samples=mutableListOf<DistanceSample>()
            db.rawQuery("SELECT payload FROM recording_points WHERE session_id=? ORDER BY sequence",arrayOf(key)).use{c->while(c.moveToNext()){val p=decode(JSONObject(c.getString(0)));samples.add(DistanceSample(Duration.ofMillis(p.fix().monotonicMillis()-mono),p.cumulativeMetres(),p.heartRate(),p.cadence(),p.fix().altitude(),p.discontinuity()))}}
            return ActivityRecord(a.id(),a.source(),a.start(),a.end(),a.startOffset(),a.endOffset(),a.distanceMetres(),a.movingTime(),a.averageHeartRate(),a.maxHeartRate(),a.elevationGainMetres(),a.cadence(),a.rpe(),a.notes(),samples,a.laps(),a.plannedWorkoutId(),a.shoeId())
        }
        /** Recompute qualified PB evidence after metadata and companion archive restore. */
        @JvmStatic fun restoreRetainedSamples(db:android.database.sqlite.SQLiteDatabase){
            val payloads=mutableListOf<String>()
            db.rawQuery("SELECT a.payload FROM recording_sessions s JOIN activities a ON a.id=s.activity_id WHERE s.state='FINISHED' AND a.provider IN ('rdd','gpx')",null).use{c->while(c.moveToNext())payloads.add(c.getString(0))}
            for(payload in payloads){val a=FeatureJson.activity(JSONObject(payload));val full=withRetainedSamples(db,a);if(full.samples().isNotEmpty())FeatureStore.putActivity(db,full,false)}
        }

        fun encode(p:RunMetricsEngine.Point):JSONObject=JSONObject().put("wall",p.fix().timestamp().toString()).put("mono",p.fix().monotonicMillis()).put("lat",p.fix().latitude()).put("lon",p.fix().longitude()).put("accuracy",p.fix().accuracy() as Any?).put("alt",p.fix().altitude() as Any?).put("vertical",p.fix().verticalAccuracy() as Any?).put("speed",p.fix().speed() as Any?).put("distance",p.cumulativeMetres()).put("active",p.activeMillis()).put("segment",p.segment()).put("gap",p.discontinuity()).put("hr",p.heartRate() as Any?).put("cadence",p.cadence() as Any?)
        private fun JSONObject.optional(key:String):Double?=if(has(key)&&!isNull(key))getDouble(key)else null
        fun decode(j:JSONObject)=RunMetricsEngine.Point(RunMetricsEngine.Fix(Instant.parse(j.getString("wall")),j.getLong("mono"),j.getDouble("lat"),j.getDouble("lon"),j.optional("accuracy"),j.optional("alt"),j.optional("vertical"),j.optional("speed")),j.getDouble("distance"),j.getLong("active"),j.getInt("segment"),j.getBoolean("gap"),j.optional("hr"),j.optional("cadence"))
    }
}
