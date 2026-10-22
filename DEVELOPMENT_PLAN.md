# Development plan and feature status — coaching test build

This increment starts from verified main
`7624a662c5a52b6cd0ab427c09e17b354b50c62e`, on
`codex/production-coach-test-build`, draft PR #2. It must **not merge to main**
until physical-device evaluation and a separate merge decision.

| Area | Implemented | Partial/pending |
| --- | --- | --- |
| Architecture | Existing Kotlin/Compose/Material 3/Nav/ViewModel/StateFlow and Android-independent Java rules preserved | Room and retained native goal form conversion remain optional separate migrations |
| Storage | Explicit 1→2→3→4 migrations; immutable coaching versions; current pointer; proposals/decisions; strength; response/feedback; atomic reminder mirror | Normalised per-set tables unnecessary until query needs justify them; JSON codecs retain typed structure |
| Health Connect/baseline | Existing availability/partial consent/import identity/update/delete, observed baseline, manual fallback, assessment, provenance/confidence | Samsung/real-phone verification; no additional gym permissions |
| Training generation | Original deterministic 5K/10K/HM/marathon/aerobic plans, actual horizon/phases, conservative workload/run-walk, taper, effort guidance | Sophisticated PB-based pace/ability modelling, advanced intensity-retaining taper, NHS C25K |
| Scheduling | Bounded hard/soft scorer, availability/frequency/race constraints, long/quality spacing, recorded lower-body context | Global optimisation and manual customise-any-week editor |
| Current plan | Explicit generation/rebuild, preserved versions and evidence/goal snapshot, weekly view/phases/future projections | Rich standalone version-history browser |
| Adaptation | Raw-evidence single-quality-session proposals, explained options/keep/easy/rest/feasible move, recorded decisions/versions | Automatic long-run modifications, multi-week interruption/fitness restructuring; explicit rebuild available |
| Run feedback | Planned-vs-actual/response separation, purpose-based component score, unplanned Run Analysis, neutral feedback | Automatic rep/lap matching, terrain/HR-normalised scoring and richer completion response form |
| Strength | Multiple exercises/ordered sets/custom library, region/effort context, full reproducible PB progression/e1RM, optional UI | Session/library editing UI, lb entry/display, provider import and strength graphs |
| Load | Duration×RPE with coverage and separate run/strength context | CTL/ATL/TSB modelling deliberately not fabricated |
| Running progress/shoes | Existing PB/history, splits/rolling-effort gates, results, shoes, weekly-distance summaries | Prediction/readiness engines and advanced graphs |
| Reminders | Existing local opt-in scheduler now driven by current generated plan, revisions and accepted changes | Real Galaxy/OEM timing and battery tests |
| Backup | Versioned user export/validated transactional replacement includes coaching graph; old backups accepted | Merge restore deliberately unsupported |
| Screenshots | Actual code instrumentation fixtures, capture artifacts, API-35 canonical publication and source digest | Human image review and physical-device accessibility checks remain necessary |
| GPX/GPS/maps/Wear | Existing extension contracts retained | Runtime implementations explicitly outside this iteration |

See TRAINING_ENGINE.md for rules/research/limitations and TEST_BUILD.md for the
Galaxy S24 evaluation checklist. VERIFICATION.md records results actually observed.
No scaffold returns fabricated health values, predictions or readiness scores.
