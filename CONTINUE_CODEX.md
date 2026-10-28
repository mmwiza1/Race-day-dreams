# Continue Codex — current main to 0.10.0-test1

## Purpose

This file is the canonical continuation handoff for Race Day Dreams.

When instructed with:

`Read CONTINUE_CODEX.md and continue from the current repo state`

continue from the repository exactly as it exists. Inspect the live repo before changing anything. Preserve completed work, infer unfinished work from the current branch/diff/log, complete partial implementation, run the relevant tests/lint/build, fix regressions, and leave a concise summary of what was completed and what remains.

Do not blindly reset, force-push, discard valid changes, or recreate already-completed work.

## Current completed baseline

Repository: `mmwiza1/Race-day-dreams`

The retained 0.9 scope is COMPLETE and merged to `main`.

At the time this handoff was refreshed, `main` was:

- commit `1fd0d2d67e7379fa3d9478e18f5bb0268a4957de`
- version `0.9.0-test1`
- versionCode `12`
- database schema `13`
- root JSON backup envelope `8`
- raw recording archive `2`
- complete backup package `1`

Treat those values as a checkpoint, not an instruction to reset to that SHA. Always fetch/inspect live `main` first in case the repository has advanced.

0.9 delivered and should be preserved:

- architecture hardening and smaller feature boundaries
- standalone Running / Strength / Hybrid focus
- race-free Strength-first Today experience
- Gym Now planned, quick, custom-workout, favourite, repeat, generated and open entry paths
- crash-safe gym execution and recovery
- prescribed targets kept separate from actual entered sets
- immutable workout/source/version provenance
- 174 canonical exercises with technique guidance plus routine movements
- relevance-first exercise discovery, favourites and recents
- reversible exercise exclusions that do not delete history/PBs
- explained today-only busy/unavailable substitutions
- start-anchored six-week gym allocation and corrected lower-body variation
- conservative existing progression and starting-load guidance
- one shared SQLite restore transaction and typed persistence for the new strength state
- retained GPS evidence and prepared Route Story projection
- no INTERNET permission, account, backend, analytics or runtime LLM

The post-merge `main` workflow passed on API 28 / 35 / 36. At that checkpoint the project had 331 instrumentation tests on EACH supported API, 55 schema tests, about 219,527 domain assertions and 91 authentic canonical screenshots.

Galaxy S24 physical acceptance is still not proven by CI and must never be claimed unless actually performed.

## Next release: 0.10.0-test1 — Strength Engine 2.0 completion

The earlier instruction saying NOT to revive the larger Strength Engine backlog is obsolete. That restriction only applied while stabilising 0.9.

The next intended feature release is `0.10.0-test1`, built from current `main`, and should resume the deferred Strength Engine 2.0 work while preserving all stable 0.9 behaviour.

Before implementation:

1. fetch/pull and inspect live `main`, `git status`, `git log`, current versioning, AGENTS/repo instructions, schema/backups and CI configuration;
2. run the appropriate baseline tests so failures are not mistaken for new regressions;
3. create/use a feature branch rather than developing directly on `main`, unless the repository's current instructions explicitly say otherwise;
4. if the version is still 0.9.0-test1/code 12, use the repository's normal convention for 0.10.0-test1 and the next versionCode; do not guess if conventions have changed.

## 0.10 Strength Engine scope

Implement the remaining Strength Engine 2.0 work as a coherent product, not as disconnected screens. Keep the principle:

**The app recommends. The runner decides. The plan adapts.**

### 1. Full custom strength programme builder

Allow users to create, edit, save and reuse their own multi-week strength programmes rather than only accepting generated programmes or one-off Gym Now sessions.

Support, where consistent with the current domain model:

- programme name and goal
- programme duration / mesocycle length
- training days per week
- day/session names
- exercise ordering
- sets and rep ranges
- load or effort guidance where supported
- RPE/RIR targets where supported
- rest periods
- notes
- progression intent
- deload/recovery weeks
- duplicate/reorder/delete session and exercise blocks
- templates and saved programmes
- preview before activation
- programme versioning so historical completed sessions continue to reference the exact prescription that was performed

Never rewrite historical actuals when a programme is edited.

### 2. Stronger Strength / Muscle / Power programme design

Make generated programmes meaningfully different by goal rather than mostly rearranging the same exercise pool.

Preserve deterministic, explainable generation. Do not claim scientific precision the evidence cannot support.

Differentiate at least:

- general strength
- muscle / hypertrophy
- power / athletic performance
- running support / hybrid
- general fitness

Use appropriate movement balance, rep ranges, volume distribution, exercise selection and fatigue management for each goal.

### 3. Experience-aware programming

Improve beginner / intermediate / advanced differentiation.

Experience should affect things such as:

- complexity
- weekly volume
- exercise selection
- progression aggressiveness
- use of advanced techniques
- recovery assumptions

Do not make advanced automatically mean unsafe volume or complexity.

### 4. Rest timer

Add a practical rest timer to gym execution.

Requirements:

- starts from the workout/exercise flow without interrupting logging
- sensible prescribed default when the programme contains one
- user can change/skip/restart it
- works with screen changes/background behaviour as safely as Android permits
- clear haptic/audio notification if enabled
- accessible controls
- timer state should not corrupt the workout if the app/process is interrupted

Do not make rest duration a hidden compliance score.

### 5. Warm-up generation for strength sessions

Generate concise optional warm-ups based on the upcoming session, movement patterns and available equipment.

Keep them deterministic and explainable. Warm-ups are optional support, not mandatory adherence evidence.

Avoid pretending a warm-up prevents injury.

### 6. Supersets and grouped blocks

Support programme/execution structures beyond a flat exercise list, including supersets where useful.

The persistent model must distinguish grouping/prescription from actual entered sets. UI should make sequence and rest behaviour obvious.

Do not break existing normal-session history or Gym Now workouts.

### 7. Exercise catalogue expansion

Expand the canonical exercise catalogue beyond the current 174 toward roughly 220+ only where quality can be maintained.

Every added canonical exercise should have the metadata needed by the existing discovery/generation system and must not degrade search quality.

Prefer useful gaps over padding the count.

Technique guidance must remain truthful. Never attach a generic or incorrect illustration/instruction to an exercise simply to satisfy coverage.

### 8. Custom exercises

Allow users to create personal exercises when the canonical catalogue does not contain what they need.

Support sensible fields such as:

- name
- movement/body area classification where known
- equipment
- measurement profile
- optional notes
- favourite/archive behaviour

Custom exercises must be clearly distinct from canonical exercises, survive backup/restore, participate in logging/history/PBs where the measurement profile allows it, and never masquerade as professionally authored technique guidance.

### 9. Progression Engine improvements

Build on the existing conservative progression logic rather than replacing it with aggressive automatic load increases.

Use actual recent working-set evidence, completed reps, recorded RPE/RIR where present, session comparability, gaps/staleness and current prescription.

Maintain reversible recommendations and explicit reasoning.

The user should be able to keep the current load, accept a suggested change or ignore it.

No silent load changes.

### 10. Strength analytics and volume trends

Add useful long-term Strength insights rather than only raw charts.

Potential supported metrics include:

- working-set volume trends where the exercise/profile makes that meaningful
- training frequency
- movement-pattern balance
- rep-range distribution
- exercise consistency
- estimated-strength trends
- PB trends
- recent vs previous meaningful windows

Use evidence thresholds and avoid fake causal conclusions.

Do not compare unlike measurement profiles as if they were equivalent.

### 11. PB redesign

Preserve existing PB/history provenance while improving the experience.

Featured exact rep PBs should include actual evidence for:

- 1RM
- 3RM
- 5RM
- 6RM
- 8RM
- 10RM

Only show categories for which actual evidence exists.

Estimated 1RM must remain clearly separate from an actually recorded 1RM.

Edits/deletions must still recalculate derived PB evidence safely.

### 12. Achievements without over-gamification

Celebrate genuinely meaningful strength milestones and consistency without turning the app into an endless badge/leaderboard system.

Good examples include a new PB, completing a programme block, meaningful consistency or returning after a gap.

Do not reward unsafe behaviour such as excessive volume or training through recovery warnings.

### 13. Deload / fatigue-aware strength support

Add conservative deload/recovery guidance where enough evidence exists.

Use logged training evidence and existing health/training context carefully. Missing data remains unknown.

Never diagnose overtraining, injury or medical conditions.

Any programme adjustment must be visible, explained and optional.

### 14. Advanced hybrid run/strength interaction

Deepen the existing interaction between running and strength without making either discipline subordinate unless the user selected that priority.

Examples:

- heavy lower-body work can inform an upcoming quality/long run recommendation
- high running load can inform strength scheduling/volume suggestions
- peak race weeks/taper can optionally reduce lower-body strength volume
- spontaneous runs remain real load
- strength-first users should not have their gym plan silently sacrificed for running

Never silently reschedule or mutate either plan. Show the conflict and give choices.

## Architecture rules for 0.10

Do not undo the 0.9 hardening.

- Keep ViewModels/coordinators/repositories scoped; do not recreate a giant CoachViewModel, FeatureStore or god service.
- Reuse the existing authoritative gym execution path rather than creating a second competing engine.
- Keep prescription, actuals, provenance and programme versions separate.
- Prefer typed persistence for durable entities/state over expanding generic JSON buckets.
- Preserve one authoritative database/restore transaction model.
- Make schema changes additive and migration-tested.
- Bump backup format only when the persistent contract genuinely requires it and retain backward reads where supported.
- Preserve old runs, raw recordings, plans, strength sessions, PBs, shoes, health context, Dream assignments, programme versions and accepted decisions.
- Unsupported/corrupt restore must remain non-destructive.
- No INTERNET permission, backend, account requirement, analytics or runtime LLM unless the owner explicitly changes that product direction.

## UX rules

- Summary-first, not engineering-test-screen-first.
- Preserve the semantic soft-tinted Race Day Dreams visual language and dark-green primary brand/CTA.
- Colour must not be the only carrier of meaning.
- Maintain light/dark mode and large-font accessibility.
- Keep TalkBack/content descriptions useful.
- User choice always wins over generated advice.
- No fake controls, dead-end placeholders or demo-only production paths.

## Safety / coaching language

Avoid medical or deterministic claims such as:

- “You are overtrained.”
- “You are injured.”
- “This exercise caused your pain.”

Prefer evidence-bounded wording such as:

- “Recent training suggests this session may feel more demanding.”
- “You recorded higher effort in your last comparable sessions.”
- “Consider keeping the session, reducing volume, moving it, or resting.”

Recommendations must remain reversible.

## Testing expectations

Do not game counts. Extend the existing test standard with focused tests for the new domain behaviour.

At minimum cover:

- custom programme create/edit/version/history behaviour
- programme templates and mesocycle boundaries
- supersets/grouped execution
- rest timer state/recovery
- generated warm-up selection and missing-evidence behaviour
- goal/experience-specific generation
- exclusions/favourites/custom exercises interaction
- custom-exercise backup/restore
- progression evidence/staleness/RPE/RIR edge cases
- volume analytics and unlike-measurement protections
- exact 6RM and PB recalculation
- actual vs estimated 1RM distinction
- deload recommendations never silently mutate plans
- hybrid conflict choices and provenance
- migration from every currently supported schema fixture
- previous backup restore and current round-trip
- API 28/35/36 instrumentation unless the supported matrix has deliberately changed
- accessibility and large-font coverage
- no INTERNET permission
- no runtime LLM

Use authentic current-build screenshots for any canonical screenshot changes. Never substitute mockups for release evidence.

## Performance / scale

Watch for long-history and backup memory growth.

The 0.9 stress fixture with about 450 workouts / 2,250 sets produced a roughly 19 MB backup and showed significant heap use. Preserve the existing safe preflight behaviour and prefer streaming/bounded processing where practical rather than constructing increasingly large in-memory representations.

Avoid O(n²) analytics/history loops and unnecessary Compose recomposition.

## Physical Galaxy S24 acceptance

CI/emulators do not prove real-device behaviour. Do not mark these as passed unless physically tested:

- outdoor GPS and poor-GPS recovery
- screen-off/background/locked recording
- long-duration battery behaviour
- force-close/process recovery
- Health Connect provider read/write/reimport/permissions
- BLE HR reconnect and real cadence hardware if available
- TTS with music/calls/audio focus
- TalkBack/200% real use
- complete backup/restore on device

Document physical-only checks separately from automated verification.

## Release workflow

For 0.10 work:

1. implement in logical, reviewable phases/commits;
2. keep this file updated when a usage limit or major checkpoint is approaching so a new Codex session can resume accurately;
3. run domain/schema/format/lint/build and supported instrumentation gates;
4. fix root causes rather than blindly rerunning red CI;
5. generate authentic APK/screenshots from the actual release candidate;
6. follow the repository's current release-note/template instructions;
7. open a PR to `main` with a truthful scope/test/migration/physical-limit summary;
8. merge only when the exact PR head is green and coherent;
9. after merge, verify live `main`, post-merge CI, final SHA, version, schema/backup changes and canonical APK artifacts.

Do not create a stable/public-release claim unless the owner explicitly asks for one.

## Continuation behaviour

If a future Codex session opens this file mid-implementation:

- inspect `git status`, `git diff`, `git log --oneline`, current branch and remote state first;
- read any current TODO/checkpoint notes and the actual changed code;
- preserve completed work;
- infer the next unfinished item from repository evidence rather than restarting the whole release;
- complete partial code before starting unrelated work;
- run the most relevant tests after each coherent subsystem;
- update this file again with the new checkpoint if substantial work remains;
- finish with a concise summary of completed work, tests run, failures fixed, remaining items and any physical-device checks that still require the owner.

The target outcome is a serious, free, private running and strength coach that gives useful recommendations without taking control away from the runner.