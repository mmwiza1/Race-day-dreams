package org.racedaydreams.storage

import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Offsets

/** Stable recognition and retained source proof over the existing normalised schema. */
class SqliteTrainingMilestones(private val database: SqlDatabase) {
    data class Stored(val award: Award, val recognizedAt: Instant) {
        init {
            require(!recognizedAt.isBefore(award.earnedAt())) { "Recognition precedes evidence." }
        }
    }

    fun awards(): List<Stored> =
        database.transaction {
            database
                .query(
                    "SELECT kind,earned_at,recognized_at FROM training_milestones ORDER BY earned_at DESC,kind"
                )
                .map { row ->
                    val kind = Kind.valueOf(row.text(0))
                    val proof =
                        database
                            .query(
                                "SELECT source_type,record_id,occurred_at,offset_seconds,distance_metres FROM training_milestone_sources WHERE award_kind=? ORDER BY position",
                                listOf(SqlValue.Text(kind.name)),
                            )
                            .map { source ->
                                Proof(
                                    Source.valueOf(source.text(0)),
                                    source.text(1),
                                    StoredInstant.parse(source.text(2)),
                                    if (source[3] == SqlValue.Null) null
                                    else
                                        Offsets.ofTotalSeconds(
                                            (source[3] as SqlValue.Integer).value.toInt()
                                        ),
                                    if (source[4] == SqlValue.Null) null
                                    else (source[4] as SqlValue.Real).value,
                                )
                            }
                    Stored(
                        Award(kind, StoredInstant.parse(row.text(1)), proof),
                        StoredInstant.parse(row.text(2)),
                    )
                }
        }

    /** Existing recognition is neither duplicated nor redated by subsequently imported history. */
    fun recognize(candidates: List<Award>, now: Instant): Int =
        database.transaction {
            val known = awards().map { it.award.kind() }.toMutableSet()
            var added = 0
            for (candidate in candidates) {
                if (known.add(candidate.kind())) {
                    val stored = Stored(candidate, now)
                    database.execute(
                        "INSERT INTO training_milestones(kind,earned_at,recognized_at) VALUES(?,?,?)",
                        listOf(
                            SqlValue.Text(candidate.kind().name),
                            SqlValue.Text(candidate.earnedAt().toString()),
                            SqlValue.Text(stored.recognizedAt.toString()),
                        ),
                    )
                    candidate.evidence().forEachIndexed { position, proof ->
                        database.execute(
                            "INSERT INTO training_milestone_sources(award_kind,position,source_type,record_id,occurred_at,offset_seconds,distance_metres) VALUES(?,?,?,?,?,?,?)",
                            listOf(
                                SqlValue.Text(candidate.kind().name),
                                SqlValue.Integer(position.toLong()),
                                SqlValue.Text(proof.source().name),
                                SqlValue.Text(proof.recordId()),
                                SqlValue.Text(proof.at().toString()),
                                proof.offset()?.let {
                                    SqlValue.Integer(it.getTotalSeconds().toLong())
                                } ?: SqlValue.Null,
                                proof.distanceMetres()?.let { SqlValue.Real(it) } ?: SqlValue.Null,
                            ),
                        )
                    }
                    added++
                }
            }
            added
        }

    fun celebrations(): Boolean =
        database
            .query("SELECT celebrations FROM training_milestone_preferences WHERE id=1")
            .singleOrNull()
            ?.single() == SqlValue.Integer(1)

    fun celebrations(enabled: Boolean) {
        database.execute(
            "INSERT OR REPLACE INTO training_milestone_preferences VALUES(1,?)",
            listOf(SqlValue.Integer(if (enabled) 1 else 0)),
        )
    }

    private fun List<SqlValue>.text(index: Int): String = (this[index] as SqlValue.Text).value
}
