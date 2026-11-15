package org.racedaydreams.domain.activity

import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository
import org.racedaydreams.domain.settings.SettingsRepository

/** Compatibility aggregate. New orchestration uses only the smaller contracts it needs. */
interface RunnerDataRepository :
    ActivityRepository,
    ResultRepository,
    ShoeRepository,
    BaselineRepository,
    SettingsRepository,
    PlannedWorkoutRepository
