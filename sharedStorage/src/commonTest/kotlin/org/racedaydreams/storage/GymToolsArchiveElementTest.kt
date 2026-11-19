package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.GymRestTimer

class GymToolsArchiveElementTest {
    @Test
    fun parentDomRetainsOriginalTimerOwnershipVersionAndPreferenceGuards() {
        val timer = GymRestTimer.start("execution", "set", 90, GymRestTimer.Clock("boot", 1000))
        val snapshot =
            GymToolsPayloadJson.Snapshot(GymRestTimer.Preferences.defaults(), listOf(timer))
        val root = GymToolsPayloadJson.element(snapshot)
        assertEquals(snapshot, GymToolsPayloadJson.decode(root, setOf("execution")))
        assertEquals(GymToolsPayloadJson.encode(snapshot), root.toString())
        assertFails { GymToolsPayloadJson.decode(root, emptySet()) }
        assertFails {
            GymToolsPayloadJson.decode(
                JsonObject(root + ("version" to JsonPrimitive(2))),
                setOf("execution"),
            )
        }
        assertFails {
            GymToolsPayloadJson.decode(
                JsonObject(
                    root +
                        ("timers" to
                            JsonArray(
                                listOf(
                                    root.getValue("timers").jsonArray.single(),
                                    root.getValue("timers").jsonArray.single(),
                                )
                            ))
                ),
                setOf("execution"),
            )
        }
        assertEquals(
            GymRestTimer.Preferences.defaults(),
            GymToolsPayloadJson.decode(null, emptySet()).preferences,
        )
    }

    @Test
    fun validLargeParentSectionDoesNotInheritStandaloneSixteenMiBLimit() {
        // Format-boundary stress fixture, not a claim about a real athlete's training history.
        val timers =
            List(40000) { i ->
                GymRestTimer.start(
                    "execution-${"e".repeat(80)}-$i",
                    "t".repeat(120),
                    90,
                    GymRestTimer.Clock("b".repeat(120), 1000),
                )
            }
        val known = timers.map { it.executionId() }.toSet()
        val root =
            GymToolsPayloadJson.element(
                GymToolsPayloadJson.Snapshot(GymRestTimer.Preferences.defaults(), timers)
            )
        val text = root.toString()
        val bytes = text.encodeToByteArray().size
        assertTrue(bytes > GymStoredJson.MAX_BYTES)
        assertTrue(bytes < BackupEnvelopeJson.MAX_BYTES)
        assertEquals(timers, GymToolsPayloadJson.decode(root, known).timers)
        assertFails {
            GymToolsPayloadJson.decode(text, known)
        } // Existing standalone limit stays intact.
        assertEquals(40000, known.size)
    }
}
