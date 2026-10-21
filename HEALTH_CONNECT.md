# Health Connect

## Current status

Phase 1 does not include the Health Connect SDK, query Health Connect, request
health permissions or import workouts. Missing health data cannot block
onboarding. There is no Google Fit integration.

## Phase 2 design

Android Health Connect will be the primary health-data integration. Samsung
Health and other compatible apps may supply records when the runner has enabled
their Health Connect synchronisation; Race Day Dreams must not promise that any
particular source has synchronised.

```text
Optional consented Health Connect records
                 ↓
provider adapter → local repository/database → local domain calculations
```

Provider-specific types belong in the Android data adapter, not the training
engine. Use a provider-neutral contract and domain types so another provider
can be added without rewriting training rules.

## Permission policy

Request only permissions consumed by implemented features, with a clear
explanation before the Android permission dialog. Do not pre-request the entire
list of potentially useful metrics. Account for provider availability, partial
consent, denial and revocation. The app remains usable without Health Connect.

Start with exercise sessions and relevant running records. Add distance, timing,
heart rate, sleep, resting HR or other metrics only when an implemented feature
uses them. Never infer the presence of a permission from a nonempty cached value.
No writes, background access or extended-history access should be requested
without an implemented reason and explicit explanation.

## Import requirements

- Preserve provider/source identifiers and provenance.
- Deduplicate repeated imports and reconcile updates/deletions.
- Keep imported actual activity separate from planned workouts.
- Use explicit time zones/instants and avoid double-counting overlapping sources.
- Treat absent metrics as absent, not zero or fabricated values.
- Stop new reads after permissions are revoked; explain the handling of locally
  retained records and provide a deletion path.
- Test unavailable provider, denied/partial permissions, empty records, repeat
  import, time-zone boundaries and recovery after provider errors.

Before Phase 2 release, update [PRIVACY.md](PRIVACY.md) with the exact record
types accessed, retention/deletion behavior and permission rationale. No health
data upload or backend is planned.
