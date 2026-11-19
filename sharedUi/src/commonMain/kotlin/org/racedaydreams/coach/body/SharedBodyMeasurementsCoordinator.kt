package org.racedaydreams.coach.body

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.racedaydreams.coach.application.applicationIoDispatcher
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.time.*

/**
 * Original optional measurement/protein operations. Provider visibility and IO stay real host
 * effects. Empty Apple permission identifiers remain UNKNOWN, never proof of a denied read.
 */
class SharedBodyMeasurementsCoordinator(
    private val repository: BodyMeasurementsRepository,
    private val primaryFocus: () -> PrimaryFocus?,
    private val bodyAvailable: () -> Boolean,
    private val bodyGrantedPermissions: suspend () -> Set<String>,
    private val syncBodyMeasurements: suspend (BodyMeasurementsRepository) -> String?,
    private val scope: CoroutineScope,
    private val now: () -> Instant,
    private val offsetAt: (Instant) -> ZoneOffset,
    private val newId: () -> String,
    private val ioDispatcher: CoroutineDispatcher = applicationIoDispatcher(),
) {
    private val mutable = MutableStateFlow(BodyMeasurementsState())
    val state: StateFlow<BodyMeasurementsState> = mutable.asStateFlow()
    private var operation: Job? = null
    private var heightLimit = 20

    private fun nextHeightLimit(value: Int): Int {
        if (value > Int.MAX_VALUE - 20) throw ArithmeticException("integer overflow")
        return value + 20
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
                primaryFocus(),
                now(),
            ),
            bodyAvailable(),
            bodyGrantedPermissions(),
            notice = notice,
        )
    }

    private fun command(work: suspend () -> String?) {
        if (operation?.isActive == true) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        operation =
            scope.launch {
                try {
                    mutable.value =
                        withContext(ioDispatcher) {
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
        heightLimit = nextHeightLimit(heightLimit)
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
        val now = now()
        val at = now
        val id = "manual-height:${newId()}"
        val metres =
            unit.toMetres(
                value.trim().toDoubleOrNull() ?: error("Enter a height in ${unit.label()}.")
            )
        repository.saveHeight(BodyHeight(id, metres, at, offsetAt(at), "manual", id, null, null))
        "Height recorded locally. Height does not determine your protein range."
    }

    fun saveWeight(value: String, unit: MassUnit) = command {
        val now = now()
        val at = now
        val id = "manual-weight:${newId()}"
        val kg =
            unit.toKilograms(
                value.trim().toDoubleOrNull()
                    ?: error("Enter a weight in ${unit.name.lowercase()}.")
            )
        repository.saveWeight(BodyWeight(id, kg, at, offsetAt(at), "manual", id, null))
        "Weight recorded locally. Choose it explicitly if you want to use it for protein guidance."
    }

    fun sync() = command { syncBodyMeasurements(repository) }
}
