package org.racedaydreams.domain.body

enum class HeightUnit(private val metres: Double, private val displayLabel: String) {
    CENTIMETRES(0.01, "cm"),
    INCHES(0.0254, "in");

    fun label(): String = displayLabel

    fun toMetres(value: Double): Double {
        require(value.isFinite() && value > 0) { "Enter a positive finite height." }
        return value * metres
    }

    fun fromMetres(value: Double): Double {
        require(value.isFinite() && value > 0) { "Height is unavailable." }
        return value / metres
    }
}
