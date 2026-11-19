package org.racedaydreams.storage

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

internal actual fun withOtherThreadHoldingDataOperation(
    operations: LocalDataOperations,
    inspect: () -> Unit,
) {
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    val failure = AtomicReference<Throwable?>()
    val worker = Thread {
        try {
            operations.backup {
                started.countDown()
                check(release.await(10, TimeUnit.SECONDS)) { "Test release timed out" }
            }
        } catch (t: Throwable) {
            failure.set(t)
        }
    }
    worker.isDaemon = true
    worker.start()
    try {
        assertTrue(started.await(10, TimeUnit.SECONDS), "Worker did not acquire operation")
        inspect()
    } finally {
        release.countDown()
        worker.join(10000)
        assertFalse(worker.isAlive, "Worker did not release operation")
    }
    failure.get()?.let { throw it }
}
