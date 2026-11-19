package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.activity.Shoe

class ShoeJsonTest {
    @Test
    fun existingAndroidPayloadKeepsUnknownVersusZeroAndEscapedText() {
        val original =
            """{"id":"shoe-1","brand":"Brand","model":"Daily runner","purchaseDate":"2026-02-28","startingDistanceMetres":null,"retired":false,"notes":"café 🏃\nquote \" and \u0000 end","defaultShoe":true}"""
        val shoe = ShoeJson.decode(original)
        assertNull(shoe.startingDistanceMetres())
        assertEquals("café 🏃\nquote \" and \u0000 end", shoe.notes())
        assertEquals(shoe, ShoeJson.decode(ShoeJson.encode(shoe)))
        val zero = ShoeJson.decode(original.replace(":null", ":0"))
        assertEquals(0.0, zero.startingDistanceMetres())
    }

    @Test
    fun originalOptionalFieldsAndExtendedDatesRemainReadable() {
        val minimal =
            ShoeJson.decode(
                """{"id":"shoe","model":"Simple","retired":false,"defaultShoe":false}"""
            )
        assertEquals("", minimal.brand())
        assertNull(minimal.purchaseDate())
        assertNull(minimal.notes())
        for (date in listOf("0000-02-29", "-999999999-01-01", "+999999999-12-31")) {
            val shoe =
                Shoe("extended", "Brand", "Model", StoredDate.parse(date), 0.0, false, null, false)
            assertEquals(shoe, ShoeJson.decode(ShoeJson.encode(shoe)))
        }
    }

    @Test
    fun invalidAndCoercedMeasurementsCannotReplaceRecords() {
        val valid =
            """{"id":"shoe","model":"Daily","startingDistanceMetres":0,"retired":false,"defaultShoe":false}"""
        for (bad in
            listOf(
                valid.replace(":0", ":\"0\""),
                valid.replace(":0", ":-1"),
                valid.replace(":false", ":\"false\""),
                valid.replace(":0", ":1e999"),
                valid + " {}",
                "[]",
                "{broken",
                valid.replace("\"id\":\"shoe\"", "\"id\":null"),
            )) {
            assertFailsWith<IllegalArgumentException>(bad) { ShoeJson.decode(bad) }
        }
    }

    @Test
    fun retainedShoeConstructorSafeguardsStillApply() {
        val retiredDefault = """{"id":"shoe","model":"Daily","retired":true,"defaultShoe":true}"""
        assertFailsWith<IllegalArgumentException> { ShoeJson.decode(retiredDefault) }
        assertFailsWith<IllegalArgumentException> {
            ShoeJson.decode(
                retiredDefault
                    .replace("\"retired\":true", "\"retired\":false")
                    .replace("Daily", "x".repeat(201))
            )
        }
    }
}
