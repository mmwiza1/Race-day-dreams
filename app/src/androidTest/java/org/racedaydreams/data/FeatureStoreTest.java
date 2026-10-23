package org.racedaydreams.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.baseline.*;
import java.time.*;
import java.util.*;

/** Tests real Android SQLite migrations, import identity and transactional portable data. */
@SuppressWarnings("deprecation")
public final class FeatureStoreTest extends AndroidTestCase {
    private FeatureStore store;
    private SqliteUserPreferencesRepository profiles;
    @Override protected void setUp() throws Exception {
        super.setUp();
        ProfileDatabase.get(getContext()).close();
        getContext().deleteDatabase(ProfileDatabase.NAME);
        store=new FeatureStore(getContext()); profiles=new SqliteUserPreferencesRepository(getContext());
    }
    @Override protected void tearDown() throws Exception {
        store.close();profiles.close();getContext().deleteDatabase(ProfileDatabase.NAME);super.tearDown();
    }
    public void testVersionOneMigratesWithoutChangingProfileOrDraft() {
        frozenVersionOne();
        assertTrue(store.activities().isEmpty());
        UserProfile profile=profiles.load().get();
        assertEquals(RaceDistance.TEN_K,profile.raceGoal().distance());
        assertEquals("Original race",profile.raceGoal().raceName());
        assertEquals(Duration.ofSeconds(3300),profile.raceGoal().targetTime());
        assertTrue(profile.preferences().runningDays().contains(DayOfWeek.TUESDAY));
        assertTrue(profile.preferences().gymDays().contains(DayOfWeek.TUESDAY));
        assertEquals("{\"step\":2}",profiles.loadDraft().get());
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){assertEquals(7,db.getVersion());}
        store.saveShoe(shoe("new",true));assertEquals(1,store.shoes().size());
    }
    public void testPublishedVersionTwoUpgradesWithoutChangingNotesOrOtherData() throws Exception {
        frozenVersionTwo(false);
        assertVersionTwoDataRetained(false);
    }
    public void testNullableDraftVersionTwoUpgradesWithoutLosingAbsentOrBlankNotes() throws Exception {
        frozenVersionTwo(true);
        assertVersionTwoDataRetained(true);
    }
    public void testVersionTwoCopyFailureRollsBackAndAllowsRetryWithoutReset() throws Exception {
        frozenVersionTwo(false);
        // Raw fixture connections intentionally leave FK enforcement off to model corrupt input.
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
            db.execSQL("UPDATE activity_annotations SET shoe_id='unavailable-shoe' WHERE record_id='preserved'");
        }
        try{store.activities();fail("A failed annotation copy must not leave a partial upgrade.");}catch(SQLiteException expected){
            try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
                assertEquals(2,db.getVersion());
                try(Cursor c=db.rawQuery("SELECT notes,rpe,shoe_id FROM activity_annotations WHERE record_id='preserved'",null)){
                    assertTrue(c.moveToFirst());assertEquals("Saved note",c.getString(0));assertEquals(7d,c.getDouble(1),0);assertEquals("unavailable-shoe",c.getString(2));
                }
                try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE name='activity_annotations_v3'",null)){assertFalse(c.moveToFirst());}
                db.execSQL("UPDATE activity_annotations SET shoe_id='migration-shoe' WHERE record_id='preserved'");
            }
        }
        assertVersionTwoDataRetained(false);
    }
    public void testVersionOneFailureAtFinalMigrationRollsBackIntermediateSchema() {
        frozenVersionOne();
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){db.execSQL("CREATE TABLE activity_annotations_v3 (collision TEXT)");}
        try{store.activities();fail("The complete sequential upgrade must be atomic.");}catch(SQLiteException expected){
            try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
                assertEquals(1,db.getVersion());
                try(Cursor c=db.rawQuery("SELECT race_name FROM race_goal",null)){assertTrue(c.moveToFirst());assertEquals("Original race",c.getString(0));}
                try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE name='activities'",null)){assertFalse(c.moveToFirst());}
                try(Cursor c=db.rawQuery("PRAGMA table_info(activity_annotations_v3)",null)){assertTrue(c.moveToFirst());assertEquals("collision",c.getString(1));}
            }
        }
    }
    public void testFailedMigrationRollsBackAllNewTablesAndRetainsVersionOne() {
        frozenVersionOne();
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){db.execSQL("CREATE TABLE shoes (collision TEXT)");}
        try{store.activities();fail("A schema collision must fail explicitly.");}catch(SQLiteException expected){
            try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
                assertEquals(1,db.getVersion());
                try(Cursor c=db.rawQuery("SELECT race_name FROM race_goal",null)){assertTrue(c.moveToFirst());assertEquals("Original race",c.getString(0));}
                try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE name='activities'",null)){assertFalse(c.moveToFirst());}
            }
        }
    }
    public void testImportDeduplicatesAndPreservesAnnotationsDuringUpdate() {
        store.saveShoe(shoe("trainer",true));
        ActivityRecord original=activity("provider-run","health-connect","record-1",5000d);
        store.applyImport("health-connect",Collections.singletonList(original),Collections.emptyList(),"token-one");
        store.saveActivityNotes(original.id(),"Felt relaxed",4d);
        store.applyImport("health-connect",Collections.singletonList(activity(original.id(),"health-connect","record-1",5100d)),Collections.emptyList(),"token-two");
        assertEquals(1,store.activities().size());ActivityRecord updated=store.activities().get(0);
        assertEquals(5100d,updated.distanceMetres(),0.01);assertEquals("Felt relaxed",updated.notes());assertEquals(4d,updated.rpe(),0.01);assertEquals("trainer",updated.shoeId());
        assertEquals("token-two",store.importToken("health-connect"));
    }
    public void testProviderNotesRemainVisibleUntilAnExplicitUserOverride() throws Exception {
        store.saveShoe(shoe("trainer",true));
        ActivityRecord run=withNotes(activity("provider-run","health-connect","record-1",5000d),"Provider note");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"one");
        assertEquals("Provider note",find(run.id()).notes());assertEquals("trainer",find(run.id()).shoeId());
        store.assignShoe(run.id(),null);
        assertEquals("Provider note",find(run.id()).notes());
        run=withNotes(run,"Updated provider note");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"two");
        assertEquals("Updated provider note",find(run.id()).notes());
        JSONObject withoutOverride=store.exportUserData();
        assertTrue(withoutOverride.getJSONArray("annotations").getJSONObject(0).isNull("notes"));
        assertFalse(withoutOverride.toString().contains("Updated provider note"));
        store.restoreUserData(withoutOverride,false);
        run=withNotes(run,"Reimported provider note");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"three");
        assertEquals("Reimported provider note",find(run.id()).notes());
        store.saveActivityNotes(run.id(),"",null);store.assignShoe(run.id(),"trainer");
        JSONObject explicitlyCleared=store.exportUserData();
        assertEquals("",explicitlyCleared.getJSONArray("annotations").getJSONObject(0).getString("notes"));
        store.restoreUserData(explicitlyCleared,false);
        run=withNotes(run,"Another provider note");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"four");
        assertEquals("",find(run.id()).notes());assertEquals("trainer",find(run.id()).shoeId());
        store.saveActivityNotes(run.id(),"My own note",5d);
        run=withNotes(run,"Latest provider note");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"five");
        assertEquals("My own note",find(run.id()).notes());
        // Editing annotations must not rewrite the separately retained provider payload.
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null);Cursor c=db.rawQuery("SELECT payload FROM activities WHERE id=?",new String[]{run.id()})){
            assertTrue(c.moveToFirst());assertEquals("Latest provider note",FeatureJson.activity(FeatureJson.object(c.getString(0))).notes());
        }
    }
    public void testExistingPerformancePreferenceRoundTripsWithoutCurrentEligibility() throws Exception {
        UserProfile previous=profile();TrainingPreferences p=previous.preferences();
        UserProfile saved=new UserProfile(previous.raceGoal(),new TrainingPreferences(TrainingStrategy.PERFORMANCE,p.runningDays(),p.runsPerWeek(),p.longRunDay(),p.gymDays(),p.scheduleType(),p.units()),false);
        profiles.save(saved);assertEquals(saved,profiles.load().get());
        JSONObject backup=store.exportUserData();store.restoreUserData(backup,false);
        assertEquals(saved,profiles.load().get());assertFalse(profiles.load().get().performanceEligible());
    }
    public void testImportDeletionRemovesDerivedEffortsButPreservesOfficialResultAndAnnotations() {
        ActivityRecord run=sampledActivity("provider-run","health-connect","record-1");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"one");
        assertFalse(store.results().isEmpty());
        RecordedResult official=new RecordedResult("chip",PerformanceDistance.ONE_K,Duration.ofSeconds(280),LocalDate.of(2030,1,1),"Race",ResultSource.OFFICIAL_CHIP,run.id(),null);
        store.saveResult(official);store.saveActivityNotes(run.id(),"Keep this note",7d);
        store.applyImport("health-connect",Collections.emptyList(),Collections.singletonList("record-1"),"two");
        assertTrue(store.activities().isEmpty());assertEquals(Collections.singletonList(official),store.results());
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"three");
        assertEquals("Keep this note",store.activities().get(0).notes());
    }
    public void testFailedImportRollsBackRecordsAndCheckpoint() {
        ActivityRecord original=activity("one","health-connect","source-one",5000d);
        store.applyImport("health-connect",Collections.singletonList(original),Collections.emptyList(),"before");
        List<ActivityRecord> conflict=Arrays.asList(activity("two","health-connect","source-two",2000d),activity("one","health-connect","different-source",3000d));
        try{store.applyImport("health-connect",conflict,Collections.emptyList(),"after");fail("Identity conflict should abort the entire page.");}catch(IllegalArgumentException expected){assertEquals(1,store.activities().size());assertEquals("before",store.importToken("health-connect"));}
    }
    public void testShoeReplacementDoesNotRemoveAssignmentsAndOnlyOneDefaultRemains() {
        store.saveShoe(shoe("first",true));store.upsertManualActivity(activity("manual","manual","manual",5000d));assertEquals("first",store.activities().get(0).shoeId());
        store.saveShoe(shoe("second",true));store.saveShoe(new Shoe("first","Brand","Updated",null,100d,false,"note",false));
        assertEquals("first",store.activities().get(0).shoeId());
        int defaults=0;for(Shoe s:store.shoes())if(s.defaultShoe())defaults++;assertEquals(1,defaults);
        store.assignShoe("manual","second");assertEquals("second",store.activities().get(0).shoeId());store.assignShoe("manual",null);assertNull(store.activities().get(0).shoeId());
    }
    public void testManualBaselineTimestampSurvivesExportAndRestore() throws Exception {
        ManualBaselineInput baseline=new ManualBaselineInput(20d,3d,10d,Duration.ofMinutes(28),null,null,390d,8);
        store.saveManualBaseline(baseline);Instant recorded=store.manualBaselineRecordedAt();JSONObject backup=store.exportUserData();
        store.saveManualBaseline(ManualBaselineInput.empty());store.restoreUserData(backup,false);
        assertEquals(baseline,store.savedManualBaseline());assertEquals(recorded,store.manualBaselineRecordedAt());
    }
    public void testQualifiedExternalPbHistorySurvivesBackupWithoutRawHealthData() throws Exception {
        ActivityRecord run=sampledActivity("external","health-connect","source-id");
        store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"one");
        List<RecordedResult> before=store.results();assertFalse(before.isEmpty());
        JSONObject backup=store.exportUserData();assertEquals(0,backup.getJSONArray("activities").length());assertTrue(backup.getJSONArray("bestEfforts").length()>0);
        store.restoreUserData(backup,false);assertTrue(store.activities().isEmpty());assertEquals(before,store.results());
        // A source deletion must invalidate archived PBs even before its activity is re-imported.
        store.applyImport("health-connect",Collections.emptyList(),Collections.singletonList("source-id"),"after");assertTrue(store.results().isEmpty());
    }
    public void testUpdatedImportedSourceInvalidatesRestoredPbsBeforeRecalculation() throws Exception {
        ActivityRecord run=sampledActivity("external","health-connect","source-id");store.applyImport("health-connect",Collections.singletonList(run),Collections.emptyList(),"one");JSONObject backup=store.exportUserData();store.restoreUserData(backup,false);assertFalse(store.results().isEmpty());
        ActivityRecord withoutSamples=activity(run.id(),"health-connect","source-id",5000d);store.applyImport("health-connect",Collections.singletonList(withoutSamples),Collections.emptyList(),"two");assertTrue(store.results().isEmpty());
    }
    public void testExportRoundTripIncludesUserDataButExcludesExternalMeasurementsAndTokens() throws Exception {
        profiles.save(profile());profiles.saveDraft("{\"step\":1}");store.saveShoe(shoe("trainer",true));
        ActivityRecord manual=sampledActivity("local","manual","local");store.upsertManualActivity(manual);
        ActivityRecord imported=activity("external","health-connect","external-id",7000d);store.applyImport("health-connect",Collections.singletonList(imported),Collections.emptyList(),"private-checkpoint");
        store.saveActivityNotes(imported.id(),"User annotation",5d);
        store.saveResult(new RecordedResult("result",PerformanceDistance.FIVE_K,Duration.ofMinutes(27),LocalDate.of(2030,1,1),"Official",ResultSource.OFFICIAL_CHIP,imported.id(),"chip result"));
        store.putSetting("baselineReviewed","true");
        AssessmentResult assessment=new AssessmentResult("assessment",new BaselineAssessment(1,AssessmentMode.CONTROLLED),Duration.ofMinutes(6),6d,Instant.parse("2030-01-01T12:00:00Z"),manual.id());store.saveAssessment(assessment);
        JSONObject backup=store.exportUserData();assertEquals(1,backup.getJSONArray("activities").length());assertFalse(backup.toString().contains("private-checkpoint"));
        store.restoreUserData(backup,false);assertEquals(profile(),profiles.load().get());assertEquals("{\"step\":1}",profiles.loadDraft().get());
        assertEquals(1,store.activities().size());assertEquals("local",store.activities().get(0).id());assertNull(store.importToken("health-connect"));assertEquals("true",store.getSetting("baselineReviewed"));assertEquals(Collections.singletonList(assessment),store.assessments());
        store.applyImport("health-connect",Collections.singletonList(imported),Collections.emptyList(),"new-token");assertEquals("User annotation",find("external").notes());
    }
    public void testInvalidRestoreLeavesExistingDataUntouched() throws Exception {
        store.upsertManualActivity(activity("keep","manual","keep",3000d));JSONObject backup=store.exportUserData();
        JSONArray rows=backup.getJSONArray("activities");rows.put(rows.getJSONObject(0));
        try{store.restoreUserData(backup,false);fail("Duplicate identities should fail before deletion.");}catch(IllegalArgumentException expected){assertEquals("keep",store.activities().get(0).id());}
    }
    public void testRestoreWriteFailureRollsBackProfileAndFeatures() throws Exception {
        profiles.save(profile());store.saveShoe(shoe("existing",true));JSONObject backup=store.exportUserData();
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){db.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON shoes BEGIN SELECT RAISE(ABORT, 'test failure'); END");}
        try{store.restoreUserData(backup,false);fail("Injected write failure should abort restore.");}catch(SQLiteException expected){assertEquals(profile(),profiles.load().get());assertEquals("existing",store.shoes().get(0).id());}
    }
    public void testMergeAndIncompatibleVersionAreRejectedWithoutWrites() throws Exception {
        store.putSetting("keep","value");JSONObject backup=store.exportUserData();
        try{store.restoreUserData(backup,true);fail("Undefined merge semantics must not mutate data.");}catch(IllegalArgumentException expected){}
        backup.put("storeVersion",999);try{store.restoreUserData(backup,false);fail("Unknown versions require a migration.");}catch(IllegalArgumentException expected){}
        assertEquals("value",store.getSetting("keep"));
    }
    public void testConfirmedResetClosesBothRepositoryViewsWithoutResurrectingData() {
        profiles.save(profile());store.saveShoe(shoe("shoe",true));store.upsertManualActivity(activity("manual","manual","manual",4000d));
        store.applyImport("health-connect",Collections.emptyList(),Collections.emptyList(),"token");profiles.clear();
        assertFalse(profiles.load().isPresent());assertTrue(store.activities().isEmpty());assertTrue(store.shoes().isEmpty());assertNull(store.importToken("health-connect"));
        store.putSetting("new","value");assertEquals("value",store.getSetting("new"));assertFalse(profiles.load().isPresent());
    }
    public void testPlannedWorkoutRevisionChangesWithoutOverwritingActualActivity() {
        PlannedWorkout plan=new PlannedWorkout("planned",LocalDate.of(2030,2,1),WorkoutCategory.EASY_RUN,5d,null,Collections.emptyList(),null);
        store.savePlannedWorkout(plan);assertEquals(1L,store.plannedWorkoutRevision(plan.id()));store.savePlannedWorkout(plan);assertEquals(2L,store.plannedWorkoutRevision(plan.id()));
        store.upsertManualActivity(activity("actual","manual","actual",6000d));assertEquals(Double.valueOf(5d),store.plannedWorkouts().get(0).distanceKm());assertEquals(6000d,store.activities().get(0).distanceMetres(),0.01);
        store.deletePlannedWorkout(plan.id());assertEquals(0L,store.plannedWorkoutRevision(plan.id()));assertEquals(1,store.activities().size());
    }
    public void testFractionalStoreVersionAndWorkoutRevisionFailBeforeReplacement() throws Exception {
        store.upsertManualActivity(activity("keep","manual","keep",3000d));
        PlannedWorkout plan=new PlannedWorkout("planned",LocalDate.of(2030,2,1),WorkoutCategory.EASY_RUN,5d,null,Collections.emptyList(),null);
        store.savePlannedWorkout(plan);
        JSONObject data=store.exportUserData();data.put("storeVersion",1.5);
        rejectedRestore(data);
        data.put("storeVersion",1);data.getJSONArray("plannedWorkouts").getJSONObject(0).put("revision",1.5);
        rejectedRestore(data);
        data.getJSONArray("plannedWorkouts").getJSONObject(0).put("revision","2");
        rejectedRestore(data);
        assertEquals(1L,store.plannedWorkoutRevision("planned"));
        assertEquals("keep",store.activities().get(0).id());
    }
    public void testDistanceCodeConflictsAreRejectedForWritesAndRestore() throws Exception {
        RecordedResult original=new RecordedResult("custom-one",new PerformanceDistance("custom",6000,"Custom"),Duration.ofMinutes(30),LocalDate.of(2030,1,1),null,ResultSource.MANUAL,null,null);
        store.saveResult(original);
        RecordedResult conflict=new RecordedResult("custom-two",new PerformanceDistance("custom",6100,"Custom"),Duration.ofMinutes(31),LocalDate.of(2030,1,2),null,ResultSource.MANUAL,null,null);
        try{store.saveResult(conflict);fail("One distance code cannot describe two distances.");}catch(IllegalArgumentException expected){}
        assertEquals(Collections.singletonList(original),store.results());
        JSONObject data=store.exportUserData();data.getJSONArray("results").put(FeatureJson.result(conflict));
        rejectedRestore(data);
        assertEquals(Collections.singletonList(original),store.results());
        // Core codes remain canonical even when there is no prior result for that distance.
        RecordedResult wrongCore=new RecordedResult("wrong-core",new PerformanceDistance("5k",6000,"5K"),Duration.ofMinutes(27),LocalDate.of(2030,1,3),null,ResultSource.MANUAL,null,null);
        try{store.saveResult(wrongCore);fail("A 5K code must remain 5000 metres.");}catch(IllegalArgumentException expected){}
        assertEquals(Collections.singletonList(original),store.results());
    }
    public void testArchivedEffortsCannotChangeTheIdentityOfARetainedActivity() throws Exception {
        ActivityRecord activity=sampledActivity("manual-samples","manual","source");store.upsertManualActivity(activity);
        List<RecordedResult> before=store.results();assertFalse(before.isEmpty());
        JSONObject data=store.exportUserData();data.getJSONArray("bestEfforts").getJSONObject(0).getJSONObject("source").put("recordId","different-source");
        rejectedRestore(data);
        assertEquals(before,store.results());assertEquals(activity.id(),store.activities().get(0).id());
    }
    public void testBackupDoesNotCoerceNullIdentitiesOrNumericAndBooleanText() throws Exception {
        store.upsertManualActivity(activity("keep","manual","keep",3000d));store.saveShoe(shoe("shoe",false));
        JSONObject data=store.exportUserData();data.getJSONArray("activities").getJSONObject(0).getJSONObject("source").put("recordId",JSONObject.NULL);
        rejectedRestore(data);
        data=store.exportUserData();data.getJSONArray("activities").getJSONObject(0).put("distanceMetres","3000");
        rejectedRestore(data);
        data=store.exportUserData();data.getJSONArray("shoes").getJSONObject(0).put("defaultShoe","false");
        rejectedRestore(data);
        assertEquals("keep",store.activities().get(0).id());assertEquals(1,store.shoes().size());
    }
    public void testAssessmentJsonUsesTheSameTypedValidationAsOtherSavePaths() throws Exception {
        try{store.recordAssessment(new JSONObject().put("id","invalid"));fail("An incomplete assessment cannot be persisted.");}catch(IllegalArgumentException expected){}
        assertTrue(store.assessments().isEmpty());
        try{FeatureJson.object("{} trailing-data");fail("A JSON value must not have trailing data.");}catch(IllegalArgumentException expected){}
    }
    private void rejectedRestore(JSONObject data) {
        try{store.restoreUserData(data,false);fail("Invalid data must be rejected before replacement.");}catch(IllegalArgumentException expected){}
    }
    private ActivityRecord find(String id){for(ActivityRecord a:store.activities())if(a.id().equals(id))return a;throw new AssertionError("Missing activity");}
    private static Shoe shoe(String id,boolean defaultShoe){return new Shoe(id,"Brand","Model",LocalDate.of(2029,1,1),null,false,null,defaultShoe);}
    private static ActivityRecord activity(String id,String provider,String recordId,Double metres){Instant start=Instant.parse("2030-01-01T12:00:00Z");return new ActivityRecord(id,new SourceIdentity(provider,recordId,"source.app"),start,start.plusSeconds(1800),ZoneOffset.ofHours(1),ZoneOffset.ofHours(1),metres,null,null,null,null,null,null,null,Collections.emptyList(),Collections.emptyList(),null,null);}
    private static ActivityRecord withNotes(ActivityRecord a,String notes){return new ActivityRecord(a.id(),a.source(),a.start(),a.end(),a.startOffset(),a.endOffset(),a.distanceMetres(),a.movingTime(),a.averageHeartRate(),a.maxHeartRate(),a.elevationGainMetres(),a.cadence(),a.rpe(),notes,a.samples(),a.laps(),a.plannedWorkoutId(),a.shoeId());}
    private static ActivityRecord sampledActivity(String id,String provider,String recordId){ActivityRecord a=activity(id,provider,recordId,5000d);List<DistanceSample> points=new ArrayList<>();for(int t=0;t<=1500;t+=10)points.add(new DistanceSample(Duration.ofSeconds(t),t/0.3,null,null,null,false));return new ActivityRecord(a.id(),a.source(),a.start(),a.end(),a.startOffset(),a.endOffset(),a.distanceMetres(),null,null,null,null,null,null,null,points,Collections.emptyList(),null,null);}
    private static UserProfile profile(){return new UserProfile(new RaceGoal(RaceDistance.TEN_K,"Original race",LocalDate.of(2031,4,20),GoalMode.TARGET_TIME,Duration.ofSeconds(3300)),new TrainingPreferences(TrainingStrategy.BALANCED,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.THURSDAY,DayOfWeek.SUNDAY),3,DayOfWeek.SUNDAY,EnumSet.of(DayOfWeek.TUESDAY),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false);}
    private void assertVersionTwoDataRetained(boolean nullable) throws Exception {
        assertEquals("Saved note",find("preserved-run").notes());assertEquals(7d,find("preserved-run").rpe(),0);assertEquals("migration-shoe",find("preserved-run").shoeId());
        assertEquals("",find("cleared-run").notes());
        if(nullable)assertEquals("Provider note",find("provider-run").notes());
        assertEquals("Original race",profiles.load().get().raceGoal().raceName());assertEquals("{\"step\":2}",profiles.loadDraft().get());
        assertEquals("before-upgrade",store.importToken("health-connect"));assertEquals("retained",store.getSetting("sentinel"));
        assertEquals("migration-shoe",store.shoes().get(0).id());assertEquals("official",store.results().get(0).id());
        assertEquals("planned",store.plannedWorkouts().get(0).id());assertEquals(7L,store.plannedWorkoutRevision("planned"));
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
            assertEquals(7,db.getVersion());
            try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE name='activity_annotations_v3'",null)){assertFalse(c.moveToFirst());}
            try(Cursor c=db.rawQuery("PRAGMA table_info(activity_annotations)",null)){while(c.moveToNext())if(c.getString(1).equals("notes"))assertEquals(0,c.getInt(3));}
            db.setForeignKeyConstraintsEnabled(true);
            db.execSQL("DELETE FROM shoes WHERE id='migration-shoe'");
            try(Cursor c=db.rawQuery("SELECT notes,rpe,shoe_id FROM activity_annotations WHERE record_id='preserved'",null)){
                assertTrue(c.moveToFirst());assertEquals("Saved note",c.getString(0));assertEquals(7d,c.getDouble(1),0);assertTrue(c.isNull(2));
            }
        }
    }
    /** Frozen published draft-v2 feature DDL, independent of the production migration code. */
    private void frozenVersionTwo(boolean nullable) throws Exception {
        frozenVersionOne();
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
            db.execSQL("CREATE TABLE activities (id TEXT PRIMARY KEY NOT NULL, provider TEXT NOT NULL, record_id TEXT NOT NULL, origin_package TEXT, start_millis INTEGER NOT NULL, end_millis INTEGER NOT NULL, payload TEXT NOT NULL, UNIQUE(provider, record_id), CHECK(end_millis >= start_millis))");
            db.execSQL("CREATE INDEX activities_start ON activities(start_millis DESC)");
            db.execSQL("CREATE TABLE shoes (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, is_default INTEGER NOT NULL DEFAULT 0 CHECK(is_default IN (0, 1)))");
            db.execSQL("CREATE UNIQUE INDEX shoes_one_default ON shoes(is_default) WHERE is_default = 1");
            db.execSQL("CREATE TABLE activity_annotations (provider TEXT NOT NULL, record_id TEXT NOT NULL, notes TEXT"+(nullable?"":" NOT NULL DEFAULT ''")+", rpe REAL CHECK(rpe BETWEEN 0 AND 10), shoe_id TEXT REFERENCES shoes(id) ON DELETE SET NULL, PRIMARY KEY(provider, record_id))");
            db.execSQL("CREATE TABLE manual_results (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT, distance_code TEXT NOT NULL, distance_metres REAL NOT NULL CHECK(distance_metres > 0), result_date TEXT NOT NULL, payload TEXT NOT NULL)");
            db.execSQL("CREATE TABLE best_efforts (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT NOT NULL REFERENCES activities(id) ON DELETE CASCADE, distance_code TEXT NOT NULL, payload TEXT NOT NULL, UNIQUE(activity_id, distance_code))");
            db.execSQL("CREATE TABLE preserved_best_efforts (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT NOT NULL, distance_code TEXT NOT NULL, provider TEXT NOT NULL, record_id TEXT NOT NULL, origin_package TEXT, payload TEXT NOT NULL, UNIQUE(activity_id, distance_code))");
            db.execSQL("CREATE INDEX preserved_efforts_source ON preserved_best_efforts(provider, record_id)");
            db.execSQL("CREATE TABLE import_checkpoints (provider TEXT PRIMARY KEY NOT NULL, token TEXT, updated_at TEXT NOT NULL)");
            db.execSQL("CREATE TABLE baseline_state (id INTEGER PRIMARY KEY CHECK(id = 1), manual_json TEXT NOT NULL, manual_recorded_at TEXT, reviewed INTEGER NOT NULL DEFAULT 0 CHECK(reviewed IN (0, 1)))");
            db.execSQL("CREATE TABLE settings (setting_key TEXT PRIMARY KEY NOT NULL, setting_value TEXT NOT NULL)");
            db.execSQL("CREATE TABLE assessment_results (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT, payload TEXT NOT NULL)");
            db.execSQL("CREATE TABLE planned_workouts (id TEXT PRIMARY KEY NOT NULL, scheduled_date TEXT NOT NULL, revision INTEGER NOT NULL CHECK(revision > 0), payload TEXT NOT NULL)");
            db.execSQL("CREATE TABLE training_plans (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL)");
            db.execSQL("CREATE TABLE plan_adjustments (id TEXT PRIMARY KEY NOT NULL, plan_id TEXT, user_decision TEXT, payload TEXT NOT NULL)");
            db.execSQL("INSERT INTO shoes VALUES (?,?,1)",new Object[]{"migration-shoe",FeatureJson.shoe(shoe("migration-shoe",true)).toString()});
            for(String id:nullable?new String[]{"preserved","cleared","provider"}:new String[]{"preserved","cleared"}){
                ActivityRecord run=withNotes(activity(id+"-run","health-connect",id,5000d),"Provider note");
                db.execSQL("INSERT INTO activities VALUES (?,?,?,?,?,?,?)",new Object[]{run.id(),run.source().provider(),run.source().recordId(),run.source().originPackage(),run.start().toEpochMilli(),run.end().toEpochMilli(),FeatureJson.activity(run).toString()});
            }
            db.execSQL("INSERT INTO activity_annotations VALUES ('health-connect','preserved','Saved note',7,'migration-shoe')");
            db.execSQL("INSERT INTO activity_annotations VALUES ('health-connect','cleared','',NULL,NULL)");
            if(nullable)db.execSQL("INSERT INTO activity_annotations VALUES ('health-connect','provider',NULL,NULL,NULL)");
            RecordedResult result=new RecordedResult("official",PerformanceDistance.FIVE_K,Duration.ofMinutes(27),LocalDate.of(2030,1,1),null,ResultSource.OFFICIAL_CHIP,"preserved-run",null);
            db.execSQL("INSERT INTO manual_results VALUES (?,?,?,?,?,?)",new Object[]{result.id(),result.activityId(),result.distance().code(),result.distance().metres(),result.date().toString(),FeatureJson.result(result).toString()});
            PlannedWorkout planned=new PlannedWorkout("planned",LocalDate.of(2030,2,1),WorkoutCategory.EASY_RUN,5d,null,Collections.emptyList(),null);
            db.execSQL("INSERT INTO planned_workouts VALUES ('planned','2030-02-01',7,?)",new Object[]{FeatureJson.planned(planned).toString()});
            db.execSQL("INSERT INTO import_checkpoints VALUES ('health-connect','before-upgrade','2030-01-01T12:00:00Z')");
            db.execSQL("INSERT INTO settings VALUES ('sentinel','retained')");db.setVersion(2);
        }
    }
    /** Frozen Phase 1 schema fixture, independent of the migration implementation. */
    private void frozenVersionOne(){
        store.close();profiles.close();getContext().deleteDatabase(ProfileDatabase.NAME);
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){
            db.execSQL("CREATE TABLE user_profile (id INTEGER PRIMARY KEY CHECK(id = 1), performance_eligible INTEGER NOT NULL CHECK(performance_eligible IN (0, 1)))");
            db.execSQL("CREATE TABLE race_goal (user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE, distance TEXT NOT NULL, race_name TEXT NOT NULL, race_date TEXT, goal_mode TEXT NOT NULL, target_seconds INTEGER CHECK(target_seconds > 0))");
            db.execSQL("CREATE TABLE training_preferences (user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE, strategy TEXT NOT NULL, runs_per_week INTEGER NOT NULL CHECK(runs_per_week BETWEEN 1 AND 7), long_run_day INTEGER CHECK(long_run_day BETWEEN 1 AND 7), schedule_type TEXT NOT NULL, units TEXT NOT NULL)");
            db.execSQL("CREATE TABLE availability_day (user_id INTEGER NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE, kind TEXT NOT NULL CHECK(kind IN ('RUN', 'GYM')), day INTEGER NOT NULL CHECK(day BETWEEN 1 AND 7), PRIMARY KEY(user_id, kind, day))");
            db.execSQL("CREATE TABLE onboarding_draft (id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL)");
            db.execSQL("INSERT INTO user_profile VALUES(1,0)");db.execSQL("INSERT INTO race_goal VALUES(1,'TEN_K','Original race','2031-04-20','TARGET_TIME',3300)");db.execSQL("INSERT INTO training_preferences VALUES(1,'BALANCED',3,7,'FLEXIBLE','METRIC')");db.execSQL("INSERT INTO availability_day VALUES(1,'RUN',2),(1,'RUN',4),(1,'RUN',7),(1,'GYM',2)");db.execSQL("INSERT INTO onboarding_draft VALUES(1,'{\"step\":2}')");db.setVersion(1);
        }
    }
}
