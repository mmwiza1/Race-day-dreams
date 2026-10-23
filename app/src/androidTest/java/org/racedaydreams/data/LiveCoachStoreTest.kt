package org.racedaydreams.data
import android.test.AndroidTestCase
import java.io.*
import java.time.*
import org.json.JSONObject
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.support.RunningRoutines
import org.racedaydreams.domain.execution.WorkoutExecutionEngine

class LiveCoachStoreTest:AndroidTestCase(){
    override fun setUp(){super.setUp();SqliteUserPreferencesRepository(context).clear()}
    override fun tearDown(){SqliteUserPreferencesRepository(context).clear();super.tearDown()}
    private fun native(id:String="owned",spec:JSONObject?=null):String {
        val wall=Instant.now().minusSeconds(120);val e=RunMetricsEngine(0,wall)
        for(i in 0..100)e.accept(RunMetricsEngine.Fix(wall.plusSeconds(i.toLong()),i*1000L,0.0,i*.00003,3.0,null,null,3.0))
        val snapshot=spec?.let{j->val execution=WorkoutExecutionEngine(SupportStore.steps(j));e.points().forEach{point->execution.update(WorkoutExecutionEngine.Input(point.cumulativeMetres(),point.activeMillis(),point.fix().monotonicMillis(),null,false,false))};val rows=org.json.JSONArray();execution.events().forEach{rows.put(JSONObject().put("type",it.type()).put("step",it.step()).put("active",it.activeMillis()).put("metres",it.metres()))};JSONObject(j.toString()).put("events",rows)}
        val store=RecordingStore(context);store.start(id,wall,0);store.flush(id,e.points(),e.events(),0,0,100000,"ACTIVE",snapshot);return store.finish(store.unfinished()!!,100000,recovered=false)
    }
    fun testRoutineCompletionNeverCreatesCalendarWorkout(){val before=FeatureStore(context).plannedWorkouts().size;SupportStore(context).completeRoutine("hips",null);assertEquals(before,FeatureStore(context).plannedWorkouts().size);assertEquals(1,SupportStore.exportData(ProfileDatabase.get(context).readableDatabase).getJSONArray("rows").length())}
    fun testDreamAssignmentRetainedAfterReopen(){val id=native();val first=SupportStore(context).dream(id,333.0);assertEquals(first,SupportStore(context).dream(id,333.0))}
    fun testSupportRejectsFutureVersionBeforeReplacingData(){val store=FeatureStore(context);SupportStore(context).completeRoutine("hips",null);val good=store.exportUserData();val bad=JSONObject(good.toString());bad.getJSONObject("support").put("version",99);try{store.restoreUserData(bad,false);fail("Future version")}catch(_:Exception){};assertEquals(good.toString(),store.exportUserData().toString())}
    fun testCompletePackageRoundTripIncludesRawSamplesAndDreamAssignment(){val id=native();val dream=SupportStore(context).dream(id,333.0);SupportStore(context).completeRoutine("hips",null);val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes);SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()));assertEquals(101,FeatureStore(context).activityDetail(id).samples().size);assertEquals(dream,SupportStore(context).dream(id,333.0));assertEquals(2,SupportStore.exportData(ProfileDatabase.get(context).readableDatabase).getJSONArray("rows").length())}
    fun testCompletePackageCorruptionLeavesDataUnchanged(){native();val before=FeatureStore(context).exportUserData().toString();try{CompleteBackup(context).restoreFrom(ByteArrayInputStream(byteArrayOf(1,2,3)));fail("Invalid package")}catch(_:Exception){};assertEquals(before,FeatureStore(context).exportUserData().toString());assertEquals(101,RecordingStore(context).points("owned").size)}
    fun testCompleteBackupRejectsActiveRecordingWithoutDiscard(){RecordingStore(context).start("unfinished",Instant.now(),0);try{CompleteBackup(context).exportTo(ByteArrayOutputStream());fail("Active recording")}catch(_:Exception){};assertEquals("unfinished",RecordingStore(context).unfinished()!!.id)}
    fun testNativeHistorySummariesDoNotHydrateCoordinates(){val id=native();assertEquals(0,FeatureStore(context).activitySummaries().first().samples().size);assertEquals(101,FeatureStore(context).activityDetail(id).samples().size)}
    fun testEveryBundledMovementHasOriginalSupportedPoseAndInstructions(){val poses=setOf("walk","lunge","stand","calf","swing","march","bridge","sit","quad");for(m in RunningRoutines.MOVEMENTS){assertTrue(m.pose() in poses);assertTrue(m.instructions().length>20);assertTrue(m.cue().isNotBlank())}}
    fun testZeroAndOversizedStepTargetsAreRejected(){for(value in listOf(0.0,Double.NaN,Double.POSITIVE_INFINITY)){try{WorkoutExecutionEngine.Step("bad",org.racedaydreams.domain.WorkoutSegment.Kind.RUN,value,null,null,null,null);fail("Invalid target")}catch(_:IllegalArgumentException){}}}
    private fun prescription():JSONObject {
        val today=LocalDate.now();val days=java.util.EnumSet.allOf(java.time.DayOfWeek::class.java)
        val profile=org.racedaydreams.domain.UserProfile(org.racedaydreams.domain.RaceGoal(org.racedaydreams.domain.RaceDistance.FIVE_K,"",today,org.racedaydreams.domain.GoalMode.FINISH,null),org.racedaydreams.domain.TrainingPreferences(org.racedaydreams.domain.TrainingStrategy.BALANCED,days,3,today.dayOfWeek,emptySet(),org.racedaydreams.domain.ScheduleType.FLEXIBLE,org.racedaydreams.domain.UnitSystem.METRIC),false)
        val segment=org.racedaydreams.domain.WorkoutSegment(org.racedaydreams.domain.WorkoutSegment.Kind.RUN,.3,null,295.0,305.0,null,7.0,"Controlled interval")
        val workout=org.racedaydreams.domain.PlannedWorkout("work",today,org.racedaydreams.domain.WorkoutCategory.INTERVALS,.3,null,listOf(segment),"Original")
        val plan=org.racedaydreams.domain.TrainingPlan("plan",today,today,emptyList(),listOf(workout))
        SqliteUserPreferencesRepository(context).save(profile)
        CoachingStore(context).saveCurrent(org.racedaydreams.domain.coaching.PlanVersion("version","plan",1,Instant.now(),"Test fixture",profile,"Synthetic",plan))
        return SupportStore(context).prepare("version","work",false)
    }
    fun testFinishLinksExactVersionAndRawWorkStepEvidence(){val id=native(spec=prescription());val response=CoachingStore(context).responses().single();assertEquals(id,response.activityId());assertEquals("version",response.planVersionId());assertEquals(1,response.completedReps());assertEquals(1,response.steps().size);assertTrue(response.steps().first().reliablePace());assertEquals(300.0,response.steps().first().metres(),.001)}
    fun testCompleteBackupPreservesPrescriptionAndStepEvidence(){val id=native(spec=prescription());val before=CoachingStore(context).responses().single();val bytes=ByteArrayOutputStream();CompleteBackup(context).exportTo(bytes);SqliteUserPreferencesRepository(context).clear();CompleteBackup(context).restoreFrom(ByteArrayInputStream(bytes.toByteArray()));assertEquals(before,CoachingStore(context).responses().single());assertEquals("work",FeatureStore(context).activityDetail(id).plannedWorkoutId());assertEquals("Original",CoachingStore(context).current().plan().workouts().first().note())}
    fun testMalformedExecutionTimelineRejectsWithoutReplacement(){native(spec=prescription());val before=FeatureStore(context).exportUserData();val corrupt=JSONObject(before.toString());val rows=corrupt.getJSONObject("support").getJSONArray("rows");for(i in 0 until rows.length()){val row=rows.getJSONObject(i);if(row.getString("kind")=="execution")row.getJSONObject("payload").getJSONArray("events").getJSONObject(0).put("active",-1)};try{FeatureStore(context).restoreUserData(corrupt,false);fail("Invalid timeline")}catch(_:Exception){};assertEquals(before.toString(),FeatureStore(context).exportUserData().toString())}
    fun testSchemaSevenToEightRetainsSampleEvidence(){val id=native();val helper=ProfileDatabase.get(context);helper.close();context.openOrCreateDatabase(ProfileDatabase.NAME,0,null).use{it.execSQL("DROP TABLE coach_support");it.version=7};assertEquals(8,helper.writableDatabase.version);assertEquals(101,FeatureStore(context).activityDetail(id).samples().size)}
}
