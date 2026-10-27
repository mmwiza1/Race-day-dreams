package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import org.racedaydreams.domain.baseline.BaselineRepository;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.baseline.ManualBaselineInput;
import org.racedaydreams.domain.baseline.AssessmentResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SQLite adapter for baseline data. Shares the application database and its transaction boundary. */
public final class SqliteBaselineRepository extends SqliteStoreAccess implements BaselineRepository {
    public SqliteBaselineRepository(Context context) { super(context); }
    public ManualBaselineInput savedManualBaseline() {
        try(Cursor c=read().rawQuery("SELECT manual_json FROM baseline_state WHERE id=1",null)){return c.moveToFirst()?FeatureJson.baseline(FeatureJson.object(c.getString(0))):ManualBaselineInput.empty();}
    }
    public Instant manualBaselineRecordedAt() {
        try(Cursor c=read().rawQuery("SELECT manual_recorded_at FROM baseline_state WHERE id=1",null)){return c.moveToFirst()&&!c.isNull(0)?Instant.parse(c.getString(0)):null;}
    }
    public void saveManualBaseline(ManualBaselineInput baseline) {putBaseline(write(),baseline,Instant.now());}
    static void putBaseline(SQLiteDatabase db,ManualBaselineInput baseline,Instant recordedAt) {
        ContentValues v=new ContentValues();v.put("id",1);v.put("manual_json",FeatureJson.baseline(Objects.requireNonNull(baseline)).toString());v.put("manual_recorded_at",recordedAt==null?null:recordedAt.toString());v.put("reviewed",1);replace(db,"baseline_state",v);
    }
    public void recordAssessment(JSONObject assessment){saveAssessment(FeatureJson.assessment(assessment));}
    public List<AssessmentResult> assessments(){List<AssessmentResult> out=new ArrayList<>();try(Cursor c=read().rawQuery("SELECT payload FROM assessment_results ORDER BY id",null)){while(c.moveToNext())out.add(FeatureJson.assessment(FeatureJson.object(c.getString(0))));}return out;}
    public void saveAssessment(AssessmentResult assessment){putOpaque(write(),"assessment_results",FeatureJson.assessment(assessment));}
    /** A completed assessment and its actual activity are committed together. */
    public void saveAssessment(ActivityRecord activity,AssessmentResult assessment){
        if(!SqliteActivityRepository.isUserActivity(activity)||!activity.id().equals(assessment.activityId()))throw new IllegalArgumentException("Assessment must link its recorded manual activity.");
        SQLiteDatabase db=write();db.beginTransaction();try{SqliteActivityRepository.putActivity(db,activity,true);putOpaque(db,"assessment_results",FeatureJson.assessment(assessment));db.setTransactionSuccessful();}finally{db.endTransaction();}
    }

}
