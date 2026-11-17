package org.racedaydreams.storage

import kotlin.test.*

class StoredJsonFieldTest {
    private fun field(text: String, depth: Int = 32, values: Int = 2000000, length: Int = 8192) =
        StoredJsonSyntax(text, depth, values, length) { throw IllegalArgumentException(it) }
            .validatedRootField("payload")

    @Test
    fun exactValueRetainsEscapesNumbersAndInteriorSpacingAndIgnoresNestedNames() {
        val value =
            """{ "payload":"not the row", "path":"tunnel \/ caf\u00e9", "n":1e+04, "array":[true, null, {"q":"\"}"}] }"""
        assertEquals(value, field("""{ "before":{"payload":0}, "payload": $value , "after":[] }"""))
    }

    @Test
    fun escapedFieldNamesAreRecognisedWithoutChangingTheirValue() {
        assertEquals("""{"n":-0.00}""", field("""{"pay\u006coad":{"n":-0.00}}"""))
        assertNull(field("""{"nested":{"payload":1}}"""))
    }

    @Test
    fun completeDocumentMustValidateBeforeAnExtractedFieldCanBeReturned() {
        for (text in
            listOf(
                """{"payload":{},"payload":1}""",
                """{"payload":{},"pay\u006coad":1}""",
                """{"payload":{},"after":[1,]}""",
                """{"payload":{}}true""",
            )) assertFailsWith<IllegalArgumentException> { field(text) }
    }

    @Test
    fun extractionKeepsOriginalDepthTokenAndStringBudgets() {
        assertFailsWith<IllegalArgumentException> { field("""{"payload":{"n":{}}}""", depth = 1) }
        assertFailsWith<IllegalArgumentException> { field("""{"payload":[1,2,3]}""", values = 3) }
        assertFailsWith<IllegalArgumentException> {
            field("""{"payload":"very long text"}""", length = 10)
        }
    }
}
