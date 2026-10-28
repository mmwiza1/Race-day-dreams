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

/** Real shared transactions, durable choices and restored graph; no mocked acknowledgements. */
@SuppressWarnings("deprecation")
public final class GymAssociationRepositoryTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2025-10-28T12:00:00Z");
    private SqliteGymAssociationRepository associations;
    private SqliteGymExecutionRepository gym;
    private GymProgrammeStore programmes;
    private CoachingStore coaching;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        associations = new SqliteGymAssociationRepository(getContext());
        gym = new SqliteGymExecutionRepository(getContext());
        programmes = new GymProgrammeStore(getContext());
        coaching = new CoachingStore(getContext());
        programmes.save(plan(1));
        finish("actual");
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testExtraCountRetryAndExplicitReversalPreserveNativeEvidence() {
        var original = gym.load("actual");
        var p = associations.proposals("actual", NOW).get(0);
        var keep = decision("keep", p, GymWorkoutAssociation.Choice.KEEP_EXTRA);
        associations.decide(keep);
        assertEquals(keep, associations.current("actual"));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        var count = decision("count", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        associations.decide(count);
        assertEquals(count, associations.decide(count));
        assertEquals(2, count("strength_association_decisions"));
        assertEquals(
                "programme:v1",
                new DevelopmentStore(getContext()).gymLinks().get("actual").versionKey());
        assertEquals(original, gym.load("actual"));
        assertEquals(GymExecution.Origin.FREE, gym.load("actual").execution().origin());
        programmes.save(plan(2));
        var reverse = decision("reverse", p, GymWorkoutAssociation.Choice.KEEP_EXTRA);
        associations.decide(reverse);
        assertEquals(reverse, associations.current("actual"));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        reject(() -> associations.decide(count));
        assertEquals(3, count("strength_association_decisions"));
        assertEquals(original, gym.load("actual"));
        assertEquals(2, programmes.current().version());
    }

    public void testCompetingActualAndChangedProgrammeCannotClaimReviewedTarget() {
        finish("other");
        var a = associations.proposals("actual", NOW).get(0);
        var b = associations.proposals("other", NOW).get(0);
        associations.decide(decision("a", a, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        reject(
                () ->
                        associations.decide(
                                decision(
                                        "b",
                                        b,
                                        GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)));
        assertEquals(1, count("strength_association_decisions"));
        assertEquals(2, coaching.strength().size());
        assertNull(associations.current("other"));
        associations.decide(decision("extra", a, GymWorkoutAssociation.Choice.KEEP_EXTRA));
        programmes.save(plan(2));
        reject(
                () ->
                        associations.decide(
                                decision(
                                        "stale",
                                        b,
                                        GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
    }

    public void testPointerFailureRollsBackDecisionLinkAndRetryTogether() {
        var p = associations.proposals("actual", NOW).get(0);
        var d = decision("retry", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER association_failure BEFORE INSERT ON strength_association_targets"
                        + " BEGIN SELECT RAISE(ABORT,'injected'); END");
        try {
            associations.decide(d);
            fail("Injected storage failure was ignored");
        } catch (SQLiteException expected) {
            assertNotNull(expected.getMessage());
        }
        assertEquals(0, count("strength_association_decisions"));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        assertEquals(1, coaching.strength().size());
        assertEquals(GymExecution.Origin.FREE, gym.load("actual").execution().origin());
        db.execSQL("DROP TRIGGER association_failure");
        assertEquals(d, associations.decide(d));
        assertEquals(1, count("strength_association_decisions"));
    }

    public void testActualEditAndDeleteDoNotRewriteFrozenChoiceOrPrescription() throws Exception {
        var p = associations.proposals("actual", NOW).get(0);
        var d = decision("chosen", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        associations.decide(d);
        var old = coaching.strength().get(0);
        coaching.saveStrength(
                new StrengthSession(
                        old.id(),
                        old.startedAt(),
                        old.offset(),
                        old.duration(),
                        null,
                        "Edited actual note",
                        Collections.singletonList(
                                new StrengthSession.ExerciseSets(
                                        ExerciseCatalog.definition("overhead-press").exercise(),
                                        old.exercises().get(0).sets()))));
        assertEquals(d, associations.current("actual"));
        assertEquals("", d.proposal().actual().notes());
        var backup = new SqliteBackupRepository(getContext());
        backup.validateSnapshot(backup.exportUserData());
        var fresh = associations.reviewCurrent("actual", Instant.now());
        assertEquals("Edited actual note", fresh.actual().notes());
        associations.decide(
                new GymWorkoutAssociation.Decision(
                        "unlink-edited",
                        fresh,
                        GymWorkoutAssociation.Choice.KEEP_EXTRA,
                        Instant.now()));
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        assertEquals(d.proposal(), p);
        backup.validateSnapshot(backup.exportUserData());
        coaching.deleteStrength("actual");
        assertNull(associations.current("actual"));
        assertEquals(0, count("strength_association_decisions"));
        assertEquals(0, count("strength_association_targets"));
        assertEquals(1, programmes.versions().size());
        backup.validateSnapshot(backup.exportUserData());
    }

    public void testLegacyMigrationProjectsLinksWithoutInventingDecisionsAndUsesTargetIndex() {
        var legacy = new DevelopmentStore(getContext());
        legacy.linkGym("actual", DAY, "programme:v1", "session");
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        db.execSQL("DROP TABLE strength_association_targets");
        db.execSQL("DROP TABLE strength_association_decisions");
        db.setVersion(11);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(12, db.getVersion());
        assertEquals(0, count("strength_association_decisions"));
        assertNull(associations.current("actual"));
        assertTrue(associations.proposals("actual", NOW).isEmpty());
        assertEquals("programme:v1", legacy.gymLinks().get("actual").versionKey());
        try (var c =
                db.rawQuery(
                        "EXPLAIN QUERY PLAN SELECT session_id FROM strength_association_targets"
                                + " WHERE programme_key=? AND actual_id<>?",
                        new String[] {"programme:v1", "other"})) {
            assertTrue(c.moveToFirst());
            assertTrue(c.getString(3).contains("strength_association_target"));
        }
        legacy.deleteStrengthLink("actual");
        assertEquals(1, associations.proposals("actual", NOW).size());
    }

    public void testBackupRoundTripAndCorruptDecisionRejectBeforeReplacement() throws Exception {
        var p = associations.proposals("actual", NOW).get(0);
        var d = decision("backup", p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        associations.decide(d);
        var settings = new SqliteSettingsRepository(getContext());
        settings.putSetting("sentinel", "original");
        byte[] bytes = export();
        var envelope = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        assertEquals(8, envelope.getInt("version"));
        assertEquals(
                2,
                envelope.getJSONObject("data").getJSONObject("strengthEngine").getInt("version"));
        associations.decide(decision("temporary", p, GymWorkoutAssociation.Choice.KEEP_EXTRA));
        manager().restoreFrom(new ByteArrayInputStream(bytes));
        assertEquals(d, associations.current("actual"));
        assertEquals(GymExecution.Origin.FREE, gym.load("actual").execution().origin());
        var corrupt = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        corrupt.getJSONObject("data")
                .getJSONObject("strengthEngine")
                .getJSONObject("associations")
                .getJSONArray("decisions")
                .getJSONObject(0)
                .getJSONObject("proposal")
                .put("programmeRevision", 99);
        try {
            manager()
                    .restoreFrom(
                            new ByteArrayInputStream(
                                    corrupt.toString().getBytes(StandardCharsets.UTF_8)));
            fail("Corrupt decision replaced local data");
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
        }
        assertEquals(d, associations.current("actual"));
        assertEquals("original", settings.getSetting("sentinel"));
    }

    public void testOldStrengthSectionOneStillRestoresAndFutureSectionsAreRejected()
            throws Exception {
        var bytes = export();
        var envelope = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        var section = envelope.getJSONObject("data").getJSONObject("strengthEngine");
        section.put("version", 1);
        section.remove("associations");
        manager()
                .restoreFrom(
                        new ByteArrayInputStream(
                                envelope.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, coaching.strength().size());
        assertNull(associations.current("actual"));
        section.put("version", 3);
        try {
            manager()
                    .restoreFrom(
                            new ByteArrayInputStream(
                                    envelope.toString().getBytes(StandardCharsets.UTF_8)));
            fail("Future section accepted");
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
        }
        assertEquals(1, coaching.strength().size());
    }

    private static final LocalDate DAY = LocalDate.of(2025, 10, 28);

    private GymProgramme plan(int version) {
        return new GymProgramme(
                "programme",
                version,
                NOW.minusSeconds(86400),
                "Reviewed programme",
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Collections.singleton(DAY.getDayOfWeek()),
                        1,
                        Collections.singleton("full gym"),
                        45,
                        GymProgramme.Effort.RPE),
                DAY,
                DAY.plusDays(41),
                Collections.singletonList(
                        new GymProgramme.Session(
                                "session",
                                DAY,
                                "Strength",
                                Collections.singletonList(
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
                                                "Strength",
                                                80.0)),
                                "Original prescription")),
                Collections.emptyList());
    }

    private void finish(String id) {
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
                "bench", new StrengthSession.SetEntry(id + "-set", 6, 60, 7.0, false), tick(2000));
        gym.finish(engine.finish(tick(3000), null, "", false), initial.revision());
    }

    private static GymExecution.Tick tick(long ms) {
        return new GymExecution.Tick("clock", ms, NOW.minusSeconds(3600).plusMillis(ms));
    }

    private static GymWorkoutAssociation.Decision decision(
            String id, GymWorkoutAssociation.Proposal p, GymWorkoutAssociation.Choice choice) {
        return new GymWorkoutAssociation.Decision(id, p, choice, NOW);
    }

    private long count(String table) {
        try (var c =
                ProfileDatabase.get(getContext())
                        .getReadableDatabase()
                        .rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            c.moveToFirst();
            return c.getLong(0);
        }
    }

    private BackupManager manager() {
        return new BackupManager(
                getContext(),
                new SqliteBackupRepository(getContext()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private byte[] export() throws IOException {
        var out = new ByteArrayOutputStream();
        manager().exportTo(out);
        return out.toByteArray();
    }

    private static void reject(Runnable action) {
        try {
            action.run();
            fail("Stale association accepted");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
