package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment;
import org.racedaydreams.domain.strength.ExerciseMetadata.Role;
import org.racedaydreams.domain.strength.ExerciseMetadata.Suitability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Original, deterministic substitution heuristics. A shared region is insufficient: candidates
 * retain the primary movement role, relevant primary body area and measurement task. These are
 * explained options, not equivalent loading, a form assessment or medical clearance.
 */
public final class ExerciseSubstitutionEngine {
    private static final String[][] ADDED_LOAD_PAIRS = {
        {"push-up", "weighted-push-up"},
        {"pull-up", "weighted-pull-up"},
        {"chin-up", "weighted-chin-up"},
        {"dip", "weighted-dip"},
        {"plank", "weighted-plank"},
        {"side-plank", "weighted-side-plank"},
        {"bodyweight-squat", "goblet-squat"},
        {"bodyweight-squat", "kettlebell-goblet-squat"},
        {"single-leg-calf-raise-bodyweight", "single-leg-calf-raise"}
    };

    private ExerciseSubstitutionEngine() {}

    public enum Reason {
        EQUIPMENT_BUSY,
        EQUIPMENT_UNAVAILABLE,
        UNCOMFORTABLE,
        EASIER,
        HARDER,
        ALTERNATIVES
    }

    /** Available equipment means specific capabilities, not a generic "machines" wildcard. */
    public record Context(
            StrengthExercise original,
            GymProgramme.Goal goal,
            GymProgramme.Experience experience,
            Set<Equipment> availableEquipment,
            Set<String> excludedIds,
            Set<String> favouriteIds,
            Set<String> historyIds,
            Reason reason) {
        public Context {
            Objects.requireNonNull(original, "original exercise");
            Objects.requireNonNull(goal, "programme goal");
            Objects.requireNonNull(experience, "experience");
            Objects.requireNonNull(reason, "substitution reason");
            if (availableEquipment == null)
                throw new IllegalArgumentException("Available equipment must be explicit.");
            for (var item : availableEquipment)
                if (item == null)
                    throw new IllegalArgumentException("Available equipment must be explicit.");
            var equipment = EnumSet.noneOf(Equipment.class);
            equipment.addAll(availableEquipment);
            availableEquipment = Collections.unmodifiableSet(equipment);
            excludedIds = identities(excludedIds);
            favouriteIds = identities(favouriteIds);
            historyIds = identities(historyIds);
        }
    }

    /**
     * A changed measurement requires a fresh target; every swap requires a fresh load reference.
     */
    public record Suggestion(
            StrengthExercise exercise, String explanation, boolean measurementChanged) {
        public Suggestion {
            Objects.requireNonNull(exercise, "suggested exercise");
            if (explanation == null || explanation.trim().isEmpty())
                throw new IllegalArgumentException("Explain the substitution.");
        }
    }

    public static String discomfortGuidance() {
        return "Avoid movements that reproduce pain. These alternatives change the setup or"
                + " movement pattern, but Race Day Dreams cannot determine which exercise is"
                + " medically appropriate for an injury.";
    }

    /**
     * Returns no invented alternative when metadata or a reviewed easier/harder relationship is
     * missing. The caller may then offer the separate Show alternatives action. A limit of 2–4 is
     * useful for the busy-equipment flow; there is no padding with unrelated movements.
     */
    public static List<Suggestion> rank(Context context, int limit) {
        Objects.requireNonNull(context, "substitution context");
        if (limit < 1 || limit > 50)
            throw new IllegalArgumentException("Choose between 1 and 50 alternatives.");
        var original = ExerciseMetadataCatalog.find(context.original().id());
        if (original == null) return Collections.emptyList();
        var canonical = ExerciseCatalog.definition(context.original().id()).exercise();
        // A familiar ID is not proof that an old/custom frozen definition has today's task or
        // mode. Display names may change; incompatible retained measurements remain unknown.
        if (context.original().measurement() != canonical.measurement()
                || context.original().mode() != canonical.mode()
                || context.original().region() != canonical.region())
            return Collections.emptyList();
        var candidates = new ArrayList<Candidate>();
        for (var exercise : ExerciseCatalog.exercises()) {
            if (exercise.id().equals(context.original().id())
                    || context.excludedIds().contains(exercise.id())) continue;
            var metadata = ExerciseMetadataCatalog.require(exercise.id());
            if (metadata.primaryRole() != original.primaryRole()
                    || Collections.disjoint(metadata.primaryAreas(), original.primaryAreas())
                    || !compatibleTask(context.original().measurement(), exercise.measurement())
                    || !metadata.suitability().appropriateFor(context.experience())
                    || !metadata.equipment().availableWith(context.availableEquipment())) continue;
            int progression = reviewedProgression(context.original().id(), exercise.id());
            int complexity = metadata.suitability().ordinal() - original.suitability().ordinal();
            if (context.reason() == Reason.EASIER && progression >= 0 && complexity >= 0) continue;
            if (context.reason() == Reason.HARDER && progression <= 0 && complexity <= 0) continue;
            // A known loading direction must not be contradicted by the general complexity class.
            if (context.reason() == Reason.EASIER && progression > 0
                    || context.reason() == Reason.HARDER && progression < 0) continue;
            var equipment = bestSetup(metadata, original, context);
            candidates.add(
                    new Candidate(
                            exercise,
                            metadata,
                            equipment,
                            goalAlignment(original, metadata, context.goal()),
                            overlap(original.roles(), metadata.roles()),
                            overlap(original.primaryAreas(), metadata.primaryAreas()),
                            equipmentChangeCost(equipment, original, context.reason()),
                            context.original().measurement() == exercise.measurement() ? 0 : 1,
                            Math.abs(complexity),
                            context.favouriteIds().contains(exercise.id()) ? 0 : 1,
                            context.historyIds().contains(exercise.id()) ? 0 : 1,
                            progression));
        }
        // When equipment is the actual obstacle, a different complete setup takes precedence
        // among already intent-compatible candidates. Other reasons have an equipment cost of0.
        candidates.sort(
                Comparator.comparingInt(Candidate::equipmentCost)
                        .thenComparing(Comparator.comparingInt(Candidate::goalAlignment).reversed())
                        .thenComparing(Comparator.comparingInt(Candidate::sharedRoles).reversed())
                        .thenComparing(
                                Comparator.comparingInt(Candidate::primaryOverlap).reversed())
                        .thenComparingInt(Candidate::measurementCost)
                        .thenComparingInt(Candidate::complexityDistance)
                        .thenComparingInt(Candidate::favouriteCost)
                        .thenComparingInt(Candidate::historyCost)
                        .thenComparing(candidate -> candidate.exercise().id()));
        var result = new ArrayList<Suggestion>();
        for (var candidate : candidates) {
            if (result.size() == limit) break;
            result.add(
                    new Suggestion(
                            candidate.exercise(),
                            explanation(context, original, candidate),
                            candidate.measurementCost() != 0));
        }
        return Immutable.copyList(result);
    }

    public static boolean compatibleTask(
            StrengthExercise.MeasurementProfile original,
            StrengthExercise.MeasurementProfile replacement) {
        Objects.requireNonNull(original, "original measurement");
        Objects.requireNonNull(replacement, "replacement measurement");
        return original.usesReps() && replacement.usesReps()
                || original.usesDuration() && replacement.usesDuration()
                || original.usesDistance() && replacement.usesDistance();
    }

    private record Candidate(
            StrengthExercise exercise,
            ExerciseMetadata metadata,
            Set<Equipment> equipment,
            int goalAlignment,
            int sharedRoles,
            int primaryOverlap,
            int equipmentCost,
            int measurementCost,
            int complexityDistance,
            int favouriteCost,
            int historyCost,
            int progression) {}

    private static int goalAlignment(
            ExerciseMetadata original, ExerciseMetadata replacement, GymProgramme.Goal goal) {
        return switch (goal) {
            case STRENGTH ->
                    original.roles().contains(Role.PRIMARY_STRENGTH)
                                    && replacement.roles().contains(Role.PRIMARY_STRENGTH)
                            ? 1
                            : 0;
            case POWER -> overlap(original.roles(), replacement.roles());
            case MUSCLE -> overlap(original.primaryAreas(), replacement.primaryAreas());
            case SUPPORT_RUNNING -> overlap(original.roles(), replacement.roles());
            case GENERAL -> replacement.suitability() == Suitability.BEGINNER_FRIENDLY ? 1 : 0;
        };
    }

    private static Set<Equipment> bestSetup(
            ExerciseMetadata metadata, ExerciseMetadata original, Context context) {
        Set<Equipment> best = null;
        int cost = Integer.MAX_VALUE;
        for (var setup : metadata.equipment().alternatives()) {
            if (!context.availableEquipment().containsAll(setup)) continue;
            int candidateCost = equipmentChangeCost(setup, original, context.reason());
            if (best == null
                    || candidateCost < cost
                    || candidateCost == cost && signature(setup).compareTo(signature(best)) < 0) {
                best = setup;
                cost = candidateCost;
            }
        }
        return Objects.requireNonNull(best, "eligible complete setup");
    }

    private static int equipmentChangeCost(
            Set<Equipment> setup, ExerciseMetadata original, Reason reason) {
        if (reason != Reason.EQUIPMENT_BUSY && reason != Reason.EQUIPMENT_UNAVAILABLE) return 0;
        var originalEquipment = EnumSet.noneOf(Equipment.class);
        original.equipment().alternatives().forEach(originalEquipment::addAll);
        // A different setup is useful, but sharing a bench is not proof that it is free. The
        // explanation explicitly asks the athlete to check; unavailable capabilities remain a gate.
        return overlap(setup, originalEquipment);
    }

    private static String explanation(
            Context context, ExerciseMetadata original, Candidate candidate) {
        String role = original.primaryRole().label().toLowerCase(Locale.UK);
        var areas = new ArrayList<String>();
        for (var area : ExerciseMetadata.BodyArea.values())
            if (original.primaryAreas().contains(area)
                    && candidate.metadata().primaryAreas().contains(area))
                areas.add(area.label().toLowerCase(Locale.UK));
        var text =
                new StringBuilder("Same ")
                        .append(role)
                        .append(" role with shared ")
                        .append(String.join(" / ", areas))
                        .append(" involvement. Uses ")
                        .append(equipmentLabel(candidate.equipment()))
                        .append('.');
        if (context.reason() == Reason.EQUIPMENT_BUSY)
            text.append(" Check that this setup is free; shared equipment may still be busy.");
        if (context.reason() == Reason.EASIER || context.reason() == Reason.HARDER) {
            if (candidate.progression() != 0)
                text.append(
                        candidate.progression() < 0
                                ? " Removes the added-loading variation."
                                : " Offers an added-loading progression; choose a manageable"
                                        + " load.");
            else
                text.append(
                        candidate.metadata().suitability().ordinal()
                                        < original.suitability().ordinal()
                                ? " A less technically complex setup."
                                : " A more technically complex variation within your selected"
                                        + " experience.");
            text.append(" Difficulty still depends on load, control and execution.");
        }
        if (candidate.measurementCost() != 0)
            text.append(
                    " Measurement changes: review a fresh target; previous targets and loads do not"
                            + " transfer.");
        else
            text.append(
                    " Previous loads do not transfer; choose and record the actual load or task.");
        if (context.favouriteIds().contains(candidate.exercise().id()))
            text.append(" Also saved as a favourite.");
        if (context.historyIds().contains(candidate.exercise().id()))
            text.append(
                    " You have recorded this exercise before; that is not a starting-load"
                            + " recommendation.");
        if (context.reason() == Reason.UNCOMFORTABLE)
            text.append(
                    " An alternative is not medical clearance; avoid movements that reproduce"
                            + " pain.");
        return text.toString();
    }

    private static String equipmentLabel(Set<Equipment> setup) {
        if (setup.isEmpty()) return "no external equipment";
        var labels = new ArrayList<String>();
        for (var item : Equipment.values()) {
            if (!setup.contains(item)) continue;
            labels.add(item.name().toLowerCase(Locale.UK).replace('_', ' '));
        }
        return String.join(" + ", labels);
    }

    private static String signature(Set<Equipment> setup) {
        var result = new StringBuilder();
        for (var value : Equipment.values())
            if (setup.contains(value)) result.append(value.name()).append('/');
        return result.toString();
    }

    /**
     * Reviewed added-load pairs, not a claim that technical complexity measures physical effort.
     */
    private static int reviewedProgression(String original, String replacement) {
        for (var pair : ADDED_LOAD_PAIRS) {
            if (pair[0].equals(original) && pair[1].equals(replacement)) return 1;
            if (pair[1].equals(original) && pair[0].equals(replacement)) return -1;
        }
        return 0;
    }

    private static Set<String> identities(Set<String> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id.trim().isEmpty()))
            throw new IllegalArgumentException("Exercise identity choices must be explicit.");
        return Immutable.copySet(ids);
    }

    private static int overlap(Set<?> left, Set<?> right) {
        int count = 0;
        for (var value : left) if (right.contains(value)) count++;
        return count;
    }
}
