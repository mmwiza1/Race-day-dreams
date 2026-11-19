package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedStrengthChoicesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var original: StrengthUserStore
    private lateinit var shared: SqliteStrengthChoices
    private lateinit var bridge: AndroidSqlTestConnection

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        bridge = AndroidSqlTestConnection(ProfileDatabase.get(context).writableDatabase)
        original = StrengthUserStore(context)
        shared = SqliteStrengthChoices(bridge)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    @Test
    fun favouriteAndArchiveChoicesCanBeEditedByEitherStoreWithoutTouchingExclusions() {
        original.save(StrengthUserStore.Choice("bench", true, false))
        shared.save(SqliteStrengthChoices.Choice("pull-up", false, true))
        shared.save(SqliteStrengthChoices.Choice("bench", false, true))
        assertEquals(
            original.choices().map {
                SqliteStrengthChoices.Choice(it.exerciseId(), it.favourite(), it.archived())
            },
            shared.choices(),
        )
        original.save(StrengthUserStore.Choice("bench", true, false))
        assertEquals(SqliteStrengthChoices.Choice("bench", true, false), shared.choices().first())
        assertTrue(original.exclusions().isEmpty())
        assertTrue(bridge.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun acknowledgementsShareStableIdentityAndDoNotAwardFabricatedPersonalBests() {
        original.acknowledge("recorded-bench")
        shared.acknowledge("recorded-bench")
        shared.acknowledge("recorded-pull-up")
        assertEquals(setOf("recorded-bench", "recorded-pull-up"), original.acknowledged())
        assertEquals(original.acknowledged(), shared.acknowledged())
        assertTrue(bridge.query("SELECT id FROM manual_results").isEmpty())
        assertTrue(bridge.query("SELECT kind FROM training_milestones").isEmpty())
    }

    @Test
    fun bothStoresRespectOuterRollbackAndOriginalIdValidation() {
        assertThrows(IllegalArgumentException::class.java) {
            shared.save(SqliteStrengthChoices.Choice("x".repeat(201), true, false))
        }
        assertThrows(IllegalArgumentException::class.java) { shared.acknowledge("x".repeat(501)) }
        val db = ProfileDatabase.get(context).writableDatabase
        db.beginTransaction()
        try {
            original.save(StrengthUserStore.Choice("old", true, false))
            shared.save(SqliteStrengthChoices.Choice("new", true, false))
            original.acknowledge("old-record")
            shared.acknowledge("new-record")
        } finally {
            db.endTransaction()
        }
        assertTrue(original.choices().isEmpty())
        assertTrue(shared.choices().isEmpty())
        assertTrue(original.acknowledged().isEmpty())
        assertTrue(shared.acknowledged().isEmpty())
    }
}
