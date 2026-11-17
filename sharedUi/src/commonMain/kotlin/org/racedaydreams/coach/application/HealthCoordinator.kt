package org.racedaydreams.coach.application

import kotlinx.coroutines.withContext
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.health.HealthGateway

/** Optional platform health orchestration; core training never depends on health access. */
class HealthCoordinator(
    private val operations: CoachOperationRunner,
    private val health: HealthGateway,
    private val imports: org.racedaydreams.domain.health.HealthImportRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val bodyPermissions: Set<String>,
    private val syncBodyMeasurements: suspend () -> String,
) {
    fun syncHealth() =
        operations.submit {
            val result = health.sync(imports)
            val bodyMessage =
                if (health.grantedPermissions().any { it in bodyPermissions })
                    syncBodyMeasurements()
                else null
            operations.update {
                it.copy(message = listOfNotNull(result.message, bodyMessage).joinToString("\n"))
            }
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
                withContext(applicationIoDispatcher()) {
                    settings.getSetting("baseline.reviewed") != "true"
                }
            if (needsReview && granted.isNotEmpty()) {
                val result = withContext(applicationIoDispatcher()) { health.sync(imports) }
                operations.update { it.copy(message = result.message) }
            }
        }
    }
}
