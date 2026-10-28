package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

/** Actual Android JSON parser: strict authoring formats and preserved frozen definitions. */
public final class StrengthProgrammingJsonTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final LocalDate START = LocalDate.of(2026, 10, 27);

    public void testGeneratedBlocksTargetsAndRevisionRoundTrip() {
        for (var goal : GymProgramme.Goal.values())
            for (var experience : GymProgramme.Experience.values()) {
                var plan = plan(goal, experience);
                assertEquals(
                        plan, StrengthProgrammingJson.read(StrengthProgrammingJson.write(plan)));
                assertEquals(4, plan.blocks().size());
                assertEquals(1, plan.revision());
                assertEquals(
                        plan.workouts().get(0).definition().slots(),
                        StrengthProgrammingJson.read(StrengthProgrammingJson.write(plan))
                                .workouts()
                                .get(0)
                                .definition()
                                .slots());
            }
    }

    public void testFrozenNamesAreDecodedWithoutCurrentCatalogueLookup() throws Exception {
        var plan = plan(GymProgramme.Goal.STRENGTH, GymProgramme.Experience.INTERMEDIATE);
        var json = StrengthProgrammingJson.encode(plan);
        json.getJSONArray("blocks")
                .getJSONObject(0)
                .getJSONArray("workouts")
                .getJSONObject(0)
                .getJSONObject("definition")
                .getJSONArray("slots")
                .getJSONObject(0)
                .getJSONObject("exercise")
                .put("name", "Historical retained name");
        var retained = StrengthProgrammingJson.read(json.toString());
        assertEquals(
                "Historical retained name",
                retained.workouts().get(0).definition().slots().get(0).exercise().name());
        assertEquals(
                retained, StrengthProgrammingJson.read(StrengthProgrammingJson.write(retained)));
    }

    public void testPartialDraftRoundTripsButCannotBeActivatedAsACompletePreview() {
        var draft =
                new StrengthProgrammeDraft(
                        "draft-a",
                        1,
                        "my-programme",
                        0,
                        "",
                        settings(GymProgramme.Goal.GENERAL, GymProgramme.Experience.BEGINNER),
                        START,
                        Collections.emptyList(),
                        StrengthProgramme.Origin.CUSTOM,
                        null);
        assertEquals(
                draft,
                StrengthProgrammingJson.readDraft(StrengthProgrammingJson.writeDraft(draft)));
        var blank = draft;
        reject(() -> blank.preview(NOW, Collections.emptySet(), Collections.emptyMap()));
        var block =
                new StrengthProgrammeDraft.WeekBlock(
                        "block-a",
                        StrengthProgramme.Phase.FOUNDATION,
                        "Foundation",
                        3,
                        Collections.singletonList(
                                new StrengthProgrammeDraft.Day(
                                        "day-a",
                                        DayOfWeek.TUESDAY,
                                        "Session A",
                                        Collections.emptyList(),
                                        null)));
        draft = draft.replaceBlock(block);
        var saved = draft;
        assertEquals(
                saved,
                StrengthProgrammingJson.readDraft(StrengthProgrammingJson.writeDraft(saved)));
        reject(() -> saved.preview(NOW, Collections.emptySet(), Collections.emptyMap()));
    }

    public void testFutureMissingUnknownCoercedAndDuplicateFieldsFailClosed() throws Exception {
        var payload =
                StrengthProgrammingJson.write(
                        plan(GymProgramme.Goal.GENERAL, GymProgramme.Experience.BEGINNER));
        var json = new JSONObject(payload);
        json.put("version", 2);
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.put("revision", "1");
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.put("unknown", true);
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.remove("effectiveFrom");
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("settings").put("days", new JSONArray().put("MONDAY").put("MONDAY"));
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.getJSONObject("settings").put("equipment", new JSONArray().put(true));
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.getJSONArray("blocks").getJSONObject(0).put("weeks", 0);
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.getJSONArray("blocks").getJSONObject(0).put("start", "bad-date");
        rejectProgramme(json.toString());
        json = new JSONObject(payload);
        json.getJSONArray("blocks").getJSONObject(0).put("futureHeuristic", true);
        rejectProgramme(json.toString());
        rejectProgramme(payload + " trailing");
        rejectProgramme(payload.replaceFirst("\\{", "{\"version\":1,"));
        rejectProgramme("{/* comment */\"version\":1}");
    }

    public void testCustomMetadataRoundTripAndNoInventedTechnique() throws Exception {
        var custom = custom();
        var payload = StrengthProgrammingJson.writeCustom(custom);
        assertEquals(custom, StrengthProgrammingJson.readCustom(payload));
        var json = new JSONObject(payload);
        json.put("technique", "pretend canonical guidance");
        reject(() -> StrengthProgrammingJson.readCustom(json.toString()));
        var future = new JSONObject(payload);
        future.put("version", 2);
        reject(() -> StrengthProgrammingJson.readCustom(future.toString()));
        var duplicated = new JSONObject(payload);
        duplicated.put(
                "equipmentAlternatives",
                new JSONArray().put(new JSONArray().put("DUMBBELLS").put("DUMBBELLS")));
        reject(() -> StrengthProgrammingJson.readCustom(duplicated.toString()));
    }

    public void testPayloadKindsCannotBeConfused() {
        String programme =
                StrengthProgrammingJson.write(
                        plan(GymProgramme.Goal.GENERAL, GymProgramme.Experience.BEGINNER));
        String custom = StrengthProgrammingJson.writeCustom(custom());
        reject(() -> StrengthProgrammingJson.readDraft(programme));
        reject(() -> StrengthProgrammingJson.readCustom(programme));
        reject(() -> StrengthProgrammingJson.read(custom));
    }

    private static StrengthProgramme plan(
            GymProgramme.Goal goal, GymProgramme.Experience experience) {
        return new StrengthProgrammeGenerator()
                .generate(
                        new StrengthProgrammeGenerator.Request(
                                "retained-plan",
                                NOW,
                                "A programme",
                                settings(goal, experience),
                                START,
                                10,
                                null,
                                PrimaryFocus.STRENGTH,
                                Collections.emptySet(),
                                null,
                                Collections.emptySet(),
                                Collections.emptySet(),
                                true));
    }

    private static StrengthProgramme.Settings settings(
            GymProgramme.Goal goal, GymProgramme.Experience experience) {
        return new StrengthProgramme.Settings(
                goal,
                GymProgramme.Priority.BALANCED,
                experience,
                EnumSet.allOf(DayOfWeek.class),
                2,
                60,
                EnumSet.allOf(ExerciseMetadata.Equipment.class));
    }

    private static CustomStrengthExercise custom() {
        return new CustomStrengthExercise(
                new StrengthExercise(
                        "custom-movement",
                        "My movement",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS),
                ExerciseMetadata.BodyArea.CHEST,
                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                new ExerciseMetadata.EquipmentRequirement(
                        Collections.singletonList(
                                EnumSet.of(ExerciseMetadata.Equipment.DUMBBELLS))),
                "Own setup; no canonical guide.");
    }

    private static void rejectProgramme(String payload) {
        reject(() -> StrengthProgrammingJson.read(payload));
    }

    private static void reject(Runnable action) {
        try {
            action.run();
            fail("Malformed authoring payload accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
