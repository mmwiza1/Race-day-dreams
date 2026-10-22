package org.racedaydreams.data;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import java.util.*;
import org.json.*;
/** Small, typed user choices shared by UI and backup; never rewrites workout evidence. */
public final class StrengthUserStore {
    private final ProfileDatabase helper;
    public StrengthUserStore(Context c){helper=ProfileDatabase.get(c);}
    private SQLiteDatabase db(){if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Exercise choices require IO.");return helper.getWritableDatabase();}
    public record Choice(String exerciseId,boolean favourite,boolean archived){}
    public List<Choice> choices(){var out=new ArrayList<Choice>();try(Cursor c=db().rawQuery("SELECT exercise_id,favourite,archived FROM strength_user_state ORDER BY exercise_id",null)){while(c.moveToNext())out.add(new Choice(c.getString(0),c.getInt(1)==1,c.getInt(2)==1));}return Collections.unmodifiableList(out);}
    public void save(Choice c){if(c.exerciseId()==null||c.exerciseId().isEmpty()||c.exerciseId().length()>200)throw new IllegalArgumentException("Invalid exercise ID.");ContentValues v=new ContentValues();v.put("exercise_id",c.exerciseId());v.put("favourite",c.favourite()?1:0);v.put("archived",c.archived()?1:0);if(db().update("strength_user_state",v,"exercise_id=?",new String[]{c.exerciseId()})==0)db().insertOrThrow("strength_user_state",null,v);}
    public Set<String> acknowledged(){var out=new HashSet<String>();try(Cursor c=db().rawQuery("SELECT id FROM pb_acknowledgements",null)){while(c.moveToNext())out.add(c.getString(0));}return Collections.unmodifiableSet(out);}
    public void acknowledge(String id){if(id==null||id.isEmpty()||id.length()>500)throw new IllegalArgumentException("Invalid PB ID.");ContentValues v=new ContentValues();v.put("id",id);db().insertWithOnConflict("pb_acknowledgements",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    static JSONObject exportData(SQLiteDatabase db){try{JSONArray choices=new JSONArray(),acks=new JSONArray();try(Cursor c=db.rawQuery("SELECT exercise_id,favourite,archived FROM strength_user_state ORDER BY exercise_id",null)){while(c.moveToNext())choices.put(new JSONObject().put("id",c.getString(0)).put("favourite",c.getInt(1)==1).put("archived",c.getInt(2)==1));}try(Cursor c=db.rawQuery("SELECT id FROM pb_acknowledgements ORDER BY id",null)){while(c.moveToNext())acks.put(c.getString(0));}return new JSONObject().put("version",1).put("choices",choices).put("acknowledged",acks);}catch(JSONException e){throw FeatureJson.invalid(e);}}
    static void validate(JSONObject data){try{if(FeatureJson.requiredInteger(data,"version")!=1)throw new IllegalArgumentException("Unsupported exercise choices.");JSONArray choices=data.getJSONArray("choices"),acks=data.getJSONArray("acknowledged");if(choices.length()>10000||acks.length()>100000)throw new IllegalArgumentException("Too many exercise choices.");Set<String> ids=new HashSet<>();for(int i=0;i<choices.length();i++){JSONObject c=choices.getJSONObject(i);String id=FeatureJson.required(c,"id");if(id.length()>200||!ids.add(id))throw new IllegalArgumentException("Invalid duplicate exercise choice.");FeatureJson.bool(c,"favourite");FeatureJson.bool(c,"archived");}ids.clear();for(int i=0;i<acks.length();i++){Object raw=acks.get(i);if(!(raw instanceof String id)||id.isEmpty()||id.length()>500||!ids.add(id))throw new IllegalArgumentException("Invalid PB acknowledgement.");}}catch(JSONException e){throw FeatureJson.invalid(e);}}
    static void clear(SQLiteDatabase db){db.delete("strength_user_state",null,null);db.delete("pb_acknowledgements",null,null);}
    static void restore(SQLiteDatabase db,JSONObject data){validate(data);try{JSONArray choices=data.getJSONArray("choices"),acks=data.getJSONArray("acknowledged");for(int i=0;i<choices.length();i++){var c=choices.getJSONObject(i);ContentValues v=new ContentValues();v.put("exercise_id",c.getString("id"));v.put("favourite",c.getBoolean("favourite")?1:0);v.put("archived",c.getBoolean("archived")?1:0);db.insertOrThrow("strength_user_state",null,v);}for(int i=0;i<acks.length();i++){ContentValues v=new ContentValues();v.put("id",acks.getString(i));db.insertOrThrow("pb_acknowledgements",null,v);}}catch(JSONException e){throw FeatureJson.invalid(e);}}
}
