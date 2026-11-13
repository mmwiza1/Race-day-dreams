package org.racedaydreams.coach.body

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.*
import java.time.format.*
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.health.*

/** Optional feature ownership: no training plan, body assessment or external account dependency. */
class BodyMeasurementsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SqliteBodyMeasurementsRepository(app)
    private val focus = SqlitePrimaryFocusRepository(app)
    private val gateway = HealthConnectGateway(app)
    private val mutable = MutableStateFlow(BodyMeasurementsState())
    val state: StateFlow<BodyMeasurementsState> = mutable.asStateFlow()
    private var operation: Job? = null
    private var heightLimit = 20

    init {
        refresh()
    }

    private suspend fun snapshot(notice: String? = null): BodyMeasurementsState {
        val weights =
            repository
                .weights()
                .sortedWith(compareByDescending<BodyWeight> { it.at() }.thenBy { it.id() })
        val preferences = repository.preferences()
        val heights = mutableListOf<BodyHeight>()
        while (heights.size <= heightLimit) {
            val count = minOf(500, heightLimit + 1 - heights.size)
            val page = repository.heights(count, heights.size)
            heights.addAll(page)
            if (page.size < count) break
        }
        return BodyMeasurementsState(
            false,
            heights.take(heightLimit),
            heights.size > heightLimit,
            weights,
            preferences,
            ProteinGuidance.evaluate(
                preferences.proteinEnabled(),
                preferences.basis(),
                weights,
                focus.load().orElse(null),
                Instant.now(),
            ),
            gateway.availability() == HealthAvailability.AVAILABLE,
            gateway.grantedPermissions(),
            notice = notice,
        )
    }

    private fun command(work: suspend () -> String?) {
        if (operation?.isActive == true) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        operation =
            viewModelScope.launch {
                try {
                    mutable.value =
                        withContext(Dispatchers.IO) {
                            val notice = work()
                            snapshot(notice)
                        }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    mutable.update {
                        it.copy(
                            busy = false,
                            error =
                                failure.message
                                    ?: "Body measurements could not be saved. Your training history is unchanged.",
                        )
                    }
                }
            }
    }

    fun refresh() = command { null }

    fun moreHeights() = command {
        heightLimit = Math.addExact(heightLimit, 20)
        null
    }

    fun protein(enabled: Boolean) = command {
        val old = repository.preferences()
        repository.preferences(
            BodyMeasurementsRepository.Preferences(enabled, old.heightUnit(), old.basis())
        )
        null
    }

    fun heightUnit(unit: HeightUnit) = command {
        val old = repository.preferences()
        repository.preferences(
            BodyMeasurementsRepository.Preferences(old.proteinEnabled(), unit, old.basis())
        )
        null
    }

    fun chooseWeight(id: String) = command {
        val row =
            repository.weights().firstOrNull { it.id() == id }
                ?: error(
                    "That measurement is no longer available. Refresh and review your records."
                )
        val old = repository.preferences()
        repository.preferences(
            BodyMeasurementsRepository.Preferences(
                old.proteinEnabled(),
                old.heightUnit(),
                ProteinGuidance.Basis.from(row),
            )
        )
        "Weight chosen for general guidance. No training target has changed."
    }

    fun saveHeight(value: String, unit: HeightUnit) = command {
        val now = Instant.now()
        val at = now
        val id = "manual-height:${UUID.randomUUID()}"
        val metres =
            unit.toMetres(
                value.trim().toDoubleOrNull() ?: error("Enter a height in ${unit.label()}.")
            )
        repository.saveHeight(
            BodyHeight(
                id,
                metres,
                at,
                ZoneId.systemDefault().rules.getOffset(at),
                "manual",
                id,
                null,
                null,
            )
        )
        "Height recorded locally. Height does not determine your protein range."
    }

    fun saveWeight(value: String, unit: MassUnit) = command {
        val now = Instant.now()
        val at = now
        val id = "manual-weight:${UUID.randomUUID()}"
        val kg =
            unit.toKilograms(
                value.trim().toDoubleOrNull()
                    ?: error("Enter a weight in ${unit.name.lowercase()}.")
            )
        repository.saveWeight(
            BodyWeight(id, kg, at, ZoneId.systemDefault().rules.getOffset(at), "manual", id, null)
        )
        "Weight recorded locally. Choose it explicitly if you want to use it for protein guidance."
    }

    fun sync() = command { gateway.syncBodyMeasurements(repository) }
}
