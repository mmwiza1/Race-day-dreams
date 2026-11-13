package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymProgramme.*
import org.racedaydreams.domain.time.*

class GymProgrammeModelsTest {
    private val start = CalendarDates.of(2026, 11, 13)

    private fun settings(
        days: Set<DayOfWeek> = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
        equipment: Set<String> = setOf("barbell", "squat rack"),
    ) =
        Settings(
            Goal.STRENGTH,
            Priority.BALANCED,
            Experience.INTERMEDIATE,
            days,
            2,
            equipment,
            45,
            Effort.RIR,
        )

    private val prescription =
        Prescription("back-squat", 3, 4, 6, null, null, 6, 8, 180, "Controlled", 82.0)

    private fun session(
        id: String = "planned",
        day: LocalDate = start,
        exercises: List<Prescription> = listOf(prescription),
    ) = Session(id, day, "Lower", exercises, "Original planned prescription")

    private fun programme(
        sessions: List<Session> = listOf(session()),
        end: LocalDate = CalendarDates.of(2026, 12, 24),
        tradeoffs: List<String> = listOf("Retained trade-off"),
    ) =
        GymProgramme(
            " version ",
            1,
            Instants.ofEpochSecond(100, 123456789),
            "Initial",
            settings(),
            start,
            end,
            sessions,
            tradeoffs,
        )

    @Test
    fun programmeAndSettingsFreezeAllOriginalPrescriptionSnapshots() {
        val days = mutableSetOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        val equipment = mutableSetOf("barbell", "squat rack")
        val configured = settings(days, equipment)
        days.clear()
        equipment.clear()
        assertEquals(2, configured.days().size)
        assertEquals(2, configured.equipment().size)
        assertFails { (configured.days() as MutableSet<DayOfWeek>).clear() }
        val exercises = mutableListOf(prescription)
        val planned = session(exercises = exercises)
        val sessions = mutableListOf(planned)
        val tradeoffs = mutableListOf("Trade-off")
        val retained = programme(sessions = sessions, tradeoffs = tradeoffs)
        exercises.clear()
        sessions.clear()
        tradeoffs.clear()
        assertEquals(prescription, retained.sessions().single().exercises().single())
        assertEquals(listOf("Trade-off"), retained.tradeoffs())
        assertEquals(" version ", retained.id())
        assertEquals(123456789, retained.createdAt().getNano())
        assertFails { (retained.sessions() as MutableList<Session>).clear() }
        assertFails { (planned.exercises() as MutableList<Prescription>).clear() }
    }

    @Test
    fun retainedVersionsRejectDuplicatePlacementsAndKeepOriginalBlockBoundary() {
        assertFailsWith<IllegalArgumentException> { programme(listOf(session(), session("other"))) }
        assertFailsWith<IllegalArgumentException> {
            programme(listOf(session(), session(day = CalendarDates.of(2026, 11, 14))))
        }
        assertFailsWith<IllegalArgumentException> {
            programme(listOf(session(day = CalendarDates.of(2026, 11, 12))))
        }
        assertFailsWith<IllegalArgumentException> { programme(end = CalendarDates.of(2027, 1, 9)) }
        assertFailsWith<IllegalArgumentException> {
            programme(end = CalendarDates.of(2026, 11, 12))
        }
        assertEquals(
            CalendarDates.of(2027, 1, 8),
            programme(end = CalendarDates.of(2027, 1, 8)).end(),
        )
        // Original retained model permits a version with no remaining sessions.
        assertTrue(programme(sessions = emptyList()).sessions().isEmpty())
    }

    @Test
    fun referencesStayOptionalAndDistinctFromThePrescribedTask() {
        assertEquals(82.0, prescription.referenceLoadKg())
        val withoutReference =
            Prescription("back-squat", 3, 4, 6, null, null, 6, 8, 180, "Controlled")
        assertNull(withoutReference.referenceLoadKg())
        assertEquals(0.0, prescription.copy(referenceLoadKg = 0.0).referenceLoadKg())
        val timed = Prescription("plank", 2, 0, 0, 30, null, 5, 7, 60, "Hold")
        val carry = Prescription("farmer-carry", 2, 0, 0, null, 30.0, 5, 7, 60, "Carry")
        assertEquals(30, timed.seconds())
        assertNull(timed.metres())
        assertEquals(30.0, carry.metres())
        assertNull(carry.seconds())
        assertFailsWith<IllegalArgumentException> { timed.copy(referenceLoadKg = 82.0) }
        assertFailsWith<IllegalArgumentException> { timed.copy(metres = 30.0) }
        assertFailsWith<IllegalArgumentException> { prescription.copy(sets = 7) }
        assertFailsWith<IllegalArgumentException> { prescription.copy(maximumReps = 101) }
        assertFailsWith<IllegalArgumentException> {
            prescription.copy(minimumRpe = 9, maximumRpe = 8)
        }
        assertFailsWith<IllegalArgumentException> { prescription.copy(restSeconds = 29) }
        assertFailsWith<IllegalArgumentException> { timed.copy(seconds = 601) }
        assertFailsWith<IllegalArgumentException> { carry.copy(metres = Double.NaN) }
    }

    @Test
    fun settingsRetainFrequencyTimeEquipmentAndStoredEnumIdentities() {
        assertEquals(
            listOf("SUPPORT_RUNNING", "STRENGTH", "MUSCLE", "GENERAL", "POWER"),
            Goal.values().map { it.name },
        )
        assertEquals(
            listOf("BEGINNER", "INTERMEDIATE", "EXPERIENCED"),
            Experience.values().map { it.name },
        )
        assertEquals(Effort.RIR, settings().effort())
        assertFailsWith<IllegalArgumentException> { settings().copy(frequency = 3) }
        assertFailsWith<IllegalArgumentException> { settings().copy(minutes = 20) }
        assertFailsWith<IllegalArgumentException> { settings(equipment = setOf("unknown")) }
        assertFailsWith<NullPointerException> { settings().copy(goal = null) }
        assertFailsWith<IllegalArgumentException> { session(exercises = emptyList()) }
        assertFailsWith<IllegalArgumentException> { session(exercises = List(11) { prescription }) }
    }
}
