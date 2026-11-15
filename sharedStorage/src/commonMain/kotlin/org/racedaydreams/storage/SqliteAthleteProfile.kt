package org.racedaydreams.storage

import org.racedaydreams.domain.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.settings.SettingsRepository
import org.racedaydreams.domain.time.*

/** Same normalised profile tables and atomic replacement as Android, without SDK/Optional types. */
class SqliteAthleteProfile(private val database: SqlDatabase) {
    fun load(): UserProfile? =
        database.transaction {
            val profile =
                database
                    .query("SELECT performance_eligible FROM user_profile WHERE id=1")
                    .singleOrNull()
            if (profile == null) {
                for (table in listOf("race_goal", "training_preferences", "availability_day")) {
                    check(
                        database.query("SELECT COUNT(*) FROM $table").single().single() ==
                            SqlValue.Integer(0)
                    ) {
                        "Saved profile is incomplete; data was retained."
                    }
                }
                null
            } else {
                val eligible = (profile.single() as SqlValue.Integer).value
                check(eligible == 0L || eligible == 1L) {
                    "Saved eligibility is invalid; data was retained."
                }
                val goal =
                    database
                        .query(
                            "SELECT distance,race_name,race_date,goal_mode,target_seconds FROM race_goal WHERE user_id=1"
                        )
                        .single()
                val preferences =
                    database
                        .query(
                            "SELECT strategy,runs_per_week,long_run_day,schedule_type,units FROM training_preferences WHERE user_id=1"
                        )
                        .single()
                UserProfile(
                    RaceGoal(
                        RaceDistance.valueOf(goal.text(0)),
                        goal.text(1),
                        goal.nullableText(2)?.let { date(it) },
                        GoalMode.valueOf(goal.text(3)),
                        goal.nullableInteger(4)?.let { Durations.ofSeconds(it) },
                    ),
                    TrainingPreferences(
                        TrainingStrategy.valueOf(preferences.text(0)),
                        days("RUN"),
                        preferences.integer(1).toInt(),
                        preferences.nullableInteger(2)?.let { day(it) },
                        days("GYM"),
                        ScheduleType.valueOf(preferences.text(3)),
                        UnitSystem.valueOf(preferences.text(4)),
                    ),
                    eligible == 1L,
                )
            }
        }

    fun save(profile: UserProfile) =
        database.transaction {
            // Cascade only the normalised profile children, never workout/programme history.
            database.execute("DELETE FROM user_profile WHERE id=1")
            database.execute(
                "INSERT INTO user_profile VALUES(1,?)",
                listOf(SqlValue.Integer(if (profile.performanceEligible()) 1 else 0)),
            )
            val goal = profile.raceGoal()
            database.execute(
                "INSERT INTO race_goal(user_id,distance,race_name,race_date,goal_mode,target_seconds) VALUES(1,?,?,?,?,?)",
                listOf(
                    SqlValue.Text(goal.distance().name),
                    SqlValue.Text(goal.raceName()),
                    text(goal.raceDate()?.toString()),
                    SqlValue.Text(goal.mode().name),
                    integer(goal.targetTime()?.getSeconds()),
                ),
            )
            val preferences = profile.preferences()
            database.execute(
                "INSERT INTO training_preferences(user_id,strategy,runs_per_week,long_run_day,schedule_type,units) VALUES(1,?,?,?,?,?)",
                listOf(
                    SqlValue.Text(preferences.strategy().name),
                    SqlValue.Integer(preferences.runsPerWeek().toLong()),
                    integer(preferences.longRunDay()?.getValue()?.toLong()),
                    SqlValue.Text(preferences.scheduleType().name),
                    SqlValue.Text(preferences.units().name),
                ),
            )
            for ((kind, days) in
                listOf("RUN" to preferences.runningDays(), "GYM" to preferences.gymDays())) {
                for (day in days) database.execute(
                    "INSERT INTO availability_day VALUES(1,?,?)",
                    listOf(SqlValue.Text(kind), SqlValue.Integer(day.getValue().toLong())),
                )
            }
            database.execute("DELETE FROM onboarding_draft")
        }

    private fun days(kind: String): Set<DayOfWeek> =
        database
            .query(
                "SELECT day FROM availability_day WHERE user_id=1 AND kind=? ORDER BY day",
                listOf(SqlValue.Text(kind)),
            )
            .map { day(it.integer(0)) }
            .toSet()

    private fun day(value: Long): DayOfWeek =
        DayOfWeek.entries.single { it.getValue().toLong() == value }

    private fun date(text: String): LocalDate {
        val parts =
            Regex("([+-]?[0-9]{4,9})-([0-9]{2})-([0-9]{2})").matchEntire(text)?.groupValues
                ?: throw IllegalArgumentException("Invalid stored race date; data was retained.")
        val date = CalendarDates.of(parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
        require(date.toString() == text)
        return date
    }

    private fun text(value: String?): SqlValue = value?.let { SqlValue.Text(it) } ?: SqlValue.Null

    private fun integer(value: Long?): SqlValue =
        value?.let { SqlValue.Integer(it) } ?: SqlValue.Null

    private fun List<SqlValue>.text(index: Int): String = (this[index] as SqlValue.Text).value

    private fun List<SqlValue>.integer(index: Int): Long = (this[index] as SqlValue.Integer).value

    private fun List<SqlValue>.nullableText(index: Int): String? =
        if (this[index] == SqlValue.Null) null else text(index)

    private fun List<SqlValue>.nullableInteger(index: Int): Long? =
        if (this[index] == SqlValue.Null) null else integer(index)
}

class SqlitePrimaryFocus(private val settings: SettingsRepository) {
    fun load(): PrimaryFocus? {
        val current = settings.getSetting("primary.focus")
        return if (current == null)
            PrimaryFocus.fromLegacyStartingPath(settings.getSetting("starting.path"))
        else PrimaryFocus.fromStorageValue(current)
    }

    fun save(focus: PrimaryFocus) = settings.putSetting("primary.focus", focus.storageValue())
}
