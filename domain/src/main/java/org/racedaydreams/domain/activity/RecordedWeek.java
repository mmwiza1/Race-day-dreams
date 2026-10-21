package org.racedaydreams.domain.activity;

import java.time.LocalDate;

/** A summary of retained records, not proof that unrecorded exercise did not occur. */
public record RecordedWeek(LocalDate weekStart, Double distanceMetres, int activityCount,
                           int runningDays, int missingDistanceCount, Double longestRunMetres) {}
