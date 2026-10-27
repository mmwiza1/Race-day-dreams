package org.racedaydreams.domain.activity;

import org.racedaydreams.domain.health.HealthImportRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Activity evidence and annotations. Synchronous operations run on an IO dispatcher. */
public interface ActivityRepository extends HealthImportRepository {
    List<ActivityRecord> activitySummaries();

    ActivityRecord activityDetail(String id);

    List<ActivityRecord> coachingActivities(LocalDate today);

    List<ActivityRecord> activitySummaryPage(Instant before, String afterId, int limit);

    void upsertManualActivity(ActivityRecord activity);

    void deleteActivity(String id);

    void saveActivityNotes(String id, String notes, Double rpe);

    void assignShoe(String activityId, String shoeId);
}
