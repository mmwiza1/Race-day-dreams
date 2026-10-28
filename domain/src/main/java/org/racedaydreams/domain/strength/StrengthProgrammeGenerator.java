package org.racedaydreams.domain.strength;

import static org.racedaydreams.domain.strength.ExerciseMetadata.Role.*;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.StrengthProgramme.*;
import org.racedaydreams.domain.strength.StrengthProgrammeTemplates.Split;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Original bounded programming heuristics. No load is invented and no programme is applied here.
 */
public final class StrengthProgrammeGenerator {
    public record Request(
            String id,
            Instant at,
            String name,
            Settings settings,
            LocalDate start,
            int weeks,
            String templateId,
            PrimaryFocus focus,
            Set<LocalDate> demandingRuns,
            LocalDate raceDate,
            Set<String> exclusions,
            Set<String> favourites,
            boolean recoveryAssessment) {
        public Request {
            GymWorkoutDefinition.requireId(id, "programme");
            Objects.requireNonNull(at);
            StrengthProgramme.text(name, 120, false, "programme name");
            Objects.requireNonNull(settings);
            Objects.requireNonNull(start);
            Objects.requireNonNull(focus);
            if (weeks < 1 || weeks > 26) throw new IllegalArgumentException("Generate 1–26 weeks.");
            demandingRuns = Immutable.copySet(demandingRuns);
            exclusions = Immutable.copySet(exclusions);
            favourites = Immutable.copySet(favourites);
            if (templateId != null
                    && StrengthProgrammeTemplates.require(templateId).frequency()
                            != settings.frequency())
                throw new IllegalArgumentException(
                        "Review the template frequency before generating.");
        }
    }

    public StrengthProgramme generate(Request request) {
        var settings = request.settings();
        Split split =
                request.templateId() != null
                        ? StrengthProgrammeTemplates.require(request.templateId()).split()
                        : settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                                ? Split.FULL_BODY
                                : settings.frequency() >= 5
                                        ? Split.PUSH_PULL_LEGS
                                        : settings.frequency() == 4
                                                ? Split.UPPER_LOWER
                                                : Split.FULL_BODY;
        var tradeoffs = new LinkedHashSet<String>();
        if (request.templateId() != null)
            tradeoffs.add(StrengthProgrammeTemplates.require(request.templateId()).tradeoff());
        tradeoffs.add(
                "Original planning heuristics: adjust effort and volume to your experience;"
                        + " prescriptions are not actual sets.");
        if (settings.goal() == GymProgramme.Goal.POWER) {
            tradeoffs.add(
                    "Power sets prioritise speed and control. Stop when either deteriorates; no"
                            + " maximal testing is required.");
            if (settings.experience() == GymProgramme.Experience.BEGINNER)
                tradeoffs.add(
                        "The current catalogue has no beginner-classified ballistic option for this"
                            + " setup. This preview builds stable strength and movement practice"
                            + " instead of inventing power exposure.");
        }
        var blocks = new ArrayList<Block>();
        LocalDate start = request.start();
        int blockIndex = 0;
        for (var phase : phases(request)) {
            String blockId = request.id() + "-block-" + blockIndex++;
            var workouts = new ArrayList<Workout>();
            for (int week = 0; week < phase.weeks; week++) {
                LocalDate first = start.plusWeeks(week);
                var days = selectDays(first, request, split);
                for (int day = 0; day < days.size(); day++) {
                    LocalDate date = days.get(day);
                    var layout = layout(split, day, settings.goal());
                    var roles = new ArrayList<>(layout.roles);
                    if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                            && settings.frequency() == 1) {
                        roles =
                                new ArrayList<>(
                                        Immutable.list(
                                                SQUAT,
                                                HINGE,
                                                UNILATERAL,
                                                CALF,
                                                CORE,
                                                HORIZONTAL_PULL));
                    }
                    if (settings.goal() == GymProgramme.Goal.POWER
                            && settings.experience() != GymProgramme.Experience.BEGINNER)
                        roles.add(0, POWER);
                    int limit =
                            settings.experience() == GymProgramme.Experience.BEGINNER
                                    ? 4
                                    : settings.experience() == GymProgramme.Experience.INTERMEDIATE
                                            ? 6
                                            : 7;
                    if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING)
                        limit = Math.max(5, limit);
                    if (settings.sessionMinutes() <= 30)
                        limit =
                                Math.min(
                                        limit,
                                        settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                                                ? 5
                                                : 4);
                    if (settings.goal() == GymProgramme.Goal.POWER) limit = Math.min(limit, 5);
                    if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                            && settings.frequency() == 1) limit = 6;
                    var slots = new ArrayList<GymWorkoutDefinition.Slot>();
                    var seen = new HashSet<String>();
                    int minutes = 4;
                    boolean raceWeek =
                            request.focus() != PrimaryFocus.STRENGTH
                                    && request.raceDate() != null
                                    && !date.isAfter(request.raceDate())
                                    && !date.isBefore(request.raceDate().minusDays(7));
                    if (raceWeek)
                        tradeoffs.add(
                                "Race-week previews use fewer working sets; activation is your"
                                        + " choice and the running plan stays unchanged.");
                    for (var role : roles) {
                        if (slots.size() >= limit) break;
                        var exercise =
                                select(
                                        role,
                                        seen,
                                        settings,
                                        request,
                                        day,
                                        layout.title.startsWith("Push"),
                                        tradeoffs);
                        if (exercise == null) continue;
                        var target =
                                target(
                                        exercise,
                                        role,
                                        slots.isEmpty(),
                                        phase.phase,
                                        settings,
                                        raceWeek);
                        int estimate = estimateMinutes(target);
                        // Reduce optional volume rather than inventing a shorter session estimate.
                        while (minutes + estimate > settings.sessionMinutes()
                                && target.sets() > 1) {
                            target = sets(target, target.sets() - 1);
                            estimate = estimateMinutes(target);
                        }
                        if (minutes + estimate > settings.sessionMinutes()) {
                            tradeoffs.add(
                                    "The session time budget limits accessory exposure; review"
                                            + " weekly balance before activation.");
                            continue;
                        }
                        seen.add(exercise.id());
                        minutes += estimate;
                        slots.add(
                                new GymWorkoutDefinition.Slot(
                                        "slot-" + slots.size(), exercise, target, null));
                    }
                    if (slots.size() < 2)
                        throw new IllegalArgumentException(
                                "Not enough eligible exercises for a balanced session. Review"
                                        + " equipment, exclusions or template.");
                    String id = request.id() + "-" + date;
                    String sessionContext = "";
                    if (request.focus() != PrimaryFocus.STRENGTH
                            && lowerBody(slots)
                            && conflict(date, request.demandingRuns()) > 0) {
                        sessionContext =
                                date
                                        + ": lower-body work is "
                                        + (request.demandingRuns().contains(date)
                                                ? "on a demanding run day"
                                                : request.demandingRuns().contains(date.plusDays(1))
                                                        ? "before a demanding run"
                                                        : "after a demanding run")
                                        + "; consider moving it or keeping more effort reserve.";
                        tradeoffs.add(sessionContext);
                    }
                    workouts.add(
                            new Workout(
                                    id,
                                    date,
                                    minutes,
                                    new GymWorkoutDefinition(
                                            id, 1, request.at(), layout.title, slots),
                                    "Use manageable working loads. "
                                            + phaseExplanation(phase.phase)
                                            + (sessionContext.isEmpty()
                                                    ? ""
                                                    : " " + sessionContext)));
                }
            }
            blocks.add(
                    new Block(
                            blockId,
                            phase.phase,
                            title(phase.phase),
                            start,
                            phase.weeks,
                            workouts,
                            phaseExplanation(phase.phase)));
            start = start.plusWeeks(phase.weeks);
        }
        var summary = new ArrayList<>(tradeoffs);
        if (summary.size() > 100) {
            int remaining = summary.size() - 99;
            summary = new ArrayList<>(summary.subList(0, 99));
            summary.add(
                    remaining + " additional trade-offs are retained in the dated workout notes.");
        }
        return new StrengthProgramme(
                request.id(),
                1,
                0,
                request.at(),
                request.name(),
                request.templateId() == null ? Origin.GENERATED : Origin.TEMPLATE,
                request.templateId(),
                settings,
                blocks,
                request.start(),
                "Athlete requested a preview; nothing is activated until explicitly accepted.",
                summary);
    }

    private record PhaseLength(Phase phase, int weeks) {}

    private static List<PhaseLength> phases(Request request) {
        int weeks = request.weeks();
        var settings = request.settings();
        var result = new ArrayList<PhaseLength>();
        int remaining = weeks;
        int recoveryWeeks = request.recoveryAssessment() && weeks >= 4 ? 1 : 0;
        int foundation =
                weeks >= 8
                        ? settings.experience() == GymProgramme.Experience.BEGINNER
                                ? 3
                                : settings.experience() == GymProgramme.Experience.EXPERIENCED
                                        ? 1
                                        : 2
                        : 1;
        result.add(new PhaseLength(Phase.FOUNDATION, foundation));
        remaining -= foundation + recoveryWeeks;
        boolean appropriate =
                settings.frequency() >= 2
                        && (settings.experience() != GymProgramme.Experience.BEGINNER
                                || settings.goal() == GymProgramme.Goal.STRENGTH && weeks >= 10)
                        && !(settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING
                                && settings.priority() == GymProgramme.Priority.RUNNING_FIRST);
        int intensify = weeks >= 8 && appropriate ? 2 : 0;
        if (remaining > intensify)
            result.add(new PhaseLength(Phase.DEVELOPMENT, remaining - intensify));
        if (intensify > 0) result.add(new PhaseLength(Phase.INTENSIFICATION, intensify));
        if (recoveryWeeks > 0) result.add(new PhaseLength(Phase.RECOVERY_ASSESSMENT, 1));
        return result;
    }

    private record Layout(String title, List<ExerciseMetadata.Role> roles) {}

    private static Layout layout(Split split, int index, GymProgramme.Goal goal) {
        if (split == Split.UPPER_LOWER) {
            if (index % 2 == 0)
                return new Layout(
                        index == 0 ? "Upper · horizontal emphasis" : "Upper · vertical emphasis",
                        index == 0
                                ? Immutable.list(
                                        HORIZONTAL_PUSH,
                                        HORIZONTAL_PULL,
                                        VERTICAL_PUSH,
                                        VERTICAL_PULL,
                                        ISOLATION,
                                        CORE)
                                : Immutable.list(
                                        VERTICAL_PUSH,
                                        VERTICAL_PULL,
                                        HORIZONTAL_PUSH,
                                        HORIZONTAL_PULL,
                                        ISOLATION,
                                        CORE));
            return new Layout(
                    index == 1 ? "Lower · squat emphasis" : "Lower · hinge emphasis",
                    Immutable.list(
                            index == 1 ? SQUAT : HINGE,
                            UNILATERAL,
                            CALF,
                            CORE,
                            index == 1 ? HAMSTRING : SQUAT));
        }
        if (split == Split.PUSH_PULL_LEGS) {
            switch (index % 3) {
                case 0:
                    return new Layout(
                            index < 3 ? "Push A" : "Push B",
                            index < 3
                                    ? Immutable.list(
                                            HORIZONTAL_PUSH, VERTICAL_PUSH, ISOLATION, CORE)
                                    : Immutable.list(
                                            VERTICAL_PUSH, HORIZONTAL_PUSH, ISOLATION, CORE));
                case 1:
                    return new Layout(
                            index < 3 ? "Pull A" : "Pull B",
                            index < 3
                                    ? Immutable.list(HORIZONTAL_PULL, VERTICAL_PULL, CORE, CARRY)
                                    : Immutable.list(VERTICAL_PULL, HORIZONTAL_PULL, CARRY, CORE));
                default:
                    return new Layout(
                            index < 3 ? "Legs · squat emphasis" : "Legs · hinge emphasis",
                            Immutable.list(
                                    index < 3 ? SQUAT : HINGE, UNILATERAL, CALF, CORE, HAMSTRING));
            }
        }
        if (goal == GymProgramme.Goal.SUPPORT_RUNNING)
            return new Layout(
                    "Running support " + (index + 1),
                    Immutable.list(
                            index % 2 == 0 ? SQUAT : HINGE,
                            UNILATERAL,
                            CALF,
                            CORE,
                            index % 2 == 0 ? HORIZONTAL_PUSH : HORIZONTAL_PULL,
                            index % 2 == 0 ? HINGE : SQUAT));
        return new Layout(
                "Full body " + (index + 1),
                Immutable.list(
                        index % 2 == 0 ? SQUAT : HINGE,
                        index % 2 == 0 ? HORIZONTAL_PUSH : VERTICAL_PUSH,
                        index % 2 == 0 ? HORIZONTAL_PULL : VERTICAL_PULL,
                        CORE,
                        UNILATERAL,
                        CALF,
                        ISOLATION));
    }

    private static StrengthExercise select(
            ExerciseMetadata.Role role,
            Set<String> seen,
            Settings settings,
            Request request,
            int variation,
            boolean pushDay,
            Set<String> tradeoffs) {
        var options =
                ExerciseCatalog.exercises().stream()
                        .filter(
                                e -> {
                                    var m = ExerciseMetadataCatalog.require(e.id());
                                    return m.primaryRole() == role
                                            && (role != ISOLATION
                                                    || !pushDay
                                                    || m.primaryAreas()
                                                            .contains(
                                                                    ExerciseMetadata.BodyArea
                                                                            .TRICEPS))
                                            && !seen.contains(e.id())
                                            && !request.exclusions().contains(e.id())
                                            && m.suitability().appropriateFor(settings.experience())
                                            && m.equipment().availableWith(settings.equipment());
                                })
                        .sorted(
                                Comparator.<StrengthExercise>comparingInt(
                                                e -> preferred(e, role, settings, variation))
                                        .thenComparingInt(
                                                e -> request.favourites().contains(e.id()) ? 0 : 1)
                                        .thenComparing(StrengthExercise::id))
                        .collect(Collectors.toList());
        if (options.isEmpty()) {
            tradeoffs.add(
                    "No eligible "
                            + role.label().toLowerCase(Locale.UK)
                            + " exercise for the selected equipment, experience and exclusions;"
                            + " this exposure is missing.");
            return null;
        }
        return options.get(0);
    }

    private static int preferred(
            StrengthExercise e, ExerciseMetadata.Role role, Settings settings, int variation) {
        // Selection remains stable within a block: no weekly exercise roulette or stronger load
        // inference.
        if (settings.experience() == GymProgramme.Experience.BEGINNER)
            return e.mode() == StrengthExercise.Mode.BODYWEIGHT ? 0 : 1;
        String preferred =
                role == SQUAT
                        ? "back-squat"
                        : role == HINGE
                                ? "romanian-deadlift"
                                : role == HORIZONTAL_PUSH
                                        ? "bench-press"
                                        : role == HORIZONTAL_PULL
                                                ? "dumbbell-row"
                                                : role == VERTICAL_PUSH
                                                        ? "overhead-press"
                                                        : role == VERTICAL_PULL
                                                                ? "lat-pulldown"
                                                                : role == POWER
                                                                        ? settings.experience()
                                                                                        == GymProgramme
                                                                                                .Experience
                                                                                                .EXPERIENCED
                                                                                ? "high-pull"
                                                                                : "kettlebell-swing"
                                                                        : "";
        return e.id().equals(preferred) ? 0 : 1;
    }

    private static GymWorkoutDefinition.Target target(
            StrengthExercise e,
            ExerciseMetadata.Role role,
            boolean primary,
            Phase phase,
            Settings s,
            boolean raceWeek) {
        boolean beginner = s.experience() == GymProgramme.Experience.BEGINNER;
        int sets = beginner ? 2 : 3;
        if (s.goal() == GymProgramme.Goal.MUSCLE
                && s.priority() == GymProgramme.Priority.STRENGTH_FIRST
                && s.experience() == GymProgramme.Experience.EXPERIENCED) sets = 4;
        if (s.priority() == GymProgramme.Priority.RUNNING_FIRST
                && e.region() != StrengthExercise.Region.UPPER) sets = Math.min(sets, 2);
        int low = 8, high = 12, rest = primary ? 120 : 60;
        if (s.goal() == GymProgramme.Goal.STRENGTH) {
            low = primary ? 4 : 6;
            high = primary ? 6 : 10;
            rest = primary ? 180 : 90;
        } else if (s.goal() == GymProgramme.Goal.SUPPORT_RUNNING) {
            low = primary && !beginner ? 5 : 8;
            high = primary && !beginner ? 8 : 12;
            if (s.priority() == GymProgramme.Priority.RUNNING_FIRST) sets = 2;
        } else if (role == POWER) {
            low = 3;
            high = 5;
            sets = 3;
            rest = 180;
        }
        int minEffort = beginner ? 5 : 6, maxEffort = beginner ? 7 : 8;
        if (s.goal() == GymProgramme.Goal.POWER && role != POWER) {
            sets = Math.min(sets, 2);
            low = 6;
            high = 8;
        }
        if (phase == Phase.FOUNDATION) {
            sets = Math.min(sets, beginner ? 1 : 2);
            minEffort = beginner ? 4 : 5;
            maxEffort = beginner ? 6 : 7;
        }
        if (phase == Phase.INTENSIFICATION && role == POWER) {
            sets = 2;
            low = 2;
            high = 3;
            rest = 210;
        }
        if (phase == Phase.INTENSIFICATION && primary && role != POWER) {
            if (s.goal() == GymProgramme.Goal.STRENGTH) {
                low = 3;
                high = 5;
                rest = 180;
            } else if (s.goal() == GymProgramme.Goal.MUSCLE) {
                low = 6;
                high = 10;
            }
            if (!beginner) {
                minEffort = 7;
                maxEffort = 8;
            }
        }
        if (phase == Phase.RECOVERY_ASSESSMENT || raceWeek) {
            sets = 1;
            minEffort = 5;
            maxEffort = 6;
        }
        if (s.goal() == GymProgramme.Goal.POWER && phase != Phase.FOUNDATION) {
            minEffort = 5;
            maxEffort = phase == Phase.RECOVERY_ASSESSMENT || raceWeek ? 6 : 7;
        }
        var effort =
                new GymWorkoutDefinition.EffortRange(
                        GymWorkoutDefinition.EffortScale.RPE, minEffort, maxEffort);
        return new GymWorkoutDefinition.Target(
                sets,
                e.measurement().usesReps() ? low : null,
                e.measurement().usesReps() ? high : null,
                e.measurement().usesDuration() ? 30L : null,
                e.measurement().usesDistance() ? 20.0 : null,
                effort,
                rest,
                null);
    }

    private static GymWorkoutDefinition.Target sets(GymWorkoutDefinition.Target t, int sets) {
        return new GymWorkoutDefinition.Target(
                sets,
                t.minimumReps(),
                t.maximumReps(),
                t.seconds(),
                t.metres(),
                t.effort(),
                t.restSeconds(),
                null);
    }

    private static int estimateMinutes(GymWorkoutDefinition.Target t) {
        return StrengthWorkoutBudget.exerciseMinutes(t);
    }

    private static boolean lowerBody(List<GymWorkoutDefinition.Slot> slots) {
        return slots.stream()
                .anyMatch(
                        s ->
                                s.exercise().region() != StrengthExercise.Region.UPPER
                                        && ExerciseMetadataCatalog.require(s.exercise().id())
                                                        .primaryRole()
                                                != CORE);
    }

    private static int conflict(LocalDate date, Set<LocalDate> runs) {
        return (runs.contains(date) ? 5 : 0)
                + (runs.contains(date.plusDays(1)) ? 4 : 0)
                + (runs.contains(date.minusDays(1)) ? 2 : 0);
    }

    private static List<LocalDate> selectDays(LocalDate first, Request r, Split split) {
        var days = new ArrayList<LocalDate>();
        for (int i = 0; i < 7; i++)
            if (r.settings().days().contains(first.plusDays(i).getDayOfWeek()))
                days.add(first.plusDays(i));
        // Enumerate at most 127 subsets; consider placement and avoid adjacent gym days when
        // feasible.
        List<LocalDate> best = null;
        int bestCost = Integer.MAX_VALUE;
        for (int mask = 1; mask < 1 << days.size(); mask++) {
            if (Integer.bitCount(mask) != r.settings().frequency()) continue;
            var chosen = new ArrayList<LocalDate>();
            for (int i = 0; i < days.size(); i++) if ((mask & 1 << i) != 0) chosen.add(days.get(i));
            int cost = 0;
            for (int i = 0; i < chosen.size(); i++) {
                boolean lower =
                        split == Split.FULL_BODY
                                || r.settings().goal() == GymProgramme.Goal.POWER
                                        && r.settings().experience()
                                                != GymProgramme.Experience.BEGINNER
                                || split == Split.UPPER_LOWER && i % 2 == 1
                                || split == Split.PUSH_PULL_LEGS && i % 3 == 2;
                if (lower && r.focus() != PrimaryFocus.STRENGTH)
                    cost += conflict(chosen.get(i), r.demandingRuns());
                if (i > 0 && chosen.get(i - 1).plusDays(1).equals(chosen.get(i))) cost += 1;
            }
            if (cost < bestCost) {
                best = chosen;
                bestCost = cost;
            }
        }
        return Objects.requireNonNull(best);
    }

    private static String title(Phase p) {
        return p.name().toLowerCase(Locale.UK).replace('_', ' ');
    }

    private static String phaseExplanation(Phase p) {
        switch (p) {
            case FOUNDATION:
                return "Practise stable movements, establish manageable working loads and keep"
                        + " effort in reserve.";
            case DEVELOPMENT:
                return "Repeat key movements with goal-specific working volume; load progression"
                        + " requires actual evidence.";
            case INTENSIFICATION:
                return "Use more specific primary work with controlled volume; accessories remain"
                        + " supportive.";
            default:
                return "Optional lower-volume practice and reflection; no maximal assessment is"
                        + " required.";
        }
    }
}
