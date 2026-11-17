package org.racedaydreams.data

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import org.racedaydreams.storage.SqlDatabase
import org.racedaydreams.storage.SqlValue

/**
 * Non-owning typed view of the EXISTING Android pool. The source is lazy and retains the host's
 * thread/lifecycle checks. Nested scopes use savepoints on the same Android transaction/session.
 */
class AndroidSqlConnection(private val source: () -> SQLiteDatabase) : SqlDatabase {
    private companion object {
        val sequence = java.util.concurrent.atomic.AtomicLong()
    }

    override fun execute(sql: String, arguments: List<SqlValue>) {
        val database = source()
        if (arguments.isEmpty()) database.execSQL(sql)
        else
            database.execSQL(
                sql,
                arguments
                    .map {
                        when (it) {
                            SqlValue.Null -> null
                            is SqlValue.Text -> it.value
                            is SqlValue.Integer -> it.value
                            is SqlValue.Real -> it.value
                            is SqlValue.Blob -> it.value.toByteArray()
                        }
                    }
                    .toTypedArray(),
            )
    }

    override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> =
        source()
            .rawQueryWithFactory(
                { _, driver, table, query ->
                    arguments.forEachIndexed { index, value ->
                        when (value) {
                            SqlValue.Null -> query.bindNull(index + 1)
                            is SqlValue.Integer -> query.bindLong(index + 1, value.value)
                            is SqlValue.Real -> query.bindDouble(index + 1, value.value)
                            is SqlValue.Text -> query.bindString(index + 1, value.value)
                            is SqlValue.Blob -> query.bindBlob(index + 1, value.value.toByteArray())
                        }
                    }
                    SQLiteCursor(driver, table.orEmpty(), query)
                },
                sql,
                null,
                "",
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(
                        List(cursor.columnCount) { index ->
                            when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_NULL -> SqlValue.Null
                                Cursor.FIELD_TYPE_INTEGER -> SqlValue.Integer(cursor.getLong(index))
                                Cursor.FIELD_TYPE_FLOAT -> SqlValue.Real(cursor.getDouble(index))
                                Cursor.FIELD_TYPE_STRING -> SqlValue.Text(cursor.getString(index))
                                Cursor.FIELD_TYPE_BLOB ->
                                    SqlValue.Blob(cursor.getBlob(index).toList())
                                else -> error("Unknown SQLite value")
                            }
                        }
                    )
                }
            }

    override fun <T> transaction(block: () -> T): T {
        val database = source()
        if (database.inTransaction()) {
            val name = "rdd_shared_${sequence.incrementAndGet()}"
            database.execSQL("SAVEPOINT $name")
            try {
                val value = block()
                database.execSQL("RELEASE $name")
                return value
            } catch (error: Throwable) {
                database.execSQL("ROLLBACK TO $name")
                database.execSQL("RELEASE $name")
                throw error
            }
        }
        database.beginTransactionNonExclusive()
        try {
            val value = block()
            database.setTransactionSuccessful()
            return value
        } finally {
            database.endTransaction()
        }
    }

    // Borrowed connection: only ProfileDatabase/application lifetime may close its shared pool.
    override fun close() = Unit
}
