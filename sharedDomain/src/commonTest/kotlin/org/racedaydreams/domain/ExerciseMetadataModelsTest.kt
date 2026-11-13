package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.ExerciseMetadata.*

class ExerciseMetadataModelsTest {
    @Test
    fun everyOriginalCanonicalExerciseHasExplicitCompleteMetadata() {
        val rows = ExerciseMetadataCatalog.entries()
        assertEquals(1, ExerciseMetadataCatalog.VERSION)
        assertEquals(ExerciseCatalog.exercises().map { it.id() }.toSet(), rows.keys)
        assertEquals(220, rows.size)
        for ((id, entry) in rows) {
            assertEquals(id, entry.exerciseId())
            assertTrue(entry.primaryRole() in entry.roles())
            assertTrue(entry.primaryAreas().isNotEmpty())
            assertTrue(entry.primaryAreas().none { it in entry.secondaryAreas() })
            assertTrue(entry.equipment().alternatives().isNotEmpty())
            for (setup in entry.equipment().alternatives()) assertTrue(
                entry.equipment().availableWith(setup)
            )
        }
        assertNull(ExerciseMetadataCatalog.find("custom-unknown"))
        assertNull(ExerciseMetadataCatalog.find(null))
        assertFailsWith<IllegalArgumentException> {
            ExerciseMetadataCatalog.require("custom-unknown")
        }
        assertFails { (rows as MutableMap<String, ExerciseMetadata>).clear() }
    }

    @Test
    fun reviewedRolesAndEquipmentAreFrozenInCanonicalEnumOrder() {
        val roleSource = mutableSetOf(Role.ISOLATION, Role.SQUAT)
        val primarySource = mutableSetOf(BodyArea.GLUTES, BodyArea.QUADS)
        val setupSource = mutableSetOf(Equipment.SQUAT_RACK, Equipment.BARBELL)
        val equipment = EquipmentRequirement(listOf(setupSource))
        val metadata =
            ExerciseMetadata(
                "retained",
                Role.SQUAT,
                roleSource,
                primarySource,
                emptySet(),
                Suitability.INTERMEDIATE,
                equipment,
            )
        roleSource.clear()
        primarySource.clear()
        setupSource.clear()
        assertEquals(listOf(Role.SQUAT, Role.ISOLATION), metadata.roles().toList())
        assertEquals(listOf(BodyArea.QUADS, BodyArea.GLUTES), metadata.primaryAreas().toList())
        assertEquals(
            listOf(Equipment.BARBELL, Equipment.SQUAT_RACK),
            metadata.equipment().alternatives().single().toList(),
        )
        assertFalse(metadata.equipment().availableWith(setOf(Equipment.BARBELL)))
        assertFalse(Suitability.TECHNICAL.appropriateFor(GymProgramme.Experience.BEGINNER))
        assertTrue(Suitability.TECHNICAL.appropriateFor(GymProgramme.Experience.EXPERIENCED))
        assertFails { (metadata.roles() as MutableSet<Role>).clear() }
        assertFails {
            (metadata.equipment().alternatives().single() as MutableSet<Equipment>).clear()
        }
    }

    @Test
    fun invalidSetupAndUnreviewedRolesCannotAcquireAutomaticMetadata() {
        assertFailsWith<IllegalArgumentException> { EquipmentRequirement(emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            EquipmentRequirement(listOf(emptySet(), setOf(Equipment.BARBELL)))
        }
        assertFailsWith<IllegalArgumentException> {
            EquipmentRequirement(listOf(setOf(Equipment.BARBELL), setOf(Equipment.BARBELL)))
        }
        val entry = ExerciseMetadataCatalog.require("back-squat")
        assertFailsWith<IllegalArgumentException> { entry.copy(roles = emptySet()) }
        assertFailsWith<IllegalArgumentException> { entry.copy(primaryAreas = emptySet()) }
        assertFailsWith<IllegalArgumentException> {
            entry.copy(secondaryAreas = entry.primaryAreas())
        }
        assertFailsWith<NullPointerException> { Suitability.INTERMEDIATE.appropriateFor(null) }
    }

    @Test
    fun sharedImmutableMapsKeepLookupOrderAndHaveNoMutableEntryEscape() {
        val source = linkedMapOf("second" to 2, "first" to 1)
        val frozen = Immutable.copyMap(source)
        source["second"] = 99
        source.clear()
        assertEquals(listOf("second", "first"), frozen.keys.toList())
        assertEquals(2, frozen["second"])
        assertEquals(mapOf("first" to 1, "second" to 2), frozen)
        assertEquals(mapOf("first" to 1, "second" to 2).hashCode(), frozen.hashCode())
        assertFails { (frozen as MutableMap<String, Int>)["other"] = 3 }
        assertFails {
            (frozen.entries.first() as MutableMap.MutableEntry<String, Int>).setValue(99)
        }
        assertFailsWith<NullPointerException> { Immutable.copyMap(mapOf<String?, Int>(null to 1)) }
    }
}
