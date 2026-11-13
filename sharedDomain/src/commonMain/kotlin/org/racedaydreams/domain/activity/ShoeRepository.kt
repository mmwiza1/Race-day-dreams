package org.racedaydreams.domain.activity

/** Existing typed persistence boundary; platform owns IO and transactions. */
interface ShoeRepository {
    fun shoes(): List<Shoe>

    fun saveShoe(shoe: Shoe)
}
