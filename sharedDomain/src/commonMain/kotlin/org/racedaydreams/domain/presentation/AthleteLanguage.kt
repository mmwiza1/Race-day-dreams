package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.strength.StrengthExercise

/** Original display-only explanations; no target, evidence or training decision changes. */
object AthleteLanguage {
    @JvmStatic
    fun measurement(exercise: StrengthExercise): String {
        val weight =
            if (exercise.mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT)
                "the extra weight you add"
            else "the weight you use"
        return when (exercise.measurement()) {
            StrengthExercise.MeasurementProfile.LOAD_REPS ->
                "Record $weight and how many repetitions you complete."
            StrengthExercise.MeasurementProfile.REPS_ONLY ->
                "Record how many repetitions you complete. One repetition is one complete movement."
            StrengthExercise.MeasurementProfile.DURATION ->
                "Record how long you hold or perform the exercise."
            StrengthExercise.MeasurementProfile.LOAD_DURATION ->
                "Record $weight and how long you perform the exercise."
            StrengthExercise.MeasurementProfile.DISTANCE ->
                "Record how far you travel during the exercise."
            StrengthExercise.MeasurementProfile.LOAD_DISTANCE ->
                "Record $weight and how far you move it."
        }
    }

    @JvmStatic
    fun recordLabel(key: String): String {
        if (key == "Estimated 1RM") return "Estimated maximum for one repetition"
        if (key.startsWith("Load × ")) {
            val repetitions = key.substring(7)
            return "Best weight for $repetitions" +
                if (repetitions == "1") " repetition" else " repetitions"
        }
        if (key == "Bodyweight reps") return "Most bodyweight repetitions"
        return key
    }

    @JvmStatic
    fun runningPurpose(category: WorkoutCategory): String =
        when (category) {
            WorkoutCategory.EASY_RUN,
            WorkoutCategory.ZONE_2_RUN ->
                "Run at an easy, comfortable effort. You should be able to hold a conversation. Follow any personal targets shown below; no heart-rate zone is assumed."
            WorkoutCategory.RECOVERY_RUN ->
                "Keep this run gentle. It is intended to fit between more demanding training days."
            WorkoutCategory.STEADY_RUN ->
                "Keep a controlled, steady effort. Follow the planned sections rather than turning this into a race."
            WorkoutCategory.LONG_RUN ->
                "Spend longer on your feet at the planned effort. Follow the sections below and adjust to conditions."
            WorkoutCategory.PROGRESSION_RUN ->
                "Build the effort gradually through the planned sections, rather than starting fast."
            WorkoutCategory.TEMPO_THRESHOLD_RUN ->
                "The main section is a sustained, challenging effort. Keep it controlled and follow the planned easier sections too."
            WorkoutCategory.INTERVALS ->
                "Alternate the harder sections with the planned recovery sections. Recovery is part of the workout."
            WorkoutCategory.RACE_PACE_RUN ->
                "Practise the planned race effort for the sections shown. Only use a pace target when one is provided."
            WorkoutCategory.TIME_TRIAL ->
                "This is a timed effort to review your current performance. Follow the prescribed effort; a maximal effort is not assumed."
            WorkoutCategory.RACE ->
                "Choose how to approach your race. Any targets are guidance, not a guaranteed result."
            WorkoutCategory.CUSTOM_RUN ->
                "Follow the sections you chose. Record what you actually do, even if you change the workout."
        }

    const val WORKING_SETS =
        "Working sets are the main sets you record, excluding warm-up practice sets. A set is a group of repetitions or one timed or distance-based effort."
    const val GYM_TERMS =
        "One repetition is one complete movement. A set is a group of repetitions, or one timed hold or distance-based effort. Warm-up sets are practice before the main work. Record only what you actually complete."
    const val BACKUP =
        "This JSON file saves your app history, goals, programmes, gym sets, results, shoes and settings. It does not include detailed GPS routes or sensor samples. Use Complete backup in Record a run to save app history together with the detailed recordings still on this device. Imported Health Connect records are not"
}
