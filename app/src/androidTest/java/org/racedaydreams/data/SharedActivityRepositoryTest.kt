package org.racedaydreams.data

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.storage.*

/** Both implementations read/write the SAME actual Android database and original payloads. */
@RunWith(AndroidJUnit4::class)
class SharedActivityRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db: SQLiteDatabase
    private lateinit var common: SqliteActivities
    private lateinit var commonResults: SqliteResults
    private lateinit var original: SqliteActivityRepository
    private lateinit var originalResults: SqliteResultRepository
    private lateinit var bridge: AndroidSqlTestConnection

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        bridge = AndroidSqlTestConnection(db)
        common = SqliteActivities(bridge) { Instant.parse("2026-11-15T12:00:00Z") }
        commonResults = SqliteResults(bridge)
        original = SqliteActivityRepository(context)
        originalResults = SqliteResultRepository(context)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun actual(provider: String = "manual"): ActivityRecord {
        val start = Instant.parse("2026-11-15T09:00:00.123456789Z")
        return ActivityRecord(
            "actual",
            SourceIdentity(provider, "record", "local"),
            start,
            start.plusSeconds(600),
            ZoneOffset.UTC,
            null,
            3000.0,
            Duration.ofSeconds(600),
            null,
            null,
            null,
            null,
            6.0,
            "provider note",
            (0..60).map {
                DistanceSample(Duration.ofSeconds(it * 10L), it * 50.0, null, null, null, false)
            },
            emptyList(),
            null,
            null,
        )
    }

    private fun result(source: ResultSource = ResultSource.OFFICIAL_CHIP) =
        RecordedResult(
            "race",
            PerformanceDistance.FIVE_K,
            Duration.ofSeconds(1500, 123456789),
            LocalDate.of(2026, 11, 15),
            null,
            source,
            if (source == ResultSource.ROLLING) "actual" else null,
            "retained",
        )

    @Test
    fun originalAndCommonRepositoriesShareActualsPbsAnnotationsAndShoes() {
        SqliteShoeRepository(context)
            .saveShoe(Shoe("shoe", "Brand", "Daily", null, 0.0, false, null, true))
        original.upsertManualActivity(actual())
        assertEquals(original.activities(), common.activities())
        assertEquals(originalResults.results(), commonResults.results())
        assertTrue(commonResults.results().isNotEmpty())
        common.saveActivityNotes("actual", "athlete note", 7.5)
        common.assignShoe("actual", null)
        assertEquals(original.activityDetail("actual"), common.activityDetail("actual"))
        assertEquals("athlete note", original.activityDetail("actual")!!.notes())
        assertNull(original.activityDetail("actual")!!.shoeId())
        commonResults.saveResult(result())
        assertEquals(originalResults.results(), commonResults.results())
        original.saveActivityNotes("actual", null, null)
        assertEquals(original.activityDetail("actual"), common.activityDetail("actual"))
    }

    @Test
    fun commonAndOriginalWritesRollBackWithTheSameOuterTransaction() {
        db.beginTransaction()
        try {
            SqliteShoes(bridge)
                .saveShoe(Shoe("shoe", "Brand", "Daily", null, null, false, null, true))
            original.upsertManualActivity(actual())
            common.saveActivityNotes("actual", "temporary", 8.0)
            commonResults.saveResult(result())
            assertFalse(original.activities().isEmpty())
        } finally {
            db.endTransaction()
        }
        assertTrue(original.activities().isEmpty())
        assertTrue(common.activities().isEmpty())
        assertTrue(originalResults.results().isEmpty())
        assertTrue(commonResults.results().isEmpty())
        assertTrue(SqliteShoeRepository(context).shoes().isEmpty())
    }

    @Test
    fun providerReimportPreservesTheOriginalOverrideAndSourceIdentity() {
        original.applyImport(
            "health-connect",
            listOf(actual("health-connect")),
            emptyList(),
            "original",
        )
        original.saveActivityNotes("actual", "user-owned", 8.0)
        common.applyImport(
            "health-connect",
            listOf(actual("health-connect").copy(notes = "provider changed")),
            emptyList(),
            "shared",
        )
        assertEquals("shared", original.importToken("health-connect"))
        assertEquals(original.activityDetail("actual"), common.activityDetail("actual"))
        assertEquals("user-owned", original.activityDetail("actual")!!.notes())
        assertThrows(IllegalArgumentException::class.java) {
            common.applyImport(
                "health-connect",
                listOf(actual("health-connect").copy(id = "conflict")),
                emptyList(),
                "bad",
            )
        }
        assertEquals("shared", original.importToken("health-connect"))
        assertEquals(originalResults.results(), commonResults.results())
    }

    @Test
    fun originalRecordingPayloadHydratesIdenticallyWithoutChangingSummaries() {
        val activity = actual("rdd")
        original.upsertManualActivity(activity)
        db.execSQL(
            "INSERT INTO recording_sessions VALUES('raw','FINISHED',?,1000,601000,'actual')",
            arrayOf(activity.start().toString()),
        )
        val points =
            (0..2).map { index ->
                Point(
                    Fix(
                        activity.start().plusSeconds(index * 10L),
                        1000 + index * 10000L,
                        51.0 + index * .001,
                        -.1,
                        null,
                        null,
                        null,
                        null,
                    ),
                    index * 50.0,
                    index * 10000L,
                    index,
                    index > 0,
                    null,
                    0.0,
                )
            }
        points.forEachIndexed { index, point ->
            db.execSQL(
                "INSERT INTO recording_points VALUES('raw',?,?)",
                arrayOf<Any?>(index, RecordingStore.encode(point).toString()),
            )
            assertEquals(point, RecordingPointJson.decode(RecordingStore.encode(point).toString()))
            assertEquals(point, RecordingStore.decode(JSONObject(RecordingPointJson.encode(point))))
        }
        assertEquals(original.activitySummaries(), common.activitySummaries())
        assertTrue(common.activitySummaries().single().samples().isEmpty())
        assertEquals(original.activityDetail("actual"), common.activityDetail("actual"))
        assertEquals(3000.0, common.activityDetail("actual")!!.distanceMetres()!!, 0.0)
        assertEquals(
            100.0,
            common.activityDetail("actual")!!.samples().last().distanceMetres(),
            0.0,
        )
    }

    @Test
    fun resultCodecsReadOriginalOutputAndManualRulesMatch() {
        for (source in ResultSource.entries) {
            val recorded = result(source)
            assertEquals(recorded, ResultJson.decode(FeatureJson.result(recorded).toString()))
            assertEquals(recorded, FeatureJson.result(JSONObject(ResultJson.encode(recorded))))
        }
        common.upsertManualActivity(actual())
        val derived = originalResults.results().first()
        assertThrows(IllegalArgumentException::class.java) {
            commonResults.saveResult(result().copy(id = derived.id()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            originalResults.saveResult(result().copy(id = derived.id()))
        }
        assertEquals(originalResults.results(), commonResults.results())
    }
}

/**
 * Test bridge only; production Android retains its existing helper, IO checks and pool lifetime.
 */
private class AndroidSqlTestConnection(private val database: SQLiteDatabase) : SqlDatabase {
    private var sequence = 0

    override fun execute(sql: String, arguments: List<SqlValue>) {
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
        database
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
        if (database.inTransaction()) {
            val name = "shared_test_${++sequence}"
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

    override fun close() = database.close()
}
