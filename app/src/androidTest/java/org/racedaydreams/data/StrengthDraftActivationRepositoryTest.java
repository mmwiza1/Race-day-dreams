package org.racedaydreams.data;

import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

/**
 * Reviewed draft and current-pointer validation belong inside the same SQLite write transaction.
 */
@SuppressWarnings("deprecation")
public final class StrengthDraftActivationRepositoryTest extends AndroidTestCase {
    private SqliteStrengthProgrammingRepository repository;
    private StrengthProgrammeDraft draft;
    private Instant now;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        now = Instant.now().minusSeconds(1);
        repository = new SqliteStrengthProgrammingRepository(getContext());
        draft = draft(1, "Reviewed programme");
        repository.saveDraft(draft, 0);
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testExactReviewedDraftActivationAndLostAcknowledgementRetry() {
        var candidate = preview(draft);
        var accepted = repository.activateDraft(candidate, null, "accept", draft);
        assertEquals(candidate, repository.programme(candidate.id(), 1));
        assertEquals(candidate.id(), repository.currentReference().id());
        assertEquals(draft, repository.draft(draft.id()));
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
        assertEquals(
                1.5,
                candidate
                        .workouts()
                        .get(0)
                        .definition()
                        .slots()
                        .get(0)
                        .target()
                        .effort()
                        .minimum());
        assertNull(candidate.workouts().get(0).definition().slots().get(0).target().restSeconds());
        repository.deleteDraft(draft.id(), 1);
        assertEquals(accepted, repository.activateDraft(candidate, null, "accept", draft));
        assertEquals(1, repository.decisions().size());
    }

    public void testConcurrentDraftEditRejectsReviewedOldSnapshotWithoutReplacingAnything() {
        var candidate = preview(draft);
        var changed = draft(2, "Changed elsewhere");
        repository.saveDraft(changed, 1);
        reject(() -> repository.activateDraft(candidate, null, "stale", draft));
        assertEquals(changed, repository.draft(draft.id()));
        assertNull(repository.currentReference());
        assertNull(repository.programme(candidate.id(), 1));
        assertTrue(repository.decisions().isEmpty());
    }

    public void testCandidateCannotBeSubstitutedAfterTheDraftWasReviewed() {
        var p = preview(draft);
        var changed =
                new StrengthProgramme(
                        p.id(),
                        p.revision(),
                        p.parentRevision(),
                        p.createdAt(),
                        "Unreviewed replacement",
                        p.origin(),
                        p.templateId(),
                        p.settings(),
                        p.blocks(),
                        p.effectiveFrom(),
                        p.reason(),
                        p.tradeoffs());
        reject(() -> repository.activateDraft(changed, null, "forged", draft));
        assertNull(repository.currentReference());
        assertNull(repository.programme(p.id(), 1));
        assertEquals(draft, repository.draft(draft.id()));
    }

    public void testExclusionAddedAfterPreviewRejectsBeforeAnyProgrammeWrite() {
        var candidate = preview(draft);
        new StrengthUserStore(getContext())
                .exclude(
                        new ExerciseExclusionRepository.Exclusion(
                                "bench-press",
                                ExerciseExclusionRepository.Reason.PREFERENCE,
                                Instant.now()));
        reject(() -> repository.activateDraft(candidate, null, "excluded", draft));
        assertNull(repository.currentReference());
        assertTrue(repository.decisions().isEmpty());
        assertEquals(draft, repository.draft(draft.id()));
    }

    public void testChangedCurrentPointerRejectsWithoutSilentlyReplacingNewProgramme() {
        var oldPreview = preview(draft);
        var otherDraft =
                new StrengthProgrammeDraft(
                        "other-draft",
                        1,
                        "other-programme",
                        0,
                        "Other programme",
                        draft.settings(),
                        draft.start(),
                        draft.blocks(),
                        StrengthProgramme.Origin.CUSTOM,
                        null);
        repository.saveDraft(otherDraft, 0);
        var other = preview(otherDraft);
        repository.activateDraft(other, null, "other", otherDraft);
        reject(() -> repository.activateDraft(oldPreview, null, "stale-current", draft));
        assertEquals(other, repository.programme(other.id(), 1));
        assertEquals(other.id(), repository.currentReference().id());
        assertNull(repository.programme(oldPreview.id(), 1));
    }

    public void testLateDecisionFailureRollsBackProgrammePointerAndRetainsDraft() {
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER fail_draft_activation BEFORE INSERT ON strength_programme_decisions"
                        + " BEGIN SELECT RAISE(ABORT,'injected'); END");
        var candidate = preview(draft);
        try {
            repository.activateDraft(candidate, null, "fail", draft);
            fail("Late SQL failure must reject");
        } catch (SQLiteException expected) {
        } finally {
            db.execSQL("DROP TRIGGER fail_draft_activation");
        }
        assertNull(repository.currentReference());
        assertNull(repository.programme(candidate.id(), 1));
        assertEquals(draft, repository.draft(draft.id()));
        assertTrue(repository.decisions().isEmpty());
    }

    private StrengthProgrammeDraft draft(int revision, String name) {
        var today = LocalDate.now();
        var target =
                new GymWorkoutDefinition.Target(
                        3,
                        5,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RIR, 1.5, 3.5),
                        null,
                        80.0);
        var slot =
                new GymWorkoutDefinition.Slot(
                        "bench",
                        ExerciseCatalog.definition("bench-press").exercise(),
                        target,
                        null);
        var day =
                new StrengthProgrammeDraft.Day(
                        "day",
                        today.getDayOfWeek(),
                        "Bench practice",
                        Collections.singletonList(slot),
                        null);
        return new StrengthProgrammeDraft(
                "draft",
                revision,
                "programme",
                0,
                name,
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Collections.singleton(today.getDayOfWeek()),
                        1,
                        45,
                        EnumSet.of(
                                ExerciseMetadata.Equipment.BARBELL,
                                ExerciseMetadata.Equipment.BENCH,
                                ExerciseMetadata.Equipment.SQUAT_RACK)),
                today,
                Collections.singletonList(
                        new StrengthProgrammeDraft.WeekBlock(
                                "block",
                                StrengthProgramme.Phase.FOUNDATION,
                                "Foundation",
                                2,
                                Collections.singletonList(day))),
                StrengthProgramme.Origin.CUSTOM,
                null);
    }

    private StrengthProgramme preview(StrengthProgrammeDraft value) {
        return value.preview(now, Collections.emptySet(), Collections.emptyMap());
    }

    private void reject(Runnable action) {
        try {
            action.run();
            fail("Invalid activation was accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
