package org.racedaydreams.health
import org.racedaydreams.domain.activity.EnhancedHealthProvider
import org.racedaydreams.domain.activity.ActivityRecord
import java.time.Instant
/** Distribution-safe boundary. No reflective SDK loading, credentials, or proprietary binary. */
class SamsungHealthProvider : EnhancedHealthProvider {
    override fun availability() = EnhancedHealthProvider.Availability.SDK_NOT_INCLUDED
    override fun readRuns(from:Instant,to:Instant):List<ActivityRecord> = emptyList()
}
