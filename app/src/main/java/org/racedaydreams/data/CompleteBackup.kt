package org.racedaydreams.data
import android.content.Context
import org.json.JSONObject
import org.racedaydreams.portability.BackupManager
import java.io.*
import java.security.MessageDigest
import java.util.zip.*

/** One bounded package. Both inner documents validate before core replacement; one outer
 * SQLite transaction covers raw merge + complete app restore. Any failure rolls all back. */
class CompleteBackup(private val context:Context) {
    private val helper=ProfileDatabase.get(context)
    private val manager=BackupManager(context,FeatureStore(context))
    private fun checksum(f:File):String {val md=MessageDigest.getInstance("SHA-256");f.inputStream().use{input->val b=ByteArray(32768);while(true){val n=input.read(b);if(n<0)break;md.update(b,0,n)}};return md.digest().joinToString(""){"%02x".format(it)}}
    fun exportTo(output:OutputStream){
        val app=File.createTempFile("full-app-",".json",context.cacheDir);val raw=File.createTempFile("full-raw-",".zip",context.cacheDir)
        try {
            val db=helper.writableDatabase;db.beginTransaction();try{
                require(RecordingStore(context).unfinished()==null){"Finish or recover the current run before complete backup"}
                app.outputStream().use{manager.exportTo(it)};raw.outputStream().use{RecordingArchive(context).exportTo(it)};db.setTransactionSuccessful()
            }finally{db.endTransaction()}
            val files=linkedMapOf("app.json" to app,"recordings.zip" to raw)
            val entries=JSONObject();files.forEach{(name,f)->entries.put(name,JSONObject().put("bytes",f.length()).put("sha256",checksum(f)))}
            val manifest=JSONObject().put("format","rdd-complete-backup").put("version",1).put("files",entries).toString().toByteArray(Charsets.UTF_8)
            val zip=ZipOutputStream(output);zip.putNextEntry(ZipEntry("manifest.json"));zip.write(manifest);zip.closeEntry();files.forEach{(name,f)->zip.putNextEntry(ZipEntry(name));f.inputStream().use{it.copyTo(zip)};zip.closeEntry()};zip.finish();zip.flush()
        }finally{app.delete();raw.delete()}
    }
    fun restoreFrom(input:InputStream){
        val app=File.createTempFile("restore-app-",".json",context.cacheDir);val raw=File.createTempFile("restore-raw-",".zip",context.cacheDir)
        try {
            val files=mapOf("app.json" to app,"recordings.zip" to raw);val names=mutableSetOf<String>();var manifest:JSONObject?=null
            val zip=ZipInputStream(input);while(true){val e=zip.nextEntry?:break;require(names.add(e.name)&&names.size<=3){"Duplicate backup entry"}
                if(e.name=="manifest.json"){val bytes=ByteArrayOutputStream();val b=ByteArray(1024);while(true){val n=zip.read(b);if(n<0)break;require(bytes.size()+n<=8192);bytes.write(b,0,n)};manifest=JSONObject(bytes.toString("UTF-8"))}
                else {val f=files[e.name]?:error("Unsupported backup entry");val limit=if(e.name=="app.json")BackupManager.MAX_BYTES.toLong() else RecordingArchive.MAX_BYTES+1024*1024;f.outputStream().use{out->val b=ByteArray(32768);var count=0L;while(true){val n=zip.read(b);if(n<0)break;count+=n;require(count<=limit){"Expanded backup is too large"};out.write(b,0,n)}}};zip.closeEntry()
            }
            val m=manifest?:error("Missing backup manifest");require(m.getString("format")=="rdd-complete-backup"&&m.get("version") is Int&&m.getInt("version")==1&&names==setOf("manifest.json","app.json","recordings.zip")){"Unsupported/incomplete backup"}
            files.forEach{(name,f)->val entry=m.getJSONObject("files").getJSONObject(name);require(entry.getLong("bytes")==f.length()&&entry.getString("sha256")==checksum(f)){"Backup checksum mismatch"}}
            app.inputStream().use{manager.inspect(it)}
            val db=helper.writableDatabase;db.beginTransaction();try{
                require(RecordingStore(context).unfinished()==null){"Finish or recover the current run before restore"}
                require(!SqliteGymExecutionRepository.hasUnfinished(db)){"Finish or discard the unfinished gym workout before restore"}
                // Raw importer validates every row before writing anything. The outer transaction
                // then protects both its merge and the typed core replacement against any failure.
                raw.inputStream().use{RecordingArchive(context).restoreFrom(it)}
                app.inputStream().use{manager.restoreFrom(it)}
                db.setTransactionSuccessful()
            }finally{db.endTransaction()}
        }finally{app.delete();raw.delete()}
    }
}
