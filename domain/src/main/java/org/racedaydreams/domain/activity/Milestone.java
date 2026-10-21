package org.racedaydreams.domain.activity;

import java.time.LocalDate;

public record Milestone(String code, String label, LocalDate date, String activityId,
                        String resultId, String explanation) {}
