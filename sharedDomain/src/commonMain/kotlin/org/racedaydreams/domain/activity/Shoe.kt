package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.LocalDate

/** Starting mileage is optional/manual; first recorded run is derived from assignments. */
data class Shoe(
    private var id: String?,
    private var brand: String?,
    private var model: String?,
    private val purchaseDate: LocalDate?,
    private val startingDistanceMetres: Double?,
    private val retired: Boolean,
    private val notes: String?,
    private val defaultShoe: Boolean,
) {
    init {
        id = ActivityChecks.required(id, "Shoe ID")
        brand = brand?.trim { it <= ' ' } ?: ""
        model = ActivityChecks.required(model, "Shoe model").trim { it <= ' ' }
        require(brand!!.length <= 200 && model!!.length <= 200) {
            "Shoe brand/model must be at most 200 characters."
        }
        ActivityChecks.nonNegative(startingDistanceMetres, "Starting shoe mileage")
        require(!retired || !defaultShoe) { "A retired shoe cannot be the default shoe." }
    }

    fun id(): String = id!!

    fun brand(): String = brand!!

    fun model(): String = model!!

    fun purchaseDate(): LocalDate? = purchaseDate

    fun startingDistanceMetres(): Double? = startingDistanceMetres

    fun retired(): Boolean = retired

    fun notes(): String? = notes

    fun defaultShoe(): Boolean = defaultShoe

    override fun toString(): String =
        "Shoe[id=$id, brand=$brand, model=$model, purchaseDate=$purchaseDate, startingDistanceMetres=$startingDistanceMetres, retired=$retired, notes=$notes, defaultShoe=$defaultShoe]"
}
