package org.racedaydreams.domain.strength;

import static org.racedaydreams.domain.strength.ExerciseMetadata.BodyArea.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Equipment.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Role.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Suitability.*;

import org.racedaydreams.domain.strength.ExerciseMetadata.BodyArea;
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment;
import org.racedaydreams.domain.strength.ExerciseMetadata.EquipmentRequirement;
import org.racedaydreams.domain.strength.ExerciseMetadata.Role;
import org.racedaydreams.domain.strength.ExerciseMetadata.Suitability;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Explicit, versioned product classifications for canonical exercises. Groups share only genuinely
 * common roles and setups. There is no name/category inference or generic fallback. A new canonical
 * exercise without a reviewed entry fails catalogue initialization and the completeness test.
 * Equipment describes the taught canonical setup, not every possible improvised variation.
 */
public final class ExerciseMetadataCatalog {
    public static final int VERSION = 1;
    private static final Map<String, ExerciseMetadata> ENTRIES = build();

    private ExerciseMetadataCatalog() {}

    public static Map<String, ExerciseMetadata> entries() {
        return ENTRIES;
    }

    /**
     * Unknown/custom exercises remain unknown; their user-supplied metadata is a separate concern.
     */
    public static ExerciseMetadata find(String id) {
        return id == null ? null : ENTRIES.get(id);
    }

    public static ExerciseMetadata require(String id) {
        var entry = find(id);
        if (entry == null)
            throw new IllegalArgumentException("No canonical exercise metadata: " + id);
        return entry;
    }

    private static Map<String, ExerciseMetadata> build() {
        var out = new LinkedHashMap<String, ExerciseMetadata>();

        define(
                out,
                "back-squat front-squat high-bar-squat low-bar-squat pause-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                INTERMEDIATE,
                setup(BARBELL, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "box-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                INTERMEDIATE,
                setup(BARBELL, SQUAT_RACK, STABLE_STEP),
                PRIMARY_STRENGTH);
        define(
                out,
                "zercher-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK, UPPER_BACK),
                TECHNICAL,
                setup(BARBELL, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "goblet-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                alternatives(set(DUMBBELLS), set(KETTLEBELLS)),
                PRIMARY_STRENGTH);
        define(
                out,
                "hack-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(HACK_SQUAT_MACHINE),
                PRIMARY_STRENGTH);
        define(
                out,
                "pendulum-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(PENDULUM_SQUAT_MACHINE),
                PRIMARY_STRENGTH);
        define(
                out,
                "belt-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(BELT_SQUAT_MACHINE),
                PRIMARY_STRENGTH);
        define(
                out,
                "smith-machine-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                setup(SMITH_MACHINE),
                PRIMARY_STRENGTH);
        define(
                out,
                "leg-press",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(LEG_PRESS_MACHINE),
                PRIMARY_STRENGTH);
        define(
                out,
                "single-leg-press",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(LEG_PRESS_MACHINE),
                PRIMARY_STRENGTH,
                UNILATERAL);
        define(
                out,
                "leg-extension",
                ISOLATION,
                areas(QUADS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(LEG_EXTENSION_MACHINE));
        define(
                out,
                "sissy-squat",
                SQUAT,
                areas(QUADS),
                areas(TRUNK),
                TECHNICAL,
                setup(),
                ISOLATION);
        define(
                out,
                "bodyweight-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                setup());

        define(
                out,
                "deadlift sumo-deadlift",
                HINGE,
                areas(GLUTES, QUADS),
                areas(HAMSTRINGS, TRUNK, UPPER_BACK),
                INTERMEDIATE,
                setup(BARBELL),
                PRIMARY_STRENGTH);
        define(
                out,
                "trap-bar-deadlift",
                HINGE,
                areas(GLUTES, QUADS),
                areas(HAMSTRINGS, TRUNK, UPPER_BACK),
                INTERMEDIATE,
                setup(TRAP_BAR),
                PRIMARY_STRENGTH);
        define(
                out,
                "romanian-deadlift stiff-leg-deadlift",
                HINGE,
                areas(HAMSTRINGS, GLUTES),
                areas(TRUNK),
                INTERMEDIATE,
                setup(BARBELL),
                PRIMARY_STRENGTH);
        define(
                out,
                "deficit-deadlift",
                HINGE,
                areas(GLUTES, QUADS),
                areas(HAMSTRINGS, TRUNK, UPPER_BACK),
                TECHNICAL,
                setup(BARBELL, DEFICIT_PLATFORM),
                PRIMARY_STRENGTH);
        define(
                out,
                "rack-pull",
                HINGE,
                areas(GLUTES, UPPER_BACK),
                areas(HAMSTRINGS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "block-pull",
                HINGE,
                areas(GLUTES, UPPER_BACK),
                areas(HAMSTRINGS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, LIFTING_BLOCKS),
                PRIMARY_STRENGTH);
        define(
                out,
                "snatch-grip-deadlift",
                HINGE,
                areas(GLUTES, HAMSTRINGS),
                areas(QUADS, UPPER_BACK, TRUNK),
                TECHNICAL,
                setup(BARBELL),
                PRIMARY_STRENGTH);
        define(
                out,
                "single-leg-rdl",
                HINGE,
                areas(HAMSTRINGS, GLUTES),
                areas(TRUNK),
                INTERMEDIATE,
                setup(DUMBBELLS),
                UNILATERAL);
        define(
                out,
                "good-morning",
                HINGE,
                areas(HAMSTRINGS, GLUTES),
                areas(TRUNK),
                TECHNICAL,
                setup(BARBELL, SQUAT_RACK));
        define(
                out,
                "cable-pull-through",
                HINGE,
                areas(GLUTES, HAMSTRINGS),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_ROPE));
        define(
                out,
                "hip-thrust",
                HIP_EXTENSION,
                areas(GLUTES),
                areas(HAMSTRINGS),
                BEGINNER_FRIENDLY,
                alternatives(set(BARBELL, BENCH), set(DUMBBELLS, BENCH)),
                PRIMARY_STRENGTH);
        define(
                out,
                "barbell-glute-bridge",
                HIP_EXTENSION,
                areas(GLUTES),
                areas(HAMSTRINGS),
                BEGINNER_FRIENDLY,
                setup(BARBELL));
        define(
                out,
                "glute-bridge",
                HIP_EXTENSION,
                areas(GLUTES),
                areas(HAMSTRINGS),
                BEGINNER_FRIENDLY,
                setup());

        define(
                out,
                "forward-lunge walking-lunge barbell-lunge lunge",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HAMSTRINGS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL));
        define(
                out,
                "reverse-lunge dumbbell-lunge",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HAMSTRINGS, TRUNK),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS));
        define(
                out,
                "lateral-lunge curtsy-lunge",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HIP_ADDUCTORS, TRUNK),
                INTERMEDIATE,
                setup(DUMBBELLS));
        define(
                out,
                "deficit-reverse-lunge front-foot-elevated-split-squat",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HAMSTRINGS, TRUNK),
                INTERMEDIATE,
                setup(DUMBBELLS, STABLE_STEP));
        define(
                out,
                "split-squat",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HAMSTRINGS, TRUNK),
                INTERMEDIATE,
                alternatives(set(BARBELL, BENCH, SQUAT_RACK), set(DUMBBELLS, BENCH)));
        define(
                out,
                "step-up box-step-up",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HAMSTRINGS, TRUNK),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, STABLE_STEP));
        define(
                out,
                "high-step-up",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(HAMSTRINGS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, STABLE_STEP, SQUAT_RACK));
        define(
                out,
                "cossack-squat",
                UNILATERAL,
                areas(QUADS, GLUTES, HIP_ADDUCTORS),
                areas(TRUNK),
                TECHNICAL,
                setup(BARBELL, SQUAT_RACK),
                SQUAT);
        define(
                out,
                "pistol-squat",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                TECHNICAL,
                setup(),
                SQUAT);
        define(
                out,
                "reverse-lunge-bodyweight",
                UNILATERAL,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                setup());

        define(
                out,
                "lying-leg-curl",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(LYING_LEG_CURL_MACHINE),
                ISOLATION);
        define(
                out,
                "seated-leg-curl",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(SEATED_LEG_CURL_MACHINE),
                ISOLATION);
        define(
                out,
                "standing-leg-curl",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(STANDING_LEG_CURL_MACHINE),
                ISOLATION,
                UNILATERAL);
        define(
                out,
                "nordic-hamstring-curl",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(GLUTES, TRUNK),
                TECHNICAL,
                setup(SECURE_FOOT_ANCHOR));
        define(
                out,
                "glute-ham-raise",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(GLUTES, TRUNK),
                TECHNICAL,
                setup(GLUTE_HAM_DEVELOPER),
                HIP_EXTENSION);
        define(
                out,
                "swiss-ball-leg-curl",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(GLUTES, TRUNK),
                INTERMEDIATE,
                setup(EXERCISE_BALL));
        define(
                out,
                "sliding-leg-curl",
                HAMSTRING,
                areas(HAMSTRINGS),
                areas(GLUTES, TRUNK),
                INTERMEDIATE,
                setup(SLIDERS, SLIDING_SURFACE));
        define(
                out,
                "calf-raise standing-calf-raise",
                CALF,
                areas(CALVES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(STANDING_CALF_MACHINE),
                ISOLATION);
        define(
                out,
                "seated-calf-raise",
                CALF,
                areas(CALVES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(SEATED_CALF_MACHINE),
                ISOLATION);
        define(
                out,
                "leg-press-calf-raise",
                CALF,
                areas(CALVES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(LEG_PRESS_MACHINE),
                ISOLATION);
        define(
                out,
                "single-leg-calf-raise",
                CALF,
                areas(CALVES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(STANDING_CALF_MACHINE),
                ISOLATION,
                UNILATERAL);
        define(
                out,
                "donkey-calf-raise",
                CALF,
                areas(CALVES),
                areas(),
                INTERMEDIATE,
                setup(DONKEY_CALF_MACHINE),
                ISOLATION);
        define(
                out,
                "tibialis-raise",
                CALF,
                areas(LOWER_LEG),
                areas(),
                BEGINNER_FRIENDLY,
                setup(WALL),
                ISOLATION);
        define(
                out,
                "single-leg-calf-raise-bodyweight",
                CALF,
                areas(CALVES),
                areas(),
                BEGINNER_FRIENDLY,
                setup(),
                ISOLATION,
                UNILATERAL);

        define(
                out,
                "bench-press wide-grip-bench-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                INTERMEDIATE,
                setup(BARBELL, BENCH, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "close-grip-bench-press",
                HORIZONTAL_PUSH,
                areas(CHEST, TRICEPS),
                areas(SHOULDERS),
                INTERMEDIATE,
                setup(BARBELL, BENCH, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "incline-bench-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                INTERMEDIATE,
                setup(BARBELL, ADJUSTABLE_BENCH, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "decline-bench-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                INTERMEDIATE,
                setup(BARBELL, DECLINE_BENCH, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "dumbbell-bench-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, BENCH),
                PRIMARY_STRENGTH);
        define(
                out,
                "incline-dumbbell-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, ADJUSTABLE_BENCH),
                PRIMARY_STRENGTH);
        define(
                out,
                "decline-dumbbell-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                INTERMEDIATE,
                setup(DUMBBELLS, DECLINE_BENCH),
                PRIMARY_STRENGTH);
        define(
                out,
                "machine-chest-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(CHEST_PRESS_MACHINE),
                PRIMARY_STRENGTH);
        define(
                out,
                "smith-machine-bench-press",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(SMITH_MACHINE, BENCH),
                PRIMARY_STRENGTH);
        define(
                out,
                "push-up",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS, TRUNK),
                BEGINNER_FRIENDLY,
                setup());
        define(
                out,
                "weighted-push-up",
                HORIZONTAL_PUSH,
                areas(CHEST),
                areas(TRICEPS, SHOULDERS, TRUNK),
                INTERMEDIATE,
                setup(WEIGHTED_VEST));
        define(
                out,
                "dip",
                VERTICAL_PUSH,
                areas(CHEST, TRICEPS),
                areas(SHOULDERS),
                INTERMEDIATE,
                setup(PARALLEL_BARS));
        define(
                out,
                "weighted-dip",
                VERTICAL_PUSH,
                areas(CHEST, TRICEPS),
                areas(SHOULDERS),
                TECHNICAL,
                setup(PARALLEL_BARS, DIP_BELT, WEIGHT_PLATE));
        define(
                out,
                "cable-fly",
                ISOLATION,
                areas(CHEST),
                areas(SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE));
        define(
                out,
                "pec-deck",
                ISOLATION,
                areas(CHEST),
                areas(SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(PEC_DECK_MACHINE));
        define(
                out,
                "dumbbell-fly",
                ISOLATION,
                areas(CHEST),
                areas(SHOULDERS),
                INTERMEDIATE,
                setup(DUMBBELLS, BENCH));

        define(
                out,
                "row barbell-row pendlay-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL),
                PRIMARY_STRENGTH);
        define(
                out,
                "dumbbell-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS, TRUNK),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, BENCH),
                UNILATERAL);
        define(
                out,
                "chest-supported-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS),
                BEGINNER_FRIENDLY,
                setup(BARBELL, ADJUSTABLE_BENCH));
        define(
                out,
                "t-bar-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, LANDMINE_ANCHOR, CABLE_HANDLE));
        define(
                out,
                "machine-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS),
                BEGINNER_FRIENDLY,
                setup(ROW_MACHINE));
        define(
                out,
                "seated-cable-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS, TRUNK),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE));
        define(
                out,
                "meadows-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, LANDMINE_ANCHOR),
                UNILATERAL);
        define(
                out,
                "inverted-row",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, LATS),
                areas(BICEPS, TRUNK),
                BEGINNER_FRIENDLY,
                setup(SECURE_LOW_BAR));
        define(
                out,
                "pull-up chin-up",
                VERTICAL_PULL,
                areas(LATS, UPPER_BACK),
                areas(BICEPS, FOREARMS),
                INTERMEDIATE,
                setup(PULL_UP_BAR),
                PRIMARY_STRENGTH);
        define(
                out,
                "neutral-grip-pull-up",
                VERTICAL_PULL,
                areas(LATS, UPPER_BACK),
                areas(BICEPS, FOREARMS),
                INTERMEDIATE,
                setup(NEUTRAL_GRIP_BAR),
                PRIMARY_STRENGTH);
        define(
                out,
                "weighted-pull-up weighted-chin-up",
                VERTICAL_PULL,
                areas(LATS, UPPER_BACK),
                areas(BICEPS, FOREARMS),
                TECHNICAL,
                setup(PULL_UP_BAR, DIP_BELT, WEIGHT_PLATE),
                PRIMARY_STRENGTH);
        define(
                out,
                "lat-pulldown close-grip-pulldown",
                VERTICAL_PULL,
                areas(LATS, UPPER_BACK),
                areas(BICEPS),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE, PULLDOWN_SEAT_AND_PAD));
        define(
                out,
                "straight-arm-pulldown",
                VERTICAL_PULL,
                areas(LATS),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE),
                ISOLATION);
        define(
                out,
                "face-pull",
                HORIZONTAL_PULL,
                areas(UPPER_BACK, SHOULDERS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_ROPE),
                ISOLATION);

        define(
                out,
                "overhead-press",
                VERTICAL_PUSH,
                areas(SHOULDERS),
                areas(TRICEPS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "push-press",
                POWER,
                areas(SHOULDERS, QUADS, GLUTES),
                areas(TRICEPS, TRUNK),
                INTERMEDIATE,
                setup(BARBELL, SQUAT_RACK),
                VERTICAL_PUSH);
        define(
                out,
                "seated-barbell-press",
                VERTICAL_PUSH,
                areas(SHOULDERS),
                areas(TRICEPS),
                INTERMEDIATE,
                setup(BARBELL, ADJUSTABLE_BENCH, SQUAT_RACK),
                PRIMARY_STRENGTH);
        define(
                out,
                "dumbbell-shoulder-press arnold-press",
                VERTICAL_PUSH,
                areas(SHOULDERS),
                areas(TRICEPS),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, ADJUSTABLE_BENCH));
        define(
                out,
                "machine-shoulder-press",
                VERTICAL_PUSH,
                areas(SHOULDERS),
                areas(TRICEPS),
                BEGINNER_FRIENDLY,
                setup(SHOULDER_PRESS_MACHINE));
        define(
                out,
                "landmine-press",
                VERTICAL_PUSH,
                areas(SHOULDERS, CHEST),
                areas(TRICEPS, TRUNK),
                BEGINNER_FRIENDLY,
                setup(BARBELL, LANDMINE_ANCHOR));
        define(
                out,
                "lateral-raise front-raise",
                ISOLATION,
                areas(SHOULDERS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS));
        define(
                out,
                "cable-lateral-raise",
                ISOLATION,
                areas(SHOULDERS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE),
                UNILATERAL);
        define(
                out,
                "rear-delt-fly",
                ISOLATION,
                areas(SHOULDERS),
                areas(UPPER_BACK),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS));
        define(
                out,
                "reverse-pec-deck",
                ISOLATION,
                areas(SHOULDERS),
                areas(UPPER_BACK),
                BEGINNER_FRIENDLY,
                setup(REVERSE_PEC_DECK_MACHINE));
        define(
                out,
                "upright-row",
                ISOLATION,
                areas(SHOULDERS),
                areas(UPPER_BACK),
                INTERMEDIATE,
                setup(DUMBBELLS));

        define(
                out,
                "barbell-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                setup(BARBELL));
        define(
                out,
                "ez-bar-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                setup(EZ_BAR));
        define(
                out,
                "dumbbell-curl hammer-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS));
        define(
                out,
                "incline-dumbbell-curl spider-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, ADJUSTABLE_BENCH));
        define(
                out,
                "preacher-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                alternatives(set(BARBELL, PREACHER_PAD), set(EZ_BAR, PREACHER_PAD)));
        define(
                out,
                "cable-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE));
        define(
                out,
                "concentration-curl",
                ISOLATION,
                areas(BICEPS),
                areas(FOREARMS),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS, BENCH));
        define(
                out,
                "triceps-pressdown overhead-cable-extension",
                ISOLATION,
                areas(TRICEPS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE));
        define(
                out,
                "rope-pressdown",
                ISOLATION,
                areas(TRICEPS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_ROPE));
        define(
                out,
                "skull-crusher",
                ISOLATION,
                areas(TRICEPS),
                areas(),
                INTERMEDIATE,
                setup(BARBELL, BENCH));
        define(
                out,
                "dumbbell-overhead-extension",
                ISOLATION,
                areas(TRICEPS),
                areas(),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS));
        define(
                out,
                "bench-dip",
                VERTICAL_PUSH,
                areas(TRICEPS),
                areas(CHEST, SHOULDERS),
                INTERMEDIATE,
                setup(BENCH));
        define(
                out,
                "jm-press",
                HORIZONTAL_PUSH,
                areas(TRICEPS),
                areas(CHEST, SHOULDERS),
                TECHNICAL,
                setup(BARBELL, BENCH, SQUAT_RACK));

        define(
                out,
                "power-clean clean hang-clean power-snatch snatch hang-snatch high-pull",
                POWER,
                areas(GLUTES, QUADS, UPPER_BACK),
                areas(HAMSTRINGS, TRUNK, SHOULDERS),
                TECHNICAL,
                setup(BARBELL));
        define(
                out,
                "clean-jerk",
                POWER,
                areas(GLUTES, QUADS, SHOULDERS),
                areas(HAMSTRINGS, UPPER_BACK, TRUNK, TRICEPS),
                TECHNICAL,
                setup(BARBELL),
                VERTICAL_PUSH);
        define(
                out,
                "push-jerk split-jerk",
                POWER,
                areas(GLUTES, QUADS, SHOULDERS),
                areas(TRUNK, TRICEPS),
                TECHNICAL,
                setup(BARBELL, SQUAT_RACK),
                VERTICAL_PUSH);
        define(
                out,
                "kettlebell-swing",
                POWER,
                areas(GLUTES, HAMSTRINGS),
                areas(TRUNK, UPPER_BACK),
                INTERMEDIATE,
                setup(KETTLEBELLS),
                HINGE);
        define(
                out,
                "american-kettlebell-swing",
                POWER,
                areas(GLUTES, HAMSTRINGS),
                areas(TRUNK, SHOULDERS, UPPER_BACK),
                TECHNICAL,
                setup(KETTLEBELLS),
                HINGE);
        define(
                out,
                "kettlebell-clean",
                POWER,
                areas(GLUTES, HAMSTRINGS),
                areas(UPPER_BACK, TRUNK),
                INTERMEDIATE,
                setup(KETTLEBELLS),
                HINGE,
                UNILATERAL);
        define(
                out,
                "kettlebell-snatch",
                POWER,
                areas(GLUTES, HAMSTRINGS),
                areas(SHOULDERS, UPPER_BACK, TRUNK),
                TECHNICAL,
                setup(KETTLEBELLS),
                HINGE,
                UNILATERAL);
        define(
                out,
                "kettlebell-goblet-squat",
                SQUAT,
                areas(QUADS, GLUTES),
                areas(TRUNK),
                BEGINNER_FRIENDLY,
                setup(KETTLEBELLS),
                PRIMARY_STRENGTH);
        define(
                out,
                "thruster",
                POWER,
                areas(QUADS, GLUTES, SHOULDERS),
                areas(TRICEPS, TRUNK),
                INTERMEDIATE,
                setup(DUMBBELLS),
                SQUAT,
                VERTICAL_PUSH);
        define(
                out,
                "wall-ball",
                POWER,
                areas(QUADS, GLUTES, SHOULDERS),
                areas(TRICEPS, TRUNK),
                INTERMEDIATE,
                setup(MEDICINE_BALL, WALL),
                SQUAT,
                VERTICAL_PUSH);
        define(
                out,
                "devil-press",
                POWER,
                areas(GLUTES, HAMSTRINGS, SHOULDERS),
                areas(CHEST, TRICEPS, TRUNK),
                TECHNICAL,
                setup(DUMBBELLS),
                HINGE,
                HORIZONTAL_PUSH);
        define(
                out,
                "dumbbell-snatch",
                POWER,
                areas(GLUTES, QUADS, SHOULDERS),
                areas(HAMSTRINGS, UPPER_BACK, TRUNK),
                TECHNICAL,
                setup(DUMBBELLS),
                HINGE,
                UNILATERAL);
        define(
                out,
                "dumbbell-clean-jerk",
                POWER,
                areas(GLUTES, QUADS, SHOULDERS),
                areas(TRICEPS, UPPER_BACK, TRUNK),
                TECHNICAL,
                setup(DUMBBELLS),
                VERTICAL_PUSH);
        define(
                out,
                "farmers-carry suitcase-carry",
                CARRY,
                areas(FOREARMS, TRUNK),
                areas(UPPER_BACK),
                BEGINNER_FRIENDLY,
                setup(DUMBBELLS));
        addRole(out, "suitcase-carry", UNILATERAL);
        define(
                out,
                "front-rack-carry",
                CARRY,
                areas(TRUNK, UPPER_BACK),
                areas(FOREARMS, SHOULDERS),
                INTERMEDIATE,
                setup(KETTLEBELLS));
        define(
                out,
                "sled-push",
                CONDITIONING,
                areas(QUADS, GLUTES),
                areas(CALVES, TRUNK, SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(SLED));
        define(
                out,
                "sled-pull",
                CONDITIONING,
                areas(QUADS, GLUTES),
                areas(UPPER_BACK, TRUNK),
                BEGINNER_FRIENDLY,
                setup(SLED, SLED_PULL_STRAP));
        define(
                out,
                "sandbag-carry",
                CARRY,
                areas(TRUNK, UPPER_BACK),
                areas(FOREARMS, BICEPS),
                INTERMEDIATE,
                setup(SANDBAG));
        define(
                out,
                "sandbag-clean",
                POWER,
                areas(GLUTES, QUADS, UPPER_BACK),
                areas(HAMSTRINGS, TRUNK, BICEPS),
                TECHNICAL,
                setup(SANDBAG),
                HINGE);

        define(
                out,
                "plank side-plank dead-bug bird-dog",
                CORE,
                areas(TRUNK),
                areas(),
                BEGINNER_FRIENDLY,
                setup());
        define(
                out,
                "weighted-plank weighted-side-plank",
                CORE,
                areas(TRUNK),
                areas(),
                INTERMEDIATE,
                setup(WEIGHT_PLATE));
        define(
                out,
                "hanging-knee-raise hanging-leg-raise",
                CORE,
                areas(TRUNK, HIP_FLEXORS),
                areas(FOREARMS),
                INTERMEDIATE,
                setup(PULL_UP_BAR));
        define(
                out,
                "toes-to-bar",
                CORE,
                areas(TRUNK, HIP_FLEXORS),
                areas(FOREARMS, LATS),
                TECHNICAL,
                setup(PULL_UP_BAR));
        define(
                out,
                "ab-wheel",
                CORE,
                areas(TRUNK),
                areas(SHOULDERS, LATS),
                INTERMEDIATE,
                setup(AB_WHEEL));
        define(
                out,
                "cable-crunch pallof-press",
                CORE,
                areas(TRUNK),
                areas(),
                BEGINNER_FRIENDLY,
                setup(CABLE_STATION, CABLE_HANDLE));
        define(
                out,
                "russian-twist",
                CORE,
                areas(TRUNK),
                areas(),
                INTERMEDIATE,
                setup(MEDICINE_BALL));
        define(
                out,
                "weighted-sit-up",
                CORE,
                areas(TRUNK),
                areas(HIP_FLEXORS),
                INTERMEDIATE,
                setup(WEIGHT_PLATE));
        define(
                out,
                "ghd-sit-up",
                CORE,
                areas(TRUNK, HIP_FLEXORS),
                areas(),
                TECHNICAL,
                setup(GLUTE_HAM_DEVELOPER));
        define(
                out,
                "dead-hang",
                GRIP,
                areas(FOREARMS),
                areas(SHOULDERS),
                BEGINNER_FRIENDLY,
                setup(PULL_UP_BAR));
        define(out, "wall-sit", SQUAT, areas(QUADS), areas(GLUTES), BEGINNER_FRIENDLY, setup(WALL));
        define(
                out,
                "weighted-wall-sit",
                SQUAT,
                areas(QUADS),
                areas(GLUTES),
                BEGINNER_FRIENDLY,
                setup(WALL, WEIGHT_PLATE));

        var canonicalIds = new java.util.HashSet<String>();
        for (var definition : ExerciseCatalog.definitions())
            canonicalIds.add(definition.exercise().id());
        if (!canonicalIds.equals(out.keySet())) {
            var missing = new java.util.TreeSet<>(canonicalIds);
            missing.removeAll(out.keySet());
            var extra = new java.util.TreeSet<>(out.keySet());
            extra.removeAll(canonicalIds);
            throw new IllegalStateException(
                    "Exercise metadata mismatch; missing=" + missing + ", extra=" + extra);
        }
        return Collections.unmodifiableMap(out);
    }

    private static void define(
            Map<String, ExerciseMetadata> out,
            String identities,
            Role primaryRole,
            Set<BodyArea> primary,
            Set<BodyArea> secondary,
            Suitability suitability,
            EquipmentRequirement equipment,
            Role... otherRoles) {
        var roles = EnumSet.of(primaryRole, otherRoles);
        for (String id : identities.split(" ")) {
            var previous =
                    out.put(
                            id,
                            new ExerciseMetadata(
                                    id,
                                    primaryRole,
                                    roles,
                                    primary,
                                    secondary,
                                    suitability,
                                    equipment));
            if (previous != null)
                throw new IllegalStateException("Duplicate exercise metadata: " + id);
        }
    }

    private static void addRole(Map<String, ExerciseMetadata> out, String id, Role role) {
        var value = out.get(id);
        var roles = EnumSet.copyOf(value.roles());
        roles.add(role);
        out.put(
                id,
                new ExerciseMetadata(
                        id,
                        value.primaryRole(),
                        roles,
                        value.primaryAreas(),
                        value.secondaryAreas(),
                        value.suitability(),
                        value.equipment()));
    }

    private static Set<BodyArea> areas(BodyArea... values) {
        var out = EnumSet.noneOf(BodyArea.class);
        out.addAll(Arrays.asList(values));
        return out;
    }

    private static Set<Equipment> set(Equipment... values) {
        var out = EnumSet.noneOf(Equipment.class);
        out.addAll(Arrays.asList(values));
        return out;
    }

    private static EquipmentRequirement setup(Equipment... values) {
        return new EquipmentRequirement(Collections.singletonList(set(values)));
    }

    @SafeVarargs
    private static EquipmentRequirement alternatives(Set<Equipment>... setups) {
        return new EquipmentRequirement(Arrays.asList(setups));
    }
}
