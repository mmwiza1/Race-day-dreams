package org.racedaydreams.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.baseline.ManualBaselineInput;
import org.racedaydreams.domain.baseline.AssessmentResult;
import org.racedaydreams.domain.baseline.BaselineAssessment;
import org.racedaydreams.domain.baseline.AssessmentMode;
import java.time.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Explicit, version-controlled JSON representation. No reflective or provider SDK serialization. */
final class FeatureJson {
    private FeatureJson() {}
    static JSONObject object(String value) {
        try {
            JSONTokener tokener = new JSONTokener(value);
            Object parsed = tokener.nextValue();
            if (!(parsed instanceof JSONObject object) || tokener.nextClean() != 0) {
                throw new IllegalArgumentException("Expected one complete JSON object.");
            }
            return object;
        }
        catch (JSONException e) { throw invalid(e); }
    }
    static JSONObject json(Object... pairs) {
        try {
            JSONObject out = new JSONObject();
            for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], pairs[i + 1] == null ? JSONObject.NULL : pairs[i + 1]);
            return out;
        } catch (JSONException e) { throw invalid(e); }
    }
    static String text(JSONObject j, String key) throws JSONException {
        if (j.isNull(key)) return null;
        if (!(j.get(key) instanceof String)) throw new IllegalArgumentException("Expected text for " + key);
        return j.getString(key);
    }
    static Double number(JSONObject j, String key) throws JSONException {
        if(j.isNull(key))return null;
        if(!(j.get(key) instanceof Number))throw new IllegalArgumentException("Expected a number for "+key);
        return finite(j.getDouble(key));
    }
    static Integer integer(JSONObject j, String key) throws JSONException {
        Double n=number(j,key);if(n==null)return null;
        if(n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new IllegalArgumentException("Expected a whole number for "+key);
        return n.intValue();
    }
    static int requiredInteger(JSONObject j,String key)throws JSONException{Integer n=integer(j,key);if(n==null)throw new IllegalArgumentException("Missing whole number for "+key);return n;}
    static long requiredLong(JSONObject j, String key) throws JSONException {
        Object value = j.get(key);
        if (!(value instanceof Integer) && !(value instanceof Long)) {
            throw new IllegalArgumentException("Expected a whole number for " + key);
        }
        return ((Number) value).longValue();
    }
    static double requiredNumber(JSONObject j, String key) throws JSONException {
        Double value = number(j, key);
        if (value == null) throw new IllegalArgumentException("Missing number for " + key);
        return value;
    }
    static boolean bool(JSONObject j, String key) throws JSONException {
        Object value = j.get(key);
        if (!(value instanceof Boolean)) throw new IllegalArgumentException("Expected true or false for " + key);
        return (Boolean) value;
    }
    static Double finite(double value) { if (!Double.isFinite(value)) throw new IllegalArgumentException("A number is not finite."); return value; }
    static String string(Object value) { return value == null ? null : value.toString(); }
    static Duration duration(JSONObject j, String key) throws JSONException { String value = text(j, key); return value == null ? null : Duration.parse(value); }
    static String required(JSONObject j, String key) throws JSONException {
        String value = text(j, key);
        if (value == null || value.trim().isEmpty() || value.length() > 256) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }
    static IllegalArgumentException invalid(Exception e) { return new IllegalArgumentException("Stored or backup data is invalid. No data was replaced.", e); }

    static JSONObject activity(ActivityRecord a) {
        JSONArray samples = new JSONArray(), laps = new JSONArray();
        for (DistanceSample s : a.samples()) samples.put(json("elapsed", string(s.elapsed()), "distanceMetres", s.distanceMetres(), "heartRate", s.heartRate(), "cadence", s.cadence(), "elevationMetres", s.elevationMetres(), "discontinuity", s.discontinuity()));
        for (ActivityLap l : a.laps()) laps.put(json("id", l.id(), "start", string(l.start()), "end", string(l.end()), "distanceMetres", l.distanceMetres(), "label", l.label(), "plannedSegmentId", l.plannedSegmentId()));
        return json("id", a.id(), "source", json("provider", a.source().provider(), "recordId", a.source().recordId(), "originPackage", a.source().originPackage()),
                "start", string(a.start()), "end", string(a.end()), "startOffset", string(a.startOffset()), "endOffset", string(a.endOffset()),
                "distanceMetres", a.distanceMetres(), "movingTime", string(a.movingTime()), "averageHeartRate", a.averageHeartRate(), "maxHeartRate", a.maxHeartRate(),
                "elevationGainMetres", a.elevationGainMetres(), "cadence", a.cadence(), "rpe", a.rpe(), "notes", a.notes(), "samples", samples, "laps", laps,
                "plannedWorkoutId", a.plannedWorkoutId(), "shoeId", a.shoeId());
    }
    static ActivityRecord activity(JSONObject j) {
        try {
            JSONObject source = j.getJSONObject("source");
            List<DistanceSample> samples = new ArrayList<>();
            JSONArray points = j.getJSONArray("samples");
            if (points.length() > 200_000) throw new IllegalArgumentException("Too many activity samples.");
            for (int i = 0; i < points.length(); i++) {
                JSONObject s = points.getJSONObject(i);
                samples.add(new DistanceSample(Duration.parse(s.getString("elapsed")), requiredNumber(s,"distanceMetres"), number(s,"heartRate"), number(s,"cadence"), number(s,"elevationMetres"), bool(s,"discontinuity")));
            }
            List<ActivityLap> laps = new ArrayList<>();
            JSONArray lapRows = j.getJSONArray("laps");
            if (lapRows.length() > 10_000) throw new IllegalArgumentException("Too many activity laps.");
            for (int i = 0; i < lapRows.length(); i++) {
                JSONObject l = lapRows.getJSONObject(i);
                laps.add(new ActivityLap(required(l,"id"), Instant.parse(l.getString("start")), Instant.parse(l.getString("end")), number(l,"distanceMetres"), text(l,"label"), text(l,"plannedSegmentId")));
            }
            return new ActivityRecord(required(j,"id"), new SourceIdentity(required(source,"provider"), required(source,"recordId"), text(source,"originPackage")),
                    Instant.parse(j.getString("start")), Instant.parse(j.getString("end")), text(j,"startOffset") == null ? null : ZoneOffset.of(j.getString("startOffset")),
                    text(j,"endOffset") == null ? null : ZoneOffset.of(j.getString("endOffset")), number(j,"distanceMetres"), duration(j,"movingTime"), number(j,"averageHeartRate"), number(j,"maxHeartRate"),
                    number(j,"elevationGainMetres"), number(j,"cadence"), number(j,"rpe"), text(j,"notes"), samples, laps, text(j,"plannedWorkoutId"), text(j,"shoeId"));
        } catch (JSONException | DateTimeException e) { throw invalid(e); }
    }
    static JSONObject result(RecordedResult r) {
        return json("id", r.id(), "distance", json("code", r.distance().code(), "metres", r.distance().metres(), "label", r.distance().label()),
                "duration", string(r.duration()), "date", string(r.date()), "eventName", r.eventName(), "source", r.source().name(), "activityId", r.activityId(), "note", r.note());
    }
    static RecordedResult result(JSONObject j) {
        try {
            JSONObject d = j.getJSONObject("distance");
            return new RecordedResult(required(j,"id"), new PerformanceDistance(required(d,"code"), requiredNumber(d,"metres"), required(d,"label")),
                    Duration.parse(j.getString("duration")), LocalDate.parse(j.getString("date")), text(j,"eventName"), ResultSource.valueOf(j.getString("source")), text(j,"activityId"), text(j,"note"));
        } catch (JSONException | DateTimeException e) { throw invalid(e); }
    }
    static JSONObject shoe(Shoe s) { return json("id",s.id(),"brand",s.brand(),"model",s.model(),"purchaseDate",string(s.purchaseDate()),"startingDistanceMetres",s.startingDistanceMetres(),"retired",s.retired(),"notes",s.notes(),"defaultShoe",s.defaultShoe()); }
    static Shoe shoe(JSONObject j) {
        try { return new Shoe(required(j,"id"), text(j,"brand"), required(j,"model"), text(j,"purchaseDate") == null ? null : LocalDate.parse(j.getString("purchaseDate")), number(j,"startingDistanceMetres"), bool(j,"retired"), text(j,"notes"), bool(j,"defaultShoe")); }
        catch (JSONException | DateTimeException e) { throw invalid(e); }
    }
    static JSONObject baseline(ManualBaselineInput b) { return json("weeklyDistanceKm",b.weeklyDistanceKm(),"runsPerWeek",b.runsPerWeek(),"longestRunKm",b.longestRunKm(),"recent5k",string(b.recent5k()),"recent10k",string(b.recent10k()),"recentHalf",string(b.recentHalf()),"easyPaceSecondsPerKm",b.easyPaceSecondsPerKm(),"consistentWeeks",b.consistentWeeks()); }
    static ManualBaselineInput baseline(JSONObject j) {
        try { return new ManualBaselineInput(number(j,"weeklyDistanceKm"), number(j,"runsPerWeek"), number(j,"longestRunKm"), duration(j,"recent5k"), duration(j,"recent10k"), duration(j,"recentHalf"), number(j,"easyPaceSecondsPerKm"), integer(j,"consistentWeeks")); }
        catch (JSONException | DateTimeException e) { throw invalid(e); }
    }
    static JSONObject assessment(AssessmentResult a){return json("id",a.id(),"activityId",a.activityId(),"distanceKm",a.assessment().distanceKm(),"mode",a.assessment().mode().name(),"duration",string(a.duration()),"rpe",a.rpe(),"completedAt",string(a.completedAt()));}
    static AssessmentResult assessment(JSONObject j){
        try{return new AssessmentResult(required(j,"id"),new BaselineAssessment(requiredInteger(j,"distanceKm"),AssessmentMode.valueOf(j.getString("mode"))),Duration.parse(j.getString("duration")),number(j,"rpe"),Instant.parse(j.getString("completedAt")),text(j,"activityId"));}
        catch(JSONException | DateTimeException e){throw invalid(e);}
    }
    static JSONObject profile(UserProfile p) {
        RaceGoal g = p.raceGoal(); TrainingPreferences t = p.preferences();
        JSONArray running = new JSONArray(), gym = new JSONArray();
        for (DayOfWeek d : t.runningDays()) running.put(d.name());
        for (DayOfWeek d : t.gymDays()) gym.put(d.name());
        return json("goal", json("distance",g.distance().name(),"raceName",g.raceName(),"raceDate",string(g.raceDate()),"mode",g.mode().name(),"targetTime",string(g.targetTime())),
                "preferences", json("strategy",t.strategy().name(),"runningDays",running,"runsPerWeek",t.runsPerWeek(),"longRunDay",string(t.longRunDay()),"gymDays",gym,"scheduleType",t.scheduleType().name(),"units",t.units().name()), "performanceEligible",p.performanceEligible());
    }
    static UserProfile profile(JSONObject j) {
        try {
            JSONObject g = j.getJSONObject("goal"), t = j.getJSONObject("preferences");
            EnumSet<DayOfWeek> running = days(t.getJSONArray("runningDays")), gym = days(t.getJSONArray("gymDays"));
            return new UserProfile(new RaceGoal(RaceDistance.valueOf(g.getString("distance")), text(g,"raceName"), text(g,"raceDate") == null ? null : LocalDate.parse(g.getString("raceDate")), GoalMode.valueOf(g.getString("mode")), duration(g,"targetTime")),
                    new TrainingPreferences(TrainingStrategy.valueOf(t.getString("strategy")), running, requiredInteger(t,"runsPerWeek"), text(t,"longRunDay") == null ? null : DayOfWeek.valueOf(t.getString("longRunDay")), gym, ScheduleType.valueOf(t.getString("scheduleType")), UnitSystem.valueOf(t.getString("units"))), bool(j,"performanceEligible"));
        } catch (JSONException | DateTimeException e) { throw invalid(e); }
    }
    private static EnumSet<DayOfWeek> days(JSONArray j) throws JSONException {
        EnumSet<DayOfWeek> out = EnumSet.noneOf(DayOfWeek.class);
        for (int i=0; i<j.length(); i++) if (!out.add(DayOfWeek.valueOf(j.getString(i)))) throw new IllegalArgumentException("Duplicate availability day.");
        return out;
    }
    static JSONObject planned(PlannedWorkout w) {
        JSONArray segments = new JSONArray();
        for (WorkoutSegment s : w.segments()) segments.put(json("kind",s.kind().name(),"distanceKm",s.distanceKm(),"duration",string(s.duration()),"minimumSecondsPerKm",s.minimumSecondsPerKm(),"maximumSecondsPerKm",s.maximumSecondsPerKm(),"heartRateZone",s.heartRateZone(),"rpe",s.rpe(),"guidance",s.guidance()));
        return json("id",w.id(),"date",string(w.date()),"category",w.category().name(),"distanceKm",w.distanceKm(),"duration",string(w.duration()),"segments",segments,"note",w.note());
    }
    static PlannedWorkout planned(JSONObject j) {
        try {
            List<WorkoutSegment> segments = new ArrayList<>(); JSONArray rows = j.getJSONArray("segments");
            if (rows.length()>1000) throw new IllegalArgumentException("Too many workout segments.");
            for(int i=0;i<rows.length();i++) { JSONObject s=rows.getJSONObject(i); segments.add(new WorkoutSegment(WorkoutSegment.Kind.valueOf(s.getString("kind")),number(s,"distanceKm"),duration(s,"duration"),number(s,"minimumSecondsPerKm"),number(s,"maximumSecondsPerKm"),integer(s,"heartRateZone"),number(s,"rpe"),text(s,"guidance"))); }
            return new PlannedWorkout(required(j,"id"),LocalDate.parse(j.getString("date")),WorkoutCategory.valueOf(j.getString("category")),number(j,"distanceKm"),duration(j,"duration"),segments,text(j,"note"));
        } catch(JSONException | DateTimeException e) {throw invalid(e);}
    }
}
