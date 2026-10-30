package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.*;

/** Incomplete authoring is saveable; a preview must pass the complete programme contract. */
public record StrengthProgrammeDraft(
        String id,
        int revision,
        String programmeId,
        int baseRevision,
        String name,
        StrengthProgramme.Settings settings,
        LocalDate start,
        List<WeekBlock> blocks,
        StrengthProgramme.Origin origin,
        String templateId) {
    public record Day(
            String id,
            DayOfWeek weekday,
            String title,
            List<GymWorkoutDefinition.Slot> slots,
            String notes) {
        public Day {
            GymWorkoutDefinition.requireId(id, "draft workout");
            Objects.requireNonNull(weekday);
            StrengthProgramme.text(title, 120, false, "workout title");
            slots = Immutable.copyList(slots);
            GymWorkoutDefinition.validateSlots(slots, true);
            if (slots.size() > 12)
                throw new IllegalArgumentException("Use up to 12 programme exercises.");
            StrengthProgramme.text(notes, 2000, true, "workout notes");
        }
    }

    public record WeekBlock(
            String id, StrengthProgramme.Phase phase, String name, int weeks, List<Day> days) {
        public WeekBlock {
            GymWorkoutDefinition.requireId(id, "draft block");
            Objects.requireNonNull(phase);
            StrengthProgramme.text(name, 120, false, "block title");
            if (weeks < 1 || weeks > 26)
                throw new IllegalArgumentException("A block spans 1–26 weeks.");
            days = Immutable.copyList(days);
            if (days.size() > 6)
                throw new IllegalArgumentException("Use up to six weekly workouts.");
        }
    }

    public StrengthProgrammeDraft {
        GymWorkoutDefinition.requireId(id, "draft");
        GymWorkoutDefinition.requireId(programmeId, "programme");
        if (revision < 1 || baseRevision < 0)
            throw new IllegalArgumentException("Invalid draft revision.");
        if (name == null || name.length() > 120)
            throw new IllegalArgumentException("Programme name is too long.");
        Objects.requireNonNull(settings);
        Objects.requireNonNull(start);
        Objects.requireNonNull(origin);
        if (origin == StrengthProgramme.Origin.TEMPLATE)
            GymWorkoutDefinition.requireId(templateId, "template");
        else if (templateId != null)
            throw new IllegalArgumentException("Only a template has template provenance.");
        blocks = Immutable.copyList(blocks);
        if (blocks.size() > 12 || blocks.stream().mapToInt(WeekBlock::weeks).sum() > 52)
            throw new IllegalArgumentException("Use up to 12 blocks across 52 weeks.");
        var ids = new HashSet<String>();
        for (var b : blocks) {
            if (!ids.add(b.id())) throw new IllegalArgumentException("Duplicate draft block.");
            for (var d : b.days())
                if (!ids.add(d.id()))
                    throw new IllegalArgumentException("Duplicate draft workout.");
        }
    }

    /** No actuals, implicit activation, generated load or fabricated custom-exercise guidance. */
    public StrengthProgramme preview(
            Instant at, Set<String> excluded, Map<String, CustomStrengthExercise> custom) {
        Objects.requireNonNull(excluded);
        Objects.requireNonNull(custom);
        var result = new ArrayList<StrengthProgramme.Block>();
        LocalDate first = start;
        var tradeoffs = new LinkedHashSet<String>();
        tradeoffs.add("Custom targets are athlete-authored, not evidence of completed training.");
        tradeoffs.add(StrengthWorkoutBudget.explanation());
        for (var block : blocks) {
            if (block.days().size() != settings.frequency())
                throw new IllegalArgumentException(
                        "Each block must match the chosen weekly frequency.");
            var weekdays = EnumSet.noneOf(DayOfWeek.class);
            var workouts = new ArrayList<StrengthProgramme.Workout>();
            for (var day : block.days()) {
                if (!weekdays.add(day.weekday()) || !settings.days().contains(day.weekday()))
                    throw new IllegalArgumentException(
                            "Choose distinct available days for each workout.");
                if (day.slots().isEmpty())
                    throw new IllegalArgumentException("Add an exercise before previewing.");
                for (var slot : day.slots()) {
                    if (slot.target() == null)
                        throw new IllegalArgumentException(
                                "Review a target for every programme exercise.");
                    if (excluded.contains(slot.exercise().id()))
                        throw new IllegalArgumentException(
                                "Restore an excluded exercise before programming it.");
                    var metadata = ExerciseMetadataCatalog.find(slot.exercise().id());
                    ExerciseMetadata.EquipmentRequirement requiredEquipment;
                    if (metadata == null) {
                        var definition = custom.get(slot.exercise().id());
                        if (definition == null || !definition.exercise().equals(slot.exercise()))
                            throw new IllegalArgumentException(
                                    "Review the custom exercise metadata.");
                        requiredEquipment = definition.equipment();
                    } else {
                        if (!ExerciseCatalog.definition(slot.exercise().id())
                                .exercise()
                                .equals(slot.exercise()))
                            throw new IllegalArgumentException(
                                    "The canonical exercise definition is inconsistent.");
                        requiredEquipment = metadata.equipment();
                    }
                    if (!requiredEquipment.availableWith(settings.equipment()))
                        throw new IllegalArgumentException(
                                "Confirm the complete equipment setup for each exercise.");
                }
                for (int week = 0; week < block.weeks(); week++) {
                    int offset =
                            Math.floorMod(
                                    day.weekday().getValue() - first.getDayOfWeek().getValue(), 7);
                    LocalDate date = first.plusWeeks(week).plusDays(offset);
                    String workoutId = day.id() + "-" + date;
                    int minutes = StrengthWorkoutBudget.workoutMinutes(day.slots());
                    if (minutes > settings.sessionMinutes())
                        tradeoffs.add(
                                day.title()
                                        + ": estimated "
                                        + minutes
                                        + " minutes exceeds your "
                                        + settings.sessionMinutes()
                                        + " minute session budget. Review targets before"
                                        + " activation.");
                    workouts.add(
                            new StrengthProgramme.Workout(
                                    workoutId,
                                    date,
                                    minutes,
                                    new GymWorkoutDefinition(
                                            workoutId,
                                            baseRevision + 1,
                                            at,
                                            day.title(),
                                            day.slots()),
                                    day.notes()));
                }
            }
            result.add(
                    new StrengthProgramme.Block(
                            block.id(),
                            block.phase(),
                            block.name(),
                            first,
                            block.weeks(),
                            workouts,
                            "Athlete-authored block. Review targets and recovery before"
                                    + " activation."));
            first = first.plusWeeks(block.weeks());
        }
        return new StrengthProgramme(
                programmeId,
                baseRevision + 1,
                baseRevision,
                at,
                name,
                origin,
                templateId,
                settings,
                result,
                start,
                "Athlete reviewed a custom programme preview; not yet activated.",
                new ArrayList<>(tradeoffs));
    }

    public StrengthProgrammeDraft replaceBlock(WeekBlock block) {
        var next = new ArrayList<>(blocks);
        int index = -1;
        for (int i = 0; i < next.size(); i++) if (next.get(i).id().equals(block.id())) index = i;
        if (index < 0) next.add(block);
        else next.set(index, block);
        return copy(next);
    }

    public StrengthProgrammeDraft deleteBlock(String blockId) {
        var next = new ArrayList<>(blocks);
        if (!next.removeIf(b -> b.id().equals(blockId)))
            throw new IllegalArgumentException("Block not found.");
        return copy(next);
    }

    public StrengthProgrammeDraft duplicateBlock(String blockId, String newId) {
        if (blocks.stream().anyMatch(b -> b.id().equals(newId)))
            throw new IllegalArgumentException("Choose a new block identity.");
        var original = block(blockId);
        var days = new ArrayList<Day>();
        for (var day : original.days())
            days.add(
                    new Day(
                            newId + "-day-" + days.size(),
                            day.weekday(),
                            day.title(),
                            day.slots(),
                            day.notes()));
        return replaceBlock(
                new WeekBlock(newId, original.phase(), original.name(), original.weeks(), days));
    }

    public StrengthProgrammeDraft replaceDay(String blockId, Day day) {
        var block = block(blockId);
        var days = new ArrayList<>(block.days());
        int index = -1;
        for (int i = 0; i < days.size(); i++) if (days.get(i).id().equals(day.id())) index = i;
        if (index < 0) days.add(day);
        else days.set(index, day);
        return replaceBlock(
                new WeekBlock(block.id(), block.phase(), block.name(), block.weeks(), days));
    }

    public StrengthProgrammeDraft duplicateDay(
            String blockId, String dayId, String newId, DayOfWeek weekday) {
        if (blocks.stream().flatMap(b -> b.days().stream()).anyMatch(d -> d.id().equals(newId)))
            throw new IllegalArgumentException("Choose a new workout identity.");
        var original = day(blockId, dayId);
        return replaceDay(
                blockId,
                new Day(newId, weekday, original.title(), original.slots(), original.notes()));
    }

    public StrengthProgrammeDraft deleteDay(String blockId, String dayId) {
        var b = block(blockId);
        var days = new ArrayList<>(b.days());
        if (!days.removeIf(d -> d.id().equals(dayId)))
            throw new IllegalArgumentException("Workout not found.");
        return replaceBlock(new WeekBlock(b.id(), b.phase(), b.name(), b.weeks(), days));
    }

    public StrengthProgrammeDraft moveExercise(
            String fromBlock,
            String fromDay,
            String toBlock,
            String toDay,
            String slotId,
            int position) {
        var from = day(fromBlock, fromDay);
        var to = day(toBlock, toDay);
        var source = new ArrayList<>(from.slots());
        var slot =
                source.stream()
                        .filter(s -> s.id().equals(slotId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Exercise not found."));
        source.remove(slot);
        if (fromBlock.equals(toBlock) && fromDay.equals(toDay)) {
            if (position < 0 || position > source.size())
                throw new IllegalArgumentException("Invalid exercise position.");
            source.add(position, slot);
            return replaceDay(
                    fromBlock,
                    new Day(from.id(), from.weekday(), from.title(), source, from.notes()));
        }
        var target = new ArrayList<>(to.slots());
        if (position < 0 || position > target.size())
            throw new IllegalArgumentException("Invalid exercise position.");
        target.add(position, slot);
        return replaceDay(
                        fromBlock,
                        new Day(from.id(), from.weekday(), from.title(), source, from.notes()))
                .replaceDay(
                        toBlock, new Day(to.id(), to.weekday(), to.title(), target, to.notes()));
    }

    public StrengthProgrammeDraft reorderBlock(String id, int position) {
        var b = block(id);
        var next = new ArrayList<>(blocks);
        next.remove(b);
        if (position < 0 || position > next.size())
            throw new IllegalArgumentException("Invalid block position.");
        next.add(position, b);
        return copy(next);
    }

    private WeekBlock block(String id) {
        return blocks.stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Block not found."));
    }

    private Day day(String blockId, String id) {
        return block(blockId).days().stream()
                .filter(d -> d.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout not found."));
    }

    private StrengthProgrammeDraft copy(List<WeekBlock> next) {
        return new StrengthProgrammeDraft(
                id,
                revision + 1,
                programmeId,
                baseRevision,
                name,
                settings,
                start,
                next,
                origin,
                templateId);
    }
}
