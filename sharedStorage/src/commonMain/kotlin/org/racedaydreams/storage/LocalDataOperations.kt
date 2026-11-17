package org.racedaydreams.storage

/**
 * One instance belongs to the application's data composition and must wrap all backup/export,
 * replacement and cleanup operations. No waiting or queued replay of stale destructive actions.
 * Native UI wiring remains disabled until that owner and all platform operation paths use it.
 */
internal class LocalDataOperations : RecordingDetailOperation {
    private enum class Kind {
        BACKUP,
        REPLACEMENT,
        CLEANUP,
    }

    private val lock = dataOperationLock()
    private var current: Kind? = null

    fun <T> backup(block: () -> T): T = run(Kind.BACKUP, block)

    fun <T> replace(block: () -> T): T = run(Kind.REPLACEMENT, block)

    /** Exclusive non-replacing review; the domain approval recheck must see its own scope idle. */
    fun <T> review(block: () -> T): T = run(Kind.CLEANUP, block)

    override fun exclusive(block: () -> Boolean): Boolean = run(Kind.CLEANUP, block)

    override fun replacementInProgress(): Boolean {
        // Another thread owns a data operation: conservatively unavailable. The current cleanup
        // may recheck its own state through the recursive lock without mistaking itself for
        // restore.
        if (!lock.tryAcquire()) return true
        return try {
            current != null && current != Kind.CLEANUP
        } finally {
            lock.release()
        }
    }

    private fun <T> run(kind: Kind, block: () -> T): T {
        check(lock.tryAcquire()) {
            "Another data operation is running. Try again when it finishes."
        }
        try {
            // Recursive platform locks support state inspection, not overlapping nested operations.
            check(current == null) {
                "Another data operation is running. Try again when it finishes."
            }
            current = kind
            return try {
                block()
            } finally {
                current = null
            }
        } finally {
            lock.release()
        }
    }
}

internal interface DataOperationLock {
    fun tryAcquire(): Boolean

    fun release()
}

internal expect fun dataOperationLock(): DataOperationLock
