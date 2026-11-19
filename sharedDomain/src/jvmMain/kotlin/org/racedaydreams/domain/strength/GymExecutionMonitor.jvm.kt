package org.racedaydreams.domain.strength

internal actual class GymExecutionMonitor {
    private val monitor = Any()

    actual fun <T> locked(action: () -> T): T = synchronized(monitor, action)
}
