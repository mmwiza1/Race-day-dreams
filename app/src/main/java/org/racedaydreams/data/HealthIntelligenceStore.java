package org.racedaydreams.data;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.racedaydreams.domain.health.HealthIntelligence.*;
import java.time.*;
import java.util.*;
/** Provider-only cache; staged authorized windows replace atomically, denied metrics remain untouched. */
public final class HealthIntelligenceStore {
    private final ProfileDatabase helper;
    public HealthIntelligenceStore(Context context){helper=ProfileDatabase.get(context);}
    public void replace(Set<Metric> metrics,LocalDate from,LocalDate until,List<Observation> rows){
        for(Observation x:rows)if(!metrics.contains(x.metric())||x.date().isBefore(from)||x.date().isAfter(until))throw new IllegalArgumentException("Outside authorized window");
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();try{
            for(Metric m:metrics)db.delete("health_observations","metric=? AND day>=? AND day<=?",new String[]{m.name(),from.toString(),until.toString()});
            for(Observation x:rows){ContentValues v=new ContentValues();v.put("id",x.id());v.put("metric",x.metric().name());v.put("day",x.date().toString());v.put("start_time",x.start().toString());v.put("end_time",x.end().toString());v.put("value",x.value());v.put("source",x.source());v.put("imported_at",x.importedAt().toString());db.insertOrThrow("health_observations",null,v);}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public List<Observation> recent(LocalDate today){List<Observation> out=new ArrayList<>();try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT id,metric,day,start_time,end_time,value,source,imported_at FROM health_observations WHERE day>=? AND day<=? ORDER BY start_time DESC",new String[]{today.minusDays(34).toString(),today.toString()})){while(c.moveToNext())out.add(new Observation(c.getString(0),Metric.valueOf(c.getString(1)),LocalDate.parse(c.getString(2)),Instant.parse(c.getString(3)),Instant.parse(c.getString(4)),c.getDouble(5),c.getString(6),Instant.parse(c.getString(7))));}return out;}
}
