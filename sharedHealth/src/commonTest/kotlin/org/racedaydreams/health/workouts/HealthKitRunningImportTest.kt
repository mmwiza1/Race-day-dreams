package org.racedaydreams.health.workouts

import kotlin.test.*
import kotlinx.datetime.Instant
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.health.HealthImportPolicy
import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.health.body.ReadCompleteness

class HealthKitRunningImportTest {
    private val from = Instant.parse("2026-11-01T00:00:00Z")
    private val until = Instant.parse("2026-11-17T00:00:00Z")
    private val window = BodyReadWindow(from, until)

    private fun run(id: String = "source-run", distance: Double? = 5000.0) =
        HealthKitRun(
            id,
            "org.source.watch",
            Instant.parse("2026-11-16T09:00:00.123Z"),
            Instant.parse("2026-11-16T09:30:00.456Z"),
            distance,
        )

    private fun batch(
        records: Collection<HealthKitRun>,
        previous: List<ActivityRecord> = emptyList(),
    ) =
        HealthKitRunningImport.batch(
            window,
            "org.racedaydreams",
            records,
            previous,
            "retained-anchor",
        )

    @Test
    fun actualSummaryKeepsSourceAndDistanceWithoutInventingOtherMeasurements() {
        val batch = batch(listOf(run()))
        val actual = batch.upserts().single()
        assertEquals("health-kit:source-run", actual.id())
        assertEquals("health-kit", actual.source().provider())
        assertEquals("source-run", actual.source().recordId())
        assertEquals("org.source.watch", actual.source().originPackage())
        assertEquals(5000.0, actual.distanceMetres())
        assertEquals("2026-11-16T09:00:00.123Z", actual.start().toString())
        assertEquals("2026-11-16T09:30:00.456Z", actual.end().toString())
        assertNull(actual.movingTime())
        assertNull(actual.averageHeartRate())
        assertNull(actual.maxHeartRate())
        assertNull(actual.startOffset())
        assertTrue(actual.samples().isEmpty())
        assertTrue(actual.laps().isEmpty())
        assertFalse(batch.completeSnapshot())
        assertEquals("retained-anchor", batch.nextToken())
    }

    @Test
    fun indoorWorkoutMetadataIsRetainedWithoutInventingASpecificMachineOrRoute() {
        val imported = batch(listOf(run().copy(indoor = true))).upserts().single()
        assertEquals(
            org.racedaydreams.domain.activity.RunEnvironment.INDOOR,
            imported.environment(),
        )
        assertTrue(imported.samples().isEmpty())
        assertNull(imported.movingTime())
        val refreshed = batch(listOf(run()), listOf(imported)).upserts().single()
        assertEquals(imported, refreshed)
    }

    @Test
    fun emptyOrHiddenReadNeverDeletesRetainedHistoryOrClearsACheckpoint() {
        val existing = batch(listOf(run())).upserts()
        var writes = 0
        val repository =
            object : HealthImportRepository {
                override fun activities() = existing

                override fun importToken(provider: String) = "retained-anchor"

                override fun applyImport(
                    provider: String,
                    upserts: List<ActivityRecord>,
                    deletedRecordIds: List<String>,
                    nextToken: String?,
                ) {
                    writes++
                    assertEquals("health-kit", provider)
                    assertTrue(upserts.isEmpty())
                    assertTrue(deletedRecordIds.isEmpty())
                    assertEquals("retained-anchor", nextToken)
                }
            }
        HealthImportPolicy.commit(repository, batch(emptyList(), existing))
        assertEquals(1, writes)
    }

    @Test
    fun missingDistanceStaysUnknownAndGenuineZeroRemainsDistinct() {
        assertNull(batch(listOf(run(distance = null))).upserts().single().distanceMetres())
        assertEquals(0.0, batch(listOf(run(distance = 0.0))).upserts().single().distanceMetres())
    }

    @Test
    fun retainedOptionalEvidenceAndOpaqueActivityIdentitySurviveAWithheldOptionalField() {
        val original =
            batch(listOf(run()))
                .upserts()
                .single()
                .copy(
                    id = "retained-app-identity",
                    notes = "Retained athlete notes",
                    shoeId = "shoe",
                    averageHeartRate = 140.0,
                )
        val updated = batch(listOf(run(distance = null)), listOf(original)).upserts().single()
        assertEquals(original, updated)
        assertEquals(
            5100.0,
            batch(listOf(run(distance = 5100.0)), listOf(original))
                .upserts()
                .single()
                .distanceMetres(),
        )
    }

    @Test
    fun ownAppObservationsAndOutsideOrUnfinishedWorkoutsAreNotReimported() {
        val records =
            listOf(
                run().copy(origin = "org.racedaydreams"),
                run("before").copy(start = Instant.parse("2026-10-31T23:59:59Z")),
                run("exclusive-end")
                    .copy(start = until, end = Instant.parse("2026-11-17T01:00:00Z")),
                run("unfinished").copy(end = Instant.parse("2026-11-17T00:01:00Z")),
            )
        assertTrue(batch(records).upserts().isEmpty())
        assertTrue(batch(records).deletedRecordIds().isEmpty())
    }

    @Test
    fun identicalDuplicatesDeduplicateButConflictingObservationsRejectTheWholeRead() {
        assertEquals(1, batch(listOf(run(), run())).upserts().size)
        assertFailsWith<IllegalArgumentException> { batch(listOf(run(), run(distance = 5001.0))) }
        assertFailsWith<IllegalArgumentException> {
            batch(listOf(run(), run().copy(origin = "other.source")))
        }
    }

    @Test
    fun incompatibleRetainedSourceOrDatesCannotTransferPreviousEvidence() {
        val original = batch(listOf(run())).upserts().single()
        assertFailsWith<IllegalArgumentException> {
            batch(listOf(run().copy(origin = "another.source")), listOf(original))
        }
        assertFailsWith<IllegalArgumentException> {
            batch(listOf(run().copy(end = Instant.parse("2026-11-16T10:00:00Z"))), listOf(original))
        }
        assertFailsWith<IllegalArgumentException> {
            batch(listOf(run()), listOf(original, original.copy(id = "conflicting")))
        }
    }

    @Test
    fun invalidValuesAndOversizedReadsRejectInsteadOfTruncatingOrFabricating() {
        for (distance in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) assertFailsWith<
            IllegalArgumentException
        > {
            run(distance = distance)
        }
        assertFailsWith<IllegalArgumentException> { run().copy(recordId = " ") }
        assertFailsWith<IllegalArgumentException> { run().copy(end = run().start) }
        assertFailsWith<IllegalArgumentException> {
            batch(List(HealthKitRunningImport.MAX_RECORDS + 1) { run("$it") })
        }
        assertFailsWith<IllegalArgumentException> {
            HealthKitRunningImport.batch(window, "", emptyList(), emptyList(), null)
        }
    }

    @Test
    fun stagingCopiesSdkResultsAndPreparesFromFreshLocalEvidenceAtCommit() {
        val observed = mutableListOf(run(distance = null))
        val read = HealthKitRunningRead(window, "org.racedaydreams", observed)
        observed.clear()
        assertEquals(ReadCompleteness.VISIBLE_RECORDS_ONLY, read.completeness)
        assertEquals(1, read.records.size)
        val fresh =
            batch(listOf(run()))
                .upserts()
                .single()
                .copy(notes = "Saved while SDK query was running")
        assertEquals(fresh, read.batch(listOf(fresh), "latest-anchor").upserts().single())
        assertEquals("latest-anchor", read.batch(listOf(fresh), "latest-anchor").nextToken())
    }
}
