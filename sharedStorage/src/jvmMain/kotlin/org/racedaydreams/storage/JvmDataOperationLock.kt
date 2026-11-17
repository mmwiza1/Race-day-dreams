package org.racedaydreams.storage

import java.util.concurrent.locks.ReentrantLock

internal actual fun dataOperationLock(): DataOperationLock =
    object : DataOperationLock {
        private val lock = ReentrantLock()

        override fun tryAcquire() = lock.tryLock()

        override fun release() = lock.unlock()
    }
