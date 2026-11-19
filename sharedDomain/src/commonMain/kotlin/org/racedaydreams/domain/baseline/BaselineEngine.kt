package org.racedaydreams.domain.baseline

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

/** Original summaries of recorded evidence; missing workouts and metrics are never invented. */
object BaselineEngine {
    @JvmStatic
    fun derive(activities: List<ActivityRecord>, from: Instant, to: Instant): RunnerBaseline =
        derive(activities, emptyList(), from, to)

    @JvmStatic
    fun derive(
        activities: List<ActivityRecord>,
        results: List<RecordedResult>,
        from: Instant,
        to: Instant,
    ): RunnerBaseline {
        val empty = RunnerBaseline.empty(from, to)
        val runs = mutableListOf<ActivityRecord>()
        val seen = mutableSetOf<String>()
        for (run in activities) {
            val identity = run.source().provider() + "\u0000" + run.source().recordId()
            if (!run.start().isBefore(from) && run.start().isBefore(to) && seen.add(identity))
                runs.add(run)
        }
        runs.sortWith(compareBy<ActivityRecord> { it.start() }.thenBy { it.id() })
        val possibleMirrors = collapseExactMirrors(runs)
        val provenance = linkedMapOf<String, BaselineProvenance>()
        val explanations = mutableListOf<String>()
        val history = mutableListOf<WeeklyRunningSummary>()
        val gaps = mutableListOf<RecordedTrainingGap>()
        val weeks = mutableMapOf<LocalDate, Week>()
        val durations = mutableListOf<Double>()
        var longest: Double? = null
        var measuredDistances = 0
        val origin = combinedOrigin(runs)
        for (run in runs) {
            val offset = run.startOffset() ?: Offsets.ofTotalSeconds(0)
            val date = CalendarDates.atOffset(run.start(), offset)
            val monday = date.minusDays((date.getDayOfWeek().getValue() - 1).toLong())
            val week =
                weeks.getOrPut(monday) {
                    val start =
                        Instants.ofEpochSecond(
                            monday.toEpochDay() * 86400L - offset.getTotalSeconds()
                        )
                    Week(
                        !start.isBefore(from) &&
                            !Instants.ofEpochSecond(checkedAdd(start.getEpochSecond(), 7 * 86400L))
                                .isAfter(to)
                    )
                }
            week.count++
            val metres = run.distanceMetres()
            if (metres == null) week.hasMissingDistance = true
            else {
                measuredDistances++
                week.distanceKm += metres / 1000.0
                longest = longest?.let { max(it, metres / 1000.0) } ?: (metres / 1000.0)
            }
            val elapsed = Durations.between(run.start(), run.end())
            durations.add((elapsed.getSeconds() + elapsed.getNano() / 1_000_000_000.0) / 60.0)
        }
        var weeklyDistance: Double? = null
        var frequency: Double? = null
        var distanceSum = 0.0
        var fullDistanceWeeks = 0
        var fullRunWeeks = 0
        var runCount = 0
        val orderedWeeks = weeks.keys.sortedBy { it.toEpochDay() }
        for (date in orderedWeeks) {
            val week = weeks.getValue(date)
            val distance = if (week.hasMissingDistance) null else week.distanceKm
            history.add(WeeklyRunningSummary(date, week.count, distance, week.complete))
            if (week.complete) {
                fullRunWeeks++
                runCount += week.count
                if (distance != null) {
                    fullDistanceWeeks++
                    distanceSum += distance
                }
            }
        }
        if (fullDistanceWeeks >= 2) weeklyDistance = distanceSum / fullDistanceWeeks
        if (fullRunWeeks >= 2) frequency = runCount.toDouble() / fullRunWeeks
        val consistent = if (runs.isEmpty()) null else consecutiveRecordedWeeks(orderedWeeks)
        for (index in 1 until runs.size) {
            val days =
                Durations.between(runs[index - 1].start(), runs[index].start()).getSeconds() / 86400
            if (days >= 10)
                gaps.add(RecordedTrainingGap(runs[index - 1].start(), runs[index].start(), days))
        }
        val typical = if (durations.isEmpty()) null else median(durations)
        if (weeklyDistance != null) provenance[RunnerBaseline.WEEKLY_DISTANCE] = origin
        if (frequency != null) provenance[RunnerBaseline.RUNS_PER_WEEK] = origin
        if (longest != null) provenance[RunnerBaseline.LONGEST_RUN] = origin
        if (typical != null) provenance[RunnerBaseline.TYPICAL_DURATION] = origin
        if (consistent != null) provenance[RunnerBaseline.CONSISTENT_WEEKS] = origin
        val recent5k = resultTime(results, 5000.0, from, to, provenance, RunnerBaseline.RECENT_5K)
        val recent10k =
            resultTime(results, 10000.0, from, to, provenance, RunnerBaseline.RECENT_10K)
        val recentHalf =
            resultTime(results, 21097.5, from, to, provenance, RunnerBaseline.RECENT_HALF)
        val latest = runs.lastOrNull()?.start()
        val recent = latest != null && Durations.between(latest, to) <= Durations.ofHours(14 * 24)
        val distanceCoverage = if (runs.isEmpty()) 0.0 else measuredDistances.toDouble() / runs.size
        var confidence = BaselineConfidence.LOW
        if (runs.size >= 6 && fullRunWeeks >= 3 && distanceCoverage >= 0.75 && recent)
            confidence = BaselineConfidence.MODERATE
        if (runs.size >= 16 && fullRunWeeks >= 6 && distanceCoverage >= 0.90 && recent)
            confidence = BaselineConfidence.STRONG
        if (runs.isEmpty()) explanations.addAll(empty.explanations())
        else {
            explanations.add(
                "Found ${runs.size} recorded runs in the selected history window. These are records, not proof of all training completed."
            )
            if (possibleMirrors > 0)
                explanations.add(
                    "$possibleMirrors possible mirrored recordings shared exact start/end times and matching distance. They were counted once for this baseline; original activities remain stored for review."
                )
            if (weeklyDistance != null)
                explanations.add(
                    "Weekly distance averages observed complete weeks with recorded distance. Weeks without records are not counted as zero."
                )
            else
                explanations.add(
                    "There are not yet two complete observed weeks with distance, so habitual weekly mileage remains unknown."
                )
            if (measuredDistances < runs.size)
                explanations.add(
                    "Some runs have no distance. Missing distances have not been replaced with zero."
                )
            explanations.add(
                "Typical duration uses elapsed session time; moving time is kept separate."
            )
            explanations.add(
                "Weekly grouping uses each run's recorded start offset, or explicitly UTC when its offset is unavailable."
            )
            if (gaps.isNotEmpty())
                explanations.add(
                    "There are gaps in recorded runs. They may reflect training changes or incomplete synchronisation."
                )
            if (!recent)
                explanations.add(
                    "The latest recorded run is more than two weeks old; current baseline confidence is reduced."
                )
            if (fullRunWeeks >= 4) explanations.add(trend(history))
        }
        explanations.add(
            "Easy pace is not inferred from session averages without evidence of easy effort. PBs require explicit results or qualified rolling efforts."
        )
        explanations.add(
            when (confidence) {
                BaselineConfidence.STRONG ->
                    "Several recent weeks support a stronger baseline; this is evidence quality, not certainty about race performance."
                BaselineConfidence.MODERATE ->
                    "Several recorded runs support a moderate baseline; more consistent history will improve it."
                else ->
                    "The baseline is provisional. A short assessment alone does not establish endurance."
            }
        )
        return RunnerBaseline(
            weeklyDistance,
            frequency,
            longest,
            typical,
            null,
            consistent,
            recent5k,
            recent10k,
            recentHalf,
            runs.size,
            weeks.size,
            from,
            to,
            latest,
            confidence,
            provenance,
            explanations,
            history,
            gaps,
        )
    }

    @JvmStatic
    fun applyManualOverlay(derived: RunnerBaseline, manual: ManualBaselineInput): RunnerBaseline =
        applyManualOverlay(derived, manual, null)

    /** Explicit review time only, never an app-reload timestamp. Unknown answers retain history. */
    @JvmStatic
    fun applyManualOverlay(
        derived: RunnerBaseline,
        manual: ManualBaselineInput,
        recordedAt: Instant?,
    ): RunnerBaseline {
        val provenance = LinkedHashMap(derived.provenance())
        mark(provenance, RunnerBaseline.WEEKLY_DISTANCE, manual.weeklyDistanceKm())
        mark(provenance, RunnerBaseline.RUNS_PER_WEEK, manual.runsPerWeek())
        mark(provenance, RunnerBaseline.LONGEST_RUN, manual.longestRunKm())
        mark(provenance, RunnerBaseline.EASY_PACE, manual.easyPaceSecondsPerKm())
        mark(provenance, RunnerBaseline.CONSISTENT_WEEKS, manual.consistentWeeks())
        mark(provenance, RunnerBaseline.RECENT_5K, manual.recent5k())
        mark(provenance, RunnerBaseline.RECENT_10K, manual.recent10k())
        mark(provenance, RunnerBaseline.RECENT_HALF, manual.recentHalf())
        val reasons = derived.explanations().toMutableList()
        var confidence = derived.confidence()
        if (manual.hasAnyValue())
            reasons.add(
                "Your corrections and optional answers are marked as manually entered. They do not fabricate workout history or increase measured-data confidence."
            )
        if (
            confidence == BaselineConfidence.STRONG &&
                materiallyDifferent(manual.weeklyDistanceKm(), derived.weeklyDistanceKm())
        ) {
            confidence = BaselineConfidence.MODERATE
            reasons.add(
                "Your weekly-distance correction differs substantially from recorded history. The combined baseline remains provisional while the difference is reviewed."
            )
        }
        return RunnerBaseline(
            manual.weeklyDistanceKm() ?: derived.weeklyDistanceKm(),
            manual.runsPerWeek() ?: derived.runsPerWeek(),
            manual.longestRunKm() ?: derived.longestRunKm(),
            derived.typicalRunMinutes(),
            manual.easyPaceSecondsPerKm() ?: derived.easyPaceSecondsPerKm(),
            manual.consistentWeeks() ?: derived.consistentWeeks(),
            manual.recent5k() ?: derived.recent5k(),
            manual.recent10k() ?: derived.recent10k(),
            manual.recentHalf() ?: derived.recentHalf(),
            derived.activityCount(),
            derived.weeksWithRuns(),
            derived.windowStart(),
            derived.windowEnd(),
            derived.lastActivityAt(),
            confidence,
            provenance,
            reasons,
            derived.weeklyHistory(),
            derived.gaps(),
            if (manual.hasAnyValue()) recordedAt else derived.manualRecordedAt(),
        )
    }

    private fun mark(values: MutableMap<String, BaselineProvenance>, key: String, value: Any?) {
        if (value != null) values[key] = BaselineProvenance.MANUAL
    }

    private fun materiallyDifferent(manual: Double?, recorded: Double?): Boolean =
        manual != null && recorded != null && abs(manual - recorded) > max(5.0, recorded * 0.25)

    private fun collapseExactMirrors(runs: MutableList<ActivityRecord>): Int {
        var mirrors = 0
        val unique = mutableListOf<ActivityRecord>()
        for (candidate in runs) {
            var duplicate = false
            for (index in unique.size - 1 downTo 0) {
                val prior = unique[index]
                if (prior.start() != candidate.start()) break
                val otherOrigin =
                    prior.source().originPackage() != candidate.source().originPackage() ||
                        prior.source().provider() != candidate.source().provider()
                val previousMetres = prior.distanceMetres()
                val metres = candidate.distanceMetres()
                if (
                    otherOrigin &&
                        prior.end() == candidate.end() &&
                        previousMetres != null &&
                        metres != null &&
                        abs(previousMetres - metres) <= 1.0
                ) {
                    duplicate = true
                    break
                }
            }
            if (duplicate) mirrors++ else unique.add(candidate)
        }
        runs.clear()
        runs.addAll(unique)
        return mirrors
    }

    private fun resultTime(
        results: List<RecordedResult>,
        metres: Double,
        from: Instant,
        to: Instant,
        provenance: MutableMap<String, BaselineProvenance>,
        field: String,
    ): Duration? {
        val utc = Offsets.ofTotalSeconds(0)
        val first = CalendarDates.atOffset(from, utc)
        val last =
            CalendarDates.atOffset(
                Instants.ofEpochSecond(to.getEpochSecond(), to.getNano().toLong() - 1),
                utc,
            )
        val relevant = mutableListOf<RecordedResult>()
        for (result in results) {
            if (
                abs(result.distance().metres() - metres) > 0.001 ||
                    result.date().toEpochDay() < first.toEpochDay() ||
                    result.date().toEpochDay() > last.toEpochDay()
            )
                continue
            relevant.add(result)
        }
        val current = PersonalBestCalculator.current(relevant)
        if (current.isNotEmpty()) {
            val result = current.first().result()
            provenance[field] =
                if (result.source() == ResultSource.ROLLING) BaselineProvenance.INFERRED
                else BaselineProvenance.MANUAL
            return result.duration()
        }
        return null
    }

    private fun combinedOrigin(runs: List<ActivityRecord>): BaselineProvenance {
        var result: BaselineProvenance? = null
        for (run in runs) {
            val current =
                when (run.source().provider()) {
                    "health-connect" -> BaselineProvenance.HEALTH_CONNECT
                    "assessment" -> BaselineProvenance.ASSESSMENT
                    "manual" -> BaselineProvenance.MANUAL
                    else -> BaselineProvenance.INFERRED
                }
            if (result == null) result = current
            else if (result != current) return BaselineProvenance.INFERRED
        }
        return result ?: BaselineProvenance.INFERRED
    }

    private fun consecutiveRecordedWeeks(weeks: List<LocalDate>): Int {
        var current = 0
        var previous: LocalDate? = null
        for (week in weeks) {
            current = if (previous != null && week == previous.plusWeeks(1)) current + 1 else 1
            previous = week
        }
        return current
    }

    private fun median(values: MutableList<Double>): Double {
        values.sort()
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle]
        else (values[middle - 1] + values[middle]) / 2
    }

    private fun trend(history: List<WeeklyRunningSummary>): String {
        val usable = mutableListOf<Double>()
        for (week in history) {
            val distance = week.distanceKm()
            if (week.completeWindowWeek() && distance != null) usable.add(distance)
        }
        if (usable.size < 4)
            return "There are not yet enough complete distance weeks to describe a trend."
        val middle = usable.size / 2
        var early = 0.0
        var late = 0.0
        for (index in usable.indices) {
            if (index < middle) early += usable[index] else late += usable[index]
        }
        early /= middle
        late /= usable.size - middle
        if (early == 0.0)
            return "Recorded weekly distance is now present after earlier observed zero-distance runs; a percentage trend would be misleading."
        val ratio = late / early
        return if (ratio > 1.15)
            "Recorded distance in later observed weeks is higher than earlier observed weeks."
        else if (ratio < 0.85)
            "Recorded distance in later observed weeks is lower than earlier observed weeks."
        else "Recorded distance is broadly similar across the observed complete weeks."
    }

    private class Week(val complete: Boolean) {
        var count = 0
        var distanceKm = 0.0
        var hasMissingDistance = false
    }
}
