package org.racedaydreams.domain

import java.time.LocalDate
import java.util.Locale
import kotlin.test.*
import org.racedaydreams.domain.coaching.*

class CoachingPlatformCompatibilityTest {
    @Test
    fun nativeDecimalAlgorithmMatchesRetainedJdkOneDecimalLabels() {
        val values =
            (-4000..4000).map { it / 80.0 } +
                listOf(
                    Double.NaN,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    -0.0,
                    Double.MIN_VALUE,
                    Double.MAX_VALUE,
                    1e20,
                    1e-20,
                    999.95,
                    9.95,
                )
        for (value in values) assertEquals(
            String.format(Locale.UK, "%.1f", value),
            coachingDecimalOne(value),
            "value=$value",
        )
    }

    @Test
    fun calendarAndSumMatchJdkBoundariesAndCompensation() {
        for (year in listOf(1900, 2000, 2024, 2025, 2026)) for (month in 1..12) {
            val day = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
            assertEquals(day.minusMonths(6), coachingSixMonthsBefore(day))
        }
        val values = listOf(1e16, 1.0, 1.0, -1e16)
        assertEquals(values.stream().mapToDouble { it }.sum(), coachingSum(values))
    }

    @Test
    fun repositoryKeepsExistingRunnableMethodAndJavaOptionalFacade() {
        assertEquals(
            java.lang.Runnable::class.java,
            CoachingRepository::class
                .java
                .getMethod("transaction", java.lang.Runnable::class.java)
                .parameterTypes
                .single(),
        )
        assertEquals(
            java.util.Optional::class.java,
            AdaptiveCoachingEngine::class.java.methods.single { it.name == "review" }.returnType,
        )
    }
}
