package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.DetailRetention.Policy
import org.racedaydreams.domain.storage.DetailRetention.Preferences
import org.racedaydreams.domain.storage.DetailRetention.Recording
import org.racedaydreams.domain.storage.RemovedRecordingDetail
import org.racedaydreams.domain.time.*

class DetailRetentionContractTest {
    private val now = Instants.ofEpochSecond(1794744000, 123456789)
    private val old =
        Instants.ofEpochSecond(now.getEpochSecond() - 4L * 366 * 86400, now.getNano().toLong())
    private val prefs = Preferences.acknowledged(Policy.LAST_24_MONTHS, now, "choice")

    private fun row(
        id: String = "r",
        at: Instant? = old,
        finished: Boolean = true,
        provider: String = "rdd",
        recordId: String = id,
        activity: String? = "a$id",
        points: Long = 10,
        events: Long = 1,
        samples: Long? = null,
    ) = Recording(id, activity, provider, recordId, at, finished, points, events, samples)

    private fun preview(rows: List<Recording> = listOf(row())) =
        DetailRetention.preview(Policy.LAST_24_MONTHS, rows, now, "epoch")

    private fun shift(seconds: Long, nanos: Long = 0) =
        Instants.ofEpochSecond(now.getEpochSecond() + seconds, now.getNano() + nanos)

    @Test
    fun retentionUsesCalendarMonthsAndPreservesFractionalObservationTime() {
        val leap =
            Instants.ofEpochSecond(
                CalendarDates.of(2024, 2, 29).toEpochDay() * 86400 + 86399,
                123456789,
            )
        val expected =
            Instants.ofEpochSecond(
                CalendarDates.of(2022, 2, 28).toEpochDay() * 86400 + 86399,
                123456789,
            )
        assertEquals(expected, Policy.LAST_24_MONTHS.cutoff(leap))
        assertNull(Policy.KEEP_FOREVER.cutoff(leap))
        assertEquals(
            CalendarDates.of(2019, 2, 28),
            CalendarDates.atOffset(Policy.LAST_5_YEARS.cutoff(leap)!!, Offsets.ofTotalSeconds(0)),
        )
    }

    @Test
    fun existingUsersCannotApproveRemovalBeforeAcknowledgingTheirChoice() {
        val current = Preferences.existing()
        assertEquals(Policy.KEEP_FOREVER, current.policy())
        assertNull(current.choiceId())
        assertNull(current.chosenAt())
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.approve(preview(), current, now, "epoch", false, false)
        }
        assertEquals(Policy.LAST_24_MONTHS, Preferences.fresh(now, "new").policy())
    }

    @Test
    fun selectionRequiresOldFinishedAppOwnedKnownDetailAndNeverInventsSampleCounts() {
        val cutoff = Policy.LAST_24_MONTHS.cutoff(now)!!
        val selected =
            preview(
                listOf(
                    row(),
                    row("boundary", cutoff),
                    row("paused", finished = false),
                    row("external", provider = "other"),
                    row("unlinked", activity = null),
                    row("source", recordId = "different"),
                    row("unknown", at = null),
                    row("empty", points = 0, events = 0, samples = null),
                )
            )
        assertEquals(listOf("r"), selected.recordings().map { it.sessionId() })
        assertEquals(10L, selected.points())
        assertEquals(1L, selected.events())
        assertEquals(0L, selected.knownDenseSamples())
        assertEquals(1, selected.unknownSampleCounts())
        assertNull(selected.recordings().single().denseSamples())
    }

    @Test
    fun conflictingIdentitiesOrAmbiguousActivitiesFailClosed() {
        val r = row()
        assertEquals(1, preview(listOf(r, r)).recordings().size)
        assertFailsWith<IllegalArgumentException> { preview(listOf(r, row(points = 11))) }
        assertFailsWith<IllegalArgumentException> {
            preview(listOf(r, row("other", activity = r.activityId())))
        }
        assertFailsWith<ArithmeticException> {
            preview(listOf(row(points = Long.MAX_VALUE), row("other", points = 1)))
        }
    }

    @Test
    fun approvalsAndEveryBatchRecheckEpochClockConsentAndActiveOperations() {
        val p = preview()
        val approval = DetailRetention.approve(p, prefs, now, "epoch", false, false)
        for (pair in listOf(true to false, false to true)) assertFailsWith<
            IllegalArgumentException
        > {
            DetailRetention.checkBatch(
                approval,
                prefs,
                now,
                "epoch",
                p.recordings(),
                pair.first,
                pair.second,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(
                approval,
                prefs,
                now,
                "restored",
                p.recordings(),
                false,
                false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(
                approval,
                Preferences.acknowledged(Policy.LAST_24_MONTHS, now, "changed"),
                now,
                "epoch",
                p.recordings(),
                false,
                false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(
                approval,
                prefs,
                shift(-1),
                "epoch",
                p.recordings(),
                false,
                false,
            )
        }
        DetailRetention.checkBatch(
            approval,
            prefs,
            shift(86400),
            "epoch",
            p.recordings(),
            false,
            false,
        )
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(
                approval,
                prefs,
                shift(86400, 1),
                "epoch",
                p.recordings(),
                false,
                false,
            )
        }
    }

    @Test
    fun boundedRepeatableChecksCannotAuthoriseUnreviewedOrChangedRows() {
        val rows = (0..20).map { row("r$it") }
        val p = preview(rows)
        val approval = DetailRetention.approve(p, prefs, now, "epoch", false, false)
        repeat(2) {
            DetailRetention.checkBatch(
                approval,
                prefs,
                now,
                "epoch",
                p.recordings().take(20),
                false,
                false,
            )
        }
        assertEquals(21, p.recordings().size)
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(approval, prefs, now, "epoch", rows, false, false)
        }
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(
                approval,
                prefs,
                now,
                "epoch",
                listOf(row("r0", points = 11)),
                false,
                false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.checkBatch(
                approval,
                prefs,
                now,
                "epoch",
                listOf(rows[0], rows[0]),
                false,
                false,
            )
        }
    }

    @Test
    fun removedDetailProvenanceRequiresExactIdentityHashesAndHonestChronology() {
        val hash = "a".repeat(64)
        val value =
            RemovedRecordingDetail(
                "r",
                "a",
                "rdd",
                "r",
                old,
                now,
                Policy.LAST_24_MONTHS,
                Instants.ofEpochSecond(old.getEpochSecond() - 100),
                0,
                100,
                1,
                0,
                hash,
                hash,
            )
        assertEquals(hash, value.pointsHash())
        assertEquals(old, value.endedAt())
        assertFailsWith<IllegalArgumentException> {
            RemovedRecordingDetail(
                "r",
                "a",
                "external",
                "r",
                old,
                now,
                Policy.LAST_24_MONTHS,
                old,
                0,
                0,
                1,
                0,
                hash,
                hash,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RemovedRecordingDetail(
                "r",
                "a",
                "rdd",
                "r",
                old,
                now,
                Policy.KEEP_FOREVER,
                old,
                0,
                0,
                1,
                0,
                hash,
                hash,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RemovedRecordingDetail(
                "r",
                "a",
                "rdd",
                "r",
                old,
                now,
                Policy.LAST_24_MONTHS,
                old,
                0,
                0,
                1,
                0,
                "A".repeat(64),
                hash,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RemovedRecordingDetail(
                "r",
                "a",
                "rdd",
                "r",
                old,
                now,
                Policy.LAST_24_MONTHS,
                old,
                0,
                0,
                0,
                0,
                hash,
                hash,
            )
        }
    }

    @Test
    fun choiceIdsAreNotSilentlyNormalisedAndPreviewCountsMustMatchEvidence() {
        assertEquals(
            " choice ",
            Preferences.acknowledged(Policy.LAST_24_MONTHS, now, " choice ").choiceId(),
        )
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.Preview(Policy.LAST_24_MONTHS, now, "epoch", listOf(row()), 11, 1, 0, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            DetailRetention.approve(preview(emptyList()), prefs, now, "epoch", false, false)
        }
    }
}
