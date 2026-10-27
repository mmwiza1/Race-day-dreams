package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymWorkoutAdapter;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.GymWorkoutSourceValidator;
import org.racedaydreams.domain.strength.StrengthExercise;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

/** Exercise retained-source validation on all supported runtimes, including API28. */
@SuppressWarnings("deprecation")
public final class GymWorkoutSourceAndroidTest extends AndroidTestCase {
    public void testFrozenProgrammeSourceChecksTargetsWithoutRenamingHistory() {
        var now = Instant.parse("2026-10-27T12:00:00Z");
        var day = LocalDate.of(2026, 10, 27);
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Immutable.set(DayOfWeek.TUESDAY),
                        1,
                        Immutable.set("full gym"),
                        45,
                        GymProgramme.Effort.RIR);
        var session =
                new GymProgramme.Session(
                        "preserved-session",
                        day,
                        "Upper",
                        Immutable.list(
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
                                        "Preserved",
                                        80.0)),
                        "Original");
        var programme =
                new GymProgramme(
                        "preserved-programme",
                        1,
                        now,
                        "Athlete chose",
                        settings,
                        day,
                        day.plusDays(41),
                        Immutable.list(session),
                        Immutable.list());
        var prepared = GymWorkoutAdapter.fromProgramme(programme, session.id());
        var slot = prepared.definition().slots().get(0);
        var frozen =
                new StrengthExercise(
                        slot.exercise().id(),
                        "Historical bench name",
                        slot.exercise().region(),
                        slot.exercise().mode(),
                        slot.exercise().estimateOneRepMax(),
                        slot.exercise().measurement());
        var original =
                new GymWorkoutDefinition(
                        prepared.definition().id(),
                        1,
                        now,
                        session.title(),
                        Immutable.list(
                                new GymWorkoutDefinition.Slot(
                                        slot.id(), frozen, slot.target(), null)));
        assertEquals(
                session,
                GymWorkoutSourceValidator.programme(original, prepared.source(), programme));
        assertEquals("Historical bench name", original.slots().get(0).exercise().name());
        assertEquals(GymWorkoutDefinition.EffortScale.RIR, slot.target().effort().scale());
        var tampered =
                new GymWorkoutDefinition(
                        original.id(),
                        1,
                        now,
                        original.title(),
                        Immutable.list(
                                new GymWorkoutDefinition.Slot(
                                        slot.id(),
                                        frozen,
                                        new GymWorkoutDefinition.Target(
                                                3,
                                                5,
                                                8,
                                                null,
                                                null,
                                                slot.target().effort(),
                                                120,
                                                80.0),
                                        null)));
        try {
            GymWorkoutSourceValidator.programme(tampered, prepared.source(), programme);
            fail("Matching source identity must not authenticate changed targets.");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
