package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.Shoe

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
        val row =
            Json.parseToJsonElement(text) as? JsonObject
                ?: throw IllegalArgumentException("Expected one complete JSON object.")
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

    private fun JsonObject.text(key: String): String? {
        val value = this[key]
        if (value == null || value == JsonNull) return null
        require(value is JsonPrimitive && value.isString) { "Expected text for $key." }
        return value.content
    }

    private fun JsonObject.required(key: String): String {
        val value = text(key)
        require(value != null && value.length <= 256 && value.any { it.code > 0x20 }) {
            "Invalid $key."
        }
        return value
    }

    private fun JsonObject.number(key: String): Double? {
        val value = this[key]
        if (value == null || value == JsonNull) return null
        require(value is JsonPrimitive && !value.isString) { "Expected a number for $key." }
        val number = value.doubleOrNull
        require(number != null && number.isFinite()) { "Expected a finite number for $key." }
        return number
    }

    private fun JsonObject.bool(key: String): Boolean {
        val value = this[key]
        require(value is JsonPrimitive && !value.isString) { "Expected true or false for $key." }
        return value.booleanOrNull
            ?: throw IllegalArgumentException("Expected true or false for $key.")
    }
}
