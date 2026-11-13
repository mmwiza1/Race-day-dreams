package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*

class ExerciseSearchPolicyTest {
    private val catalogue = ExerciseCatalog.exercises()

    private fun search(query: String?, excluded: Set<String> = emptySet()) =
        ExerciseSearch.search(
            catalogue,
            query,
            setOf("dumbbell-bench-press", "smith-romanian-deadlift"),
            listOf("push-up", "bench-press"),
            excluded,
        )

    @Test
    fun nameRelevanceOutranksPersonalPreferencesAndAliasesStillResolve() {
        val exact = search(" BENCH PRESS ")
        assertEquals("bench-press", exact.first().id())
        assertSame(
            requireNotNull(ExerciseCatalog.definition("bench-press")).exercise(),
            exact.first(),
        )
        assertEquals("romanian-deadlift", search("romanian").first().id())
        assertEquals("trap-bar-deadlift", search("hex bar").first().id())
        assertTrue(search("RDL").any { it.id() == "romanian-deadlift" })
        assertTrue(search("Farmer’s").any { it.id() == "farmers-carry" })
        assertFails { (exact as MutableList<StrengthExercise>).clear() }
    }

    @Test
    fun excludedEvidenceIsOnlyEligibleWhenCallerExplicitlyPermitsItsDiscovery() {
        val excluded = setOf("bench-press", "romanian-deadlift")
        assertTrue(search(null, excluded).none { it.id() in excluded })
        assertTrue(search("bench", excluded).none { it.id() in excluded })
        assertTrue(search("bench").any { it.id() == "bench-press" })
        assertEquals(220, search(null).size)
        assertEquals(220, catalogue.size)
    }

    @Test
    fun sharedNormalizationRetainsRootCaseAsciiTrimAndOriginalWordBoundaries() {
        val exercise =
            StrengthExercise(
                "custom",
                "Alpha\u00a0Bench İSTANBUL",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.BODYWEIGHT,
                false,
            )
        fun found(query: String) =
            ExerciseSearch.search(listOf(exercise), query, emptySet(), emptyList(), emptySet())
        assertEquals(1, found("i\u0307stanbul").size)
        assertEquals(1, found("\talpha\r").size)
        assertTrue(found("\u00a0alpha").isEmpty())
        assertTrue(found("missing").isEmpty())
        val wordBoundary =
            StrengthExercise(
                "word",
                "Alpha\u00a0Bench",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.BODYWEIGHT,
                false,
            )
        val asciiBoundary = wordBoundary.copy(id = "ascii", name = "Alpha-Bench")
        assertEquals(
            listOf("ascii", "word"),
            ExerciseSearch.search(
                    listOf(wordBoundary, asciiBoundary),
                    "bench",
                    setOf("word"),
                    emptyList(),
                    emptySet(),
                )
                .map { it.id() },
        )
    }
}
