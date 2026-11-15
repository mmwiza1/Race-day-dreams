package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.text

/** Existing FeatureJson shoe representation. SQL default selection stays authoritative on read. */
object ShoeJson {
    fun encode(shoe: Shoe): String =
        buildJsonObject {
                put("id", shoe.id())
                put("brand", shoe.brand())
                put("model", shoe.model())
                put(
                    "purchaseDate",
                    shoe.purchaseDate()?.let { JsonPrimitive(it.toString()) } ?: JsonNull,
                )
                put(
                    "startingDistanceMetres",
                    shoe.startingDistanceMetres()?.let { JsonPrimitive(it) } ?: JsonNull,
                )
                put("retired", shoe.retired())
                put("notes", shoe.notes()?.let { JsonPrimitive(it) } ?: JsonNull)
                put("defaultShoe", shoe.defaultShoe())
            }
            .toString()

    fun decode(text: String): Shoe {
        val row = StoredJson.objectValue(text)
        return Shoe(
            row.required("id"),
            row.text("brand"),
            row.required("model"),
            row.text("purchaseDate")?.let { StoredDate.parse(it) },
            row.number("startingDistanceMetres"),
            row.bool("retired"),
            row.text("notes"),
            row.bool("defaultShoe"),
        )
    }
}
