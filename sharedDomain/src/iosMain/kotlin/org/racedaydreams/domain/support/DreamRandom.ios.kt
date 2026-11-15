@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.domain.support

import platform.Foundation.NSLock

actual open class DreamRandom internal constructor(seed: Long) {
    private val source = PortableDreamRandom(seed)
    private val lock = NSLock()

    actual open fun nextInt(bound: Int): Int {
        lock.lock()
        try {
            return source.nextInt(bound)
        } finally {
            lock.unlock()
        }
    }

    actual open fun nextDouble(): Double {
        lock.lock()
        try {
            return source.nextDouble()
        } finally {
            lock.unlock()
        }
    }
}

actual object DreamRandoms {
    actual fun seeded(seed: Long): DreamRandom = DreamRandom(seed)
}
