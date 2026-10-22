# Development plan and feature status

The coaching test build has now been merged to `main` at
`09f2cbf66622c57df1a4b68d5a78bf34c73ce449`.

The next development increment focuses on multi-week adaptation, weekly coaching
reviews, improved onboarding/calendar UX, stronger strength integration, body-weight
progress tracking, and richer health-data ingestion where it can be done safely and
honestly.

| Area | Implemented | Partial/pending |
| --- | --- | --- |
| Architecture | Existing Kotlin/Compose/Material 3/Nav/ViewModel/StateFlow and Android-independent Java rules preserved | Room and retained native goal form conversion remain optional separate migrations |
| Storage | Explicit 1→2→3→4 migrations; immutable coaching versions; current pointer; proposals/decisions; strength; response/feedback; atomic reminder mirror | Normalised per-set tables unnecessary until query needs justify them; JSON codecs retain typed structure |
| Health Connect/baseline | Existing availability/partial consent/import identity/update/delete, observed baseline, manual fallback, assessment, provenance/confidence | Real-phone verification; richer provider data where available; optional body-weight import |
| Training generation | Original deterministic 5K/10K/HM/marathon/aerobic plans, actual horizon/phases, conservative workload/run-walk, taper, effort guidance | Sophisticated PB-based pace/ability modelling, advanced intensity-retaining taper, NHS C25K; distant-race foundation/base refinement |
| Scheduling | Bounded hard/soft scorer, availability/frequency/race constraints, long/quality spacing, recorded lower-body context | Race-day availability override, broader strength-aware long-run handling, global optimisation and manual customise-any-week editor |
| Current plan | Explicit generation/rebuild, preserved versions and evidence/goal snapshot, weekly view/phases/future projections | Rich standalone version-history browser |
| Adaptation | Raw-evidence single-quality-session proposals, explained options/keep/easy/rest/feasible move, recorded decisions/versions | Weekly coaching review, multi-week adaptation, repeated missed-session patterns, PB-driven pace reassessment, long-run durability, extended-break return, broader workload response |
| Run feedback | Planned-vs-actual/response separation, purpose-based component score, unplanned Run Analysis, neutral feedback | Race/time-trial-specific scoring, automatic rep/lap matching, terrain/HR-normalised scoring and richer completion response form |
| Strength | Multiple exercises/ordered sets/custom library, region/effort context, full reproducible PB progression/e1RM, optional UI | Dedicated Strength bottom tab, planned-session linking, historical/manual PB entry, session/library editing UI, lb entry/display, provider import and strength graphs |
| Load | Duration×RPE with coverage and separate run/strength context | CTL/ATL/TSB-style context and broader multi-week workload response without presenting a fake universal readiness score |
| Running progress/shoes | Existing PB/history, splits/rolling-effort gates, results, shoes, weekly-distance summaries | Body-weight tracking/graphs, prediction/readiness engines and advanced graphs |
| Calendar/history | Detailed list view and retained activity detail | Add List/Week/Month views, visible marker legend, accessible non-colour labels, remembered selected view, user-friendly dates |
| Onboarding/UX | Health-first baseline and explainable coaching information | Reduce text density, progressive disclosure, visual cards/chips, Android date pickers, hide technical provider/package details by default, DD-MM-YYYY/natural-language dates in user-facing UI |
| Reminders | Existing local opt-in scheduler now driven by current generated plan, revisions and accepted changes | Real Galaxy/OEM timing and battery tests |
| Backup | Versioned user export/validated transactional replacement includes coaching graph; old backups accepted | Merge restore deliberately unsupported |
| Screenshots | Actual code instrumentation fixtures, capture artifacts, API-35 canonical publication and source digest | Human image review and physical-device accessibility checks remain necessary; screenshots must continue to track the real UI |
| GPX/GPS/maps/Wear | Existing extension contracts retained | Runtime implementations explicitly outside this iteration |

## Samsung Health direct data — experimental and temporary

Race Day Dreams may temporarily prototype a direct Samsung Health provider so that
Galaxy Watch/Samsung Health running sessions can retain richer exercise detail when
Health Connect does not expose enough data for reliable splits.

The intended test architecture is provider-based rather than Samsung-dependent:

- Health Connect remains the universal/default health-data layer.
- Samsung Health direct access is optional and must degrade cleanly when unavailable.
- Users may be offered an automatic/richest-source mode and an explicit provider
  preference where appropriate.
- Duplicate activities arriving through both Samsung Health and Health Connect must
  resolve to one canonical activity while preserving provenance.
- Splits must only be calculated from sufficiently detailed samples; Race Day Dreams
  must never fabricate split data from aggregate pace alone.

**Important distribution caveat:** direct Samsung Health integration is considered
experimental/test-only because Samsung's Health Data SDK and production-access model
introduce partnership, registration and proprietary-SDK constraints that do not fit
cleanly with Race Day Dreams' open-source, low-friction distribution goals.

Unless those constraints become practical for the project, the direct Samsung Health
provider is expected to be removed before a normal public release. The provider
boundary must therefore remain isolated so removing Samsung-specific code does not
break Health Connect or the rest of the application.

The purpose of the temporary Samsung path is to evaluate whether direct Samsung data
materially solves split/detail loss on supported Galaxy devices, not to make Samsung
Health a permanent dependency.

See TRAINING_ENGINE.md for rules/research/limitations and TEST_BUILD.md for the
Galaxy S24 evaluation checklist. VERIFICATION.md records results actually observed.
No scaffold returns fabricated health values, predictions or readiness scores.
