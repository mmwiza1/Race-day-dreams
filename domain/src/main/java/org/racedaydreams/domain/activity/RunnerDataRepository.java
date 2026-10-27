package org.racedaydreams.domain.activity;

import org.racedaydreams.domain.baseline.BaselineRepository;
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository;
import org.racedaydreams.domain.settings.SettingsRepository;

/**
 * Compatibility aggregate for existing integrations. New feature orchestration accepts only the
 * smaller repository contracts it uses. Adapters share one local transaction boundary.
 */
public interface RunnerDataRepository extends ActivityRepository, ResultRepository, ShoeRepository,
        BaselineRepository, SettingsRepository, PlannedWorkoutRepository { }
