package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable

/**
 * Broad original product taxonomy, not measured stimulus, medical advice or set-equivalence
 * weights.
 */
data class ExerciseMetadata(
    private val exerciseId: String?,
    private val primaryRole: Role?,
    private var roles: Set<Role>?,
    private var primaryAreas: Set<BodyArea>?,
    private var secondaryAreas: Set<BodyArea>?,
    private val suitability: Suitability?,
    private val equipment: EquipmentRequirement?,
) {
    enum class Role(private val displayLabel: String) {
        PRIMARY_STRENGTH("Primary strength"),
        SQUAT("Squat"),
        HINGE("Hinge"),
        HIP_EXTENSION("Hip extension"),
        HIP_STABILITY("Hip stability"),
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
        SCAPULAR_CONTROL("Shoulder-blade control"),
        EXTERNAL_ROTATION("Shoulder outward rotation"),
        INTERNAL_ROTATION("Shoulder inward rotation"),
        CONDITIONING("Conditioning"),
        POWER("Power"),
        ISOLATION("Isolation");

        fun label(): String = displayLabel
    }

    enum class BodyArea(private val displayLabel: String) {
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

        fun label(): String = displayLabel
    }

    enum class EquipmentGroup {
        BARBELL,
        DUMBBELLS,
        KETTLEBELLS,
        CABLES,
        MACHINES,
        SMITH_MACHINE,
        PULL_UP_BAR,
        SLED,
        OTHER,
    }

    enum class Equipment(private val equipmentGroup: EquipmentGroup) {
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
        STABLE_CHAIR(EquipmentGroup.OTHER),
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
        HIP_ABDUCTOR_MACHINE(EquipmentGroup.MACHINES),
        HIP_ADDUCTOR_MACHINE(EquipmentGroup.MACHINES),
        REVERSE_HYPER_MACHINE(EquipmentGroup.MACHINES),
        GLUTE_KICKBACK_MACHINE(EquipmentGroup.MACHINES),
        BACK_EXTENSION_BENCH(EquipmentGroup.MACHINES),
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

        fun group(): EquipmentGroup = equipmentGroup
    }

    enum class Suitability {
        BEGINNER_FRIENDLY,
        INTERMEDIATE,
        TECHNICAL;

        fun appropriateFor(experience: GymProgramme.Experience?): Boolean {
            if (experience == null) throw NullPointerException("experience")
            return this == BEGINNER_FRIENDLY ||
                experience == GymProgramme.Experience.EXPERIENCED ||
                this == INTERMEDIATE && experience == GymProgramme.Experience.INTERMEDIATE
        }
    }

    data class EquipmentRequirement(private var alternatives: List<Set<Equipment>>?) {
        init {
            require(alternatives != null && alternatives!!.isNotEmpty()) {
                "At least one complete equipment setup is required."
            }
            val copy = mutableListOf<Set<Equipment>>()
            val seen = mutableSetOf<Set<Equipment>>()
            for (setup in alternatives!!) {
                val snapshot = immutableEnums(setup)
                require(seen.add(snapshot)) { "Duplicate equipment setup." }
                copy.add(snapshot)
            }
            require(copy.size <= 1 || copy.none { it.isEmpty() }) {
                "An equipment-free setup needs no alternatives."
            }
            alternatives = Immutable.copyList(copy)
        }

        fun alternatives(): List<Set<Equipment>> = alternatives!!

        fun equipmentFree(): Boolean = alternatives!!.size == 1 && alternatives!![0].isEmpty()

        fun availableWith(available: Set<Equipment>?): Boolean {
            if (available == null) throw NullPointerException("available equipment")
            for (item in available) if (item == null)
                throw IllegalArgumentException("Equipment must be known.")
            return alternatives!!.any { available.containsAll(it) }
        }

        override fun toString(): String = "EquipmentRequirement[alternatives=$alternatives]"
    }

    init {
        require(
            exerciseId != null &&
                exerciseId.isNotEmpty() &&
                Regex("[a-z0-9]+(?:-[a-z0-9]+)*").matches(exerciseId)
        ) {
            "A stable exercise identity is required."
        }
        if (primaryRole == null) throw NullPointerException("primary role")
        roles = immutableEnums(roles)
        primaryAreas = immutableEnums(primaryAreas)
        secondaryAreas = immutableEnums(secondaryAreas)
        require(primaryRole in roles!! && primaryAreas!!.isNotEmpty()) {
            "Primary role and body areas must be explicit."
        }
        require(primaryAreas!!.none { it in secondaryAreas!! }) {
            "Primary and secondary areas must be distinct."
        }
        if (suitability == null) throw NullPointerException("suitability")
        if (equipment == null) throw NullPointerException("equipment")
    }

    fun exerciseId(): String = exerciseId!!

    fun primaryRole(): Role = primaryRole!!

    fun roles(): Set<Role> = roles!!

    fun primaryAreas(): Set<BodyArea> = primaryAreas!!

    fun secondaryAreas(): Set<BodyArea> = secondaryAreas!!

    fun suitability(): Suitability = suitability!!

    fun equipment(): EquipmentRequirement = equipment!!

    override fun toString(): String =
        "ExerciseMetadata[exerciseId=$exerciseId, primaryRole=$primaryRole, roles=$roles, primaryAreas=$primaryAreas, secondaryAreas=$secondaryAreas, suitability=$suitability, equipment=$equipment]"
}

/** EnumSet's canonical ordinal iteration and read-only snapshot, on either platform. */
internal fun <E : Enum<E>> immutableEnums(values: Set<E>?): Set<E> {
    require(values != null) { "Metadata values must be known." }
    for (value in values) require(value != null) { "Metadata values must be known." }
    val rows = Immutable.copyList(values.sortedBy { it.ordinal })
    return object : AbstractSet<E>() {
        override val size: Int
            get() = rows.size

        override fun iterator(): Iterator<E> = rows.iterator()

        override fun contains(element: E): Boolean = element in rows
    }
}
