@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.storage

import kotlin.test.*
import platform.darwin.*

internal actual fun withOtherThreadHoldingDataOperation(
    operations: LocalDataOperations,
    inspect: () -> Unit,
) {
    val started = dispatch_semaphore_create(0)
    val release = dispatch_semaphore_create(0)
    val done = dispatch_semaphore_create(0)
    var failure: Throwable? = null
    fun deadline() = dispatch_time(DISPATCH_TIME_NOW, 10000000000L)
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
        try {
            operations.backup {
                dispatch_semaphore_signal(started)
                check(dispatch_semaphore_wait(release, deadline()) == 0L) {
                    "Test release timed out"
                }
            }
        } catch (t: Throwable) {
            failure = t
        } finally {
            dispatch_semaphore_signal(done)
        }
    }
    try {
        assertEquals(
            0L,
            dispatch_semaphore_wait(started, deadline()),
            "Worker did not acquire operation",
        )
        inspect()
    } finally {
        dispatch_semaphore_signal(release)
        assertEquals(
            0L,
            dispatch_semaphore_wait(done, deadline()),
            "Worker did not release operation",
        )
    }
    failure?.let { throw it }
}
