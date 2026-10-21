package org.racedaydreams.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.GoalMode;
import org.racedaydreams.domain.PaceCalculator;
import org.racedaydreams.domain.RaceDistance;
import org.racedaydreams.domain.RaceGoal;
import org.racedaydreams.domain.ScheduleType;
import org.racedaydreams.domain.TrainingPreferences;
import org.racedaydreams.domain.TrainingStrategy;
import org.racedaydreams.domain.UnitSystem;
import org.racedaydreams.domain.UserProfile;

import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.EnumSet;

/** Editable presentation state. Incomplete input is never stored as a valid domain profile. */
final class OnboardingDraft {
    private static final int VERSION = 1;
    int step;
    RaceDistance distance = RaceDistance.FIVE_K;
    boolean event;
    String raceName = "";
    LocalDate raceDate;
    GoalMode goalMode = GoalMode.FINISH;
    String targetTime = "";
    TrainingStrategy strategy = TrainingStrategy.DISTANCE_FIRST;
    EnumSet<DayOfWeek> runningDays = EnumSet.of(
            DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
    int runsPerWeek = 3;
    DayOfWeek longRunDay = DayOfWeek.SATURDAY;
    EnumSet<DayOfWeek> gymDays = EnumSet.noneOf(DayOfWeek.class);
    ScheduleType scheduleType = ScheduleType.FLEXIBLE;
    UnitSystem units = UnitSystem.METRIC;
    final boolean performanceEligible;
    boolean invalidDraft;

    OnboardingDraft(UserProfile initial, String savedJson) {
        performanceEligible = initial != null && initial.performanceEligible();
        if (initial != null) {
            RaceGoal goal = initial.raceGoal();
            TrainingPreferences preferences = initial.preferences();
            distance = goal.distance();
            event = goal.raceDate() != null;
            raceName = goal.raceName();
            raceDate = goal.raceDate();
            goalMode = goal.mode();
            targetTime = goal.targetTime() == null ? "" : PaceCalculator.formatTime(goal.targetTime());
            strategy = preferences.strategy();
            runningDays.clear();
            runningDays.addAll(preferences.runningDays());
            runsPerWeek = preferences.runsPerWeek();
            longRunDay = preferences.longRunDay();
            gymDays.addAll(preferences.gymDays());
            scheduleType = preferences.scheduleType();
            units = preferences.units();
        }
        if (savedJson != null && !savedJson.trim().isEmpty()) {
            try {
                read(new JSONObject(savedJson));
            } catch (JSONException | IllegalArgumentException | DateTimeException exception) {
                // The valid profile remains untouched. Root may show this draft error to the user.
                invalidDraft = true;
                step = 0;
            }
        }
        if (!performanceEligible && strategy == TrainingStrategy.PERFORMANCE) {
            strategy = TrainingStrategy.BALANCED;
        }
    }

    private void read(JSONObject source) throws JSONException {
        if (source.getInt("version") != VERSION) {
            throw new IllegalArgumentException("Unsupported onboarding draft version");
        }
        // Parse into locals before changing state, so malformed drafts do not partly overwrite it.
        int savedStep = source.getInt("step");
        if (savedStep < 0 || savedStep > 3) throw new IllegalArgumentException("Invalid step");
        RaceDistance savedDistance = RaceDistance.valueOf(source.getString("distance"));
        boolean savedEvent = source.getBoolean("event");
        String savedName = source.getString("raceName");
        LocalDate savedDate = source.isNull("raceDate")
                ? null : LocalDate.parse(source.getString("raceDate"));
        GoalMode savedMode = GoalMode.valueOf(source.getString("goalMode"));
        String savedTime = source.getString("targetTime");
        TrainingStrategy savedStrategy = TrainingStrategy.valueOf(source.getString("strategy"));
        EnumSet<DayOfWeek> savedRunning = readDays(source.getJSONArray("runningDays"));
        int savedRuns = source.getInt("runsPerWeek");
        if (savedRuns < 1 || savedRuns > 7) throw new IllegalArgumentException("Invalid run count");
        DayOfWeek savedLongRun = source.isNull("longRunDay")
                ? null : DayOfWeek.valueOf(source.getString("longRunDay"));
        EnumSet<DayOfWeek> savedGym = readDays(source.getJSONArray("gymDays"));
        ScheduleType savedSchedule = ScheduleType.valueOf(source.getString("scheduleType"));
        UnitSystem savedUnits = UnitSystem.valueOf(source.getString("units"));
        step = savedStep;
        distance = savedDistance;
        event = savedEvent && distance != RaceDistance.GENERAL_FITNESS;
        raceName = savedName;
        raceDate = savedDate;
        goalMode = distance == RaceDistance.GENERAL_FITNESS ? GoalMode.FINISH : savedMode;
        targetTime = savedTime;
        strategy = savedStrategy;
        runningDays = savedRunning;
        runsPerWeek = savedRuns;
        longRunDay = savedLongRun;
        gymDays = savedGym;
        scheduleType = savedSchedule;
        units = savedUnits;
    }

    String toJson() {
        try {
            return new JSONObject()
                    .put("version", VERSION)
                    .put("step", step)
                    .put("distance", distance.name())
                    .put("event", event)
                    .put("raceName", raceName)
                    .put("raceDate", raceDate == null ? JSONObject.NULL : raceDate.toString())
                    .put("goalMode", goalMode.name())
                    .put("targetTime", targetTime)
                    .put("strategy", strategy.name())
                    .put("runningDays", writeDays(runningDays))
                    .put("runsPerWeek", runsPerWeek)
                    .put("longRunDay", longRunDay == null ? JSONObject.NULL : longRunDay.name())
                    .put("gymDays", writeDays(gymDays))
                    .put("scheduleType", scheduleType.name())
                    .put("units", units.name())
                    .toString();
        } catch (JSONException exception) {
            throw new IllegalStateException("Could not serialize onboarding draft", exception);
        }
    }

    RaceGoal goal(GoalMode mode) {
        if (event && raceDate == null) {
            throw new IllegalArgumentException("Choose a race date, or turn off the specific-race option.");
        }
        return new RaceGoal(distance, event ? raceName : null, event ? raceDate : null, mode,
                mode == GoalMode.TARGET_TIME ? PaceCalculator.parseTime(targetTime) : null);
    }

    TrainingPreferences preferences() {
        return new TrainingPreferences(strategy, runningDays, runsPerWeek, longRunDay,
                gymDays, scheduleType, units);
    }

    UserProfile profile() {
        return new UserProfile(goal(goalMode), preferences(), performanceEligible);
    }

    private static EnumSet<DayOfWeek> readDays(JSONArray array) throws JSONException {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < array.length(); i++) result.add(DayOfWeek.valueOf(array.getString(i)));
        return result;
    }

    private static JSONArray writeDays(Iterable<DayOfWeek> days) {
        JSONArray result = new JSONArray();
        for (DayOfWeek day : days) result.put(day.name());
        return result;
    }
}
