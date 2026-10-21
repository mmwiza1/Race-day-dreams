package org.racedaydreams.domain.activity;

import java.time.Instant;

/** totalDistanceMetres is unavailable if earlier mileage was never supplied. */
public record ShoeMetrics(String shoeId, double recordedDistanceMetres, Double totalDistanceMetres,
        int runCount, int runsWithKnownDistance, Long ageDays, Double longestRunMetres,
        Double averageRunDistanceMetres, Instant firstRecordedRun, String wearGuidance) {}
