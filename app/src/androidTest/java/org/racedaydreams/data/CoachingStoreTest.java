package org.racedaydreams.data;

import android.test.AndroidTestCase;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.*;

@SuppressWarnings("deprecation")
public final class CoachingStoreTest extends AndroidTestCase {
    CoachingStore coaching; FeatureStore features; SqliteUserPreferencesRepository profiles;
    @Override protected void setUp()throws Exception{super.setUp();ProfileDatabase.get(getContext()).close();getContext().deleteDatabase(ProfileDatabase.NAME);coaching=new CoachingStore(getContext());features=new FeatureStore(getContext());profiles=new SqliteUserPreferencesRepository(getContext());}
    @Override protected void tearDown()throws Exception{profiles.clear();super.tearDown();}
    private PlanVersion version(){
        LocalDate date=LocalDate.now().minusDays(1);
        UserProfile p=new UserProfile(new RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),new TrainingPreferences(TrainingStrategy.ZONE_2,EnumSet.allOf(DayOfWeek.class),3,DayOfWeek.SUNDAY,EnumSet.noneOf(DayOfWeek.class),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false);
        PlannedWorkout w=new PlannedWorkout("workout",date,WorkoutCategory.EASY_RUN,5.0,null,Collections.singletonList(new WorkoutSegment(WorkoutSegment.Kind.RUN,5.0,null,null,null,null,3.0,"Conversational")),"Original prescription");
        return new PlanVersion("v1","plan",1,Instant.now(),"Runner requested plan",p,"No fabricated evidence",new TrainingPlan("plan",date,date.plusWeeks(12),Collections.singletonList(new TrainingPhase(TrainingPhase.Kind.ENDURANCE_DEVELOPMENT,date,date.plusWeeks(12),"Actual horizon")),Collections.singletonList(w)));
    }
    private StrengthSession strength(){return new StrengthSession("s",Instant.now().minus(Duration.ofDays(1)),ZoneOffset.UTC,Duration.ofMinutes(50),8.0,"",Collections.singletonList(new StrengthSession.ExerciseSets(StrengthExercise.starters().get(0),Arrays.asList(new StrengthSession.SetEntry("set2",5,90,8.0,false),new StrengthSession.SetEntry("set1",8,80,7.0,false)))));}
    public void testStrengthOrderingCustomExerciseAndDeletion(){
        var e=new StrengthExercise("custom","My lift",StrengthExercise.Region.FULL,StrengthExercise.Mode.WEIGHTED,false);coaching.saveExercise(e);assertTrue(coaching.exercises().contains(e));
        var session=strength();coaching.saveStrength(session);assertEquals(session,coaching.strength().get(0));assertEquals("set2",coaching.strength().get(0).exercises().get(0).sets().get(0).id());assertFalse(StrengthAnalytics.progression(coaching.strength()).isEmpty());
        coaching.deleteStrength(session.id());assertTrue(StrengthAnalytics.progression(coaching.strength()).isEmpty());
    }
    public void testVersionRoundTripOneCurrentAndReminderRevision(){
        var v=version();coaching.saveCurrent(v);assertEquals(v,coaching.current());assertEquals(1,features.plannedWorkouts().size());long revision=features.plannedWorkoutRevision("workout");
        var v2=new PlanVersion("v2",v.planId(),2,Instant.now(),"Runner retained schedule",v.goalSnapshot(),v.evidenceSnapshot(),v.plan());coaching.saveCurrent(v2);
        assertEquals(v2,coaching.current());assertEquals(2,coaching.versions().size());assertEquals(revision,features.plannedWorkoutRevision("workout"));
        try{coaching.saveCurrent(v2);fail("Immutable duplicate version must fail");}catch(android.database.SQLException expected){assertEquals(v2,coaching.current());}
    }
    public void testProposalDecisionAndPreservedOriginal(){
        var v=version();coaching.saveCurrent(v);var original=v.plan().workouts().get(0);
        var proposal=new AdjustmentProposal("p",v.id(),original.id(),Instant.now(),"Two supporting observations",Arrays.asList("RPE high","Completion low"),"One workout only","Moderate",Arrays.asList(new AdjustmentProposal.Option("keep","Keep original",AdjustmentProposal.Action.KEEP,null),new AdjustmentProposal.Option("rest","Rest",AdjustmentProposal.Action.REST,null)));
        coaching.saveProposal(proposal);assertEquals(proposal,coaching.proposals().get(0));
        coaching.decide(proposal,"rest",Collections.emptySet());assertEquals(2,coaching.current().version());assertTrue(coaching.current().plan().workouts().isEmpty());assertTrue(features.plannedWorkouts().isEmpty());assertEquals(original,coaching.versions().get(1).plan().workouts().get(0));assertTrue(coaching.proposals().isEmpty());
        try{coaching.decide(proposal,"keep",Collections.emptySet());fail("Stale proposal must fail");}catch(IllegalArgumentException expected){assertEquals(2,coaching.current().version());}
    }
    public void testCoachingBackupRoundTripAndInvalidGraphRollback()throws Exception{
        var v=version();coaching.saveCurrent(v);coaching.saveStrength(strength());
        var response=new RunExecution("external-pending-reimport","workout",v.id(),null,null,"");coaching.saveResponse(response);
        var backup=features.exportUserData();FeatureStore.validateUserData(backup);features.restoreUserData(backup,false);assertEquals(v,coaching.current());assertEquals(strength().exercises(),coaching.strength().get(0).exercises());assertEquals(response,coaching.responses().get(0));
        var invalid=new JSONObject(backup.toString());invalid.getJSONObject("coaching").getJSONArray("coach_current_plan").getJSONObject(0).put("version_id","missing");
        try{features.restoreUserData(invalid,false);fail("Invalid graph must fail before changes");}catch(IllegalArgumentException expected){assertEquals(v,coaching.current());assertEquals(1,coaching.strength().size());}
    }
    public void testLegacyBackupClearsCoachingWithoutCorruption()throws Exception{
        coaching.saveCurrent(version());JSONObject legacy=features.exportUserData();legacy.remove("coaching");features.restoreUserData(legacy,false);assertNull(coaching.current());assertTrue(coaching.versions().isEmpty());
    }
    public void testCompletedPrescriptionCannotBeOverwrittenByRebuild(){
        var v=version();coaching.saveCurrent(v);coaching.saveResponse(new RunExecution("completed","workout",v.id(),null,null,""));
        var w=v.plan().workouts().get(0);var changed=new PlannedWorkout(w.id(),w.date(),w.category(),3.0,null,w.segments(),"Changed target");
        var candidate=new TrainingPlan(v.planId(),v.plan().startDate(),v.plan().endDate(),v.plan().phases(),Collections.singletonList(changed));
        var next=new PlanVersion("rebuild",v.planId(),2,Instant.now(),"Rebuild",v.goalSnapshot(),v.evidenceSnapshot(),candidate);
        try{coaching.saveCurrent(next);fail("Completed targets must be immutable");}catch(IllegalArgumentException expected){assertEquals(v,coaching.current());assertEquals(1,coaching.versions().size());}
        var preserved=PlanPreservation.retainCompleted(candidate,Collections.singletonMap(w.id(),w));
        coaching.saveCurrent(new PlanVersion("preserved",v.planId(),2,Instant.now(),"Rebuild with preserved completion",v.goalSnapshot(),v.evidenceSnapshot(),preserved));
        assertEquals(w,coaching.current().plan().workouts().get(0));
    }
    public void testVersionThreeMigrationRetainsExistingActivityAndShoe(){
        var shoe=new Shoe("shoe","Brand","Model",LocalDate.now().minusDays(20),0.0,false,"",true);
        features.saveShoe(shoe);ProfileDatabase.get(getContext()).close();
        try(SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){for(String t:new String[]{"coach_support","recording_points","recording_events","recording_sessions","health_observations"})db.execSQL("DROP TABLE "+t);db.execSQL("DROP TABLE gym_progression_decisions");db.execSQL("DROP TABLE gym_current");db.execSQL("DROP TABLE gym_plan_versions");db.execSQL("DROP TABLE pb_acknowledgements");db.execSQL("DROP TABLE strength_user_state");db.execSQL("DROP TABLE coach_evidence");CoachingStore.clear(db);for(String table:CoachingStore.TABLES)db.execSQL("DROP TABLE "+table);db.setVersion(3);}
        assertEquals(shoe,features.shoes().get(0));assertNull(coaching.current());assertTrue(coaching.strength().isEmpty());
    }
}
