package org.racedaydreams.coach.application

import androidx.compose.runtime.*
import kotlin.math.roundToLong
import org.racedaydreams.coach.body.BodyMeasurementsPresentation
import org.racedaydreams.coach.body.SharedBodyMeasurementsContent
import org.racedaydreams.domain.time.*
import org.racedaydreams.recording.AppleRecordingClock

@Composable
internal fun AppleBodyMeasurements(
    session: AppleCoachSession<*>,
    back: () -> Unit,
    healthControls: @Composable () -> Unit,
) {
    val model = session.body
    val state by model.state.collectAsState()
    LaunchedEffect(model) { model.refresh() }
    SharedBodyMeasurementsContent(
        state,
        onBack = back,
        onProtein = model::protein,
        onWeight = model::chooseWeight,
        onHeightSave = model::saveHeight,
        onWeightSave = model::saveWeight,
        onHeightUnit = model::heightUnit,
        onMoreHeights = model::moreHeights,
        presentation =
            BodyMeasurementsPresentation(
                now = AppleRecordingClock::now,
                manualDate = { AppleCalendarDates.label(AppleCalendarDates.today()) },
                formatNumber = { value ->
                    val tenths = (value * 10).roundToLong()
                    "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
                },
                formatWeightDate = { measurementDate(it.at(), it.offset()) },
                formatHeightDate = { measurementDate(it.at(), it.offset()) },
                formatEvidenceTime = { it.toString() },
                heightIsOld = {
                    val now = AppleRecordingClock.now()
                    it.at()
                        .isBefore(
                            Instants.ofEpochSecond(
                                now.getEpochSecond() - 365 * 86_400L,
                                now.getNano().toLong(),
                            )
                        )
                },
                basisAccessNote = {
                    if (it.provider() == "health-kit") "Retained Apple Health measurement."
                    else null
                },
                weightAccessNote = { null },
                heightAccessNote = { _, _ -> null },
            ),
        providerControls = healthControls,
    )
}

private fun measurementDate(at: Instant, offset: ZoneOffset?): String {
    val date = CalendarDates.atOffset(at, offset ?: Offsets.ofTotalSeconds(0))
    return AppleCalendarDates.label(date) + if (offset == null) " (UTC)" else ""
}
