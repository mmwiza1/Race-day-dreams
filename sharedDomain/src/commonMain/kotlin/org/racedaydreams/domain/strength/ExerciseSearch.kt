package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable

/** Original relevance order. Personal preferences break ties without burying exact matches. */
object ExerciseSearch {
    @JvmStatic
    fun search(
        exercises: List<StrengthExercise>,
        query: String?,
        favourites: Set<String>,
        recent: List<String>,
        archived: Set<String>,
    ): List<StrengthExercise> {
        val q = normal(query)
        val found = exercises.filter { it.id() !in archived && rank(it, q) < 6 }
        return Immutable.copyList(
            found.sortedWith(
                compareBy<StrengthExercise> { rank(it, q) }
                    .thenBy { if (it.id() in favourites) 0 else 1 }
                    .thenBy {
                        recent.indexOf(it.id()).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
                    }
                    .thenBy { if (it.estimateOneRepMax()) 0 else 1 }
                    .thenBy { it.name() }
                    .thenBy { it.id() }
            )
        )
    }

    private fun normal(value: String?): String =
        value?.trim { it <= ' ' }?.lowercase()?.replace('’', '\'') ?: ""

    private fun rank(exercise: StrengthExercise, query: String): Int {
        val name = normal(exercise.name())
        if (query.isEmpty() || name == query) return 0
        if (name.startsWith(query)) return 1
        // Java's original [\\s-]+ uses ASCII whitespace, not Native ICU's wider \\s set.
        if (name.split(' ', '-', '\t', '\n', '\u000B', '\u000C', '\r').any { it.startsWith(query) })
            return 2
        if (name.contains(query)) return 3
        val definition = ExerciseCatalog.definition(exercise.id())
        if (definition != null) {
            if (definition.aliases().any { normal(it).contains(query) }) return 4
            if (normal(definition.category()).contains(query)) return 5
        }
        return 6
    }
}
