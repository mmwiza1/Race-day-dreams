package org.racedaydreams.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.*
import org.racedaydreams.domain.polish.CustomWorkout
import java.time.Instant

/** Typed, versioned user-owned definitions/preferences, in the existing transaction boundary. */
class Release08Store(context:Context) {
    private val db=ProfileDatabase.get(context).writableDatabase
    fun get(kind:String,id:String):JSONObject?=db.rawQuery("SELECT payload FROM release08 WHERE kind=? AND id=?",arrayOf(kind,id)).use{if(it.moveToFirst())JSONObject(it.getString(0))else null}
    fun save(kind:String,id:String,j:JSONObject)=put(db,kind,id,j)
    fun workouts():List<CustomWorkout> {val out=mutableListOf<CustomWorkout>();db.rawQuery("SELECT payload FROM release08 WHERE kind='custom' ORDER BY id",null).use{while(it.moveToNext())out.add(decode(JSONObject(it.getString(0))))};return out}
    fun save(w:CustomWorkout)=save("custom",w.id(),encode(w))
    fun previous():List<CustomWorkout>{val out=mutableListOf<CustomWorkout>();db.rawQuery("SELECT id,payload FROM coach_support WHERE kind='execution' ORDER BY rowid DESC LIMIT 50",null).use{c->while(c.moveToNext()){val j=JSONObject(c.getString(1));if(j.has("activityId")&&j.optString("mode")!="race")out.add(CustomWorkout(c.getString(0),j.optString("title","Previous structured run"),listOf(CustomWorkout.Group(1,SupportStore.steps(j)))))}};return out}
    fun removeWorkout(id:String){db.delete("release08","kind='custom' AND id=?",arrayOf(id))}
    fun dreamPreferences():Pair<Boolean,Set<String>> {val j=get("config","dream")?:return true to org.racedaydreams.domain.support.DreamRoutes.routes().map{it.id()}.toSet();val a=j.getJSONArray("cities");return j.getBoolean("enabled") to (0 until a.length()).map{a.getString(it)}.toSet()}
    companion object {
    fun snapshot(w:CustomWorkout,mode:String)=JSONObject().put("version",1).put("mode",mode).put("title",w.title()).put("customId",w.id()).put("steps",JSONArray().also{a->w.flattened().forEach{a.put(SupportStore.step(it))}})

        fun encode(w:CustomWorkout):JSONObject=JSONObject().put("version",1).put("id",w.id()).put("title",w.title()).put("groups",JSONArray().also{a->w.groups().forEach{g->a.put(JSONObject().put("repeats",g.repeats()).put("steps",JSONArray().also{b->g.steps().forEach{b.put(SupportStore.step(it))}}))}})
        fun decode(j:JSONObject):CustomWorkout {val a=j.getJSONArray("groups");require(a.length() in 1..500);return CustomWorkout(j.getString("id"),j.getString("title"),(0 until a.length()).map{val g=a.getJSONObject(it);CustomWorkout.Group(g.getInt("repeats"),SupportStore.steps(g))})}
        @JvmStatic fun validateRow(kind:String,j:JSONObject){require(j.get("version") is Int&&j.getInt("version")==1);when(kind){
            "custom"->decode(j)
            "origin"->{require(j.getString("mode") in listOf("free","quick","custom","repeat","workout","race"));Instant.parse(j.getString("at"));j.optString("dreamRoute").takeIf{it.isNotBlank()&&it!="null"}?.let{org.racedaydreams.domain.support.DreamRoutes.forId(it)}}
            "association"->{require(listOf("activity","planVersion","workout").all{j.getString(it).length in 1..256});Instant.parse(j.getString("at"))}
            "checklist"->{require(j.getString("label").length in 1..120);j.getBoolean("checked")}
            "config"->{when(j.getString("type")){"dream"->{j.getBoolean("enabled");val a=j.getJSONArray("cities");require(a.length()<=5);val ids=(0 until a.length()).map{a.getString(it)};require(ids.distinct().size==ids.size&&ids.all{id->org.racedaydreams.domain.support.DreamRoutes.routes().any{it.id()==id}})};"shoe_feedback"->{val a=j.getJSONArray("activities");require(a.length()<=10000);val ids=(0 until a.length()).map{a.getString(it)};require(ids.distinct().size==ids.size&&ids.all{it.length in 1..256})};"schedule"->{require(j.getInt("maximumMinutes") in 15..360);require(j.getString("distribution") in listOf("FEWER_LONGER","BALANCED","MORE_SHORTER"));val a=j.getJSONArray("unavailable");require(a.length()<=366);for(i in 0 until a.length())java.time.LocalDate.parse(a.getString(i))};"start"->require(j.getString("path") in listOf("record","race","hybrid"));else->error("Unsupported preference")}}
            else->error("Unsupported 0.8 record")
        }}
        @JvmStatic fun put(db:SQLiteDatabase,kind:String,id:String,j:JSONObject){require(id.length in 1..256);validateRow(kind,j);if(kind=="custom")require(j.getString("id")==id);if(kind=="association")require(j.getString("activity")==id);if(kind=="config")require(mapOf("dream" to "dream","schedule" to "schedule","shoe-feedback" to "shoe_feedback","start" to "start")[id]==j.getString("type"));val v=ContentValues().apply{put("kind",kind);put("id",id);put("payload",j.toString())};if(db.update("release08",v,"kind=? AND id=?",arrayOf(kind,id))==0)db.insertOrThrow("release08",null,v)}
        @JvmStatic fun exportData(db:SQLiteDatabase):JSONObject{val a=JSONArray();db.rawQuery("SELECT kind,id,payload FROM release08 ORDER BY kind,id",null).use{while(it.moveToNext())a.put(JSONObject().put("kind",it.getString(0)).put("id",it.getString(1)).put("payload",JSONObject(it.getString(2))))};return JSONObject().put("version",1).put("rows",a)}
        @JvmStatic fun validate(j:JSONObject){require(j.getInt("version")==1);val a=j.getJSONArray("rows");require(a.length()<=100000);val keys=mutableSetOf<String>();for(i in 0 until a.length()){val r=a.getJSONObject(i);require(r.getString("id").length in 1..256&&keys.add(r.getString("kind")+":"+r.getString("id")));val kind=r.getString("kind");val payload=r.getJSONObject("payload");validateRow(kind,payload);if(kind=="custom")require(payload.getString("id")==r.getString("id"));if(kind=="association")require(payload.getString("activity")==r.getString("id"));if(kind=="config")require(mapOf("dream" to "dream","schedule" to "schedule","shoe-feedback" to "shoe_feedback","start" to "start")[r.getString("id")]==payload.getString("type"))}}
        @JvmStatic fun restore(db:SQLiteDatabase,j:JSONObject?){j?.let{validate(it)};db.delete("release08",null,null);val a=j?.getJSONArray("rows")?:return;for(i in 0 until a.length()){val r=a.getJSONObject(i);put(db,r.getString("kind"),r.getString("id"),r.getJSONObject("payload"))}}
    }
}
