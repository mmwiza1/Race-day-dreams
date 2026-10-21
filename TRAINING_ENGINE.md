# Training engine

## Phase 1 status

There is no training-plan generator, adaptive engine, race predictor or readiness
scoring implementation in Phase 1. Plan, prediction and adjustment types are
domain foundations for subsequent work. The app must not show made-up workouts,
race forecasts or readiness percentages.

Implemented calculations are limited to onboarding validation, race-date
arithmetic and target pace. Target pace is finishing duration divided by exact
race distance (21.0975 km for a half marathon and 42.195 km for a marathon),
rounded for display. A 1:59:59 half-marathon target is approximately 5:41/km.
This arithmetic does not establish whether a target is realistic.

## Rules for future engines

**The app recommends. The runner decides. The plan adapts.**

- Keep engines deterministic and testable outside Android UI.
- Every significant proposed change needs a readable reason and alternatives.
- Preserve the original prescription alongside the completed workout.
- A deviation or missed session is data about a changed week, not a failure.
- Never change the runner's race goal or silently cancel training.
- Use running and gym workload together; allow both on the same day.
- Missing HR, sleep or recovery metrics must not stop useful guidance.
- Use context and consistency rather than an automatic ten-percent mileage cap.
- Make conservative predictions from multiple signals and show a range.
- No medical diagnoses, injury-prevention guarantees or claims of scientific
  certainty. Rule-based recommendations must not be marketed as AI.

## Strategies

Distance First prioritises endurance before progressively adding more
race-specific work. Balanced develops endurance and speed together. Performance
requires supporting training history; new users cannot unlock it merely by
selecting a preference. Zone 2 / Aerobic is a lower-intensity option designed to
build aerobic endurance while limiting unnecessary high-intensity workload.

No strategy currently creates workouts. Selecting a strategy stores a preference
for the future plan generator.

## Future component boundaries

| Component | Responsibility |
| --- | --- |
| TrainingPlanGenerator | Original phase/workout generation from ability, time and routine |
| AdaptiveTrainingEngine | Explain proposed changes from planned versus actual training |
| TrainingLoadCalculator | Summarise running, gym and consistency metrics |
| RecoveryEvaluator | Compare available personal recovery signals without diagnoses |
| RacePredictionEngine | Conservative range and limiting-factor explanation |
| RaceReadinessEngine | Transparent internal component score |
| PaceCalculator | Pace and target-duration arithmetic, expanded later with evidence |

Beginner 5K progression and NHS attribution are pending. See
[LICENCES_AND_ATTRIBUTION.md](LICENCES_AND_ATTRIBUTION.md) before implementation.
