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
 fun testNoSelectedCityUsesStandardPresentation(){Release08Store(context).save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",true).put("cities",JSONArray()));assertNull(SupportStore(context).dream("new",5000.0))}
 fun testBackupRestoresWorkoutsAndPreferences(){val s=Release08Store(context);s.save(workout());s.save("checklist","race:kit",JSONObject().put("version",1).put("label","Kit").put("checked",true));val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes);SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()));assertEquals(workout(),Release08Store(context).workouts().single());assertTrue(Release08Store(context).get("checklist","race:kit")!!.getBoolean("checked"))}
 fun testInvalidNewPayloadRollsBack(){Release08Store(context).save(workout());val store=FeatureStore(context);val before=store.exportUserData();val bad=JSONObject(before.toString());bad.getJSONObject("release08").getJSONArray("rows").getJSONObject(0).getJSONObject("payload").put("groups",JSONArray());try{store.restoreUserData(bad,false);fail("Invalid custom workout")}catch(_:Exception){};assertEquals(before.toString(),store.exportUserData().toString())}
 fun testLegacyBackupWithoutNewSectionAccepted(){val store=FeatureStore(context);val old=store.exportUserData();old.remove("release08");Release08Store(context).save(workout());store.restoreUserData(old,false);assertTrue(Release08Store(context).workouts().isEmpty())}
 fun testSchemaEightUpgradePreservesExistingSupport(){SupportStore(context).completeRoutine("hips",null);val h=ProfileDatabase.get(context);h.close();context.openOrCreateDatabase(ProfileDatabase.NAME,0,null).use{it.execSQL("DROP TABLE release08");it.version=8};assertEquals(9,h.writableDatabase.version);assertEquals(1,SupportStore.exportData(h.readableDatabase).getJSONArray("rows").length());assertTrue(Release08Store(context).workouts().isEmpty())}
}
