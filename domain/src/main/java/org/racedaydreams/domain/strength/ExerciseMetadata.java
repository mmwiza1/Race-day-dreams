package org.racedaydreams.domain.strength;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Original product taxonomy for browsing and inspectable programming rules. Muscle areas are broad
 * descriptions of involvement, not measured stimulus, medical advice or set-equivalence weights.
 * This static metadata never changes the exercise definition preserved in an actual session.
 */
public record ExerciseMetadata(
        String exerciseId,
        Role primaryRole,
        Set<Role> roles,
        Set<BodyArea> primaryAreas,
        Set<BodyArea> secondaryAreas,
        Suitability suitability,
        EquipmentRequirement equipment) {
    public enum Role {
        PRIMARY_STRENGTH("Primary strength"),
        SQUAT("Squat"),
        HINGE("Hinge"),
        HIP_EXTENSION("Hip extension"),
        UNILATERAL("Unilateral"),
        HORIZONTAL_PUSH("Horizontal push"),
        VERTICAL_PUSH("Vertical push"),
        HORIZONTAL_PULL("Horizontal pull"),
        VERTICAL_PULL("Vertical pull"),
        HAMSTRING("Hamstring"),
        CALF("Calf / lower leg"),
        CORE("Core"),
        CARRY("Carry"),
        GRIP("Grip"),
        CONDITIONING("Conditioning"),
        POWER("Power"),
        ISOLATION("Isolation");

        private final String label;
        Role(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum BodyArea {
        CHEST("Chest"),
        UPPER_BACK("Upper back"),
        LATS("Lats"),
        SHOULDERS("Shoulders"),
        BICEPS("Biceps"),
        TRICEPS("Triceps"),
        QUADS("Quads"),
        HAMSTRINGS("Hamstrings"),
        GLUTES("Glutes"),
        CALVES("Calves"),
        LOWER_LEG("Lower leg"),
        TRUNK("Trunk"),
        FOREARMS("Forearms / grip"),
        HIP_ADDUCTORS("Inner thigh"),
        HIP_FLEXORS("Hip flexors");

        private final String label;
        BodyArea(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Complexity of the canonical variation; technical does not mean unsafe. */
    public enum Suitability {
        BEGINNER_FRIENDLY,
        INTERMEDIATE,
        TECHNICAL;

        public boolean appropriateFor(GymProgramme.Experience experience) {
            Objects.requireNonNull(experience, "experience");
            return this == BEGINNER_FRIENDLY
                    || experience == GymProgramme.Experience.EXPERIENCED
                    || this == INTERMEDIATE && experience == GymProgramme.Experience.INTERMEDIATE;
        }
    }

    public enum EquipmentGroup {
        BARBELL, DUMBBELLS, KETTLEBELLS, CABLES, MACHINES, SMITH_MACHINE,
        PULL_UP_BAR, SLED, OTHER
    }

    /** Specific capabilities: owning one machine does not imply owning every machine. */
    public enum Equipment {
        BARBELL(EquipmentGroup.BARBELL),
        EZ_BAR(EquipmentGroup.BARBELL),
        DUMBBELLS(EquipmentGroup.DUMBBELLS),
        KETTLEBELLS(EquipmentGroup.KETTLEBELLS),
        TRAP_BAR(EquipmentGroup.BARBELL),
        SQUAT_RACK(EquipmentGroup.OTHER),
        BENCH(EquipmentGroup.OTHER),
        ADJUSTABLE_BENCH(EquipmentGroup.OTHER),
        DECLINE_BENCH(EquipmentGroup.OTHER),
        PREACHER_PAD(EquipmentGroup.OTHER),
        STABLE_STEP(EquipmentGroup.OTHER),
        LIFTING_BLOCKS(EquipmentGroup.OTHER),
        DEFICIT_PLATFORM(EquipmentGroup.OTHER),
        LANDMINE_ANCHOR(EquipmentGroup.OTHER),
        PULL_UP_BAR(EquipmentGroup.PULL_UP_BAR),
        NEUTRAL_GRIP_BAR(EquipmentGroup.PULL_UP_BAR),
        SECURE_LOW_BAR(EquipmentGroup.PULL_UP_BAR),
        PARALLEL_BARS(EquipmentGroup.OTHER),
        DIP_BELT(EquipmentGroup.OTHER),
        WEIGHTED_VEST(EquipmentGroup.OTHER),
        WEIGHT_PLATE(EquipmentGroup.OTHER),
        CABLE_STATION(EquipmentGroup.CABLES),
        CABLE_HANDLE(EquipmentGroup.CABLES),
        CABLE_ROPE(EquipmentGroup.CABLES),
        CABLE_ANKLE_CUFF(EquipmentGroup.CABLES),
        PULLDOWN_SEAT_AND_PAD(EquipmentGroup.CABLES),
        SMITH_MACHINE(EquipmentGroup.SMITH_MACHINE),
        HACK_SQUAT_MACHINE(EquipmentGroup.MACHINES),
        PENDULUM_SQUAT_MACHINE(EquipmentGroup.MACHINES),
        BELT_SQUAT_MACHINE(EquipmentGroup.MACHINES),
        LEG_PRESS_MACHINE(EquipmentGroup.MACHINES),
        LEG_EXTENSION_MACHINE(EquipmentGroup.MACHINES),
        LYING_LEG_CURL_MACHINE(EquipmentGroup.MACHINES),
        SEATED_LEG_CURL_MACHINE(EquipmentGroup.MACHINES),
        STANDING_LEG_CURL_MACHINE(EquipmentGroup.MACHINES),
        STANDING_CALF_MACHINE(EquipmentGroup.MACHINES),
        SEATED_CALF_MACHINE(EquipmentGroup.MACHINES),
        DONKEY_CALF_MACHINE(EquipmentGroup.MACHINES),
        CHEST_PRESS_MACHINE(EquipmentGroup.MACHINES),
        PEC_DECK_MACHINE(EquipmentGroup.MACHINES),
        REVERSE_PEC_DECK_MACHINE(EquipmentGroup.MACHINES),
        ROW_MACHINE(EquipmentGroup.MACHINES),
        SHOULDER_PRESS_MACHINE(EquipmentGroup.MACHINES),
        GLUTE_HAM_DEVELOPER(EquipmentGroup.MACHINES),
        EXERCISE_BALL(EquipmentGroup.OTHER),
        SLIDERS(EquipmentGroup.OTHER),
        SLIDING_SURFACE(EquipmentGroup.OTHER),
        SECURE_FOOT_ANCHOR(EquipmentGroup.OTHER),
        AB_WHEEL(EquipmentGroup.OTHER),
        MEDICINE_BALL(EquipmentGroup.OTHER),
        SANDBAG(EquipmentGroup.OTHER),
        SLED(EquipmentGroup.SLED),
        SLED_PULL_STRAP(EquipmentGroup.SLED),
        WALL(EquipmentGroup.OTHER);

        private final EquipmentGroup group;
        Equipment(EquipmentGroup group) { this.group = group; }
        public EquipmentGroup group() { return group; }
    }

    /** Each alternative is a complete setup. An empty setup means no external equipment. */
    public record EquipmentRequirement(List<Set<Equipment>> alternatives) {
        public EquipmentRequirement {
            if (alternatives == null || alternatives.isEmpty()) {
                throw new IllegalArgumentException("At least one complete equipment setup is required.");
            }
            var copy = new ArrayList<Set<Equipment>>();
            var seen = new HashSet<Set<Equipment>>();
            for (var setup : alternatives) {
                var immutable = immutableEnums(setup, Equipment.class);
                if (!seen.add(immutable)) throw new IllegalArgumentException("Duplicate equipment setup.");
                copy.add(immutable);
            }
            if (copy.size() > 1 && copy.stream().anyMatch(Set::isEmpty)) {
                throw new IllegalArgumentException("An equipment-free setup needs no alternatives.");
            }
            alternatives = Collections.unmodifiableList(copy);
        }

        public boolean equipmentFree() { return alternatives.size() == 1 && alternatives.get(0).isEmpty(); }

        public boolean availableWith(Set<Equipment> available) {
            Objects.requireNonNull(available, "available equipment");
            for (var item : available) {
                if (item == null) throw new IllegalArgumentException("Equipment must be known.");
            }
            return alternatives.stream().anyMatch(available::containsAll);
        }
    }

    public ExerciseMetadata {
        if (exerciseId == null || exerciseId.isEmpty() || !exerciseId.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("A stable exercise identity is required.");
        }
        Objects.requireNonNull(primaryRole, "primary role");
        roles = immutableEnums(roles, Role.class);
        primaryAreas = immutableEnums(primaryAreas, BodyArea.class);
        secondaryAreas = immutableEnums(secondaryAreas, BodyArea.class);
        if (!roles.contains(primaryRole) || primaryAreas.isEmpty()) {
            throw new IllegalArgumentException("Primary role and body areas must be explicit.");
        }
        if (!Collections.disjoint(primaryAreas, secondaryAreas)) {
            throw new IllegalArgumentException("Primary and secondary areas must be distinct.");
        }
        Objects.requireNonNull(suitability, "suitability");
        Objects.requireNonNull(equipment, "equipment");
    }

    private static <E extends Enum<E>> Set<E> immutableEnums(Set<E> values, Class<E> type) {
        if (values == null) {
            throw new IllegalArgumentException("Metadata values must be known.");
        }
        for (E value : values) {
            if (value == null) throw new IllegalArgumentException("Metadata values must be known.");
        }
        var copy = EnumSet.noneOf(type);
        copy.addAll(values);
        return Collections.unmodifiableSet(copy);
    }
}
