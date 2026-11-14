@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.domain.strength

import platform.Foundation.NSRecursiveLock

internal actual class GymExecutionMonitor {
    private val monitor = NSRecursiveLock()

    actual fun <T> locked(action: () -> T): T {
        monitor.lock()
        try {
            return action()
        } finally {
            monitor.unlock()
        }
    }
}
