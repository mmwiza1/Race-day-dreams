package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.coaching.Immutable

/**
 * Reproducible progression. Slower source results remain retained outside the improvement timeline.
 */
object PersonalBestCalculator {
    @JvmStatic
    fun history(results: List<RecordedResult?>?): List<PersonalBestEntry> {
        if (results == null) throw NullPointerException("Results")
        val ids = linkedMapOf<String, RecordedResult>()
        val distances = mutableMapOf<String, Double>()
        val authoritative = mutableMapOf<String, Int>()
        for (result in results) {
            if (result == null) throw NullPointerException("Result")
            val existing = ids.put(result.id(), result)
            require(existing == null || existing == result) {
                "Conflicting duplicate result ID: ${result.id()}"
            }
            val metres = distances.put(result.distance().code(), result.distance().metres())
            require(metres == null || abs(metres - result.distance().metres()) <= .000001) {
                "One distance code cannot refer to different distances."
            }
            if (result.activityId() != null) {
                val key = linkedKey(result)
                authoritative[key] = maxOf(authoritative[key] ?: 0, priority(result.source()))
            }
        }
        // Date-only evidence cannot establish within-day ordering: show the day's best first.
        val ordered =
            ids.values
                .filter {
                    it.activityId() == null ||
                        priority(it.source()) >= authoritative.getValue(linkedKey(it))
                }
                .sortedWith(
                    compareBy<RecordedResult> { it.date().toEpochDay() }
                        .thenBy { it.distance().code() }
                        .thenBy { it.duration() }
                        .thenBy { -priority(it.source()) }
                        .thenBy { it.id() }
                )
        val current = mutableMapOf<String, RecordedResult>()
        val history = mutableListOf<PersonalBestEntry>()
        for (result in ordered) {
            val key = result.distance().code()
            val previous = current[key]
            if (previous == null || result.duration() < previous.duration()) {
                history.add(
                    PersonalBestEntry(
                        result,
                        previous,
                        previous?.duration()?.minus(result.duration()),
                    )
                )
                current[key] = result
            }
        }
        return Immutable.copyList(history)
    }

    @JvmStatic
    fun current(results: List<RecordedResult?>?): List<PersonalBestEntry> {
        val current = mutableMapOf<String, PersonalBestEntry>()
        for (entry in history(results)) current[entry.result().distance().code()] = entry
        return Immutable.copyList(current.values.sortedBy { it.result().distance().metres() })
    }

    private fun priority(source: ResultSource): Int =
        when (source) {
            ResultSource.OFFICIAL_CHIP -> 3
            ResultSource.MANUAL -> 2
            else -> 1
        }

    private fun linkedKey(result: RecordedResult): String =
        result.activityId() + "\u0000" + result.distance().code()
}
