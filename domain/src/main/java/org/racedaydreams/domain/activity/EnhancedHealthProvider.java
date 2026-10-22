package org.racedaydreams.domain.activity;
import java.time.Instant;
import java.util.List;
/** Optional enhanced adapter contract. No proprietary types cross this boundary. */
public interface EnhancedHealthProvider {
    enum Availability { AVAILABLE, NOT_INSTALLED, UNSUPPORTED, PERMISSION_DENIED, SDK_NOT_INCLUDED }
    Availability availability();
    List<ActivityRecord> readRuns(Instant from,Instant to);
}
