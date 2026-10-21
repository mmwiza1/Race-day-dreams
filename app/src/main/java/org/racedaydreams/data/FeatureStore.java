package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.UserProfile;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.baseline.ManualBaselineInput;
import org.racedaydreams.domain.baseline.AssessmentResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Local feature repositories. Every database call is synchronous and must run on an IO thread. */
public final class FeatureStore implements RunnerDataRepository, AutoCloseable {
    public static final int EXPORT_STORE_VERSION = 1;
    private final ProfileDatabase helper;
    public FeatureStore(Context context) { helper = ProfileDatabase.get(Objects.requireNonNull(context)); }
    private SQLiteDatabase read() { requireIo(); return helper.getReadableDatabase(); }
    private SQLiteDatabase write() { requireIo(); return helper.getWritableDatabase(); }
    private static void requireIo() {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("Local storage requires an IO thread.");
    }
    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty() || value.length() > 256) throw new IllegalArgumentException("Invalid " + field);
        return value;
    }
    private static ContentValues values(String key, String value) { ContentValues v = new ContentValues(); v.put(key,value); return v; }
    private static void replace(SQLiteDatabase db, String table, ContentValues values) {
        if (db.insertWithOnConflict(table,null,values,SQLiteDatabase.CONFLICT_REPLACE) == -1) throw new SQLiteException("Local data could not be saved.");
    }

    @Override public List<ActivityRecord> activities() {
        SQLiteDatabase db=read();
        List<ActivityRecord> out=new ArrayList<>();
        try(Cursor cursor=db.rawQuery("SELECT a.payload, n.provider, n.notes, n.rpe, n.shoe_id FROM activities a LEFT JOIN activity_annotations n ON a.provider=n.provider AND a.record_id=n.record_id ORDER BY a.start_millis DESC, a.id",null)) {
            while(cursor.moveToNext()) {
                ActivityRecord a=FeatureJson.activity(FeatureJson.object(cursor.getString(0)));
                if(!cursor.isNull(1)) a=annotated(a,cursor.getString(2),cursor.isNull(3)?null:cursor.getDouble(3),cursor.getString(4));
                out.add(a);
            }
        }
        return out;
    }
    private static ActivityRecord annotated(ActivityRecord a,String notes,Double rpe,String shoe) {
        return new ActivityRecord(a.id(),a.source(),a.start(),a.end(),a.startOffset(),a.endOffset(),a.distanceMetres(),a.movingTime(),a.averageHeartRate(),a.maxHeartRate(),a.elevationGainMetres(),a.cadence(),rpe,notes,a.samples(),a.laps(),a.plannedWorkoutId(),shoe);
    }
    public void upsertManualActivity(ActivityRecord activity) {
        if(!isUserActivity(activity)) throw new IllegalArgumentException("Use the import boundary for provider activities.");
        SQLiteDatabase db=write(); db.beginTransaction();
        try { putActivity(db,activity,true); db.setTransactionSuccessful(); } finally {db.endTransaction();}
    }
    @Override public void applyImport(String provider,List<ActivityRecord> upserts,List<String> deletedRecordIds,String nextToken) {
        required(provider,"provider"); Objects.requireNonNull(upserts); Objects.requireNonNull(deletedRecordIds);
        if(provider.equals("manual") || provider.equals("assessment")) throw new IllegalArgumentException("Manual activities do not use provider imports.");
        for(ActivityRecord a:upserts) if(!a.source().provider().equals(provider)) throw new IllegalArgumentException("Import source does not match provider.");
        SQLiteDatabase db=write(); db.beginTransaction();
        try {
            for(String recordId:deletedRecordIds) {
                String[] source={provider,required(recordId,"source record ID")};
                db.delete("preserved_best_efforts","provider=? AND record_id=?",source);
                db.delete("activities","provider=? AND record_id=?",source);
            }
            for(ActivityRecord a:upserts) putActivity(db,a,false);
            ContentValues checkpoint=values("provider",provider); checkpoint.put("token",nextToken); checkpoint.put("updated_at",Instant.now().toString());
            replace(db,"import_checkpoints",checkpoint);
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    private static void putActivity(SQLiteDatabase db,ActivityRecord a,boolean userOwned) {
        putActivity(db,a,userOwned,true);
    }
    private static void putActivity(SQLiteDatabase db,ActivityRecord a,boolean userOwned,boolean assignDefault) {
        required(a.id(),"activity ID");required(a.source().provider(),"activity provider");required(a.source().recordId(),"source record ID");
        boolean existed=false;
        try(Cursor existing=db.rawQuery("SELECT id,provider,record_id FROM activities WHERE id=? OR (provider=? AND record_id=?)",new String[]{a.id(),a.source().provider(),a.source().recordId()})) {
            while(existing.moveToNext()) {existed=true;if(!existing.getString(0).equals(a.id()) || !existing.getString(1).equals(a.source().provider()) || !existing.getString(2).equals(a.source().recordId())) throw new IllegalArgumentException("Activity identity conflicts with existing data.");}
        }
        ContentValues v=values("id",a.id()); v.put("provider",a.source().provider());v.put("record_id",a.source().recordId());v.put("origin_package",a.source().originPackage());v.put("start_millis",a.start().toEpochMilli());v.put("end_millis",a.end().toEpochMilli());v.put("payload",FeatureJson.activity(a).toString());
        if(db.update("activities",v,"id=?",new String[]{a.id()})==0) db.insertOrThrow("activities",null,v);
        if(userOwned) {
            String shoe=a.shoeId();if(!existed&&assignDefault&&shoe==null)shoe=defaultShoe(db);
            putAnnotation(db,a.source().provider(),a.source().recordId(),a.notes(),a.rpe(),shoe);
        }
        else {
            try(Cursor annotation=db.rawQuery("SELECT 1 FROM activity_annotations WHERE provider=? AND record_id=?",new String[]{a.source().provider(),a.source().recordId()})) {
                if(!annotation.moveToFirst()) {
                    String shoe=assignDefault?defaultShoe(db):null;
                    putAnnotation(db,a.source().provider(),a.source().recordId(),null,null,shoe);
                }
            }
        }
        db.delete("best_efforts","activity_id=?",new String[]{a.id()});
        db.delete("preserved_best_efforts","activity_id=? OR (provider=? AND record_id=?)",new String[]{a.id(),a.source().provider(),a.source().recordId()});
        for(RecordedResult result:BestEffortCalculator.qualifiedResults(a)) putResult(db,result,true);
    }
    private static String defaultShoe(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT id FROM shoes WHERE is_default=1",null)){return c.moveToFirst()?c.getString(0):null;}}
    public void deleteActivity(String id) {
        SQLiteDatabase db=write(); db.beginTransaction();
        try {
            try(Cursor source=db.rawQuery("SELECT provider,record_id FROM activities WHERE id=?",new String[]{required(id,"activity ID")})) {
                if(source.moveToFirst()) db.delete("activity_annotations","provider=? AND record_id=?",new String[]{source.getString(0),source.getString(1)});
            }
            db.delete("preserved_best_efforts","activity_id=?",new String[]{id});
            db.delete("activities","id=?",new String[]{id}); db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    @Override public String importToken(String provider) {
        try(Cursor c=read().rawQuery("SELECT token FROM import_checkpoints WHERE provider=?",new String[]{required(provider,"provider")})) {return c.moveToFirst()?c.getString(0):null;}
    }
    public void saveActivityNotes(String id,String notes,Double rpe) { updateAnnotation(id,notes,rpe,null,false); }
    public void assignShoe(String id,String shoeId) {updateAnnotation(id,null,null,shoeId,true);}
    private void updateAnnotation(String id,String notes,Double rpe,String shoe,boolean changeShoe) {
        SQLiteDatabase db=write(); db.beginTransaction();
        try(Cursor c=db.rawQuery("SELECT provider,record_id FROM activities WHERE id=?",new String[]{required(id,"activity ID")})) {
            if(!c.moveToFirst()) throw new IllegalArgumentException("Activity no longer exists.");
            String provider=c.getString(0),record=c.getString(1),oldNotes="",oldShoe=null;Double oldRpe=null;
            try(Cursor old=db.rawQuery("SELECT notes,rpe,shoe_id FROM activity_annotations WHERE provider=? AND record_id=?",new String[]{provider,record})) {if(old.moveToFirst()){oldNotes=old.getString(0);oldRpe=old.isNull(1)?null:old.getDouble(1);oldShoe=old.getString(2);}}
            putAnnotation(db,provider,record,changeShoe?oldNotes:notes,changeShoe?oldRpe:rpe,changeShoe?shoe:oldShoe);
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    private static void putAnnotation(SQLiteDatabase db,String provider,String record,String notes,Double rpe,String shoe) {
        if(rpe!=null && (!Double.isFinite(rpe)||rpe<0||rpe>10)) throw new IllegalArgumentException("RPE must be between 0 and 10.");
        ContentValues v=values("provider",required(provider,"annotation provider"));v.put("record_id",required(record,"annotation record ID"));v.put("notes",notes==null?"":notes);if(rpe==null)v.putNull("rpe");else v.put("rpe",rpe);v.put("shoe_id",shoe);replace(db,"activity_annotations",v);
    }

    public List<RecordedResult> results() {
        SQLiteDatabase db=read();List<RecordedResult> out=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(String table:new String[]{"manual_results","best_efforts","preserved_best_efforts"}) try(Cursor c=db.rawQuery("SELECT id,payload FROM "+table+" ORDER BY id",null)){while(c.moveToNext())if(ids.add(c.getString(0)))out.add(FeatureJson.result(FeatureJson.object(c.getString(1))));}
        return out;
    }
    public void saveResult(RecordedResult result) {
        if(result.source()==ResultSource.ROLLING) throw new IllegalArgumentException("Rolling results must be calculated from retained activity samples.");
        SQLiteDatabase db=write();
        db.beginTransaction();
        try {
            Map<String,Double> catalog=distanceCatalog();
            for(String table:new String[]{"manual_results","best_efforts","preserved_best_efforts"})try(Cursor c=db.rawQuery("SELECT id,payload FROM "+table,null)){
                while(c.moveToNext()) {
                    if(c.getString(0).equals(result.id())) {
                        if(!table.equals("manual_results"))throw new IllegalArgumentException("A manual result cannot replace a derived effort identity.");
                    } else registerDistance(catalog,FeatureJson.result(FeatureJson.object(c.getString(1))).distance());
                }
            }
            registerDistance(catalog,result.distance());
            putResult(db,result,false);
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    private static void putResult(SQLiteDatabase db,RecordedResult result,boolean rolling) {
        required(result.id(),"result ID");required(result.distance().label(),"distance label");
        ContentValues v=values("id",result.id());v.put("activity_id",result.activityId());v.put("distance_code",result.distance().code());v.put("payload",FeatureJson.result(result).toString());
        if(!rolling){v.put("distance_metres",result.distance().metres());v.put("result_date",result.date().toString());}
        replace(db,rolling?"best_efforts":"manual_results",v);
    }
    public void deleteResult(String id) {write().delete("manual_results","id=?",new String[]{required(id,"result ID")});}
    public List<Shoe> shoes() {
        List<Shoe> out=new ArrayList<>();
        try(Cursor c=read().rawQuery("SELECT payload,is_default FROM shoes ORDER BY is_default DESC,id",null)) {while(c.moveToNext()){Shoe s=FeatureJson.shoe(FeatureJson.object(c.getString(0)));out.add(new Shoe(s.id(),s.brand(),s.model(),s.purchaseDate(),s.startingDistanceMetres(),s.retired(),s.notes(),c.getInt(1)==1));}}
        return out;
    }
    public void saveShoe(Shoe shoe) {
        SQLiteDatabase db=write();db.beginTransaction();try{putShoe(db,shoe);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    private static void putShoe(SQLiteDatabase db,Shoe shoe) {
        required(shoe.id(),"shoe ID");
        if(shoe.defaultShoe()){ContentValues unset=new ContentValues();unset.put("is_default",0);db.update("shoes",unset,null,null);}
        ContentValues v=values("id",shoe.id());v.put("payload",FeatureJson.shoe(shoe).toString());v.put("is_default",shoe.defaultShoe()?1:0);
        // UPDATE preserves foreign-key shoe assignments; REPLACE would delete the old row.
        if(db.update("shoes",v,"id=?",new String[]{shoe.id()})==0)db.insertOrThrow("shoes",null,v);
    }
    public ManualBaselineInput savedManualBaseline() {
        try(Cursor c=read().rawQuery("SELECT manual_json FROM baseline_state WHERE id=1",null)){return c.moveToFirst()?FeatureJson.baseline(FeatureJson.object(c.getString(0))):ManualBaselineInput.empty();}
    }
    public Instant manualBaselineRecordedAt() {
        try(Cursor c=read().rawQuery("SELECT manual_recorded_at FROM baseline_state WHERE id=1",null)){return c.moveToFirst()&&!c.isNull(0)?Instant.parse(c.getString(0)):null;}
    }
    public void saveManualBaseline(ManualBaselineInput baseline) {putBaseline(write(),baseline,Instant.now());}
    private static void putBaseline(SQLiteDatabase db,ManualBaselineInput baseline,Instant recordedAt) {
        ContentValues v=new ContentValues();v.put("id",1);v.put("manual_json",FeatureJson.baseline(Objects.requireNonNull(baseline)).toString());v.put("manual_recorded_at",recordedAt==null?null:recordedAt.toString());v.put("reviewed",1);replace(db,"baseline_state",v);
    }
    public String getSetting(String key) {try(Cursor c=read().rawQuery("SELECT setting_value FROM settings WHERE setting_key=?",new String[]{required(key,"setting key")})){return c.moveToFirst()?c.getString(0):null;}}
    public void putSetting(String key,String value) {putSetting(write(),key,value);}
    private static void putSetting(SQLiteDatabase db,String key,String value){ContentValues v=values("setting_key",required(key,"setting key"));v.put("setting_value",Objects.requireNonNull(value));replace(db,"settings",v);}
    public void removeSetting(String key){write().delete("settings","setting_key=?",new String[]{required(key,"setting key")});}

    public List<PlannedWorkout> plannedWorkouts() {
        List<PlannedWorkout> out=new ArrayList<>();try(Cursor c=read().rawQuery("SELECT payload FROM planned_workouts ORDER BY scheduled_date,id",null)){while(c.moveToNext())out.add(FeatureJson.planned(FeatureJson.object(c.getString(0))));}return out;
    }
    public long plannedWorkoutRevision(String id){try(Cursor c=read().rawQuery("SELECT revision FROM planned_workouts WHERE id=?",new String[]{id})){return c.moveToFirst()?c.getLong(0):0;}}
    public void savePlannedWorkout(PlannedWorkout workout){SQLiteDatabase db=write();db.beginTransaction();try{long revision=plannedWorkoutRevision(workout.id())+1;putPlanned(db,workout,revision);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static void putPlanned(SQLiteDatabase db,PlannedWorkout workout,long revision){ContentValues v=values("id",required(workout.id(),"workout ID"));v.put("scheduled_date",workout.date().toString());v.put("revision",revision);v.put("payload",FeatureJson.planned(workout).toString());replace(db,"planned_workouts",v);}
    public void deletePlannedWorkout(String id){write().delete("planned_workouts","id=?",new String[]{required(id,"workout ID")});}
    public void recordAssessment(JSONObject assessment){saveAssessment(FeatureJson.assessment(assessment));}
    public List<AssessmentResult> assessments(){List<AssessmentResult> out=new ArrayList<>();try(Cursor c=read().rawQuery("SELECT payload FROM assessment_results ORDER BY id",null)){while(c.moveToNext())out.add(FeatureJson.assessment(FeatureJson.object(c.getString(0))));}return out;}
    public void saveAssessment(AssessmentResult assessment){putOpaque(write(),"assessment_results",FeatureJson.assessment(assessment));}
    /** A completed assessment and its actual activity are committed together. */
    public void saveAssessment(ActivityRecord activity,AssessmentResult assessment){
        if(!isUserActivity(activity)||!activity.id().equals(assessment.activityId()))throw new IllegalArgumentException("Assessment must link its recorded manual activity.");
        SQLiteDatabase db=write();db.beginTransaction();try{putActivity(db,activity,true);putOpaque(db,"assessment_results",FeatureJson.assessment(assessment));db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    private static void putOpaque(SQLiteDatabase db,String table,JSONObject object){try{ContentValues v=values("id",FeatureJson.required(object,"id"));v.put("payload",object.toString());if(table.equals("assessment_results"))v.put("activity_id",FeatureJson.text(object,"activityId"));if(table.equals("plan_adjustments")){v.put("plan_id",FeatureJson.text(object,"planId"));v.put("user_decision",FeatureJson.text(object,"userDecision"));}replace(db,table,v);}catch(JSONException e){throw FeatureJson.invalid(e);}}

    /** Exports app-owned information only. Imported measurements can be re-imported with consent. */
    public JSONObject exportUserData() {
        SQLiteDatabase db=read();db.beginTransactionNonExclusive();
        try {
            UserProfile profile=SqliteUserPreferencesRepository.readProfile(db).orElse(null);
            JSONObject out=FeatureJson.json("storeVersion",EXPORT_STORE_VERSION,"profile",profile==null?null:FeatureJson.profile(profile),"draft",null,"manualBaseline",null,"manualBaselineRecordedAt",null);
            try(Cursor c=db.rawQuery("SELECT payload FROM onboarding_draft WHERE id=1",null)){if(c.moveToFirst())out.put("draft",c.getString(0));}
            try(Cursor c=db.rawQuery("SELECT manual_json,manual_recorded_at FROM baseline_state WHERE id=1",null)){if(c.moveToFirst()){out.put("manualBaseline",FeatureJson.object(c.getString(0)));out.put("manualBaselineRecordedAt",c.isNull(1)?JSONObject.NULL:c.getString(1));}}
            JSONArray activities=new JSONArray();for(ActivityRecord a:activities())if(isUserActivity(a))activities.put(FeatureJson.activity(a));out.put("activities",activities);
            JSONArray shoes=new JSONArray();for(Shoe shoe:shoes())shoes.put(FeatureJson.shoe(shoe));out.put("shoes",shoes);
            JSONArray results=new JSONArray();for(RecordedResult result:results())if(result.source()!=ResultSource.ROLLING)results.put(FeatureJson.result(result));out.put("results",results);
            JSONArray bestEfforts=new JSONArray();Set<String> effortIds=new HashSet<>();
            try(Cursor c=db.rawQuery("SELECT b.id,b.payload,a.provider,a.record_id,a.origin_package FROM best_efforts b JOIN activities a ON b.activity_id=a.id ORDER BY b.id",null)){while(c.moveToNext()){effortIds.add(c.getString(0));bestEfforts.put(FeatureJson.json("result",FeatureJson.object(c.getString(1)),"source",FeatureJson.json("provider",c.getString(2),"recordId",c.getString(3),"originPackage",c.getString(4))));}}
            try(Cursor c=db.rawQuery("SELECT id,payload,provider,record_id,origin_package FROM preserved_best_efforts ORDER BY id",null)){while(c.moveToNext())if(effortIds.add(c.getString(0)))bestEfforts.put(FeatureJson.json("result",FeatureJson.object(c.getString(1)),"source",FeatureJson.json("provider",c.getString(2),"recordId",c.getString(3),"originPackage",c.getString(4))));}out.put("bestEfforts",bestEfforts);
            JSONArray annotations=new JSONArray();try(Cursor c=db.rawQuery("SELECT provider,record_id,notes,rpe,shoe_id FROM activity_annotations ORDER BY provider,record_id",null)){while(c.moveToNext())annotations.put(FeatureJson.json("provider",c.getString(0),"recordId",c.getString(1),"notes",c.getString(2),"rpe",c.isNull(3)?null:c.getDouble(3),"shoeId",c.getString(4)));}out.put("annotations",annotations);
            JSONObject settings=new JSONObject();try(Cursor c=db.rawQuery("SELECT setting_key,setting_value FROM settings ORDER BY setting_key",null)){while(c.moveToNext())settings.put(c.getString(0),c.getString(1));}out.put("settings",settings);
            JSONArray plans=new JSONArray();try(Cursor c=db.rawQuery("SELECT payload,revision FROM planned_workouts ORDER BY id",null)){while(c.moveToNext())plans.put(FeatureJson.json("workout",FeatureJson.object(c.getString(0)),"revision",c.getLong(1)));}out.put("plannedWorkouts",plans);
            out.put("assessments",opaqueRows(db,"assessment_results"));out.put("trainingPlans",opaqueRows(db,"training_plans"));out.put("planAdjustments",opaqueRows(db,"plan_adjustments"));
            db.setTransactionSuccessful();return out;
        } catch(JSONException e){throw FeatureJson.invalid(e);}finally{db.endTransaction();}
    }
    private static JSONArray opaqueRows(SQLiteDatabase db,String table){JSONArray out=new JSONArray();try(Cursor c=db.rawQuery("SELECT payload FROM "+table+" ORDER BY id",null)){while(c.moveToNext())out.put(FeatureJson.object(c.getString(0)));}return out;}
    private static boolean isUserActivity(ActivityRecord a){return a.source().provider().equals("manual")||a.source().provider().equals("assessment");}

    /** Validation is public for backup previews; it never reads or changes the database. */
    public static void validateUserData(JSONObject data){validated(data);}
    private record RestoreData(UserProfile profile,String draft,ManualBaselineInput baseline,Instant baselineAt,List<ActivityRecord> activities,List<Shoe> shoes,List<RecordedResult> results,JSONArray bestEfforts,JSONArray annotations,JSONObject settings,JSONArray plans,JSONArray assessments,JSONArray trainingPlans,JSONArray adjustments){}
    private record SourceKey(String provider,String recordId){}
    private static RestoreData validated(JSONObject data){
        try {
            if(FeatureJson.requiredInteger(data,"storeVersion")!=EXPORT_STORE_VERSION)throw new IllegalArgumentException("Unsupported backup store version.");
            UserProfile profile=data.isNull("profile")?null:FeatureJson.profile(data.getJSONObject("profile"));
            String draft=FeatureJson.text(data,"draft");if(draft!=null)FeatureJson.object(draft);
            ManualBaselineInput baseline=data.isNull("manualBaseline")?null:FeatureJson.baseline(data.getJSONObject("manualBaseline"));
            Instant baselineAt=data.isNull("manualBaselineRecordedAt")?null:Instant.parse(data.getString("manualBaselineRecordedAt"));
            List<Shoe> shoes=new ArrayList<>();Set<String> shoeIds=new HashSet<>();int defaults=0;
            JSONArray shoeRows=array(data,"shoes");for(int i=0;i<shoeRows.length();i++){Shoe s=FeatureJson.shoe(shoeRows.getJSONObject(i));unique(shoeIds,s.id());if(s.defaultShoe())defaults++;shoes.add(s);}if(defaults>1)throw new IllegalArgumentException("A backup has more than one default shoe.");
            List<ActivityRecord> activities=new ArrayList<>();Set<String> ids=new HashSet<>();Set<SourceKey> sources=new HashSet<>();Map<String,SourceIdentity> activitySources=new HashMap<>();JSONArray rows=array(data,"activities");
            for(int i=0;i<rows.length();i++){ActivityRecord a=FeatureJson.activity(rows.getJSONObject(i));if(!isUserActivity(a))throw new IllegalArgumentException("Backup contains external health measurements.");unique(ids,a.id());unique(sources,new SourceKey(a.source().provider(),a.source().recordId()));activitySources.put(a.id(),a.source());if(a.shoeId()!=null&&!shoeIds.contains(a.shoeId()))throw new IllegalArgumentException("Activity shoe is missing.");activities.add(a);}
            Map<String,Double> catalog=distanceCatalog();
            List<RecordedResult> results=new ArrayList<>();Set<String> resultIds=new HashSet<>();JSONArray resultRows=array(data,"results");for(int i=0;i<resultRows.length();i++){RecordedResult r=FeatureJson.result(resultRows.getJSONObject(i));if(r.source()==ResultSource.ROLLING)throw new IllegalArgumentException("Derived efforts must be recalculated from retained samples.");unique(resultIds,r.id());registerDistance(catalog,r.distance());results.add(r);}
            JSONArray bestEfforts=array(data,"bestEfforts");Set<String> effortPairs=new HashSet<>();
            for(int i=0;i<bestEfforts.length();i++){JSONObject e=bestEfforts.getJSONObject(i);RecordedResult r=FeatureJson.result(e.getJSONObject("result"));if(r.source()!=ResultSource.ROLLING)throw new IllegalArgumentException("An archived effort requires rolling provenance.");unique(resultIds,r.id());unique(effortPairs,r.activityId()+"\n"+r.distance().code());registerDistance(catalog,r.distance());JSONObject source=e.getJSONObject("source");SourceIdentity identity=new SourceIdentity(FeatureJson.required(source,"provider"),FeatureJson.required(source,"recordId"),FeatureJson.text(source,"originPackage"));SourceIdentity prior=activitySources.put(r.activityId(),identity);if(prior!=null&&!prior.equals(identity))throw new IllegalArgumentException("Archived efforts disagree about their activity source.");}
            JSONArray annotations=array(data,"annotations");Set<SourceKey> annotationIds=new HashSet<>();for(int i=0;i<annotations.length();i++){JSONObject a=annotations.getJSONObject(i);String provider=FeatureJson.required(a,"provider"),record=FeatureJson.required(a,"recordId");unique(annotationIds,new SourceKey(provider,record));if(FeatureJson.text(a,"notes")==null)throw new IllegalArgumentException("Annotation notes must be text.");Double rpe=FeatureJson.number(a,"rpe");if(rpe!=null&&(rpe<0||rpe>10))throw new IllegalArgumentException("Invalid RPE.");String shoe=FeatureJson.text(a,"shoeId");if(shoe!=null&&!shoeIds.contains(shoe))throw new IllegalArgumentException("Annotation shoe is missing.");}
            JSONObject settings=data.getJSONObject("settings");for(Iterator<String> it=settings.keys();it.hasNext();){String key=it.next();required(key,"setting key");if(!(settings.get(key) instanceof String))throw new IllegalArgumentException("Setting values must be text.");}
            JSONArray plans=array(data,"plannedWorkouts");Set<String> planIds=new HashSet<>();for(int i=0;i<plans.length();i++){JSONObject row=plans.getJSONObject(i);PlannedWorkout w=FeatureJson.planned(row.getJSONObject("workout"));unique(planIds,w.id());if(FeatureJson.requiredLong(row,"revision")<1)throw new IllegalArgumentException("Invalid workout revision.");}
            JSONArray assessments=array(data,"assessments"),trainingPlans=array(data,"trainingPlans"),adjustments=array(data,"planAdjustments");validateOpaque(assessments);for(int i=0;i<assessments.length();i++)FeatureJson.assessment(assessments.getJSONObject(i));validateOpaque(trainingPlans);validateOpaque(adjustments);
            return new RestoreData(profile,draft,baseline,baselineAt,activities,shoes,results,bestEfforts,annotations,settings,plans,assessments,trainingPlans,adjustments);
        }catch(JSONException | java.time.DateTimeException e){throw FeatureJson.invalid(e);}
    }
    private static JSONArray array(JSONObject data,String key)throws JSONException{JSONArray rows=data.getJSONArray(key);if(rows.length()>100_000)throw new IllegalArgumentException("Too many "+key+" records.");return rows;}
    private static <T> void unique(Set<T> ids,T id){if(!ids.add(id))throw new IllegalArgumentException("Duplicate backup identity: "+id);}
    private static Map<String,Double> distanceCatalog(){Map<String,Double> catalog=new HashMap<>();for(PerformanceDistance distance:PerformanceDistance.CORE)registerDistance(catalog,distance);return catalog;}
    private static void registerDistance(Map<String,Double> catalog,PerformanceDistance distance){Double previous=catalog.put(distance.code(),distance.metres());if(previous!=null&&Math.abs(previous-distance.metres())>0.000001)throw new IllegalArgumentException("One distance code cannot refer to different distances.");}
    private static void validateOpaque(JSONArray rows)throws JSONException{Set<String> ids=new HashSet<>();for(int i=0;i<rows.length();i++)unique(ids,FeatureJson.required(rows.getJSONObject(i),"id"));}
    public void restoreUserData(JSONObject data,boolean merge){
        if(merge)throw new IllegalArgumentException("Merge restore is not supported; use validated replacement.");
        RestoreData snapshot=validated(FeatureJson.object(Objects.requireNonNull(data).toString()));
        SQLiteDatabase db=write();db.beginTransaction();
        try {
            for(String table:new String[]{"best_efforts","preserved_best_efforts","activity_annotations","manual_results","activities","shoes","import_checkpoints","baseline_state","settings","assessment_results","planned_workouts","training_plans","plan_adjustments","onboarding_draft","user_profile"})db.delete(table,null,null);
            if(snapshot.profile()!=null)SqliteUserPreferencesRepository.writeProfile(db,snapshot.profile());
            if(snapshot.draft()!=null){ContentValues v=values("payload",snapshot.draft());v.put("id",1);db.insertOrThrow("onboarding_draft",null,v);}
            for(Shoe s:snapshot.shoes())putShoe(db,s);
            for(ActivityRecord a:snapshot.activities())putActivity(db,a,true,false);
            for(RecordedResult r:snapshot.results())putResult(db,r,false);
            for(int i=0;i<snapshot.bestEfforts().length();i++){
                JSONObject e=snapshot.bestEfforts().getJSONObject(i),source=e.getJSONObject("source");RecordedResult r=FeatureJson.result(e.getJSONObject("result"));
                ContentValues v=values("id",r.id());v.put("activity_id",r.activityId());v.put("distance_code",r.distance().code());v.put("provider",source.getString("provider"));v.put("record_id",source.getString("recordId"));v.put("origin_package",FeatureJson.text(source,"originPackage"));v.put("payload",FeatureJson.result(r).toString());replace(db,"preserved_best_efforts",v);
            }
            for(int i=0;i<snapshot.annotations().length();i++){JSONObject a=snapshot.annotations().getJSONObject(i);putAnnotation(db,a.getString("provider"),a.getString("recordId"),a.getString("notes"),FeatureJson.number(a,"rpe"),FeatureJson.text(a,"shoeId"));}
            if(snapshot.baseline()!=null)putBaseline(db,snapshot.baseline(),snapshot.baselineAt());
            for(Iterator<String> it=snapshot.settings().keys();it.hasNext();){String key=it.next();putSetting(db,key,snapshot.settings().getString(key));}
            for(int i=0;i<snapshot.plans().length();i++){JSONObject p=snapshot.plans().getJSONObject(i);putPlanned(db,FeatureJson.planned(p.getJSONObject("workout")),FeatureJson.requiredLong(p,"revision"));}
            for(int i=0;i<snapshot.assessments().length();i++)putOpaque(db,"assessment_results",snapshot.assessments().getJSONObject(i));
            for(int i=0;i<snapshot.trainingPlans().length();i++)putOpaque(db,"training_plans",snapshot.trainingPlans().getJSONObject(i));
            for(int i=0;i<snapshot.adjustments().length();i++)putOpaque(db,"plan_adjustments",snapshot.adjustments().getJSONObject(i));
            db.setTransactionSuccessful();
        }catch(JSONException e){throw FeatureJson.invalid(e);}finally{db.endTransaction();}
    }
    @Override public void close(){requireIo();helper.close();}
}
