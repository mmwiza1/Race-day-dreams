package org.racedaydreams.domain

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Competing UI/IO requests must not overlap or lose actual sets/revisions. */
class GymCoordinatorConcurrencyTest {
    @Test
    fun concurrentSetCommandsSerializeTheWholeRepositoryOperation() {
        val inWrite = AtomicInteger()
        val overlaps = AtomicInteger()
        val ticks = AtomicLong()
        var stored: GymExecutionRepository.Stored? = null
        val repo =
            object : GymExecutionRepository {
                private fun write(e: GymExecution, revision: Long): GymExecutionRepository.Stored {
                    if (inWrite.incrementAndGet() != 1) overlaps.incrementAndGet()
                    try {
                        Thread.yield()
                        assertEquals(stored?.revision() ?: 0L, revision)
                        return GymExecutionRepository.Stored(e, revision + 1).also { stored = it }
                    } finally {
                        inWrite.decrementAndGet()
                    }
                }

                override fun start(initial: GymExecution) = write(initial, 0)

                override fun save(snapshot: GymExecution, expectedRevision: Long) =
                    write(snapshot, expectedRevision)

                override fun finish(finished: GymExecution, expectedRevision: Long) =
                    write(finished, expectedRevision)

                override fun load(id: String) = stored

                override fun unfinished(): List<GymExecutionRepository.Summary> = emptyList()

                override fun saveWorkout(
                    workout: GymWorkoutDefinition,
                    source: GymExecution.SourceReference?,
                ) = error("not used")

                override fun workout(id: String, revision: Int): GymWorkoutDefinition? = null

                override fun preferences(): List<GymExecutionRepository.Preference> = emptyList()

                override fun savePreference(preference: GymExecutionRepository.Preference) =
                    error("not used")
            }
        val c =
            GymWorkoutSessionCoordinator(repo) {
                GymExecution.Tick(
                    "boot",
                    ticks.getAndIncrement(),
                    Instants.ofEpochSecond(1793102400),
                )
            }
        val exercise = ExerciseCatalog.definition("bench-press")!!.exercise()
        val workout =
            GymWorkoutDefinition(
                "p",
                1,
                Instants.ofEpochSecond(1793102400),
                "Upper",
                listOf(GymWorkoutDefinition.Slot("a", exercise, null, null)),
            )
        c.start("workout", GymExecution.Origin.CUSTOM, workout, null, Offsets.ofTotalSeconds(0))
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results =
                pool.invokeAll(
                    (0 until 64).map { i ->
                        Callable {
                            c.saveSet("a", StrengthSession.SetEntry("set-$i", 5, 80.0, 7.0, false))
                        }
                    }
                )
            results.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
        val finished = c.finish(null, null, false).accepted()!!
        assertEquals(0, overlaps.get())
        assertEquals(0, inWrite.get())
        assertEquals(66L, finished.revision())
        assertEquals(64, finished.execution().actualSets().size)
        assertEquals(64, finished.execution().actualSets().map { it.actual().id() }.toSet().size)
        assertEquals(finished, stored)
    }
}
