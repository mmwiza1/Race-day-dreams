package org.racedaydreams.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.support.*
import org.racedaydreams.domain.WorkoutSegment
import java.time.Instant

/** Versioned app-owned support evidence. No calendar mirror or training-load side effect. */
class SupportStore(context:Context) {
    private val contextRef=context.applicationContext
    private val db=ProfileDatabase.get(context).writableDatabase
    fun get(kind:String,id:String):JSONObject?=db.rawQuery("SELECT payload FROM coach_support WHERE kind=? AND id=?",arrayOf(kind,id)).use{if(it.moveToFirst())JSONObject(it.getString(0))else null}
    fun save(kind:String,id:String,payload:JSONObject){put(db,kind,id,payload)}
    fun dream(activity:String,metres:Double):DreamRoutes.Assignment {
        db.beginTransaction();try {
            val retained=get("dream",activity)
            val result=if(retained!=null)DreamRoutes.Assignment(retained.getString("route"),retained.getDouble("start"),retained.getDouble("distance"))else DreamRoutes.assign(metres,java.security.SecureRandom())
            if(retained==null)save("dream",activity,JSONObject().put("version",1).put("route",result.routeId()).put("start",result.startMetres()).put("distance",result.coveredMetres()).put("assignedAt",Instant.now().toString()))
            db.setTransactionSuccessful();return result
        }finally{db.endTransaction()}
    }
    fun routineCount(from:Instant,routine:String?=null,related:String?=null,mobilityOnly:Boolean=false):Int {
        var count=0;db.rawQuery("SELECT payload FROM coach_support WHERE kind='routine'",null).use{c->while(c.moveToNext()){val j=JSONObject(c.getString(0));if((!mobilityOnly||RunningRoutines.isMobility(j.getString("routine")))&&!Instant.parse(j.getString("at")).isBefore(from)&&(routine==null||j.getString("routine")==routine)&&(related==null||j.optString("related")==related))count++}};return count
    }
    fun completeRoutine(id:String,related:String?){RunningRoutines.get(id);save("routine",java.util.UUID.randomUUID().toString(),JSONObject().put("version",1).put("routine",id).put("at",Instant.now().toString()).put("related",related))}
    fun prepare(versionId:String,workoutId:String,hilly:Boolean):JSONObject {
        val version=CoachingStore(contextRef).versions().first{it.id()==versionId}
        val workout=version.plan().workouts().first{it.id()==workoutId}
        val race=workout.category()==org.racedaydreams.domain.WorkoutCategory.RACE
        val goal=version.goalSnapshot().raceGoal()
        val steps=if(race)org.racedaydreams.domain.race.RaceStrategyEngine.create(goal.distance().kilometres()*1000,goal.targetTime()?.seconds,hilly).map{stage->WorkoutExecutionEngine.Step(stage.guidance(),WorkoutSegment.Kind.RUN,stage.to()-stage.from(),null,stage.fastPace(),stage.slowPace(),null)}else WorkoutExecutionEngine.from(workout)
        val rows=JSONArray();steps.forEach{rows.put(step(it))}
        val j=JSONObject().put("version",1).put("mode",if(race)"race"else"workout").put("planVersion",versionId).put("workoutId",workoutId).put("workout",FeatureJson.planned(workout)).put("steps",rows).put("goalMetres",goal.distance().kilometres()*1000).put("raceName",goal.raceName()).put("targetSeconds",goal.targetTime()?.seconds as Any?).put("hilly",hilly)
        if(race){val f=FeatureStore(contextRef);val p=org.racedaydreams.domain.race.RacePredictionEngine.predict(goal.distance().kilometres()*1000,f.results(),f.activitySummaries(),java.time.LocalDate.now());j.put("predictionLow",p.lowSeconds() as Any?).put("predictionHigh",p.highSeconds() as Any?).put("predictionConfidence",p.confidence())}
        validatePayload("execution",j);return j
    }
    fun linkFinished(sessionId:String,activityId:String,recovered:Boolean){
        val j=get("execution",sessionId)?:return
        val events=j.optJSONArray("events")?:JSONArray();val targets=steps(j)
        val completed=mutableSetOf<Int>();val skipped=mutableSetOf<Int>()
        for(i in 0 until events.length()){val e=events.getJSONObject(i);if(e.getString("type")=="STEP_COMPLETED")completed.add(e.getInt("step"));if(e.getString("type")=="STEP_SKIPPED")skipped.add(e.getInt("step"))}
        val reps=targets.indices.count{targets[it].kind()==WorkoutSegment.Kind.RUN&&it in completed}
        val recoveries=targets.indices.filter{targets[it].kind()==WorkoutSegment.Kind.RECOVERY}.all{it in completed}
        val measured=FeatureStore(contextRef).activityDetail(activityId)
        val reliable=measured!=null&&measured.samples().size>=2&&measured.samples().drop(1).none{it.discontinuity()}
        val stepEvidence=mutableListOf<org.racedaydreams.domain.coaching.RunExecution.StepEvidence>()
        targets.forEachIndexed{index,target->
            var start:JSONObject?=null;var end:JSONObject?=null
            for(i in 0 until events.length()){val e=events.getJSONObject(i);if(e.getInt("step")==index){if(e.getString("type")=="STEP_STARTED")start=e;if(e.getString("type") in listOf("STEP_COMPLETED","STEP_SKIPPED","WORKOUT_FINISHED_EARLY"))end=e}}
            val a=start;val b=end;if(a!=null&&b!=null)stepEvidence.add(org.racedaydreams.domain.coaching.RunExecution.StepEvidence(index,target.kind(),b.getDouble("metres")-a.getDouble("metres"),b.getLong("active")-a.getLong("active"),target.fastPace(),target.slowPace(),index in completed,reliable))
        }
        val response=org.racedaydreams.domain.coaching.RunExecution(activityId,j.getString("workoutId"),j.getString("planVersion"),if(FeatureJson.planned(j.getJSONObject("workout")).category()==org.racedaydreams.domain.WorkoutCategory.INTERVALS)reps else null,recoveries,"Native structured execution: ${completed.size}/${targets.size} steps completed, ${skipped.size} skipped."+(if(recovered)" Interrupted run: retained portion only; no completion is assumed."else""),stepEvidence)
        CoachingStore(contextRef).saveResponse(response)
        j.put("activityId",activityId).put("interrupted",recovered);save("execution",sessionId,j)
    }
    companion object {
        fun step(s:WorkoutExecutionEngine.Step)=JSONObject().put("label",s.label()).put("kind",s.kind().name).put("metres",s.metres() as Any?).put("millis",s.millis() as Any?).put("fast",s.fastPace() as Any?).put("slow",s.slowPace() as Any?).put("rpe",s.rpe() as Any?)
        private fun number(j:JSONObject,k:String):Double?=if(j.has(k)&&!j.isNull(k))j.getDouble(k)else null
        fun steps(j:JSONObject):List<WorkoutExecutionEngine.Step> {val a=j.getJSONArray("steps");require(a.length() in 1..500);return (0 until a.length()).map{val s=a.getJSONObject(it);WorkoutExecutionEngine.Step(s.getString("label"),WorkoutSegment.Kind.valueOf(s.getString("kind")),number(s,"metres"),if(s.has("millis")&&!s.isNull("millis"))s.getLong("millis")else null,number(s,"fast"),number(s,"slow"),number(s,"rpe"))}}
        @JvmStatic fun validatePayload(kind:String,j:JSONObject){require(j.get("version") is Int&&j.getInt("version")==1){"Unsupported support record version"};when(kind){
            "dream"->DreamRoutes.Assignment(j.getString("route"),j.getDouble("start"),j.getDouble("distance"))
            "routine"->{RunningRoutines.get(j.getString("routine"));Instant.parse(j.getString("at"))}
            "execution"->{val stepCount=steps(j).size;require(j.getString("mode") in listOf("workout","race"));require(j.getString("planVersion").length in 1..256&&j.getString("workoutId").length in 1..256);if(j.has("workout"))FeatureJson.planned(j.getJSONObject("workout"));if(j.has("events")){val e=j.getJSONArray("events");require(e.length()<=2000);var lastActive=0L;var lastMetres=0.0;for(i in 0 until e.length()){val x=e.getJSONObject(i);require(x.getInt("step") in 0..stepCount&&x.getLong("active")>=lastActive&&x.getDouble("metres").isFinite()&&x.getDouble("metres")>=lastMetres&&x.getString("type") in listOf("WORKOUT_STARTED","STEP_STARTED","STEP_COMPLETED","STEP_SKIPPED","WORKOUT_FINISHED_EARLY","WORKOUT_COMPLETED"));lastActive=x.getLong("active");lastMetres=x.getDouble("metres")}}}
            else->error("Unsupported support record kind")
        }}
        @JvmStatic fun put(db:SQLiteDatabase,kind:String,id:String,j:JSONObject){require(id.length in 1..256);validatePayload(kind,j);val v=ContentValues().apply{put("kind",kind);put("id",id);put("payload",j.toString())};if(db.update("coach_support",v,"kind=? AND id=?",arrayOf(kind,id))==0)db.insertOrThrow("coach_support",null,v)}
        @JvmStatic fun exportData(db:SQLiteDatabase):JSONObject {val rows=JSONArray();db.rawQuery("SELECT kind,id,payload FROM coach_support ORDER BY kind,id",null).use{while(it.moveToNext())rows.put(JSONObject().put("kind",it.getString(0)).put("id",it.getString(1)).put("payload",JSONObject(it.getString(2))))};return JSONObject().put("version",1).put("rows",rows)}
        @JvmStatic fun validate(j:JSONObject){require(j.get("version") is Int&&j.getInt("version")==1);val rows=j.getJSONArray("rows");require(rows.length()<=100000);val ids=mutableSetOf<String>();for(i in 0 until rows.length()){val r=rows.getJSONObject(i);val id=r.getString("id");val kind=r.getString("kind");require(id.length in 1..256&&ids.add("$kind:$id"));validatePayload(kind,r.getJSONObject("payload"))}}
        @JvmStatic fun restore(db:SQLiteDatabase,j:JSONObject?){if(j!=null)validate(j);db.delete("coach_support",null,null);if(j!=null){val rows=j.getJSONArray("rows");for(i in 0 until rows.length()){val r=rows.getJSONObject(i);put(db,r.getString("kind"),r.getString("id"),r.getJSONObject("payload"))}}}
    }
}
