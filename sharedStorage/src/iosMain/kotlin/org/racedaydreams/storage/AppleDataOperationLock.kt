@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.storage

import platform.Foundation.NSRecursiveLock

internal actual fun dataOperationLock(): DataOperationLock =
    object : DataOperationLock {
        private val lock = NSRecursiveLock()

        override fun tryAcquire() = lock.tryLock()

        override fun release() = lock.unlock()
    }
