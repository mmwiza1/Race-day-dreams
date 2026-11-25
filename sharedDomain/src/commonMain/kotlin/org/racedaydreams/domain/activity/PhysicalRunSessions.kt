package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Instant

/**
 * Groups retained activity records that describe the same physical run so analytics count each run
 * once. Read-only and derived: no record, recording or annotation is changed or removed.
 *
 * The same run often reaches the app more than once, for example as a Samsung Health or watch
 * session and an identical session written by another app into Health Connect, or as a native
 * recording plus a manual entry. Records are treated as one session only when evidence shows it:
 * they come from different sources (provider and writing app) and their time ranges overlap for at
 * least half of the shorter record. The athlete's explicit comparison choices can keep an apparent
 * overlap separate or combine a close same-source duplicate. Separate intervals stay separate.
 */
object PhysicalRunSessions {
    /** Minimum shared time, as a share of the shorter record, for two sources to be one run. */
    const val MIN_OVERLAP_SHARE = 0.5

    /**
     * One physical run. [counted] supplies the session's distance and times; [duplicates] are the
     * other sources' representations of the same run, retained but not counted again.
     */
    data class Session(
        val counted: ActivityRecord,
        val duplicates: List<ActivityRecord>,
        val reviewed: Boolean = false,
    ) {
        fun records(): List<ActivityRecord> = listOf(counted) + duplicates
    }

    @JvmStatic
    fun sessions(activities: List<ActivityRecord>): List<Session> {
        val ordered =
            activities
                .distinctBy { it.id() }
                .sortedWith(compareBy<ActivityRecord> { it.start() }.thenBy { it.id() })
        val groups = mutableListOf<MutableList<ActivityRecord>>()
        // Groups that can still overlap a later record, in creation order, with their latest end.
        // Records are visited by start time, so once every member of a group has ended at or before
        // a record's start, no later record can overlap it either. Skipping those groups keeps a
        // long history linear without changing which group a record joins.
        val open = mutableListOf<Pair<MutableList<ActivityRecord>, Instant>>()
        for (activity in ordered) {
            open.removeAll { !it.second.isAfter(activity.start()) }
            val key = sourceKey(activity)
            val index =
                open.indexOfFirst { (members, _) ->
                    members.all { prior ->
                        val decision = decision(prior, activity)
                        decision != DuplicateRunChoice.SEPARATE &&
                            (sourceKey(prior) != key || decision != null)
                    } &&
                        members.any { prior ->
                            sameRun(prior, activity) ||
                                possibleDuplicate(prior, activity) &&
                                    decision(prior, activity) != null
                        }
                }
            if (index < 0) {
                val members = mutableListOf(activity)
                groups.add(members)
                open.add(members to activity.end())
            } else {
                val (members, end) = open[index]
                members.add(activity)
                open[index] = members to maxOf(end, activity.end())
            }
        }
        val result =
            groups.map { members ->
                val votes =
                    members.associate { candidate ->
                        candidate.id() to
                            members.count {
                                it !== candidate &&
                                    decision(candidate, it) == DuplicateRunChoice.COUNT_THIS
                            }
                    }
                val chosen =
                    members
                        .sortedWith(
                            compareByDescending<ActivityRecord> { votes.getValue(it.id()) }
                                .then(PREFERENCE)
                        )
                        .first()
                val others = members.filter { it !== chosen }
                val reviewed = votes.getValue(chosen.id()) > 0
                val preservedWorkout =
                    others.mapNotNull { it.plannedWorkoutId() }.distinct().singleOrNull()
                val preservedShoe = others.mapNotNull { it.shoeId() }.distinct().singleOrNull()
                val counted =
                    if (reviewed)
                        chosen.copy(
                            plannedWorkoutId = chosen.plannedWorkoutId() ?: preservedWorkout,
                            shoeId = chosen.shoeId() ?: preservedShoe,
                        )
                    else chosen
                Session(counted, Immutable.copyList(others), reviewed)
            }
        return Immutable.copyList(
            result.sortedWith(compareBy({ it.counted.start() }, { it.counted.id() }))
        )
    }

    /** The record counted for each physical run; analytics use these instead of every record. */
    @JvmStatic
    fun counted(activities: List<ActivityRecord>): List<ActivityRecord> =
        Immutable.copyList(sessions(activities).map { it.counted })

    /** Suppression explicitly chosen by the athlete. Existing unreviewed analysis is retained. */
    fun reviewedEvidence(activities: List<ActivityRecord>): List<ActivityRecord> {
        if (activities.none { it.duplicateChoices().isNotEmpty() }) return activities
        val reviewed = sessions(activities).filter { it.reviewed }
        val ignored = reviewed.flatMap { it.duplicates }.map { it.id() }.toSet()
        val representatives = reviewed.associate { it.counted.id() to it.counted }
        return activities.filter { it.id() !in ignored }.map { representatives[it.id()] ?: it }
    }

    fun decision(first: ActivityRecord, second: ActivityRecord): DuplicateRunChoice? =
        first.duplicateChoices()[second.id()] ?: second.duplicateChoices()[first.id()]?.reciprocal()

    /** A conservative same-writer candidate is reviewable, and counts separately until reviewed. */
    fun possibleDuplicate(a: ActivityRecord, b: ActivityRecord): Boolean {
        if (a.id() == b.id()) return false
        if (sameRun(a, b)) return true
        if (sourceKey(a) != sourceKey(b)) return false
        val start = maxOf(a.start(), b.start())
        val end = minOf(a.end(), b.end())
        if (!end.isAfter(start)) return false
        val duration = maxOf(seconds(a.start(), a.end()), seconds(b.start(), b.end()))
        return seconds(start, end) >= duration * 0.9 &&
            kotlin.math.abs(seconds(a.start(), b.start())) <= 30 &&
            kotlin.math.abs(seconds(a.end(), b.end())) <= 30
    }

    /** Whether two records from different sources overlap enough to be the same run. */
    @JvmStatic
    fun sameRun(a: ActivityRecord, b: ActivityRecord): Boolean {
        if (sourceKey(a) == sourceKey(b)) return false
        val start = maxOf(a.start(), b.start())
        val end = minOf(a.end(), b.end())
        if (!end.isAfter(start)) return false
        val shared = seconds(start, end)
        val shorter = minOf(seconds(a.start(), a.end()), seconds(b.start(), b.end()))
        return shorter > 0 && shared >= shorter * MIN_OVERLAP_SHARE
    }

    private fun sourceKey(activity: ActivityRecord): Pair<String, String?> =
        activity.source().provider() to activity.source().originPackage()

    private fun seconds(
        from: org.racedaydreams.domain.time.Instant,
        to: org.racedaydreams.domain.time.Instant,
    ): Double = ActivityChecks.seconds(org.racedaydreams.domain.time.Durations.between(from, to))

    /**
     * Which representation supplies a run's totals: one the runner linked to a planned workout
     * first, then this app's own recording (it has the route and samples), imported files, health
     * platforms and manual entries; a record with a distance is preferred over one without.
     */
    private val PREFERENCE =
        compareBy<ActivityRecord>(
            { if (it.plannedWorkoutId() != null) 0 else 1 },
            { providerRank(it.source().provider()) },
            { if (it.distanceMetres() != null) 0 else 1 },
            { it.start() },
            { it.id() },
        )

    private fun providerRank(provider: String): Int =
        when (provider) {
            "rdd" -> 0
            "gpx" -> 1
            "health-connect",
            "apple-health",
            "healthkit" -> 2
            "assessment" -> 3
            "manual" -> 4
            else -> 5
        }
}
