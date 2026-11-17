package org.racedaydreams.coach.body

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.*
import java.util.UUID
import org.racedaydreams.data.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.health.*

/** Actual Android Health Connect/lifecycle/clock effects; optional body operations are shared. */
class BodyMeasurementsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SqliteBodyMeasurementsRepository(app)
    private val focus = SqlitePrimaryFocusRepository(app)
    private val gateway = HealthConnectGateway(app)
    private val shared =
        SharedBodyMeasurementsCoordinator(
            repository,
            { focus.load().orElse(null) },
            { gateway.availability() == HealthAvailability.AVAILABLE },
            { gateway.grantedPermissions() },
            { selected ->
                require(selected === repository) {
                    "Use this feature's body repository for syncing."
                }
                gateway.syncBodyMeasurements(repository)
            },
            viewModelScope,
            Instant::now,
            { at -> ZoneId.systemDefault().rules.getOffset(at) },
            { UUID.randomUUID().toString() },
        )
    val state = shared.state

    init {
        refresh()
    }

    fun refresh() = shared.refresh()

    fun moreHeights() = shared.moreHeights()

    fun protein(enabled: Boolean) = shared.protein(enabled)

    fun heightUnit(unit: HeightUnit) = shared.heightUnit(unit)

    fun chooseWeight(id: String) = shared.chooseWeight(id)

    fun saveHeight(value: String, unit: HeightUnit) = shared.saveHeight(value, unit)

    fun saveWeight(value: String, unit: MassUnit) = shared.saveWeight(value, unit)

    fun sync() = shared.sync()
}
