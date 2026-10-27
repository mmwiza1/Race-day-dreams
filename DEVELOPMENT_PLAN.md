# Active increment — 0.9.0-test1

Architecture Hardening + Strength Engine 2.0 is in development on
`feature/strength-engine-2-architecture-hardening`, based on live main `8b75755`.
Phase1 is a behaviour-preserving extraction and must pass existing backup/build/API gates
before goal-specific Strength feature work. The full requested phases are retained in
CONTINUE_CODEX.md; this increment does not reduce them to architecture alone.

Running Coach and Strength Coach will each be independently usable. Hybrid use adds
context without silently sacrificing either discipline. New Strength durability belongs
in typed/versioned storage, with explicit immutable programme and athlete decisions.
No new durable concepts go into the legacy Release08Store catch-all.

The app recommends. The athlete decides. The plan adapts.

---

# Development status — 0.8.2-test1

The app recommends. The runner decides. The plan adapts.

## Preserved foundations

Local-first Compose/Java architecture; Health Connect/manual baseline; original
running plans and versions; weekly reviews; runner-approved adaptations; Run Score;
qualified splits/PBs; shoes; reminders; body weight; Calendar List/Week/Month semantics.
No account, Internet permission, analytics, backend or runtime LLM requirement.

## Preserved 0.5 increment

- Direct Samsung scaffolding removed; old preferences normalize to Health Connect.
  No imported activity data is deleted. Schema 6; backup envelope 4 reads versions 1–3.
- 174 canonical exercise IDs, aliases/ranked search, favourites/recents, hide/archive,
  custom definitions, measurement profiles and kg/metres/seconds storage.
- Strength dashboard and exercise detail; source-preserving PB progression; manual
  PB edits/deletion; source-workout edits; timed/distance logging; previous-session
  reference; persistent explicit PB acknowledgement.
- 174 offline original illustrated guides with instructions, cues, mistakes, equipment,
  muscles and alternatives. Expert technique review and richer artwork remain desirable.
- Optional original six-week gym blocks, goals/priorities, 1–6 days, equipment/time
  constraints, RPE/RIR guidance, immutable versions, move/skip/reduction previews.
- Progress dashboard; existing-result-first PBs; bounded history filters; run comparison;
  Home summary reusing weekly evidence; pre-run context and one-off move warnings.

## Verification gate

Android build/lint, migrations/backup/UI tests, API 28/35/36 instrumentation, genuine
screenshots and final API-35 APK remain gates. VERIFICATION.md records actual results.

## Deliberate limits

No automatic technical Olympic lifting or task-specific timed/distance progression.
Session length is approximate. Programming is a test-build coaching heuristic, not
an individually validated physiological model or promise of optimal outcomes.

Native recording and GPX now supply provider-independent rich activity evidence
(see the 0.6 section below). FIT, maps and additional structured adapters remain
future work. Health Connect is supported; direct Samsung access is not.

## Test-build limitations to keep visible

History uses SQL keyset paging over metadata with activity-type/exercise/date filters
and additive schema10 indexes. List cards do not load raw GPS. Detail samples load
only when needed. Large synthetic history/query tests protect paging and filters.

Generated gym moves/skips preserve their prescriptions and reasons in block versions.
The Calendar shows current gym placements; additional old-date gym-edit annotations
remain a follow-up. Existing running moved/skipped/completion semantics are retained.

Automatic time/distance strength progression stays conservative (hold/review);
repetition/load suggestions have explicit runner approval and retained decisions.
Technique figures are original movement-specific illustrations, not individual form assessment.

## Preserved 0.6 Prompt 1 increment

Health Intelligence and native recording merged via PR #7, verified on main
`e381231f8a4c7483dea1c869df2f114a2bf2ba5f`. Optional health records and personal
trends; foreground GNSS service and monotonic metrics; pause/auto-pause/gap policies;
crash staging; BLE HR/real cadence; GPX; separate write-back; companion recording archive
are implemented. This section supersedes earlier statements that GPX/native recording
are future-only boundaries. Hardware behaviour remains subject to TEST_BUILD.md checks.

See docs/RUN_ENGINE.md for genuine limits (recover-finalize rather than continue after
process death; two-file backup; no GPX sensor extensions; no route/HR HC write-back).
Prompt 2 implementation and remaining limits are recorded below.
No direct Samsung integration, map dependency, Internet or account was added.

## 0.7 Prompt 2 increment

Branch codex/live-coach-race-day, PR #8. Deterministic service-owned execution,
qualified cues/local TTS/haptics, corroborated optional recovery alternatives, race
ranges/strategy/official result links, support routines/original figures, persisted
Dream Route equivalents, combined backup and bounded native sample hydration.
Schema8, JSON6 (reads1–5), recording archive2 (reads1), complete ZIP1.

Verification is incremental; consult VERIFICATION.md for actual observed source/run
results. Physical audio/GPS/BLE/OEM battery/accessibility behaviour remains unverified.
Current city choices use sourced geographic revisions; old assignments retain their
original drawings. Paris uses the annual Marathon de Paris2026 street reconstruction. These approximate
lines are not certified current event geometry or navigation.
The planner's existing prescriptions remain intact. Custom interval authoring and
race-week checklist were added in0.8. Repeating/restarting an active rep and targeted
late-run RPE remain future work. Geographic course/data limitations are recorded in
docs/DREAM_ROUTES.md rather than claiming current certified geometry.
Wear OS, maps/tiles, public release, cloud/accounts/LLM/social remain out of scope.

## 0.8 Today / Run Now / offline polish

See docs/RELEASE_0_8.md for implemented functionality and explicit limits. New work uses
the existing recorder, execution and versioning rules. 0.8.1 now uses separately versioned licensed geographic revisions;
docs/DREAM_ROUTES.md records factual sources and the annual Paris2026 source audit. Known-duration plans respect a saved time budget;
distance-only prescriptions disclose uncertainty rather than inventing pace. Source,
CI, capture and delivery results belong in VERIFICATION.md.

## 0.8.1 design/guidance/reliability increment

All174canonical exercises and14supporting movements have original offline illustrated
guidance, enforced by strict content/resource/provenance tests. Week-first Plan,
compact Calendar key, Live Coach range visual, human Health/Shoe/WeeklyReview summaries,
weekly-distance chart and Run Now hierarchy are implemented. Recorder staging/start
is atomic, retries are idempotent and interrupted recovery preserves honest partial
evidence. Versioned/rolling-development GitHub prerelease workflows are gated by full
API28/35/36 verification and current actual screenshots. No permanent release yet.

Five sourced geographic Dream Routes retain old IDs/assignments. Original offline
context styling adds ODbL road/water/green-space extracts; no runtime tiles, map SDK,
Internet permission, accounts, analytics or new runtime dependency. Schema10 only
adds history indexes; JSON7/raw2/complete1 remain compatible. Current CI and visual
acceptance state belong in VERIFICATION.md and docs/VISUAL_REVIEW_0_8_1.md. PRs #10
and #11 are merged; no S24 physical results are inferred.

## 0.8.2 premium Dream Routes presentation

The focused visual update is implemented on the accepted 0.8.1 geographic baseline:
five original illustrated city heroes, 15 landmark posters, the preserved full geographic
course with a dominant blue equivalent section, clear progress and exact distance
summaries. Dark/200% layouts and offline, selected-city bounded caching are complete.
Assignments, geometry IDs/offsets, Paris 2026, backups and actual GPS remain unchanged.
No schema/format/dependency/permission change was required.

Source fc47aae passed workflow 40092248558: 213 instrumentation tests on each API 28/35/36,
build/lint, strict 174/174 + 14/14 guidance and 77 actual captures adopted by bot c408306.
All 14 Dream panels were visually reviewed against the owner's inline premium board;
no design blocker was found. See RELEASE_0_8_2.md, VISUAL_REVIEW_0_8_2.md and VERIFICATION.md
for source identity, size and exact evidence. The final-head/main workflows control
publication. Physical S24 acceptance remains unperformed; this is a test release.
