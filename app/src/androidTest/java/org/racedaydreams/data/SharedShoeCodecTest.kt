package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.storage.ShoeJson

/** Cross-read the actual Android codec, not a second copy of its expected JSON implementation. */
@RunWith(AndroidJUnit4::class)
class SharedShoeCodecTest {
    @Test
    fun portableAndExistingAndroidCodecsReadEachOthersActualOutput() {
        val shoes =
            listOf(
                Shoe("unknown", null, "Daily", null, null, false, null, false),
                Shoe(
                    "zero",
                    "Brand",
                    "Daily",
                    LocalDate.of(2024, 2, 29),
                    0.0,
                    false,
                    "café 🏃\nquote \" and \u0000 end",
                    true,
                ),
                Shoe(
                    "retired",
                    "Brand",
                    "Old",
                    LocalDate.of(-999999999, 1, 1),
                    12345.5,
                    true,
                    "retained",
                    false,
                ),
            )
        for (shoe in shoes) {
            assertEquals(shoe, ShoeJson.decode(FeatureJson.shoe(shoe).toString()))
            assertEquals(shoe, FeatureJson.shoe(JSONObject(ShoeJson.encode(shoe))))
        }
    }

    @Test
    fun bothCodecsRejectCoercedWeightsAndBooleans() {
        val base =
            """{"id":"shoe","model":"Daily","startingDistanceMetres":0,"retired":false,"defaultShoe":false}"""
        for (bad in
            listOf(
                base.replace(":0", ":\"0\""),
                base.replace(":false", ":\"false\""),
                base.replace(":0", ":-1"),
            )) {
            assertThrows(IllegalArgumentException::class.java) { ShoeJson.decode(bad) }
            assertThrows(IllegalArgumentException::class.java) { FeatureJson.shoe(JSONObject(bad)) }
        }
    }
}
