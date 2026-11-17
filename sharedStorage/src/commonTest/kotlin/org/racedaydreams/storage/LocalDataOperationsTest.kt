package org.racedaydreams.storage

import kotlin.test.*

class LocalDataOperationsTest {
    @Test
    fun anotherThreadCannotOverlapBackupOrAcquireCleanupAndExplicitRetryWorksAfterRelease() {
        val operations = LocalDataOperations()
        withOtherThreadHoldingDataOperation(operations) {
            assertTrue(operations.replacementInProgress())
            assertFails { operations.exclusive { error("Other thread must not delete") } }
            assertFails { operations.replace { error("Other thread must not replace") } }
        }
        assertFalse(operations.replacementInProgress())
        assertTrue(operations.exclusive { true })
    }

    @Test
    fun backupReplacementAndCleanupCannotOverlapEvenOnTheSameThread() {
        val operations = LocalDataOperations()
        assertFalse(operations.replacementInProgress())
        operations.backup {
            assertTrue(operations.replacementInProgress())
            assertFails { operations.exclusive { error("Must not run cleanup") } }
            assertFails { operations.replace { error("Must not replace") } }
            assertTrue(operations.replacementInProgress())
        }
        operations.replace {
            assertTrue(operations.replacementInProgress())
            assertFails { operations.backup { error("Must not export") } }
            assertFails { operations.exclusive { error("Must not delete") } }
        }
        assertTrue(
            operations.exclusive {
                assertFalse(operations.replacementInProgress())
                assertFails { operations.backup { error("Must not export") } }
                assertFails { operations.exclusive { error("Must not repeat cleanup") } }
                assertFalse(operations.replacementInProgress())
                true
            }
        )
        assertFalse(operations.replacementInProgress())
    }

    @Test
    fun operationFailureReleasesTheGuardAndNeverReplaysARejectedAction() {
        val operations = LocalDataOperations()
        var invoked = 0
        assertFails {
            operations.replace {
                invoked++
                error("Replacement failed")
            }
        }
        assertFalse(operations.replacementInProgress())
        operations.backup { invoked++ }
        assertFalse(
            operations.exclusive {
                invoked++
                false
            }
        )
        assertEquals(3, invoked)
        assertFalse(operations.replacementInProgress())
    }
}

internal expect fun withOtherThreadHoldingDataOperation(
    operations: LocalDataOperations,
    inspect: () -> Unit,
)
