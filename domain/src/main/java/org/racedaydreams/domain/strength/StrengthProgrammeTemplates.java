package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.util.List;

/** Original editable starting structures, not branded programmes or guaranteed outcomes. */
public final class StrengthProgrammeTemplates {
    private StrengthProgrammeTemplates() {}

    public enum Split {
        FULL_BODY,
        UPPER_LOWER,
        PUSH_PULL_LEGS
    }

    public record Template(
            String id,
            String name,
            int frequency,
            Split split,
            GymProgramme.Goal defaultGoal,
            String intendedUser,
            String emphasis,
            int suggestedMinutes,
            String tradeoff) {}

    public static List<Template> all() {
        return Immutable.list(
                template(
                        "full-body-2",
                        "2-day Full Body",
                        2,
                        Split.FULL_BODY,
                        GymProgramme.Goal.GENERAL,
                        "A manageable whole-body starting point",
                        "Repeated practice with recovery days",
                        45,
                        "Fewer weekly sessions leave less room for specialised accessory work."),
                template(
                        "full-body-3",
                        "3-day Full Body",
                        3,
                        Split.FULL_BODY,
                        GymProgramme.Goal.GENERAL,
                        "Athletes wanting frequent whole-body practice",
                        "Alternating squat and hinge emphasis",
                        45,
                        "Space sessions apart where availability permits."),
                template(
                        "upper-lower-4",
                        "4-day Upper / Lower",
                        4,
                        Split.UPPER_LOWER,
                        GymProgramme.Goal.MUSCLE,
                        "Athletes comfortable with four weekly sessions",
                        "Two distinct upper and lower days",
                        60,
                        "Lower days may compete with demanding running; review placement."),
                template(
                        "push-pull-legs-3",
                        "3-day Push / Pull / Legs",
                        3,
                        Split.PUSH_PULL_LEGS,
                        GymProgramme.Goal.MUSCLE,
                        "Athletes preferring focused sessions",
                        "Push, pull and lower-body exposure",
                        60,
                        "Each emphasis appears once weekly; missing a session affects weekly"
                                + " balance."),
                template(
                        "push-pull-legs-6",
                        "6-day Push / Pull / Legs",
                        6,
                        Split.PUSH_PULL_LEGS,
                        GymProgramme.Goal.MUSCLE,
                        "Experienced athletes with six available days",
                        "A/B primary movements, bounded session volume",
                        45,
                        "High attendance demand; not a shortcut to faster progress."),
                template(
                        "strength-3",
                        "Strength foundation",
                        3,
                        Split.FULL_BODY,
                        GymProgramme.Goal.STRENGTH,
                        "Athletes prioritising repeatable compound strength",
                        "Lower-rep primary work and longer rest",
                        60,
                        "Longer rests limit accessory volume within short sessions."),
                template(
                        "muscle-4",
                        "Build muscle",
                        4,
                        Split.UPPER_LOWER,
                        GymProgramme.Goal.MUSCLE,
                        "Athletes prioritising broad muscle exposure",
                        "Moderate repetitions with targeted accessories",
                        60,
                        "Working-set counts are planning estimates, not equal biological"
                                + " stimulus."),
                template(
                        "running-support-2",
                        "Running support · 2 days",
                        2,
                        Split.FULL_BODY,
                        GymProgramme.Goal.SUPPORT_RUNNING,
                        "Runners adding regular strength",
                        "Unilateral, lower-leg, trunk and upper maintenance",
                        45,
                        "Keep familiar loads near important runs; nothing changes the running"
                                + " plan."),
                template(
                        "running-support-3",
                        "Running support · 3 days",
                        3,
                        Split.FULL_BODY,
                        GymProgramme.Goal.SUPPORT_RUNNING,
                        "Runners who prefer shorter frequent gym sessions",
                        "Distributed lower-body and trunk work",
                        45,
                        "Three gym visits require careful placement around quality and long"
                                + " runs."));
    }

    public static Template require(String id) {
        return all().stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown programme template."));
    }

    private static Template template(
            String id,
            String name,
            int frequency,
            Split split,
            GymProgramme.Goal goal,
            String user,
            String emphasis,
            int minutes,
            String tradeoff) {
        return new Template(id, name, frequency, split, goal, user, emphasis, minutes, tradeoff);
    }
}
