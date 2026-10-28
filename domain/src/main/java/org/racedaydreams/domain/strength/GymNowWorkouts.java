package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Date-free, one-off intentions for Quick Workout and Generate One for Me. Prescription selection
 * delegates to the existing gym planner; its temporary reference block is never returned or saved.
 * This factory reads no current programme, writes no history and creates no actual sets.
 */
public final class GymNowWorkouts {
    private static final LocalDate REFERENCE_START = LocalDate.of(2020, 1, 6);

    private GymNowWorkouts() {}

    public enum Focus {
        FULL_BODY("Full body workout"),
        UPPER("Upper body workout"),
        LOWER("Lower body workout");

        private final String title;

        Focus(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    /** Only the session lengths and equipment capabilities supported by the current planner. */
    public record Request(
            int minutes,
            Set<String> equipment,
            Focus focus,
            GymProgramme.Goal goal,
            GymProgramme.Experience experience,
            GymProgramme.Priority priority,
            GymProgramme.Effort effort) {
        public Request {
            if (equipment == null || equipment.isEmpty())
                throw new IllegalArgumentException(
                        "Choose the equipment available, including bodyweight if appropriate.");
            if (focus == null
                    || goal == null
                    || experience == null
                    || priority == null
                    || effort == null)
                throw new IllegalArgumentException(
                        "Choose a focus, goal, experience, priority and effort scale.");
            equipment = Immutable.copySet(equipment);
            // Share the planner's input contract rather than invent a second equipment/time policy.
            new GymProgramme.Settings(
                    goal,
                    priority,
                    experience,
                    Immutable.set(DayOfWeek.MONDAY),
                    1,
                    equipment,
                    minutes,
                    effort);
        }
    }

    public record Suggestion(
            GymWorkoutDefinition definition, String explanation, List<String> limitations) {
        public Suggestion {
            Objects.requireNonNull(definition, "one-off definition");
            if (explanation == null || explanation.trim().isEmpty())
                throw new IllegalArgumentException("Explain the proposed one-off workout.");
            limitations = Immutable.copyList(limitations);
        }
    }

    public static Suggestion generate(String workoutId, Instant createdAt, Request request) {
        return generate(workoutId, createdAt, request, Collections.emptySet());
    }

    /** Suitable favourites may affect the existing planner's tie-break; they never force a role. */
    public static Suggestion generate(
            String workoutId, Instant createdAt, Request request, Set<String> favourites) {
        GymWorkoutDefinition.requireId(workoutId, "workout");
        Objects.requireNonNull(createdAt, "created at");
        Objects.requireNonNull(request, "one-off request");
        Objects.requireNonNull(favourites, "exercise favourites");
        int frequency = request.focus() == Focus.FULL_BODY ? 1 : 4;
        Set<DayOfWeek> days =
                frequency == 1
                        ? Immutable.set(DayOfWeek.MONDAY)
                        : Immutable.set(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY);
        var settings =
                new GymProgramme.Settings(
                        request.goal(),
                        request.priority(),
                        request.experience(),
                        days,
                        frequency,
                        request.equipment(),
                        request.minutes(),
                        request.effort());
        // Reuse the planner's existing split/rep/effort/equipment/time policies. The fixed
        // reference
        // calendar is private implementation detail, unrelated to the athlete's current schedule.
        var reference =
                new GymProgrammeGenerator()
                        .generate(
                                "one-off-reference",
                                1,
                                createdAt,
                                "Temporary prescription selection, not an athlete programme",
                                settings,
                                REFERENCE_START,
                                Collections.emptyList(),
                                Collections.emptyList(),
                                Immutable.copySet(favourites));
        String prefix =
                request.focus() == Focus.FULL_BODY
                        ? "Full body"
                        : request.focus() == Focus.UPPER ? "Upper" : "Lower";
        GymProgramme.Session selected = null;
        for (var session : reference.sessions()) {
            if (session.title().startsWith(prefix + " ")) {
                selected = session;
                break;
            }
        }
        if (selected == null || selected.exercises().isEmpty())
            throw new IllegalArgumentException(
                    "No matching workout fits these choices. Review focus, equipment and time.");
        boolean lower = GymProgrammeGenerator.hasLowerBodyWork(selected.exercises());
        boolean upper = false;
        boolean pull = false;
        for (var prescription : selected.exercises()) {
            var metadata = ExerciseMetadataCatalog.require(prescription.exerciseId());
            upper |=
                    metadata.roles().contains(ExerciseMetadata.Role.HORIZONTAL_PUSH)
                            || metadata.roles().contains(ExerciseMetadata.Role.VERTICAL_PUSH)
                            || metadata.roles().contains(ExerciseMetadata.Role.HORIZONTAL_PULL)
                            || metadata.roles().contains(ExerciseMetadata.Role.VERTICAL_PULL);
            pull |=
                    metadata.roles().contains(ExerciseMetadata.Role.HORIZONTAL_PULL)
                            || metadata.roles().contains(ExerciseMetadata.Role.VERTICAL_PULL);
        }
        if (request.focus() == Focus.FULL_BODY && (!lower || !upper))
            throw new IllegalArgumentException(
                    "These choices do not leave room for both upper- and lower-body work. Choose"
                            + " more time or a different focus.");
        var prepared = GymWorkoutAdapter.fromProgramme(reference, selected.id());
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (var slot : prepared.definition().slots()) {
            slots.add(
                    new GymWorkoutDefinition.Slot(
                            slot.id(),
                            slot.exercise(),
                            slot.target() == null ? null : slot.target().withoutLoadReference(),
                            slot.supersetGroup()));
        }
        var definition =
                new GymWorkoutDefinition(workoutId, 1, createdAt, request.focus().title(), slots);
        var limitations = new ArrayList<String>();
        if (request.focus() != Focus.LOWER && !pull)
            limitations.add(
                    "No upper-body pulling exercise fits this selection. Core work is not a"
                        + " replacement for rows or pull-ups; review equipment or allow more time"
                        + " if pulling is wanted today.");
        if (request.goal() == GymProgramme.Goal.POWER)
            limitations.add(
                    "Power preparation uses the planner's controlled strength movements and"
                            + " movement-speed intent. It does not add Olympic lifts or maximal"
                            + " testing.");
        for (var slot : slots) {
            var metadata = ExerciseMetadataCatalog.require(slot.exercise().id());
            if (!metadata.suitability().appropriateFor(request.experience()))
                limitations.add(
                        slot.exercise().name()
                                + " uses a more complex variation than your selected experience."
                                + " Review its technique guide and choose another variation if"
                                + " preferred.");
        }
        String explanation =
                "One-off "
                        + request.focus().title().toLowerCase(Locale.UK)
                        + " for "
                        + goalLabel(request.goal())
                        + ", using "
                        + request.experience().name().toLowerCase(Locale.UK)
                        + " experience and "
                        + request.priority().name().toLowerCase(Locale.UK).replace('_', ' ')
                        + " priority. Targets follow the gym planner's current rules. Choose"
                        + " manageable loads; no starting weight has been inferred. This does not"
                        + " change a programme.";
        if (slots.stream().anyMatch(slot -> favourites.contains(slot.exercise().id())))
            explanation +=
                    " Suitable favourite exercises were preferred within the required movement"
                            + " roles.";
        return new Suggestion(definition, explanation, limitations);
    }

    private static String goalLabel(GymProgramme.Goal goal) {
        return switch (goal) {
            case SUPPORT_RUNNING -> "running support";
            case STRENGTH -> "getting stronger";
            case MUSCLE -> "building muscle";
            case GENERAL -> "general strength and fitness";
            case POWER -> "power preparation";
        };
    }
}
