package org.racedaydreams.coach.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.health.HealthConnectGateway

/** Optional Health Connect orchestration; core training does not depend on permission. */
class HealthCoordinator(
    private val operations: CoachOperationRunner,
    private val health: HealthConnectGateway,
    private val imports: org.racedaydreams.domain.health.HealthImportRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val development: DevelopmentStore,
) {
    fun syncHealth() =
        operations.submit {
            val result = health.sync(imports)
            if (HealthConnectGateway.WEIGHT_PERMISSION in health.grantedPermissions())
                health.syncWeights(development)
            operations.update { it.copy(message = result.message) }
        }

    fun permissionResult(granted: Set<String>) {
        if (granted.isEmpty()) {
            operations.update {
                it.copy(
                    message =
                        "Health access was not granted. You can continue with optional manual details and use the app without it."
                )
            }
            operations.refresh()
        } else syncHealth()
    }

    suspend fun beforeRefresh(autoImport: Boolean) {
        if (autoImport && health.availability() == HealthAvailability.AVAILABLE) {
            val granted = health.grantedPermissions()
            val needsReview =
                withContext(Dispatchers.IO) { settings.getSetting("baseline.reviewed") != "true" }
            if (needsReview && granted.isNotEmpty()) {
                val result = withContext(Dispatchers.IO) { health.sync(imports) }
                operations.update { it.copy(message = result.message) }
            }
        }
    }
}
