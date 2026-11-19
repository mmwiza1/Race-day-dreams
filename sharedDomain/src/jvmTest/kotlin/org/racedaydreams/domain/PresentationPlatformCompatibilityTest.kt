package org.racedaydreams.domain

import java.time.Instant
import java.util.Locale
import kotlin.test.*
import org.racedaydreams.domain.presentation.*

class PresentationPlatformCompatibilityTest {
    @Test
    fun epochMillisMatchesJdkIncludingNegativeFractionsAndOverflowBoundaries() {
        for (seconds in
            listOf(
                Long.MIN_VALUE / 1000 - 1,
                -100000000000L,
                -86400L,
                -1L,
                0L,
                1L,
                86400L,
                Long.MAX_VALUE / 1000,
            )) for (nanos in listOf(0L, 1L, 192000000L, 807000000L, 999999999L)) {
            val instant = Instant.ofEpochSecond(seconds, nanos)
            val original = runCatching { instant.toEpochMilli() }
            if (original.isSuccess)
                assertEquals(original.getOrThrow(), graphMillis(instant), "$seconds/$nanos")
            else assertFailsWith<ArithmeticException> { graphMillis(instant) }
        }
    }

    @Test
    fun retainedJavaFactoryListsRejectMutationAndRawConstructorsKeepAliases() {
        val home = HomeCoachingSummary.from(null)
        @Suppress("UNCHECKED_CAST") val exposed = home.observations() as java.util.List<String>
        assertFailsWith<UnsupportedOperationException> { exposed.add("invented") }
        assertFailsWith<UnsupportedOperationException> {
            exposed.iterator().let {
                it.next()
                it.remove()
            }
        }
    }

    @Test
    fun presentationTextDoesNotChangeWithTheDeviceLocale() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("Intervals", CalendarData.label(WorkoutCategory.INTERVALS))
            assertEquals(
                "Estimated maximum for one repetition",
                AthleteLanguage.recordLabel("Estimated 1RM"),
            )
        } finally {
            Locale.setDefault(before)
        }
    }
}
