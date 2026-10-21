package org.racedaydreams.health

import org.racedaydreams.domain.health.HealthImportRepository
import java.time.Instant

enum class HealthAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }
enum class HealthSyncStatus { SUCCESS, PARTIAL, PERMISSION_REQUIRED, UNAVAILABLE, UPDATE_REQUIRED, FAILED }

data class HealthSyncResult(
    val status: HealthSyncStatus,
    val importedCount: Int = 0,
    val windowStart: Instant? = null,
    val windowEnd: Instant? = null,
    val message: String
)

/** Presentation/import boundary. Future providers need not expose Health Connect record types. */
interface HealthGateway {
    fun availability(): HealthAvailability
    suspend fun grantedPermissions(): Set<String>
    suspend fun sync(store: HealthImportRepository): HealthSyncResult
}
