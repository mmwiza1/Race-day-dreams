package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.*

/**
 * The one dating rule for activities (audit B-01): a record's own start offset when its source
 * provided one, otherwise the device's time zone at that instant. HealthKit imports and some Health
 * Connect records carry no offset; they were previously dated in UTC by some screens and in the
 * device zone by others, so a late-evening or early-morning run could land on different days.
 */
object ActivityDates {
    /** Resolves offsets for records without one. Only tests replace it, through [withZone]. */
    internal var zone: LocalZone = LocalZone.SYSTEM
        private set

    @JvmStatic
    fun offsetAt(instant: Instant, recorded: ZoneOffset?): ZoneOffset =
        recorded ?: zone.offsetAt(instant)

    @JvmStatic
    fun localDate(instant: Instant, recorded: ZoneOffset?): LocalDate =
        CalendarDates.atOffset(instant, offsetAt(instant, recorded))

    /**
     * The offset a record really carries. This app's own recordings ("rdd") and GPX imports have
     * always been stored with a placeholder UTC offset rather than the runner's zone, so a zero
     * offset from those providers is treated as unknown.
     */
    @JvmStatic
    fun recordedOffset(record: ActivityRecord): ZoneOffset? =
        recordedOffset(record.source().provider(), record.startOffset())

    /** [recordedOffset] for stored rows read without building a record. */
    @JvmStatic
    fun recordedOffset(provider: String, startOffset: ZoneOffset?): ZoneOffset? =
        startOffset?.takeUnless {
            it.getTotalSeconds() == 0 && provider in PLACEHOLDER_UTC_PROVIDERS
        }

    /** The offset used to date [record]: its own, otherwise the device zone at its start. */
    @JvmStatic
    fun offsetOf(record: ActivityRecord): ZoneOffset =
        offsetAt(record.start(), recordedOffset(record))

    /** The calendar day a run belongs to, wherever it is shown or counted. */
    @JvmStatic
    fun localDate(record: ActivityRecord): LocalDate =
        CalendarDates.atOffset(record.start(), offsetOf(record))

    /** The device-zone calendar day of an instant. */
    @JvmStatic fun deviceDate(instant: Instant): LocalDate = localDate(instant, null)

    /** The first instant of [date] in the device zone (local midnight, DST-aware). */
    @JvmStatic
    fun startOfDeviceDay(date: LocalDate): Instant {
        val midnight = date.toEpochDay() * 86400L
        val guess = midnight - zone.offsetAt(Instants.ofEpochSecond(midnight)).getTotalSeconds()
        return Instants.ofEpochSecond(
            midnight - zone.offsetAt(Instants.ofEpochSecond(guess)).getTotalSeconds()
        )
    }

    private val PLACEHOLDER_UTC_PROVIDERS = setOf("rdd", "gpx")

    /** Runs [block] with a fixed device zone; for deterministic tests only. */
    internal fun <T> withZone(zone: LocalZone, block: () -> T): T {
        val previous = this.zone
        this.zone = zone
        try {
            return block()
        } finally {
            this.zone = previous
        }
    }
}
