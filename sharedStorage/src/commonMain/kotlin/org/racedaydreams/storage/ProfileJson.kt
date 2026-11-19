package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.DayOfWeek
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/** Frozen profile snapshot inside existing plan versions, independent of the editable profile. */
object ProfileJson {
    fun encode(profile: UserProfile): String = element(profile).toString()

    internal fun element(profile: UserProfile): JsonObject = buildJsonObject {
        val goal = profile.raceGoal()
        val preferences = profile.preferences()
        put(
            "goal",
            buildJsonObject {
                put("distance", goal.distance().name)
                put("raceName", goal.raceName())
                put("raceDate", StoredJson.nullable(goal.raceDate()?.toString()))
                put("mode", goal.mode().name)
                put("targetTime", StoredJson.nullable(goal.targetTime()?.toString()))
            },
        )
        put(
            "preferences",
            buildJsonObject {
                put("strategy", preferences.strategy().name)
                put(
                    "runningDays",
                    buildJsonArray {
                        preferences.runningDays().forEach { add(JsonPrimitive(it.name)) }
                    },
                )
                put("runsPerWeek", preferences.runsPerWeek())
                put("longRunDay", StoredJson.nullable(preferences.longRunDay()?.name))
                put(
                    "gymDays",
                    buildJsonArray { preferences.gymDays().forEach { add(JsonPrimitive(it.name)) } },
                )
                put("scheduleType", preferences.scheduleType().name)
                put("units", preferences.units().name)
            },
        )
        put("performanceEligible", profile.performanceEligible())
    }

    fun decode(text: String): UserProfile = read(StoredJson.objectValue(text))

    internal fun read(row: JsonObject): UserProfile {
        val goal = row.getValue("goal").row()
        val preferences = row.getValue("preferences").row()
        return UserProfile(
            RaceGoal(
                RaceDistance.valueOf(goal.string("distance")),
                goal.text("raceName"),
                goal.text("raceDate")?.let { StoredDate.parse(it) },
                GoalMode.valueOf(goal.string("mode")),
                goal.text("targetTime")?.let { StoredDuration.parse(it) },
            ),
            TrainingPreferences(
                TrainingStrategy.valueOf(preferences.string("strategy")),
                days(preferences, "runningDays"),
                preferences.requiredInteger("runsPerWeek"),
                preferences.text("longRunDay")?.let { DayOfWeek.valueOf(it) },
                days(preferences, "gymDays"),
                ScheduleType.valueOf(preferences.string("scheduleType")),
                UnitSystem.valueOf(preferences.string("units")),
            ),
            row.bool("performanceEligible"),
        )
    }

    private fun days(row: JsonObject, key: String): Set<DayOfWeek> {
        val values =
            row.array(key, 7).map {
                require(it is JsonPrimitive && it.isString)
                DayOfWeek.valueOf(it.content)
            }
        require(values.distinct().size == values.size) { "Duplicate availability day." }
        return values.toSet()
    }
}
