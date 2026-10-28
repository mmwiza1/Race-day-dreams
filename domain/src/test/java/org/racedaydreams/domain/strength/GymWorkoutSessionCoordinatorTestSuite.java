package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.strength.GymExecution.Origin;
import org.racedaydreams.domain.strength.GymExecution.Status;
import org.racedaydreams.domain.strength.GymWorkoutSessionCoordinator.Operation;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** IO failure replay around the actual execution engine, without Android or wall-clock sleeps. */
public final class GymWorkoutSessionCoordinatorTestSuite {
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");
    private static int checks;

    public static void main(String[] args) {
        noAutomaticStartOrRecovery();
        startFailureBeforeAndAfterCommit();
        failedNextStartCannotPresentPreviousHistory();
        setsArePublishedOnlyAfterAcknowledgement();
        lostSetAcknowledgementDoesNotDuplicateEvidence();
        finishRetryIsAtomicAndFrozen();
        plannedSourceSurvivesFinalisationRetry();
        explicitRecoveryNeverBridgesInterruptedTime();
        pausedRecoveryAndLostRecoveryAcknowledgement();
        aNewCoordinatorCanRecoverALostSetCommit();
        wrongAcknowledgementCannotPublishEvidence();
        exerciseActionsRemainEngineOwned();
        checkpointIsExplicitAndDiscardRetainsStagingOnly();
        invalidActionsNeverReachStorage();
        System.out.println("GymWorkoutSessionCoordinatorTestSuite: " + checks + " checks passed.");
    }

    private static void noAutomaticStartOrRecovery() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        equal(
                null,
                coordinator.observe().accepted(),
                "A new coordinator has no claimed saved workout");
        equal(
                null,
                coordinator.view().observed(),
                "Constructing a coordinator never invents execution");
        equal(
                0,
                repo.reads,
                "Construction and empty UI observation do not hydrate staged evidence");
        equal(0, repo.calls, "Construction does not write anything");
        equal(0, clock.calls, "Construction does not start a clock");
        rejects(() -> coordinator.pause(), "No implicit workout to pause");
        rejects(() -> coordinator.recoverExplicitly("absent"), "Missing staging remains missing");
        equal(0, repo.calls, "Missing recovery cannot create a workout");
        equal(
                null,
                coordinator.view().pending(),
                "Failed recovery read creates no fabricated pending snapshot");
    }

    private static void startFailureBeforeAndAfterCommit() {
        for (var failure : List.of(Failure.BEFORE, Failure.AFTER)) {
            var repo = new FakeRepository();
            var clock = new Clock();
            var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
            repo.next = failure;
            rejects(() -> start(coordinator), "Start acknowledgement failure is exposed");
            var pending = coordinator.view().pending();
            equal(Operation.START, pending.operation(), "Start uses a distinct retry operation");
            equal(
                    0L,
                    pending.expectedRevision(),
                    "Initial write has no invented persisted revision");
            equal(
                    null,
                    coordinator.view().accepted(),
                    "Unacknowledged start is not claimed durable");
            equal(
                    null,
                    coordinator.view().observed(),
                    "First-start failure has no accepted display evidence");
            equal(0, pending.snapshot().actualSets().size(), "A prescription creates no actuals");
            allLaterActionsBlocked(coordinator);
            int clockCalls = clock.calls;
            clock.millis = 10000;
            var saved = coordinator.retryPending();
            equal(clockCalls, clock.calls, "Retry does not recapture a later clock observation");
            equal(
                    pending.snapshot(),
                    saved.accepted().execution(),
                    "The exact initial snapshot is retried");
            same(
                    pending.snapshot(),
                    repo.lastSnapshot,
                    "The same immutable object reaches the retry");
            equal(
                    1L,
                    saved.accepted().revision(),
                    "Lost start ack does not create another revision");
            equal(1, repo.rows.size(), "There is one execution after start retry");
            equal(null, saved.pending(), "Only acknowledgement clears the pending write");
            equal(
                    0L,
                    saved.accepted().execution().activeMillis(),
                    "Waiting for storage is not silently added at start retry");
        }
    }

    private static void setsArePublishedOnlyAfterAcknowledgement() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        start(coordinator);
        var before = coordinator.view().accepted();
        repo.next = Failure.BEFORE;
        clock.millis = 5000;
        rejects(
                () -> coordinator.saveSet("bench", set("actual", 65)),
                "Failed actual-set write is exposed");
        var pending = coordinator.view().pending();
        equal(Operation.SAVE, pending.operation(), "Actual set uses staging save");
        equal(
                before.revision(),
                pending.expectedRevision(),
                "Expected revision is captured before IO");
        equal(
                before,
                coordinator.view().accepted(),
                "Accepted snapshot is unchanged after failed save");
        equal(
                0,
                coordinator.observe().observed().actualSets().size(),
                "UI cannot call an unsaved set durable");
        equal(
                1,
                pending.snapshot().actualSets().size(),
                "Pending entered evidence is retained for retry");
        equal(
                65.0,
                pending.snapshot().actualSets().get(0).actual().kilograms(),
                "Actual load is not the 80kg reference");
        equal(
                null,
                pending.snapshot().completedSession(),
                "Staging never claims completed training evidence");
        allLaterActionsBlocked(coordinator);
        int clockCalls = clock.calls;
        clock.millis = 20000;
        equal(
                before.execution(),
                coordinator.observe().observed(),
                "Pending observation stays at accepted evidence");
        equal(clockCalls, clock.calls, "Pending UI does not mutate its frozen retry clock");
        coordinator.retryPending();
        equal(
                5000L,
                coordinator.view().accepted().execution().activeMillis(),
                "Retry preserves the known captured time");
        same(pending.snapshot(), repo.lastSnapshot, "Save retry uses exact frozen set/timing");
        equal(
                2L,
                coordinator.view().accepted().revision(),
                "Actual set advances one persisted revision");
        equal(
                1,
                coordinator.view().accepted().execution().actualSets().size(),
                "Acknowledged actual becomes visible");
        coordinator.saveSet("bench", set("actual", 65));
        equal(
                1,
                coordinator.view().accepted().execution().actualSets().size(),
                "Stable set ID is idempotent after success");
        equal(
                2,
                coordinator.view().accepted().execution().events().size(),
                "Idempotent set retry adds no duplicate event");
        equal(
                2L,
                coordinator.view().accepted().revision(),
                "Identical saved snapshot may acknowledge the same revision");
    }

    private static void failedNextStartCannotPresentPreviousHistory() {
        for (var failure : List.of(Failure.BEFORE, Failure.AFTER)) {
            var repo = new FakeRepository();
            var clock = new Clock();
            var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
            start(coordinator);
            clock.millis = 1000;
            coordinator.saveSet("bench", set("first-actual", 65));
            clock.millis = 2000;
            coordinator.finish(null, "Completed history", true);
            var previous = coordinator.view().accepted();
            repo.next = failure;
            clock.millis = 3000;
            rejects(
                    () ->
                            coordinator.start(
                                    "next-session", Origin.FREE, null, null, ZoneOffset.UTC),
                    "A second workout start may fail before or after commit");
            equal(
                    null,
                    coordinator.view().accepted(),
                    "New pending workout does not claim previous completed actuals");
            equal(
                    null,
                    coordinator.view().observed(),
                    "Previous workout is not displayed beneath new pending identity");
            equal(
                    "next-session",
                    coordinator.view().pending().snapshot().id(),
                    "Pending new workout retains its distinct identity");
            equal(
                    previous,
                    repo.rows.get("execution"),
                    "Starting again cannot rewrite old execution history");
            equal(
                    previous.execution().completedSession(),
                    repo.completed.get("execution"),
                    "Previous completed actual remains durable history");
            coordinator.retryPending();
            equal(
                    "next-session",
                    coordinator.view().accepted().execution().id(),
                    "Retry accepts only the new workout");
            equal(
                    1,
                    repo.completed.size(),
                    "New start cannot manufacture another completed workout");
        }
    }

    private static void lostSetAcknowledgementDoesNotDuplicateEvidence() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        start(coordinator);
        repo.next = Failure.AFTER;
        clock.millis = 1000;
        rejects(() -> coordinator.saveSet("bench", set("lost", 60)), "Lost set ack is exposed");
        equal(
                1,
                repo.rows.get("execution").execution().actualSets().size(),
                "Fault occurred after a real committed set");
        equal(
                0,
                coordinator.view().accepted().execution().actualSets().size(),
                "Lost ack does not advance accepted state early");
        var pending = coordinator.view().pending();
        clock.millis = 30000;
        coordinator.retryPending();
        same(pending.snapshot(), repo.lastSnapshot, "Lost-ack retry does not recreate the command");
        equal(1L, repo.lastExpected, "Lost-ack retry retains original expected revision");
        equal(2L, coordinator.view().accepted().revision(), "Lost ack does not advance twice");
        equal(
                1,
                repo.rows.get("execution").execution().actualSets().size(),
                "No duplicate set after lost ack");
        equal(
                2,
                repo.rows.get("execution").execution().events().size(),
                "No duplicate set event after lost ack");
        equal(
                1000L,
                coordinator.view().accepted().execution().activeMillis(),
                "No timing recomputation during retry");
    }

    private static void finishRetryIsAtomicAndFrozen() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        start(coordinator);
        clock.millis = 1000;
        coordinator.saveSet("bench", set("performed", 62.5));
        repo.next = Failure.AFTER;
        clock.millis = 2000;
        rejects(
                () -> coordinator.finish(7.0, "Actual session", true),
                "Lost finalisation ack is exposed");
        var pending = coordinator.view().pending();
        equal(
                Operation.FINISH,
                pending.operation(),
                "Finalisation uses the atomic repository path");
        equal(
                Status.ACTIVE,
                coordinator.view().accepted().execution().status(),
                "Unacknowledged finish is not published complete");
        equal(
                null,
                coordinator.view().observed().completedSession(),
                "UI must not treat pending completedSession as accepted evidence");
        equal(1, repo.completed.size(), "Actual evidence committed despite the simulated lost ack");
        allLaterActionsBlocked(coordinator);
        clock.millis = 60000;
        coordinator.retryPending();
        equal(
                Status.FINISHED,
                coordinator.view().accepted().execution().status(),
                "Finish is published after exact acknowledgement");
        same(
                pending.snapshot(),
                repo.lastSnapshot,
                "Finalisation retry keeps evidence and finish time");
        equal(
                2000L,
                coordinator.view().accepted().execution().activeMillis(),
                "Waiting for finalisation is not exercise");
        equal(1, repo.completed.size(), "Actual training evidence finalised once");
        equal(
                1,
                repo.completedWrites,
                "Atomic finish retry does not insert another training session");
        equal(
                62.5,
                repo.completed.get("execution").exercises().get(0).sets().get(0).kilograms(),
                "Final actual remains independently entered");
        equal(
                null,
                coordinator.view().accepted().execution().plannedAssociation(),
                "Spontaneous actual does not satisfy a plan");
        equal(
                true,
                coordinator.view().accepted().execution().partial(),
                "Early finish remains partial");
        coordinator.finish(7.0, "Actual session", true);
        equal(1, repo.completedWrites, "An acknowledged duplicate finish remains idempotent");
    }

    private static void explicitRecoveryNeverBridgesInterruptedTime() {
        var repo = new FakeRepository();
        var firstClock = new Clock();
        var first = new GymWorkoutSessionCoordinator(repo, firstClock);
        start(first);
        firstClock.millis = 5000;
        first.saveSet("bench", set("retained", 65));
        var clock = new Clock();
        clock.continuity = "after-reboot";
        clock.wall = NOW.plusSeconds(14400);
        clock.millis = 20;
        var recovered = new GymWorkoutSessionCoordinator(repo, clock);
        int beforeCalls = repo.calls;
        recovered.observe();
        equal(beforeCalls, repo.calls, "A new process does not restart the workout clock");
        equal(null, recovered.view().accepted(), "No implicit adoption of unfinished training");
        recovered.recoverExplicitly("execution");
        equal(
                Status.RECOVERY_REQUIRED,
                recovered.view().accepted().execution().status(),
                "Active interruption requires explicit resume");
        equal(
                5000L,
                recovered.view().accepted().execution().activeMillis(),
                "Hours of interruption are not exercise");
        equal(
                1,
                recovered.view().accepted().execution().actualSets().size(),
                "Accepted sets survive process recovery");
        clock.millis = 20000;
        recovered.observe();
        equal(
                5000L,
                recovered.view().observed().activeMillis(),
                "Recovery-required clock does not advance activity");
        equal(
                5000L,
                recovered.view().observed().recordedElapsedMillis(),
                "Unknown gap is not fabricated elapsed time");
        recovered.resume();
        clock.millis = 21000;
        recovered.checkpoint();
        equal(
                6000L,
                recovered.view().accepted().execution().activeMillis(),
                "Only explicit resumed monotonic time is added");
        equal(
                1,
                recovered.view().accepted().execution().actualSets().size(),
                "Resume/checkpoint adds no duplicate actuals");
    }

    private static void plannedSourceSurvivesFinalisationRetry() {
        var day = LocalDate.of(2026, 10, 25);
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.SUNDAY),
                        1,
                        Set.of("full gym"),
                        45,
                        GymProgramme.Effort.RPE);
        var programme =
                new GymProgramme(
                        "preserved-programme",
                        3,
                        NOW,
                        "Athlete chose",
                        settings,
                        day,
                        day.plusDays(41),
                        List.of(
                                new GymProgramme.Session(
                                        "preserved-session",
                                        day,
                                        "Upper",
                                        List.of(
                                                new GymProgramme.Prescription(
                                                        "bench-press",
                                                        1,
                                                        5,
                                                        8,
                                                        null,
                                                        null,
                                                        6,
                                                        8,
                                                        120,
                                                        "Preserved",
                                                        80.0)),
                                        "Frozen intent")),
                        List.of());
        var prepared = GymWorkoutAdapter.fromProgramme(programme, "preserved-session");
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        coordinator.start(
                "planned",
                Origin.PLANNED,
                prepared.definition(),
                prepared.source(),
                ZoneOffset.UTC);
        clock.millis = 1000;
        coordinator.saveSet("slot-0", set("performed", 60));
        repo.next = Failure.AFTER;
        clock.millis = 2000;
        rejects(
                () -> coordinator.finish(null, "Actual", false),
                "Planned finalisation ack can be lost after commit");
        equal(
                prepared.source(),
                repo.associations.get("planned"),
                "Atomic repository commit receives the frozen original association");
        equal(
                null,
                coordinator.view().accepted().execution().plannedAssociation(),
                "Unacknowledged finalisation is not declared planned completion");
        coordinator.retryPending();
        equal(
                prepared.source(),
                coordinator.view().accepted().execution().plannedAssociation(),
                "Retry retains exact programme identity/version/session");
        equal(3, programme.version(), "Coordinator never rewrites a preserved programme");
        equal(1, repo.completedWrites, "Planned training evidence commits only once");
        equal(
                1,
                repo.associations.size(),
                "Lost finalisation acknowledgement does not duplicate association");
        equal(
                80.0,
                coordinator
                        .view()
                        .accepted()
                        .execution()
                        .prescription()
                        .slots()
                        .get(0)
                        .target()
                        .referenceLoadKg(),
                "Original load intention stays separate");
        equal(
                60.0,
                repo.completed.get("planned").exercises().get(0).sets().get(0).kilograms(),
                "Independent actual load survives finalisation retry");
    }

    private static void pausedRecoveryAndLostRecoveryAcknowledgement() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var first = new GymWorkoutSessionCoordinator(repo, clock);
        start(first);
        clock.millis = 3000;
        first.pause();
        clock.millis = 9000;
        first.checkpoint();
        var saved = first.view().accepted();
        var recoveredClock = new Clock();
        recoveredClock.continuity = "new-process";
        recoveredClock.millis = 1;
        recoveredClock.wall = NOW.plusSeconds(3600);
        var recovered = new GymWorkoutSessionCoordinator(repo, recoveredClock);
        repo.next = Failure.AFTER;
        rejects(
                () -> recovered.recoverExplicitly("execution"),
                "Recovery acknowledgement can fail after commit");
        equal(
                saved,
                recovered.view().accepted(),
                "Recovery failure keeps the last acknowledged staged state");
        var pending = recovered.view().pending();
        equal(
                Status.PAUSED,
                pending.snapshot().status(),
                "Paused workout remains paused after recovery");
        equal(
                3000L,
                pending.snapshot().activeMillis(),
                "Paused recovery retains actual active time");
        equal(
                9000L,
                pending.snapshot().recordedElapsedMillis(),
                "Known paused time survives without invented gap");
        recoveredClock.millis = 50000;
        recovered.retryPending();
        same(pending.snapshot(), repo.lastSnapshot, "Recovery retry does not append another gap");
        equal(
                saved.execution().events().size() + 1,
                recovered.view().accepted().execution().events().size(),
                "One interruption yields one durable recovery event");
        equal(
                Status.PAUSED,
                recovered.view().accepted().execution().status(),
                "Acknowledged recovery never auto-resumes a pause");
    }

    private static void aNewCoordinatorCanRecoverALostSetCommit() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var first = new GymWorkoutSessionCoordinator(repo, clock);
        start(first);
        repo.next = Failure.AFTER;
        clock.millis = 2000;
        rejects(
                () -> first.saveSet("bench", set("committed", 70)),
                "UI/process dies after committed set before ack");
        var nextClock = new Clock();
        nextClock.continuity = "restarted";
        var next = new GymWorkoutSessionCoordinator(repo, nextClock);
        next.recoverExplicitly("execution");
        equal(
                1,
                next.view().accepted().execution().actualSets().size(),
                "Committed actual is recovered even if old UI never received ack");
        equal(
                2000L,
                next.view().accepted().execution().activeMillis(),
                "Accepted pre-interruption time stays intact");
        equal(
                null,
                next.view().accepted().execution().completedSession(),
                "Interrupted workout is not silently complete");
        equal(0, repo.completed.size(), "Staged sets are not PB/training evidence until finish");
    }

    private static void wrongAcknowledgementCannotPublishEvidence() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        repo.next = Failure.BAD_ACK;
        rejects(() -> start(coordinator), "An incorrect repository acknowledgement is rejected");
        equal(
                null,
                coordinator.view().accepted(),
                "Wrong acknowledgement cannot publish accepted evidence");
        equal(
                Operation.START,
                coordinator.view().pending().operation(),
                "Exact pending snapshot survives invalid ack");
        coordinator.retryPending();
        equal(
                1L,
                coordinator.view().accepted().revision(),
                "Valid idempotent retry accepts the original start");
        var before = coordinator.view().accepted();
        clock.millis = 1000;
        repo.next = Failure.BAD_SNAPSHOT;
        rejects(
                () -> coordinator.saveSet("bench", set("actual", 65)),
                "A valid revision cannot excuse different acknowledged evidence");
        equal(
                before,
                coordinator.view().accepted(),
                "Wrong payload does not replace accepted actuals");
        equal(
                1,
                coordinator.view().pending().snapshot().actualSets().size(),
                "Original intended actual survives wrong payload ack");
        coordinator.retryPending();
        equal(
                1,
                coordinator.view().accepted().execution().actualSets().size(),
                "Retry acknowledges the correct committed payload");
    }

    private static void exerciseActionsRemainEngineOwned() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        start(coordinator);
        var original = coordinator.view().accepted().execution().prescription();
        clock.millis = 1000;
        coordinator.saveSet("bench", set("original-set", 65));
        coordinator.replaceExercise(
                "bench", ExerciseCatalog.definition("dumbbell-bench-press").exercise());
        equal(
                null,
                coordinator.view().accepted().execution().slots().get(0).target().referenceLoadKg(),
                "Substitution clears incompatible load transfer");
        equal(
                "bench-press",
                coordinator.view().accepted().execution().actualSets().get(0).exercise().id(),
                "Old actual retains the original frozen exercise");
        equal(
                original,
                coordinator.view().accepted().execution().prescription(),
                "Today-only replacement cannot rewrite the prescription");
        coordinator.addExercises(
                List.of(
                        new GymWorkoutDefinition.Slot(
                                "core",
                                ExerciseCatalog.definition("plank").exercise(),
                                null,
                                null)));
        coordinator.selectExercise("core");
        equal(
                "core",
                coordinator.view().accepted().execution().selectedSlotId(),
                "Selection is delegated and durably accepted");
        coordinator.saveSet(
                "core", new StrengthSession.SetEntry("hold", 0, 0, null, false, 20L, null));
        equal(
                2,
                coordinator.view().accepted().execution().actualSets().size(),
                "Time and rep actuals share the one engine");
        equal(
                Long.valueOf(20),
                coordinator
                        .view()
                        .accepted()
                        .execution()
                        .actualSets()
                        .get(1)
                        .actual()
                        .durationSeconds(),
                "Timed actual does not become invented reps");
    }

    private static void checkpointIsExplicitAndDiscardRetainsStagingOnly() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        start(coordinator);
        int calls = repo.calls;
        for (int i = 1; i <= 100; i++) {
            clock.millis = i * 1000L;
            coordinator.observe();
        }
        equal(calls, repo.calls, "One hundred UI clock observations cause no database writes");
        equal(
                0L,
                coordinator.view().accepted().execution().activeMillis(),
                "Observed clock is not advertised as durable time");
        equal(
                100000L,
                coordinator.view().observed().activeMillis(),
                "Display uses the real engine's monotonic timing");
        coordinator.checkpoint();
        equal(calls + 1, repo.calls, "Explicit checkpoint saves once");
        equal(
                100000L,
                coordinator.view().accepted().execution().activeMillis(),
                "Checkpoint acknowledges known clock evidence");
        coordinator.discard();
        equal(
                Status.DISCARDED,
                coordinator.view().accepted().execution().status(),
                "Discard is an explicit durable execution event");
        equal(0, repo.completed.size(), "Discard does not create completed strength evidence");
        equal(
                null,
                coordinator.view().accepted().execution().completedSession(),
                "Discarded staging remains outside PB evidence");
        coordinator.start("next", Origin.FREE, null, null, ZoneOffset.UTC);
        equal(
                "next",
                coordinator.view().accepted().execution().id(),
                "A finished/discarded workout does not trap the athlete");
    }

    private static void invalidActionsNeverReachStorage() {
        var repo = new FakeRepository();
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repo, clock);
        start(coordinator);
        int before = repo.calls;
        rejects(
                () -> coordinator.finish(null, "No actuals", false),
                "Empty targets never become actual training");
        equal(before, repo.calls, "Invalid finish never reaches storage");
        equal(
                null,
                coordinator.view().pending(),
                "Invalid domain action does not invent a pending write");
        clock.continuity = "unannounced-reboot";
        rejects(() -> coordinator.pause(), "Clock continuity cannot be silently bridged");
        equal(before, repo.calls, "Clock discontinuity never writes invented duration");
        equal(
                Status.ACTIVE,
                coordinator.view().accepted().execution().status(),
                "Invalid action cannot mutate accepted evidence");
        clock.continuity = "boot";
        coordinator.pause();
        before = repo.calls;
        rejects(
                () -> coordinator.saveSet("bench", set("paused", 60)),
                "Cannot enter actuals while paused");
        equal(before, repo.calls, "Paused entry is rejected before persistence");
    }

    private static void allLaterActionsBlocked(GymWorkoutSessionCoordinator coordinator) {
        var pending = coordinator.view().pending();
        for (Runnable action :
                List.<Runnable>of(
                        () -> coordinator.start("other", Origin.FREE, null, null, ZoneOffset.UTC),
                        () -> coordinator.recoverExplicitly("execution"),
                        () -> coordinator.saveSet("bench", set("later", 70)),
                        () -> coordinator.pause(),
                        () -> coordinator.resume(),
                        () -> coordinator.finish(null, "Later", true),
                        () -> coordinator.discard(),
                        () -> coordinator.checkpoint(),
                        () -> coordinator.selectExercise("bench"),
                        () ->
                                coordinator.addExercises(
                                        List.of(
                                                new GymWorkoutDefinition.Slot(
                                                        "later",
                                                        ExerciseCatalog.definition("plank")
                                                                .exercise(),
                                                        null,
                                                        null))),
                        () ->
                                coordinator.replaceExercise(
                                        "bench",
                                        ExerciseCatalog.definition("push-up").exercise()))) {
            rejects(action, "An unresolved pending acknowledgement blocks later mutations");
            same(
                    pending,
                    coordinator.view().pending(),
                    "Blocked mutation preserves the exact pending operation");
        }
    }

    private static void start(GymWorkoutSessionCoordinator coordinator) {
        var target =
                new GymWorkoutDefinition.Target(
                        2,
                        5,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RPE, 6, 8),
                        120,
                        80.0);
        var definition =
                new GymWorkoutDefinition(
                        "workout",
                        1,
                        NOW,
                        "Frozen workout",
                        List.of(
                                new GymWorkoutDefinition.Slot(
                                        "bench",
                                        ExerciseCatalog.definition("bench-press").exercise(),
                                        target,
                                        null)));
        coordinator.start("execution", Origin.CUSTOM, definition, null, ZoneOffset.UTC);
    }

    private static StrengthSession.SetEntry set(String id, double load) {
        return new StrengthSession.SetEntry(id, 7, load, 7.0, false);
    }

    private enum Failure {
        NONE,
        BEFORE,
        AFTER,
        BAD_ACK,
        BAD_SNAPSHOT
    }

    private static final class Clock implements GymWorkoutSessionCoordinator.TickSource {
        String continuity = "boot";
        long millis;
        Instant wall = NOW;
        int calls;

        @Override
        public GymExecution.Tick now() {
            calls++;
            return new GymExecution.Tick(continuity, millis, wall.plusMillis(millis));
        }
    }

    /** Faults occur before or after real retained in-memory commits, not just before every call. */
    private static final class FakeRepository implements GymExecutionRepository {
        final Map<String, Stored> rows = new LinkedHashMap<>();
        final Map<String, StrengthSession> completed = new LinkedHashMap<>();
        final Map<String, GymExecution.SourceReference> associations = new LinkedHashMap<>();
        Failure next = Failure.NONE;
        int calls, reads, completedWrites;
        GymExecution lastSnapshot;
        long lastExpected;

        @Override
        public Stored start(GymExecution snapshot) {
            return persist(snapshot, 0, Operation.START);
        }

        @Override
        public Stored save(GymExecution snapshot, long expected) {
            return persist(snapshot, expected, Operation.SAVE);
        }

        @Override
        public Stored finish(GymExecution snapshot, long expected) {
            return persist(snapshot, expected, Operation.FINISH);
        }

        @Override
        public Stored load(String id) {
            reads++;
            return rows.get(id);
        }

        private Stored persist(GymExecution snapshot, long expected, Operation operation) {
            calls++;
            lastSnapshot = snapshot;
            lastExpected = expected;
            var failure = next;
            next = Failure.NONE;
            if (failure == Failure.BEFORE)
                throw new IllegalStateException("Injected failure before commit");
            var previous = rows.get(snapshot.id());
            Stored stored;
            if (previous != null && previous.execution().equals(snapshot)) stored = previous;
            else {
                if (operation == Operation.START
                        ? previous != null
                        : previous == null || previous.revision() != expected)
                    throw new IllegalArgumentException("Conflicting revision");
                stored = new Stored(snapshot, expected + 1);
                rows.put(snapshot.id(), stored);
                if (operation == Operation.FINISH) {
                    var prior = completed.put(snapshot.id(), snapshot.completedSession());
                    if (prior == null) completedWrites++;
                    else if (!prior.equals(snapshot.completedSession()))
                        throw new AssertionError("Actual changed");
                    if (snapshot.plannedAssociation() != null)
                        associations.put(snapshot.id(), snapshot.plannedAssociation());
                }
            }
            if (failure == Failure.AFTER)
                throw new IllegalStateException("Injected lost acknowledgement after commit");
            if (failure == Failure.BAD_ACK) return new Stored(snapshot, stored.revision() + 7);
            if (failure == Failure.BAD_SNAPSHOT)
                return new Stored(previous.execution(), stored.revision());
            return stored;
        }

        @Override
        public List<Summary> unfinished() {
            throw new UnsupportedOperationException("Not queried by the coordinator");
        }

        @Override
        public void saveWorkout(GymWorkoutDefinition workout, GymExecution.SourceReference source) {
            throw new UnsupportedOperationException("Separate workout library operation");
        }

        @Override
        public GymWorkoutDefinition workout(String id, int revision) {
            throw new UnsupportedOperationException("Separate workout library operation");
        }

        @Override
        public List<Preference> preferences() {
            throw new UnsupportedOperationException("Separate workout library operation");
        }

        @Override
        public void savePreference(Preference preference) {
            throw new UnsupportedOperationException("Separate workout library operation");
        }
    }

    private static void rejects(Runnable action, String reason) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected rejection: " + reason);
    }

    private static void equal(Object expected, Object actual, String reason) {
        checks++;
        if (!Objects.equals(expected, actual))
            throw new AssertionError(reason + ": expected " + expected + ", got " + actual);
    }

    private static void same(Object expected, Object actual, String reason) {
        checks++;
        if (expected != actual) throw new AssertionError(reason);
    }
}
