package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.WorkoutCategory;
import org.racedaydreams.domain.coaching.Immutable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Original deterministic six-week blocks; scheduling penalties are inspectable product heuristics.
 */
public final class GymProgrammeGenerator {
    public GymProgramme generate(
            String id,
            int version,
            Instant createdAt,
            String reason,
            GymProgramme.Settings settings,
            LocalDate start,
            List<PlannedWorkout> runs,
            List<StrengthSession> actual) {
        return generate(
                id,
                version,
                createdAt,
                reason,
                settings,
                start,
                runs,
                actual,
                Collections.emptySet());
    }

    public GymProgramme generate(
            String id,
            int version,
            Instant createdAt,
            String reason,
            GymProgramme.Settings settings,
            LocalDate start,
            List<PlannedWorkout> runs,
            List<StrengthSession> actual,
            Set<String> favourites) {
        var sessions = new ArrayList<GymProgramme.Session>();
        var tradeoffs = new LinkedHashSet<String>();
        LocalDate end = start.plusWeeks(6).minusDays(1);
        var hardActual = StrengthAnalytics.hardLowerBodyDates(actual);
        var plans = new ArrayList<SessionPlan>();
        int lowerSessionOrdinal = 0;
        for (int i = 0; i < settings.frequency(); i++) {
            String template = template(settings.frequency(), i);
            boolean lowerTemplate = template.equals("Lower") || template.equals("Full body");
            var exercises = prescriptions(settings, template, lowerSessionOrdinal, favourites);
            if (lowerTemplate) lowerSessionOrdinal++;
            plans.add(new SessionPlan(template, exercises, hasLowerBodyWork(exercises)));
        }

        // Each programme week contains all seven weekdays, including at both block boundaries.
        for (int programmeWeek = 0; programmeWeek < 6; programmeWeek++) {
            LocalDate week = start.plusWeeks(programmeWeek);
            var candidates = new ArrayList<LocalDate>();
            for (int i = 0; i < 7; i++) {
                LocalDate day = week.plusDays(i);
                if (settings.days().contains(day.getDayOfWeek())) candidates.add(day);
            }
            var best = new Search(settings, runs, hardActual, plans, week);
            best.permute(candidates, new ArrayList<>(), new HashSet<>());
            for (int i = 0; i < best.chosen.size(); i++) {
                LocalDate day = best.chosen.get(i);
                SessionPlan plan = plans.get(i);
                tradeoffs.addAll(conflicts(day, plan.lower(), runs, hardActual));
                sessions.add(
                        new GymProgramme.Session(
                                id + "-v" + version + "-" + day,
                                day,
                                plan.template() + " " + (i + 1),
                                plan.exercises(),
                                explanation(settings, plan.exercises())));
            }
        }
        sessions.sort(Comparator.comparing(GymProgramme.Session::date));
        if (settings.frequency() > 3
                && settings.priority() == GymProgramme.Priority.RUNNING_FIRST) {
            tradeoffs.add(
                    "You selected frequent gym training with running first. Working sets are"
                        + " deliberately limited; more days are not inherently better.");
        }
        if (!settings.equipment().contains("full gym")
                && Collections.disjoint(
                        settings.equipment(),
                        Immutable.list("dumbbells", "barbell", "cables", "pull-up bar"))) {
            boolean trunkPrescribed =
                    plans.stream()
                            .flatMap(plan -> plan.exercises().stream())
                            .anyMatch(
                                    prescription ->
                                            ExerciseCatalog.definition(prescription.exerciseId())
                                                    .category()
                                                    .equals("Core"));
            tradeoffs.add(
                    trunkPrescribed
                            ? "No pulling equipment was selected. Trunk work fills a practical"
                                  + " slot; it is not equivalent to rows or pull-ups."
                            : "No pulling equipment was selected. This block does not prescribe"
                                  + " rows or pull-ups.");
        }
        if (settings.goal() == GymProgramme.Goal.POWER
                && settings.experience() == GymProgramme.Experience.BEGINNER) {
            tradeoffs.add(
                    "Beginner power preparation uses controlled fundamentals; technical Olympic"
                        + " lifts are not automatically prescribed.");
        }
        return new GymProgramme(
                id,
                version,
                createdAt,
                reason,
                settings,
                start,
                end,
                sessions,
                Immutable.copyList(tradeoffs));
    }

    static String template(int frequency, int index) {
        if (frequency <= 3) return "Full body";
        if (frequency == 4) return index % 2 == 0 ? "Upper" : "Lower";
        if (frequency == 5) return new String[] {"Upper", "Lower", "Push", "Pull", "Lower"}[index];
        return new String[] {"Push", "Pull", "Lower"}[index % 3];
    }

    private record SessionPlan(
            String template, List<GymProgramme.Prescription> exercises, boolean lower) {}

    private static final class Search {
        final GymProgramme.Settings settings;
        final List<SessionPlan> plans;
        final LocalDate week;
        final List<PlannedWorkout> runs;
        final Set<LocalDate> actual;
        List<LocalDate> chosen = new ArrayList<>();
        long best = Long.MAX_VALUE;

        Search(
                GymProgramme.Settings settings,
                List<PlannedWorkout> runs,
                Set<LocalDate> actual,
                List<SessionPlan> plans,
                LocalDate week) {
            this.settings = settings;
            this.runs = runs;
            this.actual = actual;
            this.plans = plans;
            this.week = week;
        }

        void permute(List<LocalDate> days, List<LocalDate> current, Set<LocalDate> used) {
            if (current.size() == plans.size()) {
                long score = 0;
                int conflictWeight =
                        settings.priority() == GymProgramme.Priority.RUNNING_FIRST
                                ? 100
                                : settings.priority() == GymProgramme.Priority.BALANCED ? 60 : 30;
                for (int i = 0; i < current.size(); i++) {
                    boolean lower = plans.get(i).lower();
                    score += schedulingPenalty(current.get(i), lower, runs, actual, conflictWeight);
                    for (int j = 0; j < i; j++) {
                        if (lower
                                && plans.get(j).lower()
                                && Math.abs(ChronoUnit.DAYS.between(current.get(j), current.get(i)))
                                        == 1) {
                            score += 40;
                        }
                    }
                    score +=
                            Math.abs(
                                    ChronoUnit.DAYS.between(week, current.get(i))
                                            - i * 7 / plans.size());
                }
                if (score < best) {
                    best = score;
                    chosen = new ArrayList<>(current);
                }
                return;
            }
            for (LocalDate day : days) {
                if (used.add(day)) {
                    current.add(day);
                    permute(days, current, used);
                    current.remove(current.size() - 1);
                    used.remove(day);
                }
            }
        }
    }

    private static int schedulingPenalty(
            LocalDate day,
            boolean lower,
            List<PlannedWorkout> runs,
            Set<LocalDate> actual,
            int conflictWeight) {
        if (!lower) return 0;
        int penalty = actual.contains(day.minusDays(1)) ? conflictWeight : 0;
        for (PlannedWorkout run : runs) {
            if (!demanding(run.category())) continue;
            if (run.date().equals(day) || run.date().equals(day.plusDays(1))) {
                penalty += conflictWeight;
            } else if (run.date().equals(day.minusDays(1))) {
                // Post-run recovery matters, but protecting the key run takes precedence.
                penalty += conflictWeight / 3;
            }
        }
        return penalty;
    }

    /** Core-only work does not carry the lower-body scheduling cost of a full-body session. */
    public static boolean hasLowerBodyWork(List<GymProgramme.Prescription> exercises) {
        for (GymProgramme.Prescription prescription : exercises) {
            var definition = ExerciseCatalog.definition(prescription.exerciseId());
            if (definition != null
                    && definition.exercise().region() != StrengthExercise.Region.UPPER
                    && !definition.category().equals("Core")) return true;
        }
        return false;
    }

    public static List<String> conflicts(
            LocalDate day, boolean lower, List<PlannedWorkout> runs, Set<LocalDate> actual) {
        var out = new ArrayList<String>();
        if (!lower) return out;
        for (PlannedWorkout run : runs) {
            if (!demanding(run.category())) continue;
            String relation;
            if (run.date().equals(day)) relation = "the same day as";
            else if (run.date().equals(day.plusDays(1))) relation = "the day before";
            else if (run.date().equals(day.minusDays(1))) relation = "the day after";
            else continue;
            String advice =
                    run.date().equals(day.minusDays(1))
                            ? "Review recovery and reduce gym effort if needed; nothing has been"
                                  + " removed."
                            : "Consider separating sessions or reducing gym effort; nothing has"
                                  + " been removed.";
            out.add(
                    "Lower-body strength on "
                            + date(day)
                            + " is "
                            + relation
                            + " "
                            + run.category().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                            + " on "
                            + date(run.date())
                            + ". "
                            + advice);
        }
        if (actual.contains(day.minusDays(1))) {
            out.add(
                    "Hard lower-body work was actually recorded before "
                            + date(day)
                            + ". Review the proposed session before training.");
        }
        return Immutable.copyList(out);
    }

    private static String date(LocalDate day) {
        return day.format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK));
    }

    public static boolean demanding(WorkoutCategory category) {
        return category == WorkoutCategory.LONG_RUN
                || category == WorkoutCategory.INTERVALS
                || category == WorkoutCategory.TEMPO_THRESHOLD_RUN
                || category == WorkoutCategory.RACE_PACE_RUN
                || category == WorkoutCategory.PROGRESSION_RUN
                || category == WorkoutCategory.RACE
                || category == WorkoutCategory.TIME_TRIAL;
    }

    public static boolean available(String id, Set<String> equipment) {
        if (equipment.contains("full gym")) return ExerciseCatalog.definition(id) != null;
        var definition = ExerciseCatalog.definition(id);
        if (definition == null) return false;
        boolean freeLoad =
                equipment.contains("dumbbells")
                        || equipment.contains("kettlebells")
                        || equipment.contains("barbell");
        if (id.equals("goblet-squat"))
            return equipment.contains("dumbbells") || equipment.contains("kettlebells");
        if (id.equals("split-squat")) return equipment.contains("bench") && freeLoad;
        if (id.equals("hip-thrust"))
            return equipment.contains("bench")
                    && (equipment.contains("barbell") || equipment.contains("dumbbells"));
        if (id.contains("pull-up")
                || id.contains("chin-up")
                || id.startsWith("hanging-")
                || id.equals("toes-to-bar")
                || id.equals("dead-hang")) {
            return equipment.contains("pull-up bar") && (!id.startsWith("weighted-") || freeLoad);
        }
        if (id.equals("inverted-row"))
            return equipment.contains("pull-up bar") || equipment.contains("squat rack");
        if (id.equals("bench-dip")) return equipment.contains("bench");
        if (id.equals("dip")
                || id.equals("weighted-dip")
                || id.equals("ab-wheel")
                || id.equals("nordic-hamstring-curl")
                || id.equals("swiss-ball-leg-curl")
                || id.equals("sliding-leg-curl")) return false;
        if (id.equals("step-up") || id.equals("box-step-up") || id.equals("high-step-up"))
            return equipment.contains("bench");
        String required = definition.equipment();
        if (required.equals("bodyweight")) return true;
        if (!equipment.contains(required)) return false;
        if (id.equals("back-squat") || id.equals("front-squat"))
            return equipment.contains("squat rack");
        if (id.contains("bench-press"))
            return equipment.contains("bench")
                    && (required.equals("dumbbells") || equipment.contains("squat rack"));
        return true;
    }

    private static String choose(Set<String> equipment, Set<String> favourites, String... ids) {
        for (String id : ids) {
            if (available(id, equipment)
                    && favourites.contains(id)
                    && TechniqueGuide.forExercise(id) != null
                    && TechniqueGuide.forExercise(ids[0]) != null
                    && TechniqueGuide.forExercise(id)
                            .pattern()
                            .equals(TechniqueGuide.forExercise(ids[0]).pattern())
                    && ExerciseCatalog.definition(id).exercise().measurement()
                            == ExerciseCatalog.definition(ids[0]).exercise().measurement())
                return id;
        }
        for (String id : ids) if (available(id, equipment)) return id;
        throw new IllegalArgumentException("No suitable exercise for selected equipment.");
    }

    private static List<GymProgramme.Prescription> prescriptions(
            GymProgramme.Settings settings,
            String template,
            int lowerSessionOrdinal,
            Set<String> favourites) {
        var equipment = settings.equipment();
        var ids = new ArrayList<String>();
        boolean lower = template.equals("Lower") || template.equals("Full body");
        boolean upper = !template.equals("Lower");
        if (lower) {
            ids.add(
                    choose(
                            equipment,
                            favourites,
                            lowerSessionOrdinal % 2 == 0 ? "back-squat" : "romanian-deadlift",
                            "goblet-squat",
                            "deadlift",
                            "bodyweight-squat"));
            ids.add(
                    choose(
                            equipment,
                            favourites,
                            settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                                    ? "split-squat"
                                    : "hip-thrust",
                            "reverse-lunge-bodyweight",
                            "glute-bridge"));
        }
        if (upper) {
            if (!template.equals("Pull"))
                ids.add(
                        choose(
                                equipment,
                                favourites,
                                "bench-press",
                                "dumbbell-bench-press",
                                "push-up"));
            if (!template.equals("Push"))
                ids.add(
                        choose(
                                equipment,
                                favourites,
                                "seated-cable-row",
                                "dumbbell-row",
                                "barbell-row",
                                "bird-dog"));
        }
        if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING && lower) {
            ids.add(
                    choose(
                            equipment,
                            favourites,
                            "calf-raise",
                            "single-leg-calf-raise-bodyweight"));
        }
        if (settings.minutes() >= 45 && ids.size() < 5) ids.add("plank");
        if (settings.minutes() >= 60 && upper) {
            ids.add(
                    choose(
                            equipment,
                            favourites,
                            template.equals("Pull") ? "barbell-curl" : "overhead-press",
                            "dumbbell-shoulder-press",
                            "dead-bug"));
        }
        int sets =
                settings.experience() == GymProgramme.Experience.BEGINNER
                                || settings.priority() == GymProgramme.Priority.RUNNING_FIRST
                        ? 2
                        : 3;
        if (settings.frequency() >= 4 && settings.priority() == GymProgramme.Priority.RUNNING_FIRST)
            sets = 2;
        int minimum =
                settings.goal() == GymProgramme.Goal.STRENGTH
                        ? 4
                        : settings.goal() == GymProgramme.Goal.POWER
                                ? 3
                                : settings.goal() == GymProgramme.Goal.MUSCLE ? 8 : 6;
        int maximum =
                settings.goal() == GymProgramme.Goal.STRENGTH
                        ? 6
                        : settings.goal() == GymProgramme.Goal.POWER
                                ? 5
                                : settings.goal() == GymProgramme.Goal.MUSCLE ? 12 : 10;
        int minRpe = settings.experience() == GymProgramme.Experience.BEGINNER ? 5 : 6;
        int maxRpe = settings.priority() == GymProgramme.Priority.RUNNING_FIRST ? 7 : 8;
        int rest =
                settings.goal() == GymProgramme.Goal.STRENGTH
                                || settings.goal() == GymProgramme.Goal.POWER
                        ? 150
                        : 90;
        var out = new ArrayList<GymProgramme.Prescription>();
        int budget = settings.minutes() - 6;
        for (String id : new LinkedHashSet<>(ids)) {
            int cost = sets * (rest / 60 + 1) + 1;
            if (budget < cost && !out.isEmpty()) break;
            budget -= cost;
            var profile = ExerciseCatalog.definition(id).exercise().measurement();
            String intent =
                    settings.goal() == GymProgramme.Goal.POWER
                            ? settings.experience() == GymProgramme.Experience.BEGINNER
                                    ? "Develop controlled fundamentals before adding power work. No"
                                          + " maximal testing."
                                    : "Use a manageable moderate load, controlled lowering and an"
                                          + " intentionally fast upward phase. Stop before speed or"
                                          + " control deteriorates."
                            : "Choose a manageable load or variation within the effort range;"
                                  + " progression is optional."
                                    + (favourites.contains(id)
                                            ? " This suitable exercise is one of your favourites."
                                            : "");
            out.add(
                    new GymProgramme.Prescription(
                            id,
                            sets,
                            profile.usesReps() ? minimum : 0,
                            profile.usesReps() ? maximum : 0,
                            profile.usesDuration() ? 30 : null,
                            profile.usesDistance() ? 30.0 : null,
                            minRpe,
                            maxRpe,
                            rest,
                            intent));
        }
        return Immutable.copyList(out);
    }

    private static String explanation(
            GymProgramme.Settings settings, List<GymProgramme.Prescription> exercises) {
        var movements = new LinkedHashSet<String>();
        for (GymProgramme.Prescription prescription : exercises) {
            var guide = TechniqueGuide.forExercise(prescription.exerciseId());
            if (guide == null) continue;
            String movement =
                    switch (guide.pattern()) {
                        case "split" -> "unilateral";
                        case "bridge" -> "hip extension";
                        case "press" -> "horizontal push";
                        case "overhead" -> "overhead push";
                        case "row", "pull" -> "upper-body pull";
                        case "core", "plank" -> "trunk";
                        default -> guide.pattern();
                    };
            movements.add(movement);
        }
        return "Stable six-week block. This session includes "
                + String.join(", ", movements)
                + " work. "
                + (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                        ? "Selected work supports your running plan."
                        : "Exercise and rep ranges reflect your selected strength goal.")
                + " Scheduling uses your available days and "
                + settings.priority().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " priority.";
    }
}
