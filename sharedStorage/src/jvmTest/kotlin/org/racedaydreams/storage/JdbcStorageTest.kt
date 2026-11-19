package org.racedaydreams.storage

import java.sql.DriverManager
import kotlin.test.*

class JdbcStorageTest : StorageContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")

    @Test
    fun realFileSurvivesClosingAndReopening() {
        val file = java.nio.file.Files.createTempFile("rdd-storage-", ".db").toFile()
        try {
            JdbcDatabase("jdbc:sqlite:${file.absolutePath}").also {
                ApplicationSchema.open(it) { choice() }
                SqliteSettings(it).putSetting("persisted", "actual")
                it.close()
            }
            val reopened = JdbcDatabase("jdbc:sqlite:${file.absolutePath}")
            try {
                ApplicationSchema.open(reopened) { error("Not a new install") }
                assertEquals("actual", SqliteSettings(reopened).getSetting("persisted"))
            } finally {
                reopened.close()
            }
        } finally {
            file.delete()
        }
    }
}

/** Test-only SQLite oracle; Android keeps its existing SQLiteOpenHelper and repositories. */
internal class JdbcDatabase(url: String) : SqlDatabase {
    private val connection = DriverManager.getConnection(url)
    private var depth = 0

    private fun bind(statement: java.sql.PreparedStatement, arguments: List<SqlValue>) {
        arguments.forEachIndexed { i, value ->
            when (value) {
                SqlValue.Null -> statement.setObject(i + 1, null)
                is SqlValue.Integer -> statement.setLong(i + 1, value.value)
                is SqlValue.Real -> statement.setDouble(i + 1, value.value)
                is SqlValue.Text -> statement.setString(i + 1, value.value)
                is SqlValue.Blob -> statement.setBytes(i + 1, value.value.toByteArray())
            }
        }
    }

    override fun execute(sql: String, arguments: List<SqlValue>) {
        connection.prepareStatement(sql).use {
            bind(it, arguments)
            it.executeUpdate()
        }
    }

    override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> =
        connection.prepareStatement(sql).use { statement ->
            bind(statement, arguments)
            statement.executeQuery().use { cursor ->
                buildList {
                    while (cursor.next()) add(
                        (1..cursor.metaData.columnCount).map { column ->
                            when (val value = cursor.getObject(column)) {
                                null -> SqlValue.Null
                                is Int -> SqlValue.Integer(value.toLong())
                                is Long -> SqlValue.Integer(value)
                                is Double -> SqlValue.Real(value)
                                is String -> SqlValue.Text(value)
                                is ByteArray -> SqlValue.Blob(value.toList())
                                else -> error("Unexpected SQLite value")
                            }
                        }
                    )
                }
            }
        }

    override fun <T> transaction(block: () -> T): T {
        val level = depth++
        execute(if (level == 0) "BEGIN IMMEDIATE" else "SAVEPOINT nested_$level")
        try {
            val result = block()
            execute(if (level == 0) "COMMIT" else "RELEASE nested_$level")
            return result
        } catch (error: Throwable) {
            if (level == 0) execute("ROLLBACK")
            else {
                execute("ROLLBACK TO nested_$level")
                execute("RELEASE nested_$level")
            }
            throw error
        } finally {
            depth--
        }
    }

    override fun close() = connection.close()
}

class JdbcActivityRepositoryTest : ActivityRepositoryContract() {
    @Test
    fun actualActivityAndDerivedEvidenceSurviveFileReopen() {
        val file = java.nio.file.Files.createTempFile("rdd-activity-", ".db").toFile()
        val original = activity()
        try {
            val first = JdbcDatabase("jdbc:sqlite:${file.absolutePath}")
            try {
                ApplicationSchema.open(first) {
                    ApplicationSchema.NewInstallation("2026-11-15T12:00:00Z", "file-test")
                }
                val repository =
                    SqliteActivities(first) {
                        error("Manual activity does not require wall-clock invention")
                    }
                repository.upsertManualActivity(original)
                repository.saveActivityNotes(original.id(), "persistent annotation", 7.5)
            } finally {
                first.close()
            }
            val second = JdbcDatabase("jdbc:sqlite:${file.absolutePath}")
            try {
                ApplicationSchema.open(second) { error("Not a new install") }
                val repository = SqliteActivities(second) { error("Read must not invent time") }
                assertEquals(
                    original.copy(notes = "persistent annotation", rpe = 7.5),
                    repository.activityDetail(original.id()),
                )
                assertTrue(SqliteResults(second).results().isNotEmpty())
            } finally {
                second.close()
            }
        } finally {
            file.delete()
        }
    }

    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
