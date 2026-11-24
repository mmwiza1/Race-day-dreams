package org.racedaydreams.domain.time

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The device's time zone as a rule from instant to UTC offset, so daylight-saving changes resolve
 * per instant. Used only where a record carries no offset of its own (audit B-01).
 */
fun interface LocalZone {
    fun offsetAt(instant: Instant): ZoneOffset

    companion object {
        /** The current system time zone, read at each call so a zone change applies at once. */
        @JvmField val SYSTEM: LocalZone = LocalZone { systemOffsetAt(it) }

        @JvmStatic fun fixed(offset: ZoneOffset): LocalZone = LocalZone { offset }
    }
}

internal expect fun systemOffsetAt(instant: Instant): ZoneOffset
