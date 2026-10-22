# Development plan and feature status

The coaching test build has now been merged to `main` at
`09f2cbf66622c57df1a4b68d5a78bf34c73ce449`.

The next development increment focuses on multi-week adaptation, weekly coaching
reviews, improved onboarding/calendar UX, stronger strength integration, body-weight
progress tracking, and richer health-data ingestion where it can be done safely and
honestly.

## 0.4 implementation status

| Area | Implemented | Remaining / limitation |
| --- | --- | --- |
| Architecture | Kotlin/Compose/Material 3/ViewModel snapshot, Android-independent Java rules, explicit schema 1–5 upgrades | Room remains a separate evaluated migration; tested native goal wizard retained and simplified |
| Weekly coaching | Completed-week summary, four-week raw-evidence context, explained independent channels, often no adjustment, persisted recommendations/options/decisions | Standalone historical review browser and richer effort-matched analysis |
| Multi-week choices | Preview/keep/accept two-week hold/reduction, four-week quality pace review, three-week return bridge, immutable history/reminder reconciliation | Automatic broader programme rebuild is deliberately absent |
| Patterns | Recurring weekday missed/moved evidence, moved-and-completed and explicit skipped distinction, user move/skip history | Type/duration/preceding-strength patterns and one-tap recurring-day proposal remain future work |
| Performance | >5% comparable non-rolling PB plus consistency/response can flag pace review; bounded quality ranges only | Rich confidence/assessment-based pace model; no new numeric ranges where evidence does not support them; race predictions remain pending |
| Durability | Qualified continuous sample halves, fade and optional HR/cadence context, repeat fade review flag | Terrain correction and late-run timestamped subjective RPE |
| Return | Explicit interruption reason/dates, observed gap, opt-in easy bridge after >=14 days | Medical clearance is outside scope; response-aware bridge evolution beyond ordinary weekly reassessment |
| Scheduling/scoring | Race weekday exception, one-off conflicts retained, long horizons/early base, race/time-trial high-RPE execution | Advanced taper intensity and NHS C25K remain separate future work |
| Onboarding/dates | Card choices, compact weekday selections, help disclosure, Health-first available values, final explicit Create, natural UK dates/pickers | Native wizard remains intentional; physical-device 200% font validation |
| Calendar/navigation | Remembered List/Week/Month, labelled day details/legend, real running/strength records, routine slots and moved/skipped choices; Home/Plan/Calendar/Progress/Strength with top Settings | Month markers show counts and day labels; no drag/drop editor |
| Strength | Structured editable/duplicate/removable sets, custom exercises, automatic recorded PB history/e1RM, historical PBs exact/approximate/unknown, kg/lb, routine-date link | Unified historical/recorded chart and arbitrary session editing UI; imported strength sets pending |
| Body | Manual weight edit/delete, real-point range graph/text, canonical kg and kg/lb display, provenance, optional HC read | Physical-device HC checks; bounded read refreshes provider deletions only in its window; weight never drives coaching |
| Providers | Health Connect retained; provider preference, neutral enhanced contract, conservative cross-provider reconciliation synthetic tests | Samsung SDK not included: direct ingestion unavailable pending SDK licence/access review and private adapter mapping |
| Portability | Envelope 3, evidence section 1, preserved versions 1/2 readers, validation and transactional replace, manual user data included | Merge restore unsupported; raw external provider data omitted |
| Screenshots/CI | Existing real-render pipeline expanded for onboarding/review/Calendar/PBs; API 28/35/36 preserved | Actual latest verification and captures recorded in VERIFICATION.md when observed |
| Out of scope | Existing extension boundaries retained | GPS, GPX runtime, maps, Wear OS, social, backend and LLM features not added |

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

The open-source 0.4 build contains no Samsung SDK. Direct access is not operational;
see [reviewed developer boundary](docs/SAMSUNG_PROVIDER.md).
