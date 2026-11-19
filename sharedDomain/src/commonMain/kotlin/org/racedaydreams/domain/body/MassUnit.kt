package org.racedaydreams.domain.body

enum class MassUnit(private val kilograms: Double) {
    KG(1.0),
    LB(0.45359237);

    fun toKilograms(value: Double): Double {
        require(value.isFinite() && value >= 0) { "Enter a non-negative finite mass." }
        return value * kilograms
    }

    fun fromKilograms(value: Double): Double = value / kilograms
}
