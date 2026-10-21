# Development plan and feature status

The baseline is draft PR #1 at commit `3011e037`. Its original Phase 1 behavior
and tests are retained. This increment grows data and presentation foundations;
it does not imply that all running-coach features are complete.

| Area | Current scope | Remaining |
| --- | --- | --- |
| Architecture | Kotlin/Compose/Material 3/Nav/ViewModel/StateFlow; Java domain retained | Room adapter evaluated/deferred; legacy goal form migration optional |
| Storage | Additive schema 1→2, source identities, annotations, snapshots and user-owned data | Future schema changes require explicit migrations |
| Health Connect | Optional running/distance/HR/history import, availability and permission states, source-aware dedup/update/deletion | Physical-device/vendor validation; additional metrics only with implemented use |
| Baseline | Recorded-history review, optional manual corrections, confidence/provenance and derived eligibility | Richer effort/context inference requires more evidence |
| Assessment | Optional 1–5 km controlled/best-effort choice and manually entered completion | Live GPS recording and sensor capture |
| Activity history | Calendar list/detail, available metrics, notes/RPE/shoes, laps | Month/week calendar presentation; richer planned-vs-actual comparison |
| Splits/PBs | Reliable-sample splits, rolling efforts, manual/official results and progression | Health Connect totals cannot provide rolling samples; GPX/runtime recorder pending |
| Shoes | Local shoes, default/retired state, assignments, mileage and restrained wear guidance | More advanced wear/context analysis |
| Progress | Recorded-distance and PB/shoe trends with textual equivalents | Recovery/load/prediction graphs require their respective engines |
| Notifications | Opt-in evening policy, inexact local scheduling and stale-plan guards | No generated workouts until plan generation exists |
| Backup | Versioned local export and transactional validated replace restore | Merge restore deliberately unsupported |
| GPX/GPS | Codec/route/recorder extension contracts, compatibility/licence evaluation | Runtime parser/writer, foreground recorder, sensors, route UI |
| Maps/Wear | Shared models and open-stack architectural direction | MapLibre runtime and Wear OS companion |
| Training engine | Existing prescriptions/models, generator contract, whole-plan snapshots, invariant diagnostics and five synthetic test-only fixtures | Original complete-plan generation, adaptation, taper and race prediction |

## Next development sequence

1. Exercise real Health Connect synchronization and consent on physical devices,
   with manual fallback always available. Correct gaps revealed by vendor data.
2. Add an original deterministic plan generator behind the existing contract.
   Pair each rule with whole-plan snapshots for beginner, half-marathon, marathon,
   aerobic and returning-runner fixtures. Do not substitute fake generated plans.
3. Add editable week/month scheduling and controlled plan-adjustment decisions.
   Connect every plan mutation to reminder reconciliation.
4. Implement live recording/GPX with a reviewed compatible permissive library,
   then expand retained samples/sensor provenance and routes.
5. Add conservative multi-signal predictions, pace guidance and transparent
   readiness components. Never change a target without consent.
6. Broaden charts and optional watch delivery only where supported by real data.

Before beginner C25K content is added, verify the current NHS published schedule
and reuse terms. Do not copy commercial plan catalogues, NHS app assets, GPL
source or another project's safety claims.

Every completed increment must pass domain/schema tests, Android build/lint,
device tests and migration tests before main. Pending features must remain
labelled and may not return invented health values, workouts or predictions.
