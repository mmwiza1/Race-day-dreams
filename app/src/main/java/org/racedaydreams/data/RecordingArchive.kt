package org.racedaydreams.data

import android.content.ContentValues
import android.content.Context
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.*

/** Version-one streaming companion archive for user-owned raw route/sensor/event evidence.
 * Merge only; conflicts reject atomically. This is paired with the regular app JSON backup.
 * Health Connect caches are never included. Temporary files remain application-private.
 */
class RecordingArchive(private val context:Context) {
    private val helper=ProfileDatabase.get(context)
    companion object {const val MAX_BYTES=512L*1024*1024;const val MAX_LINE=8*1024*1024
        private fun digest(file:File):String {val md=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val b=ByteArray(32768);while(true){val n=input.read(b);if(n<0)break;md.update(b,0,n)}};return md.digest().joinToString(""){"%02x".format(it)}}
        private fun lines(file:File,block:(JSONObject)->Unit){file.bufferedReader().use{r->val line=StringBuilder();while(true){val c=r.read();if(c<0){require(line.isEmpty()){ "Truncated archive row" };break};if(c==10){if(line.isNotEmpty())block(JSONObject(line.toString()));line.setLength(0)}else{require(line.length<MAX_LINE){"Archive row too large"};line.append(c.toChar())}}}}
    }
    fun exportTo(output:OutputStream){
        val file=File.createTempFile("route-export-",".jsonl",context.cacheDir)
        try{
            val db=helper.readableDatabase;db.beginTransaction();try{
                file.bufferedWriter().use{w->
                    fun row(j:JSONObject){val text=j.toString();require(text.length<=MAX_LINE){"Recording payload exceeds archive row limit"};w.write(text);w.newLine()}
                    db.rawQuery("SELECT id,state,started_at,started_mono,last_mono,activity_id FROM recording_sessions ORDER BY id",null).use{c->var sessions=0;while(c.moveToNext()){
                        require(++sessions<=1000){"Archive exceeds 1000 recordings; export routes separately"};val id=c.getString(0)
                        row(JSONObject().put("kind","session").put("id",id).put("state",c.getString(1)).put("start",c.getString(2)).put("mono",c.getLong(3)).put("last",c.getLong(4)).put("activity",c.getString(5)))
                        SupportStore(context).get("execution",id)?.let{row(JSONObject().put("kind","context").put("id",id).put("payload",it))}
                        for((table,kind) in listOf("recording_points" to "point","recording_events" to "event"))db.rawQuery("SELECT sequence,payload FROM $table WHERE session_id=? ORDER BY sequence",arrayOf(id)).use{p->while(p.moveToNext())row(JSONObject().put("kind",kind).put("id",id).put("seq",p.getInt(0)).put("payload",JSONObject(p.getString(1))))}
                        if(!c.isNull(5))db.rawQuery("SELECT payload FROM activities WHERE id=?",arrayOf(c.getString(5))).use{a->if(a.moveToFirst())row(JSONObject().put("kind","activity").put("id",id).put("payload",JSONObject(a.getString(0))))}
                    }}
                };db.setTransactionSuccessful()
            }finally{db.endTransaction()}
            require(file.length()<=MAX_BYTES){"Recording archive exceeds 512 MiB"}
            val manifest=JSONObject().put("format","rdd-owned-recordings").put("version",2).put("bytes",file.length()).put("sha256",digest(file)).toString().toByteArray()
            val zip=ZipOutputStream(output);zip.putNextEntry(ZipEntry("manifest.json"));zip.write(manifest);zip.closeEntry();zip.putNextEntry(ZipEntry("recordings.jsonl"));file.inputStream().use{it.copyTo(zip)};zip.closeEntry();zip.finish();zip.flush()
        }finally{file.delete()}
    }
    fun restoreFrom(input:InputStream){
        val file=File.createTempFile("route-restore-",".jsonl",context.cacheDir)
        try{
            val zip=ZipInputStream(input);var manifest:JSONObject?=null;var data=false;val names=mutableSetOf<String>()
            while(true){val e=zip.nextEntry?:break;require(names.add(e.name)&&names.size<=2){"Duplicate archive entry"};when(e.name){
                "manifest.json"->{val b=ByteArrayOutputStream();val buf=ByteArray(1024);while(true){val n=zip.read(buf);if(n<0)break;require(b.size()+n<=8192);b.write(buf,0,n)};manifest=JSONObject(b.toString("UTF-8"))}
                "recordings.jsonl"->{data=true;file.outputStream().use{out->val b=ByteArray(32768);var count=0L;while(true){val n=zip.read(b);if(n<0)break;count+=n;require(count<=MAX_BYTES){"Expanded archive too large"};out.write(b,0,n)}}}
                else->error("Unsupported archive entry")
            };zip.closeEntry()}
            val m=manifest?:error("Missing manifest");require(data&&m.getString("format")=="rdd-owned-recordings"&&m.get("version") is Int&&m.getInt("version") in 1..2&&m.getLong("bytes")==file.length()&&m.getString("sha256")==digest(file)){"Archive integrity/version mismatch"}
            val bounds=mutableMapOf<String,LongRange>();val completed=mutableSetOf<String>();val activityRows=mutableSetOf<String>();val activityIds=mutableMapOf<String,String>();val eventSeq=mutableMapOf<String,Int>()
            val contexts=mutableSetOf<String>();val ids=mutableSetOf<String>();val seq=mutableMapOf<String,Int>();val lastMono=mutableMapOf<String,Long>();val lastDistance=mutableMapOf<String,Double>()
            lines(file){j->val id=j.getString("id");require(id.length in 1..256);when(j.getString("kind")){
                "session"->{require(ids.add(id)&&ids.size<=1000);Instant.parse(j.getString("start"));require(j.getLong("mono")>=0&&j.getLong("last")>=j.getLong("mono"));require(j.getString("state") in listOf("ACTIVE","PAUSED","RECOVERABLE","FINISHED"));bounds[id]=j.getLong("mono")..j.getLong("last");if(j.getString("state")=="FINISHED"){completed.add(id);activityIds[id]=j.getString("activity")}}
                "context"->{require(id in ids&&contexts.add(id));SupportStore.validatePayload("execution",j.getJSONObject("payload"))}
                "point"->{require(id in ids);val expected=seq[id]?:0;require(j.getInt("seq")==expected&&expected<100000);seq[id]=expected+1;val p=RecordingStore.decode(j.getJSONObject("payload"));require(p.fix().monotonicMillis() in bounds.getValue(id)&&p.activeMillis()<=p.fix().monotonicMillis()-bounds.getValue(id).first&&p.fix().monotonicMillis()>(lastMono[id]?:-1)&&p.cumulativeMetres()>=(lastDistance[id]?:0.0)&&p.activeMillis()>=0&&p.segment()>=0);lastMono[id]=p.fix().monotonicMillis();lastDistance[id]=p.cumulativeMetres()}
                "event"->{require(id in ids&&j.getInt("seq")== (eventSeq[id]?:0));eventSeq[id]=(eventSeq[id]?:0)+1;val p=j.getJSONObject("payload");require(p.getString("type").length in 1..64&&p.getLong("mono") in bounds.getValue(id));if(p.has("value")&&!p.isNull("value"))require(p.getDouble("value").isFinite());Instant.parse(p.getString("wall"))}
                "activity"->{require(id in ids&&activityRows.add(id));val a=FeatureJson.activity(j.getJSONObject("payload"));require(a.id()==activityIds[id]&&a.source().recordId()==id&&a.source().provider() in listOf("rdd","gpx"))}
                else->error("Unsupported recording row")
            }}
            require(activityRows.containsAll(completed)){"Finished recording metadata is missing"}
            val db=helper.writableDatabase;db.beginTransaction();try{
                require(RecordingStore(context).unfinished()==null){"Finish or recover your active run before restoring"}
                val skip=mutableSetOf<String>()
                lines(file){j->val id=j.getString("id");when(j.getString("kind")){
                    "session"->{db.rawQuery("SELECT state,started_at,started_mono,last_mono FROM recording_sessions WHERE id=?",arrayOf(id)).use{c->if(c.moveToFirst()){require(c.getString(1)==j.getString("start")&&c.getLong(2)==j.getLong("mono")&&c.getLong(3)==j.getLong("last")){"Existing recording conflict"};skip.add(id)}else db.insertOrThrow("recording_sessions",null,ContentValues().apply{put("id",id);put("state",if(j.getString("state")=="FINISHED")"FINISHED"else"RECOVERABLE");put("started_at",j.getString("start"));put("started_mono",j.getLong("mono"));put("last_mono",j.getLong("last"));if(j.has("activity"))put("activity_id",j.getString("activity"))})}}
                    "context"->{val old=SupportStore(context).get("execution",id);require(old==null||old.toString()==j.getJSONObject("payload").toString()){ "Existing execution conflict" };SupportStore.put(db,"execution",id,j.getJSONObject("payload"))}
                    "point","event"->{val table=if(j.getString("kind")=="point")"recording_points"else"recording_events";if(id !in skip)db.insertOrThrow(table,null,ContentValues().apply{put("session_id",id);put("sequence",j.getInt("seq"));put("payload",j.getJSONObject("payload").toString())})else db.rawQuery("SELECT payload FROM $table WHERE session_id=? AND sequence=?",arrayOf(id,j.getInt("seq").toString())).use{c->require(c.moveToFirst()&&c.getString(0)==j.getJSONObject("payload").toString()){ "Existing sample conflict" }}}
                    "activity"->{val a=FeatureJson.activity(j.getJSONObject("payload"));db.rawQuery("SELECT payload FROM activities WHERE id=?",arrayOf(a.id())).use{c->if(!c.moveToFirst()||FeatureJson.activity(JSONObject(c.getString(0))).samples().isEmpty())FeatureStore.putActivity(db,a,false)}}
                }}
                for(id in skip)for((table,count) in listOf("recording_points" to (seq[id]?:0),"recording_events" to (eventSeq[id]?:0)))db.rawQuery("SELECT COUNT(*) FROM $table WHERE session_id=?",arrayOf(id)).use{c->require(c.moveToFirst()&&c.getInt(0)==count){"Existing recording has different evidence coverage"}}
                RecordingStore.restoreRetainedSamples(db);db.setTransactionSuccessful()
            }finally{db.endTransaction()}
        }finally{file.delete()}
    }
}
