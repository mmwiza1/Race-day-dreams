package org.racedaydreams.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.storage.SqlValue

@RunWith(AndroidJUnit4::class)
class AndroidSqlConnectionTest {
    private fun database(block: (SQLiteDatabase) -> Unit) {
        val db = SQLiteDatabase.create(null)
        try {
            block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun lazyBorrowedViewDoesNotOpenOrCloseTheApplicationPool() = database { db ->
        var reads = 0
        val connection = AndroidSqlConnection {
            reads++
            db
        }
        assertEquals(0, reads)
        connection.close()
        assertEquals(0, reads)
        assertTrue(db.isOpen)
        assertEquals(listOf(listOf(SqlValue.Integer(1))), connection.query("SELECT 1"))
        connection.close()
        assertTrue(db.isOpen)
    }

    @Test
    fun typedValuesKeepSqlNullExactLongUnicodeRealAndBlobAcrossAndroidCursors() = database { db ->
        val connection = AndroidSqlConnection { db }
        connection.execute("CREATE TABLE typed(n,l INTEGER,f REAL,t TEXT,b BLOB)")
        val values =
            listOf(
                SqlValue.Null,
                SqlValue.Integer(9007199254740993L),
                SqlValue.Real(4.25),
                SqlValue.Text("Bridge / café 🏃"),
                SqlValue.Blob(listOf(0, 127, -128, -1).map { it.toByte() }),
            )
        connection.execute("INSERT INTO typed VALUES(?,?,?,?,?)", values)
        assertEquals(listOf(values), connection.query("SELECT * FROM typed"))
    }

    @Test
    fun nestedFailureAcrossTwoViewsRollsBackOnlyItsSavepointAndOuterRejectionStillRollsBack() =
        database { db ->
            val first = AndroidSqlConnection { db }
            val second = AndroidSqlConnection { db }
            first.execute("CREATE TABLE history(value TEXT)")
            first.transaction {
                first.execute("INSERT INTO history VALUES('first')")
                try {
                    second.transaction {
                        second.execute("INSERT INTO history VALUES('reject')")
                        error("Reject inner")
                    }
                    fail("Expected inner rejection")
                } catch (expected: IllegalStateException) {
                    assertEquals("Reject inner", expected.message)
                }
                first.execute("INSERT INTO history VALUES('last')")
            }
            val before = listOf(listOf(SqlValue.Text("first")), listOf(SqlValue.Text("last")))
            assertEquals(before, first.query("SELECT value FROM history ORDER BY rowid"))
            try {
                first.transaction {
                    second.execute("INSERT INTO history VALUES('reject outer')")
                    error("Reject outer")
                }
                fail("Expected outer rejection")
            } catch (expected: IllegalStateException) {
                assertEquals("Reject outer", expected.message)
            }
            assertEquals(before, first.query("SELECT value FROM history ORDER BY rowid"))
        }

    @Test
    fun callerOwnedAndroidTransactionIsNotCommittedByASharedNestedScope() = database { db ->
        val connection = AndroidSqlConnection { db }
        connection.execute("CREATE TABLE history(value TEXT)")
        db.beginTransaction()
        try {
            connection.transaction {
                connection.execute("INSERT INTO history VALUES('caller rejects')")
            }
        } finally {
            db.endTransaction()
        }
        assertTrue(connection.query("SELECT * FROM history").isEmpty())
    }

    @Test
    fun applicationPoolFactoryRejectsUiThreadSqlBeforeOpeningTheBorrowedDatabase() {
        val context =
            androidx.test.core.app.ApplicationProvider.getApplicationContext<
                android.content.Context
            >()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val connection = AndroidSqlConnection.forApplication(context)
            connection.close()
            try {
                connection.query("SELECT 1")
                fail("Application SQL must be dispatched off the UI thread.")
            } catch (expected: IllegalStateException) {
                assertEquals("Coaching storage requires IO.", expected.message)
            }
        }
    }
}
