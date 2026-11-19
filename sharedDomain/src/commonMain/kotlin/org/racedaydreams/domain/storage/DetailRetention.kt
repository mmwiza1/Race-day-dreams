package org.racedaydreams.domain.storage

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Original selection and consent rules only. Never deletes evidence or starts background work. */
object DetailRetention {
    enum class Policy(private val months: Int) {
        LAST_24_MONTHS(24),
        LAST_5_YEARS(60),
        KEEP_FOREVER(0);

        fun cutoff(at: Instant): Instant? = if (months == 0) null else detailCutoff(at, months)
    }

    enum class Choice {
        NEW_INSTALL_DEFAULT,
        EXISTING_UNREVIEWED,
        ACKNOWLEDGED,
    }

    data class Preferences(
        private val policy: Policy?,
        private val choice: Choice?,
        private val chosenAt: Instant?,
        private val choiceId: String?,
    ) {
        init {
            if (policy == null || choice == null) throw NullPointerException()
            if (choice == Choice.EXISTING_UNREVIEWED)
                require(policy == Policy.KEEP_FOREVER && chosenAt == null && choiceId == null) {
                    "Existing details remain protected until an explicit choice."
                }
            else {
                if (chosenAt == null) throw NullPointerException()
                id(choiceId, "retention choice")
            }
        }

        companion object {
            @JvmStatic
            fun existing(): Preferences =
                Preferences(Policy.KEEP_FOREVER, Choice.EXISTING_UNREVIEWED, null, null)

            @JvmStatic
            fun fresh(at: Instant?, id: String?): Preferences =
                Preferences(Policy.LAST_24_MONTHS, Choice.NEW_INSTALL_DEFAULT, at, id)

            @JvmStatic
            fun acknowledged(policy: Policy?, at: Instant?, id: String?): Preferences =
                Preferences(policy, Choice.ACKNOWLEDGED, at, id)
        }

        fun policy(): Policy = policy!!

        fun choice(): Choice = choice!!

        fun chosenAt(): Instant? = chosenAt

        fun choiceId(): String? = choiceId

        override fun toString(): String =
            "Preferences[policy=$policy, choice=$choice, chosenAt=$chosenAt, choiceId=$choiceId]"
    }

    data class Recording(
        private val sessionId: String?,
        private val activityId: String?,
        private val provider: String?,
        private val sourceRecordId: String?,
        private val endedAt: Instant?,
        private val finished: Boolean,
        private val points: Long,
        private val events: Long,
        private val denseSamples: Long?,
    ) {
        init {
            id(sessionId, "recording")
            if (provider == null || sourceRecordId == null) throw NullPointerException()
            if (activityId != null) id(activityId, "activity")
            require(points >= 0 && events >= 0 && (denseSamples == null || denseSamples >= 0)) {
                "Invalid detail counts."
            }
        }

        fun hasKnownDetail(): Boolean =
            points > 0 || events > 0 || denseSamples?.let { it > 0 } == true

        fun sessionId(): String = sessionId!!

        fun activityId(): String? = activityId

        fun provider(): String = provider!!

        fun sourceRecordId(): String = sourceRecordId!!

        fun endedAt(): Instant? = endedAt

        fun finished(): Boolean = finished

        fun points(): Long = points

        fun events(): Long = events

        fun denseSamples(): Long? = denseSamples

        override fun toString(): String =
            "Recording[sessionId=$sessionId, activityId=$activityId, provider=$provider, sourceRecordId=$sourceRecordId, endedAt=$endedAt, finished=$finished, points=$points, events=$events, denseSamples=$denseSamples]"
    }

    data class Preview(
        private val policy: Policy?,
        private val inspectedAt: Instant?,
        private val datasetEpoch: String?,
        private var recordings: List<Recording>?,
        private val points: Long,
        private val events: Long,
        private val knownDenseSamples: Long,
        private val unknownSampleCounts: Int,
    ) {
        init {
            if (policy == null || inspectedAt == null) throw NullPointerException()
            id(datasetEpoch, "dataset")
            recordings = Immutable.copyList(recordings!!)
            require(
                points >= 0 && events >= 0 && knownDenseSamples >= 0 && unknownSampleCounts >= 0
            ) {
                "Invalid preview totals."
            }
            var expectedPoints = 0L
            var expectedEvents = 0L
            var expectedSamples = 0L
            var unknown = 0
            val ids = mutableSetOf<String>()
            val activities = mutableSetOf<String>()
            val cutoff = policy.cutoff(inspectedAt)
            for (row in recordings!!) {
                require(
                    cutoff != null &&
                        row.finished() &&
                        row.activityId() != null &&
                        row.endedAt() != null &&
                        row.endedAt()!!.isBefore(cutoff) &&
                        row.sessionId() == row.sourceRecordId() &&
                        (row.provider() == "rdd" || row.provider() == "gpx") &&
                        row.hasKnownDetail() &&
                        ids.add(row.sessionId()) &&
                        activities.add(row.activityId()!!)
                ) {
                    "Invalid selected detail ownership or age."
                }
                expectedPoints = checkedAdd(expectedPoints, row.points())
                expectedEvents = checkedAdd(expectedEvents, row.events())
                val count = row.denseSamples()
                if (count == null) unknown++
                else expectedSamples = checkedAdd(expectedSamples, count)
            }
            require(
                expectedPoints == points &&
                    expectedEvents == events &&
                    expectedSamples == knownDenseSamples &&
                    unknown == unknownSampleCounts
            ) {
                "Preview counts do not match the selected evidence."
            }
        }

        fun policy(): Policy = policy!!

        fun inspectedAt(): Instant = inspectedAt!!

        fun datasetEpoch(): String = datasetEpoch!!

        fun recordings(): List<Recording> = recordings!!

        fun points(): Long = points

        fun events(): Long = events

        fun knownDenseSamples(): Long = knownDenseSamples

        fun unknownSampleCounts(): Int = unknownSampleCounts

        override fun toString(): String =
            "Preview[policy=$policy, inspectedAt=$inspectedAt, datasetEpoch=$datasetEpoch, recordings=$recordings, points=$points, events=$events, knownDenseSamples=$knownDenseSamples, unknownSampleCounts=$unknownSampleCounts]"
    }

    data class Approval(
        private val preview: Preview?,
        private val choiceId: String?,
        private val approvedAt: Instant?,
    ) {
        init {
            if (preview == null) throw NullPointerException()
            id(choiceId, "approved retention choice")
            if (approvedAt == null) throw NullPointerException()
            require(
                !approvedAt.isBefore(preview.inspectedAt()) &&
                    !approvedAt.isAfter(deadline(preview.inspectedAt()))
            ) {
                "Review a fresh detail preview before approval."
            }
        }

        fun preview(): Preview = preview!!

        fun choiceId(): String = choiceId!!

        fun approvedAt(): Instant = approvedAt!!

        override fun toString(): String =
            "Approval[preview=$preview, choiceId=$choiceId, approvedAt=$approvedAt]"
    }

    @JvmStatic
    fun preview(
        policy: Policy,
        inventory: List<Recording>,
        now: Instant,
        datasetEpoch: String?,
    ): Preview {
        val seen = mutableMapOf<String, Recording>()
        val activityIds = mutableSetOf<String>()
        val selected = mutableListOf<Recording>()
        val cutoff = policy.cutoff(now)
        var points = 0L
        var events = 0L
        var samples = 0L
        var unknown = 0
        for (row in inventory) {
            val previous = seen[row.sessionId()]
            if (previous != null) {
                require(previous == row) { "Conflicting recording inventory." }
                continue
            }
            seen[row.sessionId()] = row
            require(row.activityId() == null || activityIds.add(row.activityId()!!)) {
                "Ambiguous recording ownership; original details retained."
            }
            if (
                cutoff == null ||
                    !row.finished() ||
                    row.activityId() == null ||
                    row.endedAt() == null ||
                    !row.endedAt()!!.isBefore(cutoff) ||
                    row.sessionId() != row.sourceRecordId() ||
                    !(row.provider() == "rdd" || row.provider() == "gpx") ||
                    !row.hasKnownDetail()
            )
                continue
            selected.add(row)
            points = checkedAdd(points, row.points())
            events = checkedAdd(events, row.events())
            val count = row.denseSamples()
            if (count == null) unknown++ else samples = checkedAdd(samples, count)
        }
        selected.sortWith(compareBy<Recording> { it.endedAt()!! }.thenBy { it.sessionId() })
        return Preview(policy, now, datasetEpoch, selected, points, events, samples, unknown)
    }

    @JvmStatic
    fun approve(
        preview: Preview,
        current: Preferences,
        now: Instant,
        epoch: String?,
        unfinishedRecording: Boolean,
        replacementInProgress: Boolean,
    ): Approval {
        checkContext(preview, current, now, epoch, unfinishedRecording, replacementInProgress)
        require(preview.recordings().isNotEmpty()) {
            "There is no eligible detailed evidence in this preview."
        }
        return Approval(preview, current.choiceId(), now)
    }

    @JvmStatic
    fun checkBatch(
        approval: Approval,
        current: Preferences,
        now: Instant,
        epoch: String?,
        fresh: List<Recording>,
        unfinishedRecording: Boolean,
        replacementInProgress: Boolean,
    ) {
        checkContext(
            approval.preview(),
            current,
            now,
            epoch,
            unfinishedRecording,
            replacementInProgress,
        )
        require(approval.choiceId() == current.choiceId() && !now.isBefore(approval.approvedAt())) {
            "Retention choice or clock changed. Review cleanup again."
        }
        require(fresh.size <= 20) { "Cleanup transactions must remain bounded." }
        val approved = approval.preview().recordings().associateBy { it.sessionId() }
        val seen = mutableSetOf<String>()
        for (row in fresh) require(seen.add(row.sessionId()) && row == approved[row.sessionId()]) {
            "Detailed evidence changed after preview. Existing records were retained."
        }
    }

    private fun checkContext(
        preview: Preview,
        current: Preferences,
        now: Instant,
        epoch: String?,
        recording: Boolean,
        replacing: Boolean,
    ) {
        require(current.choice() != Choice.EXISTING_UNREVIEWED) {
            "Choose and acknowledge a retention policy before removing existing detail."
        }
        require(current.policy() == preview.policy() && current.policy() != Policy.KEEP_FOREVER) {
            "Current policy does not approve this cleanup."
        }
        require(preview.datasetEpoch() == epoch) {
            "Restored or replaced data needs a fresh detail preview."
        }
        require(!now.isBefore(preview.inspectedAt()) && !current.chosenAt()!!.isAfter(now)) {
            "Review the device clock and preview again."
        }
        require(!now.isAfter(deadline(preview.inspectedAt()))) {
            "Refresh the detail preview before continuing cleanup."
        }
        require(!recording && !replacing) {
            "Finish recording or data replacement before cleaning up detail."
        }
    }

    private fun deadline(at: Instant): Instant =
        Instants.ofEpochSecond(checkedAdd(at.getEpochSecond(), 86400), at.getNano().toLong())

    private fun id(value: String?, field: String) {
        require(value != null && value.trim { it <= ' ' }.isNotEmpty() && value.length <= 256) {
            "Invalid $field identity."
        }
    }
}

/** Same UTC calendar-month subtraction and month-end clipping as the original OffsetDateTime. */
private fun detailCutoff(at: Instant, months: Int): Instant {
    val date = CalendarDates.atOffset(at, Offsets.ofTotalSeconds(0))
    val targetMonth = date.getYear().toLong() * 12 + date.getMonthValue() - 1 - months
    val year = targetMonth.floorDiv(12).toInt()
    val month = targetMonth.mod(12) + 1
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val last =
        when (month) {
            2 -> if (leap) 29 else 28
            4,
            6,
            9,
            11 -> 30
            else -> 31
        }
    val target = CalendarDates.of(year, month, minOf(date.getDayOfMonth(), last))
    return Instants.ofEpochSecond(
        target.toEpochDay() * 86400 + at.getEpochSecond().mod(86400),
        at.getNano().toLong(),
    )
}
