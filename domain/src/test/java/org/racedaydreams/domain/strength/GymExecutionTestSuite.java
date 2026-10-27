package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.strength.GymExecution.EventType;
import org.racedaydreams.domain.strength.GymExecution.Origin;
import org.racedaydreams.domain.strength.GymExecution.SourceKind;
import org.racedaydreams.domain.strength.GymExecution.SourceReference;
import org.racedaydreams.domain.strength.GymExecution.Status;
import org.racedaydreams.domain.strength.GymExecution.Tick;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Replayable actual-set, immutable-source and interrupted-clock regressions; no Android or IO. */
public final class GymExecutionTestSuite {
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static int checks;

    public static void main(String[] args) {
        targetsAndDefinitionsValidate();
        allMeasurementsRemainActualEvidence();
        prescribedAndActualStaySeparate();
        lostAcknowledgementIsIdempotent();
        manualTimingAndWallClockChanges();
        interruptionRequiresExplicitRecovery();
        pausedAndStructuredRecovery();
        manualExerciseChangesPreserveActuals();
        repeatNeverCopiesActualsOrInventsTargets();
        exactProgrammeAndFavouriteSourcesStayFrozen();
        malformedSnapshotsFailBeforeUse();
        freeWorkoutAndExplicitDiscard();
        System.out.println("GymExecutionTestSuite: " + checks + " checks passed.");
    }

    private static void targetsAndDefinitionsValidate() {
        rejects(() -> target(0, 5, 8, null, null, null, 90, null), "zero sets");
        rejects(() -> target(2, 8, 5, null, null, null, 90, null), "backwards reps");
        rejects(() -> target(2, 5, null, null, null, null, 90, null), "half a rep range");
        rejects(() -> target(2, null, null, null, null, null, 90, null), "missing measurement");
        rejects(() -> target(2, 5, 8, 30L, null, null, 90, null), "multiple measurements");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            rejects(() -> target(2, null, null, null, invalid, null, 90, null), "invalid metres");
            rejects(() -> target(2, 5, 8, null, null, null, 90, invalid), "invalid reference");
        }
        rejects(() -> target(2, 5, 8, null, null, null, -1, null), "negative rest");
        rejects(() -> target(2, 5, 8, null, null, null, 1801, null), "unbounded rest");
        rejects(
                () ->
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RPE, 9, 6),
                "effort range");
        rejects(
                () ->
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RIR, Double.NaN, 3),
                "unknown effort");
        rejects(
                () -> new GymWorkoutDefinition("empty", 1, NOW, "Empty", List.of()),
                "empty definition");
        var slot = slot("bench", "bench-press", 2);
        rejects(() -> workout(List.of(slot, slot)), "duplicate slots");
        rejects(
                () -> new GymWorkoutDefinition.Slot("bad", exercise("plank"), slot.target(), null),
                "wrong measurement");
        rejects(
                () ->
                        new GymWorkoutDefinition.Slot(
                                "bad", exercise("push-up"), slot.target(), null),
                "bodyweight load reference");
        var rir = new GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2, 4);
        rejects(
                () ->
                        new GymWorkoutDefinition.Slot(
                                "bad",
                                exercise("plank"),
                                target(2, null, null, 30L, null, rir, null, null),
                                null),
                "RIR duration");
        var one = new GymWorkoutDefinition.Slot("a", exercise("bench-press"), null, "pair");
        var two = new GymWorkoutDefinition.Slot("b", exercise("dumbbell-row"), null, "pair");
        var open = new GymWorkoutDefinition.Slot("c", exercise("plank"), null, null);
        rejects(() -> workout(List.of(one)), "single member superset");
        rejects(() -> workout(List.of(one, open, two)), "separated superset");
        var mutable = new ArrayList<>(List.of(one, two));
        var grouped = workout(mutable);
        mutable.clear();
        equal(2, grouped.slots().size(), "Definition takes an immutable copy");
        rejects(() -> grouped.slots().clear(), "immutable slots");
        equal(
                0,
                target(2, 5, 8, null, null, null, 0, null).restSeconds(),
                "Optional zero rest remains explicit");
    }

    private static void allMeasurementsRemainActualEvidence() {
        for (var profile : StrengthExercise.MeasurementProfile.values()) {
            var mode =
                    profile.usesLoad()
                            ? StrengthExercise.Mode.WEIGHTED
                            : StrengthExercise.Mode.BODYWEIGHT;
            var definition =
                    new StrengthExercise(
                            "measure-" + profile.name(),
                            "Measured exercise",
                            StrengthExercise.Region.FULL,
                            mode,
                            false,
                            profile);
            var target =
                    target(
                            1,
                            profile.usesReps() ? 6 : null,
                            profile.usesReps() ? 8 : null,
                            profile.usesDuration() ? 30L : null,
                            profile.usesDistance() ? 40.0 : null,
                            null,
                            60,
                            profile.usesLoad() ? 100.0 : null);
            var slot = new GymWorkoutDefinition.Slot("task", definition, target, null);
            var engine =
                    GymExecutionEngine.start(
                            "actual-" + profile,
                            Origin.CUSTOM,
                            workout(List.of(slot)),
                            null,
                            ZoneOffset.UTC,
                            tick(0));
            equal(
                    0,
                    engine.snapshot().actualSets().size(),
                    "No prescribed measurement pre-fills actuals");
            var actual =
                    new StrengthSession.SetEntry(
                            "actual-" + profile,
                            profile.usesReps() ? 7 : 0,
                            profile.usesLoad() ? 55 : 0,
                            null,
                            false,
                            profile.usesDuration() ? 24L : null,
                            profile.usesDistance() ? 31.0 : null,
                            null);
            engine.saveSet("task", actual, tick(1000));
            equal(
                    actual,
                    engine.snapshot().actualSets().get(0).actual(),
                    "Entered measurement is retained exactly");
            var finished = engine.finish(tick(2000), null, "Actuals only", false);
            equal(
                    actual,
                    finished.completedSession().exercises().get(0).sets().get(0),
                    "Finalisation preserves actual values");
            equal(
                    null,
                    finished.completedSession().rpe(),
                    "Missing session effort remains unknown");
            equal(
                    1,
                    engine.workingSets("task"),
                    "Actual working set, not target magnitude, determines count");
            equal(
                    null,
                    finished.plannedAssociation(),
                    "Custom workout never acquires a programme association");
        }
        var weightedPullUp = exercise("weighted-pull-up");
        var slot =
                new GymWorkoutDefinition.Slot(
                        "pull", weightedPullUp, target(1, 3, 6, null, null, null, 120, null), null);
        var engine = start(workout(List.of(slot)));
        var actual =
                new StrengthSession.SetEntry("added-load", 5, 12.5, null, false, null, null, 2.0);
        engine.saveSet("pull", actual, tick(1000));
        equal(
                12.5,
                engine.finish(tick(2000), null, "", false)
                        .completedSession()
                        .exercises()
                        .get(0)
                        .sets()
                        .get(0)
                        .kilograms(),
                "Weighted bodyweight retains added load");
    }

    private static void prescribedAndActualStaySeparate() {
        var definition = workout(List.of(slot("bench", "bench-press", 2)));
        var engine = start(definition);
        rejects(
                () -> engine.finish(tick(1000), null, "", false),
                "cannot finalise an empty prescription");
        equal(0L, engine.snapshot().activeMillis(), "Rejected finish leaves timing unchanged");
        var warm = new StrengthSession.SetEntry("warm", 5, 20, null, true);
        engine.saveSet("bench", warm, tick(1000));
        equal(0, engine.workingSets("bench"), "Warm-up is not working completion");
        equal(
                null,
                engine.snapshot().completedSession(),
                "Saved live sets are staged, not completed evidence");
        engine.saveSet("bench", set("work", 7, 65, 8.0), tick(2000));
        var finished = engine.finish(tick(3000), 8.0, "Stopped early", false);
        ok(
                finished.partial(),
                "One of two working sets is partial even if finish-early was not selected");
        equal(
                EventType.WORKOUT_FINISHED_EARLY,
                last(finished).type(),
                "Partial completion remains explainable");
        equal(
                80.0,
                definition.slots().get(0).target().referenceLoadKg(),
                "Prescription reference remains unchanged");
        equal(
                65.0,
                finished.completedSession().exercises().get(0).sets().get(1).kilograms(),
                "Actual never rewritten to reference load");
        equal(
                1,
                StrengthAnalytics.progression(List.of(finished.completedSession())).stream()
                        .map(StrengthAnalytics.Record::exerciseId)
                        .distinct()
                        .count(),
                "Working actual may establish independent PB evidence");
        ok(
                finished.completedSession().exercises().get(0).sets().get(0).warmUp(),
                "Warm-up flag survives finalisation");
    }

    private static void lostAcknowledgementIsIdempotent() {
        var engine = start(workout(List.of(slot("bench", "bench-press", 1))));
        var row = new GymExecutionSet("bench", exercise("bench-press"), set("stable", 8, 60, 7.0));
        engine.saveSet(row, tick(1000));
        var saved = engine.snapshot();
        equal(
                saved,
                engine.saveSet(row, tick(2000)),
                "Lost save acknowledgement retry has no duplicate set/event/time");
        rejects(
                () -> engine.saveSet("bench", set("stable", 7, 60, 7.0), tick(3000)),
                "conflicting stable set ID");
        equal(saved, engine.snapshot(), "Conflicting retry does not mutate actual evidence");
        var finished = engine.finish(tick(2000), null, "Done", false);
        equal(
                finished,
                engine.finish(tick(5000), null, "Done", false),
                "Finalisation retry is idempotent");
        equal(
                finished,
                engine.saveSet(row, tick(9000)),
                "Lost set acknowledgement after finish stays idempotent");
        rejects(
                () -> engine.finish(tick(6000), 9.0, "Changed", false),
                "finished evidence cannot be overwritten");
        rejects(
                () -> engine.saveSet("bench", set("new", 8, 60, 7.0), tick(6000)),
                "new actual after finish");
        equal(1, finished.actualSets().size(), "Exactly one retained actual set");
    }

    private static void manualTimingAndWallClockChanges() {
        var engine = start(workout(List.of(slot("bench", "bench-press", 1))));
        engine.saveSet("bench", set("clock-work", 8, 60, null), tick(60000));
        engine.pause(tick(90000));
        engine.observe(new Tick("boot", 150000, NOW.minus(Duration.ofHours(2))));
        engine.resume(new Tick("boot", 210000, NOW.plus(Duration.ofDays(1))));
        var finished =
                engine.finish(
                        new Tick("boot", 240000, NOW.minus(Duration.ofDays(1))), null, "", false);
        equal(240000L, finished.recordedElapsedMillis(), "Observed elapsed includes manual pause");
        equal(120000L, finished.activeMillis(), "Active excludes the two-minute pause");
        equal(
                Duration.ofMinutes(2),
                finished.completedSession().duration(),
                "Wall-clock changes do not alter actual duration");
        equal(
                NOW,
                finished.completedSession().startedAt(),
                "Original start provenance remains unchanged");
        var another = start(workout(List.of(slot("bench", "bench-press", 1))));
        another.observe(tick(1000));
        rejects(() -> another.observe(tick(999)), "monotonic regression");
        rejects(
                () -> another.observe(new Tick("new-boot", 2000, NOW)),
                "continuity change requires explicit recovery");
        equal(
                1000L,
                another.snapshot().activeMillis(),
                "Rejected clock observations preserve timing");
    }

    private static void interruptionRequiresExplicitRecovery() {
        var engine = start(workout(List.of(slot("bench", "bench-press", 1))));
        engine.saveSet("bench", set("before-crash", 8, 60, 7.0), tick(60000));
        var old = engine.snapshot();
        var recovered =
                GymExecutionEngine.recover(
                        old, new Tick("reboot", 2000, NOW.plus(Duration.ofHours(4))));
        equal(
                Status.RECOVERY_REQUIRED,
                recovered.snapshot().status(),
                "Active recording never secretly resumes after interruption/reboot");
        equal(60000L, recovered.snapshot().activeMillis(), "Four-hour unknown gap is not exercise");
        equal(
                old.actualSets(),
                recovered.snapshot().actualSets(),
                "Accepted actual sets survive recovery exactly");
        equal(
                EventType.RECOVERED_WITH_GAP,
                last(recovered.snapshot()).type(),
                "Interruption is explicit evidence");
        rejects(
                () ->
                        recovered.saveSet(
                                "bench", set("hidden", 8, 60, null), new Tick("reboot", 3000, NOW)),
                "resume required");
        recovered.observe(new Tick("reboot", 12000, NOW.plus(Duration.ofHours(4))));
        equal(
                60000L,
                recovered.snapshot().recordedElapsedMillis(),
                "Waiting for recovery choice adds no invented workout time");
        var directFinish =
                recovered.finish(
                        new Tick("reboot", 15000, NOW.plus(Duration.ofHours(4))),
                        null,
                        "Recovered partial recording",
                        true);
        equal(
                60000L,
                directFinish.activeMillis(),
                "Unfinished run may be safely finalised without pretending continuity");
        ok(directFinish.partial(), "Explicit recovered early finish remains partial");
        rejects(
                () -> GymExecutionEngine.recover(directFinish, tick(90000)),
                "cannot recover a finalised workout");
    }

    private static void pausedAndStructuredRecovery() {
        var engine =
                start(
                        workout(
                                List.of(
                                        slot("bench", "bench-press", 2),
                                        slot("row", "dumbbell-row", 1))));
        engine.saveSet("bench", set("kept", 8, 60, 7.0), tick(30000));
        engine.selectExercise("row", tick(40000));
        engine.pause(tick(50000));
        var recovered =
                GymExecutionEngine.recover(
                        engine.snapshot(), new Tick("boot-2", 1000, NOW.plus(Duration.ofHours(1))));
        equal(
                Status.PAUSED,
                recovered.snapshot().status(),
                "Manual pause state survives process death");
        equal(
                "row",
                recovered.snapshot().selectedSlotId(),
                "Structured exercise position survives");
        equal(
                engine.snapshot().prescription(),
                recovered.snapshot().prescription(),
                "Exact frozen prescription survives");
        equal(
                50000L,
                recovered.snapshot().recordedElapsedMillis(),
                "Unknown gap is excluded even when paused");
        recovered.resume(new Tick("boot-2", 11000, NOW.plus(Duration.ofHours(1))));
        recovered.saveSet(
                "row",
                set("new-actual", 10, 30, 7.0),
                new Tick("boot-2", 21000, NOW.plus(Duration.ofHours(1))));
        equal(
                60000L,
                recovered.snapshot().activeMillis(),
                "Only newly observed active time is added after resume");
        equal(
                70000L,
                recovered.snapshot().recordedElapsedMillis(),
                "Known post-recovery pause is elapsed, not active");
        equal(
                2,
                recovered.snapshot().actualSets().size(),
                "No duplicate actual sets across recovery");
        ok(
                recovered.finish(new Tick("boot-2", 22000, NOW), null, "", false).partial(),
                "Unfinished bench targets remain partial after structured recovery");
    }

    private static void manualExerciseChangesPreserveActuals() {
        var definition = workout(List.of(slot("bench", "bench-press", 2)));
        var engine = start(definition);
        var oldRow =
                new GymExecutionSet("bench", exercise("bench-press"), set("barbell", 8, 80, 8.0));
        engine.saveSet(oldRow, tick(1000));
        engine.replaceExercise("bench", exercise("dumbbell-bench-press"), tick(2000));
        equal(
                null,
                engine.snapshot().slots().get(0).target().referenceLoadKg(),
                "80kg barbell reference is never transferred to dumbbells");
        equal(
                "bench-press",
                definition.slots().get(0).exercise().id(),
                "Original prescription survives a today-only swap");
        equal(
                "bench-press",
                engine.snapshot().actualSets().get(0).exercise().id(),
                "Earlier actual definition survives swap");
        equal(
                0,
                engine.workingSets("bench"),
                "Old exercise's sets do not satisfy new exercise's target");
        equal(
                engine.snapshot(),
                engine.saveSet(oldRow, tick(2500)),
                "Old-set acknowledgement retry remains valid after swap");
        engine.saveSet("bench", set("dumbbell", 7, 25, 7.0), tick(3000));
        engine.replaceExercise("bench", exercise("plank"), tick(4000));
        equal(
                null,
                engine.snapshot().slots().get(0).target(),
                "Rep prescription is not silently converted to seconds");
        var finished = engine.finish(tick(5000), null, "Today-only alternatives", true);
        equal(
                2,
                finished.completedSession().exercises().size(),
                "Different actually performed exercises stay separate");
        equal(
                80.0,
                finished.completedSession().exercises().get(0).sets().get(0).kilograms(),
                "Original actual barbell evidence is unchanged");
        equal(
                25.0,
                finished.completedSession().exercises().get(1).sets().get(0).kilograms(),
                "Actual dumbbell evidence is independent");
    }

    private static void repeatNeverCopiesActualsOrInventsTargets() {
        var previous = actualSession(false);
        var repeated =
                GymWorkoutAdapter.repeatActual("repeat", 1, NOW, "Repeat exercise list", previous);
        equal(
                null,
                repeated.definition().slots().get(0).target(),
                "Observed manual performance was never a prescribed target");
        var engine =
                GymExecutionEngine.start(
                        "repeat-execution",
                        Origin.REPEATED,
                        repeated.definition(),
                        repeated.source(),
                        ZoneOffset.UTC,
                        tick(0));
        equal(0, engine.snapshot().actualSets().size(), "Previous actuals are never new actuals");
        equal(
                previous.id(),
                engine.snapshot().source().id(),
                "Repeated workout retains original actual source");
        equal(
                null,
                engine.snapshot().plannedAssociation(),
                "Repeated actual cannot silently complete a programme");
        rejects(
                () ->
                        GymWorkoutAdapter.repeatActual(
                                "bad", 1, NOW, "Warm-up only", actualSession(true)),
                "all-warmup history cannot invent a working routine");
        var structured = start(workout(List.of(slot("bench", "bench-press", 1))));
        structured.saveSet("bench", set("old", 8, 80, 7.0), tick(1000));
        var finished = structured.finish(tick(2000), null, "", false);
        var again = GymWorkoutAdapter.repeatExecution("again", 1, NOW, "Again", finished);
        equal(
                8,
                again.definition().slots().get(0).target().maximumReps(),
                "Structured repeat retains actual preserved rep target");
        equal(
                null,
                again.definition().slots().get(0).target().referenceLoadKg(),
                "Repeat does not turn stale optional loading into a new suggestion");
        var started =
                GymExecutionEngine.start(
                        "again-execution",
                        Origin.REPEATED,
                        again.definition(),
                        again.source(),
                        ZoneOffset.UTC,
                        tick(0));
        equal(
                0,
                started.snapshot().actualSets().size(),
                "Structured repeat still starts with no actual sets");
        equal(1, finished.actualSets().size(), "Original evidence remains intact");
        var partial =
                start(
                        workout(
                                List.of(
                                        slot("bench", "bench-press", 2),
                                        slot("row", "dumbbell-row", 1))));
        partial.saveSet("bench", set("partial-actual", 6, 55, 8.0), tick(1000));
        var partialDone = partial.finish(tick(2000), null, "One set performed", true);
        var repeatPartial =
                GymWorkoutAdapter.repeatExecution(
                        "partial-repeat", 1, NOW, "Repeat intentions", partialDone);
        equal(
                2,
                repeatPartial.definition().slots().size(),
                "Partial repeat explicitly preserves unperformed intentions too");
        var repeatStarted =
                GymExecutionEngine.start(
                        "partial-repeat-run",
                        Origin.REPEATED,
                        repeatPartial.definition(),
                        repeatPartial.source(),
                        ZoneOffset.UTC,
                        tick(0));
        equal(
                0,
                repeatStarted.snapshot().actualSets().size(),
                "Unperformed repeated targets never become actual evidence");
        equal(
                1,
                partialDone.actualSets().size(),
                "Repeating a partial workout does not alter its true completion");
    }

    private static void exactProgrammeAndFavouriteSourcesStayFrozen() {
        var programme = programme(1, 80.0, GymProgramme.Effort.RIR);
        var prepared = GymWorkoutAdapter.fromProgramme(programme, "session");
        equal(
                GymWorkoutDefinition.EffortScale.RIR,
                prepared.definition().slots().get(0).target().effort().scale(),
                "RIR display preference is preserved");
        equal(
                2.0,
                prepared.definition().slots().get(0).target().effort().minimum(),
                "RPE max8 maps to RIR min2");
        equal(
                4.0,
                prepared.definition().slots().get(0).target().effort().maximum(),
                "RPE min6 maps to RIR max4");
        equal(
                prepared,
                GymWorkoutAdapter.fromProgramme(programme, "session"),
                "Legacy adapter is deterministic");
        var engine =
                GymExecutionEngine.start(
                        "planned",
                        Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        var changed =
                GymWorkoutAdapter.fromProgramme(
                        programme(2, 100.0, GymProgramme.Effort.RPE), "session");
        equal(
                1,
                engine.snapshot().source().revision(),
                "Later programme changes cannot alter executing revision");
        equal(
                80.0,
                engine.snapshot().prescription().slots().get(0).target().referenceLoadKg(),
                "Original optional reference is preserved");
        equal(2, changed.source().revision(), "New version remains a distinct source");
        rejects(
                () ->
                        GymExecutionEngine.start(
                                "wrong-revision",
                                Origin.PLANNED,
                                prepared.definition(),
                                changed.source(),
                                ZoneOffset.UTC,
                                tick(0)),
                "mismatched programme revision cannot claim frozen provenance");
        rejects(
                () ->
                        GymExecutionEngine.start(
                                "wrong-programme",
                                Origin.PLANNED,
                                prepared.definition(),
                                new SourceReference(
                                        SourceKind.PROGRAMME, "another-programme", 1, "session"),
                                ZoneOffset.UTC,
                                tick(0)),
                "mismatched programme identity");
        rejects(
                () ->
                        GymExecutionEngine.start(
                                "wrong-prescription",
                                Origin.PLANNED,
                                workout(List.of(slot("bench", "bench-press", 1))),
                                prepared.source(),
                                ZoneOffset.UTC,
                                tick(0)),
                "arbitrary workout cannot masquerade as preserved programme session");
        equal(
                null,
                engine.snapshot().plannedAssociation(),
                "Unfinished staging is not linked completed evidence");
        engine.saveSet("slot-0", set("planned-actual", 6, 75, 7.0), tick(1000));
        var finished = engine.finish(tick(2000), null, "", true);
        equal(
                prepared.source(),
                finished.plannedAssociation(),
                "Finish may associate only to the exact source selected at start");
        equal(
                75.0,
                finished.completedSession().exercises().get(0).sets().get(0).kilograms(),
                "Actual remains independent of both old and new programme targets");
        for (Origin origin :
                List.of(Origin.QUICK, Origin.CUSTOM, Origin.GENERATED, Origin.REPEATED)) {
            var extra =
                    GymExecutionEngine.start(
                            "extra-" + origin,
                            origin,
                            prepared.definition(),
                            prepared.source(),
                            ZoneOffset.UTC,
                            tick(0));
            extra.saveSet("slot-0", set("extra-set-" + origin, 6, 75, 7.0), tick(1000));
            equal(
                    null,
                    extra.finish(tick(2000), null, "", true).plannedAssociation(),
                    "Spontaneous provenance does not auto-associate: " + origin);
        }
        var saved = workout(List.of(slot("bench", "bench-press", 1)));
        var source =
                new SourceReference(SourceKind.SAVED_WORKOUT, saved.id(), saved.revision(), null);
        var favourite =
                GymExecutionEngine.start(
                        "favourite", Origin.FAVOURITE, saved, source, ZoneOffset.UTC, tick(0));
        equal(source, favourite.snapshot().source(), "Favourite retains exact saved revision");
        rejects(
                () ->
                        GymExecutionEngine.start(
                                "bad-fav",
                                Origin.FAVOURITE,
                                saved,
                                new SourceReference(
                                        SourceKind.SAVED_WORKOUT,
                                        saved.id(),
                                        saved.revision() + 1,
                                        null),
                                ZoneOffset.UTC,
                                tick(0)),
                "mismatched favourite revision");
        rejects(
                () ->
                        GymExecutionEngine.start(
                                "bad-plan", Origin.PLANNED, saved, null, ZoneOffset.UTC, tick(0)),
                "missing programme source");
        rejects(
                () -> GymWorkoutAdapter.fromProgramme(programme, "missing"),
                "missing retained programme session");
    }

    private static void malformedSnapshotsFailBeforeUse() {
        var engine = start(workout(List.of(slot("bench", "bench-press", 1))));
        engine.saveSet("bench", set("only", 8, 60, null), tick(1000));
        var valid = engine.snapshot();
        rejects(
                () ->
                        copy(
                                valid,
                                List.of(valid.actualSets().get(0), valid.actualSets().get(0)),
                                valid.events(),
                                null,
                                false),
                "duplicate persisted actual IDs");
        rejects(
                () -> copy(valid, valid.actualSets(), valid.events().subList(0, 1), null, false),
                "actual missing save event");
        var corruptEvents = new ArrayList<>(valid.events());
        corruptEvents.set(
                1,
                new GymExecution.Event(9, EventType.SET_SAVED, "bench", "only", 1000, 1000, NOW));
        rejects(
                () -> copy(valid, valid.actualSets(), corruptEvents, null, false),
                "corrupt event sequence");
        var twoSlotEngine =
                start(
                        workout(
                                List.of(
                                        slot("bench", "bench-press", 1),
                                        slot("row", "dumbbell-row", 1))));
        twoSlotEngine.saveSet("bench", set("misattributed", 8, 60, null), tick(1000));
        var twoSlot = twoSlotEngine.snapshot();
        var wrongSlotEvents = new ArrayList<>(twoSlot.events());
        wrongSlotEvents.set(
                1,
                new GymExecution.Event(
                        1, EventType.SET_SAVED, "row", "misattributed", 1000, 1000, NOW));
        rejects(
                () -> copy(twoSlot, twoSlot.actualSets(), wrongSlotEvents, null, false),
                "set event cannot attribute another exercise slot");
        var repeatedStart = new ArrayList<>(valid.events());
        repeatedStart.add(
                new GymExecution.Event(
                        2, EventType.WORKOUT_STARTED, "bench", null, 1000, 1000, NOW));
        rejects(
                () -> copy(valid, valid.actualSets(), repeatedStart, null, false),
                "repeated workout-start event");
        var afterEnd = new ArrayList<>(valid.events());
        afterEnd.add(
                new GymExecution.Event(
                        2, EventType.WORKOUT_COMPLETED, "bench", null, 1000, 1000, NOW));
        afterEnd.add(
                new GymExecution.Event(
                        3, EventType.EXERCISE_SELECTED, "bench", null, 1000, 1000, NOW));
        rejects(
                () -> copy(valid, valid.actualSets(), afterEnd, null, false),
                "terminal event cannot be followed by new execution");
        var impossiblePause = new ArrayList<>(valid.events());
        impossiblePause.add(
                new GymExecution.Event(2, EventType.RESUMED, "bench", null, 1000, 1000, NOW));
        rejects(
                () -> copy(valid, valid.actualSets(), impossiblePause, null, false),
                "active workout cannot contain an impossible resume transition");
        for (EventType type :
                List.of(
                        EventType.EXERCISE_SELECTED,
                        EventType.EXERCISE_REPLACED,
                        EventType.EXERCISES_ADDED)) {
            rejects(
                    () -> new GymExecution.Event(2, type, null, null, 1000, 1000, NOW),
                    "exercise event requires affected slot");
        }
        var finished = engine.finish(tick(2000), null, "", false);
        var forged =
                new StrengthSession(
                        finished.id(),
                        finished.startedAt(),
                        finished.offset(),
                        finished.completedSession().duration(),
                        null,
                        "",
                        List.of(
                                new StrengthSession.ExerciseSets(
                                        exercise("bench-press"),
                                        List.of(set("only", 8, 150, null)))));
        rejects(
                () -> copy(finished, finished.actualSets(), finished.events(), forged, false),
                "forged completed actual values");
        rejects(
                () -> copy(valid, valid.actualSets(), valid.events(), forged, false),
                "unfinished execution with premature completed evidence");
        var partialEngine = start(workout(List.of(slot("bench", "bench-press", 2))));
        partialEngine.saveSet("bench", set("short", 6, 55, null), tick(1000));
        var partial = partialEngine.finish(tick(2000), null, "", false);
        var inventedCompletion = new ArrayList<>(partial.events());
        var end = last(partial);
        inventedCompletion.set(
                end.sequence(),
                new GymExecution.Event(
                        end.sequence(),
                        EventType.WORKOUT_COMPLETED,
                        end.slotId(),
                        null,
                        end.recordedElapsedMillis(),
                        end.activeMillis(),
                        end.observedAt()));
        rejects(
                () ->
                        copy(
                                partial,
                                partial.actualSets(),
                                inventedCompletion,
                                partial.completedSession(),
                                false),
                "missing prescribed work cannot become complete through corrupt state");
        rejects(() -> valid.actualSets().clear(), "actual evidence list immutable");
        rejects(() -> valid.events().clear(), "event history immutable");
    }

    private static void freeWorkoutAndExplicitDiscard() {
        var free =
                GymExecutionEngine.start("free", Origin.FREE, null, null, ZoneOffset.UTC, tick(0));
        equal(
                0,
                free.snapshot().slots().size(),
                "Free gym execution may start without a prescription");
        equal(null, free.snapshot().selectedSlotId(), "No selected exercise is invented");
        free.addExercises(
                List.of(new GymWorkoutDefinition.Slot("manual", exercise("push-up"), null, null)),
                tick(1000));
        equal(
                "manual",
                free.snapshot().selectedSlotId(),
                "Explicit first exercise becomes selected");
        free.saveSet(
                "manual",
                new StrengthSession.SetEntry("actual-pushups", 10, 0, null, false),
                tick(2000));
        var actual = free.finish(tick(3000), null, "Spontaneous", false);
        equal(
                null,
                actual.plannedAssociation(),
                "Free workout never acquires a planned association");
        equal(
                10,
                actual.completedSession().exercises().get(0).sets().get(0).reps(),
                "Free actual is retained");
        var discard =
                GymExecutionEngine.start(
                        "discard", Origin.FREE, null, null, ZoneOffset.UTC, tick(0));
        equal(
                Status.DISCARDED,
                discard.discard(tick(1000)).status(),
                "Explicit empty draft discard is allowed");
        equal(
                null,
                discard.snapshot().completedSession(),
                "Discard does not manufacture completed evidence");
        rejects(() -> discard.resume(tick(2000)), "discarded session cannot secretly restart");
    }

    private static GymExecution copy(
            GymExecution state,
            List<GymExecutionSet> sets,
            List<GymExecution.Event> events,
            StrengthSession complete,
            boolean partial) {
        return new GymExecution(
                state.id(),
                state.origin(),
                state.prescription(),
                state.source(),
                state.startedAt(),
                state.offset(),
                state.status(),
                state.lastTick(),
                state.recordedElapsedMillis(),
                state.activeMillis(),
                state.selectedSlotId(),
                state.slots(),
                sets,
                events,
                complete,
                partial);
    }

    private static GymExecutionEngine start(GymWorkoutDefinition definition) {
        return GymExecutionEngine.start(
                "execution", Origin.CUSTOM, definition, null, ZoneOffset.UTC, tick(0));
    }

    private static GymWorkoutDefinition workout(List<GymWorkoutDefinition.Slot> slots) {
        return new GymWorkoutDefinition("workout", 1, NOW, "My workout", slots);
    }

    private static GymWorkoutDefinition.Slot slot(String id, String exerciseId, int sets) {
        return new GymWorkoutDefinition.Slot(
                id,
                exercise(exerciseId),
                target(
                        sets,
                        5,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RPE, 6, 8),
                        120,
                        80.0),
                null);
    }

    private static GymWorkoutDefinition.Target target(
            int sets,
            Integer min,
            Integer max,
            Long seconds,
            Double metres,
            GymWorkoutDefinition.EffortRange effort,
            Integer rest,
            Double load) {
        return new GymWorkoutDefinition.Target(sets, min, max, seconds, metres, effort, rest, load);
    }

    private static StrengthExercise exercise(String id) {
        return ExerciseCatalog.definition(id).exercise();
    }

    private static StrengthSession.SetEntry set(String id, int reps, double kg, Double effort) {
        return new StrengthSession.SetEntry(id, reps, kg, effort, false);
    }

    private static Tick tick(long millis) {
        return new Tick("boot", millis, NOW.plusMillis(millis));
    }

    private static GymExecution.Event last(GymExecution state) {
        return state.events().get(state.events().size() - 1);
    }

    private static StrengthSession actualSession(boolean warmUp) {
        return new StrengthSession(
                "previous",
                NOW.minus(Duration.ofDays(2)),
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "Actual history",
                List.of(
                        new StrengthSession.ExerciseSets(
                                exercise("bench-press"),
                                List.of(
                                        new StrengthSession.SetEntry(
                                                "prior-1", 8, 80, null, warmUp),
                                        new StrengthSession.SetEntry(
                                                "prior-2", 7, 75, null, warmUp)))));
    }

    private static GymProgramme programme(
            int version, double reference, GymProgramme.Effort effort) {
        var day = NOW.atOffset(ZoneOffset.UTC).toLocalDate();
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.TUESDAY),
                        1,
                        Set.of("full gym"),
                        45,
                        effort);
        return new GymProgramme(
                "programme",
                version,
                NOW,
                "Athlete chose",
                settings,
                day,
                day.plusDays(41),
                List.of(
                        new GymProgramme.Session(
                                "session",
                                day,
                                "Upper",
                                List.of(
                                        new GymProgramme.Prescription(
                                                "bench-press",
                                                2,
                                                5,
                                                8,
                                                null,
                                                null,
                                                6,
                                                8,
                                                120,
                                                "Preserved intention",
                                                reference)),
                                "Preserved")),
                List.of());
    }

    private static void rejects(Runnable command, String message) {
        checks++;
        try {
            command.run();
        } catch (IllegalArgumentException
                | NullPointerException
                | UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected rejection: " + message);
    }

    private static void equal(Object expected, Object actual, String message) {
        checks++;
        if (expected instanceof Number && actual instanceof Number) {
            if (Double.compare(((Number) expected).doubleValue(), ((Number) actual).doubleValue())
                    == 0) return;
        } else if (Objects.equals(expected, actual)) return;
        throw new AssertionError(message + ": expected " + expected + " but was " + actual);
    }

    private static void ok(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
