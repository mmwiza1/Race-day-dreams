package org.racedaydreams.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.racedaydreams.domain.activity.SourceIdentity;
import org.racedaydreams.domain.portability.RecordedRoute;
import org.racedaydreams.domain.portability.StructuredWorkoutDelivery;
import org.racedaydreams.domain.portability.TrackPoint;
import org.racedaydreams.domain.reminders.EveningReminder;
import org.racedaydreams.domain.reminders.EveningReminderPolicy;
import org.racedaydreams.domain.reminders.ReminderPlanEntry;
import org.racedaydreams.domain.reminders.ReminderPreferences;
import org.racedaydreams.domain.reminders.ReminderScheduleRepository;

/** No Android or downloaded test dependency. Synthetic prescriptions test scheduling, not plans. */
public final class ReminderDomainTestSuite {
    private static int checks;
    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final LocalDate RUN_DATE = LocalDate.of(2030, 4, 10);

    public static void main(String[] arguments) {
        scheduling(); delivery(); quietAndDaylightSaving(); cancellationBackend(); routeBoundaries();
        System.out.println("ReminderDomainTestSuite: " + checks + " checks passed.");
    }

    private static void scheduling() {
        ReminderPlanEntry easy = entry("easy", RUN_DATE, 1, WorkoutCategory.EASY_RUN);
        ReminderPreferences all = preferences(ReminderPreferences.Mode.ALL, UTC);
        Clock earlier = clock("2030-04-09T18:00:00Z");
        equal(0, EveningReminderPolicy.schedule(Collections.singletonList(easy),
                ReminderPreferences.defaults(UTC), earlier, true).size());
        equal(0, EveningReminderPolicy.schedule(Collections.singletonList(easy), all, earlier, false).size());
        List<EveningReminder> reminders = EveningReminderPolicy.schedule(Collections.singletonList(easy), all, earlier, true);
        equal(1, reminders.size());
        equal(Instant.parse("2030-04-09T19:00:00Z"), reminders.get(0).triggerAt());
        equal("Easy run tomorrow", reminders.get(0).title());
        check(reminders.get(0).message().contains("6.0 km"));
        check(!reminders.get(0).message().contains("litre"));
        equal(0, EveningReminderPolicy.schedule(Collections.singletonList(easy), all,
                clock("2030-04-09T19:00:01Z"), true).size());
        ReminderPreferences quality = preferences(ReminderPreferences.Mode.LONG_HARD_ONLY, UTC);
        equal(0, EveningReminderPolicy.schedule(Collections.singletonList(easy), quality, earlier, true).size());
        for (WorkoutCategory category : Arrays.asList(WorkoutCategory.LONG_RUN, WorkoutCategory.INTERVALS,
                WorkoutCategory.TEMPO_THRESHOLD_RUN, WorkoutCategory.RACE_PACE_RUN,
                WorkoutCategory.TIME_TRIAL, WorkoutCategory.RACE, WorkoutCategory.PROGRESSION_RUN)) {
            equal(1, EveningReminderPolicy.schedule(Collections.singletonList(entry("hard", RUN_DATE, 1, category)),
                    quality, earlier, true).size());
        }
        for (ReminderPlanEntry.Status status : ReminderPlanEntry.Status.values()) {
            ReminderPlanEntry candidate = new ReminderPlanEntry(easy.workout(), 1, status);
            equal(status == ReminderPlanEntry.Status.PLANNED ? 1 : 0,
                    EveningReminderPolicy.schedule(Collections.singletonList(candidate), all, earlier, true).size());
        }
        fails(() -> new ReminderPlanEntry(easy.workout(), 0, ReminderPlanEntry.Status.PLANNED));
        fails(() -> new ReminderPreferences.QuietHours(LocalTime.NOON, LocalTime.NOON));
        fails(() -> reminders.add(reminders.get(0)));
    }

    private static void delivery() {
        ReminderPlanEntry original = entry("run", RUN_DATE, 7, WorkoutCategory.INTERVALS);
        ReminderPreferences all = preferences(ReminderPreferences.Mode.ALL, UTC);
        EveningReminder reminder = EveningReminderPolicy.candidate(original, all).get();
        Clock now = clock("2030-04-09T19:15:00Z");
        check(EveningReminderPolicy.canDeliver(reminder, original, all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, original, all, now, false));
        check(!EveningReminderPolicy.canDeliver(reminder, null, all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, entry("run", RUN_DATE, 8, WorkoutCategory.INTERVALS), all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, entry("run", RUN_DATE.plusDays(1), 7, WorkoutCategory.INTERVALS), all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, entry("run", RUN_DATE, 7, WorkoutCategory.EASY_RUN), all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, new ReminderPlanEntry(original.workout(), 7,
                ReminderPlanEntry.Status.COMPLETED), all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, new ReminderPlanEntry(original.workout(), 7,
                ReminderPlanEntry.Status.CANCELLED), all, now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, original, ReminderPreferences.defaults(UTC), now, true));
        check(!EveningReminderPolicy.canDeliver(reminder, original, all, clock("2030-04-09T18:59:59Z"), true));
        check(!EveningReminderPolicy.canDeliver(reminder, original, all, clock("2030-04-09T21:00:00Z"), true));
        check(!EveningReminderPolicy.canDeliver(reminder, original, all, clock("2030-04-10T19:00:00Z"), true));
    }

    private static void quietAndDaylightSaving() {
        ReminderPreferences quiet = new ReminderPreferences(ReminderPreferences.Mode.ALL, LocalTime.of(23, 0),
                new ReminderPreferences.QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0)), UTC);
        check(!EveningReminderPolicy.candidate(entry("quiet", RUN_DATE, 1, WorkoutCategory.LONG_RUN), quiet).isPresent());
        check(quiet.quietHours().contains(LocalTime.of(22, 0)));
        check(quiet.quietHours().contains(LocalTime.MIDNIGHT));
        check(!quiet.quietHours().contains(LocalTime.of(7, 0)));
        ReminderPreferences daytime = new ReminderPreferences(ReminderPreferences.Mode.ALL, LocalTime.NOON,
                new ReminderPreferences.QuietHours(LocalTime.of(11, 0), LocalTime.of(13, 0)), UTC);
        check(!EveningReminderPolicy.candidate(entry("dayquiet", RUN_DATE, 1, WorkoutCategory.LONG_RUN), daytime).isPresent());
        ZoneId london = ZoneId.of("Europe/London");
        ReminderPreferences spring = new ReminderPreferences(ReminderPreferences.Mode.ALL, LocalTime.of(1, 30), null, london);
        EveningReminder shifted = EveningReminderPolicy.candidate(entry("spring", LocalDate.of(2030, 4, 1), 1,
                WorkoutCategory.EASY_RUN), spring).get();
        equal(Instant.parse("2030-03-31T01:30:00Z"), shifted.triggerAt());
        equal(LocalTime.of(2, 30), shifted.triggerAt().atZone(london).toLocalTime());
        check(EveningReminderPolicy.canDeliver(shifted, entry("spring", LocalDate.of(2030, 4, 1), 1,
                WorkoutCategory.EASY_RUN), spring, Clock.fixed(shifted.triggerAt().plusSeconds(60), UTC), true));
        ReminderPreferences autumn = new ReminderPreferences(ReminderPreferences.Mode.ALL, LocalTime.of(1, 30), null, london);
        EveningReminder overlap = EveningReminderPolicy.candidate(entry("autumn", LocalDate.of(2030, 10, 28), 1,
                WorkoutCategory.EASY_RUN), autumn).get();
        equal(Instant.parse("2030-10-27T00:30:00Z"), overlap.triggerAt());
        check(!EveningReminderPolicy.canDeliver(overlap, entry("autumn", LocalDate.of(2030, 10, 28), 1,
                WorkoutCategory.EASY_RUN), preferences(ReminderPreferences.Mode.ALL, ZoneId.of("Asia/Tokyo")),
                Clock.fixed(overlap.triggerAt(), UTC), true));
    }

    private static void cancellationBackend() {
        final Map<String, EveningReminder> alarms = new HashMap<>();
        ReminderScheduleRepository backend = reminders -> {
            alarms.clear();
            for (EveningReminder reminder : reminders) alarms.put(reminder.workoutId(), reminder);
        };
        ReminderPreferences all = preferences(ReminderPreferences.Mode.ALL, UTC);
        Clock before = clock("2030-04-09T18:00:00Z");
        backend.replaceSchedule(EveningReminderPolicy.schedule(Collections.singletonList(
                entry("move", RUN_DATE, 1, WorkoutCategory.LONG_RUN)), all, before, true));
        equal(1, alarms.size());
        backend.replaceSchedule(EveningReminderPolicy.schedule(Collections.singletonList(
                entry("move", RUN_DATE.plusDays(1), 2, WorkoutCategory.LONG_RUN)), all, before, true));
        equal(2L, alarms.get("move").revision());
        equal(Instant.parse("2030-04-10T19:00:00Z"), alarms.get("move").triggerAt());
        backend.replaceSchedule(Collections.emptyList());
        equal(0, alarms.size());
    }

    private static void routeBoundaries() {
        TrackPoint first = new TrackPoint(Instant.EPOCH, 51.5, -0.1, null, null, null, null);
        TrackPoint next = new TrackPoint(Instant.EPOCH.plusSeconds(10), 51.51, -0.11, 40.0, 3.0, 140.0, 165.0);
        List<TrackPoint> points = new ArrayList<>(Arrays.asList(first, next));
        List<List<TrackPoint>> segments = new ArrayList<>(); segments.add(points);
        RecordedRoute route = new RecordedRoute("route", "activity", new SourceIdentity("gpx", "id", ""), segments, "Route");
        points.clear(); segments.clear();
        equal(1, route.segments().size()); equal(2, route.segments().get(0).size());
        check(route.segments().get(0).get(0).heartRate() == null);
        fails(() -> route.segments().clear());
        fails(() -> route.segments().get(0).clear());
        fails(() -> new TrackPoint(Instant.EPOCH, 91, 0, null, null, null, null));
        fails(() -> new TrackPoint(Instant.EPOCH, 0, 181, null, null, null, null));
        fails(() -> new TrackPoint(Instant.EPOCH, Double.NaN, 0, null, null, null, null));
        fails(() -> new TrackPoint(Instant.EPOCH, 0, 0, null, -1.0, null, null));
        fails(() -> new RecordedRoute("id", null, new SourceIdentity("gpx", "id", ""),
                Collections.singletonList(Arrays.asList(next, first)), ""));
        fails(() -> new StructuredWorkoutDelivery(entry("run", RUN_DATE, 1, WorkoutCategory.EASY_RUN).workout(), 0));
    }

    private static ReminderPlanEntry entry(String id, LocalDate date, long revision, WorkoutCategory category) {
        return new ReminderPlanEntry(new PlannedWorkout(id, date, category, 6.0, null,
                Collections.emptyList(), null), revision, ReminderPlanEntry.Status.PLANNED);
    }
    private static ReminderPreferences preferences(ReminderPreferences.Mode mode, ZoneId zone) {
        return new ReminderPreferences(mode, LocalTime.of(19, 0),
                new ReminderPreferences.QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0)), zone);
    }
    private static Clock clock(String instant) { return Clock.fixed(Instant.parse(instant), UTC); }
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Check " + checks + " failed"); }
    private static void equal(Object expected, Object actual) { checks++; if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual); }
    private static void fails(Runnable operation) {
        checks++;
        try { operation.run(); } catch (IllegalArgumentException | UnsupportedOperationException expected) { return; }
        throw new AssertionError("Invalid operation was accepted");
    }
}
