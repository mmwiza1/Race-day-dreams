package org.racedaydreams.data;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import org.json.*;
import java.time.Instant;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.*;

/** Shared database and transactions; immutable versions plus one current pointer. All calls require IO. */
public final class CoachingStore {
    private final ProfileDatabase helper;
    public CoachingStore(Context context) { helper=ProfileDatabase.get(context); }
    private SQLiteDatabase db(){if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Coaching storage requires IO.");return helper.getWritableDatabase();}
    private interface Parser<T>{T read(JSONObject row)throws JSONException;}
    private static <T> List<T> rows(SQLiteDatabase db,String sql,Parser<T> parser){List<T> out=new ArrayList<>();try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext())out.add(parser.read(FeatureJson.object(c.getString(0))));}catch(JSONException e){throw FeatureJson.invalid(e);}return org.racedaydreams.domain.coaching.Immutable.copyList(out);}
    public void transaction(Runnable action){SQLiteDatabase db=db();db.beginTransaction();try{action.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public List<PlanVersion> versions(){return rows(db(),"SELECT payload FROM coach_plan_versions ORDER BY rowid DESC",CoachingJson::version);}
    public PlanVersion current(){var rows=rows(db(),"SELECT v.payload FROM coach_current_plan c JOIN coach_plan_versions v ON c.version_id=v.id WHERE c.id=1",CoachingJson::version);return rows.isEmpty()?null:rows.get(0);}
    public List<StrengthExercise> exercises(){var all=new LinkedHashMap<String,StrengthExercise>();for(var e:StrengthExercise.starters())all.put(e.id(),e);for(var e:rows(db(),"SELECT payload FROM strength_exercises ORDER BY id",CoachingJson::exercise))all.put(e.id(),e);return org.racedaydreams.domain.coaching.Immutable.copyList(all.values());}
    public List<StrengthSession> strength(){return rows(db(),"SELECT payload FROM strength_sessions ORDER BY started_at DESC,id",CoachingJson::strength);}
    public List<RunExecution> responses(){return rows(db(),"SELECT payload FROM run_responses ORDER BY activity_id",CoachingJson::response);}
    public List<AdjustmentProposal> proposals(){return rows(db(),"SELECT payload FROM coach_proposals WHERE decision IS NULL ORDER BY rowid DESC",CoachingJson::proposal);}
    public boolean proposalKnown(String id){try(Cursor c=db().rawQuery("SELECT 1 FROM coach_proposals WHERE id=?",new String[]{id})){return c.moveToFirst();}}
    public void saveExercise(StrengthExercise e){ContentValues v=payload(e.id(),CoachingJson.exercise(e));upsert(db(),"strength_exercises","id",e.id(),v);}
    public void saveStrength(StrengthSession s){if(s.startedAt().isAfter(Instant.now()))throw new IllegalArgumentException("A completed strength session cannot be in the future.");ContentValues v=payload(s.id(),CoachingJson.strength(s));v.put("started_at",s.startedAt().toString());upsert(db(),"strength_sessions","id",s.id(),v);}
    public void deleteStrength(String id){db().delete("strength_sessions","id=?",new String[]{id});}
    public void saveResponse(RunExecution r){SQLiteDatabase db=db();if(r.planVersionId()!=null){PlanVersion version=versions().stream().filter(v->v.id().equals(r.planVersionId())).findFirst().orElseThrow(()->new IllegalArgumentException("Plan version not found."));if(version.plan().workouts().stream().noneMatch(w->w.id().equals(r.plannedWorkoutId())))throw new IllegalArgumentException("Workout is not in this plan version.");}
        ContentValues v=new ContentValues();v.put("activity_id",r.activityId());v.put("planned_workout_id",r.plannedWorkoutId());v.put("plan_version_id",r.planVersionId());v.put("payload",CoachingJson.response(r).toString());upsert(db,"run_responses","activity_id",r.activityId(),v);}
    public void saveFeedback(String activityId,String versionId,RunFeedback feedback){ContentValues v=new ContentValues();v.put("activity_id",activityId);v.put("plan_version_id",versionId);v.put("payload",CoachingJson.feedback(feedback).toString());upsert(db(),"run_feedback","activity_id",activityId,v);}
    public void saveProposal(AdjustmentProposal p){var v=payload(p.id(),CoachingJson.proposal(p));v.put("base_version_id",p.baseVersionId());db().insertOrThrow("coach_proposals",null,v);}
    public void saveCurrent(PlanVersion version){SQLiteDatabase db=db();db.beginTransaction();try{saveCurrent(db,version);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static void saveCurrent(SQLiteDatabase db,PlanVersion version){var v=payload(version.id(),CoachingJson.version(version));v.put("plan_id",version.planId());v.put("version",version.version());db.insertOrThrow("coach_plan_versions",null,v);
        ContentValues pointer=new ContentValues();pointer.put("id",1);pointer.put("version_id",version.id());upsert(db,"coach_current_plan","id","1",pointer);
        // Only the authoritative current version drives reminders. History lives in immutable versions.
        Map<String,Long> revisions=new HashMap<>();Map<String,String> previous=new HashMap<>();
        try(Cursor c=db.rawQuery("SELECT id,revision,payload FROM planned_workouts",null)){while(c.moveToNext()){revisions.put(c.getString(0),c.getLong(1));previous.put(c.getString(0),c.getString(2));}}
        db.delete("planned_workouts",null,null);
        for(var w:version.plan().workouts()){String json=FeatureJson.planned(w).toString();ContentValues values=payload(w.id(),FeatureJson.planned(w));values.put("scheduled_date",w.date().toString());values.put("revision",revisions.getOrDefault(w.id(),0L)+(json.equals(previous.get(w.id()))?0:1));db.insertOrThrow("planned_workouts",null,values);}
    }
    public void decide(AdjustmentProposal proposal,String optionId,Set<String> completed){SQLiteDatabase db=db();db.beginTransaction();try{
        PlanVersion current=current();if(current==null||!current.id().equals(proposal.baseVersionId()))throw new IllegalArgumentException("The plan has changed. Review the current recommendation.");
        try(Cursor c=db.rawQuery("SELECT decision FROM coach_proposals WHERE id=?",new String[]{proposal.id()})){if(!c.moveToFirst()||!c.isNull(0))throw new IllegalArgumentException("This proposal has already been resolved.");}
        var option=proposal.options().stream().filter(o->o.id().equals(optionId)).findFirst().orElseThrow(()->new IllegalArgumentException("Choose a proposal option."));
        String result=current.id();
        if(option.action()!=AdjustmentProposal.Action.KEEP){TrainingPlan changed=AdaptiveCoachingEngine.apply(current.plan(),proposal,option,completed);result=UUID.randomUUID().toString();saveCurrent(db,new PlanVersion(result,current.planId(),current.version()+1,Instant.now(),"Runner chose: "+option.label(),current.goalSnapshot(),current.evidenceSnapshot(),changed));}
        ContentValues v=new ContentValues();v.put("decision",optionId);v.put("result_version_id",result);db.update("coach_proposals",v,"id=?",new String[]{proposal.id()});db.setTransactionSuccessful();
    }finally{db.endTransaction();}}
    private static ContentValues payload(String id,JSONObject j){ContentValues v=new ContentValues();v.put("id",id);v.put("payload",j.toString());return v;}
    private static void upsert(SQLiteDatabase db,String table,String key,String id,ContentValues values){if(db.update(table,values,key+"=?",new String[]{id})==0)db.insertOrThrow(table,null,values);}

    static final String[] TABLES={"run_feedback","run_responses","coach_proposals","coach_current_plan","coach_plan_versions","strength_sessions","strength_exercises"};
    static JSONObject exportData(SQLiteDatabase db){JSONObject out=FeatureJson.json("version",1);try{for(String table:TABLES){JSONArray rows=new JSONArray();try(Cursor c=db.rawQuery("SELECT * FROM "+table+" ORDER BY rowid",null)){while(c.moveToNext()){JSONObject row=new JSONObject();for(int i=0;i<c.getColumnCount();i++)row.put(c.getColumnName(i),c.isNull(i)?JSONObject.NULL:c.getString(i));rows.put(row);}}out.put(table,rows);}return out;}catch(JSONException e){throw FeatureJson.invalid(e);}}
    /** Validate identities, typed payloads and graph references before the replacement transaction. */
    static void validate(JSONObject data){try{
        if(FeatureJson.requiredInteger(data,"version")!=1)throw new IllegalArgumentException("Unsupported coaching backup version.");
        Map<String,PlanVersion> versions=new HashMap<>();Set<String> numbers=new HashSet<>();
        for(JSONObject row:array(data,"coach_plan_versions")){var v=CoachingJson.version(FeatureJson.object(row.getString("payload")));match(row,"id",v.id());match(row,"plan_id",v.planId());match(row,"version",Integer.toString(v.version()));if(versions.put(v.id(),v)!=null||!numbers.add(v.planId()+":"+v.version()))throw new IllegalArgumentException("Duplicated plan version.");}
        var current=array(data,"coach_current_plan");if(current.size()>1)throw new IllegalArgumentException("Only one plan can be current.");for(var row:current){match(row,"id","1");if(!versions.containsKey(row.getString("version_id")))throw new IllegalArgumentException("Current version is missing.");}
        Set<String> ids=new HashSet<>();for(var row:array(data,"strength_exercises")){var e=CoachingJson.exercise(FeatureJson.object(row.getString("payload")));match(row,"id",e.id());if(!ids.add(e.id()))throw new IllegalArgumentException("Duplicate exercise.");}
        ids.clear();for(var row:array(data,"strength_sessions")){var s=CoachingJson.strength(FeatureJson.object(row.getString("payload")));match(row,"id",s.id());match(row,"started_at",s.startedAt().toString());if(!ids.add(s.id()))throw new IllegalArgumentException("Duplicate strength workout.");}
        ids.clear();for(var row:array(data,"run_responses")){var r=CoachingJson.response(FeatureJson.object(row.getString("payload")));match(row,"activity_id",r.activityId());match(row,"planned_workout_id",r.plannedWorkoutId());match(row,"plan_version_id",r.planVersionId());if(!ids.add(r.activityId()))throw new IllegalArgumentException("Duplicate response.");if(r.planVersionId()!=null){var v=versions.get(r.planVersionId());if(v==null||v.plan().workouts().stream().noneMatch(w->w.id().equals(r.plannedWorkoutId())))throw new IllegalArgumentException("Response prescription is missing.");}}
        ids.clear();for(var row:array(data,"coach_proposals")){var p=CoachingJson.proposal(FeatureJson.object(row.getString("payload")));match(row,"id",p.id());match(row,"base_version_id",p.baseVersionId());if(!ids.add(p.id())||!versions.containsKey(p.baseVersionId()))throw new IllegalArgumentException("Invalid proposal identity/version.");String decision=FeatureJson.text(row,"decision"),result=FeatureJson.text(row,"result_version_id");if(decision!=null&&(p.options().stream().noneMatch(o->o.id().equals(decision))||result==null||!versions.containsKey(result)))throw new IllegalArgumentException("Invalid proposal decision.");}
        ids.clear();for(var row:array(data,"run_feedback")){String activity=row.getString("activity_id");if(!ids.add(activity)||!versions.containsKey(row.getString("plan_version_id")))throw new IllegalArgumentException("Invalid feedback identity/version.");JSONObject f=FeatureJson.object(row.getString("payload"));if(FeatureJson.requiredInteger(f,"ruleVersion")!=RunScoreEngine.RULE_VERSION)throw new IllegalArgumentException("Unsupported feedback rule version.");Integer score=FeatureJson.integer(f,"score");if(score!=null&&(score<1||score>10))throw new IllegalArgumentException("Invalid Run Score.");f.getJSONArray("components");}
    }catch(JSONException|java.time.DateTimeException e){throw FeatureJson.invalid(e);}}
    private static void match(JSONObject row,String key,String value)throws JSONException{if(!Objects.equals(FeatureJson.text(row,key),value))throw new IllegalArgumentException("Backup column/payload disagree: "+key);}
    private static List<JSONObject> array(JSONObject j,String key)throws JSONException{JSONArray a=j.getJSONArray(key);if(a.length()>100000)throw new IllegalArgumentException("Too many coaching records.");List<JSONObject> rows=new ArrayList<>();for(int i=0;i<a.length();i++)rows.add(a.getJSONObject(i));return rows;}
    static void clear(SQLiteDatabase db){for(String table:TABLES)db.delete(table,null,null);}
    static void restore(SQLiteDatabase db,JSONObject data){try{for(int t=TABLES.length-1;t>=0;t--){String table=TABLES[t];for(var row:array(data,table)){ContentValues v=new ContentValues();for(Iterator<String> it=row.keys();it.hasNext();){String key=it.next();v.put(key,FeatureJson.text(row,key));}db.insertOrThrow(table,null,v);}}}catch(JSONException e){throw FeatureJson.invalid(e);}}
}
