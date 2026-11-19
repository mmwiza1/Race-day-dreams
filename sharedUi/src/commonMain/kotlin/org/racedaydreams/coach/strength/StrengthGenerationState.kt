package org.racedaydreams.coach.strength

import org.racedaydreams.domain.strength.*

data class ProgrammeSwapReview(
    val draftId: String,
    val draftRevision: Int,
    val workoutId: String,
    val slotId: String,
    val original: StrengthExercise,
    val reason: ExerciseSubstitutionEngine.Reason,
    val suggestions: List<ExerciseSubstitutionEngine.Suggestion> = emptyList(),
)

/** Exact concrete generation/preview ownership; durable drafts contain dates, not UI timers. */
data class StrengthGenerationState(
    val busy: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val draft: ConcreteStrengthProgrammeDraft? = null,
    val accepted: Boolean = false,
    val superseded: Boolean = false,
    val baseProgramme: StrengthProgramme? = null,
    val swapReview: ProgrammeSwapReview? = null,
    val progressionReview: ProgrammeProgressionReview? = null,
    val protectedWorkoutIds: Set<String> = emptySet(),
    val saved: List<ConcreteStrengthProgrammeDraftRepository.Summary> = emptyList(),
    val page: Int = 0,
    val more: Boolean = false,
    val current: StrengthProgrammingRepository.Reference? = null,
    val historyOpen: Boolean = false,
    val historyPage: Int = 0,
    val history: List<StrengthProgrammingRepository.Summary> = emptyList(),
    val historyMore: Boolean = false,
    val historicalProgramme: StrengthProgramme? = null,
    val exercises: List<StrengthExercise> = emptyList(),
    val favourites: Set<String> = emptySet(),
    val excluded: Set<String> = emptySet(),
    val archived: Set<String> = emptySet(),
)
