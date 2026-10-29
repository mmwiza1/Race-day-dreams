package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.JSONObject;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.Collections;

/**
 * Real Android JSON implementation, frozen sources and hostile input; no persistence side effects.
 */
public final class GymAssociationJsonTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    public void testBothChoicesRoundTripFrozenActualAndPrescription() {
        for (var choice : GymWorkoutAssociation.Choice.values()) {
            var original = decision(choice);
            var decoded = GymAssociationJson.read(GymAssociationJson.write(original));
            assertEquals(original, decoded);
            assertEquals(
                    60.0,
                    decoded.proposal().actual().exercises().get(0).sets().get(0).kilograms(),
                    0.0);
            assertEquals(
                    80.0,
                    decoded.proposal().prescription().exercises().get(0).referenceLoadKg(),
                    0.0);
            assertEquals(
                    "Historical bench",
                    decoded.proposal().actual().exercises().get(0).exercise().name());
            assertEquals(GymExecution.Origin.FREE, decoded.proposal().origin());
        }
    }

    public void testFutureUnknownMissingAndCoercedFieldsAreRejected() throws Exception {
        String payload =
                GymAssociationJson.write(
                        decision(GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        var json = new JSONObject(payload);
        json.put("version", 2);
        reject(json.toString());
        json = new JSONObject(payload);
        json.put("unknown", true);
        reject(json.toString());
        json = new JSONObject(payload);
        json.remove("choice");
        reject(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("proposal").put("programmeRevision", "1");
        reject(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("proposal").getJSONObject("prescription").put("date", "not-a-date");
        reject(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("proposal").put("origin", "PLANNED");
        reject(json.toString());
        reject(payload + " trailing");
        reject(payload.replaceFirst("\\{", "{\"version\":1,"));
    }

    public void testChangedActualTaskAndMalformedTargetsAreNotInvented() throws Exception {
        var json =
                new JSONObject(
                        GymAssociationJson.write(
                                decision(GymWorkoutAssociation.Choice.KEEP_EXTRA)));
        json.getJSONObject("proposal")
                .getJSONObject("prescription")
                .getJSONArray("exercises")
                .getJSONObject(0)
                .put("sets", 0);
        reject(json.toString());
        json =
                new JSONObject(
                        GymAssociationJson.write(
                                decision(GymWorkoutAssociation.Choice.KEEP_EXTRA)));
        json.getJSONObject("proposal")
                .getJSONObject("actual")
                .getJSONArray("exercises")
                .getJSONObject(0)
                .getJSONArray("sets")
                .getJSONObject(0)
                .put("kg", "60");
        reject(json.toString());
        json =
                new JSONObject(
                        GymAssociationJson.write(
                                decision(GymWorkoutAssociation.Choice.KEEP_EXTRA)));
        json.put("decidedAt", NOW.minusSeconds(1).toString());
        reject(json.toString());
    }

    public void testAuthoredBothChoicesRetainNullableFractionalRirAndActualEvidence()
            throws Exception {
        for (var choice : GymWorkoutAssociation.Choice.values()) {
            var d = authoredDecision(choice);
            var payload = GymAssociationJson.write(d);
            assertEquals(2, new JSONObject(payload).getInt("version"));
            var decoded = GymAssociationJson.read(payload);
            assertEquals(d, decoded);
            assertTrue(decoded.proposal().authored());
            assertNull(decoded.proposal().prescription());
            var slot = decoded.proposal().authoredPrescription().definition().slots().get(0);
            assertEquals("Historical bench", slot.exercise().name());
            assertEquals(GymWorkoutDefinition.EffortScale.RIR, slot.target().effort().scale());
            assertEquals(1.5, slot.target().effort().minimum(), 0.0);
            assertNull(slot.target().restSeconds());
            assertEquals(
                    60.0,
                    decoded.proposal().actual().exercises().get(0).sets().get(0).kilograms(),
                    0.0);
            assertEquals(80.0, slot.target().referenceLoadKg(), 0.0);
        }
    }

    public void testAuthoredFutureUnknownMissingAndCrossFamilyCannotBeDecoded() throws Exception {
        var payload =
                GymAssociationJson.write(
                        authoredDecision(GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        var json = new JSONObject(payload);
        json.put("version", 3);
        reject(json.toString());
        json = new JSONObject(payload);
        json.put("version", 1);
        reject(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("proposal").getJSONObject("prescription").remove("definition");
        reject(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("proposal").getJSONObject("prescription").put("unknown", true);
        reject(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("proposal")
                .getJSONObject("prescription")
                .getJSONObject("definition")
                .getJSONArray("slots")
                .getJSONObject(0)
                .getJSONObject("target")
                .put("sets", "3");
        reject(json.toString());
    }

    private static GymWorkoutAssociation.Decision authoredDecision(
            GymWorkoutAssociation.Choice choice) {
        var legacy = decision(choice);
        var slot =
                new GymWorkoutDefinition.Slot(
                        "slot",
                        legacy.proposal().actual().exercises().get(0).exercise(),
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
                        null);
        var workout =
                new StrengthProgramme.Workout(
                        "session",
                        LocalDate.of(2026, 10, 28),
                        20,
                        new GymWorkoutDefinition(
                                "workout",
                                1,
                                NOW.minusSeconds(86400),
                                "Authored workout",
                                Collections.singletonList(slot)),
                        null);
        return new GymWorkoutAssociation.Decision(
                "authored-choice",
                GymWorkoutAssociation.Proposal.authored(
                        legacy.proposal().actual(),
                        legacy.proposal().origin(),
                        "authored",
                        1,
                        workout,
                        NOW),
                choice,
                NOW);
    }

    private static GymWorkoutAssociation.Decision decision(GymWorkoutAssociation.Choice choice) {
        var exercise =
                new StrengthExercise(
                        "bench-press",
                        "Historical bench",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        true);
        var actual =
                new StrengthSession(
                        "actual",
                        NOW.minusSeconds(3600),
                        ZoneOffset.UTC,
                        Duration.ofMinutes(30),
                        null,
                        "Original notes",
                        Collections.singletonList(
                                new StrengthSession.ExerciseSets(
                                        exercise,
                                        Collections.singletonList(
                                                new StrengthSession.SetEntry(
                                                        "set", 6, 60, 7.0, false)))));
        var prescription =
                new GymProgramme.Session(
                        "planned",
                        LocalDate.of(2026, 10, 28),
                        "Strength",
                        Collections.singletonList(
                                new GymProgramme.Prescription(
                                        "bench-press",
                                        3,
                                        5,
                                        8,
                                        null,
                                        null,
                                        6,
                                        8,
                                        120,
                                        "Original intent",
                                        80.0)),
                        "Original explanation");
        return new GymWorkoutAssociation.Decision(
                "decision",
                new GymWorkoutAssociation.Proposal(
                        actual, GymExecution.Origin.FREE, "programme", 1, prescription, NOW),
                choice,
                NOW);
    }

    private static void reject(String payload) {
        try {
            GymAssociationJson.read(payload);
            fail("Malformed decision accepted");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
