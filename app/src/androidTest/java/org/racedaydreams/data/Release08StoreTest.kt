package org.racedaydreams.data
import android.test.AndroidTestCase
import org.json.*
import java.io.*
import java.time.*
import org.racedaydreams.domain.polish.CustomWorkout
import org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step
import org.racedaydreams.domain.WorkoutSegment
class Release08StoreTest:AndroidTestCase(){
 override fun setUp(){super.setUp();SqliteUserPreferencesRepository(context).clear()}
 override fun tearDown(){SqliteUserPreferencesRepository(context).clear();super.tearDown()}
 private fun workout()=CustomWorkout("custom","Evening intervals",listOf(CustomWorkout.Group(3,listOf(Step("Work",WorkoutSegment.Kind.RUN,400.0,null,280.0,290.0,7.0),Step("Recovery",WorkoutSegment.Kind.RECOVERY,null,90000,null,null,null)))))
 fun testSavedWorkoutExactRoundTrip(){val s=Release08Store(context);s.save(workout());assertEquals(workout(),s.workouts().single());assertEquals(6,SupportStore.steps(Release08Store.snapshot(workout(),"custom")).size)}
 fun testCustomSnapshotHasNoInventedPlanLink(){val j=Release08Store.snapshot(workout(),"custom");SupportStore.validatePayload("execution",j);assertFalse(j.has("planVersion"));assertFalse(j.has("workoutId"));assertTrue(CoachingStore(context).responses().isEmpty())}
 fun testPreferencesDoNotRerollOldAssignment(){val s=Release08Store(context);val old=SupportStore(context).dream("activity",5000.0);s.save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",false).put("cities",JSONArray()));assertEquals(old,SupportStore(context).dream("activity",5000.0));assertNull(SupportStore(context).dream("new",5000.0))}
 fun testGeographicBerlinAndLegacyAssignmentsSurviveCompleteBackupWithoutReroll() {
  val store=Release08Store(context)
  store.save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray().put("berlin-v2")))
  val support=SupportStore(context);val fresh=support.dream("fresh-berlin",5020.0)!!
  assertEquals("berlin-2014-geo-v1",fresh.routeId())
  val old=JSONObject().put("version",1).put("route","berlin-v2").put("start",1000.0).put("distance",5000.0).put("assignedAt",Instant.now().toString())
  support.save("dream","old-berlin",old)
  val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes)
  SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()))
  assertEquals(fresh,SupportStore(context).dream("fresh-berlin",5020.0))
  assertEquals("berlin-v2",SupportStore(context).dream("old-berlin",5000.0)!!.routeId())
  assertEquals(setOf("berlin-v2"),Release08Store(context).dreamPreferences().second)
 }
 fun testGeographicTokyoAndLegacyAssignmentsSurviveCompleteBackup() {
  val store=Release08Store(context)
  store.save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray().put("tokyo-v2")))
  val support=SupportStore(context);val fresh=support.dream("fresh-tokyo",10000.0)!!
  assertEquals("tokyo-2026-geo-v1",fresh.routeId())
  support.save("dream","old-tokyo",JSONObject().put("version",1).put("route","tokyo-v2").put("start",1000.0).put("distance",5000.0).put("assignedAt",Instant.now().toString()))
  val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes)
  SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()))
  assertEquals(fresh,SupportStore(context).dream("fresh-tokyo",10000.0))
  assertEquals("tokyo-v2",SupportStore(context).dream("old-tokyo",5000.0)!!.routeId())
  assertEquals(setOf("tokyo-v2"),Release08Store(context).dreamPreferences().second)
 }
 fun testGeographicLondonAndLegacyAssignmentsSurviveCompleteBackup() {
  val store=Release08Store(context)
  store.save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray().put("london-v2")))
  val support=SupportStore(context);val fresh=support.dream("fresh-london",5020.0)!!
  assertEquals("london-historical-geo-v1",fresh.routeId())
  support.save("dream","old-london",JSONObject().put("version",1).put("route","london-v2").put("start",1000.0).put("distance",5000.0).put("assignedAt",Instant.now().toString()))
  val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes)
  SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()))
  assertEquals(fresh,SupportStore(context).dream("fresh-london",5020.0))
  assertEquals("london-v2",SupportStore(context).dream("old-london",5000.0)!!.routeId())
  assertEquals(setOf("london-v2"),Release08Store(context).dreamPreferences().second)
 } fun testGeographicNewYorkAndLegacyAssignmentsSurviveCompleteBackup() {
  val store=Release08Store(context)
  store.save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray().put("new-york-v2")))
  val support=SupportStore(context);val fresh=support.dream("fresh-nyc",5020.0)!!
  assertEquals("new-york-2026-geo-v1",fresh.routeId())
  support.save("dream","old-nyc",JSONObject().put("version",1).put("route","new-york-v2").put("start",1000.0).put("distance",5000.0).put("assignedAt",Instant.now().toString()))
  val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes)
  SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()))
  assertEquals(fresh,SupportStore(context).dream("fresh-nyc",5020.0))
  assertEquals("new-york-v2",SupportStore(context).dream("old-nyc",5000.0)!!.routeId())
  assertEquals(setOf("new-york-v2"),Release08Store(context).dreamPreferences().second)
 } fun testGeographicParis2024AndLegacyAssignmentsSurviveCompleteBackup() {
  val store=Release08Store(context)
  store.save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray().put("paris-v2")))
  val support=SupportStore(context);val fresh=support.dream("fresh-paris",5020.0)!!
  assertEquals("paris-2024-olympic-geo-v1",fresh.routeId())
  support.save("dream","old-paris",JSONObject().put("version",1).put("route","paris-v2").put("start",1000.0).put("distance",5000.0).put("assignedAt",Instant.now().toString()))
  val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes)
  SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()))
  assertEquals(fresh,SupportStore(context).dream("fresh-paris",5020.0))
  assertEquals("paris-v2",SupportStore(context).dream("old-paris",5000.0)!!.routeId())
  assertEquals(setOf("paris-v2"),Release08Store(context).dreamPreferences().second)
 }
 fun testNoSelectedCityUsesStandardPresentation(){Release08Store(context).save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray()));assertNull(SupportStore(context).dream("new",5000.0))}
 fun testBackupRestoresWorkoutsAndPreferences(){val s=Release08Store(context);s.save(workout());s.save("checklist","race:kit",JSONObject().put("version",1).put("label","Kit").put("checked",true));val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes);SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()));assertEquals(workout(),Release08Store(context).workouts().single());assertTrue(Release08Store(context).get("checklist","race:kit")!!.getBoolean("checked"))}
 fun testInvalidNewPayloadRollsBack(){Release08Store(context).save(workout());val store=FeatureStore(context);val before=store.exportUserData();val bad=JSONObject(before.toString());bad.getJSONObject("release08").getJSONArray("rows").getJSONObject(0).getJSONObject("payload").put("groups",JSONArray());try{store.restoreUserData(bad,false);fail("Invalid custom workout")}catch(_:Exception){};assertEquals(before.toString(),store.exportUserData().toString())}
 fun testLegacyBackupWithoutNewSectionAccepted(){val store=FeatureStore(context);val old=store.exportUserData();old.remove("release08");Release08Store(context).save(workout());store.restoreUserData(old,false);assertTrue(Release08Store(context).workouts().isEmpty())}
 fun testSchemaEightUpgradePreservesExistingSupport(){SupportStore(context).completeRoutine("hips",null);val h=ProfileDatabase.get(context);h.close();context.openOrCreateDatabase(ProfileDatabase.NAME,0,null).use{it.execSQL("DROP TABLE release08");it.version=8};assertEquals(10,h.writableDatabase.version);assertEquals(1,SupportStore.exportData(h.readableDatabase).getJSONArray("rows").length());assertTrue(Release08Store(context).workouts().isEmpty())}
 fun testDiagnosticAllowListExcludesUserData(){val report=DiagnosticReport.build(context,"UNAVAILABLE",emptySet());val keys=report.keys().asSequence().toSet();assertFalse(keys.any{it in setOf("route","gps","notes","weight","history","activities","healthValues")});assertEquals("rdd-diagnostic",report.getString("format"));assertTrue(report.toString().length<4096)}
 fun testExplicitDiscardRemovesOnlyUnfinishedOriginAndContext(){val s=Release08Store(context);s.save(workout());s.save("origin","discard",JSONObject().put("version",1).put("mode","custom").put("at",Instant.now().toString()));SupportStore(context).save("execution","discard",Release08Store.snapshot(workout(),"custom"));RecordingStore(context).start("discard",Instant.now(),0);RecordingStore(context).discardUnfinished("discard");assertNull(s.get("origin","discard"));assertNull(SupportStore(context).get("execution","discard"));assertEquals(workout(),s.workouts().single())}
}
