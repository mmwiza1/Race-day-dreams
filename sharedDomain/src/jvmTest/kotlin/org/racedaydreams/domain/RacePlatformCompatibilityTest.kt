package org.racedaydreams.domain

import java.util.Locale
import kotlin.test.*
import org.racedaydreams.domain.activity.ResultSource
import org.racedaydreams.domain.race.*

class RacePlatformCompatibilityTest {
    @Test
    fun originalAndroidSourceCasingKeepsTheActualPlatformLocale() {
        val previous = Locale.getDefault()
        try {
            for (locale in listOf(Locale.UK, Locale.forLanguageTag("tr-TR"))) {
                Locale.setDefault(locale)
                for (source in ResultSource.entries) assertEquals(
                    source.name.lowercase(locale).replace('_', ' '),
                    raceSourceText(source),
                )
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun originalStageListAndPredictionSnapshotOwnershipArePreserved() {
        @Suppress("UNCHECKED_CAST")
        val stages =
            RaceStrategyEngine.create(5000.0, 1800, false)
                as java.util.List<RaceStrategyEngine.Stage>
        val original = stages[0]
        stages.set(0, original)
        assertEquals(original, stages[0])
        assertFailsWith<UnsupportedOperationException> { stages.add(original) }
        val evidence = mutableListOf("raw")
        val raw = RacePredictionEngine.Prediction(null, null, null, evidence)
        evidence.add("retained alias")
        assertSame(evidence, raw.reasons())
        assertEquals(2, raw.reasons()!!.size)
        @Suppress("UNCHECKED_CAST")
        val frozen =
            RacePredictionEngine.predict(
                    5000.0,
                    emptyList(),
                    emptyList(),
                    java.time.LocalDate.of(2026, 11, 15),
                )
                .reasons() as java.util.List<String>
        assertFailsWith<UnsupportedOperationException> { frozen.clear() }
    }
}
