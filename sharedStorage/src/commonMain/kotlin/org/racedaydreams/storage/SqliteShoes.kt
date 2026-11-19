package org.racedaydreams.storage

import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.ShoeRepository

/** Update instead of REPLACE: shoe assignments must survive edits and default changes. */
class SqliteShoes(private val database: SqlDatabase) : ShoeRepository {
    override fun shoes(): List<Shoe> =
        database.query("SELECT payload,is_default FROM shoes ORDER BY is_default DESC,id").map { row
            ->
            ShoeJson.decode((row[0] as SqlValue.Text).value)
                .copy(defaultShoe = (row[1] as SqlValue.Integer).value == 1L)
        }

    override fun saveShoe(shoe: Shoe) =
        database.transaction {
            require(shoe.id().length <= 256 && shoe.id().any { it.code > 0x20 }) {
                "Invalid shoe ID."
            }
            if (shoe.defaultShoe()) database.execute("UPDATE shoes SET is_default=0")
            val exists =
                database
                    .query("SELECT id FROM shoes WHERE id=?", listOf(SqlValue.Text(shoe.id())))
                    .isNotEmpty()
            val values =
                listOf(
                    SqlValue.Text(ShoeJson.encode(shoe)),
                    SqlValue.Integer(if (shoe.defaultShoe()) 1 else 0),
                    SqlValue.Text(shoe.id()),
                )
            if (exists)
                database.execute("UPDATE shoes SET payload=?,is_default=? WHERE id=?", values)
            else database.execute("INSERT INTO shoes(payload,is_default,id) VALUES(?,?,?)", values)
        }
}
