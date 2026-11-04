package org.racedaydreams.data;

import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import org.json.JSONObject;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.portability.BackupManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/**
 * Exact authored association snapshots use the existing choice projection and restore transaction.
 */
@SuppressWarnings("deprecation")
public final class AuthoredGymAssociationRepositoryTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 28);
    private SqliteStrengthProgrammingRepository programmes;
    private SqliteGymAssociationRepository associations;
    private SqliteGymExecutionRepository gym;
    private StrengthProgramme programme;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        programmes =
                new SqliteStrengthProgrammingRepository(
                        getContext(), Clock.fixed(NOW, ZoneOffset.UTC));
        associations = new SqliteGymAssociationRepository(getContext());
        gym = new SqliteGymExecutionRepository(getContext());
        programme = programme(1);
        programmes.activate(programme, null, "activate", Collections.emptyList());
        finish("actual", false);
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testDiscoveryCountAndExplicitKeepPreserveActualsTargetsAndVersion() {
        var original = gym.load("actual");
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        var p = associations.proposals("actual", NOW).get(0);
        assertTrue(p.authored());
        assertEquals(programme.workouts().get(0), p.authoredPrescription());
        assertNull(p.authoredPrescription().definition().slots().get(0).target().restSeconds());
        var count = choice("count", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        associations.decide(count);
        assertEquals(count, associations.decide(count));
        assertEquals(
                "authored:v1",
                new DevelopmentStore(getContext()).gymLinks().get("actual").versionKey());
        assertTrue(associations.proposals("actual", NOW).isEmpty());
        var reviewed = associations.reviewCurrent("actual", NOW);
        associations.decide(choice("keep", reviewed, GymWorkoutAssociation.Choice.KEEP_EXTRA));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        assertEquals(original, gym.load("actual"));
        assertEquals(programme, programmes.programme(programme.id(), 1));
        assertEquals(1, programmes.currentReference().revision());
        assertEquals(2, countDecisions());
        reject(() -> associations.decide(count));
    }

    public void testCompetingActualAndChangedCurrentVersionRejectStaleCount() {
        finish("other", false);
        var first = associations.proposals("actual", NOW).get(0);
        var other = associations.proposals("other", NOW).get(0);
        associations.decide(
                choice("count", first, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        reject(
                () ->
                        associations.decide(
                                choice(
                                        "compete",
                                        other,
                                        GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)));
        associations.decide(choice("keep", first, GymWorkoutAssociation.Choice.KEEP_EXTRA));
        programmes.activate(
                programme(2), programmes.currentReference(), "next", Collections.emptyList());
        reject(
                () ->
                        associations.decide(
                                choice(
                                        "stale",
                                        first,
                                        GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)));
        assertEquals(2, programmes.currentReference().revision());
        assertEquals(programme, programmes.programme(programme.id(), 1));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
    }

    public void testEditedActualRequiresReviewAndKeepStillUsesHistoricalPrescription() {
        var p = associations.proposals("actual", NOW).get(0);
        associations.decide(choice("count", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        var stored = gym.load("actual");
        var actual = new CoachingStore(getContext()).strength().get(0);
        var edited =
                new StrengthSession(
                        actual.id(),
                        actual.startedAt(),
                        actual.offset(),
                        actual.duration(),
                        null,
                        "Corrected actual",
                        Collections.singletonList(
                                new StrengthSession.ExerciseSets(
                                        actual.exercises().get(0).exercise(),
                                        Collections.singletonList(
                                                new StrengthSession.SetEntry(
                                                        "edited", 5, 65, 9.0, false)))));
        new CoachingStore(getContext()).saveStrength(edited);
        reject(
                () ->
                        associations.decide(
                                choice("stale-keep", p, GymWorkoutAssociation.Choice.KEEP_EXTRA)));
        var fresh = associations.reviewCurrent("actual", NOW);
        assertEquals(edited, fresh.actual());
        assertEquals(p.authoredPrescription(), fresh.authoredPrescription());
        associations.decide(
                choice("reviewed-keep", fresh, GymWorkoutAssociation.Choice.KEEP_EXTRA));
        assertEquals(stored.execution(), gym.load("actual").execution());
        assertEquals(edited, new CoachingStore(getContext()).strength().get(0));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        roundTrip();
        assertEquals(edited, new CoachingStore(getContext()).strength().get(0));
        assertEquals(
                p.authoredPrescription(),
                associations.current("actual").proposal().authoredPrescription());
    }

    public void testEnvelopeNineRoundTripRetainsVersionTwoDecisionAndRejectsCorruptSnapshot()
            throws Exception {
        var p = associations.proposals("actual", NOW).get(0);
        var decision = choice("count", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        associations.decide(decision);
        var settings = new SqliteSettingsRepository(getContext());
        settings.putSetting("sentinel", "preserved");
        var envelope = envelope();
        assertEquals(9, envelope.getInt("version"));
        var row =
                envelope.getJSONObject("data")
                        .getJSONObject("strengthEngine")
                        .getJSONObject("associations")
                        .getJSONArray("decisions")
                        .getJSONObject(0);
        assertEquals(2, row.getInt("version"));
        manager().restoreFrom(input(envelope));
        assertEquals(decision, associations.current("actual"));
        assertEquals(programme, programmes.programme(programme.id(), 1));
        row.getJSONObject("proposal")
                .getJSONObject("prescription")
                .getJSONObject("definition")
                .getJSONArray("slots")
                .getJSONObject(0)
                .getJSONObject("target")
                .put("sets", 4);
        try {
            manager().restoreFrom(input(envelope));
            fail("Changed prescription must reject before replacement");
        } catch (IOException expected) {
        }
        assertEquals(decision, associations.current("actual"));
        assertEquals("preserved", settings.getSetting("sentinel"));
        assertEquals(programme, programmes.programme(programme.id(), 1));
    }

    public void testChoiceProjectionSqlFailureRollsBackDecisionAndCoachingLink() {
        var p = associations.proposals("actual", NOW).get(0);
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER fail_authored_choice BEFORE INSERT ON strength_association_targets"
                        + " BEGIN SELECT RAISE(ABORT,'injected'); END");
        try {
            try {
                associations.decide(
                        choice("fail", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
                fail("Expected SQL rollback");
            } catch (SQLiteException expected) {
            }
            assertNull(associations.current("actual"));
            assertEquals(0, countDecisions());
            assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
            assertEquals(GymExecution.Origin.FREE, gym.load("actual").execution().origin());
        } finally {
            db.execSQL("DROP TRIGGER fail_authored_choice");
        }
    }

    public void testWarmUpOnlyEvidenceDoesNotSuggestAuthoredAssociation() {
        finish("warm-only", true);
        assertTrue(associations.proposals("warm-only", NOW).isEmpty());
        assertFalse(associations.proposals("actual", NOW).isEmpty());
        assertNull(associations.current("actual"));
    }

    private void finish(String id, boolean warm) {
        var engine =
                GymExecutionEngine.start(
                        id, GymExecution.Origin.FREE, null, null, ZoneOffset.UTC, tick(0));
        var initial = gym.start(engine.snapshot());
        engine.addExercises(
                Collections.singletonList(
                        new GymWorkoutDefinition.Slot(
                                "bench",
                                ExerciseCatalog.definition("bench-press").exercise(),
                                null,
                                null)),
                tick(1000));
        engine.saveSet(
                "bench", new StrengthSession.SetEntry(id + "-set", 6, 60, 7.0, warm), tick(2000));
        gym.finish(engine.finish(tick(3000), null, "Actual evidence", false), initial.revision());
    }

    private static GymExecution.Tick tick(long ms) {
        return new GymExecution.Tick("clock", ms, NOW.minusSeconds(3600).plusMillis(ms));
    }

    private static StrengthProgramme programme(int revision) {
        var slots =
                Collections.singletonList(
                        new GymWorkoutDefinition.Slot(
                                "bench",
                                ExerciseCatalog.definition("bench-press").exercise(),
                                new GymWorkoutDefinition.Target(
                                        3,
                                        5,
                                        8,
                                        null,
                                        null,
                                        new GymWorkoutDefinition.EffortRange(
                                                GymWorkoutDefinition.EffortScale.RIR, 1.5, 3.5),
                                        null,
                                        80.0),
                                null));
        var workout =
                new StrengthProgramme.Workout(
                        "session",
                        DAY,
                        StrengthWorkoutBudget.workoutMinutes(slots),
                        new GymWorkoutDefinition(
                                "workout", 1, NOW.minusSeconds(86400), "Authored session", slots),
                        null);
        return new StrengthProgramme(
                "authored",
                revision,
                revision - 1,
                NOW.minusSeconds(86400),
                "Authored programme",
                StrengthProgramme.Origin.CUSTOM,
                null,
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.BALANCED,
                        GymProgramme.Experience.INTERMEDIATE,
                        Collections.singleton(DAY.getDayOfWeek()),
                        1,
                        45,
                        EnumSet.allOf(ExerciseMetadata.Equipment.class)),
                Collections.singletonList(
                        new StrengthProgramme.Block(
                                "block",
                                StrengthProgramme.Phase.FOUNDATION,
                                "Foundation",
                                DAY,
                                1,
                                Collections.singletonList(workout),
                                "Athlete chose this block")),
                DAY,
                "Explicit programme revision",
                Collections.emptyList());
    }

    private static GymWorkoutAssociation.Decision choice(
            String id, GymWorkoutAssociation.Proposal p, GymWorkoutAssociation.Choice choice) {
        return new GymWorkoutAssociation.Decision(id, p, choice, NOW);
    }

    private BackupManager manager() {
        return new BackupManager(
                getContext(),
                new SqliteBackupRepository(getContext()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private JSONObject envelope() throws Exception {
        var out = new ByteArrayOutputStream();
        manager().exportTo(out);
        return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
    }

    private static InputStream input(JSONObject value) {
        return new ByteArrayInputStream(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void roundTrip() {
        var backups = new SqliteBackupRepository(getContext());
        backups.restoreUserData(backups.exportUserData(), false);
    }

    private int countDecisions() {
        try (var c =
                ProfileDatabase.get(getContext())
                        .getReadableDatabase()
                        .rawQuery("SELECT COUNT(*) FROM strength_association_decisions", null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private static void reject(Runnable action) {
        try {
            action.run();
            fail("Stale association accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
