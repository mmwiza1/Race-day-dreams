package org.racedaydreams.data;

import static org.racedaydreams.data.FeatureJson.*;
import org.json.*;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.*;

/** Explicit version-one coaching payloads, shared by the database and local backup. */
final class CoachingJson {
    private CoachingJson() { }
    private static String prose(JSONObject j,String key)throws JSONException {String s=text(j,key);if(s==null||s.length()>100000)throw new IllegalArgumentException("Invalid coaching text: "+key);return s;}
    static JSONObject exercise(StrengthExercise e) { return json("id", e.id(), "name", e.name(), "region", e.region().name(), "mode", e.mode().name(), "estimate", e.estimateOneRepMax(), "measurement", e.measurement().name()); }
    static StrengthExercise exercise(JSONObject j) throws JSONException { return new StrengthExercise(required(j,"id"), required(j,"name"), StrengthExercise.Region.valueOf(required(j,"region")), StrengthExercise.Mode.valueOf(required(j,"mode")), bool(j,"estimate"), j.has("measurement") ? StrengthExercise.MeasurementProfile.valueOf(required(j,"measurement")) : (StrengthExercise.Mode.valueOf(required(j,"mode")) == StrengthExercise.Mode.BODYWEIGHT ? StrengthExercise.MeasurementProfile.REPS_ONLY : StrengthExercise.MeasurementProfile.LOAD_REPS)); }
    static JSONObject strength(StrengthSession s) {
        JSONArray exercises = new JSONArray();
        for (var e : s.exercises()) {
            JSONArray sets = new JSONArray();
            for (var set : e.sets()) sets.put(json("id",set.id(),"reps",set.reps(),"kg",set.kilograms(),"rpe",set.rpe(),"warmUp",set.warmUp(),"seconds",set.durationSeconds(),"metres",set.distanceMetres(),"rir",set.repetitionsInReserve()));
            exercises.put(json("exercise",exercise(e.exercise()),"sets",sets));
        }
        return json("id",s.id(),"startedAt",s.startedAt().toString(),"offset",s.offset().toString(),"duration",s.duration().toString(),"rpe",s.rpe(),"notes",s.notes(),"exercises",exercises);
    }
    static StrengthSession strength(JSONObject j) throws JSONException {
        List<StrengthSession.ExerciseSets> exercises = new ArrayList<>(); JSONArray rows = j.getJSONArray("exercises");
        if (rows.length() > 100) throw new IllegalArgumentException("Too many exercises.");
        for (int i=0;i<rows.length();i++) { JSONObject e=rows.getJSONObject(i); JSONArray sets=e.getJSONArray("sets");
            if (sets.length()>100) throw new IllegalArgumentException("Too many sets.");
            List<StrengthSession.SetEntry> values=new ArrayList<>();
            for(int k=0;k<sets.length();k++) {JSONObject v=sets.getJSONObject(k);values.add(new StrengthSession.SetEntry(required(v,"id"),requiredInteger(v,"reps"),requiredNumber(v,"kg"),number(v,"rpe"),bool(v,"warmUp"),v.isNull("seconds")?null:requiredLong(v,"seconds"),number(v,"metres"),number(v,"rir")));}
            exercises.add(new StrengthSession.ExerciseSets(exercise(e.getJSONObject("exercise")),values));
        }
        return new StrengthSession(required(j,"id"),Instant.parse(required(j,"startedAt")),ZoneOffset.of(required(j,"offset")),Duration.parse(required(j,"duration")),number(j,"rpe"),prose(j,"notes"),exercises);
    }
    static JSONObject version(PlanVersion v) {
        JSONArray phases=new JSONArray(), workouts=new JSONArray();
        for(var p:v.plan().phases())phases.put(json("kind",p.kind().name(),"start",p.startDate().toString(),"end",p.endDate().toString(),"explanation",p.explanation()));
        for(var w:v.plan().workouts())workouts.put(planned(w));
        return json("id",v.id(),"planId",v.planId(),"version",v.version(),"createdAt",v.createdAt().toString(),"reason",v.reason(),"goal",profile(v.goalSnapshot()),"evidence",v.evidenceSnapshot(),
            "start",v.plan().startDate().toString(),"end",v.plan().endDate().toString(),"phases",phases,"workouts",workouts);
    }
    static PlanVersion version(JSONObject j) throws JSONException {
        List<TrainingPhase> phases=new ArrayList<>();List<PlannedWorkout> workouts=new ArrayList<>();
        JSONArray a=j.getJSONArray("phases"),b=j.getJSONArray("workouts");
        if(a.length()>1000||b.length()>6000)throw new IllegalArgumentException("Plan is too large.");
        for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);phases.add(new TrainingPhase(TrainingPhase.Kind.valueOf(required(p,"kind")),LocalDate.parse(required(p,"start")),LocalDate.parse(required(p,"end")),prose(p,"explanation")));}
        Set<String> ids=new HashSet<>();Set<LocalDate> dates=new HashSet<>();
        for(int i=0;i<b.length();i++){var w=planned(b.getJSONObject(i));if(!ids.add(w.id())||!dates.add(w.date()))throw new IllegalArgumentException("Duplicated workout placement.");workouts.add(w);}
        String planId=required(j,"planId");
        return new PlanVersion(required(j,"id"),planId,requiredInteger(j,"version"),Instant.parse(required(j,"createdAt")),prose(j,"reason"),profile(j.getJSONObject("goal")),prose(j,"evidence"),
            new TrainingPlan(planId,LocalDate.parse(required(j,"start")),LocalDate.parse(required(j,"end")),phases,workouts));
    }
    static JSONObject response(RunExecution r) {return json("activityId",r.activityId(),"plannedWorkoutId",r.plannedWorkoutId(),"planVersionId",r.planVersionId(),"completedReps",r.completedReps(),"recoveriesCompleted",r.recoveriesCompleted(),"notes",r.responseNotes());}
    static RunExecution response(JSONObject j)throws JSONException{return new RunExecution(required(j,"activityId"),text(j,"plannedWorkoutId"),text(j,"planVersionId"),integer(j,"completedReps"),j.isNull("recoveriesCompleted")?null:bool(j,"recoveriesCompleted"),prose(j,"notes"));}
    static JSONObject proposal(AdjustmentProposal p){JSONArray evidence=new JSONArray(p.evidence()),options=new JSONArray();for(var o:p.options())options.put(json("id",o.id(),"label",o.label(),"action",o.action().name(),"replacement",o.replacement()==null?null:planned(o.replacement())));
        return json("id",p.id(),"baseVersionId",p.baseVersionId(),"workoutId",p.workoutId(),"createdAt",p.createdAt().toString(),"reason",p.reason(),"evidence",evidence,"impact",p.impact(),"confidence",p.confidence(),"options",options);}
    static AdjustmentProposal proposal(JSONObject j)throws JSONException{List<String> evidence=new ArrayList<>();List<AdjustmentProposal.Option> options=new ArrayList<>();JSONArray a=j.getJSONArray("evidence"),b=j.getJSONArray("options");
        if(a.length()>100||b.length()>20)throw new IllegalArgumentException("Proposal is too large.");
        for(int i=0;i<a.length();i++)evidence.add(a.getString(i));for(int i=0;i<b.length();i++){JSONObject o=b.getJSONObject(i);options.add(new AdjustmentProposal.Option(required(o,"id"),required(o,"label"),AdjustmentProposal.Action.valueOf(required(o,"action")),o.isNull("replacement")?null:planned(o.getJSONObject("replacement"))));}
        if(options.isEmpty()||options.stream().noneMatch(o->o.action()==AdjustmentProposal.Action.KEEP))throw new IllegalArgumentException("Proposal must preserve runner choice.");
        return new AdjustmentProposal(required(j,"id"),required(j,"baseVersionId"),required(j,"workoutId"),Instant.parse(required(j,"createdAt")),prose(j,"reason"),evidence,prose(j,"impact"),prose(j,"confidence"),options);}
    static JSONObject feedback(RunFeedback f){JSONArray c=new JSONArray();for(var component:f.components())c.put(json("name",component.name(),"fraction",component.fraction(),"explanation",component.explanation()));return json("ruleVersion",RunScoreEngine.RULE_VERSION,"score",f.score(),"label",f.label(),"components",c,"wentWell",new JSONArray(f.wentWell()),"differed",new JSONArray(f.differed()),"learned",f.learned(),"nextStep",f.nextStep());}
}
