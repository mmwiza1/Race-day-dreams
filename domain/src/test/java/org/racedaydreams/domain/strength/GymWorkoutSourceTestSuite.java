package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.strength.GymExecution.SourceKind;
import org.racedaydreams.domain.strength.GymExecution.SourceReference;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Retained-source integrity checks never resolve historical exercises from today's catalogue. */
public final class GymWorkoutSourceTestSuite {
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 27);
    private static int checks;

    public static void main(String[] args) {
        exactProgrammeAndHistoricalDefinitions();
        programmeIdentityAndTargetsAreChecked();
        frozenMeasurementsControlEffortAndReference();
        savedRevisionsMatchInFull();
        manualRepeatsStayOpen();
        partialExecutionRepeatsPreserveIntentions();
        missingSourcesFailBeforeStorage();
        System.out.println("GymWorkoutSourceTestSuite: " + checks + " checks passed.");
    }

    private static void exactProgrammeAndHistoricalDefinitions() {
        var retained =
                programme(GymProgramme.Effort.RPE, List.of(prescription("bench-press", 80.0)));
        var prepared = GymWorkoutAdapter.fromProgramme(retained, "session");
        equal(
                retained.sessions().get(0),
                GymWorkoutSourceValidator.programme(
                        prepared.definition(), prepared.source(), retained),
                "Returns exact retained session including its date");
        var original = prepared.definition().slots().get(0);
        var renamed =
                new StrengthExercise(
                        original.exercise().id(),
                        "Historical bench name",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        true);
        var historical =
                copy(
                        prepared.definition(),
                        List.of(
                                new GymWorkoutDefinition.Slot(
                                        original.id(), renamed, original.target(), null)));
        equal(
                retained.sessions().get(0),
                GymWorkoutSourceValidator.programme(historical, prepared.source(), retained),
                "A changed current canonical name does not reinterpret frozen history");
        equal(
                "Historical bench name",
                historical.slots().get(0).exercise().name(),
                "Validation preserves the original exercise definition");

        var custom =
                new StrengthExercise(
                        "custom-push",
                        "My horizontal press",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false);
        var customProgramme =
                programme(GymProgramme.Effort.RPE, List.of(prescription(custom.id(), null)));
        var customPrepared =
                GymWorkoutAdapter.fromProgramme(
                        customProgramme, "session", Map.of(custom.id(), custom));
        equal(
                customProgramme.sessions().get(0),
                GymWorkoutSourceValidator.programme(
                        customPrepared.definition(), customPrepared.source(), customProgramme),
                "Explicit frozen custom exercises need no canonical lookup");
    }

    private static void programmeIdentityAndTargetsAreChecked() {
        var retained =
                programme(
                        GymProgramme.Effort.RPE,
                        List.of(
                                prescription("bench-press", 80.0),
                                prescription("dumbbell-row", null)));
        var prepared = GymWorkoutAdapter.fromProgramme(retained, "session");
        var workout = prepared.definition();
        var source = prepared.source();
        var first = workout.slots().get(0);
        var target = first.target();
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                workout,
                                new SourceReference(SourceKind.PROGRAMME, "other", 1, "session"),
                                retained),
                "wrong programme");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                workout,
                                new SourceReference(
                                        SourceKind.PROGRAMME, retained.id(), 2, "session"),
                                retained),
                "wrong version");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                workout,
                                new SourceReference(
                                        SourceKind.PROGRAMME, retained.id(), 1, "missing"),
                                retained),
                "missing session");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                new GymWorkoutDefinition(
                                        "wrong-hash",
                                        workout.revision(),
                                        workout.createdAt(),
                                        workout.title(),
                                        workout.slots()),
                                source,
                                retained),
                "wrong derived identity");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                new GymWorkoutDefinition(
                                        workout.id(),
                                        2,
                                        workout.createdAt(),
                                        workout.title(),
                                        workout.slots()),
                                source,
                                retained),
                "workout revision");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                new GymWorkoutDefinition(
                                        workout.id(),
                                        1,
                                        workout.createdAt().plusSeconds(1),
                                        workout.title(),
                                        workout.slots()),
                                source,
                                retained),
                "creation evidence");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                new GymWorkoutDefinition(
                                        workout.id(),
                                        1,
                                        workout.createdAt(),
                                        "A different session",
                                        workout.slots()),
                                source,
                                retained),
                "session title");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                copy(workout, List.of(workout.slots().get(1), first)),
                                source,
                                retained),
                "reordered slots");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                copy(workout, List.of(first)), source, retained),
                "omitted prescribed exercise");
        var wrongExercise =
                new GymWorkoutDefinition.Slot(first.id(), exercise("deadlift"), target, null);
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                copy(workout, List.of(wrongExercise, workout.slots().get(1))),
                                source,
                                retained),
                "same derived hash cannot authenticate a replaced exercise");
        var grouped =
                List.of(
                        new GymWorkoutDefinition.Slot(first.id(), first.exercise(), target, "pair"),
                        new GymWorkoutDefinition.Slot(
                                "slot-1",
                                workout.slots().get(1).exercise(),
                                workout.slots().get(1).target(),
                                "pair"));
        rejects(
                () -> GymWorkoutSourceValidator.programme(copy(workout, grouped), source, retained),
                "legacy programme did not prescribe supersets");
        var targets =
                List.of(
                        new GymWorkoutDefinition.Target(
                                1, 5, 8, null, null, target.effort(), 120, 80.0),
                        new GymWorkoutDefinition.Target(
                                2, 6, 8, null, null, target.effort(), 120, 80.0),
                        new GymWorkoutDefinition.Target(
                                2, 5, 9, null, null, target.effort(), 120, 80.0),
                        new GymWorkoutDefinition.Target(
                                2, 5, 8, null, null, target.effort(), 90, 80.0),
                        new GymWorkoutDefinition.Target(
                                2, 5, 8, null, null, target.effort(), 120, 82.5),
                        new GymWorkoutDefinition.Target(2, 5, 8, null, null, null, 120, 80.0),
                        new GymWorkoutDefinition.Target(
                                2,
                                5,
                                8,
                                null,
                                null,
                                new GymWorkoutDefinition.EffortRange(
                                        GymWorkoutDefinition.EffortScale.RIR, 2, 4),
                                120,
                                80.0));
        for (var changed : targets) {
            var tampered =
                    copy(
                            workout,
                            List.of(
                                    new GymWorkoutDefinition.Slot(
                                            first.id(), first.exercise(), changed, null),
                                    workout.slots().get(1)));
            equal(workout.id(), tampered.id(), "Tamper preserves the same source-derived identity");
            rejects(
                    () -> GymWorkoutSourceValidator.programme(tampered, source, retained),
                    "same source identity with changed target");
        }
    }

    private static void frozenMeasurementsControlEffortAndReference() {
        var timed = new GymProgramme.Prescription("plank", 2, 0, 0, 30, null, 6, 8, 90, "Hold");
        var distance =
                new GymProgramme.Prescription(
                        "farmers-carry", 2, 0, 0, null, 40.0, 6, 8, 90, "Carry");
        var bodyweight = prescription("push-up", 20.0);
        var retained =
                programme(
                        GymProgramme.Effort.RIR,
                        List.of(prescription("bench-press", 80.0), timed, distance, bodyweight));
        var prepared = GymWorkoutAdapter.fromProgramme(retained, "session");
        GymWorkoutSourceValidator.programme(prepared.definition(), prepared.source(), retained);
        checks++;
        var slots = prepared.definition().slots();
        equal(
                GymWorkoutDefinition.EffortScale.RIR,
                slots.get(0).target().effort().scale(),
                "Rep-based RIR is retained");
        equal(2.0, slots.get(0).target().effort().minimum(), "RPE maximum maps to minimum RIR");
        equal(4.0, slots.get(0).target().effort().maximum(), "RPE minimum maps to maximum RIR");
        equal(
                GymWorkoutDefinition.EffortScale.RPE,
                slots.get(1).target().effort().scale(),
                "Timed task keeps RPE rather than invalid RIR");
        equal(
                GymWorkoutDefinition.EffortScale.RPE,
                slots.get(2).target().effort().scale(),
                "Distance task keeps RPE");
        equal(
                null,
                slots.get(3).target().referenceLoadKg(),
                "Bodyweight exercises do not inherit an external load reference");
        var changed = new ArrayList<>(slots);
        var first = slots.get(0);
        changed.set(
                0,
                new GymWorkoutDefinition.Slot(
                        first.id(),
                        first.exercise(),
                        new GymWorkoutDefinition.Target(
                                2,
                                5,
                                8,
                                null,
                                null,
                                new GymWorkoutDefinition.EffortRange(
                                        GymWorkoutDefinition.EffortScale.RIR, 1, 3),
                                120,
                                80.0),
                        null));
        var changedEffort = copy(prepared.definition(), changed);
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                changedEffort, prepared.source(), retained),
                "incorrect RIR conversion");
        var duration = slots.get(1);
        changed = new ArrayList<>(slots);
        changed.set(
                1,
                new GymWorkoutDefinition.Slot(
                        duration.id(),
                        duration.exercise(),
                        new GymWorkoutDefinition.Target(
                                2, null, null, 31L, null, duration.target().effort(), 90, null),
                        null));
        var changedTime = copy(prepared.definition(), changed);
        rejects(
                () -> GymWorkoutSourceValidator.programme(changedTime, prepared.source(), retained),
                "changed duration");
    }

    private static void savedRevisionsMatchInFull() {
        var workout =
                GymWorkoutAdapter.fromProgramme(
                                programme(
                                        GymProgramme.Effort.RPE,
                                        List.of(prescription("bench-press", 80.0))),
                                "session")
                        .definition();
        var source = new SourceReference(SourceKind.SAVED_WORKOUT, workout.id(), 1, null);
        GymWorkoutSourceValidator.savedWorkout(workout, source, workout);
        checks++;
        rejects(
                () ->
                        GymWorkoutSourceValidator.savedWorkout(
                                workout,
                                new SourceReference(
                                        SourceKind.SAVED_WORKOUT, workout.id(), 2, null),
                                workout),
                "saved revision mismatch");
        var old = workout.slots().get(0);
        var renamed =
                new StrengthExercise(
                        old.exercise().id(),
                        "Changed saved name",
                        old.exercise().region(),
                        old.exercise().mode(),
                        old.exercise().estimateOneRepMax(),
                        old.exercise().measurement());
        rejects(
                () ->
                        GymWorkoutSourceValidator.savedWorkout(
                                copy(
                                        workout,
                                        List.of(
                                                new GymWorkoutDefinition.Slot(
                                                        old.id(), renamed, old.target(), null))),
                                source,
                                workout),
                "saved frozen exercise content differs");
        rejects(
                () ->
                        GymWorkoutSourceValidator.savedWorkout(
                                new GymWorkoutDefinition(
                                        workout.id(),
                                        1,
                                        workout.createdAt(),
                                        "Changed title",
                                        workout.slots()),
                                source,
                                workout),
                "saved title differs");
    }

    private static void manualRepeatsStayOpen() {
        var actual =
                new StrengthSession(
                        "previous-actual",
                        NOW,
                        ZoneOffset.UTC,
                        Duration.ofMinutes(25),
                        null,
                        "",
                        List.of(
                                new StrengthSession.ExerciseSets(
                                        exercise("bench-press"),
                                        List.of(
                                                new StrengthSession.SetEntry(
                                                        "warm", 5, 20, null, true))),
                                new StrengthSession.ExerciseSets(
                                        exercise("dumbbell-row"),
                                        List.of(
                                                new StrengthSession.SetEntry(
                                                        "work", 8, 24, 7.0, false)))));
        var prepared = GymWorkoutAdapter.repeatActual("repeat", 1, NOW, "Repeat", actual);
        GymWorkoutSourceValidator.repeatedActual(prepared.definition(), prepared.source(), actual);
        checks++;
        equal(1, prepared.definition().slots().size(), "Warm-up-only blocks are not repeated");
        equal(
                "dumbbell-row",
                prepared.definition().slots().get(0).exercise().id(),
                "Only actual working exercise structure is reused");
        equal(
                null,
                prepared.definition().slots().get(0).target(),
                "A previous actual performance is not an invented prescription");
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedActual(
                                prepared.definition(),
                                new SourceReference(
                                        SourceKind.ACTUAL_SESSION, "other-actual", 0, null),
                                actual),
                "wrong repeated actual");
        var old = prepared.definition().slots().get(0);
        var target = new GymWorkoutDefinition.Target(3, 8, 10, null, null, null, 60, null);
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedActual(
                                copy(
                                        prepared.definition(),
                                        List.of(
                                                new GymWorkoutDefinition.Slot(
                                                        old.id(), old.exercise(), target, null))),
                                prepared.source(),
                                actual),
                "manual actual cannot authenticate new targets");
    }

    private static void partialExecutionRepeatsPreserveIntentions() {
        var programme =
                programme(
                        GymProgramme.Effort.RPE,
                        List.of(
                                prescription("bench-press", 80.0),
                                prescription("dumbbell-row", null)));
        var prepared = GymWorkoutAdapter.fromProgramme(programme, "session");
        var engine =
                GymExecutionEngine.start(
                        "partial",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        new GymExecution.Tick("boot", 0, NOW));
        engine.saveSet(
                "slot-0",
                new StrengthSession.SetEntry("actual", 6, 65, 7.0, false),
                new GymExecution.Tick("boot", 1000, NOW.plusSeconds(1)));
        var finished =
                engine.finish(
                        new GymExecution.Tick("boot", 2000, NOW.plusSeconds(2)),
                        null,
                        "Partial evidence",
                        true);
        var repeated =
                GymWorkoutAdapter.repeatExecution(
                        "repeat-partial", 1, NOW, "Repeat intentions", finished);
        GymWorkoutSourceValidator.repeatedExecution(
                repeated.definition(), repeated.source(), finished);
        checks++;
        equal(2, repeated.definition().slots().size(), "Unperformed intentions remain available");
        equal(
                null,
                repeated.definition().slots().get(0).target().referenceLoadKg(),
                "A repeat never transfers an old optional load reference");
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedActual(
                                repeated.definition(),
                                repeated.source(),
                                finished.completedSession()),
                "Retained partial actual alone cannot authenticate unperformed intentions");
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedExecution(
                                prepared.definition(), repeated.source(), finished),
                "repeat retains no old reference load");
        var unfinished =
                GymExecutionEngine.start(
                                "unfinished",
                                GymExecution.Origin.PLANNED,
                                prepared.definition(),
                                prepared.source(),
                                ZoneOffset.UTC,
                                new GymExecution.Tick("boot", 0, NOW))
                        .snapshot();
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedExecution(
                                repeated.definition(), repeated.source(), unfinished),
                "unfinished execution is not a repeat source");
    }

    private static void missingSourcesFailBeforeStorage() {
        var programme =
                programme(GymProgramme.Effort.RPE, List.of(prescription("bench-press", 80.0)));
        var prepared = GymWorkoutAdapter.fromProgramme(programme, "session");
        rejects(
                () -> GymWorkoutSourceValidator.programme(prepared.definition(), null, programme),
                "missing programme source");
        rejects(
                () ->
                        GymWorkoutSourceValidator.programme(
                                prepared.definition(), prepared.source(), null),
                "missing retained programme");
        rejects(
                () -> GymWorkoutSourceValidator.programme(null, prepared.source(), programme),
                "missing frozen prescription");
        rejects(
                () ->
                        GymWorkoutSourceValidator.savedWorkout(
                                prepared.definition(), prepared.source(), prepared.definition()),
                "wrong saved source kind");
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedActual(
                                prepared.definition(), prepared.source(), null),
                "missing actual source");
        rejects(
                () ->
                        GymWorkoutSourceValidator.repeatedExecution(
                                prepared.definition(), prepared.source(), null),
                "missing execution source");
    }

    private static GymProgramme programme(
            GymProgramme.Effort effort, List<GymProgramme.Prescription> prescriptions) {
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.TUESDAY),
                        1,
                        Set.of("full gym"),
                        45,
                        effort);
        return new GymProgramme(
                "retained-programme",
                1,
                NOW,
                "Athlete chose",
                settings,
                DAY,
                DAY.plusDays(41),
                List.of(
                        new GymProgramme.Session(
                                "session", DAY, "Upper", prescriptions, "Preserved")),
                List.of());
    }

    private static GymProgramme.Prescription prescription(String exerciseId, Double reference) {
        return new GymProgramme.Prescription(
                exerciseId, 2, 5, 8, null, null, 6, 8, 120, "Preserved intent", reference);
    }

    private static StrengthExercise exercise(String id) {
        return Objects.requireNonNull(ExerciseCatalog.definition(id)).exercise();
    }

    private static GymWorkoutDefinition copy(
            GymWorkoutDefinition source, List<GymWorkoutDefinition.Slot> slots) {
        return new GymWorkoutDefinition(
                source.id(), source.revision(), source.createdAt(), source.title(), slots);
    }

    private static void equal(Object expected, Object actual, String reason) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(reason + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void rejects(Runnable action, String reason) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Should reject " + reason);
    }
}
