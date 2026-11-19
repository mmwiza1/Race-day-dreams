package org.racedaydreams.domain.presentation

internal actual fun prescriptionNumber(value: Double): String {
    if (!value.isFinite()) throw NumberFormatException("A finite decimal value is required.")
    return expandPrescriptionDecimal(value.toString())
}

internal actual fun prescriptionDifference(next: Double, current: Double): String {
    require(next >= 0 && current >= 0)
    return differencePrescriptionDecimals(prescriptionNumber(next), prescriptionNumber(current))
}
