package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.racedaydreams.domain.activity.PerformanceDistance;
import org.racedaydreams.domain.activity.RecordedResult;
import org.racedaydreams.domain.activity.ResultRepository;
import org.racedaydreams.domain.activity.ResultSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** SQLite adapter for result data. Shares the application database and its transaction boundary. */
public final class SqliteResultRepository extends SqliteStoreAccess implements ResultRepository {
    public SqliteResultRepository(Context context) { super(context); }
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
    static void putResult(SQLiteDatabase db,RecordedResult result,boolean rolling) {
        required(result.id(),"result ID");required(result.distance().label(),"distance label");
        ContentValues v=values("id",result.id());v.put("activity_id",result.activityId());v.put("distance_code",result.distance().code());v.put("payload",FeatureJson.result(result).toString());
        if(!rolling){v.put("distance_metres",result.distance().metres());v.put("result_date",result.date().toString());}
        replace(db,rolling?"best_efforts":"manual_results",v);
    }
    public void deleteResult(String id) {write().delete("manual_results","id=?",new String[]{required(id,"result ID")});}
    static Map<String,Double> distanceCatalog(){Map<String,Double> catalog=new HashMap<>();for(PerformanceDistance distance:PerformanceDistance.CORE)registerDistance(catalog,distance);return catalog;}
    static void registerDistance(Map<String,Double> catalog,PerformanceDistance distance){Double previous=catalog.put(distance.code(),distance.metres());if(previous!=null&&Math.abs(previous-distance.metres())>0.000001)throw new IllegalArgumentException("One distance code cannot refer to different distances.");}

}
