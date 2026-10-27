package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import org.json.JSONObject;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.settings.PrimaryFocusRepository;
import org.racedaydreams.domain.settings.SettingsRepository;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymProgrammeGenerator;
import org.racedaydreams.portability.BackupManager;

/** Focus is a typed preference in the existing transactional database and backup format. */
@SuppressWarnings("deprecation")
public final class PrimaryFocusRepositoryTest extends AndroidTestCase {
    private FeatureStore store;
    private SettingsRepository settings;
    private PrimaryFocusRepository focus;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        store = new FeatureStore(getContext());
        settings = new SqliteSettingsRepository(getContext());
        focus = new SqlitePrimaryFocusRepository(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        store.close();
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testLegacyPathsResolveWithoutRewritingTheOriginalChoice() {
        assertFalse(focus.load().isPresent());
        for (String path : new String[] {"record", "race", "hybrid"}) {
            settings.putSetting("starting.path", path);
            assertEquals(
                    "hybrid".equals(path) ? PrimaryFocus.HYBRID : PrimaryFocus.RUNNING,
                    focus.load().get());
            assertEquals(path, settings.getSetting("starting.path"));
            assertNull(settings.getSetting("primary.focus"));
        }
    }

    public void testExplicitFocusSurvivesANewAdapterAndRetainsLegacyPathAndDraft() {
        settings.putSetting("starting.path", "race");
        SqliteUserPreferencesRepository profiles = new SqliteUserPreferencesRepository(getContext());
        String draft = "{\"targetTime\":\"1:\",\"unfinished\":true}";
        profiles.saveDraft(draft);
        for (PrimaryFocus choice : PrimaryFocus.values()) {
            focus.save(choice);
            assertEquals(choice.storageValue(), settings.getSetting("primary.focus"));
            assertEquals(choice, new SqlitePrimaryFocusRepository(getContext()).load().get());
            assertEquals("race", settings.getSetting("starting.path"));
            assertEquals(draft, profiles.loadDraft().get());
            assertFalse(profiles.load().isPresent());
        }
    }

    public void testRaceFreeStrengthAndGymProgrammeRoundTripInTheCurrentBackup() throws Exception {
        settings.putSetting("starting.path", "record");
        focus.save(PrimaryFocus.STRENGTH);
        GymProgramme programme = programme();
        GymProgrammeStore gym = new GymProgrammeStore(getContext());
        gym.save(programme);
        String draft = "{\"targetTime\":\"1:\",\"unfinished\":true}";
        SqliteUserPreferencesRepository profiles = new SqliteUserPreferencesRepository(getContext());
        profiles.saveDraft(draft);
        BackupManager manager = new BackupManager(getContext(), store);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        manager.exportTo(output);
        byte[] snapshot = output.toByteArray();
        JSONObject envelope = new JSONObject(new String(snapshot, StandardCharsets.UTF_8));
        assertEquals(7, envelope.getInt("version"));
        assertEquals("strength", envelope.getJSONObject("data").getJSONObject("settings").getString("primary.focus"));

        focus.save(PrimaryFocus.RUNNING);
        gym.disable();
        profiles.clearDraft();
        manager.restoreFrom(new ByteArrayInputStream(snapshot));
        assertEquals(PrimaryFocus.STRENGTH, focus.load().get());
        assertEquals(programme, gym.current());
        assertEquals(Collections.singletonList(programme), gym.versions());
        assertEquals(draft, profiles.loadDraft().get());
        assertFalse(profiles.load().isPresent());
        assertEquals("record", settings.getSetting("starting.path"));
    }

    public void testBackupWithoutExplicitFocusRestoresLegacyChoiceInsteadOfKeepingNewerFocus()
            throws Exception {
        settings.putSetting("starting.path", "hybrid");
        BackupRepository backups = new SqliteBackupRepository(getContext());
        JSONObject legacy = backups.exportUserData();
        assertFalse(legacy.getJSONObject("settings").has("primary.focus"));
        focus.save(PrimaryFocus.STRENGTH);
        backups.restoreUserData(legacy, false);
        assertNull(settings.getSetting("primary.focus"));
        assertEquals("hybrid", settings.getSetting("starting.path"));
        assertEquals(PrimaryFocus.HYBRID, focus.load().get());
    }

    public void testFocusWritesAndRestoreParticipateInTheExistingOuterTransaction() {
        focus.save(PrimaryFocus.RUNNING);
        SQLiteDatabase db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.beginTransaction();
        try {
            focus.save(PrimaryFocus.STRENGTH);
            assertEquals(PrimaryFocus.STRENGTH, focus.load().get());
        } finally {
            db.endTransaction();
        }
        assertEquals(PrimaryFocus.RUNNING, focus.load().get());

        BackupRepository backups = new SqliteBackupRepository(getContext());
        focus.save(PrimaryFocus.STRENGTH);
        JSONObject snapshot = backups.exportUserData();
        focus.save(PrimaryFocus.HYBRID);
        settings.putSetting("retained", "before failed restore");
        db.execSQL(
                "CREATE TRIGGER primary_focus_restore_failure BEFORE INSERT ON settings "
                        + "WHEN NEW.setting_key='primary.focus' "
                        + "BEGIN SELECT RAISE(ABORT, 'injected primary focus restore failure'); END");
        try {
            backups.restoreUserData(snapshot, false);
            fail("A focus restore failure must abort the complete replacement.");
        } catch (SQLiteException expected) {
            assertEquals(PrimaryFocus.HYBRID, focus.load().get());
            assertEquals("before failed restore", settings.getSetting("retained"));
        }
    }

    private static GymProgramme programme() {
        GymProgramme.Settings preferences =
                new GymProgramme.Settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.BEGINNER,
                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                        2,
                        Collections.singleton("bodyweight"),
                        45,
                        GymProgramme.Effort.RPE);
        return new GymProgrammeGenerator()
                .generate(
                        "race-free-focus-fixture",
                        1,
                        Instant.parse("2030-04-01T12:00:00Z"),
                        "Explicit test-only Strength programme",
                        preferences,
                        LocalDate.of(2030, 4, 1),
                        Collections.emptyList(),
                        Collections.emptyList());
    }
}
