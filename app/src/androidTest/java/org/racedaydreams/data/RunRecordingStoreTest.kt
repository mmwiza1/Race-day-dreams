package org.racedaydreams.data
import android.test.AndroidTestCase
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.recording.GpxFiles
import java.io.*
import java.time.Instant

class RunRecordingStoreTest:AndroidTestCase(){
 override fun setUp(){super.setUp();ProfileDatabase.get(context).writableDatabase.apply{delete("recording_sessions",null,null);delete("activities",null,null)}}
 override fun tearDown(){newClear();super.tearDown()}
 private fun newClear(){SqliteUserPreferencesRepository(context).clear()}
 private fun trace(id:String="native"):RecordingStore.Session {val store=RecordingStore(context);val e=RunMetricsEngine(0,Instant.parse("2026-10-22T00:00:00Z"));for(i in 0..100)e.accept(RunMetricsEngine.Fix(Instant.parse("2026-10-22T00:00:00Z").plusSeconds(i.toLong()),i*1000L,0.0,i*0.00003,3.0,null,null,3.0));store.start(id,Instant.parse("2026-10-22T00:00:00Z"),0);store.flush(id,e.points(),e.events(),0,0,100000,"ACTIVE");return store.unfinished()!!}
 fun testFinalizationRetainsRawPointsAndComputesSamples(){val s=trace();val id=RecordingStore(context).finish(s,100000);val a=FeatureStore(context).activities().first{it.id()==id};assertEquals(101,a.samples().size);assertEquals(101,RecordingStore(context).points(s.id).size);assertNull(RecordingStore(context).unfinished());assertTrue(a.distanceMetres()>300)}
 fun testInterruptedStageCanRecoverWithoutInventingDowntime(){val s=trace();RecordingStore(context).recoverable();val recovered=RecordingStore(context).unfinished()!!;assertEquals("RECOVERABLE",recovered.state);assertEquals(s.last,recovered.last);RecordingStore(context).finish(recovered,null);assertEquals(100000L,FeatureStore(context).activities().first().elapsedTime().toMillis())}
 fun testFailedFinalizationRetainsStaging(){val store=RecordingStore(context);store.start("empty",Instant.now(),0);try{store.finish(store.unfinished()!!,null);fail("No samples should fail")}catch(_:IllegalArgumentException){};assertNotNull(store.unfinished())}
 fun testGpxRoundTripAndDuplicateImport(){val s=trace();RecordingStore(context).finish(s,100000);val output=ByteArrayOutputStream();GpxFiles(context).exportTo(s.id,output);val parsed=GpxFiles.parse(ByteArrayInputStream(output.toByteArray()));assertEquals(101,parsed.size);val first=GpxFiles(context).importFrom(ByteArrayInputStream(output.toByteArray()));val second=GpxFiles(context).importFrom(ByteArrayInputStream(output.toByteArray()));assertEquals(first,second);assertEquals(1,FeatureStore(context).activities().size)}
 fun testMalformedGpxCannotInsertActivity(){try{GpxFiles(context).importFrom(ByteArrayInputStream("<gpx><trk><trkseg><trkpt lat='0' lon='0'/></trkseg></trk></gpx>".toByteArray()));fail("Missing time")}catch(_:Exception){};assertTrue(FeatureStore(context).activities().isEmpty())}
 fun testGpxDoctypeRejected(){try{GpxFiles.parse(ByteArrayInputStream("<!DOCTYPE gpx [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><gpx>&x;</gpx>".toByteArray()));fail("Entity must reject")}catch(_:Exception){};assertTrue(FeatureStore(context).activities().isEmpty())}
 fun testRawArchiveRoundTripAndRepeatMerge(){val s=trace();val id=RecordingStore(context).finish(s,100000);val output=ByteArrayOutputStream();RecordingArchive(context).exportTo(output);FeatureStore(context).deleteActivity(id);assertTrue(RecordingStore(context).points(s.id).isEmpty());RecordingArchive(context).restoreFrom(ByteArrayInputStream(output.toByteArray()));RecordingArchive(context).restoreFrom(ByteArrayInputStream(output.toByteArray()));assertEquals(101,RecordingStore(context).points(s.id).size);assertEquals(1,FeatureStore(context).activities().size)}
 fun testBadArchiveRetainsExistingData(){trace();try{RecordingArchive(context).restoreFrom(ByteArrayInputStream(byteArrayOf(1,2,3)));fail("Bad archive")}catch(_:Exception){};assertEquals(101,RecordingStore(context).points("native").size)}
}
