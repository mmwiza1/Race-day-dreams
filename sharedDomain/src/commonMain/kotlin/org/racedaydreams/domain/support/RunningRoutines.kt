package org.racedaydreams.domain.support

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.coaching.WorkoutCategories

/** Original short supportive routines; never calendar workouts, load or adherence targets. */
object RunningRoutines {
    data class Movement(
        private val id: String?,
        private val title: String?,
        private val dose: String?,
        private val instructions: String?,
        private val cue: String?,
        private val mistake: String?,
        private val pose: String?,
    ) {
        fun id(): String? = id

        fun title(): String? = title

        fun dose(): String? = dose

        fun instructions(): String? = instructions

        fun cue(): String? = cue

        fun mistake(): String? = mistake

        fun pose(): String? = pose

        override fun toString(): String =
            "Movement[id=$id, title=$title, dose=$dose, instructions=$instructions, cue=$cue, mistake=$mistake, pose=$pose]"
    }

    data class Routine(
        private val id: String?,
        private val title: String?,
        private val purpose: String?,
        private val minutes: Int,
        private val movements: List<Movement>?,
    ) {
        fun id(): String? = id

        fun title(): String? = title

        fun purpose(): String? = purpose

        fun minutes(): Int = minutes

        fun movements(): List<Movement>? = movements

        override fun toString(): String =
            "Routine[id=$id, title=$title, purpose=$purpose, minutes=$minutes, movements=$movements]"
    }

    @JvmField
    val MOVEMENTS: List<Movement> =
        Immutable.list(
            Movement(
                "jog",
                "Easy walk or jog",
                "2–5 min",
                "Start comfortably and gradually settle into conversational movement.",
                "Relax your shoulders.",
                "Starting hard before you feel prepared.",
                "walk",
            ),
            Movement(
                "ankle",
                "Ankle rocks",
                "8 each side",
                "With one foot ahead, gently move your knee forward over the foot; keep the heel down.",
                "Use a comfortable range.",
                "Forcing the heel up or pushing into pain.",
                "lunge",
            ),
            Movement(
                "circles",
                "Ankle circles",
                "6 each direction",
                "With support if needed, lift one foot slightly and circle the ankle slowly.",
                "Move the ankle rather than your whole leg.",
                "Rushing or forcing range.",
                "stand",
            ),
            Movement(
                "calf",
                "Calf raises",
                "8–10",
                "Hold support, rise onto the forefoot and lower under control.",
                "Keep pressure comfortable across the forefoot.",
                "Bouncing or losing balance.",
                "calf",
            ),
            Movement(
                "swing",
                "Supported leg swings",
                "8 each side",
                "Hold steady support and swing one leg gently forward and back.",
                "Keep the trunk steady.",
                "Swinging forcefully or twisting.",
                "swing",
            ),
            Movement(
                "lunge",
                "Walking lunges",
                "6 each side",
                "Step forward, lower the back knee comfortably, then stand and change sides.",
                "Keep your torso upright.",
                "Taking an unstable step or forcing depth.",
                "lunge",
            ),
            Movement(
                "march",
                "Controlled marching",
                "30 sec",
                "March with gentle alternating knee lifts and natural arm movement.",
                "Build rhythm, not height.",
                "Leaning backwards.",
                "march",
            ),
            Movement(
                "stride",
                "Controlled strides",
                "3 × 15 sec, easy walk between",
                "On a clear flat space, gradually accelerate to relaxed brisk running, then ease down. Recover fully.",
                "Smooth rather than maximal.",
                "Sprinting or doing strides when uncomfortable.",
                "walk",
            ),
            Movement(
                "bridge",
                "Glute bridges",
                "8",
                "Lie on your back with knees bent and feet planted. Gently lift hips, pause, then lower.",
                "Use a comfortable hip extension.",
                "Overarching the lower back.",
                "bridge",
            ),
            Movement(
                "hip",
                "Hip-flexor stretch",
                "20 sec each side",
                "From a supported split stance or half-kneeling position, gently shift forward.",
                "Keep the pelvis and trunk steady.",
                "Forcing into discomfort or arching.",
                "lunge",
            ),
            Movement(
                "glute",
                "Seated glute stretch",
                "20 sec each side",
                "Sit securely, place an ankle across the opposite thigh if comfortable and lean forward slightly.",
                "Use a gentle stretch.",
                "Pressing the knee down forcefully.",
                "sit",
            ),
            Movement(
                "calf-stretch",
                "Calf stretch",
                "20 sec each side",
                "Place one foot behind, heel down, and lean gently toward a wall or support.",
                "Keep the back foot comfortably forward.",
                "Bouncing or forcing range.",
                "lunge",
            ),
            Movement(
                "quad",
                "Supported quad stretch",
                "20 sec each side",
                "Hold support. Bend one knee and hold the ankle only if comfortably reachable.",
                "Stay balanced; use less bend if needed.",
                "Pulling hard or twisting the knee.",
                "quad",
            ),
            Movement(
                "walk",
                "Easy walk",
                "2–3 min",
                "Gradually ease down to comfortable walking.",
                "Let breathing settle naturally.",
                "Treating this as compulsory recovery treatment.",
                "walk",
            ),
        )

    private fun routine(
        id: String,
        title: String,
        purpose: String,
        minutes: Int,
        vararg ids: String,
    ): Routine =
        Routine(
            id,
            title,
            purpose,
            minutes,
            Immutable.copyList(ids.map { key -> MOVEMENTS.first { it.id() == key } }),
        )

    @JvmStatic
    fun all(): List<Routine> =
        fixedSize(
            routine(
                "warm-easy",
                "Easy-run preparation",
                "Short, optional comfortable preparation.",
                3,
                "jog",
                "ankle",
            ),
            routine(
                "warm-quality",
                "Quality-session warm-up",
                "Dynamic preparation before demanding running. Count the easy jog within any prescribed warm-up, rather than adding mileage.",
                8,
                "jog",
                "ankle",
                "swing",
                "march",
                "stride",
            ),
            routine(
                "warm-long",
                "Long-run preparation",
                "Short preparation without unnecessary expenditure.",
                5,
                "jog",
                "ankle",
                "calf",
            ),
            routine(
                "warm-race-short",
                "5K / 10K race warm-up",
                "Comfortable dynamic preparation; strides only if familiar and comfortable.",
                10,
                "jog",
                "swing",
                "march",
                "stride",
            ),
            routine(
                "warm-race-half",
                "Half-marathon warm-up",
                "Moderate preparation, conserving energy.",
                6,
                "jog",
                "ankle",
                "march",
            ),
            routine(
                "warm-race-marathon",
                "Marathon preparation",
                "Brief preparation; conserve energy for the distance.",
                3,
                "walk",
                "ankle",
            ),
            routine(
                "cool-easy",
                "Optional easy-run cool-down",
                "A short comfortable finish.",
                3,
                "walk",
            ),
            routine(
                "cool-demanding",
                "After demanding running",
                "Optional short walk and gentle stretches; not a guarantee of recovery.",
                6,
                "walk",
                "calf-stretch",
                "hip",
                "glute",
            ),
            routine(
                "mobility-five",
                "5-minute runner mobility",
                "A flexible way to support comfortable movement.",
                5,
                "ankle",
                "swing",
                "bridge",
                "hip",
            ),
            routine(
                "mobility-ten",
                "10-minute runner mobility",
                "Short general movement practice.",
                10,
                "march",
                "circles",
                "swing",
                "lunge",
                "bridge",
                "calf-stretch",
                "hip",
            ),
            routine(
                "hips",
                "Hips & glutes",
                "May help with ordinary stiffness; not treatment.",
                8,
                "march",
                "lunge",
                "bridge",
                "hip",
                "glute",
            ),
            routine(
                "ankles",
                "Calves & ankles",
                "Gentle ankle and calf movement.",
                6,
                "circles",
                "ankle",
                "calf",
                "calf-stretch",
            ),
            routine(
                "post-long",
                "Post-long-run mobility",
                "Optional gentle movement after a long run.",
                8,
                "walk",
                "calf-stretch",
                "quad",
                "hip",
                "glute",
            ),
        )

    @JvmStatic
    fun isMobility(id: String?): Boolean =
        listOf("mobility-five", "mobility-ten", "hips", "ankles", "post-long").contains(id)

    @JvmStatic
    fun get(id: String?): Routine =
        all().firstOrNull { it.id() == id } ?: throw IllegalArgumentException("Unknown routine")

    @JvmStatic
    fun warmup(category: WorkoutCategory?, raceKm: Double): String {
        if (category == WorkoutCategory.RACE)
            return if (raceKm >= 40) "warm-race-marathon"
            else if (raceKm >= 20) "warm-race-half" else "warm-race-short"
        if (category == WorkoutCategory.LONG_RUN) return "warm-long"
        return if (WorkoutCategories.isQuality(category)) "warm-quality" else "warm-easy"
    }

    /**
     * Original Arrays.asList contract: per-call element replacement, no resizing or global
     * mutation.
     */
    private fun <T> fixedSize(vararg values: T): List<T> =
        object : AbstractMutableList<T>() {
            private val rows = values.toMutableList()
            override val size: Int
                get() = rows.size

            override fun get(index: Int): T = rows[index]

            override fun set(index: Int, element: T): T {
                val old = rows[index]
                rows[index] = element
                return old
            }

            override fun add(index: Int, element: T): Unit = throw UnsupportedOperationException()

            override fun removeAt(index: Int): T = throw UnsupportedOperationException()
        }
}
